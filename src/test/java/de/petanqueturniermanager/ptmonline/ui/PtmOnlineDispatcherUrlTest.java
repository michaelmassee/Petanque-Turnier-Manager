/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Adressen, die „Im Browser öffnen“, „Live-Turnierseite öffnen“ und „QR-Code Live-Ansicht öffnen“ im
 * Standardbrowser aufrufen.
 */
public class PtmOnlineDispatcherUrlTest {

    @Test
    public void turnierUrlOhneDoppeltenSchraegstrich() {
        assertThat(PtmOnlineDispatcher.turnierUrl("https://ptm.example/", "/turniere/", "t1"))
                .isEqualTo("https://ptm.example/turniere/t1");
        assertThat(PtmOnlineDispatcher.turnierUrl("https://ptm.example", "/turniere/", "t1"))
                .isEqualTo("https://ptm.example/turniere/t1");
    }

    @Test
    public void turnierseiteTraegtDenAnzeigeSchluessel() {
        assertThat(PtmOnlineDispatcher.turnierseitenUrl("https://ptm.example/", "t 1", "a&b"))
                .isEqualTo("https://ptm.example/turnierseite/t+1?share=a%26b");
    }

    @Test
    public void qrSeiteHaengtAnDenPersoenlichenLiveLink() {
        assertThat("https://ptm.example/live/t/abc" + PtmOnlineDispatcher.LIVE_QR_SEITE)
                .isEqualTo("https://ptm.example/live/t/abc/qr");
    }

    @Test
    public void onlineIdDerZeileUeberLokaleUuid() {
        assertThat(PtmOnlineDispatcher.onlineIdDerZeile(Map.of(5, "u5"), 5, Map.of("u5", "online-5")))
                .contains("online-5");
    }

    @Test
    public void zeileOhneUuidOderOhneOnlineZuordnungHatKeineOnlineId() {
        assertThat(PtmOnlineDispatcher.onlineIdDerZeile(Map.of(), 5, Map.of("u5", "online-5"))).isEmpty();
        assertThat(PtmOnlineDispatcher.onlineIdDerZeile(Map.of(5, "u5"), 5, Map.of())).isEmpty();
    }
}
