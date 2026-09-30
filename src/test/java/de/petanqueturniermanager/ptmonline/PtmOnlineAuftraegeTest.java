/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;

class PtmOnlineAuftraegeTest {

    private static final String TURNIER = "t1";

    private static PtmOnlineStatusAuftrag.Eintrag aktiv(String uuid) {
        return new PtmOnlineStatusAuftrag.Eintrag(uuid, OnlineTeilnahme.AKTIV, null);
    }

    private static int erwarteteRevision(SyncAuftrag auftrag) {
        JsonObject body = JsonParser.parseString(auftrag.body()).getAsJsonObject();
        return body.getAsJsonArray("registrations").get(0).getAsJsonObject().get("expectedExecutionRevision").getAsInt();
    }

    @Test
    void teilnahmeUebergehtNichtZugeordneteMeldungen() {
        AuftragsBestand bestand = AuftragsBestand.leer();

        assertThat(PtmOnlineAuftraege.teilnahme(bestand, TURNIER, List.of(aktiv("u1")), Map.of(), Map.of()))
                .isEmpty();
        assertThat(bestand.hatOffene()).isFalse();
    }

    @Test
    void teilnahmeErwartetDieRevisionNachAllenOffenenAuftraegen() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        Map<String, String> onlineIds = Map.of("u1", "r1");

        SyncAuftrag erster = PtmOnlineAuftraege
                .teilnahme(bestand, TURNIER, List.of(aktiv("u1")), onlineIds, Map.of("u1", 3)).orElseThrow();
        SyncAuftrag zweiter = PtmOnlineAuftraege
                .teilnahme(bestand, TURNIER, List.of(aktiv("u1")), onlineIds, Map.of("u1", 3)).orElseThrow();

        assertThat(erwarteteRevision(erster)).isEqualTo(3);
        assertThat(erwarteteRevision(zweiter)).as("der offene erste Auftrag erhöht die Revision").isEqualTo(4);
    }

    @Test
    void startVerwirftUngesendeteAnlagenUndKommtZuletzt() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.teilnahme(bestand, TURNIER, List.of(aktiv("u1")), Map.of("u1", "r1"), Map.of());
        PtmOnlineAuftraege.runde(bestand, TURNIER, 1, List.of());

        List<SyncAuftrag> verworfen = PtmOnlineAuftraege.start(bestand, TURNIER, Instant.EPOCH, "Start");

        assertThat(verworfen).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.TEILNAHME);
        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.RUNDE,
                AuftragsArt.START);
    }

    @Test
    void neuerRundenstandUeberholtDenUngesendetenAlten() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.runde(bestand, TURNIER, 1, List.of());
        PtmOnlineAuftraege.runde(bestand, TURNIER, 2, List.of());
        PtmOnlineAuftraege.rundeLoeschen(bestand, TURNIER, 1);

        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.RUNDE,
                AuftragsArt.RUNDE_LOESCHEN);
    }

    @Test
    void trennenVerwirftAlleOffenenAuftraege() {
        AuftragsBestand bestand = AuftragsBestand.leer();
        PtmOnlineAuftraege.runde(bestand, TURNIER, 1, List.of());

        PtmOnlineAuftraege.trennen(bestand, TURNIER, "getrennt");

        assertThat(bestand.zuSenden(false)).extracting(SyncAuftrag::art).containsExactly(AuftragsArt.TRENNEN);
    }
}
