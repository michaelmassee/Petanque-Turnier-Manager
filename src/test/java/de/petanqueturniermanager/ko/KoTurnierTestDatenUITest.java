package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetUpdate;
import de.petanqueturniermanager.toolbar.ToolbarAktionDispatcher;

/**
 * UITest für das vollständige K.-O.-Testturnier in drei Varianten:
 * 8 Teams (Viertelfinale), 16 Teams (Achtelfinale + 2 Gruppen) und 10 Teams (Cadrage).
 *
 * <p>Reproduzierbarkeit über {@link RandomSource#setSeed(long)}: Spielerergebnisse,
 * Setzposition-Shuffle und Bahnen-Auslosung werden gegen JSON-Referenzdateien
 * verglichen. Bei Algorithmen-Änderungen müssen die Referenzen neu erfasst werden
 * (Aufruf {@code writeToJson(...)} temporär aktivieren – schreibt nach
 * {@code $HOME}, von dort in {@code src/test/resources/.../ko/} kopieren).
 */
public class KoTurnierTestDatenUITest extends BaseCalcUITest {

	private static final long SEED_FUER_TESTS = 42L;
	private static final int MELDELISTE_NR_SPALTE = 0;
	private static final int MELDELISTE_NACHNAME_SPALTE = 2;
	private static final int ERSTE_DATEN_ZEILE = 3;

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
	public void testKoTurnier8Teams() throws GenerateException {
		new KoTurnierTestDaten(wkingSpreadsheet, 8).generate();
		validiereGrundstruktur(8);
		validiereMeldelistePerJson(8, "ko-meldeliste-8.json");
		validiereTurnierbaumPerJson(SheetNamen.koTurnierbaumEinzel(), "ko-turnierbaum-8.json");
	}

	@Test
	public void testKoTurnier16TeamsMitGruppen() throws GenerateException {
		new Ko16TeamsTurnierTestDaten(wkingSpreadsheet).generate();
		validiereGrundstruktur(16);
		validiereMeldelistePerJson(16, "ko-meldeliste-16.json");
		validiereTurnierbaumPerJson(SheetNamen.koTurnierbaumGruppe("A"), "ko-turnierbaum-16-gruppe-a.json");
		validiereTurnierbaumPerJson(SheetNamen.koTurnierbaumGruppe("B"), "ko-turnierbaum-16-gruppe-b.json");
	}

	@Test
	public void testKoTurnier10TeamsCadrage() throws GenerateException {
		new KoCadrageTurnierTestDaten(wkingSpreadsheet).generate();
		validiereGrundstruktur(10);
		validiereMeldelistePerJson(10, "ko-meldeliste-10-cadrage.json");
		validiereTurnierbaumPerJson(SheetNamen.koTurnierbaumEinzel(), "ko-turnierbaum-10-cadrage.json");
	}

	/**
	 * Korrektheit der PTM-Metadaten (8 Teams, Einzel-Bracket): Meldeliste und das einzelne
	 * Turnierbaum-Blatt müssen je exakt ihren Identitäts-Schlüssel tragen.
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel8Teams() throws GenerateException {
		new KoTurnierTestDaten(wkingSpreadsheet, 8).generate();

		Map<String, String> erwartung = new LinkedHashMap<>();
		erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE);
		erwartung.put(SheetNamen.koTurnierbaumEinzel(), SheetMetadataHelper.schluesselKoTurnierbaum(""));

		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	/**
	 * Korrektheit der PTM-Metadaten (16 Teams, 2 Gruppen): Meldeliste plus die beiden
	 * Gruppen-Turnierbäume A/B müssen je exakt ihren Identitäts-Schlüssel tragen.
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel16TeamsMitGruppen() throws GenerateException {
		new Ko16TeamsTurnierTestDaten(wkingSpreadsheet).generate();

		Map<String, String> erwartung = new LinkedHashMap<>();
		erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE);
		erwartung.put(SheetNamen.koTurnierbaumGruppe("A"), SheetMetadataHelper.schluesselKoTurnierbaum("A"));
		erwartung.put(SheetNamen.koTurnierbaumGruppe("B"), SheetMetadataHelper.schluesselKoTurnierbaum("B"));

		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	/**
	 * Korrektheit der PTM-Metadaten (10 Teams mit Cadrage, Einzel-Bracket): Meldeliste und das
	 * einzelne Turnierbaum-Blatt müssen je exakt ihren Identitäts-Schlüssel tragen.
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel10TeamsCadrage() throws GenerateException {
		new KoCadrageTurnierTestDaten(wkingSpreadsheet).generate();

		Map<String, String> erwartung = new LinkedHashMap<>();
		erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_KO_MELDELISTE);
		erwartung.put(SheetNamen.koTurnierbaumEinzel(), SheetMetadataHelper.schluesselKoTurnierbaum(""));

		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	private void validiereGrundstruktur(int erwarteteTeams) throws GenerateException {
		assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
				.as("Meldeliste-Sheet muss existieren").isNotNull();

		long anzTurnierbaumSheets = anzahlSheetsMitPraefix(SheetNamen.koTurnierbaumEinzel());
		assertThat(anzTurnierbaumSheets)
				.as("Mindestens ein Turnierbaum-Sheet (Praefix '%s') muss existieren",
						SheetNamen.koTurnierbaumEinzel())
				.isGreaterThanOrEqualTo(1L);

		assertThat(erwarteteTeams).isPositive();
	}

	private long anzahlSheetsMitPraefix(String praefix) {
		var alleNamen = wkingSpreadsheet.getWorkingSpreadsheetDocument().getSheets().getElementNames();
		long count = 0;
		for (String name : alleNamen) {
			if (name.startsWith(praefix)) {
				count++;
			}
		}
		return count;
	}

	private void validiereTurnierbaumPerJson(String sheetName, String referenzDatei) throws GenerateException {
		XSpreadsheet turnierbaum = sheetHlp.findByName(sheetName);
		assertThat(turnierbaum).as("Turnierbaum-Sheet '%s' muss existieren", sheetName).isNotNull();

		// Großzügiger Bereich, der alle 8/10/16-Team-Turnierbäume sicher umfasst.
		// Mit Bahn-Spielbahn=N und Cadrage: bis zu 5 Runden × 4 Spalten + Cadrage-Offset = ~24 Spalten;
		// 16 Teams brauchen bis zu ~32 Datenzeilen.
		RangePosition turnierbaumRange = RangePosition.from(0, 0, 23, 32);

		// writeToJson(referenzDatei, turnierbaumRange, turnierbaum, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(turnierbaumRange, turnierbaum,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = KoTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereMeldelistePerJson(int anzTeams, String referenzDatei) throws GenerateException {
		XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
		assertThat(meldeliste).isNotNull();

		RangePosition meldelisteRange = RangePosition.from(
				MELDELISTE_NR_SPALTE, ERSTE_DATEN_ZEILE,
				MELDELISTE_NACHNAME_SPALTE, ERSTE_DATEN_ZEILE + anzTeams - 1);

		// Zum Erfassen einer neuen Referenzdatei: Zeile entkommentieren, Test laufen lassen,
		// dann die Datei aus $HOME nach src/test/resources/de/petanqueturniermanager/ko/ kopieren.
		// writeToJson(referenzDatei, meldelisteRange, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(meldelisteRange, meldeliste,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = KoTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	/**
	 * Regression im Kiosk-Modus: nach Vollaufbau (8 Teams) muss ein erneutes
	 * {@link KoTurnierbaumSheet#run()} unter aktivem TurnierModus + KO-Blattschutz
	 * sauber durchlaufen.
	 */
	@Test
	public void kioskModus_turnierbaumRebuildUnterSchutz() throws GenerateException {
		new KoTurnierTestDaten(wkingSpreadsheet, 8).generate();
		mitKioskModus(TurnierSystem.KO, () -> new KoTurnierbaumSheet(wkingSpreadsheet).run());

		assertThat(sheetHlp.findByName(SheetNamen.koTurnierbaumEinzel()))
				.as("Turnierbaum-Sheet muss nach Kiosk-Rebuild weiterhin existieren")
				.isNotNull();
	}

