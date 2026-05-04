package de.petanqueturniermanager.jedergegenjeden.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
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
		meldeListeSheetNew.createMeldelisteWithParams(Formation.TETE, false, false, SpielplanTeamAnzeige.NR);
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
		int nrSpalte = getMeldeListeSheetNew().getMeldungenSpalte().getSpielerNrSpalte();
		XSpreadsheet sheet = getMeldeListeSheetNew().getXSpreadSheet();

		List<Object[]> listeMitTestMeldungen = listeMitTestMeldungen();
		RangeData data = new RangeData();
		listeMitTestMeldungen.forEach(row -> {
			var zeile = data.addNewRow();
			zeile.newInt((int) row[0]);
			zeile.newString((String) row[1]);
		});

		Position startPos = Position.from(nrSpalte, ersteDatenZeile);
		RangePosition rangePos = RangeHelper.from(sheet, doc, data.getRangePosition(startPos)).setDataInRange(data)
				.getRangePos();

		int startRow1Basiert = ersteDatenZeile + 1;
		int endRow1Basiert = startRow1Basiert + listeMitTestMeldungen.size() - 1;
		assertThat(rangePos.getAddress()).isEqualTo("A" + startRow1Basiert + ":B" + endRow1Basiert);
		return listeMitTestMeldungen.size();
	}

	public List<Object[]> listeMitTestMeldungen() {
		List<Object[]> testMeldungen = new ArrayList<>();
		testMeldungen.add(new Object[] { 1, "A" });
		testMeldungen.add(new Object[] { 2, "B" });
		testMeldungen.add(new Object[] { 3, "C" });
		testMeldungen.add(new Object[] { 4, "D" });
		testMeldungen.add(new Object[] { 5, "E" });
		return testMeldungen;
	}

}
