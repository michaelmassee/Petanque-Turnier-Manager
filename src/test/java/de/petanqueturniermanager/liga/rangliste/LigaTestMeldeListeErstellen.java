package de.petanqueturniermanager.liga.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetNew;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetUpdate;

/**
 * Erstellung: LigaTestMeldeListeErstellen<br>
 * Erstellt eine Liga-Meldeliste mit 4 Teams fuer UITests.
 */
public class LigaTestMeldeListeErstellen {

	private final WorkingSpreadsheet wkingSpreadsheet;
	private final XSpreadsheetDocument doc;
	private LigaMeldeListeSheetNew meldeListeSheetNew;

	public LigaTestMeldeListeErstellen(WorkingSpreadsheet wkingSpreadsheet, XSpreadsheetDocument doc) {
		this.wkingSpreadsheet = wkingSpreadsheet;
		this.doc = doc;
	}

	public int run() throws GenerateException {
		meldeListeSheetNew = new LigaMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeSheetNew.run();
		int anzMeldungen = testMeldungenEinfuegen();
		LigaMeldeListeSheetUpdate meldeListeSheetUpdate = new LigaMeldeListeSheetUpdate(wkingSpreadsheet);
		meldeListeSheetUpdate.run();
		return anzMeldungen;
	}

	private int testMeldungenEinfuegen() throws GenerateException {
		int ersteDatenZeile = meldeListeSheetNew.getMeldungenSpalte().getErsteDatenZiele();
		int spielerNameErsteSpalte = meldeListeSheetNew.getMeldungenSpalte().getErsteMeldungNameSpalte();

		List<Object> testNamen = listeMitTestNamen();
		RangeData data = new RangeData(testNamen);

		Position startPos = Position.from(spielerNameErsteSpalte, ersteDatenZeile);
		RangePosition rangePos = RangeHelper.from(meldeListeSheetNew.getXSpreadSheet(), doc, data.getRangePosition(startPos))
				.setDataInRange(data).getRangePos();
		assertThat(rangePos.getAddress()).isEqualTo("B3:B" + (2 + testNamen.size()));
		return testNamen.size();
	}

	public List<Object> listeMitTestNamen() {
		return List.of("Alpha", "Beta", "Gamma", "Delta");
	}

}
