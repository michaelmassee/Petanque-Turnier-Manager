/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class FormationTest {

	@Test
	void meleeAnmeldungNurBeiDoubletteUndTriplette() {
		EnumSet<Formation> erlaubt = EnumSet.noneOf(Formation.class);
		for (Formation formation : Formation.values()) {
			if (formation.erlaubtMeleeAnmeldung()) {
				erlaubt.add(formation);
			}
		}
		assertThat(erlaubt).containsExactlyInAnyOrder(Formation.DOUBLETTE, Formation.TRIPLETTE);
	}
}
