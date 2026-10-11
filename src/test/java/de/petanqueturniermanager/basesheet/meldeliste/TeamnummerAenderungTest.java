/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAenderung.Ablehnung;
import de.petanqueturniermanager.basesheet.meldeliste.TeamnummerAenderung.Plan;

class TeamnummerAenderungTest {

	private static final String UUID_A = "aaaaaaaa-0000-4000-8000-000000000001";
	private static final String UUID_B = "bbbbbbbb-0000-4000-8000-000000000002";

	/** Kopfzeile mit Zahl 3, darunter Nr 3 (A) und Nr 7 (B). */
	private static final String[][] FORMELN = { { "3" }, { GlobalImpl.FORMAT_PTM_TNR_UUID(3, UUID_A) },
			{ GlobalImpl.FORMAT_PTM_TNR_UUID(7, UUID_B) }, { "" } };
	private static final int[] NUMMERN = { 3, 3, 7, 0 };

	@Test
	void freieNummerAendertNurDieZahlDerMeldungUndBehaeltDieUuid() {
		Plan plan = TeamnummerAenderung.plane(FORMELN, NUMMERN, 3, 12).plan().orElseThrow();

		assertThat(plan.getauscht()).isFalse();
		assertThat(plan.formelProZeile()).containsExactly(
				entry(1, GlobalImpl.FORMAT_PTM_TNR_UUID(12, UUID_A)));
	}

	@Test
	void vergebeneNummerTauschtBeideMeldungen() {
		Plan plan = TeamnummerAenderung.plane(FORMELN, NUMMERN, 3, 7).plan().orElseThrow();

		assertThat(plan.getauscht()).isTrue();
		assertThat(plan.formelProZeile()).containsExactly(
				entry(1, GlobalImpl.FORMAT_PTM_TNR_UUID(7, UUID_A)),
				entry(2, GlobalImpl.FORMAT_PTM_TNR_UUID(3, UUID_B)));
	}

	@Test
	void zahlInDerKopfzeileZaehltNichtAlsMeldung() {
		String[][] formeln = { { "9" }, { GlobalImpl.FORMAT_PTM_TNR_UUID(3, UUID_A) } };

		assertThat(TeamnummerAenderung.plane(formeln, new int[] { 9, 3 }, 9, 4).ablehnung())
				.contains(Ablehnung.ALTE_NR_FEHLT);
		assertThat(TeamnummerAenderung.plane(formeln, new int[] { 9, 3 }, 3, 9).plan().orElseThrow().getauscht())
				.isFalse();
	}

	@Test
	void lehntUngueltigeEingabenAb() {
		assertThat(TeamnummerAenderung.plane(FORMELN, NUMMERN, 3, 0).ablehnung())
				.contains(Ablehnung.NEUE_NR_UNGUELTIG);
		assertThat(TeamnummerAenderung.plane(FORMELN, NUMMERN, 7, 7).ablehnung()).contains(Ablehnung.GLEICHE_NR);
		assertThat(TeamnummerAenderung.plane(FORMELN, NUMMERN, 5, 6).ablehnung()).contains(Ablehnung.ALTE_NR_FEHLT);
	}

	@Test
	void meldungOhneUuidImDatenbereichWirdNichtStillUeberschrieben() {
		String[][] formeln = { { GlobalImpl.FORMAT_PTM_TNR_UUID(3, UUID_A) }, { "8" } };

		assertThat(TeamnummerAenderung.plane(formeln, new int[] { 3, 8 }, 3, 8).ablehnung())
				.contains(Ablehnung.OHNE_UUID);
	}
}
