/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.ko.konfiguration.IKoBracketKonfiguration;

/**
 * Liest einen KO-Turnierbaum (KO, Poule-KO, Maastrichter-Finalrunden, Kaskaden-Felder) über die beim Erzeugen
 * gespeicherten Positionen der Ergebnis-Zellen ({@code PTM_EDIT:spalte,zeile|…}, je Partie Team A und Team B).
 * <p>
 * Layout je Partie: Team-Zelle links neben der Ergebnis-Zelle, Bahn links neben der Team-Zelle der Seite A – in
 * welchen Runden, bestimmt die Turnierbaum-Konfiguration ({@link BahnSpalten}). Jede Ergebnis-Spalte ist eine
 * Runde; eine erste Spalte mit weniger Partien als für einen vollständigen Baum nötig ist die Cadrage. Die zweite Partie der Finalspalte ist das Spiel um Platz 3.
 * Partien, deren Teams noch nicht feststehen, entfallen. Jede Partie kennt ihre beiden Ergebnis-Zellen, damit eine
 * übernommene Spieler-Ergebnismeldung dort eingetragen werden kann. Ist das Finale entschieden, liefert
 * {@link Feld#sieger()} den Sieger des Baums.
 */
final class TurnierbaumLiveLeser {

    private static final Logger logger = LogManager.getLogger(TurnierbaumLiveLeser.class);
    private static final String SCORE_PRAEFIX = "PTM_EDIT:";

    /** Buchstaben, unter denen mehrere Turnierbäume/Felder eines Turniers angelegt werden. */
    private static final List<String> BAUM_BUCHSTABEN = List.of("A", "B", "C", "D", "E", "F", "G", "H");

    private TurnierbaumLiveLeser() {}

    /**
     * Liest alle vorhandenen, gleichzeitig gespielten Turnierbäume „A“ bis „H“.
     *
     * @param schluessel Metadaten-Schlüssel je Buchstabe
     * @param praefix    Anzeige vor der Rundenbezeichnung je Buchstabe
     * @param konfig     Turnierbaum-Konfiguration des Systems (Bahnspalten)
     * @return je Baum die Partien je Runde
     */
    static List<List<List<LivePartie>>> leseBaeume(WorkingSpreadsheet ws, UnaryOperator<String> schluessel,
            UnaryOperator<String> praefix, IKoBracketKonfiguration konfig) {
        return leseFelder(ws, schluessel, praefix, konfig).stream().map(Feld::runden).toList();
    }

    /**
     * Wie {@link #leseBaeume}, je Baum mit seiner Bezeichnung und dem Finale.
     *
     * @return je vorhandenem Baum das Feld, in der Reihenfolge „A“ bis „H“
     */
    static List<Feld> leseFelder(WorkingSpreadsheet ws, UnaryOperator<String> schluessel,
            UnaryOperator<String> praefix, IKoBracketKonfiguration konfig) {
        BahnSpalten bahnSpalten = BahnSpalten.aus(konfig);
        List<Feld> felder = new ArrayList<>();
        for (String buchstabe : BAUM_BUCHSTABEN) {
            String bezeichnung = praefix.apply(buchstabe);
            Baum baum = leseBaum(ws, schluessel.apply(buchstabe), bezeichnung, bahnSpalten);
            if (!baum.runden().isEmpty()) {
                felder.add(new Feld(bezeichnung, baum.runden(), baum.finale()));
            }
        }
        return felder;
    }

    /**
     * Ein gelesener Turnierbaum mit Bezeichnung.
     *
     * @param finale Finalpartie, {@code null} solange ihre Teams noch nicht feststehen
     */
    record Feld(String bezeichnung, List<List<LivePartie>> runden, @Nullable LivePartie finale) {

        Feld {
            runden = List.copyOf(runden);
        }

        /** Sieger des Felds, sobald das Finale entschieden ist (beide Ergebnisse eingetragen, kein Gleichstand). */
        Optional<LiveKoSieger> sieger() {
            if (finale == null || finale.punkteA() == null || finale.punkteB() == null
                    || finale.punkteA().equals(finale.punkteB())) {
                return Optional.empty();
            }
            List<Integer> team = finale.punkteA() > finale.punkteB() ? finale.teamA() : finale.teamB();
            return Optional.of(new LiveKoSieger(bezeichnung, team));
        }
    }

    /** Runden eines Baums und sein Finale ({@code null}, solange dessen Teams nicht feststehen). */
    private record Baum(List<List<LivePartie>> runden, @Nullable LivePartie finale) {

        static final Baum LEER = new Baum(List.of(), null);
    }

    /**
     * @param baumSchluessel Metadaten-Schlüssel des Turnierbaum-Blatts
     * @param praefix        Anzeige vor der Rundenbezeichnung (z.&nbsp;B. „Poule A“), {@code null} wenn keine
     * @param bahnSpalten    Runden mit Bahnspalte laut Konfiguration
     * @return Partien je Runde des Baums; leer, wenn der Baum (noch) nicht existiert
     */
    static List<List<LivePartie>> leseRunden(WorkingSpreadsheet ws, String baumSchluessel, @Nullable String praefix,
            BahnSpalten bahnSpalten) {
        return leseBaum(ws, baumSchluessel, praefix, bahnSpalten).runden();
    }

