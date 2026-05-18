package de.petanqueturniermanager.liga.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * UITest fuer LigaMeldeListeSheetTestDaten.<br>
 * Regressionstest: testNamenEinfuegen() darf keine NullPointerException werfen,
 * wenn das Sheet zuvor korrekt angelegt wurde (Bugfix: doRun() rief
 * testNamenEinfuegen() auf bevor das Sheet existierte).
 */
public class LigaMeldeListeSheetUITest extends BaseCalcUITest {

	private LigaMeldeListeSheetNew meldeListe;

	@BeforeEach
	public void meldelisteAnlegen() throws GenerateException {
		meldeListe = new LigaMeldeListeSheetNew(wkingSpreadsheet);
		meldeListe.createMeldelisteWithParams("Test Gruppe");
	}

	@Test
	public void testNamenEinfuegenGeradeAnzahl() throws GenerateException {
		LigaMeldeListeSheetTestDaten testDaten = new LigaMeldeListeSheetTestDaten(wkingSpreadsheet, true);
		testDaten.testNamenEinfuegen();

		XSpreadsheet sheet = testDaten.getXSpreadSheet();
		assertThat(sheet).as("Meldelisten-Sheet muss nach testNamenEinfuegen existieren").isNotNull();
	}

	@Test
	public void testNamenEinfuegenUngeradeAnzahl() throws GenerateException {
		LigaMeldeListeSheetTestDaten testDaten = new LigaMeldeListeSheetTestDaten(wkingSpreadsheet, false);
		testDaten.testNamenEinfuegen();

		XSpreadsheet sheet = testDaten.getXSpreadSheet();
		assertThat(sheet).as("Meldelisten-Sheet muss nach testNamenEinfuegen existieren").isNotNull();
	}

	/**
	 * Regression im Kiosk-Modus: das anschließende Schützen der Liga-Sheets muss
	 * sauber durchlaufen und die Schutz-Invariante (Sheets geschützt, editierbare
	 * Bereiche bleiben editierbar) ist erfüllt.
	 */
	@Test
	public void kioskModus_meldelisteBleibtMitEditierbarenBereichenGesperrt() throws GenerateException {
		new LigaMeldeListeSheetTestDaten(wkingSpreadsheet, true).testNamenEinfuegen();
		mitKioskModus(TurnierSystem.LIGA, () -> {
			// reines Smoke-Setup: schuetzen() läuft, Invariante wird durch mitKioskModus geprüft.
		});
	}
}
