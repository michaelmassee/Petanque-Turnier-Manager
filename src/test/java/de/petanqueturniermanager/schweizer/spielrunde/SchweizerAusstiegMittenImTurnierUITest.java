package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

/**
 * Regression: ein Team steigt NACH bereits gespielter Runde 1 aus (Aktiv-Spalte = 2).
 * Erwartet (aktuelles, konsistentes Verhalten): Runde 2 paart das Team nicht mehr, Runde-1-Ergebnisse
 * bleiben unverändert stehen, und die Rangliste führt (wie Paarungen) nur noch die verbleibenden
 * aktiven Teams – das ausgestiegene Team fällt komplett aus der Rangliste.
 */
public class SchweizerAusstiegMittenImTurnierUITest extends BaseCalcUITest {

	private static final int ANZ_TEAMS = 16;
	private static final int AUSGESTIEGENES_TEAM_NR = 1;

	private SchweizerTurnierTestDaten testDaten;

	@BeforeEach
	public void setup() {
		testDaten = new SchweizerTurnierTestDaten(wkingSpreadsheet);
	}

	@Test
	public void ausstiegNachRunde1_wirdInRunde2NichtMehrGepaart() throws GenerateException {
		// Runde 1 spielen, noch keine Rangliste
		testDaten.generate(1, false);

		String runde1SheetName = SheetNamen.spielrunde(1);
		List<List<Integer>> runde1VorAusstieg = leseSpielpaarungWerte(runde1SheetName);

		// Team als "ausgestiegen" markieren
		SchweizerMeldeListeSheetUpdate meldeliste = new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet);
		int zeile = meldeliste.getSpielerZeileNr(AUSGESTIEGENES_TEAM_NR);
		assertThat(zeile).as("Team %d muss in der Meldeliste gefunden werden", AUSGESTIEGENES_TEAM_NR)
				.isGreaterThan(0);
		sheetHlp.setNumberValueInCell(NumberCellValue
				.from(meldeliste.getXSpreadSheet(), Position.from(meldeliste.getAktivSpalte(), zeile))
				.setValue(SchweizerMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN));

		// Runde 2 erzeugen und mit Ergebnissen füllen
		testDaten.naechsteSpielrunde.doRun();
		String runde2SheetName = SheetNamen.spielrunde(2);
		XSpreadsheet runde2Sheet = sheetHlp.findByName(runde2SheetName);
		assertThat(runde2Sheet).as(runde2SheetName + " muss vorhanden sein").isNotNull();
		testDaten.ergebnisseEinfuegen(runde2Sheet);

		// Runde 1: Ergebnisse unverändert (Ausstieg wirkt nicht rückwirkend)
		List<List<Integer>> runde1NachAusstieg = leseSpielpaarungWerte(runde1SheetName);
		assertThat(runde1NachAusstieg).as("Runde 1 darf durch den Ausstieg nicht verändert werden")
				.isEqualTo(runde1VorAusstieg);

		// Runde 2: das ausgestiegene Team taucht in keiner Paarung mehr auf
		List<List<Integer>> runde2Paarungen = leseSpielpaarungWerte(runde2SheetName);
		for (var row : runde2Paarungen) {
			int nrA = row.get(0);
			int nrB = row.get(1);
			assertThat(nrA).as("Ausgestiegenes Team darf nicht mehr als Team A gepaart werden")
					.isNotEqualTo(AUSGESTIEGENES_TEAM_NR);
			assertThat(nrB).as("Ausgestiegenes Team darf nicht mehr als Team B gepaart werden")
					.isNotEqualTo(AUSGESTIEGENES_TEAM_NR);
		}

		// Rangliste erzeugen: darf nicht crashen. SchweizerRanglisteSheet baut ausschließlich aus
		// getAktiveMeldungen() auf (Aktiv==1) – ein ausgestiegenes Team fällt damit konsequent auch
		// aus der Rangliste raus (Rangliste = Ranking der aktuell aktiven Teams), analog zu Paarungen.
		SchweizerRanglisteSheet ranglisteSheet = new SchweizerRanglisteSheet(wkingSpreadsheet);
		ranglisteSheet.doRun();

		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		assertThat(rangliste).as("Rangliste-Sheet").isNotNull();

		RangePosition ranglisteRange = RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.SIEGE_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 2);
		RangeData ranglisteData = RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
				.getDataFromRange();
		assertThat(ranglisteData)
				.as("Rangliste muss die " + (ANZ_TEAMS - 1) + " weiterhin aktiven Teams listen")
				.hasSize(ANZ_TEAMS - 1);

		boolean ausgestiegenesTeamInRangliste = ranglisteData.stream()
				.anyMatch(row -> row.get(SchweizerRanglisteSheet.TEAM_NR_SPALTE).getIntVal(-1)
						== AUSGESTIEGENES_TEAM_NR);
		assertThat(ausgestiegenesTeamInRangliste)
				.as("Ausgestiegenes Team wird konsequent nicht mehr in der Rangliste geführt")
				.isFalse();

		// Alle übrigen 15 aktiven Teams müssen in Runde 2 gepaart worden sein (ggf. inkl. 1 Freilos)
		var gepaarteNrn = new java.util.HashSet<Integer>();
		for (var row : runde2Paarungen) {
			int nrA = row.get(0);
			if (nrA > 0) {
				gepaarteNrn.add(nrA);
			}
			int nrB = row.get(1);
			if (nrB > 0) {
				gepaarteNrn.add(nrB);
			}
		}
		assertThat(gepaarteNrn).as("Alle 15 weiterhin aktiven Teams müssen in Runde 2 gepaart sein")
				.hasSize(ANZ_TEAMS - 1)
				.doesNotContain(AUSGESTIEGENES_TEAM_NR);
	}

	/**
	 * Liest Team-A-/Team-B-/Ergebnis-Werte als {@code List<List<Integer>>} statt {@code RangeData} –
	 * {@link de.petanqueturniermanager.helper.sheet.rangedata.CellData} hat kein {@code equals()},
	 * ein direkter {@code RangeData}-Vergleich wäre daher immer Objekt-Identität statt Werte-Vergleich.
	 */
	private List<List<Integer>> leseSpielpaarungWerte(String sheetName) throws GenerateException {
		XSpreadsheet sheet = sheetHlp.findByName(sheetName);
		assertThat(sheet).as(sheetName + " muss vorhanden sein").isNotNull();
		RangePosition paarungenRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 100);
		RangeData data = RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), paarungenRange)
				.getDataFromRange();

		List<List<Integer>> werte = new ArrayList<>();
		for (var row : data) {
			List<Integer> zeile = new ArrayList<>();
			for (var cell : row) {
				zeile.add(cell.getIntVal(0));
			}
			werte.add(zeile);
		}
		return werte;
	}
}
