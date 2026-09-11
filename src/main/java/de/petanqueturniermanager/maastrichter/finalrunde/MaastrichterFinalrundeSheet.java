/*
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
import de.petanqueturniermanager.algorithmen.common.GruppenAufteilungRechner;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterGruppenSpalteHelper;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterGruppenModus;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellt die Finalrundenblätter (A-Finale, B-Finale, C-Finale, D-Finale) für das
 * Maastrichter Turniersystem.
 * <p>
 * Ablauf:
 * <ol>
 *   <li>Vorrunden-Rangliste aktualisieren und deren bereits sortierte Ergebnisse übernehmen
 *       ({@link MaastrichterVorrundenRanglisteSheetUpdate#getZuletztSortierteErgebnisse()}) –
 *       kein eigenes, zweites Einlesen der "N. Vorrunde"-Blätter</li>
 *   <li>Optional: nur die besten "Maximale Anzahl Teams KO-Phase" Teams übernehmen
 *       (0 = kein Limit); schwächer platzierte Teams bleiben ohne KO-Spiel, aber mit
 *       Cutoff-Markierung in der Vorrunden-Rangliste stehen</li>
 *   <li>Teams nach konfiguriertem Modus in Finalgruppen einteilen:
 *       <ul>
 *         <li>{@link MaastrichterGruppenModus#NACH_SIEGEN}: A = max. Siege, B = max-1, ...</li>
 *         <li>{@link MaastrichterGruppenModus#NACH_GROESSE}: einfache Aufteilung
 *             nach Rang in Chunks der konfigurierten Gruppengröße; Rest erhält
 *             immer eine eigene Folgegruppe (1-Team-Rest wird in vorherige
 *             Gruppe gefaltet)</li>
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

		MaastrichterVorrundenRanglisteSheetUpdate ranglisteUpdate = pruefeUndAktualisiereVorrundenRangliste();
		if (ranglisteUpdate == null) {
			return;
		}

		var meldeliste = new MaastrichterMeldeListeSheetUpdate(getWorkingSpreadsheet());
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen.size() < 2) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("maastrichter.finalrunde.caption"))
					.message(I18n.get("maastrichter.finalrunde.fehler.zu.wenige.teams"))
					.show();
			return;
		}

		MaastrichterKonfigurationSheet konfigSheet = getKonfigurationSheet();
		int anzVorrunden = konfigSheet.getAnzVorrunden();

		// Bereits von der Vorrunden-Rangliste berechnete und geschriebene Reihenfolge
		// wiederverwenden – kein zweites Einlesen der Vorrunden-Sheets, damit KO-Einteilung und
		// Rangliste niemals auseinanderlaufen können (ausgestiegene Teams sind darin enthalten,
		// siehe SchweizerRanglisteSheet; sie werden weiter unten in
		// erstelleGruppeTeams(gruppeErg, aktiveMeldungen) trotzdem NICHT den spielenden
		// Finalgruppen zugeordnet, da dort bewusst die rein aktive Teamliste verwendet wird).
		List<SchweizerTeamErgebnis> sortiert = ranglisteUpdate.getZuletztSortierteErgebnisse();

		if (sortiert.isEmpty()) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("maastrichter.finalrunde.caption"))
					.message(I18n.get("maastrichter.finalrunde.fehler.keine.ergebnisse"))
					.show();
			return;
		}

		// Obergrenze für die KO-Phase: schwächer platzierte Teams bleiben ohne KO-Spiel,
		// tauchen aber weiterhin (mit Cutoff-Markierung statt Gruppenbuchstabe) in der
		// Vorrunden-Rangliste auf.
		int maxTeamsKoPhase = konfigSheet.getMaxTeamsKoPhase();
		List<SchweizerTeamErgebnis> teamsFuerKo = sortiert;
		List<SchweizerTeamErgebnis> ausserhalbCutoff = List.of();
		if (maxTeamsKoPhase > 0) {
			// Ausgestiegene Teams bleiben für die Vorrundenrangliste erhalten, dürfen aber
			// keinen der tatsächlich spielenden KO-Plätze verbrauchen.
			List<SchweizerTeamErgebnis> aktiveTeamsNachRang = sortiert.stream()
					.filter(erg -> aktiveMeldungen.getTeam(erg.teamNr()) != null)
					.toList();
			List<SchweizerTeamErgebnis> aktiveTeamsImKo = aktiveTeamsNachRang.subList(0,
					Math.min(maxTeamsKoPhase, aktiveTeamsNachRang.size()));
			teamsFuerKo = aktiveTeamsImKo;
			ausserhalbCutoff = sortiert.stream().filter(erg -> !aktiveTeamsImKo.contains(erg)).toList();
			if (!ausserhalbCutoff.isEmpty()) {
				processBoxinfo("processbox.maastrichter.cutoff.info", ausserhalbCutoff.size());
			}
		}

		// Gruppen gemäß konfiguriertem Modus bilden
		MaastrichterGruppenModus gruppenModus = konfigSheet.getMaastrichterGruppenModus();
		List<List<SchweizerTeamErgebnis>> gruppen = switch (gruppenModus) {
			case NACH_SIEGEN -> teileNachSiegen(teamsFuerKo, anzVorrunden);
			case NACH_GROESSE -> teileNachGroesse(teamsFuerKo, konfigSheet.getGruppenGroesse(),
					konfigSheet.getMinLetzteGruppeGroesse());
		};

		// Buchstabe wird nur für Gruppen mit ≥2 Teams vergeben, damit 'A' immer belegt ist
		Map<Integer, String> teamNrZuGruppe = new HashMap<>();
		for (SchweizerTeamErgebnis erg : ausserhalbCutoff) {
			teamNrZuGruppe.put(erg.teamNr(), MaastrichterGruppenSpalteHelper.keinKoMarker());
		}
		List<Finalgruppe> finalgruppen = new ArrayList<>();
		char naechsterBuchstabe = 'A';

		for (List<SchweizerTeamErgebnis> gruppeErg : gruppen) {
			SheetRunner.testDoCancelTask();
			TeamMeldungen gruppeTeams = erstelleGruppeTeams(gruppeErg, aktiveMeldungen);
			if (gruppeTeams.size() >= 2) {
				String gruppenBuchstabe = String.valueOf(naechsterBuchstabe++);
				finalgruppen.add(new Finalgruppe(gruppenBuchstabe, gruppeTeams));
				for (SchweizerTeamErgebnis erg : gruppeErg) {
					teamNrZuGruppe.put(erg.teamNr(), gruppenBuchstabe);
				}
			}
		}

		if (irgendeinFinaleSheetVorhanden()) {
			MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption(I18n.get("maastrichter.finalrunde.caption"))
					.message(I18n.get("maastrichter.finalrunde.bereits.vorhanden.text"))
					.show();
			if (result != MessageBoxResult.YES) {
				logger.debug("Maastrichter-Finalrunden bereits vorhanden – Neuaufbau vom Benutzer abgelehnt.");
				return;
			}
		}

		// Alte Finale-Blätter löschen
		alleFinaleSheetNamenLoeschen();

		// KO-Bracket für jede Gruppe erstellen
		KoTurnierbaumSheet koSheet = new KoTurnierbaumSheet(getWorkingSpreadsheet());
		short sheetPos = DefaultSheetPos.MAASTRICHTER_FINALE;
		List<KoTurnierbaumSheet.GruppenBracketAuftrag> bracketAuftraege = new ArrayList<>();

		for (Finalgruppe finalgruppe : finalgruppen) {
			String sheetName = SheetNamen.koFinaleGruppe(finalgruppe.buchstabe());
			processBoxinfo("processbox.erstelle.sheet.teams", sheetName, finalgruppe.teams().size());
			bracketAuftraege.add(new KoTurnierbaumSheet.GruppenBracketAuftrag(
					finalgruppe.teams(), sheetName, sheetPos,
					SheetMetadataHelper.schluesselMaastrichterFinalrunde(finalgruppe.buchstabe()),
					finalgruppe.buchstabe()));
			sheetPos++;
		}
		koSheet.erstelleGruppenBrackets(bracketAuftraege, konfigSheet);

		// Gruppen-Buchstaben einmalig in die Vorrunden-Rangliste schreiben
		// (Spalte "Gruppe"). Nachfolgende Rangliste-Refreshs erhalten die Werte.
		if (!teamNrZuGruppe.isEmpty()) {
			new MaastrichterGruppenUebersichtSheet(getWorkingSpreadsheet(), teamNrZuGruppe).generate();
			new MaastrichterVorrundenRanglisteSheetUpdate(getWorkingSpreadsheet())
					.aktualisiereMitGruppenZuweisungen(teamNrZuGruppe);
		}
	}

	private record Finalgruppe(String buchstabe, TeamMeldungen teams) {}

	private boolean irgendeinFinaleSheetVorhanden() throws GenerateException {
		for (char c = 'A'; c <= 'Z'; c++) {
			if (getSheetHelper().findByName(SheetNamen.koFinaleGruppe(String.valueOf(c))) != null) {
				return true;
			}
		}
		return false;
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
	 * Teilt die sortierten Teams nach Rang in Chunks gemäß {@link GruppenAufteilungRechner}.
	 * Kleine letzte Gruppen werden in die vorherige gefaltet; Cadrage übernimmt den Ausgleich.
	 */
	private List<List<SchweizerTeamErgebnis>> teileNachGroesse(
			List<SchweizerTeamErgebnis> sortiert, int gruppenGroesse, int minLetzteGruppe) {

		List<Integer> gruppenGroessen = GruppenAufteilungRechner.berechne(
				sortiert.size(), gruppenGroesse, minLetzteGruppe);
		List<List<SchweizerTeamErgebnis>> gruppen = new ArrayList<>();
		int startIndex = 0;
		for (int groesse : gruppenGroessen) {
			gruppen.add(new ArrayList<>(sortiert.subList(startIndex, startIndex + groesse)));
			startIndex += groesse;
		}
		return gruppen;
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
	 * @return die aktualisierte Vorrunden-Rangliste (zum Weiterverwenden ihrer sortierten
	 *         Ergebnisse), oder {@code null} wenn der Benutzer abgebrochen hat
	 */
	private MaastrichterVorrundenRanglisteSheetUpdate pruefeUndAktualisiereVorrundenRangliste()
			throws GenerateException {
		var ranglisteUpdate = new MaastrichterVorrundenRanglisteSheetUpdate(getWorkingSpreadsheet());
		if (ranglisteUpdate.getXSpreadSheet() == null) {
			MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption(I18n.get("maastrichter.finalrunde.vorrunden.rangliste.fehlt.caption"))
					.message(I18n.get("maastrichter.finalrunde.vorrunden.rangliste.fehlt.text"))
					.show();
			if (result != MessageBoxResult.YES) {
				return null;
			}
		}
		processBoxinfo("processbox.rangliste.aktualisieren");
		ranglisteUpdate.doRun();
		return ranglisteUpdate;
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
