/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static de.petanqueturniermanager.basesheet.meldeliste.MeleeNeuAuslosungPlan.namensSchluessel;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.MeleeDoppelteNamen.Ergebnis;

/** Testet die Namensprüfung vor der Mêlée-Übernahme. */
class MeleeDoppelteNamenTest {

	@Test
	void doppelteEingecheckteNamenWerdenGefunden() {
		Ergebnis ergebnis = MeleeDoppelteNamen.pruefe(List.of(
				zeile(1, "sda", "as", true, false),
				zeile(2, "sdasad", "qw", true, false),
				zeile(5, " SDA ", "AS", true, true)), Set.of());

		assertThat(ergebnis.doppelteMeleeZeilen()).containsExactly(1, 5);
		assertThat(ergebnis.istFehlerfrei()).isFalse();
	}

	@Test
	void nichtEingecheckteZaehlenNicht() {
		Ergebnis ergebnis = MeleeDoppelteNamen.pruefe(List.of(
				zeile(1, "sda", "as", true, false),
				zeile(2, "sda", "as", false, false)), Set.of());

		assertThat(ergebnis.istFehlerfrei()).isTrue();
	}

	@Test
	void neuerSpielerSchonVonHandInDerMeldeliste() {
		Ergebnis ergebnis = MeleeDoppelteNamen.pruefe(List.of(
				zeile(1, "Anna", "Müller", true, false),
				zeile(2, "Bert", "B", true, true)),
				Set.of(namensSchluessel("anna", "müller"), namensSchluessel("Bert", "B")));

		assertThat(ergebnis.meldelisteKonflikte()).as("Bert stammt selbst aus der Mêlée-Übernahme").containsExactly(1);
		assertThat(ergebnis.meldelisteSchluessel()).containsExactly(namensSchluessel("Anna", "Müller"));
	}

	private static MeleeAnmeldungZeile zeile(int zeile, String vorname, String nachname, boolean eingecheckt,
			boolean uebernommen) {
		return new MeleeAnmeldungZeile(zeile, zeile, vorname, nachname, 0, eingecheckt, uebernommen);
	}
}
