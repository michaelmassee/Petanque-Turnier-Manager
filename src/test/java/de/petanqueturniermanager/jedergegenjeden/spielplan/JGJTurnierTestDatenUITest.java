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

/**
 * UITest für das vollständige JGJ-Beispielturnier (10 Teams, Tête): Meldeliste,
 * Spielplan mit Zufallsergebnissen und Rangliste.
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}; bei Algorithmen-
 * Änderungen Referenz-JSONs neu erfassen (writeToJson temporär aktivieren).
 */
public class JGJTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int ANZ_TEAMS = 10;

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
		new JGJTurnierTestDaten(wkingSpreadsheet).generate();

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
		// Meldeliste-Bereich: Spalten 0..2 (Nr, Vorname, Nachname), 10 Datenzeilen ab Zeile 3.
		RangePosition meldelisteRange = RangePosition.from(0, 3, 2, 3 + ANZ_TEAMS - 1);

		// writeToJson("jgj-meldeliste.json", meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = JGJTurnierTestDatenUITest.class.getResourceAsStream("jgj-meldeliste.json");
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereSpielplanPerJson() throws GenerateException {
		XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
		// Spielplan: 10 Teams → 9 Runden, 5 Paarungen pro Runde. Großzügiger Bereich.
		RangePosition spielplanRange = RangePosition.from(0, 0, 30, 60);

		// writeToJson("jgj-spielplan.json", spielplanRange, spielplan, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(spielplanRange, spielplan,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = JGJTurnierTestDatenUITest.class.getResourceAsStream("jgj-spielplan.json");
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereRanglistePerJson() throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		// Rangliste: Platz, Nr, Name, Punkte usw. – Spalten 0..6 für 10 Teams.
		RangePosition ranglisteRange = RangePosition.from(0, 2, 6, 2 + ANZ_TEAMS - 1);

		// writeToJson("jgj-rangliste.json", ranglisteRange, rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, rangliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = JGJTurnierTestDatenUITest.class.getResourceAsStream("jgj-rangliste.json");
		validateWithJson(rangeData, jsonFile);
	}
}
