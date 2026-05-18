package de.petanqueturniermanager.supermelee.spieltagrangliste;

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
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * UITest für das vollständige Supermelee-Beispielturnier (100 Spieler, 5 Spieltage):
 * Meldeliste plus Spieltag-Ranglisten 1 und 5 gegen JSON-Referenzen validiert.
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}; bei
 * Algorithmen-Änderungen Referenz-JSONs neu erfassen (writeToJson temporär aktivieren).
 */
public class SupermeleeTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int ANZ_SPIELER = 100;
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
	public void testSupermeleeTurnier100SpielerFuenfSpieltage() throws GenerateException {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();

		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.spieltagRangliste(1)))
				.as("Spieltag-Rangliste 1 muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.spieltagRangliste(5)))
				.as("Spieltag-Rangliste 5 muss existieren").isNotNull();

		validiereMeldelistePerJson();
		validiereSpieltagRanglistePerJson(1, "supermelee-spieltagrangliste-1.json");
		validiereSpieltagRanglistePerJson(2, "supermelee-spieltagrangliste-2.json");
		validiereSpieltagRanglistePerJson(3, "supermelee-spieltagrangliste-3.json");
		validiereSpieltagRanglistePerJson(4, "supermelee-spieltagrangliste-4.json");
		validiereSpieltagRanglistePerJson(5, "supermelee-spieltagrangliste-5.json");
	}

	private void validiereMeldelistePerJson() throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		// Meldeliste: Spalten 0..2 (Nr, Vorname, Nachname), 100 Spieler-Zeilen ab Zeile 3.
		RangePosition meldelisteRange = RangePosition.from(
				0, MELDELISTE_ERSTE_DATEN_ZEILE,
				2, MELDELISTE_ERSTE_DATEN_ZEILE + ANZ_SPIELER - 1);

		// writeToJson("supermelee-meldeliste.json", meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = SupermeleeTurnierTestDatenUITest.class.getResourceAsStream("supermelee-meldeliste.json");
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereSpieltagRanglistePerJson(int spieltagNr, String referenzDatei) throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.spieltagRangliste(spieltagNr));
		assertThat(sheet).as("Spieltag-Rangliste %d", spieltagNr).isNotNull();

		// Großzügiger Bereich – Spalten 0..8 für 100 Spieler.
		RangePosition ranglisteRange = RangePosition.from(0, 0, 8, 110);

		// writeToJson(referenzDatei, ranglisteRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = SupermeleeTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	/**
	 * Regression im Kiosk-Modus: nach voller Beispielturnier-Generierung (100 Spieler, 5 Spieltage)
	 * muss ein erneutes Update der Spieltag-1-Rangliste unter aktivem TurnierModus durchlaufen
	 * und die Schutz-Invariante erfüllt bleiben.
	 */
	@Test
	public void kioskModus_spieltagRanglisteUpdateNach100SpielerTurnier() throws GenerateException {
		new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
		mitKioskModus(TurnierSystem.SUPERMELEE, () ->
				new SpieltagRanglisteSheet(wkingSpreadsheet, SpielTagNr.from(1)).run());

		assertThat(sheetHlp.findByName(SheetNamen.spieltagRangliste(1)))
				.as("Spieltag-Rangliste 1 muss nach Kiosk-Update weiterhin existieren")
				.isNotNull();
	}
}
