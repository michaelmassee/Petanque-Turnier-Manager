/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.triptete;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.SpielErgebnis;
import de.petanqueturniermanager.model.Team;

public class TripTeteTeamErgebnisTest {

	@Test
	public void getterLiefernVerbuchteWerte() {
		Team a = Team.from(1);
		Team b = Team.from(2);
		TripTeteBegegnungErgebnis begegnung = new TripTeteBegegnungErgebnis(a, b);
		begegnung.setPartieErgebnis(TripTetePartie.TRIPLETTE, new SpielErgebnis(13, 9));
		begegnung.setPartieErgebnis(TripTetePartie.DOUBLETTE, new SpielErgebnis(6, 13));
		begegnung.setPartieErgebnis(TripTetePartie.TETE, new SpielErgebnis(13, 11));

		TripTeteTeamErgebnis ergebnis = new TripTeteTeamErgebnis(a);
		ergebnis.verbucheBegegnung(true, begegnung);

		assertThat(ergebnis.getTeam()).isEqualTo(a);
		assertThat(ergebnis.getBegegnungenGespielt()).isEqualTo(1);
		assertThat(ergebnis.getBegegnungenGewonnen()).isEqualTo(1);
		assertThat(ergebnis.getBegegnungenVerloren()).isZero();
		assertThat(ergebnis.getBegegnungenUnentschieden()).isZero();
		assertThat(ergebnis.getPartienGewonnen()).isEqualTo(2);
		assertThat(ergebnis.getPartienVerloren()).isEqualTo(1);
		assertThat(ergebnis.getSpielPunktePlus()).isEqualTo(13 + 6 + 13);
		assertThat(ergebnis.getSpielPunkteMinus()).isEqualTo(9 + 13 + 11);
		assertThat(ergebnis.getSpielPunkteDiff()).isEqualTo((13 + 6 + 13) - (9 + 13 + 11));
	}

	@Test
	public void toStringEnthaeltKennzahlen() {
		TripTeteTeamErgebnis ergebnis = new TripTeteTeamErgebnis(Team.from(3));

		assertThat(ergebnis.toString())
				.contains("team=3")
				.contains("begSiege=0")
				.contains("partSiege=0");
	}
}
