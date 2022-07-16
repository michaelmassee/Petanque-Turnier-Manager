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

/**
 * Testet inhalten der Meldeliste wenn alles okay
 * 
 * @author M.Massee
 */

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
		valaditeNr(dataFromRange, anzMeldungen);
		validateMeldungen(dataFromRange, anzMeldungen);
		validateSetzPos(dataFromRange);
		validateAktivSpalte(dataFromRange, anzMeldungen);

		validate1SummenBlock(meldeListeSheetUpdate.getXSpreadSheet(), meldeListeSheetUpdate.ersteSummeSpalte());
		validateTripletteBlock(meldeListeSheetUpdate.getXSpreadSheet(), meldeListeSheetUpdate.ersteSummeSpalte());
		validateDoubletteBlock(meldeListeSheetUpdate.getXSpreadSheet(), meldeListeSheetUpdate.ersteSummeSpalte());
		validateSpieltagSpielrunde(meldeListeSheetUpdate.getXSpreadSheet(), meldeListeSheetUpdate.ersteSummeSpalte());
		// waitEnter();

	}

	private void validateSpieltagSpielrunde(XSpreadsheet xSpreadsheet, int summeSpalte) throws GenerateException {
		// daten einlesen
		RangeHelper raHlp = RangeHelper.from(xSpreadsheet, doc,
				RangePosition.from(summeSpalte, AbstractSupermeleeMeldeListeSheet.ERSTE_ZEILE_INFO, summeSpalte + 1,
						AbstractSupermeleeMeldeListeSheet.ERSTE_ZEILE_INFO + 1));
		RangeData dataFromRange = raHlp.getDataFromRange();

		assertThat(dataFromRange.get(0).get(1).getStringVal()).isEqualTo("1"); // Spieltag 
		assertThat(dataFromRange.get(1).get(1).getStringVal()).isEqualTo("1"); // Spielrunde 

	}

	private void validateDoubletteBlock(XSpreadsheet xSpreadsheet, int summeSpalte) throws GenerateException {
		// daten einlesen
		RangeHelper raHlp = RangeHelper.from(xSpreadsheet, doc,
				RangePosition.from(summeSpalte, AbstractSupermeleeMeldeListeSheet.DOUBL_MODE_HEADER, summeSpalte + 1,
						AbstractSupermeleeMeldeListeSheet.DOUBL_MODE_SUMMEN_SPIELBAHNEN));
		RangeData dataFromRange = raHlp.getDataFromRange();

		assertThat(dataFromRange.get(1).get(1).getStringVal()).isEqualTo("8");
		assertThat(dataFromRange.get(2).get(1).getStringVal()).isEqualTo("2");
		assertThat(dataFromRange.get(3).get(1).getStringVal()).isEqualTo("0");
		assertThat(dataFromRange.get(4).get(1).getStringVal()).isEqualTo("5");
	}

	private void validateTripletteBlock(XSpreadsheet xSpreadsheet, int summeSpalte) throws GenerateException {
		// daten einlesen
		RangeHelper raHlp = RangeHelper.from(xSpreadsheet, doc,
				RangePosition.from(summeSpalte, AbstractSupermeleeMeldeListeSheet.TRIPL_MODE_HEADER, summeSpalte + 1,
						AbstractSupermeleeMeldeListeSheet.TRIPL_MODE_SUMMEN_SPIELBAHNEN));
		RangeData dataFromRange = raHlp.getDataFromRange();

		assertThat(dataFromRange.get(1).get(1).getStringVal()).isEqualTo("2");
		assertThat(dataFromRange.get(2).get(1).getStringVal()).isEqualTo("6");
		assertThat(dataFromRange.get(3).get(1).getStringVal()).isEqualTo("0");
		assertThat(dataFromRange.get(4).get(1).getStringVal()).isEqualTo("4");
	}

	private void validate1SummenBlock(XSpreadsheet xSpreadsheet, int summeSpalte) throws GenerateException {
		// daten einlesen 1 summen block
		RangeHelper raHlp = RangeHelper.from(xSpreadsheet, doc,
				RangePosition.from(summeSpalte, AbstractSupermeleeMeldeListeSheet.SUMMEN_ERSTE_ZEILE, summeSpalte + 1,
						AbstractSupermeleeMeldeListeSheet.SUMMEN_GESAMT_ANZ_SPIELER));
		RangeData dataFromRange = raHlp.getDataFromRange();

		assertThat(dataFromRange.get(0).get(0).getStringVal()).isEqualTo("Aktiv");
		assertThat(dataFromRange.get(0).get(1).getStringVal()).isEqualTo("22");

		assertThat(dataFromRange.get(1).get(0).getStringVal()).isEqualTo("InAktiv");
		assertThat(dataFromRange.get(1).get(1).getStringVal()).isEqualTo("2");

		assertThat(dataFromRange.get(2).get(0).getStringVal()).isEqualTo("Ausgestiegen");
		assertThat(dataFromRange.get(2).get(1).getStringVal()).isEqualTo("1");

		assertThat(dataFromRange.get(3).get(0).getStringVal()).isEqualTo("Akt + Ausg");
		assertThat(dataFromRange.get(3).get(1).getStringVal()).isEqualTo("23");

		assertThat(dataFromRange.get(4).get(0).getStringVal()).isEqualTo("Summe");
		assertThat(dataFromRange.get(4).get(1).getStringVal()).isEqualTo("25");
	}

	private void valaditeNr(RangeData dataFromRange, int anzMeldungen) {
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
	}

	private void validateMeldungen(RangeData dataFromRange, int anzMeldungen) {

		List<Object> listeMitTestNamen = listeMitTestNamen();

		HashSet<String> pruefListe = new HashSet();
		dataFromRange.stream().map(r -> {
			return r.get(1); // 2 spalte mit Namen
		}).forEach(celData -> {
			// name muss vorhanden sein
			assertThat(celData.getData()).isNotNull().isInstanceOf(String.class);
			String cellVal = celData.getStringVal();
			// nr darf nur einmal in der liste sein
			assertThat(!pruefListe.contains(cellVal));
			assertThat(listeMitTestNamen.contains(cellVal));
			pruefListe.add(cellVal);
		});
		assertThat(pruefListe.size()).isEqualTo(anzMeldungen);

	}

	private void validateSetzPos(RangeData dataFromRange) {
		long anzSpielermitSetzpos1 = dataFromRange.stream()
				.filter(rData -> setzPos1.contains(rData.get(1).getStringVal()) && rData.get(2).getIntVal(-1) == 1)
				.count();
		assertThat(anzSpielermitSetzpos1).isEqualTo(setzPos1.size());

		anzSpielermitSetzpos1 = dataFromRange.stream().filter(rData -> rData.get(2).getIntVal(-1) == 1).count();
		assertThat(anzSpielermitSetzpos1).isEqualTo(setzPos1.size());

		long anzSpielermitSetzpos2 = dataFromRange.stream()
				.filter(rData -> setzPos2.contains(rData.get(1).getStringVal()) && rData.get(2).getIntVal(-1) == 2)
				.count();
		assertThat(anzSpielermitSetzpos2).isEqualTo(setzPos2.size());

		anzSpielermitSetzpos2 = dataFromRange.stream().filter(rData -> rData.get(2).getIntVal(-1) == 2).count();
		assertThat(anzSpielermitSetzpos2).isEqualTo(setzPos2.size());

	}

	private void validateAktivSpalte(RangeData dataFromRange, int anzMeldungen) {

		long anzSpielernichtmitspielen = dataFromRange.stream().filter(
				rData -> nichtmitspielen.contains(rData.get(1).getStringVal()) && rData.get(3).getIntVal(-1) == -1)
				.count();
		assertThat(anzSpielernichtmitspielen).isEqualTo(nichtmitspielen.size());

		anzSpielernichtmitspielen = dataFromRange.stream().filter(rData -> rData.get(3).getIntVal(-1) == -1).count();
		assertThat(anzSpielernichtmitspielen).isEqualTo(nichtmitspielen.size());

		long anzSpielerAusgestiegen = dataFromRange.stream()
				.filter(rData -> ausgestiegen.contains(rData.get(1).getStringVal()) && rData.get(3).getIntVal(-1) == 2)
				.count();
		assertThat(anzSpielerAusgestiegen).isEqualTo(ausgestiegen.size());

		anzSpielerAusgestiegen = dataFromRange.stream().filter(rData -> rData.get(3).getIntVal(-1) == 2).count();
		assertThat(anzSpielerAusgestiegen).isEqualTo(ausgestiegen.size());

		long anzSpielerSpielen = dataFromRange.stream().filter(rData -> rData.get(3).getIntVal(-1) == 1).count();
		assertThat(anzSpielerSpielen).isEqualTo(anzMeldungen - ausgestiegen.size() - nichtmitspielen.size());

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