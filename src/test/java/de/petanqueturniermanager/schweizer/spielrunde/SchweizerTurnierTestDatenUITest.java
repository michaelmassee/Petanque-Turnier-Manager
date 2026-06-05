package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

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
		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		assertThat(rangliste).as("Rangliste-Sheet").isNotNull();

		// Rangliste hat genau ANZ_TEAMS Zeilen (Range ab TEAM_NR_SPALTE=0, damit Konstanten als Index stimmen)
		RangePosition ranglisteRange = RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);
		RangeData ranglisteData = RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
				.getDataFromRange();
		assertThat(ranglisteData).as("Rangliste muss " + ANZ_TEAMS + " Einträge haben").hasSize(ANZ_TEAMS);

		// Platzierungen: beginnen bei 1, sind monoton nicht-fallend, max = ANZ_TEAMS
		// (Bei Gleichstand teilen sich Teams einen Platz → keine lückenlose Folge garantiert)
		var plaetze = ranglisteData.stream()
				.map(row -> row.get(SchweizerRanglisteSheet.PLATZ_SPALTE).getIntVal(0))
				.toList();
		assertThat(plaetze.get(0)).as("Erster Platz muss 1 sein").isEqualTo(1);
		assertThat(plaetze.get(plaetze.size() - 1)).as("Letzter Platz muss <= " + ANZ_TEAMS + " sein")
				.isLessThanOrEqualTo(ANZ_TEAMS);
		for (int i = 0; i < plaetze.size() - 1; i++) {
			assertThat(plaetze.get(i)).as("Platz[%d]=%d muss <= Platz[%d]=%d sein", i, plaetze.get(i), i + 1,
					plaetze.get(i + 1)).isLessThanOrEqualTo(plaetze.get(i + 1));
		}

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

	/**
	 * Korrektheit der PTM-Metadaten: nach voller Generierung (Meldeliste, 3 Spielrunden,
	 * Rangliste) muss jedes Blatt exakt seinen erwarteten Identitäts-Schlüssel tragen –
	 * und kein weiteres Blatt einen unerwarteten.
	 */
	@Test
	public void jedesBlattTraegtKorrektenSchluessel() throws GenerateException {
		testDaten.generate();

		Map<String, String> erwartung = new LinkedHashMap<>();
		erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_MELDELISTE);
		for (int runde = 1; runde <= ANZ_RUNDEN; runde++) {
			erwartung.put(SheetNamen.spielrunde(runde), SheetMetadataHelper.schluesselSchweizerSpielrunde(runde));
		}
		erwartung.put(SheetNamen.rangliste(), SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE);

		pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
	}

	/**
	 * Regression im Kiosk-Modus: nach Generierung des vollständigen Turniers (Meldeliste +
	 * 3 Spielrunden + Rangliste) muss das anschließende {@link SchweizerRanglisteSheetUpdate}
	 * unter aktivem TurnierModus durchlaufen, die Rangliste weiterhin {@link #ANZ_TEAMS}
	 * Zeilen enthalten und der Blattschutz danach intakt sein (von {@code mitKioskModus} geprüft).
	 */
	@Test
	public void kioskModus_ranglisteUpdateNachGenerierung() throws GenerateException {
		testDaten.generate();
		mitKioskModus(TurnierSystem.SCHWEIZER, () ->
				new SchweizerRanglisteSheetUpdate(wkingSpreadsheet).doRun());

		XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
		RangePosition ranglisteRange = RangePosition.from(
				SchweizerRanglisteSheet.TEAM_NR_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
				SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
				SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);
		RangeData ranglisteData = RangeHelper
				.from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
				.getDataFromRange();
		assertThat(ranglisteData)
				.as("Rangliste muss nach Kiosk-Update " + ANZ_TEAMS + " Einträge haben")
				.hasSize(ANZ_TEAMS);
	}

	/**
	 * Regression (In-Place-Heilung): Nicht-editierbare Spalten (Bahn-Nr, Teamnamen) einer
	 * Spielrunde müssen im Kiosk-Modus gesperrt sein – auch wenn sie wie in einem
	 * Bestandsdokument zuvor fälschlich entsperrt waren. Die editierbaren Ergebnis-Spalten
	 * bleiben entsperrt.
	 */
	@Test
	public void kioskModus_nichtEditierbareSpaltenWerdenGesperrt() throws Exception {
		testDaten.generate();
		XSpreadsheet spielrunde = sheetHlp.findByName(SheetNamen.spielrunde(1));
		int ersteZeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
		int letzteZeile = ersteZeile + SPIELE_PRO_RUNDE - 1;

		// Bestandsdokument simulieren: Bahn-Nr-Spalte vorab fälschlich entsperren
		setzeIsLocked(spielrunde, SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, ersteZeile,
				SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, letzteZeile, false);

		mitKioskModus(TurnierSystem.SCHWEIZER, () -> { /* nur Schutz anwenden, keine Aktion */ });

		assertThat(istGesperrt(spielrunde, SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, ersteZeile))
				.as("Bahn-Nr (nicht editierbar) muss im Kiosk-Modus gesperrt sein").isTrue();
		assertThat(istGesperrt(spielrunde, SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, ersteZeile))
				.as("Ergebnis A (editierbar) darf im Kiosk-Modus nicht gesperrt sein").isFalse();
	}

	private boolean istGesperrt(XSpreadsheet sheet, int spalte, int zeile) throws Exception {
		var cell = sheet.getCellByPosition(spalte, zeile);
		XPropertySet props = Lo.qi(XPropertySet.class, cell);
		return ((CellProtection) props.getPropertyValue("CellProtection")).IsLocked;
	}

	private void setzeIsLocked(XSpreadsheet sheet, int startSpalte, int startZeile,
			int endeSpalte, int endeZeile, boolean locked) throws Exception {
		var range = sheet.getCellRangeByPosition(startSpalte, startZeile, endeSpalte, endeZeile);
		XPropertySet props = Lo.qi(XPropertySet.class, range);
		var cp = new CellProtection();
		cp.IsLocked = locked;
		props.setPropertyValue("CellProtection", cp);
	}
}
