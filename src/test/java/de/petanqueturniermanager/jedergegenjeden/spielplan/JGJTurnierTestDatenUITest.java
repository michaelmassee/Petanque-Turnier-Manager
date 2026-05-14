package de.petanqueturniermanager.jedergegenjeden.spielplan;

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
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteDirektvergleichSheet;

/**
 * UITest für die JGJ-Beispielturniere in zwei Konstellationen:
 * <ul>
 *   <li>10 Teams Tête (Standardvariante).</li>
 *   <li>17 Teams Doublette mit Gruppengrösse 6 – deckt den Mehrgruppen-Pfad ab.</li>
 * </ul>
 * Jeder Test validiert Meldeliste, Spielplan, Rangliste und das separat erzeugte
 * Direktvergleich-Sheet gegen JSON-Referenzdateien.
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}; bei Algorithmen-
 * Änderungen Referenz-JSONs neu erfassen (writeToJson temporär aktivieren).
 */
public class JGJTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

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
	public void testJGJTurnier10TeamsTete() throws GenerateException {
		final int anzTeams = 10;
		new JGJTurnierTestDaten(wkingSpreadsheet).generate();

		validiereGrundstruktur();
		validiereMeldelistePerJson(anzTeams, "jgj-meldeliste.json");
		validiereSpielplanPerJson("jgj-spielplan.json");
		validiereRanglistePerJson(anzTeams, "jgj-rangliste.json");

		new JGJRanglisteDirektvergleichSheet(wkingSpreadsheet).run();
		validiereDirektvergleichPerJson(anzTeams, "jgj-direktvergleich.json");
	}

	@Test
	public void testJGJTurnier17TeamsDoublette() throws GenerateException {
		final int anzTeams = 17;
		new JGJDoublette17TurnierTestDaten(wkingSpreadsheet).generate();

		validiereGrundstruktur();
		validiereMeldelistePerJson(anzTeams, "jgj-meldeliste-17.json");
		validiereSpielplanPerJson("jgj-spielplan-17.json");
		validiereRanglistePerJson(anzTeams, "jgj-rangliste-17.json");

		new JGJRanglisteDirektvergleichSheet(wkingSpreadsheet).run();
		validiereDirektvergleichPerJson(anzTeams, "jgj-direktvergleich-17.json");
	}

	private void validiereGrundstruktur() {
		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.spielplan()))
				.as("Spielplan-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.rangliste()))
				.as("Rangliste-Sheet muss existieren").isNotNull();
	}

	private void validiereMeldelistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		RangePosition meldelisteRange = RangePosition.from(
				0, MELDELISTE_ERSTE_DATEN_ZEILE,
				2, MELDELISTE_ERSTE_DATEN_ZEILE + anzTeams - 1);

		// writeToJson(referenzDatei, meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = JGJTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereSpielplanPerJson(String referenzDatei) throws GenerateException {
		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		// Großzügiger Bereich, deckt 10 + 17 Teams (Doublette, Gruppengröße 6) ab.
		RangePosition spielplanRange = RangePosition.from(0, 0, 30, 80);

		// writeToJson(referenzDatei, spielplanRange, spielplan, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(spielplanRange, spielplan,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = JGJTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereRanglistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		// Bei mehreren Gruppen kommen Gruppen-Header-Zeilen dazu (bis zu anzTeams/2 zusätzliche Zeilen).
		// Großzügiger Puffer von 10 deckt 17-Teams-Doublette (3 Gruppen) sicher ab.
		RangePosition ranglisteRange = RangePosition.from(0, 2, 6, 2 + anzTeams + 10);

		// writeToJson(referenzDatei, ranglisteRange, rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, rangliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = JGJTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereDirektvergleichPerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.direktvergleich());
		assertThat(sheet).as("Direktvergleich-Sheet muss existieren").isNotNull();

		// Direktvergleich-Matrix: anzTeams × anzTeams plus Header-Zeilen/Spalten.
		// Großzügig Spalten 0..(anzTeams+5), Zeilen 0..(anzTeams+10).
		RangePosition direktRange = RangePosition.from(0, 0, anzTeams + 5, anzTeams + 10);

		// writeToJson(referenzDatei, direktRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(direktRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = JGJTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}
}
