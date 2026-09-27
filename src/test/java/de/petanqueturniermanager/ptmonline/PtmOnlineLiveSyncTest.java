/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void uebertraegtAlleRundenLoeschtUeberzaehligeUndDannDieRangliste() throws Exception {
        TournamentSyncClient client = mock(TournamentSyncClient.class);
        when(client.deleteRound("t1", 3)).thenReturn(true);
        when(client.deleteRound("t1", 4)).thenReturn(false);
        LiveTurnierStand stand = new LiveTurnierStand(
                List.of(runde(1), runde(2)), List.of(LiveRanglistenEintrag.team(1, 1, 2, 26, 10)));

        PtmOnlineLiveSync.uebertragen(client, "t1", stand, ONLINE_IDS);

        InOrder reihenfolge = inOrder(client);
        reihenfolge.verify(client).putRound("t1", 1,
                List.of(new LiveMatchDto(List.of("r1"), List.of("r2"), 13, 5, null, null)));
        reihenfolge.verify(client).putRound(eq("t1"), eq(2), anyList());
        reihenfolge.verify(client).deleteRound("t1", 3);
        reihenfolge.verify(client).deleteRound("t1", 4);
        reihenfolge.verify(client).putRanking("t1", List.of(new LiveRankingEntryDto(1, List.of("r1"), 2, 26, 10)));
    }

    @Test
    void ohneRanglisteBleibtDerOnlineSnapshotUnveraendert() throws Exception {
        TournamentSyncClient client = mock(TournamentSyncClient.class);

        PtmOnlineLiveSync.uebertragen(client, "t1", LiveTurnierStand.ohneRangliste(List.of(runde(1))), ONLINE_IDS);

        verify(client).deleteRound("t1", 2);
        verify(client, never()).putRanking(anyString(), anyList());
    }

    @Test
    void ohneRundenWerdenAlleOnlineRundenAbRundeEinsGeloescht() throws Exception {
        TournamentSyncClient client = mock(TournamentSyncClient.class);

        PtmOnlineLiveSync.uebertragen(client, "t1", LiveTurnierStand.ohneRangliste(List.of()), ONLINE_IDS);

        verify(client, never()).putRound(anyString(), anyInt(), anyList());
        verify(client).deleteRound("t1", 1);
    }

    private static LiveRunde runde(int nr) {
        return new LiveRunde(nr, List.of(LivePartie.teams(1, 2, 13, 5, null, null)));
    }
}
