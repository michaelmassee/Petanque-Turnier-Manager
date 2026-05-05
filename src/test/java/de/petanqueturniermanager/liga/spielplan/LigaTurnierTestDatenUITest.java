package de.petanqueturniermanager.liga.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * UITest für das vollständige Liga-Beispielturnier (6 Teams, Hin- und Rückrunde):
 * Meldeliste, Spielplan mit Zufallsergebnissen und Rangliste.
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}; bei
 * Algorithmen-Änderungen Referenz-JSONs neu erfassen (writeToJson temporär
 * aktivieren und Datei nach src/test/resources/.../liga/spielplan/ kopieren).
 */
public class LigaTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int ANZ_TEAMS = 6;
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 2;

	@BeforeEach
	@Override
	public void beforeTest() {
		super.beforeTest();
		RandomSource.setSeed(SEED_FUER_TESTS);
	}

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	public void testLigaTurnier6Teams() throws GenerateException {
		new LigaTurnierTestDaten(wkingSpreadsheet).erzeugeBeispielturnier();

		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.spielplan()))
				.as("Spielplan-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.rangliste()))
				.as("Rangliste-Sheet muss existieren").isNotNull();

		validiereMeldelistePerJson();
		validiereSpielplanPerJson();
		validiereRanglistePerJson();
	}

	private void validiereMeldelistePerJson() throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		// Meldeliste: Spalten 0..2 (Nr, Vorname/Teamname, ...), 6 Datenzeilen ab Zeile 2.
		RangePosition meldelisteRange = RangePosition.from(
				0, MELDELISTE_ERSTE_DATEN_ZEILE,
				2, MELDELISTE_ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);

		// writeToJson("liga-meldeliste.json", meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = LigaTurnierTestDatenUITest.class.getResourceAsStream("liga-meldeliste.json");
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereSpielplanPerJson() throws GenerateException {
		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		// 6 Teams, Hin- und Rückrunde → 10 Spieltage. Großzügiger Bereich.
		RangePosition spielplanRange = RangePosition.from(0, 0, 12, 80);

		// writeToJson("liga-spielplan.json", spielplanRange, spielplan, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(spielplanRange, spielplan,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = LigaTurnierTestDatenUITest.class.getResourceAsStream("liga-spielplan.json");
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereRanglistePerJson() throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		// Rangliste: Spalten 0..6 für 6 Teams (Platz, Nr, Name, Punkte, ...).
		RangePosition ranglisteRange = RangePosition.from(0, 1, 6, 1 + ANZ_TEAMS);

		// writeToJson("liga-rangliste.json", ranglisteRange, rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, rangliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = LigaTurnierTestDatenUITest.class.getResourceAsStream("liga-rangliste.json");
		validateWithJson(rangeData, jsonFile);
	}
}
