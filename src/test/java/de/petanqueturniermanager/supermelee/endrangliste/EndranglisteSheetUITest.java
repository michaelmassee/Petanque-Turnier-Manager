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
import de.petanqueturniermanager.supermelee.RanglisteTestDaten;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.TestMeldeListeErstellen;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

/**
 * Erstellung 28.03.2023 / Michael Massee
 */

public class EndranglisteSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(EndranglisteSheetUITest.class);

	private DocumentPropertiesHelper docPropHelper;

	private MeldeListeSheet_NeuerSpieltag meldeListeSheet_NeuerSpieltag;
	private SpieltagRanglisteSheet spieltagRangliste;

	private SpielrundeSheet_Naechste spielrundeSheetNaechste;
	private TestMeldeListeErstellen testMeldeListeErstellen;
	private RanglisteTestDaten<EndranglisteSheetUITest> ranglisteTestDaten;

	private static final int ANZ_MELDUNGEN = 20;
	private static final int ANZ_RUNDEN = 3;

	@Before
	public void testMeldeListeErstellen() throws GenerateException {
		// erst mal eine meldeListe erstellen
		testMeldeListeErstellen = new TestMeldeListeErstellen(wkingSpreadsheet, doc);
		this.docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN, true);
		spieltagRangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
		meldeListeSheet_NeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet);
		spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		ranglisteTestDaten = new RanglisteTestDaten<>(wkingSpreadsheet, sheetHlp, this);
	}

	@Test
	public void testRanglisteOK() throws GenerateException, IOException {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);
		ranglisteTestDaten.erstelleTestSpielrunden(ANZ_RUNDEN, false, SpielTagNr.from(1));
		//		waitEnter();
	}

	@Test
	@Ignore
	public void generateAndSaveTestDatenToJsonFiles() throws IOException, GenerateException {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);

		// testrunden erstellen
		SpielTagNr spieltag = SpielTagNr.from(1);
		ranglisteTestDaten.generateSpielrundenJsonFilesIntmp(ANZ_RUNDEN, spieltag);

		waitEnter();
	}

}
