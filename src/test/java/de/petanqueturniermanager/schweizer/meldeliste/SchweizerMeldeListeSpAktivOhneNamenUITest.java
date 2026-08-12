/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;

/**
 * Regressionstest: SP- und Aktiv-Werte in einer Zeile ohne Namen (Formation NUR_TEAMNAME, keine
 * Teamname-Eingabe) müssen bei einem Refresh gelöscht werden, statt "verwaist" stehen zu bleiben
 * (siehe Bugreport mit angehängter bug-red.ods, die genau dieses Muster zeigte: SP=1 in Zeilen
 * ohne Nr/Teamname).
 */
class SchweizerMeldeListeSpAktivOhneNamenUITest extends BaseCalcUITest {

	@Test
	void spOhneNamenWirdBeiRefreshGeloescht() throws Exception {
		SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.NUR_TEAMNAME, true, false);

		int ersteDatenZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
		int spSpalte = meldeListeNew.getSetzPositionSpalte();
		XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();

		// Zeile ohne Teamname (und ohne Nr), aber mit gesetztem SP-Wert - exakt das Muster aus der
		// Bugreport-ODS.
		int zeileOhneNamen = ersteDatenZeile + 4;
		meldeListeNew.getSheetHelper()
				.setNumberValueInCell(NumberCellValue.from(xSheet, Position.from(spSpalte, zeileOhneNamen)).setValue(1));

		SchweizerMeldeListeSheetUpdate meldeListeUpdate = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
		meldeListeUpdate.vollstaendigAktualisieren();

		String spText = meldeListeNew.getSheetHelper().getTextFromCell(xSheet, Position.from(spSpalte, zeileOhneNamen));
		assertThat(spText).as("SP-Wert in Zeile ohne Namen muss beim Refresh gelöscht werden").isNullOrEmpty();
	}

	@Test
	void aktivOhneNamenWirdBeiRefreshGeloescht() throws Exception {
		SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.NUR_TEAMNAME, true, false);

		int ersteDatenZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
		int aktivSpalte = meldeListeNew.getAktivSpalte();
		XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();

		int zeileOhneNamen = ersteDatenZeile + 4;
		meldeListeNew.getSheetHelper().setNumberValueInCell(
				NumberCellValue.from(xSheet, Position.from(aktivSpalte, zeileOhneNamen)).setValue(1));

		SchweizerMeldeListeSheetUpdate meldeListeUpdate = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
		meldeListeUpdate.vollstaendigAktualisieren();

		String aktivText = meldeListeNew.getSheetHelper().getTextFromCell(xSheet,
				Position.from(aktivSpalte, zeileOhneNamen));
		assertThat(aktivText).as("Aktiv-Wert in Zeile ohne Namen muss beim Refresh gelöscht werden").isNullOrEmpty();
	}

	@Test
	void reinesLeerzeichenInAktivSpalteWirdBeiRefreshGeloescht() throws Exception {
		SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.DOUBLETTE, true, false);

		int ersteDatenZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
		int aktivSpalte = meldeListeNew.getAktivSpalte();
		XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();

		// Zeile MIT Namen, aber Aktiv-Zelle enthält nur ein Leerzeichen statt wirklich leer zu sein -
		// exakt das Muster aus der Bugreport-ODS (ODF text:s-Leerzeichen).
		meldeListeNew.getSheetHelper()
				.setStringValueInCell(StringCellValue.from(xSheet, Position.from(aktivSpalte, ersteDatenZeile), " "));

		SchweizerMeldeListeSheetUpdate meldeListeUpdate = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
		meldeListeUpdate.vollstaendigAktualisieren();

		String aktivText = meldeListeNew.getSheetHelper().getTextFromCell(xSheet,
				Position.from(aktivSpalte, ersteDatenZeile));
		assertThat(aktivText).as("Reines Leerzeichen in der Aktiv-Spalte muss beim Refresh gelöscht werden")
				.isNullOrEmpty();
	}
}
