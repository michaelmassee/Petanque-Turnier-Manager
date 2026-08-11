package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.petanqueturniermanager.helper.ISheet;

class MeldungenSpalteTest {

	/**
	 * Regression: bei Formation.NUR_TEAMNAME (anzNamenSpalten = 0) müssen
	 * {@link MeldungenSpalte#getNamenOderTeamnameSpalte()} und
	 * {@link MeldungenSpalte#getLetzteMeldungNameSpalte()} beide auf die Teamname-Spalte
	 * zeigen - sonst iteriert die Duplikat-Rotfärbung in
	 * {@link MeldeListeHelper#testDoppelteMeldungen()} über eine leere Spaltenspanne und
	 * färbt nichts ein.
	 */
	@Test
	void nurTeamnameFormationLiefertTeamnameAlsErsteUndLetzteNamensSpalte() {
		MeldungenSpalte<Object, Object> meldungenSpalte = MeldungenSpalte.builder()
				.ersteDatenZiele(2)
				.spielerNrSpalte(0)
				.ersteMeldungNameSpalteOffset(2) // Teamname aktiv -> Offset 2, wie in allen *ListeDelegate
				.anzNamenSpalten(0) // Formation.NUR_TEAMNAME: keine Spieler-Namensspalten
				.formation(Formation.NUR_TEAMNAME)
				.sheet(Mockito.mock(ISheet.class))
				.build();

		assertThat(meldungenSpalte.getTeamnameSpalte()).isEqualTo(1);
		assertThat(meldungenSpalte.getNamenOderTeamnameSpalte()).isEqualTo(1);
		assertThat(meldungenSpalte.getLetzteMeldungNameSpalte()).isEqualTo(1);
	}
}
