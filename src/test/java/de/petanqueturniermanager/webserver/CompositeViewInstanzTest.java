package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CompositeViewInstanzTest {

    @Test
    void splitSteuerungJsonAusRequestNormalisiertGueltigeGruppen() {
        String json = CompositeViewInstanz.splitSteuerungJsonAusRequest("""
                {"gruppen":{"R":[60.123,39.877],"R/L":[50,50]}}
                """);

        assertThat(json)
                .contains("\"typ\":\"split_steuerung\"")
                .contains("\"R\":[60.12,39.88]")
                .contains("\"R/L\":[50.0,50.0]");
    }

    @Test
    void splitSteuerungJsonAusRequestLehntUngueltigePfadeAb() {
        assertThatThrownBy(() -> CompositeViewInstanz.splitSteuerungJsonAusRequest("""
                {"gruppen":{"../x":[60,40]}}
                """))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void splitSteuerungJsonAusRequestLehntFalscheSummenAb() {
        assertThatThrownBy(() -> CompositeViewInstanz.splitSteuerungJsonAusRequest("""
                {"gruppen":{"R":[60,30]}}
                """))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
