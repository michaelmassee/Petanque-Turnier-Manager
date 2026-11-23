/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_ERG_A;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_ERG_B;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_TEAM_A_NR;
import static de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet.SPALTE_TEAM_B_NR;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.onlinesync.TurnierSystemOnlineTypMapping;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.poule.vorrunde.PouleVorrundeSheet;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/**
 * Spieler-Ergebnismeldung in der Poule-Vorrunde gegen den lokal gestarteten Worker: Die Turnierleitung übernimmt die
 * Meldung, PTM trägt sie in die Ergebnis-Zellen der Partie in der Vorrunde ein.
 */
class PtmOnlinePouleErgebnismeldungZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    /** Acht Teams: zwei Vierer-Poules. */
    private static final int ANZ_WEITERE_TEAMS = 7;
    /** Zeilen, in denen die Vorrunde gesucht wird (zwei Vierer-Poules à fünf Zeilen, mit Reserve). */
    private static final int ZEILEN_VORRUNDE = 20;
    private static final int SPIELERKONTO = 12;
    private static final Person[] TEAM_MIT_KONTO = { Person.konto(SPIELERKONTO),
            Person.gast("Paul", "Poule"), Person.gast("Pia", "Poule") };

    @Test
    void uebernommeneMeldungStehtInDerPouleVorrunde() throws Exception {
        pouleAnlegen();
        String idMitKonto = online.anmelden(turnierId, TEAM_MIT_KONTO);
        vollstaendigeTeams(ANZ_WEITERE_TEAMS);
        verbinden();
        abgleichen();
        for (int zeile : lokaleMeldungen().keySet()) {
            ziel.stelleAktivWertWiederHer(zeile, 1);
        }
        int nrMitKonto = ziel.getTeamNrAusZeile(zeile(TEAM_MIT_KONTO[0]));

        PouleVorrundeSheet vorrunde = new PouleVorrundeSheet(wkingSpreadsheet);
        starteUndWarte(vorrunde);
        assertThat(vorrunde.isLetzterLaufFehlgeschlagen()).as("Vorrunde ausgelost").isFalse();
        warteBis("erste Poule-Runde online", () -> !online.runden(turnierId).isEmpty());
        int zeile = offenePartieVon(nrMitKonto);
        boolean kontoIstTeamA = teamNr(SPALTE_TEAM_A_NR, zeile) == nrMitKonto;

        assertThat(online.ergebnisMelden(idMitKonto, SPIELERKONTO, 13, 4)).as("Meldung im ersten Poule-Spiel")
                .isEqualTo(HttpURLConnection.HTTP_OK);
        PtmOnlineLiveBeobachter.ergebnismeldungenAbholen(wkingSpreadsheet.getWorkingSpreadsheetDocument());
        List<String> faelle = new ArrayList<>();
        warteBis("Meldung in der Konfliktliste", () -> {
            faelle.clear();
            mapping.konfliktListe().leseFaelle().keySet().stream()
                    .filter(key -> key.startsWith(PtmOnlineErgebnismeldungen.SCHLUESSEL_PRAEFIX))
                    .forEach(faelle::add);
            return faelle.size() == 1;
        });
        mapping.konfliktListe().setzeEntscheidung(faelle.getFirst(), Entscheidung.ERGEBNIS_UEBERNEHMEN.anzeige());
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());

        warteBis("gemeldetes Ergebnis in der Poule-Vorrunde eingetragen",
                () -> ergebnis(zeile).equals(kontoIstTeamA ? List.of(13, 4) : List.of(4, 13)));
        warteBis("Meldung online erledigt", () -> online.offeneErgebnismeldungen(turnierId).isEmpty());
    }

    /** Poule (Triplette) mit eingeschalteter Ergebnismeldung durch Spieler. */
    private void pouleAnlegen() throws Exception {
        system = TurnierSystem.POULE;
        spieltagNr = null;
        anmeldeTyp = "forme";
        onlineTyp = TurnierSystemOnlineTypMapping.onlineTyp(system).orElseThrow();
        wieImSheetRunner(() -> new PouleMeldeListeSheetNew(wkingSpreadsheet)
                .createMeldelisteWithParams(Formation.TRIPLETTE, false, false));
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, system.getId());
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        JsonObject einstellungen = new JsonObject();
        einstellungen.addProperty("playerResultReportingEnabled", true);
        turnierId = online.turnierAnlegen("E2E " + getClass().getSimpleName(), onlineTyp, anmeldeTyp, "triplette",
                IN_EINEM_MONAT, einstellungen);
        dokumentSchalterWieOnline(einstellungen);
    }

    /** Zeile der Vorrunde mit der Partie des Teams, die noch kein Ergebnis hat. */
    private int offenePartieVon(int teamNr) {
        for (int zeile = ERSTE_DATEN_ZEILE; zeile < ERSTE_DATEN_ZEILE + ZEILEN_VORRUNDE; zeile++) {
            boolean dabei = teamNr(SPALTE_TEAM_A_NR, zeile) == teamNr || teamNr(SPALTE_TEAM_B_NR, zeile) == teamNr;
            if (dabei && ergebnis(zeile).equals(Arrays.asList(null, null))) {
                return zeile;
            }
        }
        throw new AssertionError("Keine offene Poule-Partie von Team " + teamNr);
    }

    private int teamNr(int spalte, int zeile) {
        return sheetHlp.getIntFromCell(vorrunde(), Position.from(spalte, zeile));
    }

    /** Punkte der Partie ({@code null} = leer). */
    private List<Integer> ergebnis(int zeile) {
        List<Integer> punkte = new ArrayList<>();
        for (int spalte : new int[] { SPALTE_ERG_A, SPALTE_ERG_B }) {
            String text = sheetHlp.getTextFromCell(vorrunde(), Position.from(spalte, zeile));
            punkte.add(text == null || text.isBlank() ? null : sheetHlp.getIntFromCell(vorrunde(),
                    Position.from(spalte, zeile)));
        }
        return punkte;
    }

    private XSpreadsheet vorrunde() {
        return SheetMetadataHelper.findeSheet(wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE).orElseThrow();
    }
}
