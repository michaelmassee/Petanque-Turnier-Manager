/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncSheet;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncStatus;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;

/**
 * Supermelee: jeder Spieltag ist online ein eigenes Turnier. Ein neuer Spieltag trennt die Verbindung des
 * bisherigen Spieltags online und archiviert dessen Blatt „PTMOnline Sync“ – auch ohne Netz.
 */
class PtmOnlineSpieltagWechselUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "t1";
    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";

    private PtmOnlineTestServer server;
    private PtmOnlineRegistrationMapping spieltag1;

    @BeforeEach
    void spieltag1Verbinden() throws Exception {
        MessageBox.setDialogeUeberspringen(true);
        server = new PtmOnlineTestServer(TURNIER_ID, "{\"registrations\":[]}");
        new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc).initMitAlleDieSpielen(4);
        spieltag1 = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SUPERMELEE, 1);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Spieltag 1 online";
        spieltag1.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);
    }

    @AfterEach
    void aufraeumen() {
        server.close();
        MessageBox.setDialogeUeberspringen(false);
    }

    @Test
    void neuerSpieltagTrenntBisherigenOnlineUndArchiviertSeinBlatt() throws Exception {
        PtmOnlineSpieltagWechsel.trenneVorherigeSpieltage(wkingSpreadsheet, 2, server.zugangsdaten());

        assertThat(server.anzahlGetrennt()).as("online getrennt").isEqualTo(1);
        pruefeArchiviert();
    }

    @Test
    void ohneNetzWirdTrotzdemLokalArchiviert() throws Exception {
        LibreOfficePtmOnlineSpeicher.Zugangsdaten nichtErreichbar = server.zugangsdaten();
        server.close();

        PtmOnlineSpieltagWechsel.trenneVorherigeSpieltage(wkingSpreadsheet, 2, nichtErreichbar);

        pruefeArchiviert();
    }

    @Test
    void geloeschtesOnlineTurnierGiltBeimTrennenAlsBereitsGetrennt() {
        server.turnierLoeschen();

        assertThatCode(() -> PtmOnlineTrennung.online(wkingSpreadsheet, 1, server.zugangsdaten(), spieltag1))
                .as("gelöschtes Online-Turnier darf das lokale Trennen nicht blockieren").doesNotThrowAnyException();
        assertThat(server.anzahlGetrennt()).isZero();
    }

    @Test
    void erneutesVerbindenHebtDasArchivAuf() throws Exception {
        PtmOnlineSpieltagWechsel.trenneVorherigeSpieltage(wkingSpreadsheet, 2, server.zugangsdaten());
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Spieltag 1 online";

        spieltag1.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 2), LEASE_TOKEN);

        assertThat(spieltag1.getTournamentId()).contains(TURNIER_ID);
        assertThat(PtmOnlineSyncSheet.leseStatus(wkingSpreadsheet, 1).verbunden()).isTrue();
    }

    private void pruefeArchiviert() throws Exception {
        assertThat(spieltag1.getTournamentId()).as("archiviert gilt als nicht verbunden").isEmpty();
        assertThat(spieltag1.getSyncDocumentId()).as("Blatt bleibt als Archiv erhalten").contains(DOCUMENT_ID);
        PtmOnlineSyncStatus status = PtmOnlineSyncSheet.leseStatus(wkingSpreadsheet, 1);
        assertThat(status.verbunden()).isFalse();
        assertThat(spieltag1.istPausiert()).isFalse();
    }
}
