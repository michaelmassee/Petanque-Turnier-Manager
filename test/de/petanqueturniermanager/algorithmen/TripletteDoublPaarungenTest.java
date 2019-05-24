package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
/*
* PaarungenTest.java
* Erstellung     : 31.08.2017 / Michael Massee
*/
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.util.concurrent.UncheckedExecutionException;

import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.MeleeSpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;

public class TripletteDoublPaarungenTest {

	TripletteDoublPaarungen paarungen;
	List<Team> teams;
	Meldungen meldungen;

	@Before
	public void setup() throws AlgorithmenException {
		// this.paarungen = new TripletteDoublPaarungen();
		paarungen = spy(TripletteDoublPaarungen.class);

		meldungen = newTestMeldungen(16);
		teams = newTestTeams(meldungen);
	}

	@Test
	public void testNurDoubletteOhneFesteTeamGroese() throws Exception {

		Meldungen meldungen = new Meldungen();

		for (int spielrNr = 1; spielrNr < 9; spielrNr++) {
			meldungen.addSpielerWennNichtVorhanden(Spieler.from(spielrNr));
		}
		MeleeSpielRunde spielRunde = paarungen.neueSpielrunde(1, meldungen);

		assertThat(spielRunde.teams()).isNotEmpty();
		assertThat(spielRunde.teams()).hasSize(4);
		assertThat(meldungen.size()).isEqualTo(8);

		for (Team team : spielRunde.teams()) {
			// nur doublette vorhanden
			assertThat(team.size()).isEqualTo(2);
		}
	}

	@Test
	public void mehrereSpielrundenUnterschiedlicheTeamGroßeTest() throws Exception {

		List<PaarungenExpectedAnzahl> testDatenList = testDaten();

		int anzSpielrunden = 4;
		int expectedCalls = testDatenList.size() * 4;

		testDatenList.forEach((paarungenExpectedAnzahl) -> {
			try {
				meldungen = new Meldungen();
				for (int spielrNr = 1; spielrNr <= paarungenExpectedAnzahl.expAnzSpieler; spielrNr++) {
					meldungen.addSpielerWennNichtVorhanden(Spieler.from(spielrNr));
				}
				for (int spielrunde = 1; spielrunde <= anzSpielrunden; spielrunde++) {
					MeleeSpielRunde spielRunde = paarungen.neueSpielrunde(spielrunde, meldungen);
					pruefeTeamMischungSpielrunde(paarungenExpectedAnzahl.expAnzSpieler, paarungenExpectedAnzahl.expAnzDoubl, paarungenExpectedAnzahl.expAnzTriplett, spielRunde);
				}
			} catch (AlgorithmenException e) {
				throw new UncheckedExecutionException(e);
			}
		});
		verify(paarungen, times(expectedCalls)).neueSpielrunde(anyInt(), any(Meldungen.class));
	}

	@Test
	public void testMischingDoubletteUndTripletteOhneFesteTeamGroese() throws Exception {
		pruefeTeamMischung(4, 2, 0);
		pruefeTeamMischung(5, 1, 1);
		pruefeTeamMischung(6, 0, 2);
		pruefeTeamMischung(8, 4, 0);
		pruefeTeamMischung(9, 3, 1);
		pruefeTeamMischung(11, 1, 3);
		pruefeTeamMischung(16, 2, 4);
		pruefeTeamMischung(17, 1, 5);
		pruefeTeamMischung(18, 0, 6);
		pruefeTeamMischung(19, 5, 3);
		pruefeTeamMischung(20, 4, 4);
		pruefeTeamMischung(21, 3, 5);
		pruefeTeamMischung(22, 2, 6);
		pruefeTeamMischung(23, 1, 7);
		pruefeTeamMischung(24, 0, 8);
		pruefeTeamMischung(31, 5, 7);
	}

	private void pruefeObeDoppelteSpielerVorhandenInSpielrunde(MeleeSpielRunde spielRunde) {
		HashSet<Integer> spielrNr = new HashSet<>();

		for (Team team : spielRunde.teams()) {
			for (Spieler spielr : team.spieler()) {
				assertThat(!spielrNr.contains(spielr.getNr()));
				spielrNr.add(spielr.getNr());
			}
		}
	}

