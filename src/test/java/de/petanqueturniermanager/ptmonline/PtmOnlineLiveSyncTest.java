/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;
import de.petanqueturniermanager.ptmonline.live.LivePartie;
import de.petanqueturniermanager.ptmonline.live.LiveRanglistenEintrag;
import de.petanqueturniermanager.ptmonline.live.LiveRunde;
import de.petanqueturniermanager.ptmonline.live.LiveTurnierStand;

class PtmOnlineLiveSyncTest {

    private static final Map<Integer, List<String>> ONLINE_IDS = Map.of(1, List.of("r1"), 2, List.of("r2"));
    private static final List<LiveRanglistenEintrag> RANGLISTE = List.of(LiveRanglistenEintrag.team(1, 1, 2, 26, 10));

    private final TournamentSyncClient client = mock(TournamentSyncClient.class);
    private final LiveUebertragungsGedaechtnis gedaechtnis = new LiveUebertragungsGedaechtnis();

    @Test
    void ersteUebertragungSendetAlleRundenLoeschtUeberzaehligeUndDannDieRangliste() throws Exception {
        when(client.deleteRound("t1", 3)).thenReturn(true);

        uebertragen(new LiveTurnierStand(List.of(runde(1, 13), runde(2, 5)), RANGLISTE));

        InOrder reihenfolge = inOrder(client);
        reihenfolge.verify(client).putRound("t1", 1,
                List.of(new LiveMatchDto(List.of("r1"), List.of("r2"), 13, 5, null, null)));
        reihenfolge.verify(client).putRound(eq("t1"), eq(2), anyList());
        reihenfolge.verify(client).deleteRound("t1", 3);
        reihenfolge.verify(client).deleteRound("t1", 4);
        reihenfolge.verify(client).putRanking("t1", List.of(new LiveRankingEntryDto(1, List.of("r1"), 2, 26, 10)));
    }

    @Test
    void unveraenderterStandWirdNichtErneutGesendet() throws Exception {
        LiveTurnierStand stand = new LiveTurnierStand(List.of(runde(1, 13)), RANGLISTE);
        uebertragen(stand);
        clearInvocations(client);

        uebertragen(stand);

        verifyNoInteractions(client);
    }

    @Test
    void nurDieGeaenderteRundeWirdGesendet() throws Exception {
        uebertragen(new LiveTurnierStand(List.of(runde(1, 13), runde(2, 5)), RANGLISTE));
        clearInvocations(client);

        uebertragen(new LiveTurnierStand(List.of(runde(1, 13), runde(2, 13)), RANGLISTE));

        verify(client).putRound(eq("t1"), eq(2), anyList());
        verify(client, never()).putRound(eq("t1"), eq(1), anyList());
        verify(client, never()).deleteRound(anyString(), anyInt());
        verify(client, never()).putRanking(anyString(), anyList());
    }

    @Test
    void wenigerRundenAlsZuletztUebertragenLoeschtOnline() throws Exception {
        uebertragen(LiveTurnierStand.ohneRangliste(List.of(runde(1, 13), runde(2, 5))));
        clearInvocations(client);
        when(client.deleteRound("t1", 2)).thenReturn(true);

        uebertragen(LiveTurnierStand.ohneRangliste(List.of(runde(1, 13))));

        verify(client).deleteRound("t1", 2);
        verify(client).deleteRound("t1", 3);
        verify(client, never()).putRound(anyString(), anyInt(), anyList());
    }

    @Test
    void nachNetzfehlerWirdBeimNaechstenMalAllesGesendet() throws Exception {
        LiveTurnierStand stand = new LiveTurnierStand(List.of(runde(1, 13), runde(2, 5)), RANGLISTE);
        uebertragen(stand);
        when(client.putRound(eq("t1"), eq(2), anyList())).thenThrow(new IOException("Netz weg"));
        assertThatThrownBy(() -> uebertragen(new LiveTurnierStand(List.of(runde(1, 13), runde(2, 13)), RANGLISTE)))
                .isInstanceOf(IOException.class);
        clearInvocations(client);
        when(client.putRound(eq("t1"), eq(2), anyList())).thenReturn(1);

        uebertragen(stand);

        verify(client).putRound(eq("t1"), eq(1), anyList());
        verify(client).putRound(eq("t1"), eq(2), anyList());
        verify(client).deleteRound("t1", 3);
        verify(client).putRanking(eq("t1"), anyList());
    }

    @Test
    void ohneRanglisteBleibtDerOnlineSnapshotUnveraendert() throws Exception {
        uebertragen(LiveTurnierStand.ohneRangliste(List.of(runde(1, 13))));

        verify(client).deleteRound("t1", 2);
        verify(client, never()).putRanking(anyString(), anyList());
    }

    @Test
    void ohneRundenWerdenAlleOnlineRundenAbRundeEinsGeloescht() throws Exception {
        uebertragen(LiveTurnierStand.ohneRangliste(List.of()));

        verify(client, never()).putRound(anyString(), anyInt(), anyList());
        verify(client).deleteRound("t1", 1);
    }

    private void uebertragen(LiveTurnierStand stand) throws Exception {
        PtmOnlineLiveSync.uebertragen(client, "t1", stand, ONLINE_IDS, gedaechtnis);
    }

    private static LiveRunde runde(int nr, int punkteA) {
        return new LiveRunde(nr, List.of(LivePartie.teams(1, 2, punkteA, 5, null, null)));
    }
}
