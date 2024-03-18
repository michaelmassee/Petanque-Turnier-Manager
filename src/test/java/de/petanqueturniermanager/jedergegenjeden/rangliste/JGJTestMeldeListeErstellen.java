package de.petanqueturniermanager.jedergegenjeden.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;

/**
 * Erstellung 16.07.2022 / Michael Massee
 */

public class JGJTestMeldeListeErstellen {

	private final WorkingSpreadsheet wkingSpreadsheet;
	private final XSpreadsheetDocument doc;
	private JGJMeldeListeSheet_New meldeListeSheetNew;

	public JGJTestMeldeListeErstellen(WorkingSpreadsheet wkingSpreadsheet, XSpreadsheetDocument doc) {
		this.wkingSpreadsheet = wkingSpreadsheet;
		this.doc = doc;
	}

	public int run() throws GenerateException {
		meldeListeSheetNew = new JGJMeldeListeSheet_New(wkingSpreadsheet);
		meldeListeSheetNew.run(); // do not start a Thread ! 
		int anzMeldungen = testMeldungenEinfuegen();
		JGJMeldeListeSheet_Update meldeListeSheetUpdate = new JGJMeldeListeSheet_Update(wkingSpreadsheet);
		meldeListeSheetUpdate.run();// do not start a Thread !

		return anzMeldungen;
	}

	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return meldeListeSheetNew.getXSpreadSheet();
	}

	public JGJMeldeListeSheet_New getMeldeListeSheetNew() {
		return meldeListeSheetNew;
	}

	private int testMeldungenEinfuegen() throws GenerateException {

		int ersteDatenZeile = getMeldeListeSheetNew().getMeldungenSpalte().getErsteDatenZiele();
		int spielerNameErsteSpalte = getMeldeListeSheetNew().getMeldungenSpalte().getErsteMeldungNameSpalte();
		XSpreadsheet sheet = getMeldeListeSheetNew().getXSpreadSheet();

		List<Object> listeMitTestNamen = listeMitTestNamen();
		RangeData data = new RangeData(listeMitTestNamen);

		Position startPos = Position.from(spielerNameErsteSpalte, ersteDatenZeile);
		RangePosition rangePos = RangeHelper.from(sheet, doc, data.getRangePosition(startPos)).setDataInRange(data)
				.getRangePos();
		assertThat(rangePos.getAddress()).isEqualTo("B3:B" + (2 + listeMitTestNamen.size()));
		return listeMitTestNamen.size();
	}

	public List<Object> listeMitTestNamen() {
		List<Object> testNamen = new ArrayList<>();

		testNamen.add("A");
		testNamen.add("B");
		testNamen.add("C");
		testNamen.add("D");
		testNamen.add("E");
		return testNamen;
	}

}
