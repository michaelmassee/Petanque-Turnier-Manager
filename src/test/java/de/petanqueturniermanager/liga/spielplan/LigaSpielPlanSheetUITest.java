package de.petanqueturniermanager.liga.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.liga.rangliste.LigaTestMeldeListeErstellen;

/**
 * UITest fuer den Liga-Spielplan.<br>
 * 4 Teams: 3 Runden Hinrunde (HR-1..HR-6) + 3 Runden Rueckrunde (RR-1..RR-6) = 12 Datenzeilen.
 */
public class LigaSpielPlanSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(LigaSpielPlanSheetUITest.class);

	// private Konstanten aus LigaSpielPlanSheet nachgebaut
	private static final int SPIEL_NR_SPALTE = 0; // Spalte A
	private static final int NAME_A_SPALTE = 6; // ORT_SPALTE(5) + 1
	private static final int NAME_B_SPALTE = 7; // NAME_A_SPALTE + 1

	// 4 Teams: 3 Runden x 2 Paarungen = 6 pro Halbzeit, x 2 Halbzeiten = 12 gesamt
	private static final int ANZ_HR_ZEILEN = 6;
	private static final int ANZ_DATEN_ZEILEN = ANZ_HR_ZEILEN * 2;

	@BeforeEach
	public void testMeldeListeErstellen() throws GenerateException {
		LigaTestMeldeListeErstellen testMeldeListeErstellen = new LigaTestMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListeErstellen.run();
	}

	/**
	 * Prueft, dass das Sheet "Spielplan" nach dem Erstellen existiert.
	 */
	@Test
	public void testSpielplanWirdErstellt() throws GenerateException {
		logger.info("testSpielplanWirdErstellt");
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		XSpreadsheet sheet = spielPlan.getXSpreadSheet();
		assertThat(sheet).as("Sheet 'Spielplan' muss existieren").isNotNull();
	}

	/**
	 * Prueft die Spielnummern-Spalte: HR-1 bis HR-6 (Hinrunde), dann RR-1 bis RR-6 (Rueckrunde).
	 */
	@Test
	public void testSpielplanNummern() throws GenerateException {
		logger.info("testSpielplanNummern");
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		int ersteZeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		int letzteZeile = ersteZeile + ANZ_DATEN_ZEILEN - 1;

		RangeData nummern = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(SPIEL_NR_SPALTE, ersteZeile, SPIEL_NR_SPALTE, letzteZeile))
				.getDataFromRange();

		assertThat(nummern).as("Anzahl Datenzeilen").hasSize(ANZ_DATEN_ZEILEN);

		// Hinrunde: HR-1 bis HR-6
		for (int i = 0; i < ANZ_HR_ZEILEN; i++) {
			assertThat(nummern.get(i).get(0).getStringVal())
					.as("Hinrunde Zeile " + (i + 1))
					.isEqualTo("HR-" + (i + 1));
		}
		// Rueckrunde: RR-1 bis RR-6
		for (int i = 0; i < ANZ_HR_ZEILEN; i++) {
			assertThat(nummern.get(ANZ_HR_ZEILEN + i).get(0).getStringVal())
					.as("Rueckrunde Zeile " + (i + 1))
					.isEqualTo("RR-" + (i + 1));
		}
	}

	/**
	 * Prueft, dass die VLOOKUP-Formeln in den Teamnamen-Spalten korrekt auf die Meldeliste verweisen.<br>
	 * Feste Paarung (Team 1=Alpha vs Team 4=Delta) in Arbeitsspalten schreiben,
	 * danach werden die Teamnamen aus der Meldeliste gelesen.
	 */
	@Test
	public void testSpielplanTeamNamenViaVLOOKUP() throws GenerateException {
		logger.info("testSpielplanTeamNamenViaVLOOKUP");
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		// Feste Paarung in erste Datenzeile schreiben: Team 1 (Alpha) vs Team 4 (Delta)
		RangeData paarung = new RangeData(new Object[][] { { 1, 4 } });
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						LigaSpielPlanSheet.TEAM_B_NR_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE))
				.setDataInRange(paarung);

		// Teamnamen-Formeln auslesen (NAME_A_SPALTE=6, NAME_B_SPALTE=7)
		RangeData namesData = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(NAME_A_SPALTE, LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						NAME_B_SPALTE, LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE))
				.getDataFromRange();

		assertThat(namesData).as("Genau eine Datenzeile erwartet").hasSize(1);
		assertThat(namesData.get(0).get(0).getStringVal()).as("Heim-Team (Nr. 1 = Alpha)").isEqualTo("Alpha");
		assertThat(namesData.get(0).get(1).getStringVal()).as("Gast-Team (Nr. 4 = Delta)").isEqualTo("Delta");
	}

	/**
	 * Prueft, dass die Punkte-Formeln korrekt berechnen: 1 fuer den Gewinner, 0 fuer den Verlierer.<br>
	 * Formel: WENN(SiegeA > SiegeB; 1; 0)
	 */
	@Test
	public void testSpielplanPunkteFormeln() throws GenerateException {
		logger.info("testSpielplanPunkteFormeln");
		LigaSpielPlanSheet spielPlan = new LigaSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		// Siege in die ersten zwei Zeilen schreiben
		// Zeile 1: Team A gewinnt (SiegeA=1, SiegeB=0)
		// Zeile 2: Team B gewinnt (SiegeA=0, SiegeB=1)
		RangeData siege = new RangeData(new Object[][] { { 1, 0 }, { 0, 1 } });
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(LigaSpielPlanSheet.SPIELE_A_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						LigaSpielPlanSheet.SPIELE_B_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + 1))
				.setDataInRange(siege);

		// Punkte-Formeln lesen
		RangeData punkte = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(LigaSpielPlanSheet.PUNKTE_A_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						LigaSpielPlanSheet.PUNKTE_B_SPALTE,
						LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + 1))
				.getDataFromRange();

		assertThat(punkte).as("Zwei Ergebniszeilen erwartet").hasSize(2);
		// Zeile 1: Team A gewinnt
		assertThat(punkte.get(0).get(0).getIntVal()).as("Zeile 1: Punkte A (Gewinner)").isEqualTo(1);
		assertThat(punkte.get(0).get(1).getIntVal()).as("Zeile 1: Punkte B (Verlierer)").isEqualTo(0);
		// Zeile 2: Team B gewinnt
		assertThat(punkte.get(1).get(0).getIntVal()).as("Zeile 2: Punkte A (Verlierer)").isEqualTo(0);
		assertThat(punkte.get(1).get(1).getIntVal()).as("Zeile 2: Punkte B (Gewinner)").isEqualTo(1);
	}
}
