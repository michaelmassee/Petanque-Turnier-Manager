package de.petanqueturniermanager.jedergegenjeden.spielplan;

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
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJTestMeldeListeErstellen;

/**
 * UITest fuer den JGJ-Spielplan (Jeder-gegen-Jeden).<br>
 * 5 Teams: 5 Runden Hinrunde (HR-1..HR-15) + 5 Runden Rueckrunde (RR-1..RR-15) = 30 Datenzeilen.<br>
 * Pro Runde: 2 Begegnungen + 1 Freispiel = 3 Zeilen.
 */
public class JGJSpielPlanSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(JGJSpielPlanSheetUITest.class);

	// private Konstanten aus JGJSpielPlanSheet nachgebaut
	private static final int SPIEL_NR_SPALTE = 0; // Spalte A

	// 5 Teams (ungerade): 5 Runden x 3 Zeilen (inkl. Freispiel) = 15 pro Halbzeit
	private static final int ANZ_HR_ZEILEN = 15;
	private static final int ANZ_DATEN_ZEILEN = ANZ_HR_ZEILEN * 2;

	@BeforeEach
	public void testMeldeListeErstellen() throws GenerateException {
		JGJTestMeldeListeErstellen testMeldeListeErstellen = new JGJTestMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListeErstellen.run();
	}

	/**
	 * Prueft, dass das Sheet "Spielplan" nach dem Erstellen existiert.
	 */
	@Test
	public void testSpielplanWirdErstellt() throws GenerateException {
		logger.info("testSpielplanWirdErstellt");
		JGJSpielPlanSheet spielPlan = new JGJSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		XSpreadsheet sheet = spielPlan.getXSpreadSheet();
		assertThat(sheet).as("Sheet 'Spielplan' muss existieren").isNotNull();
	}

	/**
	 * Prueft die Spielnummern-Spalte: HR-1 bis HR-15 (Hinrunde), dann RR-1 bis RR-15 (Rueckrunde).<br>
	 * 5 Teams ergeben 5 Runden x 3 Zeilen = 15 Zeilen pro Halbzeit.
	 */
	@Test
	public void testSpielplanNummern() throws GenerateException {
		logger.info("testSpielplanNummern");
		JGJSpielPlanSheet spielPlan = new JGJSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		int ersteZeile = JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		int letzteZeile = ersteZeile + ANZ_DATEN_ZEILEN - 1;

		RangeData nummern = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(SPIEL_NR_SPALTE, ersteZeile, SPIEL_NR_SPALTE, letzteZeile))
				.getDataFromRange();

		assertThat(nummern).as("Anzahl Datenzeilen").hasSize(ANZ_DATEN_ZEILEN);

		// Hinrunde: HR-1 bis HR-15
		for (int i = 0; i < ANZ_HR_ZEILEN; i++) {
			assertThat(nummern.get(i).get(0).getStringVal())
					.as("Hinrunde Zeile " + (i + 1))
					.isEqualTo("HR-" + (i + 1));
		}
		// Rueckrunde: RR-1 bis RR-15
		for (int i = 0; i < ANZ_HR_ZEILEN; i++) {
			assertThat(nummern.get(ANZ_HR_ZEILEN + i).get(0).getStringVal())
					.as("Rueckrunde Zeile " + (i + 1))
					.isEqualTo("RR-" + (i + 1));
		}
	}

	/**
	 * Prueft die Siege-Formeln (SPIELE_A/B): WENN(SpielPunkteA > SpielPunkteB; 1; 0).<br>
	 * SpielPunkte in erste Datenzeile schreiben, danach Siege-Formeln auslesen.
	 */
	@Test
	public void testSpielplanSiegeFormeln() throws GenerateException {
		logger.info("testSpielplanSiegeFormeln");
		JGJSpielPlanSheet spielPlan = new JGJSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();

		// SpielPunkte in die ersten zwei Datenzeilen schreiben
		// Zeile 1: Team A gewinnt (SpPktA=13, SpPktB=5)
		// Zeile 2: Team B gewinnt (SpPktA=4, SpPktB=13)
		RangeData spielpunkte = new RangeData(new Object[][] { { 13, 5 }, { 4, 13 } });
		RangeHelper.from(spielPlan.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE,
						JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						JGJSpielPlanSheet.SPIELPNKT_B_SPALTE,
						JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + 1))
				.setDataInRange(spielpunkte);

		// Siege-Formeln (SPIELE_A/B) lesen
		RangeData siege = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				RangePosition.from(JGJSpielPlanSheet.SPIELE_A_SPALTE,
						JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						JGJSpielPlanSheet.SPIELE_B_SPALTE,
						JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + 1))
				.getDataFromRange();

		assertThat(siege).as("Zwei Ergebniszeilen erwartet").hasSize(2);
		// Zeile 1: Team A gewinnt (13 > 5)
		assertThat(siege.get(0).get(0).getIntVal()).as("Zeile 1: Siege A (Gewinner)").isEqualTo(1);
		assertThat(siege.get(0).get(1).getIntVal()).as("Zeile 1: Siege B (Verlierer)").isEqualTo(0);
		// Zeile 2: Team B gewinnt (4 < 13)
		assertThat(siege.get(1).get(0).getIntVal()).as("Zeile 2: Siege A (Verlierer)").isEqualTo(0);
		assertThat(siege.get(1).get(1).getIntVal()).as("Zeile 2: Siege B (Gewinner)").isEqualTo(1);
	}
}
