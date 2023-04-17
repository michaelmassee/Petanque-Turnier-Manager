package de.petanqueturniermanager.supermelee.endrangliste;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.supermelee.RanglisteTestDaten;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.konfiguration.SuprMleEndranglisteSortMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.TestMeldeListeErstellen;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

/**
 * Erstellung 28.03.2023 / Michael Massee
 */

public class EndranglisteSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(EndranglisteSheetUITest.class);

	private DocumentPropertiesHelper docPropHelper;

	private MeldeListeSheet_NeuerSpieltag meldeListeSheet_NeuerSpieltag;
	private SpieltagRanglisteSheet spieltagRangliste;
	private TestMeldeListeErstellen testMeldeListeErstellen;
	private EndranglisteSheet endranglisteSheet;
	private EndranglisteSheet_Sort endranglisteSheetSort;
	private RanglisteTestDaten<EndranglisteSheetUITest> ranglisteTestDaten;

	private static final int ANZ_MELDUNGEN = 20;
	private static final int ANZ_RUNDEN = 3;
	private static final int ANZ_SPIELTAGE = 3;

	private static final String ENDRANGLISTESHEETUITEST_REFFILE = "EndranglisteSheetUITestRef.json";
	private static final String ENDRANGLISTESHEETUITEST_REFFILE_VALIDATE = "EndranglisteSheetUITestValidateRef.json";
	private static final String ENDRANGLISTESHEETUITEST_ANZ_TAGE_REFFILE = "EndranglisteSheetUITestRefSortByAnzTage.json";
	private static final String ENDRANGLISTESHEETUITEST_ANZ_TAGE_REFFILE_VALIDATE = "EndranglisteSheetUITestValidateRefSortByAnzTage.json";

	@Before
	public void testMeldeListeErstellen() throws GenerateException {
		// erst mal eine meldeListe erstellen
		testMeldeListeErstellen = new TestMeldeListeErstellen(wkingSpreadsheet, doc);
		this.docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN, true);
		spieltagRangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
		endranglisteSheetSort = new EndranglisteSheet_Sort(wkingSpreadsheet);
		meldeListeSheet_NeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet);
		ranglisteTestDaten = new RanglisteTestDaten<>(wkingSpreadsheet, sheetHlp, this);
		endranglisteSheet = new EndranglisteSheet(wkingSpreadsheet);
	}

	@Test
	public void testRanglisteSortierungOK() throws GenerateException, IOException {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);

		// testrunden erstellen
		for (int i = 1; i <= ANZ_SPIELTAGE; i++) {
			SpielTagNr spieltag = SpielTagNr.from(i);
			meldeListeSheet_NeuerSpieltag.setAktiveSpieltag(spieltag); // in konfig speichern
			meldeListeSheet_NeuerSpieltag.setAktiveSpielRunde(SpielRundeNr.from(1)); // in konfig speichern

			testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(spieltag);
			if (i == 1) { // 2 mit 1 spieltag weniger
				Position clear = Position.from(3, 10); // D:11  Gerhard Niko 
				sheetHlp.setStringValueInCell(StringCellValue.from(meldeListeSheet_NeuerSpieltag, clear).setValue(""));
				Position clear2 = Position.from(3, 11); // D:12  Grau Franka  
				sheetHlp.setStringValueInCell(StringCellValue.from(meldeListeSheet_NeuerSpieltag, clear2).setValue(""));
			}
			ranglisteTestDaten.erstelleTestSpielrunden(ANZ_RUNDEN, false, spieltag); // von json dateien laden
			spieltagRangliste.run(); // rangliste erstellen
		}
		endranglisteSheet.run();

		RangeHelper rngHlpr = endranglisteRange(endranglisteSheet.getXSpreadSheet());
		RangeHelper rngHlprValidate = endranglisteRangeValidateSpalten(endranglisteSheet.getXSpreadSheet());

		// save ref file 
		// writeToJson(ENDRANGLISTESHEETUITEST_REFFILE, rngHlpr.getDataFromRange());
		// writeToJson(ENDRANGLISTESHEETUITEST_REFFILE_VALIDATE, rngHlprValidate.getDataFromRange());

		// compare ref file
		try (InputStream jsonFile = EndranglisteSheetUITest.class
				.getResourceAsStream(ENDRANGLISTESHEETUITEST_REFFILE_VALIDATE)) {
			validateWithJson(rngHlprValidate.getDataFromRange(), jsonFile);
		}

		// compare validate file
		try (InputStream jsonFile = EndranglisteSheetUITest.class
				.getResourceAsStream(ENDRANGLISTESHEETUITEST_REFFILE)) {
			validateWithJson(rngHlpr.getDataFromRange(), jsonFile);
		}

		endranglisteSheetSort.run(); // sollte gleich bleiben

		// compare ref file
		try (InputStream jsonFile = EndranglisteSheetUITest.class
				.getResourceAsStream(ENDRANGLISTESHEETUITEST_REFFILE)) {
			validateWithJson(rngHlpr.getDataFromRange(), jsonFile);
		}

		// ----------------------------------------------------------------------------------------------------
		// sortierung ändern nach gespielte tage
		// ----------------------------------------------------------------------------------------------------

		docPropHelper.setStringProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_ENDRANGLISTE_SORT_MODE,
				SuprMleEndranglisteSortMode.ANZTAGE.getKey());
		endranglisteSheet.run(); // neuen Scheet wird erstellt

		rngHlpr = endranglisteRange(endranglisteSheet.getXSpreadSheet()); // neu einlesen !
		RangeHelper rngHlprValidateSortByDay = endranglisteRangeValidateSpaltenSortByDay(
				endranglisteSheet.getXSpreadSheet());

		// save ref files 
		//		writeToJson(ENDRANGLISTESHEETUITEST_ANZ_TAGE_REFFILE, rngHlpr.getDataFromRange());
		//		writeToJson(ENDRANGLISTESHEETUITEST_ANZ_TAGE_REFFILE_VALIDATE, rngHlprValidateSortByDay.getDataFromRange());

		// compare ref file für daten
		try (InputStream jsonFile = EndranglisteSheetUITest.class
				.getResourceAsStream(ENDRANGLISTESHEETUITEST_ANZ_TAGE_REFFILE)) {
			validateWithJson(rngHlpr.getDataFromRange(), jsonFile);
		}
		// compare ref file für validate
		try (InputStream jsonFile = EndranglisteSheetUITest.class
				.getResourceAsStream(ENDRANGLISTESHEETUITEST_ANZ_TAGE_REFFILE_VALIDATE)) {
			validateWithJson(rngHlprValidateSortByDay.getDataFromRange(), jsonFile);
		}

		// waitEnter();
	}

	@Test
	@Ignore
	public void generateAndSaveTestDatenToJsonFiles() throws IOException, GenerateException {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);

		// testrunden erstellen
		for (int i = 1; i <= 1; i++) {
			SpielTagNr spieltag = SpielTagNr.from(i);
			testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(spieltag);
			if (i == 1) { // 2 mit 1 spieltag weniger
				Position clear = Position.from(3, 10); // D:11  Gerhard Niko 
				sheetHlp.setStringValueInCell(StringCellValue.from(meldeListeSheet_NeuerSpieltag, clear).setValue(""));
				Position clear2 = Position.from(3, 11); // D:12  Grau Franka  
				sheetHlp.setStringValueInCell(StringCellValue.from(meldeListeSheet_NeuerSpieltag, clear2).setValue(""));
			}
			ranglisteTestDaten.generateSpielrundenJsonFilesIntmp(ANZ_RUNDEN, spieltag); // json dateien erstellen
		}

		waitEnter();
	}

	private RangeHelper endranglisteRange(XSpreadsheet endRangliste) {
		RangePosition rangeKomplett = RangePosition.from(0, 3, "AC", 22); // 20 Spieler
		return RangeHelper.from(endRangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeKomplett);
	}

	/**
	 * AF-AJ
	 * 
	 * @param endRangliste
	 * @return
	 */
	private RangeHelper endranglisteRangeValidateSpalten(XSpreadsheet endRangliste) {
		RangePosition rangeKomplett = RangePosition.from("AF", 3, "AJ", 22); // 20 Spieler
		return RangeHelper.from(endRangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeKomplett);

	}

	/**
	 * AF-AK eine Spalte mehr als default !
	 * 
	 * @param endRangliste
	 * @return
	 */
	private RangeHelper endranglisteRangeValidateSpaltenSortByDay(XSpreadsheet endRangliste) {
		RangePosition rangeKomplett = RangePosition.from("AF", 3, "AK", 22); // 20 Spieler
		return RangeHelper.from(endRangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangeKomplett);

	}

}
