package de.petanqueturniermanager.schweizer.meldeliste;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Erstellung 04.03.2024 / Michael Massee
 */

public class SchweizerMeldeListeSheetNewUITest extends BaseCalcUITest {

	private SchweizerMeldeListeSheetTestDaten schweizerMeldeListeSheetTestDaten;

	@Before
	public void setup() {
		this.schweizerMeldeListeSheetTestDaten = new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet);
	}

	@Test
	public void testSchweizerMeldeListeSheetNewMitTestDaten() throws IOException, GenerateException {
		schweizerMeldeListeSheetTestDaten.run();

		// waitEnter();

	}

}
