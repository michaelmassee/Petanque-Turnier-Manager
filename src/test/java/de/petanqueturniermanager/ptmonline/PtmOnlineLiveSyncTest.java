/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.live.LivePartie;
import de.petanqueturniermanager.ptmonline.live.LiveRanglistenEintrag;
import de.petanqueturniermanager.ptmonline.live.LiveRunde;
import de.petanqueturniermanager.ptmonline.live.LiveTurnierStand;

class PtmOnlineLiveSyncTest {

    private static final Map<Integer, List<String>> ONLINE_IDS = Map.of(1, List.of("r1"), 2, List.of("r2"));
    private static final List<LiveRanglistenEintrag> RANGLISTE = List.of(LiveRanglistenEintrag.team(1, 1, 2, 26, 10));

    private final AuftragsBestand bestand = AuftragsBestand.leer();
    private final LiveUebertragungsGedaechtnis gedaechtnis = new LiveUebertragungsGedaechtnis();

    @Test
    void ersteErfassungSendetAlleRundenLoeschtUeberzaehligeUndDannDieRangliste() {
        erfasse(new LiveTurnierStand(List.of(runde(1, 13), runde(2, 5)), RANGLISTE), OptionalInt.of(4));

        assertThat(auftraege()).extracting(SyncAuftrag::art, SyncAuftrag::pfad).containsExactly(
                tuple(AuftragsArt.RUNDE, "/api/sync/tournaments/t1/rounds/1"),
                tuple(AuftragsArt.RUNDE, "/api/sync/tournaments/t1/rounds/2"),
                tuple(AuftragsArt.RUNDE_LOESCHEN, "/api/sync/tournaments/t1/rounds/3"),
                tuple(AuftragsArt.RUNDE_LOESCHEN, "/api/sync/tournaments/t1/rounds/4"),
                tuple(AuftragsArt.RANGLISTE, "/api/sync/tournaments/t1/ranking"));
        assertThat(auftraege().getFirst().body()).isEqualTo(
                "{\"matches\":[{\"teamA\":[\"r1\"],\"teamB\":[\"r2\"],\"scoreA\":13,\"scoreB\":5}]}");
    }

    @Test
    void ohneBekannteOnlineRundenWirdNichtsGeloescht() {
        erfasse(LiveTurnierStand.ohneRangliste(List.of(runde(1, 13))), OptionalInt.empty());

        assertThat(auftraege()).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.RUNDE);
    }

    @Test
    void unveraenderterStandErzeugtKeinenAuftrag() {
        LiveTurnierStand stand = new LiveTurnierStand(List.of(runde(1, 13)), RANGLISTE);
        erfasse(stand, OptionalInt.of(1));
        int vorher = auftraege().size();

        assertThat(erfasse(stand, OptionalInt.of(1))).isZero();
        assertThat(auftraege()).hasSize(vorher);
    }

    @Test
    void nurDieGeaenderteRundeWirdErfasstUndErsetztDenUngesendetenStand() {
        erfasse(new LiveTurnierStand(List.of(runde(1, 13), runde(2, 5)), RANGLISTE), OptionalInt.of(2));

        erfasse(new LiveTurnierStand(List.of(runde(1, 13), runde(2, 13)), RANGLISTE), OptionalInt.of(2));

        assertThat(auftraege()).filteredOn(auftrag -> auftrag.pfad().endsWith("/rounds/2")).singleElement()
                .satisfies(auftrag -> assertThat(auftrag.body()).contains("\"scoreA\":13,\"scoreB\":5")
                        .doesNotContain("\"scoreA\":5"));
    }

    @Test
    void wenigerRundenAlsZuletztErfasstLoeschtOnline() {
        erfasse(LiveTurnierStand.ohneRangliste(List.of(runde(1, 13), runde(2, 5))), OptionalInt.of(2));

        erfasse(LiveTurnierStand.ohneRangliste(List.of(runde(1, 13))), OptionalInt.empty());

        assertThat(auftraege()).filteredOn(auftrag -> auftrag.pfad().endsWith("/rounds/2")).singleElement()
                .extracting(SyncAuftrag::art).isEqualTo(AuftragsArt.RUNDE_LOESCHEN);
    }

    @Test
    void ohneRundenWerdenAlleOnlineRundenGeloescht() {
        erfasse(LiveTurnierStand.ohneRangliste(List.of()), OptionalInt.of(2));

        assertThat(auftraege()).extracting(SyncAuftrag::art)
                .containsExactly(AuftragsArt.RUNDE_LOESCHEN, AuftragsArt.RUNDE_LOESCHEN);
    }

    private int erfasse(LiveTurnierStand stand, OptionalInt rundenOnline) {
        return PtmOnlineLiveSync.erfasse(bestand, "t1", stand, ONLINE_IDS, rundenOnline, gedaechtnis);
    }

    private List<SyncAuftrag> auftraege() {
        return bestand.zuSenden(false);
    }

    private static LiveRunde runde(int nr, int punkteA) {
        return new LiveRunde(nr, List.of(LivePartie.teams(1, 2, punkteA, 5, null, null)));
    }
}
