package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

public class SchweizerTurnierTestDatenUITest extends BaseCalcUITest {

	private static final int ANZ_RUNDEN = 3;
	private static final int ANZ_TEAMS  = 16; // Default in SchweizerMeldeListeSheetTestDaten
	private static final int SPIELE_PRO_RUNDE = ANZ_TEAMS / 2; // Triplette: 8 Spiele

	private SchweizerTurnierTestDaten testDaten;

	@BeforeEach
	public void setup() {
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet);
	}

	@Test
	public void testVollstaendigesTurnierWirdErstellt() throws GenerateException {
		testDaten.generate();

		// 1. Meldeliste-Sheet muss vorhanden sein
		XSpreadsheet meldeliste = sheetHlp.findByName("Meldeliste");
		assertThat(meldeliste).as("Meldeliste-Sheet").isNotNull();

		// 2. Spielrunden 1–3 müssen vorhanden sein und ausgefüllte Ergebnisse haben
		for (int runde = 1; runde <= ANZ_RUNDEN; runde++) {
			String sheetName = runde + ". " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN;
			XSpreadsheet spielrundeSheet = sheetHlp.findByName(sheetName);
			assertThat(spielrundeSheet).as(sheetName + " muss vorhanden sein").isNotNull();

			RangePosition paarungenRange = RangePosition.from(
					SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
					SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
					SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
					SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + SPIELE_PRO_RUNDE - 1);
			RangeData data = RangeHelper
					.from(spielrundeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), paarungenRange)
					.getDataFromRange();

			assertThat(data).as("Runde " + runde + ": " + SPIELE_PRO_RUNDE + " Paarungen erwartet")
					.hasSize(SPIELE_PRO_RUNDE);

			// Jede Paarung hat ein eingetragenes Ergebnis (ergA + ergB > 0)
			for (int i = 0; i < data.size(); i++) {
				var row = data.get(i);
				int ergA = row.size() > 2 ? row.get(2).getIntVal(0) : 0;
				int ergB = row.size() > 3 ? row.get(3).getIntVal(0) : 0;
				assertThat(ergA + ergB).as("Runde %d, Zeile %d: Ergebnis muss eingetragen sein", runde, i + 1)
						.isGreaterThan(0);
			}
		}

		// 3. Rangliste-Sheet muss vorhanden sein
		XSpreadsheet rangliste = sheetHlp.findByName(SchweizerRanglisteSheet.SHEETNAME);
		assertThat(rangliste).as("Rangliste-Sheet").isNotNull();

		// Rangliste hat genau ANZ_TEAMS Zeilen
		RangePosition ranglisteRange = RangePosition.from(
				SchweizerRanglisteSheet.PLATZ_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);
		RangeData ranglisteData = RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
				.getDataFromRange();
		assertThat(ranglisteData).as("Rangliste muss " + ANZ_TEAMS + " Einträge haben").hasSize(ANZ_TEAMS);

		// Platzierungen 1–16 lückenlos
		assertThat(ranglisteData)
				.as("Platzierungen 1–" + ANZ_TEAMS + " lückenlos")
				.extracting(row -> row.get(SchweizerRanglisteSheet.PLATZ_SPALTE).getIntVal(0))
				.containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);

		// Siege: jedes Team hat 0–ANZ_RUNDEN Siege
		assertThat(ranglisteData)
				.as("Siege müssen zwischen 0 und " + ANZ_RUNDEN + " liegen")
				.extracting(row -> row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(-1))
				.allSatisfy(siege -> assertThat(siege).isBetween(0, ANZ_RUNDEN));

		// BHZ und FBHZ: nicht negativ
		assertThat(ranglisteData)
				.as("BHZ muss >= 0 sein")
				.extracting(row -> row.get(SchweizerRanglisteSheet.BHZ_SPALTE).getIntVal(-1))
				.allSatisfy(bhz -> assertThat(bhz).isGreaterThanOrEqualTo(0));

		assertThat(ranglisteData)
				.as("FBHZ muss >= 0 sein")
				.extracting(row -> row.get(SchweizerRanglisteSheet.FBHZ_SPALTE).getIntVal(-1))
				.allSatisfy(fbhz -> assertThat(fbhz).isGreaterThanOrEqualTo(0));
	}
}
