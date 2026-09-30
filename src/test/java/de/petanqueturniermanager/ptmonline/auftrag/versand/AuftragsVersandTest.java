/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag.versand;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;

class AuftragsVersandTest {

    private static SyncAuftrag auftrag(long zaehler) {
        return new SyncAuftrag("id-" + zaehler, zaehler, AuftragsArt.TEILNAHME, "POST", "/api/x", "{}", "{}");
    }

    private static SyncAntwort antwort(int status, String body) {
        return new SyncAntwort(status, body, false);
    }

    @Test
    void sendetInZaehlerReihenfolgeUndMeldetJedesErgebnis() throws Exception {
        List<Long> gesendet = new ArrayList<>();
        List<VersandErgebnis> ergebnisse = new ArrayList<>();

        VersandStopp stopp = AuftragsVersand.sende(List.of(auftrag(3), auftrag(1), auftrag(2)), auftrag -> {
            gesendet.add(auftrag.zaehler());
            return antwort(200, "{}");
        }, ergebnisse::add);

        assertThat(stopp).isEqualTo(VersandStopp.FERTIG);
        assertThat(gesendet).containsExactly(1L, 2L, 3L);
        assertThat(ergebnisse).allMatch(VersandErgebnis::angenommen);
    }

    @Test
    void faehrtNachFachlicherAblehnungFort() throws Exception {
        List<VersandErgebnis> ergebnisse = new ArrayList<>();

        VersandStopp stopp = AuftragsVersand.sende(List.of(auftrag(1), auftrag(2)),
                auftrag -> auftrag.zaehler() == 1
                        ? antwort(409, "{\"error\":\"x\",\"details\":{\"code\":\"tournament_running\"}}")
                        : antwort(200, "{}"),
                ergebnisse::add);

        assertThat(stopp).isEqualTo(VersandStopp.FERTIG);
        assertThat(ergebnisse).extracting(VersandErgebnis::angenommen).containsExactly(false, true);
    }

    @Test
    void haeltBeiKopieDesDokumentsAnOhneDenAuftragZuVerbrauchen() throws Exception {
        List<VersandErgebnis> ergebnisse = new ArrayList<>();

        VersandStopp stopp = AuftragsVersand.sende(List.of(auftrag(1), auftrag(2)),
                auftrag -> antwort(409, "{\"details\":{\"code\":\"document_forked\"}}"), ergebnisse::add);

        assertThat(stopp).isEqualTo(VersandStopp.DOKUMENT_GEFORKT);
        assertThat(ergebnisse).isEmpty();
    }

    @Test
    void haeltBeiNetzfehlerAnUndWiederholtSpaeter() throws Exception {
        VersandStopp stopp = AuftragsVersand.sende(List.of(auftrag(1)), auftrag -> {
            throw new IOException("offline");
        }, ergebnis -> {});

        assertThat(stopp).isEqualTo(VersandStopp.NETZ);
        assertThat(stopp.spaeterErneut()).isTrue();
    }

    @Test
    void ordnetAntwortenDenRichtigenStoppgruendenZu() {
        assertThat(AuftragsVersand.stoppGrund(antwort(409, "{\"details\":{\"code\":\"lease_invalid\"}}")))
                .contains(VersandStopp.BINDUNG_ABGELOEST);
        assertThat(AuftragsVersand.stoppGrund(antwort(410, "{}"))).contains(VersandStopp.TURNIER_GELOESCHT);
        assertThat(AuftragsVersand.stoppGrund(antwort(404, "{\"error\":\"Turnier nicht gefunden\"}")))
                .contains(VersandStopp.TURNIER_GELOESCHT);
        assertThat(AuftragsVersand.stoppGrund(antwort(404, "{\"error\":\"Anmeldung nicht gefunden\"}"))).isEmpty();
        assertThat(AuftragsVersand.stoppGrund(antwort(403, "{}"))).contains(VersandStopp.NICHT_BERECHTIGT);
        assertThat(AuftragsVersand.stoppGrund(antwort(503, "kaputt"))).contains(VersandStopp.SERVERFEHLER);
        assertThat(AuftragsVersand.stoppGrund(antwort(429, "{}"))).contains(VersandStopp.SERVERFEHLER);
        assertThat(AuftragsVersand.stoppGrund(antwort(409, "{\"details\":{\"code\":\"idempotency_mismatch\"}}")))
                .isEmpty();
        assertThat(AuftragsVersand.stoppGrund(antwort(200, "{}"))).isEmpty();
    }

    @Test
    void liestCodeFeldUndFehlertextDerAntwort() {
        SyncAntwort antwort = antwort(409, "{\"error\":\"doppelt\",\"details\":{\"field\":\"lastName\"}}");

        assertThat(antwort.nenntFeld()).isTrue();
        assertThat(antwort.code()).isEmpty();
        assertThat(antwort.fehlertext()).contains("doppelt");
        assertThat(antwort(500, "kein json").json()).isEmpty();
    }
}
