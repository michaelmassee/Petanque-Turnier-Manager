package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;

public class SchweizerTeamErgebnisTest {

	@Test
	public void testKonstruktorUndAccessor() {
		SchweizerTeamErgebnis ergebnis = new SchweizerTeamErgebnis(7, 3, 12, 25, List.of(1, 4, 9));

		assertThat(ergebnis.teamNr()).isEqualTo(7);
		assertThat(ergebnis.siege()).isEqualTo(3);
		assertThat(ergebnis.punktedifferenz()).isEqualTo(12);
		assertThat(ergebnis.erzieltePunkte()).isEqualTo(25);
		assertThat(ergebnis.gegnerNrn()).containsExactly(1, 4, 9);
	}

	@Test
	public void testEqualsUndHashCode_gleicheWerte_sindEqual() {
		SchweizerTeamErgebnis a = new SchweizerTeamErgebnis(1, 2, 5, 20, List.of(3, 4));
		SchweizerTeamErgebnis b = new SchweizerTeamErgebnis(1, 2, 5, 20, List.of(3, 4));

		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

	@Test
	public void testEquals_unterschiedlicheTeamNr_nichtEqual() {
		SchweizerTeamErgebnis a = new SchweizerTeamErgebnis(1, 2, 5, 20, List.of(3, 4));
		SchweizerTeamErgebnis b = new SchweizerTeamErgebnis(2, 2, 5, 20, List.of(3, 4));

		assertThat(a).isNotEqualTo(b);
	}

	@Test
	public void testEquals_unterschiedlicheGegnerListe_nichtEqual() {
		SchweizerTeamErgebnis a = new SchweizerTeamErgebnis(1, 2, 5, 20, List.of(3, 4));
		SchweizerTeamErgebnis b = new SchweizerTeamErgebnis(1, 2, 5, 20, List.of(4, 3));

		// Reihenfolge der Gegnerliste ist semantisch relevant (List.equals)
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	public void testLeereGegnerListe() {
		SchweizerTeamErgebnis ergebnis = new SchweizerTeamErgebnis(5, 0, 0, 0, List.of());
		assertThat(ergebnis.gegnerNrn()).isEmpty();
		assertThat(ergebnis.siege()).isZero();
	}

	@Test
	public void testNegativePunktedifferenz() {
		SchweizerTeamErgebnis ergebnis = new SchweizerTeamErgebnis(3, 1, -7, 5, List.of(1));
		assertThat(ergebnis.punktedifferenz()).isEqualTo(-7);
	}
}
