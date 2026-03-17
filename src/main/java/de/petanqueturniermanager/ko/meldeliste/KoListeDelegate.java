/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Delegate für die K.-O. Meldeliste.
 * Einfache Spaltenstruktur: Nr | Teamname | Aktiv
 */
class KoListeDelegate implements MeldeListeKonstanten {

	/** Minimale Anzahl Datenzeilen (immer vorhanden). */
	static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 32;

	// Spaltenpositionen (0-basiert)
	static final int NR_SPALTE = 0;
	static final int TEAMNAME_SPALTE = 1;
	static final int AKTIV_SPALTE = 2;

	// Erste Datenzeile (1 Header-Zeile)
	static final int ERSTE_DATEN_ZEILE = 1;

	static final int AKTIV_WERT_NIMMT_TEIL = 1;
	static final int AKTIV_WERT_AUSGESTIEGEN = 2;

	private static final int NR_SPALTE_WIDTH = 800;
	private static final int NAME_SPALTE_WIDTH = 3500;
	private static final int AKTIV_SPALTE_WIDTH = 700;

	private final ISheet sheet;
	private final KoKonfigurationSheet konfigurationSheet;

	KoListeDelegate(ISheet sheet) {
		this.sheet = checkNotNull(sheet);
		konfigurationSheet = new KoKonfigurationSheet(sheet.getWorkingSpreadsheet());
	}

	KoKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	void upDateSheet() throws GenerateException {
		sheet.processBoxinfo("Aktualisiere K.-O. Meldeliste");
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		TurnierSheet.from(xSheet, sheet.getWorkingSpreadsheet()).setActiv();
		insertHeaderInSheet();
		formatDatenSpalten();
		formatZeilenfarben();
		SheetFreeze.from(xSheet, sheet.getWorkingSpreadsheet()).anzZeilen(1).doFreeze();
	}

	private void insertHeaderInSheet() throws GenerateException {
		sheet.processBoxinfo("K.-O. Meldeliste Header");
		int headerColor = konfigurationSheet.getMeldeListeHeaderFarbe();

		// Titelzeile oben links
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue.from(sheet.getXSpreadSheet(), Position.from(0, 0),
						"Turniersystem: " + TurnierSystem.KO.getBezeichnung())
						.setEndPosMergeSpaltePlus(2)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.LEFT)
						.setCharColor("00599d"));

		// Spaltenbreiten
		ColumnProperties colNr = ColumnProperties.from().setWidth(NR_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		ColumnProperties colName = ColumnProperties.from().setWidth(NAME_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.LEFT).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		ColumnProperties colAktiv = ColumnProperties.from().setWidth(AKTIV_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);

		// Header: Nr
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue.from(sheet.getXSpreadSheet(), Position.from(NR_SPALTE, 0), "Nr")
						.addColumnProperties(colNr)
						.setCellBackColor(headerColor)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().doubleLn().forRight().toBorder())
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCharWeight(FontWeight.BOLD));

		// Header: Teamname
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue.from(sheet.getXSpreadSheet(), Position.from(TEAMNAME_SPALTE, 0), "Teamname")
						.addColumnProperties(colName)
						.setCellBackColor(headerColor)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setCharWeight(FontWeight.BOLD));

		// Header: Aktiv
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue.from(sheet.getXSpreadSheet(), Position.from(AKTIV_SPALTE, 0), "Aktiv")
						.addColumnProperties(colAktiv)
						.setCellBackColor(headerColor)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setHoriJustify(CellHoriJustify.CENTER)
						.setCharWeight(FontWeight.BOLD));
	}

	private void formatDatenSpalten() throws GenerateException {
		sheet.processBoxinfo("K.-O. Meldeliste Daten formatieren");
		XSpreadsheet xSheet = sheet.getXSpreadSheet();

		int letzteZeile = ERSTE_DATEN_ZEILE + MIN_ANZAHL_MELDUNGEN_ZEILEN;

		// Nr-Spalte: doppelte Linie rechts
		RangePosition nrRange = RangePosition.from(NR_SPALTE, ERSTE_DATEN_ZEILE, NR_SPALTE, letzteZeile);
		RangeHelper.from(sheet, nrRange).setRangeProperties(
				RangeProperties.from()
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().doubleLn().forRight().toBorder()));

		// Teamname-Spalte
		RangePosition nameRange = RangePosition.from(TEAMNAME_SPALTE, ERSTE_DATEN_ZEILE, TEAMNAME_SPALTE, letzteZeile);
		RangeHelper.from(sheet, nameRange).setRangeProperties(
				RangeProperties.from().setBorder(BorderFactory.from().allThin().toBorder()));

		// Aktiv-Spalte mit Default-Wert
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			sheet.getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(xSheet, Position.from(AKTIV_SPALTE, zeile))
							.setValue(AKTIV_WERT_NIMMT_TEIL)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setHoriJustify(CellHoriJustify.CENTER));
		}
	}

	private void formatZeilenfarben() throws GenerateException {
		MeldungenHintergrundFarbeGeradeStyle farbeGerade = konfigurationSheet.getMeldeListeHintergrundFarbeGeradeStyle();
		MeldungenHintergrundFarbeUnGeradeStyle farbeUngerade = konfigurationSheet.getMeldeListeHintergrundFarbeUnGeradeStyle();
		int letzteZeile = ERSTE_DATEN_ZEILE + MIN_ANZAHL_MELDUNGEN_ZEILEN;

		RangePosition datenRange = RangePosition.from(NR_SPALTE, ERSTE_DATEN_ZEILE, AKTIV_SPALTE, letzteZeile);
		ConditionalFormatHelper.from(sheet, datenRange).clear().formulaIsEvenRow().style(farbeGerade).applyAndDoReset();
		ConditionalFormatHelper.from(sheet, datenRange).formulaIsOddRow().style(farbeUngerade).applyAndDoReset();
	}

	/** Liefert alle aktiven Teams aus der Meldeliste, sortiert nach Nr. */
	TeamMeldungen getAktiveMeldungen() throws GenerateException {
		sheet.processBoxinfo("K.-O. Meldungen einlesen");
		TeamMeldungen meldungen = new TeamMeldungen();
		XSpreadsheet xSheet = sheet.getXSpreadSheet();

		for (int zeile = ERSTE_DATEN_ZEILE; zeile < ERSTE_DATEN_ZEILE + 9999; zeile++) {
			Integer nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(NR_SPALTE, zeile));
			if (nr == null || nr <= 0) {
				break;
			}
			Integer aktiv = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(AKTIV_SPALTE, zeile));
			if (aktiv != null && aktiv == AKTIV_WERT_NIMMT_TEIL) {
				meldungen.addTeamWennNichtVorhanden(Team.from(nr));
			}
		}
		return meldungen;
	}

	int getErsteDatenZeile() {
		return ERSTE_DATEN_ZEILE;
	}

	int getNrSpalte() {
		return NR_SPALTE;
	}

	int getTeamnameSpalte() {
		return TEAMNAME_SPALTE;
	}

	int getAktivSpalte() {
		return AKTIV_SPALTE;
	}
}
