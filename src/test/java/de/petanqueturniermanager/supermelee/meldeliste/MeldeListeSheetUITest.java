package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

public class MeldeListeSheetUITest extends BaseCalcUITest {

	private static List<String> setzPos1 = Arrays.asList(new String[] { "Cummings, Kay", "Morgenroth, Waldtraut" });
	private static List<String> setzPos2 = Arrays.asList(new String[] { "Barber, Arne", "Weaver, Erwin" });
	private static List<String> nichtmitspielen = Arrays
			.asList(new String[] { "Karrer, Milan", "Schaeffer, Thorsten" });
	private static List<String> ausgestiegen = Arrays.asList(new String[] { "Töpfer, Lilian" });

	@Test
	public void testMeldeListe() throws IOException, GenerateException {
		MeldeListeSheet_New meldeListeSheetNew = new MeldeListeSheet_New(wkingSpreadsheet);
		meldeListeSheetNew.run(); // do not start a Thread ! 
		int spielerNameErsteSpalte = meldeListeSheetNew.getMeldungenSpalte().getSpielerNameErsteSpalte();
		int erstDatenZeile = meldeListeSheetNew.getMeldungenSpalte().getErsteDatenZiele();
		int anzMeldungen = testMeldungenEinfuegen(spielerNameErsteSpalte, erstDatenZeile,
				meldeListeSheetNew.getXSpreadSheet());
		MeldeListeSheet_Update meldeListeSheetUpdate = new MeldeListeSheet_Update(wkingSpreadsheet);
		meldeListeSheetUpdate.run();// do not start a Thread !

		// daten einlesen
		RangeHelper raHlp = RangeHelper.from(meldeListeSheetUpdate.getXSpreadSheet(), doc,
				RangePosition.from(spielerNameErsteSpalte - 1, // nr
						erstDatenZeile, spielerNameErsteSpalte + 2, (erstDatenZeile + anzMeldungen) - 1));
		RangeData dataFromRange = raHlp.getDataFromRange();

		assertThat(raHlp.getRangePos().getAddress()).isEqualTo("A3:D" + (2 + anzMeldungen));
		assertThat(dataFromRange.size()).isEqualTo(anzMeldungen);

		// nr pruefen
		HashSet<Integer> pruefListe = new HashSet();
		dataFromRange.stream().map(r -> {
			return r.get(0); // erste spalte mit Nr
		}).forEach(celData -> {
			// nr muss vorhanden sein
			assertThat(celData.getData()).isNotNull().isInstanceOf(Double.class);
			int cellVal = celData.getIntVal(-1);
			// nr darf nur einmal in der liste sein
			assertThat(!pruefListe.contains(cellVal));
			pruefListe.add(cellVal);
		});

		assertThat(pruefListe.size()).isEqualTo(anzMeldungen);

		// waitEnter();
	}

	private int testMeldungenEinfuegen(int spielerNameErsteSpalte, int ersteDatenZeile, XSpreadsheet sheet)
			throws GenerateException {
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

	private List<Object> listeMitTestNamen() {
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