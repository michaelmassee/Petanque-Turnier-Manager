/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;

/**
 * Regression: Der Online-Turnierstart entstand vor den Vorprüfungen der ersten Runde. Scheiterte die Runde (zu wenige
 * Teams), blieb der Startauftrag trotzdem stehen – der Hintergrund setzte das Online-Turnier auf „laufend“ und beendete
 * die Anmeldung.
 * <p>
 * Der Sync ist pausiert, damit kein Netzzugriff (Vorabcheck) nötig ist; die Rückfrage „Start trotz Pause senden“
 * beantwortet der headless Test mit Ja.
 */
class PtmOnlineTurnierstartUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "turnierstart-test";

    private PtmOnlineRegistrationMapping mapping;

    @AfterEach
    void aufraeumen() {
        PtmOnlineLiveSync.vergessen(TURNIER_ID);
        PtmOnlineSpieleinstellungen.vergessen(TURNIER_ID);
        PtmOnlineAuftraege.vergessen(doc);
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
    }

    @Test
    void gescheiterteErsteRundeErzeugtKeinenStart() throws Exception {
        verbindeMitTeams(5);

        new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet).doRun();

        assertThat(SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.schluesselSchweizerSpielrunde(1)))
                .as("5 Teams reichen nicht für eine Schweizer Runde").isEmpty();
        assertThat(mapping.getRunningAusstehendSeit()).as("kein ausstehender Start").isEmpty();
        assertThat(mapping.leseAuftragsBestand().eintraege()).extracting(eintrag -> eintrag.auftrag().art())
                .doesNotContain(AuftragsArt.START);
    }

    @Test
    void erstellteErsteRundeErzeugtDenStart() throws Exception {
        verbindeMitTeams(6);

        new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet).doRun();

        assertThat(SheetMetadataHelper.findeSheet(doc, SheetMetadataHelper.schluesselSchweizerSpielrunde(1)))
                .isPresent();
        assertThat(mapping.getRunningAusstehendSeit()).isPresent();
        assertThat(mapping.leseAuftragsBestand().eintraege()).extracting(eintrag -> eintrag.auftrag().art())
                .contains(AuftragsArt.START);
    }

    private void verbindeMitTeams(int anzTeams) throws Exception {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "http://127.0.0.1", true));
        new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, anzTeams).doRun();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Turnierstart";
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        mapping.verbinden(turnier, new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                "01234567890123456789012345678901");
        mapping.setPausiert(true);
        assertThat(PtmOnlineVerbindung.ermitteln(wkingSpreadsheet, TurnierSystem.SCHWEIZER))
                .as("Vorbedingung: Dokument gilt als mit PTM-Online verbunden").isPresent();
    }
}
