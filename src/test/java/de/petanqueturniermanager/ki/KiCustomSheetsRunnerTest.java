/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.i18n.I18n;

/** Namenswahl der KI-Zusatzblätter: vom Plugin verwaltete Blätter werden nie ersetzt. */
class KiCustomSheetsRunnerTest {

    private static final Set<String> VERWALTET = Set.of("Meldeliste", "1. Spielrunde");

    @BeforeEach
    void deutscheTexte() {
        I18n.initFuerTest(Locale.GERMAN);
    }

    @Test
    void freierOderNichtVerwalteterNameBleibt() {
        assertThat(KiCustomSheetsRunner.zielName("Statistik", VERWALTET::contains, Set.of("Statistik")::contains))
                .isEqualTo("Statistik");
        assertThat(KiCustomSheetsRunner.zielName("Neu", VERWALTET::contains, Set.<String>of()::contains))
                .isEqualTo("Neu");
    }

    @Test
    void verwaltetesBlattBekommtErsatznamen() {
        assertThat(KiCustomSheetsRunner.zielName("Meldeliste", VERWALTET::contains, Set.of("Meldeliste")::contains))
                .isEqualTo("Meldeliste (KI)");
    }

    @Test
    void belegteErsatznamenWerdenHochgezaehlt() {
        Set<String> vorhanden = Set.of("1. Spielrunde", "1. Spielrunde (KI)", "1. Spielrunde (KI 2)");

        assertThat(KiCustomSheetsRunner.zielName("1. Spielrunde", VERWALTET::contains, vorhanden::contains))
                .isEqualTo("1. Spielrunde (KI 3)");
    }
}
