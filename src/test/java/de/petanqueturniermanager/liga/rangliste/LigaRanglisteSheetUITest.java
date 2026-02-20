package de.petanqueturniermanager.liga.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;

/**
 * UITest fuer die Liga-Rangliste.<br>
 * Sortierkriterien: 1. Punkte+, 2. Spiele+, 3. Spielpunkte+, 4. Spielpunkte Δ, 5. Direktvergleich (manuell), 6. das Los (manuell)
 */
public class LigaRanglisteSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(LigaRanglisteSheetUITest.class);

	private static final String LIGA_RANGLISTE_JSON = "LigaRangliste.json";

	@Before
	public void testMeldeListeErstellen() throws GenerateException {
		LigaTestMeldeListeErstellen testMeldeListeErstellen = new LigaTestMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListeErstellen.run();
	}

	@Test
	public void testRangliste() throws GenerateException, IOException {
		// Spielplan erstellen
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		// Paarungen fest vorgeben (deterministisch), dann keine Abhaengigkeit vom Shuffle
		// 4 Teams, JederGegenJeden: 3 Runden Hin- und 3 Runden Rueckrunde = 12 Zeilen
		// TEAM_A_NR_SPALTE=14, TEAM_B_NR_SPALTE=15, ERSTE_SPIELTAG_DATEN_ZEILE=2
		RangeData paarungen = new RangeData(SPIELPAARUNGEN_HR);
		paarungen.addData(SPIELPAARUNGEN_RR);
		RangePosition rangePaarungen = RangePosition.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
				LigaSpielPlanSheet.TEAM_A_NR_SPALTE + paarungen.getAnzSpalten() - 1,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + paarungen.size() - 1);
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangePaarungen)
				.setDataInRange(paarungen);

		// Ergebnisse eintragen
		// Spalten: PUNKTE_A=8, PUNKTE_B=9, SPIELE_A=10, SPIELE_B=11, SPIELPNKT_A=12, SPIELPNKT_B=13
		RangeData ergebnisse = new RangeData(ERGEBNISSE_HR);
		ergebnisse.addData(ERGEBNISSE_RR);
		RangePosition rangeErgebnisse = RangePosition.from(LigaSpielPlanSheet.PUNKTE_A_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
				LigaSpielPlanSheet.SPIELPNKT_B_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + ergebnisse.size() - 1);
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeErgebnisse)
				.setDataInRange(ergebnisse);

		// Rangliste erstellen
		LigaRanglisteSheet rangliste = new LigaRanglisteSheet(wkingSpreadsheet);
		rangliste.run();

		// Beim ersten Lauf auskommentieren und writeToJson aktivieren:
		// writeLigaRanglisteToJson(rangliste);
		validateLigaRanglisteToJson(rangliste);
	}

	private void validateLigaRanglisteToJson(LigaRanglisteSheet rangliste) throws GenerateException {
		logger.info("validateLigaRanglisteToJson");
		RangeData data = getRanglisteRange(rangliste).getDataFromRange();
		InputStream jsonFile = LigaRanglisteSheetUITest.class.getResourceAsStream(LIGA_RANGLISTE_JSON);
		validateWithJson(data, jsonFile);
	}

	@SuppressWarnings("unused")
	private void writeLigaRanglisteToJson(LigaRanglisteSheet rangliste) throws GenerateException {
		RangeData data = getRanglisteRange(rangliste).getDataFromRange();
		writeToJson(LIGA_RANGLISTE_JSON, data);
	}

	private RangeHelper getRanglisteRange(LigaRanglisteSheet rangliste) throws GenerateException {
		// 4 Sortspalten + Validate-Spalte
		int endeSpalte = rangliste.getManuellSortSpalte() + rangliste.getRanglisteSpalten().size();
		RangePosition range = RangePosition.from(rangliste.getErsteSpalte(), rangliste.getErsteDatenZiele(),
				endeSpalte, rangliste.sucheLetzteZeileMitSpielerNummer());
		assertThat(range.getAddress()).as("Rangliste-Bereich").isNotNull();
		logger.info("Rangliste Bereich: " + range.getAddress());
		return RangeHelper.from(rangliste.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(), range);
	}

	/**
	 * Prüft Sortierung nach dem 3. Kriterium: Spielpunkte +<br>
	 * Beta(2) und Gamma(3) haben gleiche Punkte+(6) und Spiele+(3),<br>
	 * aber Beta hat mehr Spielpunkte+(66) als Gamma(53).<br>
	 * Erwartete Reihenfolge: Alpha(1), Beta(2), Gamma(3), Delta(4)
	 */
	@Test
	public void testSortierungNachSpielPunktePlus() throws GenerateException {
		// Spielplan erstellen
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		// Paarungen eintragen
		RangeData paarungen = new RangeData(SPIELPAARUNGEN_HR);
		paarungen.addData(SPIELPAARUNGEN_RR);
		RangePosition rangePaarungen = RangePosition.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
				LigaSpielPlanSheet.TEAM_A_NR_SPALTE + paarungen.getAnzSpalten() - 1,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + paarungen.size() - 1);
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangePaarungen)
				.setDataInRange(paarungen);

		// Ergebnisse: Beta(2) und Gamma(3) gleich bei Punkte+(6) und Spiele+(3),
		// aber Beta hat mehr Spielpunkte+(66) als Gamma(53)
		RangeData ergebnisse = new RangeData(ERGEBNISSE_SPPKT_HR);
		ergebnisse.addData(ERGEBNISSE_SPPKT_RR);
		RangePosition rangeErgebnisse = RangePosition.from(LigaSpielPlanSheet.PUNKTE_A_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
				LigaSpielPlanSheet.SPIELPNKT_B_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + ergebnisse.size() - 1);
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeErgebnisse)
				.setDataInRange(ergebnisse);

		// Rangliste erstellen
		LigaRanglisteSheet rangliste = new LigaRanglisteSheet(wkingSpreadsheet);
		rangliste.run();

		// Team-Nummern in Rang-Reihenfolge lesen
		int ersteDatenZeile = rangliste.getErsteDatenZiele();
		int letzteZeile = rangliste.sucheLetzteZeileMitSpielerNummer();
		RangeData teamNrData = RangeHelper.from(rangliste.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(0, ersteDatenZeile, 0, letzteZeile)).getDataFromRange();

		// Erwartete Reihenfolge: Alpha(1), Beta(2), Gamma(3), Delta(4)
		assertThat(teamNrData).hasSize(4);
		assertThat(teamNrData.get(0).get(0).getIntVal()).as("Rang 1: Alpha").isEqualTo(1);
		assertThat(teamNrData.get(1).get(0).getIntVal()).as("Rang 2: Beta (mehr SpPkt+)").isEqualTo(2);
		assertThat(teamNrData.get(2).get(0).getIntVal()).as("Rang 3: Gamma (weniger SpPkt+)").isEqualTo(3);
		assertThat(teamNrData.get(3).get(0).getIntVal()).as("Rang 4: Delta").isEqualTo(4);

		// Summen-Spalten lesen: Punkte+(0), Spiele+(2), SpPkt+(5)
		int ersteSumme = rangliste.getErsteSummeSpalte();
		RangeData summenData = RangeHelper.from(rangliste.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(ersteSumme, ersteDatenZeile, ersteSumme + 7, letzteZeile)).getDataFromRange();

		RowData betaSummen = summenData.get(1);
		RowData gammaSummen = summenData.get(2);

		// Punkte+ und Spiele+ muessen gleich sein (1. und 2. Kriterium)
		assertThat(betaSummen.get(0).getIntVal()).as("Beta Punkte+").isEqualTo(6);
		assertThat(gammaSummen.get(0).getIntVal()).as("Gamma Punkte+").isEqualTo(6);
		assertThat(betaSummen.get(2).getIntVal()).as("Beta Spiele+").isEqualTo(3);
		assertThat(gammaSummen.get(2).getIntVal()).as("Gamma Spiele+").isEqualTo(3);

		// Spielpunkte+ ist der Tiebreaker (3. Kriterium): Beta(66) > Gamma(53)
		assertThat(betaSummen.get(5).getIntVal()).as("Beta SpPkt+").isEqualTo(66);
		assertThat(gammaSummen.get(5).getIntVal()).as("Gamma SpPkt+").isEqualTo(53);
	}

	// @formatter:off

	// Paarungen Hinrunde: Team A Nr, Team B Nr
	// 4 Teams, 3 Runden, 2 Paarungen pro Runde
	private static final Object[][] SPIELPAARUNGEN_HR = new Object[][] {
		// Runde 1
		{ 1, 4 },
		{ 2, 3 },
		// Runde 2
		{ 1, 3 },
		{ 4, 2 },
		// Runde 3
		{ 1, 2 },
		{ 3, 4 }
	};

	// Paarungen Rueckrunde (Teams getauscht)
	private static final Object[][] SPIELPAARUNGEN_RR = new Object[][] {
		// Runde 4
		{ 4, 1 },
		{ 3, 2 },
		// Runde 5
		{ 3, 1 },
		{ 2, 4 },
		// Runde 6
		{ 2, 1 },
		{ 4, 3 }
	};

	// Ergebnisse Hinrunde: PunkteA, PunkteB, SpieleA, SpieleB, SpielPunkteA, SpielPunkteB
	// Team 1 gewinnt alle HR-Spiele, Team 3 gewinnt gegen Team 4
	private static final Object[][] ERGEBNISSE_HR = new Object[][] {
		// Runde 1: (1,4) -> 1 gewinnt, (2,3) -> 2 gewinnt
		{ 2, 0, 1, 0, 13,  5 },
		{ 2, 0, 1, 0, 13,  8 },
		// Runde 2: (1,3) -> 1 gewinnt, (4,2) -> 2 gewinnt
		{ 2, 0, 1, 0, 13,  6 },
		{ 0, 2, 0, 1,  7, 13 },
		// Runde 3: (1,2) -> 1 gewinnt, (3,4) -> 3 gewinnt
		{ 2, 0, 1, 0, 13,  9 },
		{ 2, 0, 1, 0, 13,  4 }
	};

	// Ergebnisse Rueckrunde: Gleiche Ergebnisse wie HR, Teams getauscht
	private static final Object[][] ERGEBNISSE_RR = new Object[][] {
		// Runde 4: (4,1) -> 1 gewinnt als B, (3,2) -> 2 gewinnt als B
		{ 0, 2, 0, 1,  5, 13 },
		{ 0, 2, 0, 1,  8, 13 },
		// Runde 5: (3,1) -> 1 gewinnt als B, (2,4) -> 2 gewinnt als A
		{ 0, 2, 0, 1,  6, 13 },
		{ 2, 0, 1, 0, 13,  7 },
		// Runde 6: (2,1) -> 1 gewinnt als B, (4,3) -> 3 gewinnt als B
		{ 0, 2, 0, 1,  9, 13 },
		{ 0, 2, 0, 1,  4, 13 }
	};

	/**
	 * Prüft Sortierung nach dem 4. Kriterium: Spielpunkte Δ<br>
	 * Beta(2) und Gamma(3) haben gleiche Punkte+(6), Spiele+(3) und Spielpunkte+(58),<br>
	 * aber Beta hat SpPkt-Delta=+6, Gamma hat SpPkt-Delta=0.<br>
	 * Erwartete Reihenfolge: Alpha(1), Beta(2), Gamma(3), Delta(4)
	 */
	@Test
	public void testSortierungNachSpielPunkteDelta() throws GenerateException {
		// Spielplan erstellen
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		// Paarungen eintragen
		RangeData paarungen = new RangeData(SPIELPAARUNGEN_HR);
		paarungen.addData(SPIELPAARUNGEN_RR);
		RangePosition rangePaarungen = RangePosition.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
				LigaSpielPlanSheet.TEAM_A_NR_SPALTE + paarungen.getAnzSpalten() - 1,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + paarungen.size() - 1);
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangePaarungen)
				.setDataInRange(paarungen);

		// Ergebnisse: Beta(2) und Gamma(3) gleich bei Punkte+(6), Spiele+(3) und SpPkt+(58),
		// aber Beta hat SpPkt-Delta=+6, Gamma hat SpPkt-Delta=0
		RangeData ergebnisse = new RangeData(ERGEBNISSE_SPPKT_DELTA_HR);
		ergebnisse.addData(ERGEBNISSE_SPPKT_DELTA_RR);
		RangePosition rangeErgebnisse = RangePosition.from(LigaSpielPlanSheet.PUNKTE_A_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
				LigaSpielPlanSheet.SPIELPNKT_B_SPALTE,
				LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + ergebnisse.size() - 1);
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeErgebnisse)
				.setDataInRange(ergebnisse);

		// Rangliste erstellen
		LigaRanglisteSheet rangliste = new LigaRanglisteSheet(wkingSpreadsheet);
		rangliste.run();

		// Team-Nummern in Rang-Reihenfolge lesen
		int ersteDatenZeile = rangliste.getErsteDatenZiele();
		int letzteZeile = rangliste.sucheLetzteZeileMitSpielerNummer();
		RangeData teamNrData = RangeHelper.from(rangliste.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(0, ersteDatenZeile, 0, letzteZeile)).getDataFromRange();

		// Erwartete Reihenfolge: Alpha(1), Beta(2), Gamma(3), Delta(4)
		assertThat(teamNrData).hasSize(4);
		assertThat(teamNrData.get(0).get(0).getIntVal()).as("Rang 1: Alpha").isEqualTo(1);
		assertThat(teamNrData.get(1).get(0).getIntVal()).as("Rang 2: Beta (SpPkt-Delta +6)").isEqualTo(2);
		assertThat(teamNrData.get(2).get(0).getIntVal()).as("Rang 3: Gamma (SpPkt-Delta 0)").isEqualTo(3);
		assertThat(teamNrData.get(3).get(0).getIntVal()).as("Rang 4: Delta").isEqualTo(4);

		// Summen-Spalten lesen: Punkte+(0), Spiele+(2), SpPkt+(5), SpPktDelta(7)
		int ersteSumme = rangliste.getErsteSummeSpalte();
		RangeData summenData = RangeHelper.from(rangliste.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(ersteSumme, ersteDatenZeile, ersteSumme + 7, letzteZeile)).getDataFromRange();

		RowData betaSummen = summenData.get(1);
		RowData gammaSummen = summenData.get(2);

		// Punkte+, Spiele+ und Spielpunkte+ muessen gleich sein (1., 2. und 3. Kriterium)
		assertThat(betaSummen.get(0).getIntVal()).as("Beta Punkte+").isEqualTo(6);
		assertThat(gammaSummen.get(0).getIntVal()).as("Gamma Punkte+").isEqualTo(6);
		assertThat(betaSummen.get(2).getIntVal()).as("Beta Spiele+").isEqualTo(3);
		assertThat(gammaSummen.get(2).getIntVal()).as("Gamma Spiele+").isEqualTo(3);
		assertThat(betaSummen.get(5).getIntVal()).as("Beta SpPkt+").isEqualTo(58);
		assertThat(gammaSummen.get(5).getIntVal()).as("Gamma SpPkt+").isEqualTo(58);

		// SpPkt-Delta ist der Tiebreaker (4. Kriterium): Beta(+6) > Gamma(0)
		assertThat(betaSummen.get(7).getIntVal()).as("Beta SpPkt-Delta").isEqualTo(6);
		assertThat(gammaSummen.get(7).getIntVal()).as("Gamma SpPkt-Delta").isEqualTo(0);
	}

	// Ergebnisse Hinrunde fuer SpielPunkte+ Test
	// Team 1 gewinnt alle, Team 4 verliert alle
	// Team 2 und 3: gleiche Punkte+(6) und Spiele+(3), aber Beta SpPkt+(66) > Gamma SpPkt+(53)
	// Team 2 gewinnt gegen Team 3 (HR) und Team 4 (HR), verliert gegen Team 1 (HR)
	// Team 3 gewinnt gegen Team 4 (HR), verliert gegen Team 1 und Team 2 (HR)
	private static final Object[][] ERGEBNISSE_SPPKT_HR = new Object[][] {
		// Runde 1: (1,4) → 1 gewinnt, (2,3) → 2 gewinnt
		{ 2, 0, 1, 0, 13,  5 },
		{ 2, 0, 1, 0, 13,  8 },
		// Runde 2: (1,3) → 1 gewinnt, (4,2) → 2 gewinnt als B
		{ 2, 0, 1, 0, 13,  6 },
		{ 0, 2, 0, 1,  5, 13 },
		// Runde 3: (1,2) → 1 gewinnt, (3,4) → 3 gewinnt
		{ 2, 0, 1, 0, 13,  9 },
		{ 2, 0, 1, 0, 10,  5 }
	};

	// Ergebnisse Rueckrunde fuer SpielPunkte+ Test
	// Team 3 gewinnt gegen Team 2 (RR) und Team 4 (RR), verliert gegen Team 1 (RR)
	// Team 2 gewinnt gegen Team 4 (RR), verliert gegen Team 1 und Team 3 (RR)
	private static final Object[][] ERGEBNISSE_SPPKT_RR = new Object[][] {
		// Runde 4: (4,1) → 1 gewinnt als B, (3,2) → 3 gewinnt
		{ 0, 2, 0, 1,  5, 13 },
		{ 2, 0, 1, 0, 13,  9 },
		// Runde 5: (3,1) → 1 gewinnt als B, (2,4) → 2 gewinnt
		{ 0, 2, 0, 1,  6, 13 },
		{ 2, 0, 1, 0, 13,  5 },
		// Runde 6: (2,1) → 1 gewinnt als B, (4,3) → 3 gewinnt als B
		{ 0, 2, 0, 1,  9, 13 },
		{ 0, 2, 0, 1,  5, 10 }
	};

	// Ergebnisse fuer SpielPunkte-Delta Test
	// Alpha gewinnt alle, Delta verliert alle
	// Beta und Gamma: gleiche Punkte+(6), Spiele+(3), SpPkt+(58), aber Beta SpPktΔ(+6) > Gamma SpPktΔ(0)
	// Gamma schlaegt Beta in HR (13:9), Beta schlaegt Gamma in RR (13:3)
	// Alpha schlaegt Beta mit 13:5 (beide Male), Alpha schlaegt Gamma mit 13:8 (beide Male)
	private static final Object[][] ERGEBNISSE_SPPKT_DELTA_HR = new Object[][] {
		// Runde 1: (1,4) → Alpha gewinnt, (2,3) → Gamma gewinnt
		{ 2, 0, 1, 0, 13,  5 },
		{ 0, 2, 0, 1,  9, 13 },
		// Runde 2: (1,3) → Alpha gewinnt, (4,2) → Beta gewinnt als B
		{ 2, 0, 1, 0, 13,  8 },
		{ 0, 2, 0, 1,  5, 13 },
		// Runde 3: (1,2) → Alpha gewinnt, (3,4) → Gamma gewinnt
		{ 2, 0, 1, 0, 13,  5 },
		{ 2, 0, 1, 0, 13,  5 }
	};

	private static final Object[][] ERGEBNISSE_SPPKT_DELTA_RR = new Object[][] {
		// Runde 4: (4,1) → Alpha gewinnt als B, (3,2) → Beta gewinnt als B
		{ 0, 2, 0, 1,  5, 13 },
		{ 0, 2, 0, 1,  3, 13 },
		// Runde 5: (3,1) → Alpha gewinnt als B, (2,4) → Beta gewinnt
		{ 0, 2, 0, 1,  8, 13 },
		{ 2, 0, 1, 0, 13,  5 },
		// Runde 6: (2,1) → Alpha gewinnt als B, (4,3) → Gamma gewinnt als B
		{ 0, 2, 0, 1,  5, 13 },
		{ 0, 2, 0, 1,  5, 13 }
	};

	// @formatter:on
}
