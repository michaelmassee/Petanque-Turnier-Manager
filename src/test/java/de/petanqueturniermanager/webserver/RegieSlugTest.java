package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RegieSlugTest {

    @Test
    void ausNameNormalisiertAufKleinschreibungZiffernUndBindestriche() {
        assertThat(RegieSlug.ausName(" Fernseher Bühne 1 "))
                .isEqualTo("fernseher-buhne-1");
    }

    @Test
    void validiereAkzeptiertGueltigenSlug() {
        RegieSlug.validiere("ziel-1");
    }

    @Test
    void validiereLehntReservierteRoutingSegmenteAb() {
        assertThatThrownBy(() -> RegieSlug.validiere("assets"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RegieSlug.validiere("events"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RegieSlug.validiere("steuerung"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validiereLehntUnsichereZeichenUndRandBindestricheAb() {
        assertThatThrownBy(() -> RegieSlug.validiere("../ziel"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RegieSlug.validiere("-ziel"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RegieSlug.validiere("ziel-"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