	private void pruefeTeamMischung(int expAnzSpieler, int expAnzDoubl, int expAnzTriplett) throws AlgorithmenException {

		meldungen = new Meldungen();
		for (int spielrNr = 1; spielrNr <= expAnzSpieler; spielrNr++) {
			meldungen.addSpielerWennNichtVorhanden(Spieler.from(spielrNr));
		}
		MeleeSpielRunde spielRunde = paarungen.neueSpielrunde(1, meldungen);
		pruefeTeamMischungSpielrunde(expAnzSpieler, expAnzDoubl, expAnzTriplett, spielRunde);
	}

	private void pruefeTeamMischungSpielrunde(int expAnzSpieler, int expAnzDoubl, int expAnzTriplett, MeleeSpielRunde spielRunde) {

		int expAnzTeams = expAnzDoubl + expAnzTriplett;

		pruefeObeDoppelteSpielerVorhandenInSpielrunde(spielRunde);

		assertThat(spielRunde.teams()).isNotEmpty();
		assertThat(spielRunde.teams()).hasSize(expAnzTeams);
		assertThat(meldungen.size()).isEqualTo(expAnzSpieler);

		int teamCntr = 1;
		int anzTriplette = 0;
		for (Team team : spielRunde.teams()) {
			// erste teams doublette
			if (teamCntr <= expAnzDoubl) {
				assertThat(team.size()).as("anz spieler %d, test doublette teamNr %d", expAnzSpieler, teamCntr).isEqualTo(2);
			} else {
				// dann triplette teams
				assertThat(team.size()).as("anz spieler %d, test triplette teamNr %d", expAnzSpieler, teamCntr).isEqualTo(3);
				anzTriplette++;
			}
			teamCntr++;
		}
		assertThat(anzTriplette).as("anz spieler %d, exp anz Triplette %d", expAnzSpieler, anzTriplette).isEqualTo(expAnzTriplett);
	}

	@Test
	public void testKannTauschenMitSpielerOhneTeam() throws Exception {

		// team 1
		// 1,2,3
		// team 2
		// 4,5,6

		// team 3
		// 10,11,12
		Team spielerteam3 = new Team(3);
		for (int i = 10; i < 13; i++) {
			spielerteam3.addSpielerWennNichtVorhanden(Spieler.from(i));
		}

		// 11 hat mit 1,2,3 zusammen gespielt
		Team spielerteam1 = teams.get(0);

		Spieler spieler11_OhneTeam = spielerteam3.findSpielerByNr(11);
		Spieler spieler1 = spielerteam1.findSpielerByNr(1);
		Spieler spieler2 = spielerteam1.findSpielerByNr(2);
		Spieler spieler3 = spielerteam1.findSpielerByNr(3);
		spieler1.addWarImTeamMitWennNichtVorhanden(spieler11_OhneTeam);
		spieler2.addWarImTeamMitWennNichtVorhanden(spieler11_OhneTeam);
		spieler3.addWarImTeamMitWennNichtVorhanden(spieler11_OhneTeam);

		spieler11_OhneTeam.deleteTeam();

		Spieler result = paarungen.kannTauschenMitSpielerOhneTeam(spieler11_OhneTeam, spielerteam3, teams);

		assertNotNull(result);
		assertEquals(4, result.getNr());
	}

	@Test
	public void testFindNextTeamInSpielrunde_Einfach_next_Freie_Spieler() throws Exception {
		MeleeSpielRunde spielrunde = new MeleeSpielRunde(1);
		spielrunde.addTeamsWennNichtVorhanden(teams);
		Team resultTeam = paarungen.findNextTeamInSpielrunde(3, meldungen, spielrunde);
		assertNotNull(resultTeam);
		assertEquals(3, resultTeam.size());
	}

	@Test
	public void testSpielerNr() throws Exception {
		for (int teamNr = 1; teamNr < 5; teamNr++) {
			for (int i = 1; i < 4; i++) {
				System.out.print(spielerNr(teamNr, i, 3) + ",");
			}
		}
	}

