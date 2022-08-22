package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import java.io.IOException;

import org.junit.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJTestMeldeListeErstellen;

/**
 * Erstellung 22.08.2022 / Michael Massee
 */

public class JGJMeldeListeSheetUITest extends BaseCalcUITest {

	@Test
	public void testMeldeListeTripletteMitAlias() throws GenerateException, IOException {

		JGJTestMeldeListeErstellen testMeldeListeErstellen = new JGJTestMeldeListeErstellen(wkingSpreadsheet, doc);
		testMeldeListeErstellen.run();
		waitEnter();
	}

}
