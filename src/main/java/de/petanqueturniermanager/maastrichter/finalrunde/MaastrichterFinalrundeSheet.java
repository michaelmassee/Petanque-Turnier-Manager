/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.finalrunde;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.GruppenAufteilungRechner;
import de.petanqueturniermanager.algorithmen.SchweizerSystem;
import de.petanqueturniermanager.algorithmen.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt die Finalrundenblätter (A-Finale, B-Finale, C-Finale, D-Finale) für das
 * Maastrichter Turniersystem.
 * <p>
 * Ablauf:
 * <ol>
 *   <li>Alle "N. Vorrunde"-Blätter lesen → Siege/Punkte pro Team berechnen</li>
 *   <li>Teams nach Siegen gruppieren: A = max. Siege, B = max-1, C = max-2, D = Rest</li>
 *   <li>Innerhalb jeder Gruppe nach Schweizer Kriterien sortieren (für Setzliste)</li>
 *   <li>Pro nicht-leerer Gruppe mit ≥2 Teams: KO-Bracket-Blatt erstellen</li>
 * </ol>
 */
public class MaastrichterFinalrundeSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(MaastrichterFinalrundeSheet.class);

	public MaastrichterFinalrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Finalrunde");
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		// Kein eigenes Sheet – delegiert an KoTurnierbaumSheet-Sheets
		return null;
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return null;
	}

	@Override
	protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
		return new MaastrichterKonfigurationSheet(getWorkingSpreadsheet());
	}

	@Override
	public void doRun() throws GenerateException {
		processBoxinfo("Erstelle Maastrichter Finalrunden...");

		SchweizerMeldeListeSheetUpdate meldeliste = new SchweizerMeldeListeSheetUpdate(getWorkingSpreadsheet());
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen == null || aktiveMeldungen.size() < 2) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption("Maastrichter Finalrunde")
					.message("Mindestens 2 aktive Teams erforderlich.")
					.show();
			return;
		}

		MaastrichterKonfigurationSheet konfigSheet = getKonfigurationSheet();
		int anzVorrunden = konfigSheet.getAnzVorrunden();
		SchweizerRankingModus modus = konfigSheet.getRankingModus();

		// Alle Vorrunden-Ergebnisse einlesen
		List<SchweizerTeamErgebnis> ergebnisse = leseVorrundenErgebnisse(
				aktiveMeldungen, anzVorrunden, meldeliste);

		// Nach Schweizer Kriterien sortieren (für korrekte Setzliste pro Gruppe)
		List<SchweizerTeamErgebnis> sortiert = new SchweizerSystem()
				.sortiereNachAuswertungskriterien(ergebnisse, modus);

		if (sortiert.isEmpty()) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
					.caption("Maastrichter Finalrunde")
					.message("Keine Ergebnisse gefunden. Bitte zuerst alle Vorrunden eintragen.")
					.show();
			return;
		}

		// Gruppen nach Rang-Aufteilung (identisch zum KO-System)
		int gruppenGroesse = konfigSheet.getGruppenGroesse();
		int minRestGroesse = konfigSheet.getMinRestGroesse();
		List<Integer> gruppenGroessen = GruppenAufteilungRechner.berechne(
				sortiert.size(), gruppenGroesse, minRestGroesse);
		int anzGruppen = gruppenGroessen.size();

		// Alte Finale-Blätter löschen
		alleFinaleSheetNamenLoeschen();

		// KO-Bracket für jede Gruppe erstellen
		KoTurnierbaumSheet koSheet = new KoTurnierbaumSheet(getWorkingSpreadsheet());
		int startIndex = 0;
		short sheetPos = DefaultSheetPos.MAASTRICHTER_FINALE;

		for (int g = 0; g < anzGruppen; g++) {
			SheetRunner.testDoCancelTask();
			int groesse = gruppenGroessen.get(g);
			String sheetName = (char) ('A' + g) + "-Finale";
			processBoxinfo("Erstelle " + sheetName + " (" + groesse + " Teams)...");
			List<SchweizerTeamErgebnis> gruppeErg = sortiert.subList(startIndex, startIndex + groesse);
			startIndex += groesse;
			TeamMeldungen gruppeTeams = erstelleGruppeTeams(gruppeErg, aktiveMeldungen);
			if (gruppeTeams.size() >= 2) {
				koSheet.erstelleGruppeBracket(gruppeTeams, sheetName, sheetPos, konfigSheet);
				sheetPos++;
			}
		}
	}

	/**
	 * Liest alle Vorrunden-Blätter und berechnet Siege, Punkte und Gegnerlisten pro Team.
	 */
	private List<SchweizerTeamErgebnis> leseVorrundenErgebnisse(
			TeamMeldungen aktiveMeldungen, int anzVorrunden,
			SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {

		Map<Integer, int[]> statsMap = new HashMap<>(); // teamNr → [0]=siege, [1]=punkte+, [2]=punkte-
		Map<Integer, List<Integer>> gegnerMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new int[3]);
			gegnerMap.put(team.getNr(), new ArrayList<>());
		}

		for (int runde = 1; runde <= anzVorrunden; runde++) {
			SheetRunner.testDoCancelTask();
			String rundeSheetName = runde + ". " + MaastrichterSpielrundeSheetNaechste.SHEET_BASIS_NAME;
			XSpreadsheet rundeSheet = getSheetHelper().findByName(rundeSheetName);
			if (rundeSheet == null) {
				logger.warn("Vorrunden-Sheet '{}' nicht gefunden, übersprungen.", rundeSheetName);
				continue;
			}
			leseRundeEin(rundeSheet, aktiveMeldungen, statsMap, gegnerMap, meldeliste);
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

	/**
	 * Liest eine einzelne Spielrunde ein und aktualisiert Statistiken.
	 */
	private void leseRundeEin(XSpreadsheet rundeSheet, TeamMeldungen aktiveMeldungen,
			Map<Integer, int[]> statsMap, Map<Integer, List<Integer>> gegnerMap,
			SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {

		RangePosition readRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 999);

		RangeData rowsData = RangeHelper
				.from(rundeSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
				.getDataFromRange();

		for (RowData row : rowsData) {
			if (row.size() < 2) break;

			int nrA = resolveTeamNr(row.get(0), meldeliste);
			if (nrA <= 0) break;
			Team teamA = aktiveMeldungen.getTeam(nrA);
			if (teamA == null) continue;

			int nrB = resolveTeamNr(row.get(1), meldeliste);
			if (nrB <= 0) {
				// Freilos für Team A
				statsMap.computeIfAbsent(nrA, k -> new int[3])[0]++;
				continue;
			}
			Team teamB = aktiveMeldungen.getTeam(nrB);
			if (teamB == null) continue;

			gegnerMap.computeIfAbsent(nrA, k -> new ArrayList<>()).add(nrB);
			gegnerMap.computeIfAbsent(nrB, k -> new ArrayList<>()).add(nrA);

			int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0;
			int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0;

			if (ergA > 0 || ergB > 0) {
				statsMap.computeIfAbsent(nrA, k -> new int[3])[1] += ergA;
				statsMap.computeIfAbsent(nrA, k -> new int[3])[2] += ergB;
				statsMap.computeIfAbsent(nrB, k -> new int[3])[1] += ergB;
				statsMap.computeIfAbsent(nrB, k -> new int[3])[2] += ergA;
				if (ergA > ergB) {
					statsMap.computeIfAbsent(nrA, k -> new int[3])[0]++;
				} else if (ergB > ergA) {
					statsMap.computeIfAbsent(nrB, k -> new int[3])[0]++;
				}
			}
		}
	}

	private int resolveTeamNr(CellData cell, SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {
		int nr = cell.getIntVal(0);
		if (nr > 0) return nr;
		String name = cell.getStringVal();
		if (name != null && !name.isEmpty()) {
			return meldeliste.getTeamNrByTeamname(name);
		}
		return 0;
	}

	/**
	 * Erstellt ein {@link TeamMeldungen}-Objekt aus den sortierten Ergebnissen einer Gruppe.
	 * Die Reihenfolge der Teams entspricht der Rangliste (Platz 1 zuerst) und dient
	 * als Setzliste für den KO-Bracket.
	 */
	private TeamMeldungen erstelleGruppeTeams(List<SchweizerTeamErgebnis> gruppe,
			TeamMeldungen aktiveMeldungen) {
		TeamMeldungen gruppeTeams = new TeamMeldungen();
		for (SchweizerTeamErgebnis erg : gruppe) {
			Team team = aktiveMeldungen.getTeam(erg.teamNr());
			if (team != null) {
				gruppeTeams.addTeamWennNichtVorhanden(team);
			}
		}
		return gruppeTeams;
	}

	/**
	 * Löscht alle vorhandenen Finale-Sheets (Namen nach Muster "[A-Z]-Finale").
	 */
	private void alleFinaleSheetNamenLoeschen() throws GenerateException {
		for (String name : getSheetHelper().getSheets().getElementNames()) {
			if (name.matches("[A-Z]-Finale")) {
				getSheetHelper().removeSheet(name);
			}
		}
	}

}
