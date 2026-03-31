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
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterGruppenModus;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt die Finalrundenblätter (A-Finale, B-Finale, C-Finale, D-Finale) für das
 * Maastrichter Turniersystem.
 * <p>
 * Ablauf:
 * <ol>
 *   <li>Alle "N. Vorrunde"-Blätter lesen → Siege/Punkte pro Team berechnen</li>
 *   <li>Teams nach konfiguriertem Modus in Finalgruppen einteilen:
 *       <ul>
 *         <li>{@link MaastrichterGruppenModus#NACH_SIEGEN}: A = max. Siege, B = max-1, ...</li>
 *         <li>{@link MaastrichterGruppenModus#NACH_GROESSE}: gleichmäßige Aufteilung via
 *             {@code GruppenAufteilungRechner}</li>
 *       </ul>
 *   </li>
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
		processBoxinfo("processbox.maastrichter.finalrunde.erstellen");

		if (!pruefeUndAktualisiereVorrundenRangliste()) {
			return;
		}

		var meldeliste = new MaastrichterMeldeListeSheetUpdate(getWorkingSpreadsheet());
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen == null || aktiveMeldungen.size() < 2) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("maastrichter.finalrunde.caption"))
					.message(I18n.get("maastrichter.finalrunde.fehler.zu.wenige.teams"))
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
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("maastrichter.finalrunde.caption"))
					.message(I18n.get("maastrichter.finalrunde.fehler.keine.ergebnisse"))
					.show();
			return;
		}

		// Gruppen gemäß konfiguriertem Modus bilden
		MaastrichterGruppenModus gruppenModus = konfigSheet.getMaastrichterGruppenModus();
		List<List<SchweizerTeamErgebnis>> gruppen = switch (gruppenModus) {
			case NACH_SIEGEN -> teileNachSiegen(sortiert, anzVorrunden);
			case NACH_GROESSE -> teileNachGroesse(sortiert,
					konfigSheet.getGruppenGroesse(), konfigSheet.getMinRestGroesse());
		};

		// Alte Finale-Blätter löschen
		alleFinaleSheetNamenLoeschen();

		// KO-Bracket für jede Gruppe erstellen
		KoTurnierbaumSheet koSheet = new KoTurnierbaumSheet(getWorkingSpreadsheet());
		short sheetPos = DefaultSheetPos.MAASTRICHTER_FINALE;

		for (int g = 0; g < gruppen.size(); g++) {
			SheetRunner.testDoCancelTask();
			List<SchweizerTeamErgebnis> gruppeErg = gruppen.get(g);
			String gruppenBuchstabe = String.valueOf((char) ('A' + g));
			String sheetName = SheetNamen.koFinaleGruppe(gruppenBuchstabe);
			processBoxinfo("processbox.erstelle.sheet.teams", sheetName, gruppeErg.size());
			TeamMeldungen gruppeTeams = erstelleGruppeTeams(gruppeErg, aktiveMeldungen);
			if (gruppeTeams.size() >= 2) {
				koSheet.erstelleGruppeBracket(gruppeTeams, sheetName, sheetPos, konfigSheet,
						SheetMetadataHelper.schluesselMaastrichterFinalrunde(gruppenBuchstabe));
				sheetPos++;
			}
		}
	}

	/**
	 * Teilt die sortierten Teams nach exakter Sieganzahl in Gruppen auf.
	 * A = maxSiege, B = maxSiege-1, C = maxSiege-2, usw. Leere Gruppen werden übersprungen.
	 */
	private List<List<SchweizerTeamErgebnis>> teileNachSiegen(
			List<SchweizerTeamErgebnis> sortiert, int anzVorrunden) {

		Map<Integer, List<SchweizerTeamErgebnis>> nachSiegen = new HashMap<>();
		for (SchweizerTeamErgebnis erg : sortiert) {
			nachSiegen.computeIfAbsent(erg.siege(), k -> new ArrayList<>()).add(erg);
		}

		List<List<SchweizerTeamErgebnis>> gruppen = new ArrayList<>();
		for (int siege = anzVorrunden; siege >= 0; siege--) {
			List<SchweizerTeamErgebnis> gruppe = nachSiegen.get(siege);
			if (gruppe != null && !gruppe.isEmpty()) {
				gruppen.add(gruppe);
			}
		}
		return gruppen;
	}

	/**
	 * Teilt die sortierten Teams gleichmäßig nach Rang auf (bisheriges Verhalten).
	 */
	private List<List<SchweizerTeamErgebnis>> teileNachGroesse(
			List<SchweizerTeamErgebnis> sortiert, int gruppenGroesse, int minRestGroesse) {

		List<Integer> gruppenGroessen = GruppenAufteilungRechner.berechne(
				sortiert.size(), gruppenGroesse, minRestGroesse);

		List<List<SchweizerTeamErgebnis>> gruppen = new ArrayList<>();
		int startIndex = 0;
		for (int groesse : gruppenGroessen) {
			gruppen.add(new ArrayList<>(sortiert.subList(startIndex, startIndex + groesse)));
			startIndex += groesse;
		}
		return gruppen;
	}

	/**
	 * Liest alle Vorrunden-Blätter und berechnet Siege, Punkte und Gegnerlisten pro Team.
	 */
	private List<SchweizerTeamErgebnis> leseVorrundenErgebnisse(
			TeamMeldungen aktiveMeldungen, int anzVorrunden,
			MaastrichterMeldeListeSheetUpdate meldeliste) throws GenerateException {

		Map<Integer, int[]> statsMap = new HashMap<>(); // teamNr → [0]=siege, [1]=punkte+, [2]=punkte-
		Map<Integer, List<Integer>> gegnerMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new int[3]);
			gegnerMap.put(team.getNr(), new ArrayList<>());
		}

		for (int runde = 1; runde <= anzVorrunden; runde++) {
			SheetRunner.testDoCancelTask();
			String legacyName = runde + ". " + SheetNamen.LEGACY_MAASTRICHTER_VORRUNDE_PRAEFIX;
			XSpreadsheet rundeSheet = SheetMetadataHelper.findeSheetUndHeile(
					getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
					SheetMetadataHelper.schluesselMaastrichterVorrunde(runde), legacyName);
			if (rundeSheet == null) {
				logger.warn("Vorrunden-Sheet '{}' nicht gefunden, übersprungen.", legacyName);
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
			MaastrichterMeldeListeSheetUpdate meldeliste) throws GenerateException {

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

	private int resolveTeamNr(CellData cell, MaastrichterMeldeListeSheetUpdate meldeliste) throws GenerateException {
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
	 * Prüft ob die Vorrunden-Rangliste vorhanden ist. Wenn nicht, wird der Benutzer gefragt ob sie
	 * erstellt werden soll. Wenn vorhanden (oder gerade erstellt), wird sie immer aktualisiert.
	 *
	 * @return true wenn die Rangliste vorhanden und aktualisiert wurde, false wenn abgebrochen
	 */
	private boolean pruefeUndAktualisiereVorrundenRangliste() throws GenerateException {
		var ranglisteUpdate = new MaastrichterVorrundenRanglisteSheetUpdate(getWorkingSpreadsheet());
		if (ranglisteUpdate.getXSpreadSheet() == null) {
			MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption(I18n.get("maastrichter.finalrunde.vorrunden.rangliste.fehlt.caption"))
					.message(I18n.get("maastrichter.finalrunde.vorrunden.rangliste.fehlt.text"))
					.show();
			if (result != MessageBoxResult.YES) {
				return false;
			}
		}
		processBoxinfo("processbox.rangliste.aktualisieren");
		ranglisteUpdate.doRun();
		return true;
	}

	/**
	 * Löscht alle vorhandenen Finale-Sheets (A-Finale bis Z-Finale gemäß i18n-Muster).
	 */
	private void alleFinaleSheetNamenLoeschen() throws GenerateException {
		for (char c = 'A'; c <= 'Z'; c++) {
			String sheetName = SheetNamen.koFinaleGruppe(String.valueOf(c));
			if (getSheetHelper().findByName(sheetName) != null) {
				getSheetHelper().removeSheet(sheetName);
			}
		}
	}

}
