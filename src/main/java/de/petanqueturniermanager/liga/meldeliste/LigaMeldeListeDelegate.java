/**
 * Erstellung : 15.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.liga.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

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
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

class LigaMeldeListeDelegate implements MeldeListeKonstanten {

	private static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 7;

	private final IMeldeliste<TeamMeldungen, Team> sheet;
	private final LigaKonfigurationSheet konfigurationSheet;
	private final MeldungenSpalte<TeamMeldungen, Team> meldungenSpalte;
	private final MeldeListeHelper<TeamMeldungen, Team> meldeListeHelper;
	private final TurnierSystem turnierSystem;

	LigaMeldeListeDelegate(IMeldeliste<TeamMeldungen, Team> sheet, WorkingSpreadsheet ws, TurnierSystem turnierSystem,
			String metadatenSchluessel) {
		this.sheet = sheet;
		this.turnierSystem = turnierSystem;
		konfigurationSheet = new LigaKonfigurationSheet(ws);
		meldungenSpalte = MeldungenSpalte.builder()
				.spalteMeldungNameWidth(LigaKonfigurationSheet.LIGA_MELDUNG_NAME_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(sheet)
				.minAnzZeilen(MIN_ANZAHL_MELDUNGEN_ZEILEN).formation(Formation.TETE).build();
		meldeListeHelper = new MeldeListeHelper<>(sheet, metadatenSchluessel);
	}

	LigaKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	MeldungenSpalte<TeamMeldungen, Team> getMeldungenSpalte() {
		return meldungenSpalte;
	}

	private void renameSpielPlanSheet() throws GenerateException {
		String oldName = "Liga Spielplan";
		sheet.processBoxinfo("processbox.pruefe.ob.vorhanden", oldName);
		if (sheet.getSheetHelper().findByName(oldName) != null) {
			MessageBoxResult answer = MessageBox.from(sheet.getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption(I18n.get("msg.caption.tabelle", oldName))
					.message(I18n.get("msg.text.tabelle.umbenennen.frage", oldName, LigaSpielPlanSheet.sheetName()))
					.show();

			if (answer == MessageBoxResult.YES) {
				XSpreadsheet spielplan = sheet.getSheetHelper().findByName(oldName);
				if (sheet.getSheetHelper().reNameSheet(spielplan, LigaSpielPlanSheet.sheetName())) {
					MessageBox.from(sheet.getxContext(), MessageBoxTypeEnum.INFO_OK)
							.caption(I18n.get("msg.caption.tabelle", oldName))
							.message(I18n.get("msg.text.tabelle.umbenannt.ok", oldName, LigaSpielPlanSheet.sheetName()))
							.show();
				} else {
					MessageBox.from(sheet.getxContext(), MessageBoxTypeEnum.ERROR_OK)
							.caption(I18n.get("msg.caption.tabelle", oldName))
							.message(I18n.get("msg.text.tabelle.umbenannt.fehler", oldName, LigaSpielPlanSheet.sheetName()))
							.show();
					throw new GenerateException(I18n.get("error.sheet.umbenennen", oldName, LigaSpielPlanSheet.sheetName()));
				}
			} else {
				throw new GenerateException(I18n.get("error.sheet.umbenennen", oldName, LigaSpielPlanSheet.sheetName()));
			}
		}
	}

	void upDateSheet() throws GenerateException {
		sheet.processBoxinfo("processbox.meldeliste.sortieren");
		renameSpielPlanSheet();

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
