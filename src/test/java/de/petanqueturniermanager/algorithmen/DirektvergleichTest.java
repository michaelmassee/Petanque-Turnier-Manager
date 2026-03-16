package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DirektvergleichTest {

	// ---------------------------------------------------------------
	// Siege als erstes Kriterium
	// ---------------------------------------------------------------

	@Test
	public void testCalc_teamAGewinntDurchMehrSiege() {
		int teamA = 1, teamB = 2;
		int[][] paarungen = { { 1, 2 } };
		int[][] siege = { { 2, 1 } };
		int[][] spielpunkte = { { 10, 8 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.GEWONNEN);
	}

	@Test
	public void testCalc_teamBGewinntDurchMehrSiege() {
		int teamA = 1, teamB = 2;
		int[][] paarungen = { { 1, 2 } };
		int[][] siege = { { 1, 2 } };
		int[][] spielpunkte = { { 10, 8 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.VERLOREN);
	}

	// ---------------------------------------------------------------
	// Spielpunkte als zweites Kriterium (Siege gleich)
	// ---------------------------------------------------------------

	@Test
	public void testCalc_gleichstandSiege_teamAGewinntDurchSpielPunkte() {
		int teamA = 1, teamB = 2;
		int[][] paarungen = { { 1, 2 } };
		int[][] siege = { { 1, 1 } };
		int[][] spielpunkte = { { 13, 10 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.GEWONNEN);
	}

	@Test
	public void testCalc_gleichstandSiege_teamBGewinntDurchSpielPunkte() {
		int teamA = 1, teamB = 2;
		int[][] paarungen = { { 1, 2 } };
		int[][] siege = { { 1, 1 } };
		int[][] spielpunkte = { { 8, 13 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.VERLOREN);
	}

	@Test
	public void testCalc_gleichstandSiegeUndSpielPunkte_gleich() {
		int teamA = 1, teamB = 2;
		int[][] paarungen = { { 1, 2 } };
		int[][] siege = { { 1, 1 } };
		int[][] spielpunkte = { { 10, 10 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.GLEICH);
	}

	// ---------------------------------------------------------------
	// Sonderfaelle
	// ---------------------------------------------------------------

	@Test
	public void testCalc_keinePaarungZwischenTeams_keinErgebnis() {
		int teamA = 1, teamB = 2;
		int[][] paarungen = { { 3, 4 }, { 5, 6 } };
		int[][] siege = { { 1, 1 }, { 1, 1 } };
		int[][] spielpunkte = { { 10, 10 }, { 10, 10 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.KEINERGEBNIS);
	}

	@Test
	public void testCalc_leereArrays_fehler() {
		assertThat(new Direktvergleich(1, 2, new int[0][0], new int[0][0], new int[0][0]).calc())
				.isEqualTo(DirektvergleichResult.FEHLER);
	}

	@Test
	public void testCalc_ungueltigeTeamNummer_fehler() {
		int[][] paarungen = { { 1, 2 } };
		int[][] siege = { { 1, 1 } };
		int[][] spielpunkte = { { 10, 10 } };

		assertThat(new Direktvergleich(0, 2, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.FEHLER);
		assertThat(new Direktvergleich(-1, 2, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.FEHLER);
		assertThat(new Direktvergleich(1, 0, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.FEHLER);
	}

	@Test
	public void testCalc_arrayLaengenUngleich_fehler() {
		int[][] paarungen = { { 1, 2 }, { 1, 2 } };
		int[][] siege = { { 1, 1 } }; // zu kurz
		int[][] spielpunkte = { { 10, 10 }, { 10, 10 } };

		assertThat(new Direktvergleich(1, 2, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.FEHLER);

		int[][] siege2 = { { 1, 1 }, { 1, 1 } };
		int[][] spielpunkte2 = { { 10, 10 } }; // zu kurz

		assertThat(new Direktvergleich(1, 2, paarungen, siege2, spielpunkte2).calc())
				.isEqualTo(DirektvergleichResult.FEHLER);
	}

	// ---------------------------------------------------------------
	// Paarungsreihenfolge und mehrere Spiele
	// ---------------------------------------------------------------

	@Test
	public void testCalc_paarungenInUmgekehrterReihenfolge() {
		int teamA = 1, teamB = 2;
		// Paarung {2,1}: Index 0 = teamB, Index 1 = teamA
		int[][] paarungen = { { 2, 1 } };
		int[][] siege = { { 1, 3 } }; // teamA bekommt siege[0][1]=3
		int[][] spielpunkte = { { 5, 10 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.GEWONNEN);
	}

	@Test
	public void testCalc_mehrereSpiele_ergebnisseWerdenSummiert() {
		int teamA = 1, teamB = 2;
		int[][] paarungen = { { 1, 2 }, { 1, 2 } };
		int[][] siege = { { 2, 1 }, { 1, 0 } }; // teamA: 3, teamB: 1
		int[][] spielpunkte = { { 13, 5 }, { 7, 3 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.GEWONNEN);
	}

	@Test
	public void testCalc_mehrereSpieleMitFremdenPaarungen_nurEigeneZaehlen() {
		int teamA = 3, teamB = 4;
		// Gemischt: Spiele mit anderen Teams werden ignoriert
		int[][] paarungen = { { 1, 8 }, { 6, 8 }, { 3, 4 }, { 3, 8 }, { 4, 3 } };
		int[][] siege = { { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 } };
		int[][] spielpunkte = { { 1, 4 }, { 1, 4 }, { 40, 53 }, { 1, 4 }, { 20, 65 } };

		// {3,4}: siege A=1, B=4; SpPkt A=40, B=53
		// {4,3}: siege A=4(→B bekommt), B=1(→A bekommt) → summSiegeA += 4, summSiegeB += 1; SpPkt A=65, B=20
		// gesamt: siegeA=1+4=5, siegeB=4+1=5 → gleich; SpPktA=40+65=105, SpPktB=53+20=73 → A gewinnt
		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.GEWONNEN);
	}

	@Test
	public void testCalc_mehrereSpieleMitFremdenPaarungen_verloren() {
		int teamA = 4, teamB = 3;
		int[][] paarungen = { { 1, 8 }, { 6, 8 }, { 3, 4 }, { 3, 8 }, { 4, 3 } };
		int[][] siege = { { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 } };
		int[][] spielpunkte = { { 1, 4 }, { 1, 4 }, { 40, 53 }, { 1, 4 }, { 20, 65 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.VERLOREN);
	}

	@Test
	public void testCalc_mehrereSpiele_siegeEntscheiden() {
		int teamA = 4, teamB = 3;
		int[][] paarungen = { { 1, 8 }, { 6, 8 }, { 3, 4 }, { 3, 8 }, { 4, 3 } };
		int[][] siege = { { 1, 4 }, { 1, 4 }, { 1, 4 }, { 1, 4 }, { 3, 2 } };
		int[][] spielpunkte = { { 1, 1 }, { 1, 1 }, { 1, 1 }, { 1, 4 }, { 1, 1 } };

		assertThat(new Direktvergleich(teamA, teamB, paarungen, siege, spielpunkte).calc())
				.isEqualTo(DirektvergleichResult.GEWONNEN);
	}
}
