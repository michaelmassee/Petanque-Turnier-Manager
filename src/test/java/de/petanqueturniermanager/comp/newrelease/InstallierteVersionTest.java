/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InstallierteVersionTest {

    @Test
    void liefertVersionWennExtensionGefunden() {
        var liste = new String[][] {
                { "com.fremd.extension", "9.9.9" },
                { InstallierteVersion.EXTENSION_ID, "1.2.3" }
        };
        var result = InstallierteVersion.ausExtensionListe(liste);

        assertThat(result).isPresent();
        assertThat(result.get().raw()).isEqualTo("1.2.3");
    }

    @Test
    void leereListeLiefertEmpty() {
        assertThat(InstallierteVersion.ausExtensionListe(new String[0][])).isEmpty();
    }

    @Test
    void nullListeLiefertEmpty() {
        assertThat(InstallierteVersion.ausExtensionListe(null)).isEmpty();
    }

    @Test
    void extensionNichtVorhandenLiefertEmpty() {
        var liste = new String[][] { { "com.foo", "1.0" }, { "com.bar", "2.0" } };
        assertThat(InstallierteVersion.ausExtensionListe(liste)).isEmpty();
    }

    @Test
    void leererVersionsSlotLiefertEmpty() {
        var liste = new String[][] { { InstallierteVersion.EXTENSION_ID, "" } };
        assertThat(InstallierteVersion.ausExtensionListe(liste)).isEmpty();
    }

    @Test
    void blankerVersionsSlotLiefertEmpty() {
        var liste = new String[][] { { InstallierteVersion.EXTENSION_ID, "   " } };
        assertThat(InstallierteVersion.ausExtensionListe(liste)).isEmpty();
    }

    @Test
    void nullVersionsSlotLiefertEmpty() {
        var liste = new String[][] { { InstallierteVersion.EXTENSION_ID, null } };
        assertThat(InstallierteVersion.ausExtensionListe(liste)).isEmpty();
    }

    @Test
    void zuKurzesSubArrayWirdUebersprungen() {
        // Mischung aus defekten und korrekten Einträgen
        var liste = new String[][] {
                { "nur-eine-spalte" },
                new String[0],
                null,
                { InstallierteVersion.EXTENSION_ID, "1.2.3" }
        };
        var result = InstallierteVersion.ausExtensionListe(liste);

        assertThat(result).isPresent();
        assertThat(result.get().raw()).isEqualTo("1.2.3");
    }

    @Test
    void equalsUndHashCodeBasierenAufRaw() {
        var a = InstallierteVersion.ausExtensionListe(
                new String[][] { { InstallierteVersion.EXTENSION_ID, "1.0.0" } }).orElseThrow();
        var b = InstallierteVersion.ausExtensionListe(
                new String[][] { { InstallierteVersion.EXTENSION_ID, "1.0.0" } }).orElseThrow();
        var c = InstallierteVersion.ausExtensionListe(
                new String[][] { { InstallierteVersion.EXTENSION_ID, "2.0.0" } }).orElseThrow();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}
