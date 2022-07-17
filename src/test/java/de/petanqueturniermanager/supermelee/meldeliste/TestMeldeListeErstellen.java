package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Erstellung 16.07.2022 / Michael Massee
 */

public class TestMeldeListeErstellen {

	private final WorkingSpreadsheet wkingSpreadsheet;
	private final XSpreadsheetDocument doc;
	private MeldeListeSheet_New meldeListeSheetNew;

	public static List<String> setzPos1 = Arrays.asList(new String[] { "Cummings, Kay", "Morgenroth, Waldtraut" });
	public static List<String> setzPos2 = Arrays.asList(new String[] { "Barber, Arne", "Weaver, Erwin" });
	public static List<String> nichtmitspielen = Arrays.asList(new String[] { "Karrer, Milan", "Schaeffer, Thorsten" });
	public static List<String> ausgestiegen = Arrays.asList(new String[] { "Töpfer, Lilian" });

	public TestMeldeListeErstellen(WorkingSpreadsheet wkingSpreadsheet, XSpreadsheetDocument doc) {
		this.wkingSpreadsheet = wkingSpreadsheet;
		this.doc = doc;
	}

	public int run() throws GenerateException {
		meldeListeSheetNew = new MeldeListeSheet_New(wkingSpreadsheet);
		meldeListeSheetNew.run(); // do not start a Thread ! 
		int anzMeldungen = testMeldungenEinfuegen();
		MeldeListeSheet_Update meldeListeSheetUpdate = new MeldeListeSheet_Update(wkingSpreadsheet);
		meldeListeSheetUpdate.run();// do not start a Thread !

		return anzMeldungen;
	}

	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return meldeListeSheetNew.getXSpreadSheet();
	}

	public MeldeListeSheet_New getMeldeListeSheetNew() {
		return meldeListeSheetNew;
	}

	public int ersteSummeSpalte() throws GenerateException {
		return meldeListeSheetNew.ersteSummeSpalte();
	}

	private int testMeldungenEinfuegen() throws GenerateException {

		int ersteDatenZeile = getMeldeListeSheetNew().getMeldungenSpalte().getErsteDatenZiele();
		int spielerNameErsteSpalte = getMeldeListeSheetNew().getMeldungenSpalte().getSpielerNameErsteSpalte();
		XSpreadsheet sheet = getMeldeListeSheetNew().getXSpreadSheet();

		List<Object> listeMitTestNamen = listeMitTestNamen();
		RangeData data = new RangeData(listeMitTestNamen);
		data.addNewEmptySpalte(); // setz position
		data.addNewSpalte(1); // alle spielen

		// ------------------------------------------------------------------------------------------
		// setzPosition 1
		data.stream().filter(rData -> setzPos1.contains(rData.get(0).getStringVal())).forEach(r -> {
			r.get(1).setVal(1);
		});

		// setzPosition 2
		data.stream().filter(rData -> setzPos2.contains(rData.get(0).getStringVal())).forEach(r -> {
			r.get(1).setVal(2);
		});
		// ------------------------------------------------------------------------------------------
		// die spielen nicht mit
		data.stream().filter(rData -> nichtmitspielen.contains(rData.get(0).getStringVal())).forEach(r -> {
			r.get(2).setVal(null);
		});

		// ausgestiegen
		data.stream().filter(rData -> ausgestiegen.contains(rData.get(0).getStringVal())).forEach(r -> {
			r.get(2).setVal(2);
		});
		// ------------------------------------------------------------------------------------------

		Position startPos = Position.from(spielerNameErsteSpalte, ersteDatenZeile);
		RangePosition rangePos = RangeHelper.from(sheet, doc, data.getRangePosition(startPos)).setDataInRange(data)
				.getRangePos();
		assertThat(rangePos.getAddress()).isEqualTo("B3:D" + (2 + listeMitTestNamen.size()));
		return listeMitTestNamen.size();
	}

	// http://migano.de/testdaten.php

	public List<Object> listeMitTestNamen() {
		List<Object> testNamen = new ArrayList<>();

		testNamen.add("Wegner, Silas");
		testNamen.add("Wright, Silvia");
		testNamen.add("Karrer, Milan");
		testNamen.add("Böhme, Bjarne");
		testNamen.add("Cummings, Kay");
		testNamen.add("Trost, Simon");
		testNamen.add("Adrian, Isabella");
		testNamen.add("Gruber, Chantall");
		testNamen.add("Erpel, Leander");
		testNamen.add("Breunig, Lili");
		testNamen.add("Schulte, Catharina");
		testNamen.add("Lau, Henrik");
		testNamen.add("Seel, Dominic");
		testNamen.add("Edwards, Victor");
		testNamen.add("Hoffmann, Arne");
		testNamen.add("Morgenroth, Waldtraut");
		testNamen.add("Töpfer, Lilian");
		testNamen.add("Reiter, Enno");
		testNamen.add("Schaeffer, Thorsten");
		testNamen.add("Kübler, Matis");
		testNamen.add("Barber, Arne");
		testNamen.add("Sinn, Lya");
		testNamen.add("Schreiber, Justus");
		testNamen.add("Weaver, Erwin");
		testNamen.add("Crawford, Lorena");
		return testNamen;
	}

}
