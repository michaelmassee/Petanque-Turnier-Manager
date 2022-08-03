package de.petanqueturniermanager.forme.korunde;

import java.io.IOException;

import org.junit.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Erstellt 26.07.2022
 * 
 * @author Michael Massee
 * 
 *
 */
public class KoGruppeABSheetUITest extends BaseCalcUITest {

	@Test
	public void testKoGruppe() throws IOException, GenerateException {
		new KoGruppeABSheet(wkingSpreadsheet).run();

		waitEnter();
	}

}