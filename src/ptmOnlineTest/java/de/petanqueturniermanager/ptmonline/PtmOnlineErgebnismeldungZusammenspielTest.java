/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Ergebnismeldung durch Spieler bei einem verbundenen Turnierdokument (E-27, E-28) gegen den lokal gestarteten
 * Worker: Ein Spieler meldet in der Live-Ansicht, PTM holt die Meldung im Hintergrund ab und stellt sie in die
 * Konfliktliste. Erst die Entscheidung der Turnierleitung trägt das Ergebnis ein bzw. verwirft die Meldung.
 */
class PtmOnlineErgebnismeldungZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final int SPIELERKONTO = 9;
    private static final Person[] TEAM_MIT_KONTO = { Person.konto("Mara", "Melder", SPIELERKONTO),
            Person.gast("Max", "Melder"), Person.gast("Mia", "Melder") };

    private String idMitKonto;
    private int zeileMitKonto;
    private boolean kontoIstTeamA;

    @Test
    void uebernommeneMeldungStehtErstNachDerEntscheidungInDerSpielrunde() throws Exception {
        rundeMitMeldungEinrichten(true);

        assertThat(online.ergebnisMelden(idMitKonto, SPIELERKONTO, 13, 7)).as("Meldung des Spielers").isEqualTo(200);
        String schluessel = warteAufFallInDerKonfliktliste();
        assertThat(ergebnisImBlatt()).as("vor der Entscheidung unverändert").containsExactly(null, null);

        mapping.konfliktListe().setzeEntscheidung(schluessel, Entscheidung.ERGEBNIS_UEBERNEHMEN.anzeige());
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());

        warteBis("Ergebnis aus Sicht des Teams eingetragen", () -> ergebnisImBlatt()
                .equals(kontoIstTeamA ? List.of(13, 7) : List.of(7, 13)));
        warteBis("Meldung online erledigt", () -> online.offeneErgebnismeldungen(turnierId).isEmpty());
        warteBis("Fall aus der Konfliktliste entfernt", () -> mapping.konfliktListe().leseFaelle().keySet().stream()
                .noneMatch(key -> key.startsWith(PtmOnlineErgebnismeldungen.SCHLUESSEL_PRAEFIX)));
    }

    @Test
    void verworfeneMeldungLaesstDieSpielrundeUnveraendertUndDasTeamDarfNeuMelden() throws Exception {
        rundeMitMeldungEinrichten(true);
        online.ergebnisMelden(idMitKonto, SPIELERKONTO, 13, 7);
        String schluessel = warteAufFallInDerKonfliktliste();

        mapping.konfliktListe().setzeEntscheidung(schluessel, Entscheidung.ERGEBNIS_VERWERFEN.anzeige());
        // Wie „Abgleichen“ im Menü: auch dort wirken die Entscheidungen der Konfliktliste.
        abgleichen();

        assertThat(online.offeneErgebnismeldungen(turnierId)).as("online verworfen").isEmpty();
        assertThat(online.live(idMitKonto, SPIELERKONTO).getAsJsonObject("registration")
                .getAsJsonObject("resultReport").get("status").getAsString()).isEqualTo("dismissed");
        assertThat(ergebnisImBlatt()).as("Spielrunde unverändert").containsExactly(null, null);
        assertThat(online.ergebnisMelden(idMitKonto, SPIELERKONTO, 13, 9)).as("erneute Meldung").isEqualTo(200);
    }

    @Test
    void ohneEingeschalteteErgebnismeldungNimmtPtmOnlineNichtsAn() throws Exception {
        rundeMitMeldungEinrichten(false);

        assertThat(online.ergebnisMelden(idMitKonto, SPIELERKONTO, 13, 7)).isEqualTo(403);
        assertThat(online.offeneErgebnismeldungen(turnierId)).isEmpty();
    }

    /** Schweizer Triplette mit sechs Teams, verbunden, eingecheckt, erste Runde ausgelost und online. */
    private void rundeMitMeldungEinrichten(boolean meldungErlaubt) throws Exception {
        JsonObject einstellungen = new JsonObject();
        einstellungen.addProperty("playerResultReportingEnabled", meldungErlaubt);
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT, einstellungen);
        idMitKonto = online.anmelden(turnierId, TEAM_MIT_KONTO);
        Map<String, Person[]> weitere = vollstaendigeTeams(5);
        verbinden();
        abgleichen();
        einchecken(zeile(TEAM_MIT_KONTO[0]));
        for (Person[] team : weitere.values()) {
            einchecken(zeile(team[0]));
        }
        schweizerRundeAuslosen();
        warteBis("Runde 1 online", () -> !online.runden(turnierId).isEmpty());
        int nrMitKonto = ziel.getTeamNrAusZeile(zeile(TEAM_MIT_KONTO[0]));
        int[] paar = paareImBlatt().stream().filter(p -> p[1] == nrMitKonto || p[2] == nrMitKonto).findFirst()
                .orElseThrow();
        zeileMitKonto = paar[0];
        kontoIstTeamA = paar[1] == nrMitKonto;
    }

    /**
     * Die Meldung kommt nach dem letzten Schreiben des Dokuments; im Betrieb holt PTM sie spätestens nach
     * {@link PtmOnlineLiveBeobachter#ERGEBNISMELDUNGEN_ABRUF_MS} ab. Der Test stößt den Abruf sofort an.
     */
    private String warteAufFallInDerKonfliktliste() throws Exception {
        PtmOnlineLiveBeobachter.ergebnismeldungenAbholen(wkingSpreadsheet.getWorkingSpreadsheetDocument());
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

    /** Punkte der Partie des Teams mit Konto, wie sie in der Spielrunde stehen ({@code null} = leer). */
    private List<Integer> ergebnisImBlatt() throws Exception {
        RowData zeile = RangeHelper.from(letzteRunde.getXSpreadSheet(),
                wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                RangePosition.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeileMitKonto,
                        SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, zeileMitKonto))
                .getDataFromRange().getFirst();
        List<Integer> punkte = new ArrayList<>();
        for (int spalte = 0; spalte < 2; spalte++) {
            String text = zeile.get(spalte).getStringVal();
            punkte.add(text == null || text.isBlank() ? null : zeile.get(spalte).getIntVal(0));
        }
        return punkte;
    }
}
