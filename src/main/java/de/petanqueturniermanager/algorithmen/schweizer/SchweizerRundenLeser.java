/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Liest die Spielrunden-Sheets eines Schweizer-basierten Turniersystems (Schweizer System,
 * Maastrichter-Vorrunden, ...) ein und berechnet je aktivem Team Siege, Punktedifferenz und
 * die Gegnerliste (für BHZ/FBHZ).
 * <p>
 * Gemeinsam genutzt von {@link de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet}
 * (schreibt die angezeigte Rangliste) und
 * {@link de.petanqueturniermanager.maastrichter.finalrunde.MaastrichterFinalrundeSheet}
 * (KO-Cutoff/Gruppeneinteilung) – beide müssen exakt dieselbe Auswertung verwenden,
 * insbesondere die Freilos-Punkte-Verbuchung, da sonst die KO-Gruppen von einer
 * abweichend berechneten Rangliste abweichen.
 */
public final class SchweizerRundenLeser {

	private static final Logger logger = LogManager.getLogger(SchweizerRundenLeser.class);

	private SchweizerRundenLeser() {
		// Utility-Klasse – keine Instanzen
	}

	/**
	 * Liest die Runden 1..anzRunden ein und berechnet je Team in {@code aktiveMeldungen}
	 * Siege, Punktedifferenz und Gegnerliste.
	 *
	 * @param rundenMetadatenSchluessel liefert für eine Rundennummer den Named-Range-Schlüssel
	 *                                  fürs Sheet-Lookup (Metadaten-first, überlebt Umbenennung)
	 * @param rundenBasisName           Basisname für den Legacy-Namens-Fallback ("N. " + Basisname)
	 * @param freispielPunktePlus       konfigurierte Punkte+ für ein Freilos
	 * @param freispielPunkteMinus      konfigurierte Punkte- für ein Freilos
	 */
	public static List<SchweizerTeamErgebnis> leseErgebnisse(WorkingSpreadsheet workingSpreadsheet,
			TeamMeldungen aktiveMeldungen, int anzRunden, IntFunction<String> rundenMetadatenSchluessel,
			String rundenBasisName, SchweizerMeldeListeSheetUpdate meldeliste, int freispielPunktePlus,
			int freispielPunkteMinus) throws GenerateException {

		Map<Integer, int[]> statsMap = new HashMap<>(); // teamNr → [0]=siege, [1]=punkte+, [2]=punkte-
		Map<Integer, List<Integer>> gegnerMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new int[3]);
			gegnerMap.put(team.getNr(), new ArrayList<>());
		}

		var xDoc = workingSpreadsheet.getWorkingSpreadsheetDocument();
		for (int runde = 1; runde <= anzRunden; runde++) {
			SheetRunner.testDoCancelTask();
			XSpreadsheet rundeSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
					rundenMetadatenSchluessel.apply(runde), runde + ". " + rundenBasisName);
			if (rundeSheet == null) {
				logger.debug("leseErgebnisse: Runde {} – Sheet nicht gefunden, übersprungen", runde);
				continue;
			}
			leseRundeEin(workingSpreadsheet, rundeSheet, aktiveMeldungen, statsMap, gegnerMap, meldeliste,
					freispielPunktePlus, freispielPunkteMinus);
		}

		List<SchweizerTeamErgebnis> ergebnisse = new ArrayList<>();
		for (Team team : aktiveMeldungen.teams()) {
			int[] stats = statsMap.getOrDefault(team.getNr(), new int[3]);
			List<Integer> gegnerNrn = gegnerMap.getOrDefault(team.getNr(), new ArrayList<>());
			int punkteDiff = stats[1] - stats[2];
			ergebnisse.add(new SchweizerTeamErgebnis(team.getNr(), stats[0], punkteDiff, stats[1], gegnerNrn));
		}
		return ergebnisse;
	}

	private static void leseRundeEin(WorkingSpreadsheet workingSpreadsheet, XSpreadsheet rundeSheet,
			TeamMeldungen aktiveMeldungen, Map<Integer, int[]> statsMap, Map<Integer, List<Integer>> gegnerMap,
			SchweizerMeldeListeSheetUpdate meldeliste, int freispielPunktePlus,
			int freispielPunkteMinus) throws GenerateException {

		RangePosition readRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 999);
		RangeData rowsData = RangeHelper
				.from(rundeSheet, workingSpreadsheet.getWorkingSpreadsheetDocument(), readRange)
				.getDataFromRange();

		for (RowData row : rowsData) {
			if (row.size() < 2) break;

			int nrA = resolveTeamNr(row.get(0), meldeliste);
			if (nrA <= 0) break; // Ende der Daten
			Team teamA = aktiveMeldungen.getTeam(nrA);
			if (teamA == null) continue;

			int nrB = resolveTeamNr(row.get(1), meldeliste);
			if (nrB <= 0) {
				// Freilos für Team A – Sieg zählen und die konfigurierten Freispiel-Punkte
				// verbuchen (kein echter Gegner, daher kein Eintrag in gegnerMap für BHZ/FBHZ).
				int[] statsA = statsMap.computeIfAbsent(nrA, k -> new int[3]);
				statsA[0]++; // siege
				statsA[1] += freispielPunktePlus;
				statsA[2] += freispielPunkteMinus;
				continue;
			}
			Team teamB = aktiveMeldungen.getTeam(nrB);
			if (teamB == null) continue;

			int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0;
			int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0;

			if (ergA > 0 || ergB > 0) {
				// Gegner erst bei tatsächlich eingetragenem Ergebnis für BHZ/FBHZ zählen –
				// eine bereits erzeugte, aber noch ungespielte Paarung darf die Buchholz-Werte
				// der Rangliste nicht beeinflussen.
				gegnerMap.computeIfAbsent(nrA, k -> new ArrayList<>()).add(nrB);
				gegnerMap.computeIfAbsent(nrB, k -> new ArrayList<>()).add(nrA);

				statsMap.computeIfAbsent(nrA, k -> new int[3])[1] += ergA; // punkte+
				statsMap.computeIfAbsent(nrA, k -> new int[3])[2] += ergB; // punkte-
				statsMap.computeIfAbsent(nrB, k -> new int[3])[1] += ergB; // punkte+
				statsMap.computeIfAbsent(nrB, k -> new int[3])[2] += ergA; // punkte-
				if (ergA > ergB) {
					statsMap.computeIfAbsent(nrA, k -> new int[3])[0]++; // siege für A
				} else if (ergB > ergA) {
					statsMap.computeIfAbsent(nrB, k -> new int[3])[0]++; // siege für B
				}
			}
		}
	}

	private static int resolveTeamNr(CellData cell, SchweizerMeldeListeSheetUpdate meldeliste)
			throws GenerateException {
		int nr = cell.getIntVal(0);
		if (nr > 0) {
			return nr;
		}
		String name = cell.getStringVal();
		if (name != null && !name.isEmpty()) {
			return meldeliste.getTeamNrByTeamname(name);
		}
		return 0;
	}
}