    private static Baum leseBaum(WorkingSpreadsheet ws, String baumSchluessel, @Nullable String praefix,
            BahnSpalten bahnSpalten) {
        var xDoc = ws.getWorkingSpreadsheetDocument();
        Optional<XSpreadsheet> blatt = SheetMetadataHelper.findeSheet(xDoc, baumSchluessel);
        List<Position> positionen = scorePositionen(
                SheetMetadataHelper.leseScoreText(xDoc, SheetMetadataHelper.scoreSchluessel(baumSchluessel)));
        if (blatt.isEmpty() || positionen.size() < 2) {
            return Baum.LEER;
        }
        int letzteSpalte = positionen.stream().mapToInt(Position::getSpalte).max().orElse(0);
        int letzteZeile = positionen.stream().mapToInt(Position::getZeile).max().orElse(0);
        LiveZellbereich zellen = LiveZellbereich.lese(ws, blatt.get(), 0, 0, letzteSpalte, letzteZeile);
        String blattName = Lo.qi(XNamed.class, blatt.get()).getName();

        Map<Integer, List<Position[]>> paareProSpalte = new TreeMap<>();
        for (int i = 0; i + 1 < positionen.size(); i += 2) {
            paareProSpalte.computeIfAbsent(positionen.get(i).getSpalte(), spalte -> new ArrayList<>())
                    .add(new Position[] { positionen.get(i), positionen.get(i + 1) });
        }
        List<List<Position[]>> spalten = new ArrayList<>(paareProSpalte.values());
        boolean mitCadrage = spalten.size() > 1 && spalten.get(0).size() < (1 << (spalten.size() - 1));

        List<List<LivePartie>> runden = new ArrayList<>();
        LivePartie finale = null;
        for (int s = 0; s < spalten.size(); s++) {
            String rundenTitel = mitCadrage && s == 0 ? I18n.get("column.header.cadrage")
                    : rundenTitel(spalten.size() - 1 - s);
            List<LivePartie> partien = new ArrayList<>();
            List<Position[]> paare = spalten.get(s);
            boolean mitBahn = bahnSpalten.inSpalte(s);
            for (int p = 0; p < paare.size(); p++) {
                boolean platz3 = s == spalten.size() - 1 && p > 0;
                String titel = platz3 ? I18n.get("dialog.ko.spiel.um.platz3") : rundenTitel;
                Position[] paar = paare.get(p);
                Optional<LivePartie> partie = partie(zellen, paar, mitBahn,
                        praefix == null ? titel : praefix + " · " + titel)
                        .map(gelesen -> gelesen.mitErgebnisOrt(
                                ErgebnisOrt.inZellen(baumSchluessel, blattName, paar[0], paar[1])));
                partie.ifPresent(partien::add);
                if (s == spalten.size() - 1 && p == 0) {
                    finale = partie.orElse(null);
                }
            }
            runden.add(partien);
        }
        return new Baum(runden, finale);
    }

    private static Optional<LivePartie> partie(LiveZellbereich zellen, Position[] paar, boolean mitBahn,
            String stufe) {
        Position a = paar[0];
        Position b = paar[1];
        int teamA = zellen.teamNr(a.getSpalte() - 1, a.getZeile());
        int teamB = zellen.teamNr(b.getSpalte() - 1, b.getZeile());
        if (teamA <= 0 || teamB <= 0) {
            return Optional.empty();
        }
        // Als Text gelesen, damit auch Bahnbezeichnungen wie „A3“ ankommen (die Bahnspalte erlaubt bewusst Text).
        String bahn = mitBahn ? zellen.text(a.getSpalte() - 2, a.getZeile()) : null;
        return Optional.of(LivePartie.teams(teamA, teamB, zellen.punkte(a.getSpalte(), a.getZeile()),
                zellen.punkte(b.getSpalte(), b.getZeile()), bahn, stufe));
    }

    /**
     * Ergebnis-Spalten mit Bahnspalte, nach derselben Regel wie beim Erzeugen des Turnierbaums
     * ({@code KoTurnierbaumLayout#mitBahnInRunde}): Mit „Bahn nur in Runde 1“ hat nur die erste Spalte (Cadrage bzw.
     * erste Hauptrunde) eine Bahn; die späteren Runden haben dort die Verbinder-Spalte der Vorrunde.
     */
    record BahnSpalten(boolean mitBahn, boolean nurErsteSpalte) {

        static BahnSpalten aus(IKoBracketKonfiguration konfig) {
            return new BahnSpalten(konfig.getSpielbaumSpielbahn() != SpielrundeSpielbahn.X,
                    konfig.isSpielbaumBahnNurRunde1());
        }

        boolean inSpalte(int spaltenIndex) {
            return mitBahn && (!nurErsteSpalte || spaltenIndex == 0);
        }
    }

    /** @param abstandZumFinale 0 = Finale, 1 = Halbfinale, n = 1/2ⁿ-Finale */
    private static String rundenTitel(int abstandZumFinale) {
        return switch (abstandZumFinale) {
            case 0 -> I18n.get("ko.runde.titel.finale");
            case 1 -> I18n.get("ko.runde.titel.halbfinale");
            default -> I18n.get("ko.runde.titel.ntel.finale", 1 << abstandZumFinale);
        };
    }

    static List<Position> scorePositionen(@Nullable String scoreText) {
        List<Position> positionen = new ArrayList<>();
        if (scoreText == null || !scoreText.startsWith(SCORE_PRAEFIX)) {
            return positionen;
        }
        for (String eintrag : scoreText.substring(SCORE_PRAEFIX.length()).split("\\|")) {
            String[] teile = eintrag.split(",");
            if (teile.length != 2) {
                continue;
            }
            try {
                positionen.add(Position.from(Integer.parseInt(teile[0].strip()), Integer.parseInt(teile[1].strip())));
            } catch (NumberFormatException e) {
                logger.warn("PTM-Online Live: ungültige Score-Position '{}' übersprungen", eintrag, e);
            }
        }
        return positionen;
    }
}
