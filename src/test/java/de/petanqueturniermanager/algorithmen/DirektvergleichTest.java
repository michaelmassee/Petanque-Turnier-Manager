package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import javax.annotation.processing.Generated;

import org.junit.jupiter.api.Test;

@Generated(value = "org.junit-tools-1.1.0")
public class DirektvergleichTest {

	@Test
	public void testCalcWinSpielPnkt() throws Exception {
		int teamA = 3;
		int teamB = 4;

		int[][] teamPaarungen = { { 1, 8 }, { 6, 8 }, { 3, 4 }, { 3, 8 }, { 4, 3 } };
		int[][] siege = { { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 } };
		int[][] spielpunkte = { { 1, 4 }, { 1, 4 }, { 40, 53 }, { 1, 4 }, { 20, 65 } };

		Direktvergleich vrgl = new Direktvergleich(teamA, teamB, teamPaarungen, siege, spielpunkte);
		DirektvergleichResult resultCalc = vrgl.calc();

		assertThat(resultCalc).isEqualTo(DirektvergleichResult.GEWONNEN);
	}

	@Test
	public void testCalcLooseSpielPnkt() throws Exception {
		int teamA = 4;
		int teamB = 3;

		int[][] teamPaarungen = { { 1, 8 }, { 6, 8 }, { 3, 4 }, { 3, 8 }, { 4, 3 } };
		int[][] siege = { { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 } };
		int[][] spielpunkte = { { 1, 4 }, { 1, 4 }, { 40, 53 }, { 1, 4 }, { 20, 65 } };

		Direktvergleich vrgl = new Direktvergleich(teamA, teamB, teamPaarungen, siege, spielpunkte);
		DirektvergleichResult resultCalc = vrgl.calc();

		assertThat(resultCalc).isEqualTo(DirektvergleichResult.VERLOREN);
	}

	@Test
	public void testCalcWinSiege() throws Exception {
		int teamA = 4;
		int teamB = 3;

		int[][] teamPaarungen = { { 1, 8 }, { 6, 8 }, { 3, 4 }, { 3, 8 }, { 4, 3 } };
		int[][] siege = { { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 }, { 3, 2 } };
		int[][] spielpunkte = { { 1, 1 }, { 1, 1 }, { 1, 1 }, { 1, 4 }, { 1, 1 } };

		Direktvergleich vrgl = new Direktvergleich(teamA, teamB, teamPaarungen, siege, spielpunkte);
		DirektvergleichResult resultCalc = vrgl.calc();

		assertThat(resultCalc).isEqualTo(DirektvergleichResult.GEWONNEN);
	}
}