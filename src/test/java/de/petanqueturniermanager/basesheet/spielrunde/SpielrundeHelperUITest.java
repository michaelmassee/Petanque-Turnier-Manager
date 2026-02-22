package de.petanqueturniermanager.basesheet.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.EmptyISheet;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellung 13.04.2024 / Michael Massee
 */

public class SpielrundeHelperUITest extends BaseCalcUITest {
	EmptyISheet emptyISheet;
	SpielrundeHintergrundFarbeGeradeStyle spielrundeHintergrundFarbeGeradeStyle;
	SpielrundeHintergrundFarbeUnGeradeStyle spielrundeHintergrundFarbeUnGeradeStyle;

	@BeforeEach
	public void beforeTestSpielrundeHelper() throws GenerateException {
		emptyISheet = new EmptyISheet(wkingSpreadsheet, TurnierSystem.SUPERMELEE, "testErsteSpalte");
		emptyISheet.initSheet();

		spielrundeHintergrundFarbeGeradeStyle = new SpielrundeHintergrundFarbeGeradeStyle(
				BasePropertiesSpalte.DEFAULT_GERADE_BACK_COLOR);

		spielrundeHintergrundFarbeUnGeradeStyle = new SpielrundeHintergrundFarbeUnGeradeStyle(
				BasePropertiesSpalte.DEFAULT_UNGERADE_BACK_COLOR);
	}

	@Test
	public void testErsteSpalte() throws IOException, GenerateException {

		// Prepare
		SpielrundeHelper spielrundeHelper = new SpielrundeHelper(emptyISheet, spielrundeHintergrundFarbeGeradeStyle,
				spielrundeHintergrundFarbeUnGeradeStyle);

		int erstZeile = 2;
		int letzteZeile = 11;
		int nrSpalte = 0;
		int headerZeile1 = 0;
		int headerZeile2 = 1;

		List<Integer> expectedList = IntStream.range(1, 11).boxed().collect(Collectors.toList()); // endExclusive !!!
		Integer[] expectedArray = expectedList.toArray(new Integer[0]);
		RangeData bahnNrData;

		// Run
		// random nummer
		spielrundeHelper.datenErsteSpalte(SpielrundeSpielbahn.R, erstZeile, letzteZeile, nrSpalte, headerZeile1,
				headerZeile2, BasePropertiesSpalte.DEFAULT_HEADER_BACK_COLOR);

		// Validate
		bahnNrData = validateErsteSpalte(nrSpalte, headerZeile1, erstZeile, letzteZeile, "Bahn");
		assertThat(bahnNrData).extracting(t -> t.get(0).getIntVal(-1)).containsAll(expectedList);

		// durchnummerieren
		spielrundeHelper.datenErsteSpalte(SpielrundeSpielbahn.N, erstZeile, letzteZeile, nrSpalte, headerZeile1,
				headerZeile2, BasePropertiesSpalte.DEFAULT_HEADER_BACK_COLOR);
		bahnNrData = validateErsteSpalte(nrSpalte, headerZeile1, erstZeile, letzteZeile, "Bahn");
		assertThat(bahnNrData).extracting(t -> t.get(0).getIntVal(-1)).containsExactly(expectedArray);

		spielrundeHelper.datenErsteSpalte(SpielrundeSpielbahn.L, erstZeile, letzteZeile, nrSpalte, headerZeile1,
				headerZeile2, BasePropertiesSpalte.DEFAULT_HEADER_BACK_COLOR);
		bahnNrData = validateErsteSpalte(nrSpalte, headerZeile1, erstZeile, letzteZeile, "Bahn");

		assertThat(bahnNrData).extracting(t -> t.get(0).getStringVal()).containsExactly("", "", "", "", "", "", "", "",
				"", ""); // 10 leere felder

		// nur laufende nummer
		spielrundeHelper.datenErsteSpalte(SpielrundeSpielbahn.X, erstZeile, letzteZeile, nrSpalte, headerZeile1,
				headerZeile2, BasePropertiesSpalte.DEFAULT_HEADER_BACK_COLOR);
		bahnNrData = validateErsteSpalte(nrSpalte, headerZeile1, erstZeile, letzteZeile, "#");
		assertThat(bahnNrData).extracting(t -> t.get(0).getIntVal(-1))
				.containsExactly(expectedList.toArray(new Integer[0]));
	}

	private RangeData validateErsteSpalte(int nrSpalte, int headerZeile1, int erstZeile, int letzteZeile,
			String headerSt) throws GenerateException {

		String headerVal = sheetHlp.getTextFromCell(emptyISheet.getXSpreadSheet(),
				Position.from(nrSpalte, headerZeile1));
		assertThat(headerVal).isNotNull().isNotEmpty().isEqualTo(headerSt);

		RangePosition bahnNrRange = RangePosition.from(nrSpalte, erstZeile, nrSpalte, letzteZeile);
		RangeData bahnNrData = RangeHelper.from(emptyISheet, bahnNrRange).getDataFromRange();
		assertThat(bahnNrData).isNotNull().isNotEmpty().hasSize(10);
		return bahnNrData;

	}

}
