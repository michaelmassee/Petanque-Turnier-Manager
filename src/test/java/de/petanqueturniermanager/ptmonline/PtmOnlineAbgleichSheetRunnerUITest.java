/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * PTM-Online-Anbindung gegen einen lokalen Test-Server:
 * <ul>
 * <li>Der manuelle Abgleich läuft als SheetRunner, aktualisiert die Meldeliste im selben Runner (kein
 * zweiter Runner, keine „Verarbeitung läuft“-Kollision) und lässt sich während eines Serverabrufs
 * abbrechen.</li>
 * <li>Die Prüfung vor dem Turnierstart importiert nichts, meldet fehlende Online-Meldungen und verknüpft
 * vor Ort erfasste, namensgleiche Zeilen.</li>
 * </ul>
 */
class PtmOnlineAbgleichSheetRunnerUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "t1";
    private static final String ANMELDUNGEN = """
            {"registrations":[
              {"id":"r1","tournamentId":"t1","firstName":"Anna","lastName":"Schmidt","status":"confirmed"},
              {"id":"r2","tournamentId":"t1","firstName":"Hans","lastName":"Müller","status":"confirmed"},
              {"id":"r3","tournamentId":"t1","firstName":"Offen","lastName":"Noch","status":"pending"}
            ]}""";

    private PtmOnlineTestServer server;
    private MeldelisteZiel ziel;
    private PtmOnlineRegistrationMapping mapping;

    @BeforeEach
    void turnierUndServerAnlegen() throws Exception {
        server = new PtmOnlineTestServer(TURNIER_ID, ANMELDUNGEN);

        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TETE, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        ziel = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
        ziel.schreibeBlock(List.of(new SpielerMitVerein(0, "Hans", "Müller", null, null, List.of(), List.of(), null)));

        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Testturnier";
        turnier.type = "schweizer";
        mapping.verbinden(turnier);
        mapping.setSyncBinding(new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                "01234567890123456789012345678901");
    }

    @AfterEach
    void serverStoppen() {
        server.close();
        MessageBox.setDialogeUeberspringen(false);
    }

    @Test
    void abgleichUebernimmtUndVerknuepftImSelbenRunner() throws Exception {
        PtmOnlineAbgleichSheetRunner runner = neuerRunner();

        runner.start();
        runner.join();

        assertThat(runner.isLetzterLaufFehlgeschlagen()).isFalse();
        assertThat(mapping.istBereitsImportiert("r1")).as("neue Online-Anmeldung übernommen").isTrue();
        assertThat(mapping.istBereitsImportiert("r2")).as("mit vor Ort erfasster Zeile verknüpft").isTrue();
        assertThat(mapping.istBereitsImportiert("r3")).as("offene Anmeldung nicht übernommen").isFalse();
        assertThat(nachnamen()).containsExactlyInAnyOrder("Schmidt", "Müller");
        assertThat(ziel.getTeamNrAusZeile(ziel.findeZeileMitName("Anna Schmidt")))
                .as("Meldeliste im selben Runner aktualisiert: neue Zeile hat eine Nr").isPositive();
        assertThat(server.anzahlOnlineAngelegt()).as("alle lokalen Meldungen sind schon online").isZero();
    }

    @Test
    void abbruchWaehrendDesServerabrufsUebernimmtNichts() throws Exception {
        MessageBox.setDialogeUeberspringen(true);
        server.antwortenZurueckhalten();
        PtmOnlineAbgleichSheetRunner runner = neuerRunner();

        runner.start();
        assertThat(server.warteAufErstenAbruf()).isTrue();
        SheetRunner.cancelRunner();
        runner.join();

        assertThat(runner.isLetzterLaufFehlgeschlagen()).as("Abbruch beendet den Lauf").isTrue();
        assertThat(mapping.istBereitsImportiert("r1")).isFalse();
        assertThat(nachnamen()).containsExactly("Müller");
    }

    @Test
    void pruefungVorTurnierstartMeldetFehlendeUndImportiertNichts() throws Exception {
        List<String> fehlend = RegistrationImportTask.pruefeVorTurnierstart(server.zugangsdaten(), mapping,
                TURNIER_ID, ziel);

        assertThat(fehlend).as("nur bestätigte, noch nicht erfasste Meldungen").containsExactly("Anna Schmidt");
        assertThat(nachnamen()).as("nichts importiert").containsExactly("Müller");
        assertThat(mapping.istBereitsImportiert("r1")).isFalse();
        assertThat(mapping.istBereitsImportiert("r2")).as("vor Ort erfasste Zeile verknüpft").isTrue();
        assertThat(mapping.getLastSync()).as("späterer Abgleich ruft die fehlenden weiter ab").isEmpty();
    }

    private PtmOnlineAbgleichSheetRunner neuerRunner() {
        return new PtmOnlineAbgleichSheetRunner(wkingSpreadsheet, TurnierSystem.SCHWEIZER, server.zugangsdaten(),
                mapping, TURNIER_ID, ziel);
    }

    private List<String> nachnamen() {
        return ziel.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::nachname).toList();
    }
}
