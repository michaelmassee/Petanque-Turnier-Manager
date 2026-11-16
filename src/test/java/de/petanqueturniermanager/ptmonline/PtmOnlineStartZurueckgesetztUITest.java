/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;

/**
 * Regression: Ein in PTM-Online ausdrücklich zurückgesetzter Turnierstart (E-23) wurde beim ersten Live-Stand
 * (Öffnen, Abgleich) wieder nachgeholt, sobald lokal eine Runde existierte – das Turnier lief online erneut.
 */
class PtmOnlineStartZurueckgesetztUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "start-zurueckgesetzt-test";

    private PtmOnlineRegistrationMapping mapping;

    @BeforeEach
    void vorbereiten() throws Exception {
        new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, 6).doRun();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        // Runde ohne Verbindung erstellt (Testlauf), danach verbunden.
        new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet).doRun();
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "http://127.0.0.1", true));
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Start zurückgesetzt";
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        mapping.verbinden(turnier, new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                "01234567890123456789012345678901");
        mapping.setPausiert(false);
    }

    @AfterEach
    void aufraeumen() {
        PtmOnlineLiveSync.vergessen(TURNIER_ID);
        PtmOnlineSpieleinstellungen.vergessen(TURNIER_ID);
        PtmOnlineAuftraege.vergessen(doc);
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
    }

    @Test
    void zurueckgesetzterStartWirdNichtNachgeholt() throws Exception {
        erfasseMitOnlineStand("2026-11-16T10:00:00Z");

        assertThat(mapping.leseAuftragsBestand().eintraege()).extracting(eintrag -> eintrag.auftrag().art())
                .doesNotContain(AuftragsArt.START);
    }

    @Test
    void nichtLaufendesTurnierOhneZuruecksetzenWirdGestartet() throws Exception {
        erfasseMitOnlineStand(null);

        assertThat(mapping.leseAuftragsBestand().eintraege()).extracting(eintrag -> eintrag.auftrag().art())
                .contains(AuftragsArt.START);
    }

    private void erfasseMitOnlineStand(@Nullable String runningResetAt) {
        var abruf = new PtmOnlineErfassungsRunner.OnlineStandAbruf(TURNIER_ID,
                Optional.of(new SyncStandDto("registration", true, runningResetAt, 1, 0, null, false)));
        var runner = new PtmOnlineErfassungsRunner(wkingSpreadsheet, TurnierSystem.SCHWEIZER, new PtmOnlineCheckin(),
                abruf, null);
        runner.run();
        assertThat(runner.isLetzterLaufFehlgeschlagen()).isFalse();
        assertThat(runner.erfassung()).as("Dokument gilt als verbunden").isPresent();
    }
}
