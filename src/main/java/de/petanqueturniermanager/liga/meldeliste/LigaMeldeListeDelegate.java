/*
 * Erstellung : 15.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.liga.meldeliste;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.SpielrundeGespielt;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import com.sun.star.util.CellProtection;

import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
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
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

class LigaMeldeListeDelegate implements MeldeListeKonstanten {

	private static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 7;

	static final int MANNSCHAFTSFUEHRER_SPALTE = SPIELER_NR_SPALTE + 2;
	static final int EMAIL_SPALTE = SPIELER_NR_SPALTE + 3;
	static final int TELEFON_SPALTE = SPIELER_NR_SPALTE + 4;
	static final int STARTSPIELZEIT_SPALTE = SPIELER_NR_SPALTE + 5;
	static final int ADRESSE_SPALTE = SPIELER_NR_SPALTE + 6;
	static final int LETZTE_INFO_SPALTE = ADRESSE_SPALTE;

	private static final int MANNSCHAFTSFUEHRER_WIDTH = 5000;
	private static final int EMAIL_WIDTH = 5000;
	private static final int TELEFON_WIDTH = 3000;
	private static final int STARTSPIELZEIT_WIDTH = 2500;
	private static final int ADRESSE_WIDTH = 8000;

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
		formatInfoSpalten(headerBackColor);

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

		var nameSpalteRange = RangePosition.from(SPIELER_NR_SPALTE + 1, ERSTE_DATEN_ZEILE,
				SPIELER_NR_SPALTE + 1, letzteDatenZeile);
		EditierbaresZelleFormatHelper.anwenden(sheet, nameSpalteRange);

		var editierbar = new CellProtection();
		editierbar.IsLocked = false;
		sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), nameSpalteRange,
				CellProperties.from().setCellProtection(editierbar));
	}

	private void formatInfoSpalten(int headerBackColor) throws GenerateException {
		sheet.processBoxinfo("processbox.meldeliste.spalten.formatieren");

		int letzteDatenZeile = meldungenSpalte.getLetzteDatenZeileUseMin();

		infoSpaltenHeaderSchreiben(headerBackColor);

		var infoRange = RangePosition.from(MANNSCHAFTSFUEHRER_SPALTE, ERSTE_DATEN_ZEILE, LETZTE_INFO_SPALTE, letzteDatenZeile);
		sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), infoRange, CellProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()).setShrinkToFit(true));

		EditierbaresZelleFormatHelper.anwenden(sheet, infoRange);

		var editierbar = new CellProtection();
		editierbar.IsLocked = false;
		sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), infoRange,
				CellProperties.from().setCellProtection(editierbar));
	}

	private void infoSpaltenHeaderSchreiben(int headerBackColor) throws GenerateException {
		record InfoSpalte(int spalte, String i18nKey, int breite) {}
		var spalten = List.of(
				new InfoSpalte(MANNSCHAFTSFUEHRER_SPALTE, "liga.meldeliste.column.header.mannschaftsfuehrer", MANNSCHAFTSFUEHRER_WIDTH),
				new InfoSpalte(EMAIL_SPALTE, "liga.meldeliste.column.header.email", EMAIL_WIDTH),
				new InfoSpalte(TELEFON_SPALTE, "liga.meldeliste.column.header.telefon", TELEFON_WIDTH),
				new InfoSpalte(STARTSPIELZEIT_SPALTE, "liga.meldeliste.column.header.startspielzeit", STARTSPIELZEIT_WIDTH),
				new InfoSpalte(ADRESSE_SPALTE, "liga.meldeliste.column.header.adresse", ADRESSE_WIDTH));

		for (var info : spalten) {
			var colProps = ColumnProperties.from().setWidth(info.breite())
					.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
					.margin(MeldeListeKonstanten.CELL_MARGIN);
			var celVal = StringCellValue
					.from(sheet.getXSpreadSheet(), Position.from(info.spalte(), ZWEITE_HEADER_ZEILE),
							I18n.get(info.i18nKey()))
					.addColumnProperties(colProps)
					.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder())
					.setCellBackColor(headerBackColor)
					.setVertJustify(CellVertJustify2.CENTER)
					.setShrinkToFit(true);
			sheet.getSheetHelper().setStringValueInCell(celVal);
		}
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

	int letzteSpielTagSpalte() {
		return LETZTE_INFO_SPALTE;
	}

	int getSpielerNameErsteSpalte() {
		return meldungenSpalte.getErsteMeldungNameSpalte();
	}

	int letzteZeileMitSpielerName() throws GenerateException {
		return meldungenSpalte.letzteZeileMitSpielerName();
	}

	Map<Integer, String> leseTeamNamenMap() throws GenerateException {
		Map<Integer, String> result = new HashMap<>();
		XSpreadsheet mlSheet = sheet.getXSpreadSheet();
		if (mlSheet == null) {
			return result;
		}
		var xDoc = sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		RangeData data = RangeHelper.from(mlSheet, xDoc,
				RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE + 1, ERSTE_DATEN_ZEILE + 999))
				.getDataFromRange();
		for (RowData row : data) {
			if (row.isEmpty()) {
				break;
			}
			int nr = row.get(0).getIntVal(0);
			if (nr <= 0) {
				continue;
			}
			String name = row.size() > 1 ? row.get(1).getStringVal().trim() : "";
			result.put(nr, name);
		}
		return result;
	}

}
