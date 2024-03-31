package de.petanqueturniermanager.schweizer.spielrunde;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;

public class SchweizerSpielrundeSheetTestDatenTestUITest extends BaseCalcUITest {

	private SchweizerSpielrundeSheetTestDaten schweizerSpielrundeSheetTestDaten;

	@Before
	public void setup() {
		this.schweizerSpielrundeSheetTestDaten = new SchweizerSpielrundeSheetTestDaten(wkingSpreadsheet);
	}

	@Test
	public void testSchweizer3RundenTestDaten() throws IOException, GenerateException {

		schweizerSpielrundeSheetTestDaten.generate();

		waitEnter();

	}

}