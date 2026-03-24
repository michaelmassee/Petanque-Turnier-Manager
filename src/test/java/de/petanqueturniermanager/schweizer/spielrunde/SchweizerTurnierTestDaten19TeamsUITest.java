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
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

/**
 * UI-Test für das Schweizer Testturnier mit 19 Teams (ungerade Anzahl).
 * Besonderheiten:
 * <ul>
 *   <li>19 Teams → pro Runde 9 Paarungen + 1 Freilos</li>
 *   <li>Teamname-Anzeige in der Spielrunde (SpielplanTeamAnzeige.NAME)</li>
 *   <li>Zufällige Bahnvergabe (SpielrundeSpielbahn.R)</li>
 * </ul>
 */
public class SchweizerTurnierTestDaten19TeamsUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS    = 19;
	private static final int ANZ_RUNDEN   = 3;
	private static final int PAARUNGEN_PRO_RUNDE = ANZ_TEAMS / 2; // 9
	private static final int ZEILEN_PRO_RUNDE    = (ANZ_TEAMS + 1) / 2; // 10 (9 Paarungen + 1 Freilos)

	private SchweizerTurnierTestDaten testDaten;

	@BeforeEach
	public void setup() {
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, SpielplanTeamAnzeige.NAME);
	}

	@Test
	public void testVollstaendigesTurnierMit19TeamsWirdErstellt() throws GenerateException {
		testDaten.generate();

		// 1. Meldeliste-Sheet muss vorhanden sein
		XSpreadsheet meldeliste = sheetHlp.findByName("Meldeliste");
		assertThat(meldeliste).as("Meldeliste-Sheet").isNotNull();

		// 2. Spielrunden 1–3 prüfen
		for (int runde = 1; runde <= ANZ_RUNDEN; runde++) {
			pruefeSpielrundeSheet(runde);
		}

		// 3. Rangliste prüfen
		pruefeRanglisteSheet();
	}

	private void pruefeSpielrundeSheet(int runde) throws GenerateException {
		String sheetName = runde + ". " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN;
		XSpreadsheet spielrundeSheet = sheetHlp.findByName(sheetName);
		assertThat(spielrundeSheet).as(sheetName + " muss vorhanden sein").isNotNull();

		// Alle Zeilen lesen (Paarungen + Freilos)
		RangePosition paarungenRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + ZEILEN_PRO_RUNDE - 1);
		RangeData data = RangeHelper
				.from(spielrundeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), paarungenRange)
				.getDataFromRange();

		assertThat(data).as("Runde " + runde + ": " + ZEILEN_PRO_RUNDE + " Zeilen erwartet (inkl. Freilos)")
				.hasSize(ZEILEN_PRO_RUNDE);

		int freilosZaehler = 0;
		int paarungenMitErgebnisZaehler = 0;

		for (int i = 0; i < data.size(); i++) {
			var row = data.get(i);

			// Bahn-Spalte (Index 0 in diesem Range): bei SpielrundeSpielbahn.R muss eine Zahl stehen
			// (Ausnahme: Freilos-Zeile hat keine Bahn)
			String teamAName = row.size() > 1 ? row.get(1).getStringVal() : "";
			assertThat(teamAName).as("Runde %d, Zeile %d: Team A Name darf nicht leer sein", runde, i + 1)
					.isNotEmpty();

			String teamBName = row.size() > 2 ? row.get(2).getStringVal() : "";

			if (teamBName == null || teamBName.isBlank()) {
				// Freilos-Zeile
				freilosZaehler++;
			} else {
				// Normale Paarung: Ergebnis muss eingetragen sein
				int ergA = row.size() > 3 ? row.get(3).getIntVal(0) : 0;
				int ergB = row.size() > 4 ? row.get(4).getIntVal(0) : 0;
				assertThat(ergA + ergB)
						.as("Runde %d, Zeile %d: Ergebnis muss eingetragen sein", runde, i + 1)
						.isGreaterThan(0);
				// Eines der Ergebnisse muss 13 sein (Petanque-Endstand)
				assertThat(Math.max(ergA, ergB))
						.as("Runde %d, Zeile %d: Sieger muss 13 Punkte haben", runde, i + 1)
						.isEqualTo(13);
				paarungenMitErgebnisZaehler++;
			}
		}

		assertThat(freilosZaehler).as("Runde " + runde + ": genau 1 Freilos erwartet").isEqualTo(1);
		assertThat(paarungenMitErgebnisZaehler)
				.as("Runde " + runde + ": " + PAARUNGEN_PRO_RUNDE + " Paarungen mit Ergebnis erwartet")
				.isEqualTo(PAARUNGEN_PRO_RUNDE);
	}

	private void pruefeRanglisteSheet() throws GenerateException {
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		assertThat(rangliste).as("Rangliste-Sheet").isNotNull();

		RangePosition ranglisteRange = RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);
		RangeData ranglisteData = RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
				.getDataFromRange();

		assertThat(ranglisteData).as("Rangliste muss " + ANZ_TEAMS + " Einträge haben").hasSize(ANZ_TEAMS);

		// Platzierungen: beginnen bei 1, monoton nicht-fallend
		var plaetze = ranglisteData.stream()
				.map(row -> row.get(SchweizerRanglisteSheet.PLATZ_SPALTE).getIntVal(0))
				.toList();
		assertThat(plaetze.get(0)).as("Erster Platz muss 1 sein").isEqualTo(1);
		assertThat(plaetze.get(plaetze.size() - 1))
				.as("Letzter Platz muss <= " + ANZ_TEAMS + " sein")
				.isLessThanOrEqualTo(ANZ_TEAMS);
		for (int i = 0; i < plaetze.size() - 1; i++) {
			assertThat(plaetze.get(i))
					.as("Platz[%d]=%d muss <= Platz[%d]=%d sein", i, plaetze.get(i), i + 1, plaetze.get(i + 1))
					.isLessThanOrEqualTo(plaetze.get(i + 1));
		}

		// Siege: 0 bis ANZ_RUNDEN (mit Freilos: max = ANZ_RUNDEN)
		assertThat(ranglisteData)
				.as("Siege müssen zwischen 0 und " + ANZ_RUNDEN + " liegen")
				.extracting(row -> row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(-1))
				.allSatisfy(siege -> assertThat(siege).isBetween(0, ANZ_RUNDEN));
	}
}
