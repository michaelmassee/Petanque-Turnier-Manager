package de.petanqueturniermanager.jedergegenjeden.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;

/**
 * Erstellung 06.08.2022 / Michael Massee
 */

public class DirektVergleichUITest extends BaseCalcUITest {

	private static final Logger logger = LogManager.getLogger(DirektVergleichUITest.class);

	private static final String TEST_SHEET_NAME = "Direktvergleich Test";

	@Test
	public void testDirektVergleich() throws IOException {
		// direktvergleich test erstellen
		XSpreadsheet testSheet = sheetHlp.newIfNotExist(TEST_SHEET_NAME, 0);
		assertThat(testSheet).isNotNull();

		//@formatter:off
		Object spielPaarungen = new Object[][] { 
			{ 1, 2 },
			{ 2, 1 } };
			
		Object spielPunkte = new Object[][] { 
			{ 13, 7 },
			{ 13, 7 } };
		//@formatter:on

		// waitEnter();

	}

}
