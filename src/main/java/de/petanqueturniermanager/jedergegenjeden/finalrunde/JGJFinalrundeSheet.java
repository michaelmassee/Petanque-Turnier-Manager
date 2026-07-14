/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.finalrunde;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.common.GruppenAufteilungRechner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJGesamtranglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteRechner;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteRechner.TeamStats;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Erstellt JGJ-Finalrunden aus der gruppenübergreifenden Gesamtrangliste.
 */
public class JGJFinalrundeSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(JGJFinalrundeSheet.class);

	public JGJFinalrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.JGJ, "JGJ-Finalrunde");
	}

	@Override
	public XSpreadsheet getXSpreadSheet() {
		return null;
	}

	@Override
	public TurnierSheet getTurnierSheet() {
		return null;
	}

	@Override
	protected JGJKonfigurationSheet getKonfigurationSheet() {
		return new JGJKonfigurationSheet(getWorkingSpreadsheet());
	}

	@Override
	public void doRun() throws GenerateException {
		processBoxinfo("processbox.jgj.finalrunde.erstellen");

		if (!pruefeUndAktualisiereGesamtrangliste()) {
			return;
		}

		var meldeliste = new JGJMeldeListeSheet_Update(getWorkingSpreadsheet());
		meldeliste.upDateSheet();
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen == null || aktiveMeldungen.size() < 2) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("jgj.finalrunde.caption"))
					.message(I18n.get("jgj.finalrunde.fehler.zu.wenige.teams"))
					.show();
			return;
		}

		List<TeamStats> rangfolge = berechneGesamtrangfolge(aktiveMeldungen);
		if (rangfolge.size() < 2) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("jgj.finalrunde.caption"))
					.message(I18n.get("jgj.finalrunde.fehler.keine.ergebnisse"))
					.show();
			return;
		}

		JGJKonfigurationSheet konfig = getKonfigurationSheet();
		List<Finalgruppe> finalgruppen = bildeFinalgruppen(rangfolge, aktiveMeldungen, konfig);

		if (irgendeinFinaleSheetVorhanden()) {
			MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption(I18n.get("jgj.finalrunde.caption"))
					.message(I18n.get("jgj.finalrunde.bereits.vorhanden.text"))
					.show();
			if (result != MessageBoxResult.YES) {
				logger.debug("JGJ-Finalrunden bereits vorhanden - Neuaufbau vom Benutzer abgelehnt.");
				return;
			}
		}

		alleFinaleSheetNamenLoeschen();

		KoTurnierbaumSheet koSheet = new KoTurnierbaumSheet(getWorkingSpreadsheet());
		short sheetPos = DefaultSheetPos.JGJ_GESAMTRANGLISTE;
		List<KoTurnierbaumSheet.GruppenBracketAuftrag> bracketAuftraege = new ArrayList<>();
		for (Finalgruppe finalgruppe : finalgruppen) {
			String sheetName = SheetNamen.koFinaleGruppe(finalgruppe.buchstabe());
			processBoxinfo("processbox.erstelle.sheet.teams", sheetName, finalgruppe.teams().size());
			bracketAuftraege.add(new KoTurnierbaumSheet.GruppenBracketAuftrag(
					finalgruppe.teams(), sheetName, sheetPos,
					SheetMetadataHelper.schluesselJgjFinalrunde(finalgruppe.buchstabe()),
					finalgruppe.buchstabe()));
			sheetPos++;
		}
		koSheet.erstelleGruppenBrackets(bracketAuftraege, konfig);
	}

	private record Finalgruppe(String buchstabe, TeamMeldungen teams) {}

	private boolean pruefeUndAktualisiereGesamtrangliste() throws GenerateException {
		var gesamtrangliste = new JGJGesamtranglisteSheet(getWorkingSpreadsheet());
		if (gesamtrangliste.getXSpreadSheet() == null) {
			MessageBoxResult result = MessageBox.from(getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption(I18n.get("jgj.finalrunde.gesamtrangliste.fehlt.caption"))
					.message(I18n.get("jgj.finalrunde.gesamtrangliste.fehlt.text"))
					.show();
			if (result != MessageBoxResult.YES) {
				return false;
			}
		}
		processBoxinfo("processbox.rangliste.aktualisieren");
		gesamtrangliste.upDateSheet();
		return true;
	}

	private List<TeamStats> berechneGesamtrangfolge(TeamMeldungen aktiveMeldungen) throws GenerateException {
		var gesamtrangliste = new JGJGesamtranglisteSheet(getWorkingSpreadsheet());
		List<TeamMeldungen> gruppen = gesamtrangliste.ermittleGruppen(aktiveMeldungen);
		return gesamtrangliste.berechneReihenfolge(gruppen);
	}

	private List<Finalgruppe> bildeFinalgruppen(List<TeamStats> rangfolge,
			TeamMeldungen aktiveMeldungen, JGJKonfigurationSheet konfig) throws GenerateException {
		List<Integer> gruppenGroessen = GruppenAufteilungRechner.berechne(
				rangfolge.size(), konfig.getGruppenGroesse(), konfig.getMinLetzteGruppeGroesse());
		List<Finalgruppe> finalgruppen = new ArrayList<>();
		int startIndex = 0;
		char buchstabe = 'A';
		for (int groesse : gruppenGroessen) {
			SheetRunner.testDoCancelTask();
			TeamMeldungen gruppeTeams = new TeamMeldungen();
			for (TeamStats stats : rangfolge.subList(startIndex, startIndex + groesse)) {
				var team = aktiveMeldungen.getTeam(stats.teamNr());
				if (team != null) {
					gruppeTeams.addTeamWennNichtVorhanden(team);
				}
			}
			if (gruppeTeams.size() >= 2) {
				finalgruppen.add(new Finalgruppe(String.valueOf(buchstabe++), gruppeTeams));
			}
			startIndex += groesse;
		}
		return finalgruppen;
	}

	private boolean irgendeinFinaleSheetVorhanden() throws GenerateException {
		for (char c = 'A'; c <= 'Z'; c++) {
			if (getSheetHelper().findByName(SheetNamen.koFinaleGruppe(String.valueOf(c))) != null) {
				return true;
			}
		}
		return false;
	}

	private void alleFinaleSheetNamenLoeschen() throws GenerateException {
		for (char c = 'A'; c <= 'Z'; c++) {
			String sheetName = SheetNamen.koFinaleGruppe(String.valueOf(c));
			if (getSheetHelper().findByName(sheetName) != null) {
				getSheetHelper().removeSheet(sheetName);
			}
		}
	}
}