	private int spielerNr(int teamNr, int idx, int teamSize) {
		return ((teamNr - 1) * teamSize) + idx;
	}

	@Test
	public void testFindNextTeamInSpielrunde_letzteTeam_Muss_tauschen() throws Exception {

		// erste runde [1,2,3] [4,5,6] [7,8,9] [10,11,12]

		Meldungen meldungen2 = newTestMeldungen(12);

		// erste runde team 1 bis 4
		// hat jeder mit jeder gespielt
		// -------------------------------------------------------------
		for (int teamNr = 1; teamNr < 5; teamNr++) {
			Team spielerteam = new Team(teamNr);
			for (int i = 1; i < 4; i++) {
				int spielerNr = spielerNr(teamNr, i, 3);
				Spieler spielerAusMeldungen = meldungen2.findSpielerByNr(spielerNr);
				spielerteam.addSpielerWennNichtVorhanden(spielerAusMeldungen);
			}
			assertEquals(3, spielerteam.spieler().size());
			for (int i = 1; i < 4; i++) {
				int spielerNr = spielerNr(teamNr, i, 3);
				Spieler testSpieler = spielerteam.findSpielerByNr(spielerNr);
				assertNotNull(testSpieler);
				assertEquals("Spieler " + testSpieler.getNr(), 2, testSpieler.anzahlMitSpieler());
			}
		}

		// -------------------------------------------------------------
		// 2 runde start
		// team löschen
		meldungen2.resetTeam();

		// noch keine Teams
		List<Team> teams2 = new ArrayList<>();

		// neue Spielrunde
		MeleeSpielRunde spielrunde = new MeleeSpielRunde(1);

		Team resultTeam = paarungen.findNextTeamInSpielrunde(3, meldungen2, spielrunde);
		assertNotNull(resultTeam);
		assertEquals(3, resultTeam.size());
		teams2.add(resultTeam);

		resultTeam = paarungen.findNextTeamInSpielrunde(3, meldungen2, spielrunde);
		assertNotNull(resultTeam);
		assertEquals(3, resultTeam.size());
		teams2.add(resultTeam);

		resultTeam = paarungen.findNextTeamInSpielrunde(3, meldungen2, spielrunde);
		assertNotNull(resultTeam);
		assertEquals(3, resultTeam.size());
		teams2.add(resultTeam);

		// letztes Team nicht so einfach muss tauschen
		resultTeam = paarungen.findNextTeamInSpielrunde(3, meldungen2, spielrunde);
		assertNotNull(resultTeam);
		assertEquals(3, resultTeam.size());
		teams2.add(resultTeam);

		// zweite runde [4,7,11] [2,8,12] [3,6,9] [10,1,5]
		// not valid weil mir mischen
		assertNotNull(teams2.get(0).findSpielerByNr(4));
		assertNotNull(teams2.get(0).findSpielerByNr(7));
		assertNotNull(teams2.get(0).findSpielerByNr(11));

		assertNotNull(teams2.get(1).findSpielerByNr(2));
		assertNotNull(teams2.get(1).findSpielerByNr(8));
		assertNotNull(teams2.get(1).findSpielerByNr(12));

		assertNotNull(teams2.get(2).findSpielerByNr(3));
		assertNotNull(teams2.get(2).findSpielerByNr(6));
		assertNotNull(teams2.get(2).findSpielerByNr(9));

		assertNotNull(teams2.get(3).findSpielerByNr(10));
		assertNotNull(teams2.get(3).findSpielerByNr(1));
		assertNotNull(teams2.get(3).findSpielerByNr(5));
	}

