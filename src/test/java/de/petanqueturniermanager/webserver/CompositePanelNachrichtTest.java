package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CompositePanelNachrichtTest {

    @Test
    void statischeDateiVerwendetRelativeLocalPanelUrl() {
        CompositePanelNachricht nachricht = CompositePanelNachricht.statischeDatei(2, 150, "/tmp/anzeige.html");

        assertThat(nachricht.externeUrl())
                .startsWith("local-panel/2/?v=")
                .doesNotStartWith("/");
        assertThat(nachricht.zoom()).isEqualTo(150);
    }
}
