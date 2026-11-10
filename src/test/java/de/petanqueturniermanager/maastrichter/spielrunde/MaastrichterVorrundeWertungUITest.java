/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RundenWertungHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.MaastrichterTurnierTestDaten;

/**
 * Maastrichter: Eine Vorrunde lässt sich nur aus der Wertung nehmen, solange noch keine
 * Finalrunde erzeugt wurde.
 */
class MaastrichterVorrundeWertungUITest extends BaseCalcUITest {

	private static final String VORRUNDE_1 = SheetMetadataHelper.schluesselMaastrichterVorrunde(1);

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(42L);
		MessageBox.setDialogeUeberspringen(true);
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	void vorrundeOhneFinalrundeWirdNichtGewertet() throws Exception {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate(2, false);

		umschaltenAufVorrunde1();

		assertThat(RundenWertungHelper.istNichtGewertet(doc, VORRUNDE_1)).isTrue();
	}

	@Test
	void vorrundeMitFinalrundeBleibtGewertet() throws Exception {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate(3, true);

		umschaltenAufVorrunde1();

		assertThat(RundenWertungHelper.istNichtGewertet(doc, VORRUNDE_1))
				.as("Finalrunde vorhanden – Umschalten gesperrt").isFalse();
	}

	private void umschaltenAufVorrunde1() throws Exception {
		sheetHlp.setActiveSheet(SheetMetadataHelper.findeSheet(doc, VORRUNDE_1).orElseThrow());
		new MaastrichterVorrundeWertungUmschalten(wkingSpreadsheet).doRun();
	}
}