	@Test
	@Ignore
	public void testNeueSpielrunde_Fail_in_runde_vier() throws Exception {
		// @formatter:off
		// SpielRunde{Nr=1, Teams=[Team{nr=1, Spieler=[9,12,10]},Team{nr=2, Spieler=[6,1,7]},Team{nr=3,Spieler=[2,8,4]},Team{nr=4, Spieler=[5,3,11]}]}
		// SpielRunde{Nr=2, Teams=[Team{nr=1, Spieler=[10,2,6]},Team{nr=2, Spieler=[11,9,1]},Team{nr=3,Spieler=[12,4,5]},Team{nr=4, Spieler=[7,8,3]}]}
		// SpielRunde{Nr=3, Teams=[Team{nr=1, Spieler=[4,10,7]},Team{nr=2, Spieler=[5,9,6]},Team{nr=3,Spieler=[12,8,11]},Team{nr=4, Spieler=[3,2,1]}]}
		// @formatter:on

		Meldungen testMeldungen = newTestMeldungen(16);

		List<Integer[]> spielerNrTeamListe = new ArrayList<>();
		spielerNrTeamListe.add(new Integer[] { 9, 12, 10 });
		spielerNrTeamListe.add(new Integer[] { 6, 1, 7 });
		spielerNrTeamListe.add(new Integer[] { 2, 8, 4 });
		spielerNrTeamListe.add(new Integer[] { 5, 3, 11 });
		MeleeSpielRunde ersteRunde = buildTestRunde(1, testMeldungen, spielerNrTeamListe);
		System.out.println(ersteRunde);

		spielerNrTeamListe = new ArrayList<>();
		spielerNrTeamListe.add(new Integer[] { 10, 2, 6 });
		spielerNrTeamListe.add(new Integer[] { 11, 9, 1 });
		spielerNrTeamListe.add(new Integer[] { 12, 4, 5 });
		spielerNrTeamListe.add(new Integer[] { 7, 8, 3 });
		MeleeSpielRunde zweiteRunde = buildTestRunde(2, testMeldungen, spielerNrTeamListe);
		System.out.println(zweiteRunde);

		spielerNrTeamListe = new ArrayList<>();
		spielerNrTeamListe.add(new Integer[] { 4, 10, 7 });
		spielerNrTeamListe.add(new Integer[] { 5, 9, 6 });
		spielerNrTeamListe.add(new Integer[] { 12, 8, 11 });
		spielerNrTeamListe.add(new Integer[] { 3, 2, 1 });
		MeleeSpielRunde dritteRunde = buildTestRunde(3, testMeldungen, spielerNrTeamListe);
		System.out.println(dritteRunde);

		MeleeSpielRunde vierteRunde = paarungen.generiereNeuSpielrundeMitFesteTeamGroese(4, 3, testMeldungen);
		System.out.println(vierteRunde);
		assertNotNull(vierteRunde);
		assertEquals(4, vierteRunde.teams().size());
	}

	private MeleeSpielRunde buildTestRunde(int nr, Meldungen testMeldungen, List<Integer[]> spielerNrTeamListe) throws AlgorithmenException {
		MeleeSpielRunde spielRunde = new MeleeSpielRunde(nr);

		int tmNr = 1;
		for (Integer[] teamliste : spielerNrTeamListe) {
			Team team = buildTestTeam(tmNr, testMeldungen, teamliste);
			spielRunde.addTeamWennNichtVorhanden(team);
			tmNr++;
		}
		return spielRunde;
	}

	private Team buildTestTeam(int nr, Meldungen testMeldungen, Integer[] spielerNr) throws AlgorithmenException {
		Team team = new Team(nr);

		for (Integer splnr : spielerNr) {
			team.addSpielerWennNichtVorhanden(testMeldungen.findSpielerByNr(splnr));
		}
		return team;

	}

	@Test
	public void testNeueSpielrunde_12Spieler() throws Exception {

		// 12 spieler = 4 runden
		// 18 spieler = 6 runden
		// 24 spieler = 8 runden

		Meldungen meldungen = newTestMeldungen(12);

		// TODO 3 runden sind nicht immer möglich
		for (int rundenr = 1; rundenr < 3; rundenr++) {
			MeleeSpielRunde runde = paarungen.generiereNeuSpielrundeMitFesteTeamGroese(rundenr, 3, meldungen);
			System.out.println(runde);
			assertNotNull(runde);
			assertEquals(4, runde.teams().size());
		}
	}

