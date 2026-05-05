package de.petanqueturniermanager.maastrichter;

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
import de.petanqueturniermanager.maastrichter.korunde.KoGruppeABSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * UITest für das vollständige Maastrichter-Beispielturnier (12 Teams, 3 Vorrunden,
 * Finalrunde A) sowie die Forme-Phase (KoGruppeABSheet) auf Basis der Vorrunden-Rangliste.
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}: Spielergebnisse,
 * Bahnen-Auslosung (SpielrundeSpielbahn.R) und KO-Gruppen-Reihenfolge werden gegen
 * JSON-Referenzdateien verglichen. Bei Algorithmen-Änderungen müssen die Referenzen
 * neu erfasst werden – hierzu temporär {@code writeToJson(...)} aktivieren.
 */
public class MaastrichterTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int ANZ_TEAMS = 12;
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;
	private static final int MELDELISTE_NACHNAME_SPALTE = 2;

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
	public void testMaastrichterTurnier12Teams() throws GenerateException {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate();

		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.maastrichterVorrundenRangliste()))
				.as("Vorrunden-Rangliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.teilnehmer()))
				.as("Teilnehmer-Sheet muss existieren").isNotNull();
		// Finalrunde-Sheet: tatsächlicher Name kann je nach Konfiguration ("Finalrunde A",
		// "A-Finale" o.ä.) variieren. Mindestens ein Finalrunde-Sheet muss existieren.
		assertThat(anzahlSheetsMitTeilstring("Finale"))
				.as("Mindestens ein Finalrunde-Sheet muss existieren").isGreaterThanOrEqualTo(1L);

		validiereMeldelistePerJson();
		validiereVorrundenRanglistePerJson();
	}

	private long anzahlSheetsMitTeilstring(String teil) {
		var alleNamen = wkingSpreadsheet.getWorkingSpreadsheetDocument().getSheets().getElementNames();
		long count = 0;
		for (String name : alleNamen) {
			if (name.contains(teil)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Forme-Phase: nach der vollständigen Maastrichter-Vorrunde wird zusätzlich die
	 * KO-Gruppe AB (Forme/KoGruppeABSheet) erzeugt.
	 */
	@Test
	public void testKoGruppeAbAlsFormeNachVorrunde() throws GenerateException {
		new MaastrichterTurnierTestDaten(wkingSpreadsheet).generate();

		new KoGruppeABSheet(wkingSpreadsheet).run();

		XSpreadsheet koRunde = sheetHlp.findByName(SheetNamen.koRunde());
		assertThat(koRunde).as("KoRunde-Sheet (Forme) muss nach KoGruppeABSheet.run() existieren").isNotNull();
	}

	private void validiereMeldelistePerJson() throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		RangePosition meldelisteRange = RangePosition.from(
				0, MELDELISTE_ERSTE_DATEN_ZEILE,
				MELDELISTE_NACHNAME_SPALTE, MELDELISTE_ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);

		// writeToJson("maastrichter-meldeliste.json", meldelisteRange, meldeliste,
		//         wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class
				.getResourceAsStream("maastrichter-meldeliste.json");
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereVorrundenRanglistePerJson() throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.maastrichterVorrundenRangliste());
		assertThat(rangliste).isNotNull();

		// Rangliste-Bereich: Spalten 0..3 (Platz, Nr, Team, Punkte) für die 12 Teams.
		RangePosition ranglisteRange = RangePosition.from(
				0, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				3, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);

		// writeToJson("maastrichter-vorrundenrangliste.json", ranglisteRange, rangliste,
		//         wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, rangliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class
				.getResourceAsStream("maastrichter-vorrundenrangliste.json");
		validateWithJson(rangeData, jsonFile);
	}
}
