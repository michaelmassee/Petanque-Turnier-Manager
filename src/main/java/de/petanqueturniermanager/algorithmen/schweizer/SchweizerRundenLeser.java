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
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RundenWertungHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
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
	 * Siege, Punktedifferenz, Gegnerliste und Freilose. Als „nicht gewertet" markierte Runden
	 * ({@link RundenWertungHelper}) werden übersprungen. Freilose zählen für den Freilos-Buchholz nur in Runden,
	 * die bereits ein eingetragenes Ergebnis haben – eine ausgeloste, ungespielte Runde verändert BHZ/FBHZ nicht.
	 *
	 * @param rundenMetadatenSchluessel liefert für eine Rundennummer den Named-Range-Schlüssel
	 *                                  fürs Sheet-Lookup (Metadaten-first, überlebt Umbenennung)
	 * @param rundenBasisName           Basisname für den Legacy-Namens-Fallback ("N. " + Basisname)
	 * @param freispielPunktePlus       konfigurierte Punkte+ für ein Freilos
	 * @param freispielPunkteMinus      konfigurierte Punkte- für ein Freilos
	 * @return Teamergebnisse und Anzahl gewerteter Runden mit Ergebnissen
	 */
	public static SchweizerAuswertung leseErgebnisse(WorkingSpreadsheet workingSpreadsheet,
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
		var teamNrAufloeser = new SchweizerTeamNrAufloeser(meldeliste::erstelleTeamAnzeigeIndex,
				() -> MeldelisteUuidIndex.lese(workingSpreadsheet));
		var chronik = new HerunterlosChronik();
		Map<Integer, Integer> freiloseJeTeam = new HashMap<>();
		Map<Integer, List<Integer>> freilosRundenJeTeam = new HashMap<>();
		int anzGewerteteRunden = 0;
		for (int runde = 1; runde <= anzRunden; runde++) {
			SheetRunner.testDoCancelTask();
			String rundenSchluessel = rundenMetadatenSchluessel.apply(runde);
			XSpreadsheet rundeSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
					rundenSchluessel, runde + ". " + rundenBasisName);
			if (rundeSheet == null) {
				logger.debug("leseErgebnisse: Runde {} – Sheet nicht gefunden, übersprungen", runde);
				continue;
			}
			if (RundenWertungHelper.istNichtGewertet(xDoc, rundeSheet, rundenSchluessel)) {
				logger.debug("leseErgebnisse: Runde {} – nicht gewertet, übersprungen", runde);
				continue;
			}
			Map<Integer, Integer> siegeVorRunde = siegeJeTeam(statsMap);
			List<HerunterlosChronik.Begegnung> begegnungen = new ArrayList<>();
			List<Integer> freilosTeams = new ArrayList<>();
			boolean hatErgebnis = leseRundeEin(workingSpreadsheet, rundeSheet, aktiveMeldungen, statsMap, gegnerMap,
					begegnungen, freilosTeams, teamNrAufloeser, freispielPunktePlus, freispielPunkteMinus);
			chronik.vermerkeRunde(runde, begegnungen, freilosTeams, siegeVorRunde);
			int freilosRunde = runde;
			freilosTeams.forEach(teamNr -> freilosRundenJeTeam.computeIfAbsent(teamNr, k -> new ArrayList<>())
					.add(freilosRunde));
			if (hatErgebnis) {
				anzGewerteteRunden++;
				freilosTeams.forEach(teamNr -> freiloseJeTeam.merge(teamNr, 1, Integer::sum));
			}
		}

		List<SchweizerTeamErgebnis> ergebnisse = new ArrayList<>();
		for (Team team : aktiveMeldungen.teams()) {
			int[] stats = statsMap.getOrDefault(team.getNr(), new int[3]);
			List<Integer> gegnerNrn = gegnerMap.getOrDefault(team.getNr(), new ArrayList<>());
			int punkteDiff = stats[1] - stats[2];
			ergebnisse.add(new SchweizerTeamErgebnis(team.getNr(), stats[0], punkteDiff, stats[1], gegnerNrn,
					chronik.rundenFuer(team.getNr()), freiloseJeTeam.getOrDefault(team.getNr(), 0),
					freilosRundenJeTeam.getOrDefault(team.getNr(), List.of())));
		}
		return new SchweizerAuswertung(ergebnisse, anzGewerteteRunden);
	}

	private static Map<Integer, Integer> siegeJeTeam(Map<Integer, int[]> statsMap) {
		Map<Integer, Integer> siege = new HashMap<>();
		statsMap.forEach((teamNr, stats) -> siege.put(teamNr, stats[0]));
		return siege;
	}

	/**
	 * @return {@code true}, wenn die Runde mindestens ein eingetragenes Ergebnis hat
	 */
	private static boolean leseRundeEin(WorkingSpreadsheet workingSpreadsheet, XSpreadsheet rundeSheet,
			TeamMeldungen aktiveMeldungen, Map<Integer, int[]> statsMap, Map<Integer, List<Integer>> gegnerMap,
			List<HerunterlosChronik.Begegnung> begegnungen, List<Integer> freilosTeams,
			SchweizerTeamNrAufloeser teamNrAufloeser, int freispielPunktePlus,
			int freispielPunkteMinus) throws GenerateException {

		// Teamzellen und Ergebnisse (relativ 0..3)
		RangePosition readRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 999);
		RangeData rowsData = RangeHelper
				.from(rundeSheet, workingSpreadsheet.getWorkingSpreadsheetDocument(), readRange)
				.getDataFromRange();

		String[][] teamFormeln = new SheetHelper(workingSpreadsheet).getFormulaArrayFromRange(rundeSheet,
				RangePosition.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE, SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 999));

		boolean hatErgebnis = false;

		for (int i = 0; i < rowsData.size(); i++) {
			RowData row = rowsData.get(i);
			if (row.size() < 2) {
				break;
			}

			int nrA = teamNrAufloeser.teamA(row, teamFormeln[i][0]);
			if (nrA == SchweizerTeamNrAufloeser.KEIN_TEAM) {
				break; // Ende der Daten
			}
			Team teamA = aktiveMeldungen.getTeam(nrA);
			if (teamA == null) {
				continue; // Team inaktiv oder gelöscht
			}

			int nrB = teamNrAufloeser.teamB(row, teamFormeln[i][1]);
			if (nrB == SchweizerTeamNrAufloeser.KEIN_TEAM) {
				// Freilos für Team A – Sieg zählen und die konfigurierten Freispiel-Punkte
				// verbuchen (kein echter Gegner, daher kein Eintrag in gegnerMap für BHZ/FBHZ).
				int[] statsA = statsMap.computeIfAbsent(nrA, k -> new int[3]);
				freilosTeams.add(nrA); // Freilos = Runde ohne gleich starken Gegner, zählt als Herunterlosung
				statsA[0]++; // siege
				statsA[1] += freispielPunktePlus;
				statsA[2] += freispielPunkteMinus;
				continue;
			}
			Team teamB = aktiveMeldungen.getTeam(nrB);
			if (teamB == null) {
				continue;
			}
			begegnungen.add(new HerunterlosChronik.Begegnung(nrA, nrB));

			int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0;
			int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0;

			if (ergA > 0 || ergB > 0) {
				hatErgebnis = true;
				// Gegner erst bei tatsächlich eingetragenem Ergebnis für BHZ/FBHZ zählen –
				// eine bereits erzeugte, aber noch ungespielte Paarung darf die Buchholz-Werte
				// der Rangliste nicht beeinflussen.
				gegnerMap.computeIfAbsent(nrA, k -> new ArrayList<>()).add(nrB);
				gegnerMap.computeIfAbsent(nrB, k -> new ArrayList<>()).add(nrA);

				int[] statsA = statsMap.computeIfAbsent(nrA, k -> new int[3]);
				int[] statsB = statsMap.computeIfAbsent(nrB, k -> new int[3]);
				statsA[1] += ergA; // punkte+
				statsA[2] += ergB; // punkte-
				statsB[1] += ergB; // punkte+
				statsB[2] += ergA; // punkte-
				if (ergA > ergB) {
					statsA[0]++; // siege
				} else if (ergB > ergA) {
					statsB[0]++; // siege
				}
			}
		}
		return hatErgebnis;
	}
}
