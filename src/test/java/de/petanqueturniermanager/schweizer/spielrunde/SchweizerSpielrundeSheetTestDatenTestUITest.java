package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.common.primitives.Ints;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

// noch baustelle
@Disabled
public class SchweizerSpielrundeSheetTestDatenTestUITest extends BaseCalcUITest {

	private SchweizerSpielrundeSheetTestDaten schweizerSpielrundeSheetTestDaten;
	// private SchweizerSpielrundeSheetNaechste spielrundeSheetNaechste;

	@BeforeEach
	public void setup() {
		this.schweizerSpielrundeSheetTestDaten = new SchweizerSpielrundeSheetTestDaten(wkingSpreadsheet);
		//		this.spielrundeSheetNaechste = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
	}

	@Test
	public void testSchweizer3RundenTestDaten() throws IOException, GenerateException {

		schweizerSpielrundeSheetTestDaten.generate();

		RangePosition rangeSpielpaarungenSpalten = RangePosition.from(SchweizerSpielrundeSheetNaechste.TEAM_A_SPALTE,
				SchweizerSpielrundeSheetNaechste.ERSTE_DATEN_ZEILE, SchweizerSpielrundeSheetNaechste.TEAM_B_SPALTE,
				SchweizerSpielrundeSheetNaechste.ERSTE_DATEN_ZEILE + 15); // 32 Teams 16 Zeilen

		RangePosition rangePosBahnnummer = RangePosition.from(SchweizerSpielrundeSheetNaechste.BAHN_NR_SPALTE,
				SchweizerSpielrundeSheetNaechste.ERSTE_DATEN_ZEILE, SchweizerSpielrundeSheetNaechste.BAHN_NR_SPALTE,
				SchweizerSpielrundeSheetNaechste.ERSTE_DATEN_ZEILE + 15); // 32 Teams 16 Zeilen 

		XSpreadsheet spielrunde1 = sheetHlp.findByName("1. Spielrunde");
		assertThat(spielrunde1).isNotNull();
		assertThat(schweizerSpielrundeSheetTestDaten.letztePositionRechtsUnten().getAddress()).isEqualTo("E18");

		RangeHelper rngHlpr = RangeHelper.from(spielrunde1, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
				rangeSpielpaarungenSpalten);
		RangeData spielpaarungen = rngHlpr.getDataFromRange();
		assertThat(spielpaarungen).isNotNull().isNotEmpty().hasSize(16);

		List<CellData> collect = spielpaarungen.stream().flatMap(Collection::stream).toList();
		assertThat(collect).isNotEmpty().hasSize(32);
		List<Integer> teamList = Ints.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
				22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32);
		assertThat(collect).extracting(t -> t.getIntVal(-1)).containsAll(teamList); // Alle Teams müssen einmal in der liste sein

		// Bahnnummer
		RangeData bahnrData = RangeHelper
				.from(spielrunde1, wkingSpreadsheet.getWorkingSpreadsheetDocument(), rangePosBahnnummer)
				.getDataFromRange();
		assertThat(bahnrData).isNotEmpty().hasSize(16);
		// 16 Bahnen Random
		assertThat(bahnrData).extracting(t -> t.get(0).getIntVal(-1))
				.containsAll(Ints.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));

		// waitEnter();

	}

}