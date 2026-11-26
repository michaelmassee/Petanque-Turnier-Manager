/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;

/**
 * „Neue Meldeliste“ und das Erzeugen von Testdaten trennen in einem verbundenen Dokument die Verbindung vorher. Ohne Zugangsdaten ist PTM-Online nicht
 * erreichbar; die übersprungenen Rückfragen bestätigen dann das nur lokale Trennen – der Test erreicht nie einen
 * Server.
 */
class PtmOnlineNeueMeldelisteUITest extends BaseCalcUITest {

    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";
    private static final String LOKALE_UUID = "3b0b8a3e-6c52-4f2d-9d57-1c2c7c1d9e10";
    private static final int ANZ_TESTDATEN_TEAMS = 4;

    @BeforeEach
    void ohneServerUndOhneRueckfragen() {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(new LibreOfficePtmOnlineSpeicher.Zugangsdaten("", "", false));
        MessageBox.setDialogeUeberspringen(true);
    }

    @AfterEach
    void zuruecksetzen() {
        MessageBox.setDialogeUeberspringen(false);
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
    }

    @Test
    void ohneVerbindungGehtEsDirektWeiter() throws Exception {
        schweizerTurnier();

        assertThat(PtmOnlineNeueMeldeliste.vorbereiten(wkingSpreadsheet, TurnierSystem.SCHWEIZER, Formation.TRIPLETTE))
                .isTrue();
    }

    @Test
    void trenntDieVerbindungVorDerNeuenMeldeliste() throws Exception {
        schweizerTurnier();
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null);

        assertThat(PtmOnlineNeueMeldeliste.vorbereiten(wkingSpreadsheet, TurnierSystem.SCHWEIZER, Formation.DOUBLETTE))
                .isTrue();

        assertThat(mapping.getTournamentId()).isEmpty();
        assertThat(PtmOnlineNeueMeldeliste.verbindungen(wkingSpreadsheet)).isEmpty();
    }

    @Test
    void trenntAuchBeimWechselDesTurniersystems() throws Exception {
        schweizerTurnier();
        verbinde(TurnierSystem.SCHWEIZER, null);

        assertThat(PtmOnlineNeueMeldeliste.vorbereiten(wkingSpreadsheet, TurnierSystem.KO, Formation.TRIPLETTE))
                .isTrue();

        assertThat(PtmOnlineNeueMeldeliste.verbindungen(wkingSpreadsheet)).isEmpty();
    }

    @Test
    void trenntBeiSupermeleeJedenVerbundenenSpieltag() throws Exception {
        new SuperMeleeKonfigurationSheet(wkingSpreadsheet).update();
        new MeldeListeSheet_New(wkingSpreadsheet).createMeldelisteWithParams(SuperMeleeMode.Triplette);
        verbinde(TurnierSystem.SUPERMELEE, 1);
        verbinde(TurnierSystem.SUPERMELEE, 3);

        assertThat(PtmOnlineNeueMeldeliste.verbindungen(wkingSpreadsheet))
                .extracting(PtmOnlineNeueMeldeliste.Verbindung::spieltagNr).containsExactlyInAnyOrder(1, 3);

        assertThat(PtmOnlineNeueMeldeliste.vorbereiten(wkingSpreadsheet, TurnierSystem.SUPERMELEE,
                Formation.TRIPLETTE)).isTrue();

        assertThat(PtmOnlineNeueMeldeliste.verbindungen(wkingSpreadsheet)).isEmpty();
    }

    @Test
    void gesperrteFormationBrichtVorDemTrennenAb() throws Exception {
        schweizerTurnier();
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null);
        mapping.addMapping(LOKALE_UUID, "r1", 1, "Hans Müller", "Bestätigt");

        assertThatThrownBy(() -> PtmOnlineNeueMeldeliste.vorbereiten(wkingSpreadsheet, TurnierSystem.SCHWEIZER,
                Formation.TRIPLETTE)).isInstanceOf(GenerateException.class);

        assertThat(mapping.getTournamentId()).as("Verbindung bleibt bestehen").contains("t1");
    }

    @Test
    void testdatenTrennenDieVerbindungVorDemErsetzenDerBlaetter() throws Exception {
        schweizerTurnier();
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null);

        new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TESTDATEN_TEAMS).doRun();

        assertThat(mapping.getTournamentId()).as("keine Testnamen als Online-Anmeldungen").isEmpty();
        assertThat(PtmOnlineNeueMeldeliste.verbindungen(wkingSpreadsheet)).isEmpty();
    }

    @Test
    void testdatenTrennenAuchBeiGesperrterFormation() throws Exception {
        schweizerTurnier();
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null);
        mapping.addMapping(LOKALE_UUID, "r1", 1, "Hans Müller", "Bestätigt");

        assertThat(PtmOnlineNeueMeldeliste.vorbereitenFuerTestdaten(wkingSpreadsheet)).isTrue();

        assertThat(mapping.getTournamentId()).isEmpty();
    }

    @Test
    void trennenLeertAuchDenGeladenenAuftragsbestand() throws Exception {
        schweizerTurnier();
        PtmOnlineRegistrationMapping mapping = verbinde(TurnierSystem.SCHWEIZER, null);
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(wkingSpreadsheet, mapping, null);
        bestand.erzeuge(AuftragsArt.RUNDE, "PUT", "/test", "{}", "{}");
        PtmOnlineAuftraege.speichern(bestand, mapping);

        mapping.trennen();

        assertThat(PtmOnlineAuftraege.bestand(wkingSpreadsheet, mapping, null).eintraege())
                .as("Bestand im Speicher wie die Dokumenteigenschaften leer").isEmpty();
        verbinde(TurnierSystem.SCHWEIZER, null);
        PtmOnlineAuftraege.neueBindung(wkingSpreadsheet, mapping, null, 0, true, "neue Bindung");
        assertThat(mapping.leseAuftragsBestand().eintraege())
                .as("kein Protokoll der alten Verbindung im Dokument").isEmpty();
    }

    private void schweizerTurnier() throws GenerateException {
        new SchweizerKonfigurationSheet(wkingSpreadsheet).update();
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.DOUBLETTE, false, false);
    }

    private PtmOnlineRegistrationMapping verbinde(TurnierSystem system, Integer spieltagNr) throws GenerateException {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, system, spieltagNr);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = "t1";
        turnier.name = "Testturnier";
        mapping.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);
        return mapping;
    }
}
