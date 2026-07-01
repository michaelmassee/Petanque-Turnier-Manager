package de.petanqueturniermanager.jedergegenjeden.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * UITest für die JGJ-Beispielturniere in mehreren Konstellationen:
 * <ul>
 *   <li>10 Teams Tête (Standardvariante).</li>
 *   <li>17 Teams Doublette mit Gruppengrösse 6 – deckt den Mehrgruppen-Pfad ab.</li>
 *   <li>8 Teams Triplette in 2 Gruppen à 4 – erzeugt zusätzlich die gruppenübergreifende
 *       Gesamtrangliste.</li>
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

	@Test
	public void testJGJTurnier8TeamsTriplette2Gruppen() throws GenerateException {
		final int anzTeams = 8;
		new JGJTriplette2Gruppen4TurnierTestDaten(wkingSpreadsheet).generate();

		validiereGrundstruktur();
		validiereMeldelistePerJson(anzTeams, "jgj-meldeliste-2g4t.json");
		validiereSpielplanPerJson("jgj-spielplan-2g4t.json");
		validiereRanglistePerJson(anzTeams, "jgj-rangliste-2g4t.json");
		validiereGesamtranglistePerJson(anzTeams, "jgj-gesamtrangliste-2g4t.json");

		new JGJRanglisteDirektvergleichSheet(wkingSpreadsheet).run();
		validiereDirektvergleichPerJson(anzTeams, "jgj-direktvergleich-2g4t.json");
	}

	/**
	 * Korrektheit der PTM-Metadaten (10 Teams Tête): Meldeliste, Spielplan, Rangliste und das
	 * separat erzeugte Direktvergleich-Blatt müssen je exakt ihren Identitäts-Schlüssel tragen.
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel10TeamsTete() throws GenerateException {
		new JGJTurnierTestDaten(wkingSpreadsheet).generate();
		new JGJRanglisteDirektvergleichSheet(wkingSpreadsheet).run();

		Map<String, String> erwartung = jgjBasisErwartung();
		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	/**
	 * Korrektheit der PTM-Metadaten (17 Teams Doublette, Gruppengröße 6 → 3 Gruppen): zusätzlich
	 * zu den Standard-Blättern müssen die drei Gruppen-Spielplan-Blätter A/B/C ihren
	 * jeweiligen Identitäts-Schlüssel tragen.
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel17TeamsDoublette() throws GenerateException {
		new JGJDoublette17TurnierTestDaten(wkingSpreadsheet).generate();
		new JGJRanglisteDirektvergleichSheet(wkingSpreadsheet).run();

		Map<String, String> erwartung = jgjBasisErwartung();
		for (String gruppe : new String[]{"A", "B", "C"}) {
			erwartung.put(SheetNamen.jgjGruppeSpielplan(gruppe),
					SheetMetadataHelper.schluesselJgjGruppeSpielplan(gruppe));
		}
		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	private Map<String, String> jgjBasisErwartung() {
		Map<String, String> erwartung = new LinkedHashMap<>();
		erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_JGJ_MELDELISTE);
		erwartung.put(SheetNamen.spielplan(), SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN);
		erwartung.put(SheetNamen.rangliste(), SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE);
		erwartung.put(SheetNamen.direktvergleich(), SheetMetadataHelper.SCHLUESSEL_JGJ_DIREKTVERGLEICH);
		return erwartung;
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

	private void validiereGesamtranglistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.jgjGesamtrangliste());
		assertThat(sheet).as("Gesamtrangliste-Sheet muss existieren").isNotNull();

		// Gesamtrangliste: 10 Spalten (Nr..Spielpunkte-Diff), eine Datenzeile je Team (keine Gruppen-Header).
		RangePosition gesamtRange = RangePosition.from(0, 2, 9, 2 + anzTeams - 1);

		// writeToJson(referenzDatei, gesamtRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(gesamtRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = JGJTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	/**
	 * Regression im Kiosk-Modus: nach voller 10-Team-Turniergenerierung muss ein
	 * erneutes {@link JGJRanglisteSheet#run()} unter aktivem TurnierModus +
	 * JGJ-Blattschutz sauber durchlaufen.
	 */
	@Test
	public void kioskModus_ranglisteUpdateNach10TeamTurnier() throws GenerateException {
		new JGJTurnierTestDaten(wkingSpreadsheet).generate();
		mitKioskModus(TurnierSystem.JGJ, () -> new JGJRanglisteSheet(wkingSpreadsheet).run());

		assertThat(sheetHlp.findByName(SheetNamen.rangliste()))
				.as("JGJ-Rangliste muss nach Kiosk-Update weiterhin existieren")
				.isNotNull();
	}
}
