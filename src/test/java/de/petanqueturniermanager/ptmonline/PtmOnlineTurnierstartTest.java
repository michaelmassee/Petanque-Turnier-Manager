/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

/**
 * Ein verloren gegangener Turnierstart wird nachgeholt. Ob das Dokument übernehmen darf, entscheidet PTM-Online: ein
 * online mit eigenen Runden durchgeführtes Turnier lehnt der Start mit 409 ab.
 */
class PtmOnlineTurnierstartTest {

    private static final String TURNIER_ID = "t1";

    private final TournamentSyncClient client = mock(TournamentSyncClient.class);

    @Test
    void startWirdNachgeholt() throws Exception {
        assertThat(PtmOnlineTurnierstart.nachholen(client, TURNIER_ID)).isTrue();
        verify(client).start(TURNIER_ID);
    }

    @Test
    void onlineDurchgefuehrtesTurnierWirdNichtUebernommen() throws Exception {
        doThrow(new PtmOnlineHttpException(409,
                "{\"error\":\"Dieses Turnier wird online durchgeführt. Runden werden nicht aus dem Turnierdokument übernommen.\"}"))
                .when(client).start(TURNIER_ID);

        assertThat(PtmOnlineTurnierstart.nachholen(client, TURNIER_ID)).isFalse();
    }

    @Test
    void andereFehlerWerdenWeitergereicht() throws Exception {
        PtmOnlineHttpException fehler = new PtmOnlineHttpException(500, "{\"error\":\"x\"}");
        doThrow(fehler).when(client).start(TURNIER_ID);

        assertThatThrownBy(() -> PtmOnlineTurnierstart.nachholen(client, TURNIER_ID)).isSameAs(fehler);
    }
}
