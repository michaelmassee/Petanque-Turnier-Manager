package de.petanqueturniermanager.liga.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;

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
}
