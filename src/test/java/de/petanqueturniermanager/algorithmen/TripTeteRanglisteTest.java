/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.SpielErgebnis;
import de.petanqueturniermanager.model.Team;

public class TripTeteRanglisteTest {

	@Test
	public void sortiertNachBegegnungssiegen() {
		Team a = Team.from(1);
		Team b = Team.from(2);
		Team c = Team.from(3);

		TripTeteRangliste rangliste = new TripTeteRangliste();
		// A schlägt B 3:0, A schlägt C 2:1, B schlägt C 2:1 → A=2 Siege, B=1, C=0
		rangliste.addBegegnung(begegnung(a, b, 13, 5, 13, 7, 13, 9));
		rangliste.addBegegnung(begegnung(a, c, 13, 11, 9, 13, 13, 11));
		rangliste.addBegegnung(begegnung(b, c, 13, 7, 9, 13, 13, 11));

		List<TripTeteTeamErgebnis> liste = rangliste.getRangliste();

		assertThat(liste).extracting(e -> e.getTeam().getNr()).containsExactly(1, 2, 3);
		assertThat(liste.get(0).getBegegnungenGewonnen()).isEqualTo(2);
		assertThat(liste.get(1).getBegegnungenGewonnen()).isEqualTo(1);
		assertThat(liste.get(2).getBegegnungenGewonnen()).isZero();
	}

	@Test
	public void tiebreakPartienSiegeBeiGleichenBegegnungen() {
		Team a = Team.from(1);
		Team b = Team.from(2);
		Team c = Team.from(3);
		Team d = Team.from(4);

		TripTeteRangliste rangliste = new TripTeteRangliste();
		// A gewinnt 3:0, B gewinnt 2:1 → beide 1 Begegnungssieg, A hat mehr Partiensiege
		rangliste.addBegegnung(begegnung(a, c, 13, 5, 13, 7, 13, 9));
		rangliste.addBegegnung(begegnung(b, d, 13, 11, 9, 13, 13, 11));

		List<TripTeteTeamErgebnis> liste = rangliste.getRangliste();
		assertThat(liste).extracting(e -> e.getTeam().getNr()).startsWith(1, 2);
		assertThat(liste.get(0).getPartienGewonnen()).isEqualTo(3);
		assertThat(liste.get(1).getPartienGewonnen()).isEqualTo(2);
	}

	@Test
	public void tiebreakKugelDifferenzBeiGleichenPartien() {
		Team a = Team.from(1);
		Team b = Team.from(2);
		Team c = Team.from(3);
		Team d = Team.from(4);

		TripTeteRangliste rangliste = new TripTeteRangliste();
		// beide gewinnen 3:0, gleiche Partiensiege, A mit größerer Kugel-Δ
		rangliste.addBegegnung(begegnung(a, c, 13, 0, 13, 0, 13, 0));
		rangliste.addBegegnung(begegnung(b, d, 13, 10, 13, 10, 13, 10));

		List<TripTeteTeamErgebnis> liste = rangliste.getRangliste();
		assertThat(liste).extracting(e -> e.getTeam().getNr()).startsWith(1, 2);
		assertThat(liste.get(0).getKugelDiff()).isEqualTo(39);
		assertThat(liste.get(1).getKugelDiff()).isEqualTo(9);
	}

	@Test
	public void tiebreakKugelnPlusBeiGleicherDifferenz() {
		Team a = Team.from(1);
		Team b = Team.from(2);
		Team c = Team.from(3);
		Team d = Team.from(4);

		TripTeteRangliste rangliste = new TripTeteRangliste();
		// gleiche Begegnungs-/Partiensiege, gleiche Kugel-Δ, aber A mit mehr Σ+
		rangliste.addBegegnung(begegnung(a, c, 13, 10, 13, 10, 13, 10));
		rangliste.addBegegnung(begegnung(b, d, 12, 9, 12, 9, 12, 9));

		List<TripTeteTeamErgebnis> liste = rangliste.getRangliste();
		assertThat(liste.get(0).getKugelDiff()).isEqualTo(liste.get(1).getKugelDiff());
		assertThat(liste).extracting(e -> e.getTeam().getNr()).startsWith(1, 2);
	}

	@Test
	public void unvollstaendigeBegegnungWirdAbgelehnt() {
		TripTeteBegegnungErgebnis erg = new TripTeteBegegnungErgebnis(Team.from(1), Team.from(2));
		erg.setPartieErgebnis(TripTetePartie.TRIPLETTE, new SpielErgebnis(13, 9));

		TripTeteRangliste rangliste = new TripTeteRangliste();
		assertThatThrownBy(() -> rangliste.addBegegnung(erg))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private TripTeteBegegnungErgebnis begegnung(Team teamA, Team teamB,
			int trA, int trB, int doA, int doB, int teA, int teB) {
		return new TripTeteBegegnungErgebnis(teamA, teamB)
				.setPartieErgebnis(TripTetePartie.TRIPLETTE, new SpielErgebnis(trA, trB))
				.setPartieErgebnis(TripTetePartie.DOUBLETTE, new SpielErgebnis(doA, doB))
				.setPartieErgebnis(TripTetePartie.TETE, new SpielErgebnis(teA, teB));
	}
}
