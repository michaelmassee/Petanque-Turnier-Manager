/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VersionVergleicherTest {

    @ParameterizedTest
    @CsvSource({
            // installiert , verfuegbar , erwartetNeuer
            "1.0.0       , 1.0.1       , true",
            "1.0.0       , 1.1.0       , true",
            "1.0.0       , 2.0.0       , true",
            "1.0.0       , v1.0.1      , true",
            "v1.0.0      , 1.0.1       , true",
            "v1.0.0      , v1.0.1      , true",
            "1.0.1       , 1.0.0       , false",
            "1.0.0       , 1.0.0       , false",
            "1.0.0       , V1.0.0      , false",
            "1.2.3       , 1.2.3       , false",
            "1.0.0       , 1.0.10      , true",
            "1.2.3       , 1.10.0      , true"
    })
    void erkenntStabileVersionsAenderungen(String installiert, String verfuegbar, boolean erwartet) {
        assertThat(VersionVergleicher.istNeuer(installiert, verfuegbar)).isEqualTo(erwartet);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.0.1-rc1", "1.0.1-RC1", "1.0.1-beta", "1.0.1-BETA",
            "1.0.1-alpha", "1.0.1-snapshot", "1.0.1-SNAPSHOT",
            "1.0.1-pre", "1.0.1-m1", "1.0.1-dev"
    })
    void preReleaseWirdNieAlsUpdateGemeldet(String verfuegbar) {
        assertThat(VersionVergleicher.istNeuer("1.0.0", verfuegbar))
                .as("Pre-Release %s darf nicht als Update gelten", verfuegbar)
                .isFalse();
    }

    @Test
    void installierteVersionDarfPreReleaseSein() {
        // Wer ein Pre-Release fährt und es kommt eine Stable raus → Update melden.
        assertThat(VersionVergleicher.istNeuer("1.0.0-rc1", "1.0.0")).isTrue();
        assertThat(VersionVergleicher.istNeuer("1.0.0-rc1", "1.0.1")).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "''          , 1.0.0",
            "1.0.0       , ''",
            "'   '       , 1.0.0",
            "1.0.0       , '   '",
            "v           , 1.0.0",
            "1.0.0       , v"
    })
    void leereOderNullEingabenLiefernFalse(String installiert, String verfuegbar) {
        assertThat(VersionVergleicher.istNeuer(installiert, verfuegbar)).isFalse();
    }

    @Test
    void nullEingabenSindToleriert() {
        assertThat(VersionVergleicher.istNeuer(null, "1.0.0")).isFalse();
        assertThat(VersionVergleicher.istNeuer("1.0.0", null)).isFalse();
        assertThat(VersionVergleicher.istNeuer(null, null)).isFalse();
    }

    @Test
    void normalisierenStripptVPrefix() {
        assertThat(VersionVergleicher.normalisieren("v1.2.3")).isEqualTo("1.2.3");
        assertThat(VersionVergleicher.normalisieren("V1.2.3")).isEqualTo("1.2.3");
        assertThat(VersionVergleicher.normalisieren("  v1.2.3  ")).isEqualTo("1.2.3");
        assertThat(VersionVergleicher.normalisieren("1.2.3")).isEqualTo("1.2.3");
    }

    @Test
    void normalisierenGibtNullBeiLeereingabe() {
        assertThat(VersionVergleicher.normalisieren(null)).isNull();
        assertThat(VersionVergleicher.normalisieren("")).isNull();
        assertThat(VersionVergleicher.normalisieren("   ")).isNull();
        assertThat(VersionVergleicher.normalisieren("v")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.0.0-rc1", "1.0.0-RC1", "2.0.0-SNAPSHOT", "1.0.0-beta.2", "1.0.0-alpha" })
    void preReleaseMarkerWirdErkannt(String version) {
        assertThat(VersionVergleicher.istPreRelease(version)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.0.0", "1.2.3", "10.20.30", "1.0.0+build42" })
    void stabileVersionIstKeinPreRelease(String version) {
        assertThat(VersionVergleicher.istPreRelease(version)).isFalse();
    }
}
