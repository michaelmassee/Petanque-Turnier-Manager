/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;

/**
 * Regression: Ein in PTM-Online ausdrücklich zurückgesetzter Turnierstart (E-23) wurde beim ersten Live-Stand
 * (Öffnen, Abgleich) wieder nachgeholt, sobald lokal eine Runde existierte – das Turnier lief online erneut. Auch
 * über einen nur per Huckepack nachgeführten Status (ohne {@code runningResetAt}). Umgekehrt nahm ein ausdrücklicher
 * Start der zweiten Runde das zurückgesetzte Turnier nicht wieder auf; die Rundenübertragung scheiterte dann mit 409.
 */
class PtmOnlineStartZurueckgesetztUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "start-zurueckgesetzt-test";

    private PtmOnlineRegistrationMapping mapping;

    @BeforeEach
    void vorbereiten() throws Exception {
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        // Runde 1 mit Ergebnissen ohne Verbindung erstellt (Testlauf), danach verbunden.
        new SchweizerTurnierTestDaten(wkingSpreadsheet, 6, TeamAnzeige.NR).generate(1, false);
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
        // Verwirft mit den Auftragspuffern auch Live-Stand und Spieleinstellungen, die daran hängen.
        PtmOnlineAuftraege.vergessen(doc);
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
    }

    @Test
    void zurueckgesetzterStartWirdNichtNachgeholt() throws Exception {
        erfasseMitOnlineStand("2026-11-16T10:00:00Z");

        assertThat(auftragsArten()).doesNotContain(AuftragsArt.START);
    }

    @Test
    void nichtLaufendesTurnierOhneZuruecksetzenWirdGestartet() throws Exception {
        erfasseMitOnlineStand(null);

        assertThat(auftragsArten()).contains(AuftragsArt.START);
    }

    @Test
    void perHuckepackGemeldeterRuecksetzungsstatusErzwingtVollabruf() throws Exception {
        var laufend = new PtmOnlineErfassungsRunner.OnlineStandAbruf(TURNIER_ID,
                Optional.of(new SyncStandDto("running", true, null, 1, 0, null, false)));
        var nachHuckepack = laufend.mitGemeldetemStatus("registration").orElseThrow();

        var erfassung = erfasse(nachHuckepack);

        assertThat(erfassung.brauchtStand()).as("vollständiger Online-Stand wird zuerst abgerufen").isTrue();
        assertThat(auftragsArten()).doesNotContain(AuftragsArt.START);
    }

    @Test
    void zweiteRundeNimmtZurueckgesetztesTurnierWiederAuf() throws Exception {
        mapping.setOnlineStatus("registration");

        new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet).doRun();

        assertThat(SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.schluesselSchweizerSpielrunde(2)))
                .isPresent();
        assertThat(mapping.getRunningAusstehendSeit()).isPresent();
        assertThat(auftragsArten()).contains(AuftragsArt.START);
    }

    @Test
    void zweiteRundeBeiLaufendemTurnierSendetKeinenStart() throws Exception {
        mapping.setOnlineStatus("running");

        new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet).doRun();

        assertThat(SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.schluesselSchweizerSpielrunde(2)))
                .isPresent();
        assertThat(mapping.getRunningAusstehendSeit()).isEmpty();
        assertThat(auftragsArten()).doesNotContain(AuftragsArt.START);
    }

    private void erfasseMitOnlineStand(@Nullable String runningResetAt) {
        erfasse(new PtmOnlineErfassungsRunner.OnlineStandAbruf(TURNIER_ID,
                Optional.of(new SyncStandDto("registration", true, runningResetAt, 1, 0, null, false))));
    }

    private PtmOnlineErfassungsRunner.Erfassung erfasse(PtmOnlineErfassungsRunner.OnlineStandAbruf abruf) {
        var runner = new PtmOnlineErfassungsRunner(wkingSpreadsheet, TurnierSystem.SCHWEIZER, new PtmOnlineCheckin(),
                new PtmOnlineMeldelistenSync(),
                abruf, null);
        runner.run();
        assertThat(runner.isLetzterLaufFehlgeschlagen()).isFalse();
        assertThat(runner.erfassung()).as("Dokument gilt als verbunden").isPresent();
        return runner.erfassung().orElseThrow();
    }

    private List<AuftragsArt> auftragsArten() throws GenerateException {
        return mapping.leseAuftragsBestand().eintraege().stream().map(eintrag -> eintrag.auftrag().art()).toList();
    }
}
