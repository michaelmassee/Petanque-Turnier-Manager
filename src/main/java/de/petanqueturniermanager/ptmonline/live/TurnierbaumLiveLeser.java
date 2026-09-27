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

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Liest einen KO-Turnierbaum (KO, Poule-KO, Maastrichter-Finalrunden, Kaskaden-Felder) über die beim Erzeugen
 * gespeicherten Positionen der Ergebnis-Zellen ({@code PTM_EDIT:spalte,zeile|…}, je Partie Team A und Team B).
 * <p>
 * Layout je Partie: Team-Zelle links neben der Ergebnis-Zelle, Bahn (sofern geführt) links neben der
 * Team-Zelle der Seite A. Jede Ergebnis-Spalte ist eine Runde; eine erste Spalte mit weniger Partien als für
 * einen vollständigen Baum nötig ist die Cadrage. Die zweite Partie der Finalspalte ist das Spiel um Platz 3.
 * Partien, deren Teams noch nicht feststehen, entfallen.
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
     * @return je Baum die Partien je Runde
     */
    static List<List<List<LivePartie>>> leseBaeume(WorkingSpreadsheet ws, UnaryOperator<String> schluessel,
            UnaryOperator<String> praefix) {
        List<List<List<LivePartie>>> baeume = new ArrayList<>();
        for (String buchstabe : BAUM_BUCHSTABEN) {
            List<List<LivePartie>> runden = leseRunden(ws, schluessel.apply(buchstabe), praefix.apply(buchstabe));
            if (!runden.isEmpty()) {
                baeume.add(runden);
            }
        }
        return baeume;
    }

    /**
     * @param baumSchluessel Metadaten-Schlüssel des Turnierbaum-Blatts
     * @param praefix        Anzeige vor der Rundenbezeichnung (z.&nbsp;B. „Poule A“), {@code null} wenn keine
     * @return Partien je Runde des Baums; leer, wenn der Baum (noch) nicht existiert
     */
    static List<List<LivePartie>> leseRunden(WorkingSpreadsheet ws, String baumSchluessel, @Nullable String praefix) {
        var xDoc = ws.getWorkingSpreadsheetDocument();
        Optional<XSpreadsheet> blatt = SheetMetadataHelper.findeSheet(xDoc, baumSchluessel);
        List<Position> positionen = scorePositionen(
                SheetMetadataHelper.leseScoreText(xDoc, SheetMetadataHelper.scoreSchluessel(baumSchluessel)));
        if (blatt.isEmpty() || positionen.size() < 2) {
            return List.of();
        }
        int letzteSpalte = positionen.stream().mapToInt(Position::getSpalte).max().orElse(0);
        int letzteZeile = positionen.stream().mapToInt(Position::getZeile).max().orElse(0);
        LiveZellbereich zellen = LiveZellbereich.lese(ws, blatt.get(), 0, 0, letzteSpalte, letzteZeile);

        Map<Integer, List<Position[]>> paareProSpalte = new TreeMap<>();
        for (int i = 0; i + 1 < positionen.size(); i += 2) {
            paareProSpalte.computeIfAbsent(positionen.get(i).getSpalte(), spalte -> new ArrayList<>())
                    .add(new Position[] { positionen.get(i), positionen.get(i + 1) });
        }
        List<List<Position[]>> spalten = new ArrayList<>(paareProSpalte.values());
        boolean mitCadrage = spalten.size() > 1 && spalten.get(0).size() < (1 << (spalten.size() - 1));

        List<List<LivePartie>> runden = new ArrayList<>();
        for (int s = 0; s < spalten.size(); s++) {
            String rundenTitel = mitCadrage && s == 0 ? I18n.get("column.header.cadrage")
                    : rundenTitel(spalten.size() - 1 - s);
            List<LivePartie> partien = new ArrayList<>();
            List<Position[]> paare = spalten.get(s);
            for (int p = 0; p < paare.size(); p++) {
                boolean platz3 = s == spalten.size() - 1 && p > 0;
                String titel = platz3 ? I18n.get("dialog.ko.spiel.um.platz3") : rundenTitel;
                partie(zellen, paare.get(p), praefix == null ? titel : praefix + " · " + titel)
                        .ifPresent(partien::add);
            }
            runden.add(partien);
        }
        return runden;
    }

    private static Optional<LivePartie> partie(LiveZellbereich zellen, Position[] paar, String stufe) {
        Position a = paar[0];
        Position b = paar[1];
        int teamA = zellen.teamNr(a.getSpalte() - 1, a.getZeile());
        int teamB = zellen.teamNr(b.getSpalte() - 1, b.getZeile());
        if (teamA <= 0 || teamB <= 0) {
            return Optional.empty();
        }
        int bahnSpalte = a.getSpalte() - 2;
        Integer bahn = bahnSpalte >= 0 && zellen.zahl(bahnSpalte, a.getZeile()) > 0
                ? zellen.zahl(bahnSpalte, a.getZeile()) : null;
        return Optional.of(LivePartie.teams(teamA, teamB, zellen.punkte(a.getSpalte(), a.getZeile()),
                zellen.punkte(b.getSpalte(), b.getZeile()), bahn == null ? null : String.valueOf(bahn), stufe));
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
