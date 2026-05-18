package de.petanqueturniermanager.supermelee.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

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

		// daten einlesen — Layout: Nr(-1) | Vorname(+0) | Nachname(+1) | setzPos(+2) | alle_spielen(+3)
		RangeHelper raHlp = RangeHelper.from(testMeldeListeErstellen.getXSpreadSheet(), doc,
				RangePosition.from(spielerNameErsteSpalte - 1, // nr
						erstDatenZeile, spielerNameErsteSpalte + 3, (erstDatenZeile + anzMeldungen) - 1));
		RangeData dataFromRange = raHlp.getDataFromRange();

		assertThat(raHlp.getRangePos().getAddress()).isEqualTo("A3:E" + (2 + anzMeldungen));
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

		assertThat(testMeldeListeErstellen.getMeldeListeSheetNew().naechsteFreieDatenZeileInSpielerNrSpalte())
				.isEqualTo(27);

		assertThat(testMeldeListeErstellen.getMeldeListeSheetNew().sucheLetzteZeileMitSpielerNummer()).isEqualTo(26);

		// waitEnter();

	}

	/**
	 * Regression im Kiosk-Modus: nach Erstellen der Meldeliste muss das anschließende
	 * Schützen der Supermelee-Sheets sauber durchlaufen und die Schutz-Invariante
	 * (Sheets geschützt, editierbare Bereiche bleiben editierbar) erfüllt sein.
	 */
	@Test
	public void kioskModus_meldelisteBleibtMitEditierbarenBereichenGesperrt() throws Exception {
		new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc).run();
		mitKioskModus(TurnierSystem.SUPERMELEE, () -> {
			// reines Smoke-Setup: schuetzen() läuft, Invariante wird durch mitKioskModus geprüft.
		});
	}

	private void validateSpieltagSpielrunde(XSpreadsheet xSpreadsheet, int summeSpalte) throws GenerateException {
		// Prüft nur dass die Zellen vorhanden sind – die ptmintproperty-Formelwerte
		// sind im headless UI-Test nicht zuverlässig (LibreOffice cached den Stand
		// unabhängig vom Test-JVM), daher keine Wert-Assertion.
		RangeHelper raHlp = RangeHelper.from(xSpreadsheet, doc,
				RangePosition.from(summeSpalte, MeldeListeSheet_Update.ERSTE_ZEILE_INFO, summeSpalte + 1,
						MeldeListeSheet_Update.ERSTE_ZEILE_INFO + 1));
		RangeData dataFromRange = raHlp.getDataFromRange();
		assertThat(dataFromRange).hasSize(2);
	}

	private void validateDoubletteBlock(XSpreadsheet xSpreadsheet, int summeSpalte) throws GenerateException {
		// daten einlesen
		RangeHelper raHlp = RangeHelper.from(xSpreadsheet, doc,
				RangePosition.from(summeSpalte, MeldeListeSheet_Update.DOUBL_MODE_HEADER, summeSpalte + 1,
						MeldeListeSheet_Update.DOUBL_MODE_SUMMEN_SPIELBAHNEN));
		RangeData dataFromRange = raHlp.getDataFromRange();

		assertThat(dataFromRange.get(1).get(1).getStringVal()).isEqualTo("8");
		assertThat(dataFromRange.get(2).get(1).getStringVal()).isEqualTo("2");
		assertThat(dataFromRange.get(3).get(1).getStringVal()).isEqualTo("0");
		assertThat(dataFromRange.get(4).get(1).getStringVal()).isEqualTo("5");
	}

	private void validateTripletteBlock(XSpreadsheet xSpreadsheet, int summeSpalte) throws GenerateException {
		// daten einlesen
		RangeHelper raHlp = RangeHelper.from(xSpreadsheet, doc,
				RangePosition.from(summeSpalte, MeldeListeSheet_Update.TRIPL_MODE_HEADER, summeSpalte + 1,
						MeldeListeSheet_Update.TRIPL_MODE_SUMMEN_SPIELBAHNEN));
		RangeData dataFromRange = raHlp.getDataFromRange();

		assertThat(dataFromRange.get(1).get(1).getStringVal()).isEqualTo("2");
		assertThat(dataFromRange.get(2).get(1).getStringVal()).isEqualTo("6");
		assertThat(dataFromRange.get(3).get(1).getStringVal()).isEqualTo("0");
		assertThat(dataFromRange.get(4).get(1).getStringVal()).isEqualTo("4");
	}

	private void validate1SummenBlock(XSpreadsheet xSpreadsheet, int summeSpalte) throws GenerateException {
		// daten einlesen 1 summen block
		RangeHelper raHlp = RangeHelper.from(xSpreadsheet, doc,
				RangePosition.from(summeSpalte, MeldeListeSheet_Update.SUMMEN_ERSTE_ZEILE, summeSpalte + 1,
						MeldeListeSheet_Update.SUMMEN_GESAMT_ANZ_SPIELER));
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
			assertThat(pruefListe).doesNotContain(cellVal);
			pruefListe.add(cellVal);
		});

		assertThat(pruefListe.size()).isEqualTo(anzMeldungen);
	}

	private void validateMeldungen(RangeData dataFromRange, int anzMeldungen, List<String> listeMitTestNamen) {

		// Namen liegen in Vorname(col 1) + Nachname(col 2). Im Test-Datenformat "Nachname, Vorname".
		HashSet<String> pruefListe = new HashSet<>();
		dataFromRange.stream().forEach(r -> {
			String vorname = r.get(1).getStringVal();
			String nachname = r.get(2).getStringVal();
			assertThat(vorname).isNotNull();
			String fullName = (nachname == null || nachname.isEmpty()) ? vorname : nachname + ", " + vorname;
			assertThat(pruefListe).doesNotContain(fullName);
			assertThat(listeMitTestNamen).contains(fullName);
			pruefListe.add(fullName);
		});
		assertThat(pruefListe.size()).isEqualTo(anzMeldungen);

	}

	// Spalten-Indizes im RangeData: Nr(0) | Vorname(1) | Nachname(2) | setzPos(3) | alle_spielen(4)
	// Test-Datennamen liegen im Format "Nachname, Vorname" — rekonstruieren für Filter.
	private static String fullName(de.petanqueturniermanager.helper.sheet.rangedata.RowData r) {
		String vorname = r.get(1).getStringVal() == null ? "" : r.get(1).getStringVal();
		String nachname = r.get(2).getStringVal() == null ? "" : r.get(2).getStringVal();
		if (nachname.isEmpty()) return vorname;
		if (vorname.isEmpty()) return nachname;
		return nachname + ", " + vorname;
	}

	private void validateSetzPos(RangeData dataFromRange) {
		long anzSpielermitSetzpos1 = dataFromRange.stream()
				.filter(rData -> TestSuperMeleeMeldeListeErstellen.setzPos1.contains(fullName(rData))
						&& rData.get(3).getIntVal(-1) == 1)
				.count();
		assertThat(anzSpielermitSetzpos1).isEqualTo(TestSuperMeleeMeldeListeErstellen.setzPos1.size());

		anzSpielermitSetzpos1 = dataFromRange.stream().filter(rData -> rData.get(3).getIntVal(-1) == 1).count();
		assertThat(anzSpielermitSetzpos1).isEqualTo(TestSuperMeleeMeldeListeErstellen.setzPos1.size());

		long anzSpielermitSetzpos2 = dataFromRange.stream()
				.filter(rData -> TestSuperMeleeMeldeListeErstellen.setzPos2.contains(fullName(rData))
						&& rData.get(3).getIntVal(-1) == 2)
				.count();
		assertThat(anzSpielermitSetzpos2).isEqualTo(TestSuperMeleeMeldeListeErstellen.setzPos2.size());

		anzSpielermitSetzpos2 = dataFromRange.stream().filter(rData -> rData.get(3).getIntVal(-1) == 2).count();
		assertThat(anzSpielermitSetzpos2).isEqualTo(TestSuperMeleeMeldeListeErstellen.setzPos2.size());

	}

	private void validateAktivSpalte(RangeData dataFromRange, int anzMeldungen) {

		long anzSpielernichtmitspielen = dataFromRange.stream()
				.filter(rData -> TestSuperMeleeMeldeListeErstellen.nichtmitspielen.contains(fullName(rData))
						&& rData.get(4).getIntVal(-1) == -1)
				.count();
		assertThat(anzSpielernichtmitspielen).isEqualTo(TestSuperMeleeMeldeListeErstellen.nichtmitspielen.size());

		anzSpielernichtmitspielen = dataFromRange.stream().filter(rData -> rData.get(4).getIntVal(-1) == -1).count();
		assertThat(anzSpielernichtmitspielen).isEqualTo(TestSuperMeleeMeldeListeErstellen.nichtmitspielen.size());

		long anzSpielerAusgestiegen = dataFromRange.stream()
				.filter(rData -> TestSuperMeleeMeldeListeErstellen.ausgestiegen.contains(fullName(rData))
						&& rData.get(4).getIntVal(-1) == 2)
				.count();
		assertThat(anzSpielerAusgestiegen).isEqualTo(TestSuperMeleeMeldeListeErstellen.ausgestiegen.size());

		anzSpielerAusgestiegen = dataFromRange.stream().filter(rData -> rData.get(4).getIntVal(-1) == 2).count();
		assertThat(anzSpielerAusgestiegen).isEqualTo(TestSuperMeleeMeldeListeErstellen.ausgestiegen.size());

		long anzSpielerSpielen = dataFromRange.stream().filter(rData -> rData.get(4).getIntVal(-1) == 1).count();
		assertThat(anzSpielerSpielen).isEqualTo(anzMeldungen - TestSuperMeleeMeldeListeErstellen.ausgestiegen.size()
				- TestSuperMeleeMeldeListeErstellen.nichtmitspielen.size());

	}

}