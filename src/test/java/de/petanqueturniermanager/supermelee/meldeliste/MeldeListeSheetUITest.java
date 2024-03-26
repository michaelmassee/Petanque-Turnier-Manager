package de.petanqueturniermanager.supermelee.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Testet inhalten der Meldeliste wenn alles okay
 * 
 * @author M.Massee
 */

public class MeldeListeSheetUITest extends BaseCalcUITest {

	@Test
	public void testMeldeListe() throws IOException, GenerateException {

		TestSuperMeleeMeldeListeErstellen testMeldeListeErstellen = new TestSuperMeleeMeldeListeErstellen(
				wkingSpreadsheet, doc);
		int anzMeldungen = testMeldeListeErstellen.run();

		int spielerNameErsteSpalte = testMeldeListeErstellen.getMeldeListeSheetNew().getMeldungenSpalte()
				.getErsteMeldungNameSpalte();
		int erstDatenZeile = testMeldeListeErstellen.getMeldeListeSheetNew().getMeldungenSpalte().getErsteDatenZiele();

		// daten einlesen
		RangeHelper raHlp = RangeHelper.from(testMeldeListeErstellen.getXSpreadSheet(), doc,
				RangePosition.from(spielerNameErsteSpalte - 1, // nr
						erstDatenZeile, spielerNameErsteSpalte + 2, (erstDatenZeile + anzMeldungen) - 1));
		RangeData dataFromRange = raHlp.getDataFromRange();

		assertThat(raHlp.getRangePos().getAddress()).isEqualTo("A3:D" + (2 + anzMeldungen));
		assertThat(dataFromRange.size()).isEqualTo(anzMeldungen);

		// nr pruefen
		valaditeNr(dataFromRange, anzMeldungen);
		validateMeldungen(dataFromRange, anzMeldungen, testMeldeListeErstellen.listeMitTestNamen());
		validateSetzPos(dataFromRange);
		validateAktivSpalte(dataFromRange, anzMeldungen);

		validate1SummenBlock(testMeldeListeErstellen.getXSpreadSheet(), testMeldeListeErstellen.ersteSummeSpalte());
		validateTripletteBlock(testMeldeListeErstellen.getXSpreadSheet(), testMeldeListeErstellen.ersteSummeSpalte());
		validateDoubletteBlock(testMeldeListeErstellen.getXSpreadSheet(), testMeldeListeErstellen.ersteSummeSpalte());
		validateSpieltagSpielrunde(testMeldeListeErstellen.getXSpreadSheet(),
				testMeldeListeErstellen.ersteSummeSpalte());
		assertThat(testMeldeListeErstellen.getMeldeListeSheetNew().getLetzteMitDatenZeileInSpielerNrSpalte())
				.isEqualTo(26);

		assertThat(testMeldeListeErstellen.getMeldeListeSheetNew().neachsteFreieDatenZeileInSpielerNrSpalte())
				.isEqualTo(27);

		assertThat(testMeldeListeErstellen.getMeldeListeSheetNew().sucheLetzteZeileMitSpielerNummer()).isEqualTo(26);

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
		HashSet<Integer> pruefListe = new HashSet<>();
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

	private void validateMeldungen(RangeData dataFromRange, int anzMeldungen, List<String> listeMitTestNamen) {

		HashSet<String> pruefListe = new HashSet<>();
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
				.filter(rData -> TestSuperMeleeMeldeListeErstellen.setzPos1.contains(rData.get(1).getStringVal())
						&& rData.get(2).getIntVal(-1) == 1)
				.count();
		assertThat(anzSpielermitSetzpos1).isEqualTo(TestSuperMeleeMeldeListeErstellen.setzPos1.size());

		anzSpielermitSetzpos1 = dataFromRange.stream().filter(rData -> rData.get(2).getIntVal(-1) == 1).count();
		assertThat(anzSpielermitSetzpos1).isEqualTo(TestSuperMeleeMeldeListeErstellen.setzPos1.size());

		long anzSpielermitSetzpos2 = dataFromRange.stream()
				.filter(rData -> TestSuperMeleeMeldeListeErstellen.setzPos2.contains(rData.get(1).getStringVal())
						&& rData.get(2).getIntVal(-1) == 2)
				.count();
		assertThat(anzSpielermitSetzpos2).isEqualTo(TestSuperMeleeMeldeListeErstellen.setzPos2.size());

		anzSpielermitSetzpos2 = dataFromRange.stream().filter(rData -> rData.get(2).getIntVal(-1) == 2).count();
		assertThat(anzSpielermitSetzpos2).isEqualTo(TestSuperMeleeMeldeListeErstellen.setzPos2.size());

	}

	private void validateAktivSpalte(RangeData dataFromRange, int anzMeldungen) {

		long anzSpielernichtmitspielen = dataFromRange.stream()
				.filter(rData -> TestSuperMeleeMeldeListeErstellen.nichtmitspielen.contains(rData.get(1).getStringVal())
						&& rData.get(3).getIntVal(-1) == -1)
				.count();
		assertThat(anzSpielernichtmitspielen).isEqualTo(TestSuperMeleeMeldeListeErstellen.nichtmitspielen.size());

		anzSpielernichtmitspielen = dataFromRange.stream().filter(rData -> rData.get(3).getIntVal(-1) == -1).count();
		assertThat(anzSpielernichtmitspielen).isEqualTo(TestSuperMeleeMeldeListeErstellen.nichtmitspielen.size());

		long anzSpielerAusgestiegen = dataFromRange.stream()
				.filter(rData -> TestSuperMeleeMeldeListeErstellen.ausgestiegen.contains(rData.get(1).getStringVal())
						&& rData.get(3).getIntVal(-1) == 2)
				.count();
		assertThat(anzSpielerAusgestiegen).isEqualTo(TestSuperMeleeMeldeListeErstellen.ausgestiegen.size());

		anzSpielerAusgestiegen = dataFromRange.stream().filter(rData -> rData.get(3).getIntVal(-1) == 2).count();
		assertThat(anzSpielerAusgestiegen).isEqualTo(TestSuperMeleeMeldeListeErstellen.ausgestiegen.size());

		long anzSpielerSpielen = dataFromRange.stream().filter(rData -> rData.get(3).getIntVal(-1) == 1).count();
		assertThat(anzSpielerSpielen).isEqualTo(anzMeldungen - TestSuperMeleeMeldeListeErstellen.ausgestiegen.size()
				- TestSuperMeleeMeldeListeErstellen.nichtmitspielen.size());

	}

}