	@Test
	public void turnierbaumAktualisiertMeldelisteMitFehlendenStartnummern() throws GenerateException {
		new KoMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams();
		KoMeldeListeSheetUpdate meldeliste = new KoMeldeListeSheetUpdate(wkingSpreadsheet);
		XSpreadsheet meldelisteSheet = sheetHlp.findByName(SheetNamen.meldeliste());

		int vornameSpalte = meldeliste.getVornameSpalte(0);
		int rangSpalte = meldeliste.getRanglisteSpalte();
		for (int i = 0; i < 4; i++) {
			int zeile = ERSTE_DATEN_ZEILE + i;
			sheetHlp.setStringValueInCell(StringCellValue.from(
					meldelisteSheet, Position.from(vornameSpalte, zeile), "Team " + (i + 1)));
			sheetHlp.setNumberValueInCell(NumberCellValue.from(
					meldelisteSheet, Position.from(rangSpalte, zeile)).setValue(i + 1));
		}

		new KoTurnierbaumSheet(wkingSpreadsheet).run();

		assertThat(sheetHlp.findByName(SheetNamen.koTurnierbaumEinzel()))
				.as("Turnierbaum-Sheet muss auch ohne vorherigen Meldeliste-Update-Klick entstehen")
				.isNotNull();
		for (int i = 0; i < 4; i++) {
			assertThat(sheetHlp.getIntFromCell(meldelisteSheet, Position.from(MELDELISTE_NR_SPALTE, ERSTE_DATEN_ZEILE + i)))
					.as("fehlende Startnummer in Datenzeile %s", i + 1)
					.isEqualTo(i + 1);
		}
	}

	@Test
	public void toolbarWeiterErstelltKoTurnierbaum() throws Exception {
		new KoMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams();
		new DocumentPropertiesHelper(wkingSpreadsheet)
				.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.KO.getId());
		KoMeldeListeSheetUpdate meldeliste = new KoMeldeListeSheetUpdate(wkingSpreadsheet);
		XSpreadsheet meldelisteSheet = sheetHlp.findByName(SheetNamen.meldeliste());

		int vornameSpalte = meldeliste.getVornameSpalte(0);
		int rangSpalte = meldeliste.getRanglisteSpalte();
		for (int i = 0; i < 4; i++) {
			int zeile = ERSTE_DATEN_ZEILE + i;
			sheetHlp.setStringValueInCell(StringCellValue.from(
					meldelisteSheet, Position.from(vornameSpalte, zeile), "Team " + (i + 1)));
			sheetHlp.setNumberValueInCell(NumberCellValue.from(
					meldelisteSheet, Position.from(rangSpalte, zeile)).setValue(i + 1));
		}

		ToolbarAktionDispatcher.weiter(wkingSpreadsheet);
		wartenAufRunnerFertig(30_000);

		assertThat(sheetHlp.findByName(SheetNamen.koTurnierbaumEinzel()))
				.as("Toolbar Weiter muss im KO-System den Turnierbaum erstellen")
				.isNotNull();
	}

	private void wartenAufRunnerFertig(long timeoutMs) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		Thread.sleep(50);
		while (SheetRunner.isRunning() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertThat(SheetRunner.isRunning())
				.as("SheetRunner muss innerhalb von %d ms fertig werden", timeoutMs)
				.isFalse();
	}
}
