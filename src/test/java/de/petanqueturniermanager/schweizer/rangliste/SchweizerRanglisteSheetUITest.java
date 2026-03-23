package de.petanqueturniermanager.schweizer.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;

/**
 * UI-Tests für das Schweizer Rangliste-Sheet.
 * Prüft die korrekte Befüllung aller Spalten nach Turniergenerierung.
 */
public class SchweizerRanglisteSheetUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS  = 16;
	private static final int ANZ_RUNDEN = 3;

	private SchweizerTurnierTestDaten testDaten;

	@BeforeEach
	public void setup() {
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet);
	}

	/** Liest alle Rangliste-Datenspalten (TEAM_NR bis PUNKTE_DIFF), ANZ_TEAMS Zeilen. */
	private RangeData ladeRanglisteDaten() throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SchweizerRanglisteSheet.SHEETNAME);
		assertThat(rangliste).as("Rangliste-Sheet muss vorhanden sein").isNotNull();

		RangePosition ranglisteRange = RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);
		return RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
				.getDataFromRange();
	}

	@Test
	public void testMitBuchholz_GrunddatenKorrekt() throws GenerateException {
		testDaten.generate();

		RangeData data = ladeRanglisteDaten();

		assertThat(data).as("Rangliste muss " + ANZ_TEAMS + " Einträge haben").hasSize(ANZ_TEAMS);

		assertThat(data)
				.as("Siege müssen zwischen 0 und " + ANZ_RUNDEN + " liegen")
				.extracting(row -> row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(-1))
				.allSatisfy(siege -> assertThat(siege).isBetween(0, ANZ_RUNDEN));

		assertThat(data)
				.as("BHZ muss >= 0 sein")
				.extracting(row -> row.get(SchweizerRanglisteSheet.BHZ_SPALTE).getIntVal(-1))
				.allSatisfy(bhz -> assertThat(bhz).isGreaterThanOrEqualTo(0));

		assertThat(data)
				.as("FBHZ muss >= 0 sein")
				.extracting(row -> row.get(SchweizerRanglisteSheet.FBHZ_SPALTE).getIntVal(-1))
				.allSatisfy(fbhz -> assertThat(fbhz).isGreaterThanOrEqualTo(0));

		assertThat(data)
				.as("PLATZ muss zwischen 1 und " + ANZ_TEAMS + " liegen")
				.extracting(row -> row.get(SchweizerRanglisteSheet.PLATZ_SPALTE).getIntVal(-1))
				.allSatisfy(platz -> assertThat(platz).isBetween(1, ANZ_TEAMS));

		assertThat(data)
				.as("TeamNr muss > 0 sein")
				.extracting(row -> row.get(SchweizerRanglisteSheet.TEAM_NR_SPALTE).getIntVal(0))
				.allSatisfy(nr -> assertThat(nr).isGreaterThan(0));
	}

	@Test
	public void testPunkteDiffGleichPlusMinus() throws GenerateException {
		testDaten.generate();

		RangeData data = ladeRanglisteDaten();

		for (int i = 0; i < data.size(); i++) {
			var row = data.get(i);
			int plus  = row.get(SchweizerRanglisteSheet.PUNKTE_PLUS_SPALTE).getIntVal(0);
			int minus = row.get(SchweizerRanglisteSheet.PUNKTE_MINUS_SPALTE).getIntVal(0);
			int diff  = row.get(SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE).getIntVal(Integer.MIN_VALUE);
			assertThat(diff)
					.as("Zeile %d: PunkteDiff muss PunktePlus - PunkteMinus sein", i + 1)
					.isEqualTo(plus - minus);
		}
	}

	@Test
	public void testSortierungNachSiege() throws GenerateException {
		testDaten.generate();

		RangeData data = ladeRanglisteDaten();

		for (int i = 0; i < data.size() - 1; i++) {
			int siegeI    = data.get(i).get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(0);
			int siegeIp1  = data.get(i + 1).get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(0);
			assertThat(siegeI)
					.as("Zeile %d: Siege[i]=%d muss >= Siege[i+1]=%d sein", i + 1, siegeI, siegeIp1)
					.isGreaterThanOrEqualTo(siegeIp1);
		}
	}

	@Test
	public void testPlatzMonotonNichtFallend() throws GenerateException {
		testDaten.generate();

		RangeData data = ladeRanglisteDaten();

		for (int i = 0; i < data.size() - 1; i++) {
			int platzI   = data.get(i).get(SchweizerRanglisteSheet.PLATZ_SPALTE).getIntVal(0);
			int platzIp1 = data.get(i + 1).get(SchweizerRanglisteSheet.PLATZ_SPALTE).getIntVal(0);
			assertThat(platzI)
					.as("Zeile %d: PLATZ[i]=%d muss <= PLATZ[i+1]=%d sein", i + 1, platzI, platzIp1)
					.isLessThanOrEqualTo(platzIp1);
		}
	}

	@Test
	public void testGleicherPlatzBeiGleichenKriterien() throws GenerateException {
		testDaten.generate();

		RangeData data = ladeRanglisteDaten();

		for (int i = 0; i < data.size() - 1; i++) {
			var rowI   = data.get(i);
			var rowIp1 = data.get(i + 1);

			int siegeI   = rowI.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(0);
			int siegeIp1 = rowIp1.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(0);
			int bhzI     = rowI.get(SchweizerRanglisteSheet.BHZ_SPALTE).getIntVal(0);
			int bhzIp1   = rowIp1.get(SchweizerRanglisteSheet.BHZ_SPALTE).getIntVal(0);
			int fbhzI    = rowI.get(SchweizerRanglisteSheet.FBHZ_SPALTE).getIntVal(0);
			int fbhzIp1  = rowIp1.get(SchweizerRanglisteSheet.FBHZ_SPALTE).getIntVal(0);
			int diffI    = rowI.get(SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE).getIntVal(0);
			int diffIp1  = rowIp1.get(SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE).getIntVal(0);

			boolean alleGleich = siegeI == siegeIp1 && bhzI == bhzIp1
					&& fbhzI == fbhzIp1 && diffI == diffIp1;
			if (alleGleich) {
				int platzI   = rowI.get(SchweizerRanglisteSheet.PLATZ_SPALTE).getIntVal(0);
				int platzIp1 = rowIp1.get(SchweizerRanglisteSheet.PLATZ_SPALTE).getIntVal(0);
				assertThat(platzI)
						.as("Zeile %d und %d: Alle Kriterien gleich → PLATZ muss gleich sein", i + 1, i + 2)
						.isEqualTo(platzIp1);
			}
		}
	}

	@Test
	public void testOhneBuchholz_BhzSpaltenNull() throws GenerateException {
		testDaten.naechsteSpielrunde.getKonfigurationSheet()
				.setRankingModus(de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus.OHNE_BUCHHOLZ);
		testDaten.generate();

		RangeData data = ladeRanglisteDaten();

		assertThat(data)
				.as("BHZ_SPALTE muss 0 sein bei OHNE_BUCHHOLZ")
				.extracting(row -> row.get(SchweizerRanglisteSheet.BHZ_SPALTE).getIntVal(-1))
				.allSatisfy(bhz -> assertThat(bhz).isEqualTo(0));

		assertThat(data)
				.as("FBHZ_SPALTE muss 0 sein bei OHNE_BUCHHOLZ")
				.extracting(row -> row.get(SchweizerRanglisteSheet.FBHZ_SPALTE).getIntVal(-1))
				.allSatisfy(fbhz -> assertThat(fbhz).isEqualTo(0));

		assertThat(data)
				.as("Siege müssen befüllt sein")
				.extracting(row -> row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(-1))
				.allSatisfy(siege -> assertThat(siege).isBetween(0, ANZ_RUNDEN));
	}

	/**
	 * Prüft dass die Rangliste korrekt neu aufgebaut wird nachdem Ergebnisse direkt
	 * in der Spielrunde geändert wurden (dirty-Flag + manueller Rebuild).
	 * <p>
	 * Szenario:
	 * <ol>
	 *   <li>Vollständiges Turnier generieren (3 Runden + Rangliste)</li>
	 *   <li>Alle Ergebnis-Zellen der 1. Spielrunde auf 0 setzen</li>
	 *   <li>Rangliste neu aufbauen (simuliert Tab-Wechsel zur Rangliste)</li>
	 *   <li>Kein Team kann mehr als ANZ_RUNDEN-1 Siege haben (Runde 1 ergab 0:0)</li>
	 * </ol>
	 */
	@Test
	public void testLiveUpdate_NachErgebnisaenderung() throws GenerateException {
		testDaten.generate();

		// Siegesumme vor der Änderung – mindestens ein Team hat > 0 Siege
		int siegeSummeVorher = ladeRanglisteDaten().stream()
				.mapToInt(row -> row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(0))
				.sum();
		assertThat(siegeSummeVorher)
				.as("Vor Änderung muss Siegesumme > 0 sein")
				.isGreaterThan(0);

		// 1. Spielrunde: alle Ergebnis-Zellen auf 0 setzen
		String rundeSheetName = "1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN;
		XSpreadsheet runde1 = sheetHlp.findByName(rundeSheetName);
		assertThat(runde1).as("1. Spielrunde muss existieren").isNotNull();
		nulleErgebnisse(runde1);

		// Rangliste neu aufbauen (wird im echten Betrieb durch Tab-Wechsel ausgelöst)
		new SchweizerRanglisteSheet(wkingSpreadsheet).doRun();

		// Nach dem Nullen der 1. Runde darf kein Team mehr als ANZ_RUNDEN-1 Siege haben
		RangeData nachher = ladeRanglisteDaten();
		assertThat(nachher)
				.as("Nach Nullen der 1. Runde darf kein Team mehr als %d Siege haben", ANZ_RUNDEN - 1)
				.extracting(row -> row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(Integer.MAX_VALUE))
				.allSatisfy(siege -> assertThat(siege).isLessThanOrEqualTo(ANZ_RUNDEN - 1));

		// Gesamtsiegezahl muss kleiner geworden sein
		int siegeSummeNachher = nachher.stream()
				.mapToInt(row -> row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(0))
				.sum();
		assertThat(siegeSummeNachher)
				.as("Siegesumme muss nach Nullen der 1. Runde kleiner sein als vorher (%d)", siegeSummeVorher)
				.isLessThan(siegeSummeVorher);
	}

	/**
	 * Setzt alle Ergebnis-Zellen (ERG_TEAM_A und ERG_TEAM_B) in der gegebenen
	 * Spielrunde auf 0. Liest zuerst den Bereich, um die tatsächliche Zeilenzahl
	 * zu bestimmen.
	 */
	private void nulleErgebnisse(XSpreadsheet sheet) throws GenerateException {
		int startZeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
		RangePosition leseRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, startZeile,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, startZeile + ANZ_TEAMS - 1);

		RangeData data = RangeHelper
				.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), leseRange)
				.getDataFromRange();

		for (int i = 0; i < data.size(); i++) {
			RowData row = data.get(i);
			if (row.isEmpty()) break;
			// Prüfen ob Zeile eine echte Paarung enthält (TeamNr A > 0 oder Teamname gesetzt)
			int nrA = row.get(0).getIntVal(0);
			if (nrA <= 0 && (row.get(0).getStringVal() == null || row.get(0).getStringVal().isBlank())) break;

			int zeile = startZeile + i;
			sheetHlp.setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile)).setValue(0));
			sheetHlp.setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, zeile)).setValue(0));
		}
	}
}
