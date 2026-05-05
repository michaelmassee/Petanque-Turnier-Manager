package de.petanqueturniermanager.ko;

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
	}

	@Test
	public void testKoTurnier16TeamsMitGruppen() throws GenerateException {
		new Ko16TeamsTurnierTestDaten(wkingSpreadsheet).generate();
		validiereGrundstruktur(16);
		validiereMeldelistePerJson(16, "ko-meldeliste-16.json");
	}

	@Test
	public void testKoTurnier10TeamsCadrage() throws GenerateException {
		new KoCadrageTurnierTestDaten(wkingSpreadsheet).generate();
		validiereGrundstruktur(10);
		validiereMeldelistePerJson(10, "ko-meldeliste-10-cadrage.json");
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
}
