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
		// Bei 12 Teams nutzt die Maastrichter-Finalrunde das KO-Bracket (Sheet-Name "A-Finale").
		assertThat(sheetHlp.findByName(SheetNamen.koFinaleGruppe("A")))
				.as("A-Finale-Sheet muss existieren").isNotNull();

		validiereMeldelistePerJson();
		validiereVorrundenErgebnissePerJson(1, "maastrichter-vorrunde-1.json");
		validiereVorrundenErgebnissePerJson(2, "maastrichter-vorrunde-2.json");
		validiereVorrundenErgebnissePerJson(3, "maastrichter-vorrunde-3.json");
		validiereVorrundenRanglistePerJson();
		validiereTeilnehmerPerJson();
		validiereFinaleAPerJson();
	}

	private void validiereVorrundenErgebnissePerJson(int rundeNr, String referenzDatei) throws GenerateException {
		String sheetName = SheetNamen.maastrichterVorrunde(rundeNr);
		XSpreadsheet sheet = sheetHlp.findByName(sheetName);
		assertThat(sheet).as("Vorrunden-Sheet '%s' muss existieren", sheetName).isNotNull();

		// Schweizer-Format: 12 Teams → 6 Paarungen pro Runde, Spalten 0..6 (Bahn, TeamA-Nr, TeamA-Name, ErgA, ErgB, TeamB-Name, TeamB-Nr).
		RangePosition vorrundenRange = RangePosition.from(
				0, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				6, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + (ANZ_TEAMS / 2) - 1);

		// writeToJson(referenzDatei, vorrundenRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(vorrundenRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereTeilnehmerPerJson() throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.teilnehmer());
		assertThat(sheet).isNotNull();

		// Teilnehmerliste (Doublette → 2 Spieler pro Team × 12 Teams = 24 Spieler).
		// Bereich: Spalten 0..3 (Startnummer, Vorname, Nachname, Verein/Teamname), Zeilen ab 1 für 24 Spieler.
		RangePosition teilnehmerRange = RangePosition.from(0, 1, 3, 24);

		// writeToJson("maastrichter-teilnehmer.json", teilnehmerRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(teilnehmerRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class.getResourceAsStream("maastrichter-teilnehmer.json");
		validateWithJson(rangeData, jsonFile);
	}

	private void validiereFinaleAPerJson() throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.koFinaleGruppe("A"));
		assertThat(sheet).isNotNull();

		// Großzügiger Bereich – analog KO-Turnierbaum.
		RangePosition finaleRange = RangePosition.from(0, 0, 23, 32);

		// writeToJson("maastrichter-finale-a.json", finaleRange, sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument());

		RangeData rangeData = rangeDateFromRangePosition(finaleRange, sheet,
				wkingSpreadsheet.getWorkingSpreadsheetDocument());

		InputStream jsonFile = MaastrichterTurnierTestDatenUITest.class.getResourceAsStream("maastrichter-finale-a.json");
		validateWithJson(rangeData, jsonFile);
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
