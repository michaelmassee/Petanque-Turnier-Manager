/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.petanqueturniermanager.exception.GenerateException;

import org.junit.jupiter.api.Test;

class TeamSchluesselTest {

	private static final String BEREICH = "$'Meldeliste'.$A$1:$A$999";
	private static final String UUID = "aaaaaaaa-0000-4000-8000-000000000001";

	@Test
	void uuidSuchtUeberUuidZeile() {
		TeamSchluessel team = TeamSchluessel.uuidLiteral(UUID);

		assertThat(team.zeileImBereich(BEREICH)).isEqualTo("PTM.ALG.UUIDZEILE(\"" + UUID + "\";" + BEREICH + ")");
		assertThat(team.nrAnzeige(BEREICH))
				.isEqualTo("PTM.ALG.UUIDTEAMNR(\"" + UUID + "\";" + BEREICH + ")");
	}

	@Test
	void angezeigteNrSuchtUeberMatchUndZeigtDenAusdruck() {
		TeamSchluessel team = TeamSchluessel.angezeigteNr("C5");

		assertThat(team.zeileImBereich(BEREICH)).isEqualTo("MATCH(C5;" + BEREICH + ";0)");
		assertThat(team.nrAnzeige(BEREICH)).isEqualTo("C5");
	}

	@Test
	void bestehendeNrFormelBehaeltDieUuidStattErneutDieNummerZuSuchen() throws GenerateException {
		TeamSchluessel team = TeamSchluessel.ausNrFormel(
				"=DE.PETANQUETURNIERMANAGER.ADDIN.GLOBALADDIN.PTMUUIDTEAMNR(\"" + UUID + "\";A1:A999)", "G6");
		assertThat(team).isEqualTo(TeamSchluessel.uuidLiteral(UUID));
		assertThat(team.zeileImBereich(BEREICH)).doesNotContain("MATCH", "G6");
	}

	@Test
	void nummerOhneUuidWirdAbgewiesen() {
		for (String formel : new String[] { "7", "0", "", "=G6", null }) {
			assertThatThrownBy(() -> TeamSchluessel.ausNrFormel(formel, "G6"))
					.isInstanceOf(GenerateException.class);
		}
	}
	@Test
	void nurExpliziteNullIstFreispiel() {
		assertThat(TeamSchluessel.istFreispielNrFormel("0")).isTrue();
		assertThat(TeamSchluessel.istFreispielNrFormel("=0")).isTrue();
		for (String formel : new String[] { "7", "=G6", "", "=1-1", null }) {
			assertThat(TeamSchluessel.istFreispielNrFormel(formel)).isFalse();
		}
	}

}
