package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoVersionVergleicherTest {

    @Test
    void gleicheMajorVersionIstNichtAbweichend() {
        assertThat(LoVersionVergleicher.istMajorAbweichend("24.8.3.2", "24.2.1.1")).isFalse();
    }

    @Test
    void unterschiedlicheMajorVersionIstAbweichend() {
        assertThat(LoVersionVergleicher.istMajorAbweichend("24.8.3.2", "26.2.1.1")).isTrue();
    }

    @Test
    void einZahligeVersionenWerdenVerglichen() {
        assertThat(LoVersionVergleicher.istMajorAbweichend("24", "26")).isTrue();
        assertThat(LoVersionVergleicher.istMajorAbweichend("24", "24")).isFalse();
    }

    @Test
    void nullVersionErzeugtKeinenFehlalarm() {
        assertThat(LoVersionVergleicher.istMajorAbweichend(null, "24.8.3.2")).isFalse();
        assertThat(LoVersionVergleicher.istMajorAbweichend("24.8.3.2", null)).isFalse();
        assertThat(LoVersionVergleicher.istMajorAbweichend(null, null)).isFalse();
    }

    @Test
    void leereOderKaputteVersionErzeugtKeinenFehlalarm() {
        assertThat(LoVersionVergleicher.istMajorAbweichend("", "24.8.3.2")).isFalse();
        assertThat(LoVersionVergleicher.istMajorAbweichend("unbekannt", "24.8.3.2")).isFalse();
        assertThat(LoVersionVergleicher.istMajorAbweichend("?", "24.8.3.2")).isFalse();
    }
}
