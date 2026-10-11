/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MeldelisteUuidIndexTest {

	private static final String UUID_A = "aaaaaaaa-0000-4000-8000-000000000001";
	private static final String UUID_B = "bbbbbbbb-0000-4000-8000-000000000002";

	@Test
	void ordnetUuidUndNummerBeidseitigZu() {
		MeldelisteUuidIndex index = MeldelisteUuidIndex.aus(new String[][] { { "Nr" },
				{ "=PTM.ALG.TNRUUID(3;\"" + UUID_A + "\")" }, { "=PTM.ALG.TNRUUID(7;\"" + UUID_B + "\")" } },
				new int[] { 0, 3, 7 });

		assertThat(index.nr(UUID_A)).hasValue(3);
		assertThat(index.nr(UUID_B)).hasValue(7);
		assertThat(index.uuid(7)).contains(UUID_B);
	}

	@Test
	void zeilenOhneUuidOderOhneNummerZaehlenNicht() {
		MeldelisteUuidIndex index = MeldelisteUuidIndex.aus(
				new String[][] { { "5" }, { "=PTM.ALG.TNRUUID(0;\"" + UUID_A + "\")" } }, new int[] { 5, 0 });

		assertThat(index.uuid(5)).isEmpty();
		assertThat(index.nr(UUID_A)).isEmpty();
	}
}
