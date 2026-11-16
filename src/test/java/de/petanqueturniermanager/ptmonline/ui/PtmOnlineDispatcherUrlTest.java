/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Adressen, die „Im Browser öffnen“ und „Live-Turnierseite öffnen“ im Standardbrowser aufrufen. */
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
}
