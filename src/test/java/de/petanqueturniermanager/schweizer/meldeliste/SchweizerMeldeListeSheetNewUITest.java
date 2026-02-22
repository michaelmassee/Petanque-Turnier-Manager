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

		// 32 min + 2 header zeilen
		assertEquals(2, schweizerMeldeListeSheetTestDaten.getErsteDatenZiele());
		assertEquals(33, schweizerMeldeListeSheetTestDaten.getLetzteDatenZeileUseMin());
		assertEquals(1, schweizerMeldeListeSheetTestDaten.getSpielerNameErsteSpalte());
		assertEquals(3, schweizerMeldeListeSheetTestDaten.getMeldungenSpalte().getLetzteMeldungNameSpalte());

		RangePosition rangeMeldeListe = RangePosition.from(0, 1, 3, 33);
		assertThat(rangeMeldeListe.getAddress()).isEqualTo("A2:D34");
		//		writeToJson("schweizer-meldeliste-ref.json", rangeMeldeListe,
		//				schweizerMeldeListeSheetTestDaten.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFileMeldungen = SchweizerMeldeListeSheetNewUITest.class
				.getResourceAsStream("schweizer-meldeliste-ref.json");
		RangeData rangeData = rangeDateFromRangePosition(rangeMeldeListe,
				schweizerMeldeListeSheetTestDaten.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument());
		validateWithJson(rangeData, jsonFileMeldungen);

		// 		waitEnter();

	}

}
