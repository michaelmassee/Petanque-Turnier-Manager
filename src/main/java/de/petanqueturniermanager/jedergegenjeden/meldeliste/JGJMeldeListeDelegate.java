/**
 * Erstellung : 15.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import java.util.List;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.SpielrundeGespielt;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

class JGJMeldeListeDelegate implements MeldeListeKonstanten {

	private static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 5;

	private final IMeldeliste<TeamMeldungen, Team> sheet;
	private final JGJKonfigurationSheet konfigurationSheet;
	private final MeldungenSpalte<TeamMeldungen, Team> meldungenSpalte;
	private final MeldeListeHelper<TeamMeldungen, Team> meldeListeHelper;
	private final TurnierSystem turnierSystem;

	JGJMeldeListeDelegate(IMeldeliste<TeamMeldungen, Team> sheet, WorkingSpreadsheet ws, TurnierSystem turnierSystem,
			String metadatenSchluessel) {
		this.sheet = sheet;
		this.turnierSystem = turnierSystem;
		konfigurationSheet = new JGJKonfigurationSheet(ws);
		meldungenSpalte = MeldungenSpalte.builder()
				.spalteMeldungNameWidth(JGJKonfigurationSheet.MELDUNG_NAME_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(sheet)
				.minAnzZeilen(MIN_ANZAHL_MELDUNGEN_ZEILEN).formation(Formation.TETE).build();
		meldeListeHelper = new MeldeListeHelper<>(sheet, metadatenSchluessel);
	}

	JGJKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	MeldungenSpalte<TeamMeldungen, Team> getMeldungenSpalte() {
		return meldungenSpalte;
	}

	void upDateSheet() throws GenerateException {
		sheet.processBoxinfo("processbox.meldeliste.sortieren");

		TurnierSheet.from(sheet.getXSpreadSheet(), sheet.getWorkingSpreadsheet()).setActiv();
		meldeListeHelper.testDoppelteMeldungen();

		int headerBackColor = konfigurationSheet.getMeldeListeHeaderFarbe();
		meldungenSpalte.insertHeaderInSheet(headerBackColor);
		meldeListeHelper.zeileOhneSpielerNamenEntfernen();
		meldeListeHelper.updateMeldungenNr();
		meldeListeHelper.doSort(meldungenSpalte.getSpielerNrSpalte(), true);

		meldungenSpalte.formatSpielrNrUndNamenspalten();
		formatDaten();

		meldeListeHelper.insertTurnierSystemInHeader(turnierSystem);

		SheetFreeze.from(sheet.getTurnierSheet()).anzZeilen(2).doFreeze();
	}

	void formatDaten() throws GenerateException {
		sheet.processBoxinfo("processbox.meldeliste.spalten.formatieren");

		int letzteDatenZeile = meldungenSpalte.getLetzteDatenZeileUseMin();

		MeldungenHintergrundFarbeGeradeStyle meldungenHintergrundFarbeGeradeStyle = konfigurationSheet
				.getMeldeListeHintergrundFarbeGeradeStyle();
		MeldungenHintergrundFarbeUnGeradeStyle meldungenHintergrundFarbeUnGeradeStyle = konfigurationSheet
				.getMeldeListeHintergrundFarbeUnGeradeStyle();

		meldeListeHelper.insertFormulaFuerDoppelteSpielerNrGeradeUngradeFarbe(letzteDatenZeile, sheet,
				meldungenHintergrundFarbeGeradeStyle, meldungenHintergrundFarbeUnGeradeStyle);

		meldeListeHelper.insertFormulaFuerDoppelteNamenGeradeUngradeFarbe(SPIELER_NR_SPALTE + 1, SPIELER_NR_SPALTE + 1,
				letzteDatenZeile, sheet, meldungenHintergrundFarbeGeradeStyle, meldungenHintergrundFarbeUnGeradeStyle);
	}

	String formulaSverweisSpielernamen(String spielrNrAdresse) {
		return meldeListeHelper.formulaSverweisSpielernamen(spielrNrAdresse);
	}

	TeamMeldungen getAlleMeldungen() throws GenerateException {
		return (TeamMeldungen) meldeListeHelper.getMeldungen(SpielTagNr.from(1), null, new TeamMeldungen());
	}

	int getSpielerZeileNr(int spielerNr) throws GenerateException {
		return meldungenSpalte.getSpielerZeileNr(spielerNr);
	}

	int naechsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		return meldungenSpalte.naechsteFreieDatenZeileInSpielerNrSpalte();
	}

	int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return meldungenSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	int getErsteDatenZiele() {
		return meldungenSpalte.getErsteDatenZiele();
	}

	List<String> getSpielerNamenList() throws GenerateException {
		return meldungenSpalte.getSpielerNamenList();
	}

	List<Integer> getSpielerNrList() throws GenerateException {
		return meldungenSpalte.getSpielerNrList();
	}

	int getLetzteDatenZeileUseMin() throws GenerateException {
		return meldungenSpalte.getLetzteDatenZeileUseMin();
	}

	int letzteSpielTagSpalte() throws GenerateException {
		return meldeListeHelper.ersteSpieltagSpalte();
	}

	int getSpielerNameErsteSpalte() {
		return meldungenSpalte.getErsteMeldungNameSpalte();
	}

	int letzteZeileMitSpielerName() throws GenerateException {
		return meldungenSpalte.letzteZeileMitSpielerName();
	}

}