	@Test
	public void testNeueSpielrunde_18() throws Exception {
		Meldungen meldungen = newTestMeldungen(18);
		int anzTriplette = 6;

		MeleeSpielRunde ersteRunde = paarungen.generiereNeuSpielrundeMitFesteTeamGroese(1, 3, meldungen);
		System.out.println(ersteRunde);
		assertNotNull(ersteRunde);
		assertEquals(anzTriplette, ersteRunde.teams().size());

		MeleeSpielRunde zweiteRunde = paarungen.generiereNeuSpielrundeMitFesteTeamGroese(2, 3, meldungen);
		System.out.println(zweiteRunde);
		assertNotNull(zweiteRunde);
		assertEquals(anzTriplette, zweiteRunde.teams().size());

		MeleeSpielRunde dritteRunde = paarungen.generiereNeuSpielrundeMitFesteTeamGroese(3, 3, meldungen);
		System.out.println(dritteRunde);
		assertNotNull(dritteRunde);
		assertEquals(anzTriplette, dritteRunde.teams().size());

		// SpielRunde vierteRunde = paarungen.neueSpielrunde(4, 3, meldungenOhneMitSpieler);
		// System.out.println(vierteRunde);
		// assertNotNull(vierteRunde);
		// assertEquals(4, vierteRunde.teams().size());
		//
		// SpielRunde fuenfteRunde = paarungen.neueSpielrunde(5, 3, meldungenOhneMitSpieler);
		// System.out.println(fuenfteRunde);
		// assertNotNull(fuenfteRunde);
		// assertEquals(4, fuenfteRunde.teams().size());

	}

	private Meldungen newTestMeldungen(int anzSpieler) {
		Meldungen meldungen = new Meldungen();

		for (int i = 1; i <= anzSpieler; i++) {
			Spieler spieler = Spieler.from(i);
			meldungen.addSpielerWennNichtVorhanden(spieler);
		}
		return meldungen;
	}

	private List<Team> newTestTeams(Meldungen meldungen) throws AlgorithmenException {

		ArrayList<Team> teams = new ArrayList<>();

		// team 1
		// 1,2,3
		Team spielerteam1 = new Team(1);
		for (int i = 1; i < 4; i++) {
			spielerteam1.addSpielerWennNichtVorhanden(meldungen.findSpielerByNr(i));
		}

		// team 2
		// 4,5,6
		Team spielerteam2 = new Team(2);
		for (int i = 4; i < 7; i++) {
			spielerteam2.addSpielerWennNichtVorhanden(meldungen.findSpielerByNr(i));
		}
		teams.add(spielerteam1);
		teams.add(spielerteam2);

		return teams;
	}

	private List<PaarungenExpectedAnzahl> testDaten() {
		List<PaarungenExpectedAnzahl> paarungenExpectedAnzahl = new ArrayList<>();

		// paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(4, 2, 0));
		// paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(5, 1, 1));
		// paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(6, 0, 2));
		// paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(8, 4, 0));
		// paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(9, 3, 1));
		// paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(10, 2, 2));
		// paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(11, 1, 3));
		// paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(12, 0, 4));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(13, 5, 1));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(14, 4, 2));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(15, 3, 3));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(16, 2, 4));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(17, 1, 5));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(18, 0, 6));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(19, 5, 3));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(20, 4, 4));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(21, 3, 5));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(22, 2, 6));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(23, 1, 7));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(24, 0, 8));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(25, 5, 5));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(26, 4, 6));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(27, 3, 7));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(28, 2, 8));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(29, 1, 9));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(30, 0, 10));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(31, 5, 7));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(32, 4, 8));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(33, 3, 9));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(34, 2, 10));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(35, 1, 11));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(36, 0, 12));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(37, 5, 9));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(38, 4, 10));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(39, 3, 11));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(40, 2, 12));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(41, 1, 13));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(42, 0, 14));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(43, 5, 11));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(44, 4, 12));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(45, 3, 13));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(46, 2, 14));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(47, 1, 15));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(48, 0, 16));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(49, 5, 13));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(50, 4, 14));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(51, 3, 15));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(52, 2, 16));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(53, 1, 17));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(54, 0, 18));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(55, 5, 15));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(56, 4, 16));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(57, 3, 17));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(58, 2, 18));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(59, 1, 19));
		paarungenExpectedAnzahl.add(new PaarungenExpectedAnzahl(60, 0, 20));

		return paarungenExpectedAnzahl;
	}

}
