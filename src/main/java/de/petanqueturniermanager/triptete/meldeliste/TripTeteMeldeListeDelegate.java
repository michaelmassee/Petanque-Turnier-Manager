package de.petanqueturniermanager.triptete.meldeliste;

import java.util.List;

import com.sun.star.util.CellProtection;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;

/**
 * Delegate für die Trip-Tête-Meldeliste – analog {@code LigaMeldeListeDelegate},
 * jedoch ohne Spielplan-Umbenenn-Logik (kein Legacy-Sheet).
 */
class TripTeteMeldeListeDelegate implements MeldeListeKonstanten {

	public static final int MELDUNG_NAME_WIDTH = 8000;
	private static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 7;

	private final IMeldeliste<TeamMeldungen, Team> sheet;
	private final TripTeteKonfigurationSheet konfigurationSheet;
	private final MeldungenSpalte<TeamMeldungen, Team> meldungenSpalte;
	private final MeldeListeHelper<TeamMeldungen, Team> meldeListeHelper;
	private final TurnierSystem turnierSystem;

	TripTeteMeldeListeDelegate(IMeldeliste<TeamMeldungen, Team> sheet, WorkingSpreadsheet ws,
			TurnierSystem turnierSystem, String metadatenSchluessel) {
		this.sheet = sheet;
		this.turnierSystem = turnierSystem;
		konfigurationSheet = new TripTeteKonfigurationSheet(ws);
		meldungenSpalte = MeldungenSpalte.builder()
				.spalteMeldungNameWidth(MELDUNG_NAME_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE).sheet(sheet)
				.minAnzZeilen(MIN_ANZAHL_MELDUNGEN_ZEILEN).formation(Formation.TETE).build();
		meldeListeHelper = new MeldeListeHelper<>(sheet, metadatenSchluessel);
	}

	TripTeteKonfigurationSheet getKonfigurationSheet() {
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

		MeldungenHintergrundFarbeGeradeStyle geradeStyle = konfigurationSheet.getMeldeListeHintergrundFarbeGeradeStyle();
		MeldungenHintergrundFarbeUnGeradeStyle ungeradeStyle = konfigurationSheet
				.getMeldeListeHintergrundFarbeUnGeradeStyle();

		meldeListeHelper.insertFormulaFuerDoppelteSpielerNrGeradeUngradeFarbe(letzteDatenZeile, sheet, geradeStyle,
				ungeradeStyle);
		meldeListeHelper.insertFormulaFuerDoppelteNamenGeradeUngradeFarbe(SPIELER_NR_SPALTE + 1, SPIELER_NR_SPALTE + 1,
				letzteDatenZeile, sheet, geradeStyle, ungeradeStyle);

		var nameSpalteRange = RangePosition.from(SPIELER_NR_SPALTE + 1, ERSTE_DATEN_ZEILE,
				SPIELER_NR_SPALTE + 1, letzteDatenZeile);
		EditierbaresZelleFormatHelper.anwenden(sheet, nameSpalteRange);

		var editierbar = new CellProtection();
		editierbar.IsLocked = false;
		sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), nameSpalteRange,
				CellProperties.from().setCellProtection(editierbar));
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
