package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellung 04.03.2024 / Michael Massee
 */

public class SchweizerMeldeListeSheetNewUITest extends BaseCalcUITest {

	private SchweizerMeldeListeSheetTestDaten schweizerMeldeListeSheetTestDaten;

	@BeforeEach
	public void setup() {
		this.schweizerMeldeListeSheetTestDaten = new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, 10); // 10 Teams
	}

	@Test
	public void testSchweizerMeldeListeSheetNewMitTestDaten() throws IOException, GenerateException {
		schweizerMeldeListeSheetTestDaten.run();

		// 32 min + 3 header zeilen (Turniersystem + Block-Titel + Spalten-Namen)
		assertEquals(3, schweizerMeldeListeSheetTestDaten.getErsteDatenZiele());
		assertEquals(34, schweizerMeldeListeSheetTestDaten.getLetzteDatenZeileUseMin());
		assertEquals(2, schweizerMeldeListeSheetTestDaten.getSpielerNameErsteSpalte());

		RangePosition rangeMeldeListe = RangePosition.from(0, 1, 3, 34);
		assertThat(rangeMeldeListe.getAddress()).isEqualTo("A2:D35");
		//		writeToJson("schweizer-meldeliste-ref.json", rangeMeldeListe,
		//				schweizerMeldeListeSheetTestDaten.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFileMeldungen = SchweizerMeldeListeSheetNewUITest.class
				.getResourceAsStream("schweizer-meldeliste-ref.json");
		RangeData rangeData = rangeDateFromRangePosition(rangeMeldeListe,
				schweizerMeldeListeSheetTestDaten.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument());
		validateWithJson(rangeData, jsonFileMeldungen);

		// 		waitEnter();

	}

	/**
	 * Regression im Kiosk-Modus: Nach erfolgreichem Aufbau der Schweizer-Meldeliste darf
	 * das anschließende {@code schuetzen()} der {@link de.petanqueturniermanager.schweizer.blattschutz.SchweizerBlattschutzKonfiguration}
	 * nicht crashen und die Meldeliste muss editierbare Datenbereiche
	 * ({@code CellProtection.IsLocked == false}) auch nach dem Sperren behalten. Diese
	 * Invariante wird zentral durch {@code BaseCalcUITest#mitKioskModus} geprüft.
	 */
	@Test
	public void kioskModus_meldelisteBleibtMitEditierbarenBereichenGesperrt() throws GenerateException {
		schweizerMeldeListeSheetTestDaten.run();
		mitKioskModus(TurnierSystem.SCHWEIZER, () -> {
			// reines Smoke-Setup: schuetzen() läuft, Invariante wird durch mitKioskModus geprüft.
		});
	}

}
