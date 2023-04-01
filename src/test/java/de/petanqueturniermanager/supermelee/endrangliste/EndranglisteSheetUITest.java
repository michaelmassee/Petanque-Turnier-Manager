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
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.TestMeldeListeErstellen;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet_TestDaten;

/**
 * Erstellung 28.03.2023 / Michael Massee
 */

public class EndranglisteSheetUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(EndranglisteSheetUITest.class);

	private DocumentPropertiesHelper docPropHelper;

	private SpieltagRanglisteSheet spieltagRangliste;

	private MeldeListeSheet_NeuerSpieltag meldeListeSheet_NeuerSpieltag;
	private TestMeldeListeErstellen testMeldeListeErstellen;

	private static final int ANZ_MELDUNGEN = 20;

	@Before
	public void testMeldeListeErstellen() throws GenerateException {
		// erst mal eine meldeListe erstellen
		testMeldeListeErstellen = new TestMeldeListeErstellen(wkingSpreadsheet, doc);
		this.docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);
		docPropHelper.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_ZEIGE_ARBEITS_SPALTEN, true);
		spieltagRangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
		meldeListeSheet_NeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(wkingSpreadsheet);
	}

	@Test
	public void testRanglisteOK() throws GenerateException, IOException {
		testMeldeListeErstellen.initMitAlleDieSpielen(ANZ_MELDUNGEN);
		meldeListeSheet_NeuerSpieltag.run();
		testMeldeListeErstellen.addMitAlleDieSpielenAktuelleSpieltag(2);

		// waitEnter();
	}

	private void generateTestSpieltag(int tag) throws GenerateException {
		spieltagRangliste.run();
	}

	@Test
	@Ignore
	public void generateAndSaveTestDatenToJsonFiles() throws IOException {
		new SpieltagRanglisteSheet_TestDaten(wkingSpreadsheet).run();

		waitEnter();
	}

}
