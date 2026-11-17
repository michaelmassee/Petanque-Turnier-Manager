/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.finalrunde.MaastrichterFinalrundeSheet;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Ein komplettes Maastrichter Turnier gegen den lokal gestarteten Worker: Online-Anmeldungen, zwei Vorrunden im
 * Schweizer System, KO-Finalgruppen A und B bis zum Finale, Abschluss. Ein Spieler meldet Ergebnisse über die
 * Live-Ansicht: In der Vorrunde trägt PTM die übernommene Meldung in die Spielrunde ein; im Turnierbaum kann die
 * Turnierleitung eine Meldung nur verwerfen und trägt das Ergebnis selbst ein.
 */
class PtmOnlineMaastrichterZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final int ANZ_TEAMS = 8;
    private static final int ANZ_VORRUNDEN = 2;
    /** Acht Teams in zwei KO-Gruppen à vier: Halbfinale und Finale je Gruppe. */
    private static final int GRUPPEN_GROESSE = 4;
    private static final int ANZ_RUNDEN_ONLINE = ANZ_VORRUNDEN + 2;
    private static final List<String> GRUPPEN = List.of("A", "B");
    private static final String SCORE_PRAEFIX = "PTM_EDIT:";
    private static final int SPIELERKONTO = 11;
    private static final Person[] TEAM_MIT_KONTO = { Person.konto("Mia", "Maas", SPIELERKONTO),
            Person.gast("Max", "Maas") };

    private String idMitKonto;

    /** Partie im Turnierbaum: Ergebnis-Zellen der Seiten A und B. */
    private record KoPartie(XSpreadsheet blatt, Position punkteA, Position punkteB) {}

    @Test
    void komplettesTurnierMitVorrundenKoRundenUndErgebnismeldungen() throws Exception {
        JsonObject einstellungen = new JsonObject();
        einstellungen.addProperty("playerResultReportingEnabled", true);
        maastrichterTurnierAnlegen(Formation.DOUBLETTE, IN_EINEM_MONAT, einstellungen, ANZ_VORRUNDEN,
                GRUPPEN_GROESSE);
        List<Person[]> teams = onlineAnmelden();
        verbinden();
        abgleichen();
        assertThat(lokaleMeldungen()).as("alle Online-Anmeldungen in der Meldeliste").hasSize(ANZ_TEAMS);
        for (Person[] team : teams) {
            einchecken(zeile(team[0]));
        }
        int nrMitKonto = ziel.getTeamNrAusZeile(zeile(TEAM_MIT_KONTO[0]));

        // Vorrunde 1: das Team mit Konto meldet, die Turnierleitung übernimmt; die übrigen trägt sie selbst ein.
        assertThat(maastrichterVorrundeAuslosen()).as("Vorrunde 1 mit allen Teams").hasSize(ANZ_TEAMS);
        warteBis("Vorrunde 1 online", () -> online.runden(turnierId).size() == 1);
        int[] partieMitKonto = partieVon(nrMitKonto);
        boolean kontoIstTeamA = partieMitKonto[1] == nrMitKonto;
        assertThat(online.ergebnisMelden(idMitKonto, SPIELERKONTO, 13, 5)).as("Meldung in der Vorrunde")
                .isEqualTo(HttpURLConnection.HTTP_OK);
        entscheide(warteAufMeldung(), Entscheidung.ERGEBNIS_UEBERNEHMEN);
        warteBis("gemeldetes Ergebnis in der Vorrunde eingetragen", () -> ergebnisInDerRunde(partieMitKonto[0])
                .equals(kontoIstTeamA ? List.of(13, 5) : List.of(5, 13)));
        alleUebrigenErgebnisseEintragen();
        warteBis("Vorrunde 1 online vollständig", () -> rundeVollstaendig(1));
        assertThat(online.offeneErgebnismeldungen(turnierId)).as("Meldung online erledigt").isEmpty();

        // Vorrunde 2
        assertThat(maastrichterVorrundeAuslosen()).as("Vorrunde 2 mit allen Teams").hasSize(ANZ_TEAMS);
        warteBis("Vorrunde 2 online", () -> online.runden(turnierId).size() == ANZ_VORRUNDEN);
        alleUebrigenErgebnisseEintragen();
        warteBis("Vorrunde 2 online vollständig", () -> rundeVollstaendig(ANZ_VORRUNDEN));
        assertThat(online.rangliste(turnierId)).as("Vorrunden-Rangliste online").hasSize(ANZ_TEAMS);

        // Finalrunden: zwei Turnierbäume, deren Halbfinals gleichzeitig online als eine Runde erscheinen.
        MaastrichterFinalrundeSheet finalrunden = new MaastrichterFinalrundeSheet(wkingSpreadsheet);
        starteUndWarte(finalrunden);
        assertThat(finalrunden.isLetzterLaufFehlgeschlagen()).as("Finalrunden erstellt").isFalse();
        warteBis("Halbfinals online", () -> online.runden(turnierId).size() == ANZ_VORRUNDEN + 1);
        assertThat(spiele(ANZ_VORRUNDEN + 1)).as("je Gruppe zwei Halbfinals").hasSize(4)
                .allSatisfy(spiel -> assertThat(text(spiel, "stageLabel")).isNotBlank());

        // Meldung im Turnierbaum: nur Verwerfen, die Turnierleitung trägt selbst ein.
        KoPartie koPartieMitKonto = offeneKoPartien().stream()
                .filter(partie -> teamNr(partie, partie.punkteA()) == nrMitKonto
                        || teamNr(partie, partie.punkteB()) == nrMitKonto)
                .findFirst().orElseThrow();
        assertThat(online.ergebnisMelden(idMitKonto, SPIELERKONTO, 13, 9)).as("Meldung im Halbfinale")
                .isEqualTo(HttpURLConnection.HTTP_OK);
        entscheide(warteAufMeldung(), Entscheidung.ERGEBNIS_VERWERFEN);
        warteBis("Meldung im Halbfinale online verworfen", () -> online.offeneErgebnismeldungen(turnierId).isEmpty());
        assertThat(istOffen(koPartieMitKonto)).as("Turnierbaum unverändert").isTrue();

        assertThat(koErgebnisseEintragen()).as("vier Halbfinals").isEqualTo(4);
        assertThat(koErgebnisseEintragen()).as("zwei Finals (ggf. mit Spiel um Platz 3)").isBetween(2, 4);
        assertThat(koErgebnisseEintragen()).as("alle KO-Partien gespielt").isZero();
        warteBis("Finals online", () -> online.runden(turnierId).size() == ANZ_RUNDEN_ONLINE);
        warteBis("Turnierbaum online vollständig", () -> rundeVollstaendig(ANZ_VORRUNDEN + 1)
                && rundeVollstaendig(ANZ_RUNDEN_ONLINE));
        String finale = I18n.get("ko.runde.titel.finale");
        assertThat(spiele(ANZ_RUNDEN_ONLINE)).as("Finale beider Gruppen")
                .anySatisfy(spiel -> assertThat(text(spiel, "stageLabel")).startsWith("A").endsWith(finale))
                .anySatisfy(spiel -> assertThat(text(spiel, "stageLabel")).startsWith("B").endsWith(finale));

        PtmOnlineAbschlussRunner abschluss = new PtmOnlineAbschlussRunner(wkingSpreadsheet, system, spieltagNr);
        starteUndWarte(abschluss);
        assertThat(abschluss.isLetzterLaufFehlgeschlagen()).as("Abschluss ohne Fehler").isFalse();
        assertThat(online.turnier(turnierId).get("status").getAsString()).as("online abgeschlossen")
                .isEqualTo("finished");
    }

    /** Acht Doublettes, eines davon mit verifiziertem Spielerkonto (meldet Ergebnisse). */
    private List<Person[]> onlineAnmelden() throws Exception {
        List<Person[]> teams = new ArrayList<>();
        idMitKonto = online.anmelden(turnierId, TEAM_MIT_KONTO);
        teams.add(TEAM_MIT_KONTO);
        for (int i = 2; i <= ANZ_TEAMS; i++) {
            Person[] team = { Person.gast("Maas" + i, "Erster"), Person.gast("Maas" + i, "Zweiter") };
            online.anmelden(turnierId, team);
            teams.add(team);
        }
        return teams;
    }

    /** {Blattzeile, Team A, Team B} der Partie des Teams in der zuletzt ausgelosten Vorrunde. */
    private int[] partieVon(int teamNr) throws Exception {
        return paareImBlatt().stream().filter(paar -> paar[1] == teamNr || paar[2] == teamNr).findFirst()
                .orElseThrow();
    }

    /** Wie die Turnierleitung: alle noch leeren Partien der zuletzt ausgelosten Vorrunde. */
    private void alleUebrigenErgebnisseEintragen() throws Exception {
        for (int[] paar : paareImBlatt()) {
            if (ergebnisInDerRunde(paar[0]).contains(null)) {
                ergebnisEintragen(paar[0], 13, 7);
            }
        }
    }

    /** Punkte einer Partie der zuletzt ausgelosten Vorrunde ({@code null} = leer). */
    private List<Integer> ergebnisInDerRunde(int zeile) throws Exception {
        XSpreadsheet blatt = letzteRunde.getXSpreadSheet();
        List<Integer> punkte = new ArrayList<>();
        punkte.add(punkte(blatt, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile)));
        punkte.add(punkte(blatt, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, zeile)));
        return punkte;
    }

    /** Die Meldung kommt nach dem letzten Schreiben; der Test stößt den Abruf sofort an. */
    private String warteAufMeldung() throws Exception {
        PtmOnlineLiveBeobachter.ergebnismeldungenAbholen(dokument());
        List<String> gefunden = new ArrayList<>();
        warteBis("Meldung in der Konfliktliste", () -> {
            gefunden.clear();
            mapping.konfliktListe().leseFaelle().keySet().stream()
                    .filter(key -> key.startsWith(PtmOnlineErgebnismeldungen.SCHLUESSEL_PRAEFIX))
                    .forEach(gefunden::add);
            return gefunden.size() == 1;
        });
        return gefunden.getFirst();
    }

    private void entscheide(String schluessel, Entscheidung entscheidung) throws Exception {
        mapping.konfliktListe().setzeEntscheidung(schluessel, entscheidung.anzeige());
        PtmOnlineLiveBeobachter.anstossen(dokument());
        warteBis("Fall aus der Konfliktliste entfernt", () -> mapping.konfliktListe().leseFaelle().keySet()
                .stream().noneMatch(key -> key.startsWith(PtmOnlineErgebnismeldungen.SCHLUESSEL_PRAEFIX)));
    }

    /**
     * Trägt alle KO-Partien ein, deren Teams feststehen und die noch kein Ergebnis haben (Seite A gewinnt 13:7). Die
     * Sieger rücken per Formel in die nächste Spalte vor; jeder Aufruf spielt also eine Runde.
     *
     * @return Anzahl eingetragener Partien
     */
    private int koErgebnisseEintragen() throws Exception {
        List<KoPartie> offen = offeneKoPartien();
        for (KoPartie partie : offen) {
            sheetHlp.setNumberValueInCell(NumberCellValue.from(partie.blatt(), partie.punkteA()).setValue(13));
            sheetHlp.setNumberValueInCell(NumberCellValue.from(partie.blatt(), partie.punkteB()).setValue(7));
        }
        return offen.size();
    }

    /** KO-Partien aller Finalgruppen mit feststehenden Teams und ohne Ergebnis. */
    private List<KoPartie> offeneKoPartien() {
        List<KoPartie> offen = new ArrayList<>();
        for (String gruppe : GRUPPEN) {
            String schluessel = SheetMetadataHelper.schluesselMaastrichterFinalrunde(gruppe);
            Optional<XSpreadsheet> blatt = SheetMetadataHelper.findeSheet(dokument(), schluessel);
            assertThat(blatt).as("Finalgruppe %s", gruppe).isPresent();
            List<Position> positionen = ergebnisPositionen(
                    SheetMetadataHelper.leseScoreText(dokument(), SheetMetadataHelper.scoreSchluessel(schluessel)));
            for (int i = 0; i + 1 < positionen.size(); i += 2) {
                KoPartie partie = new KoPartie(blatt.get(), positionen.get(i), positionen.get(i + 1));
                if (teamNr(partie, partie.punkteA()) > 0 && teamNr(partie, partie.punkteB()) > 0 && istOffen(partie)) {
                    offen.add(partie);
                }
            }
        }
        return offen;
    }

    /** Team-Zelle links neben der Ergebnis-Zelle; ≤ 0, solange das Team nicht feststeht. */
    private int teamNr(KoPartie partie, Position punkte) {
        return sheetHlp.getIntFromCell(partie.blatt(), Position.from(punkte.getSpalte() - 1, punkte.getZeile()));
    }

    private boolean istOffen(KoPartie partie) {
        return punkte(partie.blatt(), partie.punkteA()) == null && punkte(partie.blatt(), partie.punkteB()) == null;
    }

    /** Beim Erzeugen gespeicherte Ergebnis-Zellen des Turnierbaums ({@code PTM_EDIT:spalte,zeile|…}). */
    private static List<Position> ergebnisPositionen(String scoreText) {
        assertThat(scoreText).as("Ergebnis-Positionen des Turnierbaums").startsWith(SCORE_PRAEFIX);
        List<Position> positionen = new ArrayList<>();
        for (String eintrag : scoreText.substring(SCORE_PRAEFIX.length()).split("\\|")) {
            String[] teile = eintrag.split(",");
            positionen.add(Position.from(Integer.parseInt(teile[0].strip()), Integer.parseInt(teile[1].strip())));
        }
        return positionen;
    }

    private Integer punkte(XSpreadsheet blatt, Position position) {
        String text = sheetHlp.getTextFromCell(blatt, position);
        return text == null || text.isBlank() ? null : sheetHlp.getIntFromCell(blatt, position);
    }

    /** Online-Partien der Runde (1-basiert) in der Reihenfolge der Spielverwaltung. */
    private List<JsonObject> spiele(int rundeNr) throws Exception {
        Map<Integer, JsonObject> runden = new LinkedHashMap<>();
        for (JsonObject runde : online.runden(turnierId)) {
            runden.put(runde.get("roundNumber").getAsInt(), runde);
        }
        List<JsonObject> spiele = new ArrayList<>();
        JsonObject runde = runden.get(rundeNr);
        if (runde != null) {
            runde.getAsJsonArray("matches").forEach(spiel -> spiele.add(spiel.getAsJsonObject()));
        }
        return spiele;
    }

    /** Alle Partien der Runde haben online ein Ergebnis. */
    private boolean rundeVollstaendig(int rundeNr) throws Exception {
        List<JsonObject> spiele = spiele(rundeNr);
        return !spiele.isEmpty() && spiele.stream().allMatch(spiel -> hatWert(spiel, "scoreA") && hatWert(spiel,
                "scoreB"));
    }

    private static boolean hatWert(JsonObject json, String feld) {
        JsonElement wert = json.get(feld);
        return wert != null && !wert.isJsonNull();
    }

    private static String text(JsonObject json, String feld) {
        return hatWert(json, feld) ? json.get(feld).getAsString() : "";
    }

    private XSpreadsheetDocument dokument() {
        return wkingSpreadsheet.getWorkingSpreadsheetDocument();
    }
}
