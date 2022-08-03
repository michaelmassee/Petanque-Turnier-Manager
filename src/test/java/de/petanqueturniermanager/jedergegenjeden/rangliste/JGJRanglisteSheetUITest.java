package de.petanqueturniermanager.jedergegenjeden.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJPropertiesSpalte;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheetUITest;

public class JGJRanglisteSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheetUITest.class);

	private DocumentPropertiesHelper docPropHelper;

	@Before
	public void testMeldeListeErstelln() throws GenerateException {
		JGJTestMeldeListeErstellen testMeldeListeErstellen = new JGJTestMeldeListeErstellen(wkingSpreadsheet, doc);

		int anzMeldungen = testMeldeListeErstellen.run();
		this.docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN, true);
		docPropHelper.setStringProperty(JGJPropertiesSpalte.KONFIG_PROP_NAME_GRUPPE, "Gruppe Test");
	}

	@Test
	public void testRangliste() throws GenerateException, IOException {
		// spielplan 
		JGJSpielPlanSheet spielPlan = new JGJSpielPlanSheet(wkingSpreadsheet);
		spielPlan.run();// no thread

		// paarungen fest vorgeben, sonnst kein Validate
		RangeData spielpaarungen = new RangeData(SPIELPAARUNGEN_HR);
		spielpaarungen.addData(SPIELPAARUNGEN_RR);
		RangePosition rangeSpielPaarungen = RangePosition.from(JGJSpielPlanSheet.TEAM_A_NR_SPALTE,
				JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
				JGJSpielPlanSheet.TEAM_A_NR_SPALTE + spielpaarungen.getAnzSpalten() - 1, // Team A + B
				JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + spielpaarungen.size() - 1);
		RangeHelper rngHlprSpielPaarungen = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeSpielPaarungen);
		rngHlprSpielPaarungen.setDataInRange(spielpaarungen);

		// ergebnisse einfuegen
		RangeData ergebnisseDataRunde = new RangeData(TESTDATARUNDE1);
		ergebnisseDataRunde.addData(TESTDATARUNDE2);
		ergebnisseDataRunde.addData(TESTDATARUNDE3);
		ergebnisseDataRunde.addData(TESTDATARUNDE4);
		ergebnisseDataRunde.addData(TESTDATARUNDE5);
		ergebnisseDataRunde.addData(TESTDATARUNDE6);
		ergebnisseDataRunde.addData(TESTDATARUNDE7);
		ergebnisseDataRunde.addData(TESTDATARUNDE8);
		ergebnisseDataRunde.addData(TESTDATARUNDE9);
		ergebnisseDataRunde.addData(TESTDATARUNDE10);
		RangePosition rangeSpielErg = RangePosition.from(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE,
				JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
				JGJSpielPlanSheet.SPIELPNKT_A_SPALTE + ergebnisseDataRunde.getAnzSpalten() - 1, // Team A + B
				JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE + ergebnisseDataRunde.size() - 1);
		RangeHelper rngHlprSpielErg = RangeHelper.from(spielPlan.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeSpielErg);
		rngHlprSpielErg.setDataInRange(ergebnisseDataRunde);

		// rangliste erstellen
		JGJRanglisteSheet rangliste = new JGJRanglisteSheet(wkingSpreadsheet);
		rangliste.run();

		//writeJGJRanglisteToJson(rangliste);
		validateJGJRanglisteToJson(rangliste);

		JGJRanglisteDirektvergleichSheet direktVergleich = new JGJRanglisteDirektvergleichSheet(wkingSpreadsheet);
		direktVergleich.run();

		//writeJGJDirektVergleichToJson(direktVergleich);
		validateJGJDirektVergleichToJson(direktVergleich);

		// waitEnter();
	}

	private void validateJGJRanglisteToJson(JGJRanglisteSheet ranglist) throws GenerateException {
		logger.info("validateJGJRanglisteToJson");
		RangeData jgjRangliste = getRanglisteRange(ranglist).getDataFromRange();
		InputStream jsonFile = JGJRanglisteSheetUITest.class.getResourceAsStream("JGJRangliste.json");
		validateWithJson(jgjRangliste, jsonFile);
	}

	private void writeJGJRanglisteToJson(JGJRanglisteSheet ranglist) throws GenerateException {
		RangeData vertikalErgRange = getRanglisteRange(ranglist).getDataFromRange();
		writeToJson("JGJRangliste.json", vertikalErgRange);
	}

	private RangeHelper getRanglisteRange(JGJRanglisteSheet ranglist) throws GenerateException {
		RangePosition rangeSplrErg = RangePosition.from(ranglist.getErsteSpalte(), ranglist.getErsteDatenZiele(),
				ranglist.getManuellSortSpalte() + 3, ranglist.sucheLetzteZeileMitSpielerNummer());
		assertThat(rangeSplrErg.getAddress()).isEqualTo("A4:BD8");
		RangeHelper rngHlpr = RangeHelper.from(ranglist.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeSplrErg);
		return rngHlpr;
	}

	private void validateJGJDirektVergleichToJson(JGJRanglisteDirektvergleichSheet ranglist) throws GenerateException {
		logger.info("validateJGJDirektVergleichToJson");
		RangeData jgjRangliste = getDirektRange(ranglist).getDataFromRange();
		InputStream jsonFile = JGJRanglisteSheetUITest.class.getResourceAsStream("JGJDirektVergleich.json");
		validateWithJson(jgjRangliste, jsonFile);
	}

	private void writeJGJDirektVergleichToJson(JGJRanglisteDirektvergleichSheet ranglist) throws GenerateException {
		RangeData vertikalErgRange = getDirektRange(ranglist).getDataFromRange();
		writeToJson("JGJDirektVergleich.json", vertikalErgRange);
	}

	private RangeHelper getDirektRange(JGJRanglisteDirektvergleichSheet ranglist) throws GenerateException {
		RangePosition rangeSplrErg = RangePosition.from(JGJRanglisteDirektvergleichSheet.TEAM_NR_SPALTE,
				JGJRanglisteDirektvergleichSheet.ERSTE_DATEN_ZEILE,
				JGJRanglisteDirektvergleichSheet.TEAM_NR_SPALTE + ranglist.anzTeams() + 1,
				JGJRanglisteDirektvergleichSheet.ERSTE_DATEN_ZEILE + ranglist.anzTeams() - 1);
		assertThat(rangeSplrErg.getAddress()).isEqualTo("A2:G6");
		RangeHelper rngHlpr = RangeHelper.from(ranglist.getXSpreadSheet(),
				wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeSplrErg);
		return rngHlpr;
	}

	//@formatter:off
	
	private static final Object[][] SPIELPAARUNGEN_HR = new Object[][] { 
		    { 5, 0 },// freispiel
			{ 1, 4 }, 
			{ 2, 3 },
			{ 2, 0 },// freispiel
			{ 3, 1 },
			{ 4, 5 },
			{ 3, 0 },// freispiel
			{ 4, 2 },
			{ 5, 1 },
			{ 1, 0 },// freispiel
			{ 2, 5 },
			{ 3, 4 },
			{ 4, 0 },// freispiel
			{ 5, 3 },
			{ 1, 2 }
	};
	
	private static final Object[][] SPIELPAARUNGEN_RR = new Object[][] { 
	    { 5, 0 },// freispiel
		{ 4, 1 }, 
		{ 3, 2 },
		{ 2, 0 },// freispiel
		{ 1, 3 },
		{ 5, 4 },
		{ 3, 0 },// freispiel
		{ 2, 4 },
		{ 1, 5 },
		{ 1, 0 },// freispiel
		{ 5, 2 },
		{ 4, 3 },
		{ 4, 0 },// freispiel
		{ 3, 5 },
		{ 2, 1 }
	};
	
	
	
	private static final Object[][] TESTDATARUNDE1 = new Object[][] { 
		    { null, null }, // freispiel E
			{ 0, 13 }, // A-D
			{ 13, 10 } }; // B-C
	private static final Object[][] TESTDATARUNDE2 = new Object[][] {
			{ null, null }, // freispiel B 
			{ 6, 13 }, // C-A
			{ 13, 4 } }; // D-E
	private static final Object[][] TESTDATARUNDE3 = new Object[][] {
			{ null, null }, // freispiel C 
			{ 0, 13 },  // D-B
			{ 13, 5 } }; // E-A
	private static final Object[][] TESTDATARUNDE4 = new Object[][] {
			{ null, null }, // freispiel A 
			{ 13, 8 },   // B-E
			{ 12, 13 } }; // C-D
	private static final Object[][] TESTDATARUNDE5 = new Object[][] {
			{ null, null }, // freispiel D 
			{ 13, 4 },  // E-C
			{ 13, 2 } }; // A-B
			
	private static final Object[][] TESTDATARUNDE6 = new Object[][] { 
		    { null, null }, // freispiel E
			{ 2, 13 }, // A-D
			{ 10, 13 } }; // B-C
	private static final Object[][] TESTDATARUNDE7= new Object[][] {
			{ null, null }, // freispiel B 
			{ 4, 13 }, // C-A
			{ 13, 12 } }; // D-E
	private static final Object[][] TESTDATARUNDE8= new Object[][] {
			{ null, null }, // freispiel C 
			{ 3, 13 },  // D-B
			{ 12, 13 } }; // E-A
	private static final Object[][] TESTDATARUNDE9 = new Object[][] {
			{ null, null }, // freispiel A 
			{ 2, 13 },   // B-E
			{ 10, 13 } }; // C-D
	private static final Object[][] TESTDATARUNDE10 = new Object[][] {
			{ null, null }, // freispiel D 
			{ 6, 13 },  // E-C
			{ 0, 13 } }; // A-B			
			
			
	//@formatter:on

}