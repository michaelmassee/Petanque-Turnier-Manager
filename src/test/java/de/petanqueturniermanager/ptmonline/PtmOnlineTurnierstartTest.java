/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;

/**
 * Ein offline verloren gegangener Turnierstart wird nachgeholt – aber nur, solange das Online-Turnier noch nicht
 * läuft; ein bereits laufendes wird online durchgeführt und darf nicht vom Dokument übernommen werden.
 */
class PtmOnlineTurnierstartTest {

    private static final String TURNIER_ID = "t1";

    private final TournamentSyncClient client = mock(TournamentSyncClient.class);

    @Test
    void nichtGestartetesTurnierWirdGestartet() throws Exception {
        when(client.listTournaments()).thenReturn(List.of(turnier(TURNIER_ID, "registration")));

        assertThat(PtmOnlineTurnierstart.nachholen(client, TURNIER_ID)).isTrue();
        verify(client).start(TURNIER_ID);
    }

    @Test
    void onlineLaufendesTurnierWirdNichtUebernommen() throws Exception {
        when(client.listTournaments()).thenReturn(List.of(turnier(TURNIER_ID, "running")));

        assertThat(PtmOnlineTurnierstart.nachholen(client, TURNIER_ID)).isFalse();
        verify(client, never()).start(anyString());
    }

    @Test
    void unbekanntesTurnierWirdNichtGestartet() throws Exception {
        when(client.listTournaments()).thenReturn(List.of(turnier("anderes", "registration")));

        assertThat(PtmOnlineTurnierstart.nachholen(client, TURNIER_ID)).isFalse();
        verify(client, never()).start(anyString());
    }

    private static OnlineTournamentDto turnier(String id, String status) {
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = id;
        turnier.status = status;
        return turnier;
    }
}
