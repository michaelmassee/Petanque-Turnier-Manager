package de.petanqueturniermanager.helper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BrowserOeffnerTest {

    @Test
    void macosNutztOpen() {
        assertThat(BrowserOeffner.befehlFuerOs("Mac OS X", "http://localhost:8080"))
                .containsExactly("open", "http://localhost:8080");
    }

    @Test
    void windowsNutztRundll32() {
        assertThat(BrowserOeffner.befehlFuerOs("Windows 11", "http://localhost:8080/a b"))
                .containsExactly("rundll32", "url.dll,FileProtocolHandler", "http://localhost:8080/a b");
    }

    @Test
    void linuxNutztXdgOpen() {
        assertThat(BrowserOeffner.befehlFuerOs("Linux", "http://localhost:8080"))
                .containsExactly("xdg-open", "http://localhost:8080");
    }

    @Test
    void unbekanntesSystemNutztXdgOpen() {
        assertThat(BrowserOeffner.befehlFuerOs(null, "http://localhost:8080"))
                .containsExactly("xdg-open", "http://localhost:8080");
    }
}
