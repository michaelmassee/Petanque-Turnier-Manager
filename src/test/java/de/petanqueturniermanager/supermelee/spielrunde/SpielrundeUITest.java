package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

/**
 * Erstellung 17.07.2022 / Michael Massee
 */

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TestMeldeListeErstellen;

/**
 * Mehrere Spielrunden testen + rangliste
 *
 */
public class SpielrundeUITest extends BaseCalcUITest {

	@Test
	public void testSpielrundenOk() throws IOException, GenerateException {
		// erst mal eine meldeListe erstellen
		TestMeldeListeErstellen testMeldeListeErstellen = new TestMeldeListeErstellen(wkingSpreadsheet, doc);
		int anzMeldungen = testMeldeListeErstellen.run();

		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(wkingSpreadsheet);

		SpielrundeSheet_Naechste spielrundeSheetNaechste = new SpielrundeSheet_Naechste(wkingSpreadsheet);
		for (int runde = 1; runde < 5; runde++) { // 4 runden
			spielrundeSheetNaechste.run(); // no thread
			validateAnzSpielrundeInMeldeliste(runde, docPropHelper);
		}
		waitEnter();
	}

	/**
	 * 
	 * @param runde
	 * @param docPropHelper
	 */

	private void validateAnzSpielrundeInMeldeliste(int runde, DocumentPropertiesHelper docPropHelper) {
		int spielrunde = docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, -1);
		assertThat(runde).isEqualTo(runde);

	}

}
