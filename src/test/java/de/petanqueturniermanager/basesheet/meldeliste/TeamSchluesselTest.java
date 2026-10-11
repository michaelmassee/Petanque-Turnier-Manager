/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TeamSchluesselTest {

	private static final String BEREICH = "$'Meldeliste'.$A$1:$A$999";
	private static final String UUID = "aaaaaaaa-0000-4000-8000-000000000001";

	@Test
	void uuidSuchtUeberUuidZeile() {
		TeamSchluessel team = TeamSchluessel.uuidLiteral(UUID);

		assertThat(team.zeileImBereich(BEREICH)).isEqualTo("PTM.ALG.UUIDZEILE(\"" + UUID + "\";" + BEREICH + ")");
		assertThat(team.nrAnzeige(BEREICH))
				.isEqualTo("INDEX(" + BEREICH + ";PTM.ALG.UUIDZEILE(\"" + UUID + "\";" + BEREICH + "))");
	}

	@Test
	void nrSuchtUeberMatchUndZeigtDenAusdruck() {
		TeamSchluessel team = TeamSchluessel.nr("C5");

		assertThat(team.zeileImBereich(BEREICH)).isEqualTo("MATCH(C5;" + BEREICH + ";0)");
		assertThat(team.nrAnzeige(BEREICH)).isEqualTo("C5");
	}
}
