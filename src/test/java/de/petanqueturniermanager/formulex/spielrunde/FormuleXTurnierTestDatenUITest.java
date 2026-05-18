package de.petanqueturniermanager.formulex.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * UITest für das vollständige Formule-X-Beispielturnier (39 Teams, 5 Spielrunden):
 * Meldeliste, alle fünf Spielrunden mit Zufallsergebnissen, Rangliste und Teilnehmerliste.
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}; bei Algorithmen-
 * Änderungen Referenz-JSONs neu erfassen (writeToJson temporär aktivieren).
 */
public class FormuleXTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int ANZ_TEAMS = FormuleXTurnierTestDaten.ANZ_TEAMS;
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
	public void testFormuleXTurnier39TeamsFuenfRunden() throws GenerateException {
		new FormuleXTurnierTestDaten(wkingSpreadsheet).generate();

		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.formulexRangliste()))
				.as("Formule-X-Rangliste-Sheet muss existieren").isNotNull();
		assertThat(sheetHlp.findByName(SheetNamen.teilnehmer()))
				.as("Teilnehmer-Sheet muss existieren").isNotNull();

		validiereMeldelistePerJson();
		validiereSpielrundePerJson(1, "formulex-spielrunde-1.json");
		validiereSpielrundePerJson(2, "formulex-spielrunde-2.json");
		validiereSpielrundePerJson(3, "formulex-spielrunde-3.json");
		validiereSpielrundePerJson(4, "formulex-spielrunde-4.json");
		validiereSpielrundePerJson(5, "formulex-spielrunde-5.json");
		validiereRanglistePerJson();
	}

	private void validiereMeldelistePerJson() throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		// Spalten 0..2 (Nr, Vorname, Nachname), 39 Datenzeilen.
		RangePosition meldelisteRange = RangePosition.from(
				0, MELDELISTE_ERSTE_DATEN_ZEILE,
				2, MELDELISTE_ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);

		// writeToJson("formulex-meldeliste.json", meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = FormuleXTurnierTestDatenUITest.class.getResourceAsStream("formulex-meldeliste.json");
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereSpielrundePerJson(int rundeNr, String referenzDatei) throws GenerateException {
		String sheetName = SheetNamen.formulexSpielrunde(rundeNr);
		XSpreadsheet sheet = sheetHlp.findByName(sheetName);
		assertThat(sheet).as("Spielrunde-Sheet '%s' muss existieren", sheetName).isNotNull();

		// 39 Teams → 19 Paarungen + 1 Freilos = 20 Zeilen, Spalten 0..6 (Bahn, NrA, NrB, ErgA, ErgB, ...).
		RangePosition spielrundenRange = RangePosition.from(
				0, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				6, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 19);

		// writeToJson(referenzDatei, spielrundenRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(spielrundenRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = FormuleXTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereRanglistePerJson() throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.formulexRangliste());
		// Rangliste: Platz/Nr/Team/Punkte – Spalten 0..6 für 39 Teams.
		RangePosition ranglisteRange = RangePosition.from(0, 2, 6, 2 + ANZ_TEAMS - 1);

		// writeToJson("formulex-rangliste.json", ranglisteRange, rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(ranglisteRange, rangliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = FormuleXTurnierTestDatenUITest.class.getResourceAsStream("formulex-rangliste.json");
		validateWithJson(rangeData, jsonFile);
	}

	/**
	 * Regression im Kiosk-Modus: nach voller 39-Team-Turniergenerierung muss ein
	 * erneutes {@link FormuleXRanglisteSheetUpdate#doRun()} unter aktivem TurnierModus
	 * + FormuleX-Blattschutz sauber durchlaufen.
	 */
	@Test
	public void kioskModus_ranglisteUpdateNach39TeamTurnier() throws GenerateException {
		new FormuleXTurnierTestDaten(wkingSpreadsheet).generate();
		mitKioskModus(TurnierSystem.FORMULEX, () ->
				new FormuleXRanglisteSheetUpdate(wkingSpreadsheet).doRun());

		assertThat(sheetHlp.findByName(SheetNamen.formulexRangliste()))
				.as("FormuleX-Rangliste muss nach Kiosk-Update weiterhin existieren")
				.isNotNull();
	}
}
