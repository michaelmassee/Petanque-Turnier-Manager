/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;

/**
 * Nachstellung des Falls „Neustart ohne Speichern“: Die Datei steht bei Zähler 23, PTM-Online hat 24–29 schon
 * angenommen. Nach der Übernahme gehen die offenen Aufträge oberhalb des Server-Zählers hinaus.
 */
class PtmOnlineDokumentstandTest {

    private static final String TURNIER = "t1";

    @Test
    void uebernimmtDenZaehlerVonPtmOnlineFuerOffeneAuftraege() throws Exception {
        AuftragsBestand bestand = AuftragsBestand.aus(23, List.of());
        SyncAuftrag teilnahme = bestand.erzeuge(AuftragsArt.TEILNAHME, "POST", "/api/x", "{}", "{}");
        SyncAuftrag runde = bestand.erzeuge(AuftragsArt.RUNDE, "PUT", "/api/r/1", "{}", "{}");
        TournamentSyncClient client = mock(TournamentSyncClient.class);
        when(client.fetchSyncStand(TURNIER))
                .thenReturn(new SyncStandDto("running", true, null, 1, 29, null, false));

        PtmOnlineDokumentstand.uebernehme(bestand, client, TURNIER);

        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::auftragsId)
                .containsExactly(teilnahme.auftragsId(), runde.auftragsId());
        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::zaehler).containsExactly(30L, 31L);
        assertThat(bestand.versandSperre().isLocked()).isFalse();
    }
}
