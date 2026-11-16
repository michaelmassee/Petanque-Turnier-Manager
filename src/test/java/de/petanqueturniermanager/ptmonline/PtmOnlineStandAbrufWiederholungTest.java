/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Regression: Nach einem gescheiterten Statusabruf blieb der Runden-Abgleich offen, ohne dass ein weiterer Versuch
 * eingeplant wurde.
 */
class PtmOnlineStandAbrufWiederholungTest {

    private final PtmOnlineStandAbrufWiederholung wiederholung = new PtmOnlineStandAbrufWiederholung(3_000, 10_000);

    @Test
    void gescheiterterAbrufMitOffenemRundenabgleichWirdMitWachsendemAbstandWiederholt() {
        assertThat(wiederholung.nachAbruf(true, false)).hasValue(3_000);
        assertThat(wiederholung.nachAbruf(true, false)).hasValue(6_000);
        assertThat(wiederholung.nachAbruf(true, false)).hasValue(10_000);
        assertThat(wiederholung.nachAbruf(true, false)).as("gedeckelt").hasValue(10_000);
    }

    @Test
    void erfolgreicherAbrufBeendetUndSetztDenAbstandZurueck() {
        wiederholung.nachAbruf(true, false);
        wiederholung.nachAbruf(true, false);

        assertThat(wiederholung.nachAbruf(true, true)).isEmpty();
        assertThat(wiederholung.nachAbruf(true, false)).hasValue(3_000);
    }

    @Test
    void ohneOffenenRundenabgleichKeineWiederholung() {
        assertThat(wiederholung.nachAbruf(false, false)).isEmpty();
        assertThat(wiederholung.nachAbruf(false, true)).isEmpty();
    }
}
