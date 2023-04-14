package de.petanqueturniermanager.supermelee.endrangliste;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.RanglisteTestDaten;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
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
	private RanglisteTestDaten<EndranglisteSheetUITest> ranglisteTestDaten;

	private static final int ANZ_MELDUNGEN = 20;
	private static final int ANZ_RUNDEN = 3;
	private static final int ANZ_SPIELTAGE = 3;

	@Before
	public void testMeldeListeErstellen() throws GenerateException {
		// erst mal eine meldeListe erstellen
		testMeldeListeErstellen = new TestMeldeListeErstellen(wkingSpreadsheet, doc);
		this.docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN, true);
		spieltagRangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
		meldeListeSheet_NeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet);
		ranglisteTestDaten = new RanglisteTestDaten<>(wkingSpreadsheet, sheetHlp, this);
		endranglisteSheet = new EndranglisteSheet(wkingSpreadsheet);
	}

	@Test
	public void testRanglisteOK() throws GenerateException, IOException {
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
		waitEnter();
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

}
