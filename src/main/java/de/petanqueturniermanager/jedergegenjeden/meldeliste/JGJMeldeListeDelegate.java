/*
 * Erstellung : 15.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

class JGJMeldeListeDelegate implements MeldeListeKonstanten {

	static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 32;

	static final int DRITTE_HEADER_ZEILE = 2;
	static final int ERSTE_DATEN_ZEILE = 3;

	static final int NR_SPALTE_WIDTH = 800;
	static final int NAME_SPALTE_WIDTH = 3000;
	static final int TEAMNAME_SPALTE_WIDTH = 3000;
	static final int VEREINSNAME_SPALTE_WIDTH = 2500;
	static final int AKTIV_SPALTE_WIDTH = 700;

	static final int AKTIV_WERT_NIMMT_TEIL = 1;

	private final ISheet sheet;
	private final JGJKonfigurationSheet konfigurationSheet;
	private final TurnierSystem turnierSystem;

	JGJMeldeListeDelegate(ISheet sheet, WorkingSpreadsheet ws, TurnierSystem turnierSystem) {
		this.sheet = sheet;
		this.turnierSystem = turnierSystem;
		konfigurationSheet = new JGJKonfigurationSheet(ws);
	}

	JGJKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	// ---------------------------------------------------------------
	// Spalten-Berechnung (abhängig von Konfiguration)
	// ---------------------------------------------------------------

	int getTeamNrSpalte() {
		return SPIELER_NR_SPALTE;
	}

	int getTeamnameSpalte() {
		return konfigurationSheet.isMeldeListeTeamnameAnzeigen() ? 1 : -1;
	}

	int getSpaltenProSpieler() {
		return konfigurationSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
	}

	int getErsterSpielerOffset() {
		return konfigurationSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
	}

	int getSpielerNameErsteSpalte() {
		return getErsterSpielerOffset();
	}

	int getVornameSpalte(int spielerIdx) {
		return getErsterSpielerOffset() + spielerIdx * getSpaltenProSpieler();
	}

	int getNachnameSpalte(int spielerIdx) {
		return getVornameSpalte(spielerIdx) + 1;
	}

	int getVereinsnameSpalte(int spielerIdx) {
		if (!konfigurationSheet.isMeldeListeVereinsnameAnzeigen()) {
			return -1;
		}
		return getVornameSpalte(spielerIdx) + 2;
	}

	int getLetzteDataSpalte() {
		Formation f = konfigurationSheet.getMeldeListeFormation();
		return getErsterSpielerOffset() + f.getAnzSpieler() * getSpaltenProSpieler() - 1;
	}

	int getSetzPositionSpalte() {
		return getLetzteDataSpalte() + 1;
	}

	int getAktivSpalte() {
		return getSetzPositionSpalte() + 1;
	}

	/** Erste Spieltag-Spalte = direkt nach der Aktiv-Spalte. */
	int letzteSpielTagSpalte() {
		return getAktivSpalte() + 1;
	}

	// ---------------------------------------------------------------
	// Sheet-Aufbau
	// ---------------------------------------------------------------

	void upDateSheet() throws GenerateException {
		sheet.processBoxinfo("processbox.meldeliste.spalten.formatieren");

		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		TurnierSheet.from(xSheet, sheet.getWorkingSpreadsheet()).setActiv();

		insertHeaderInSheet(konfigurationSheet.getMeldeListeHeaderFarbe());
		formatDatenSpalten();
		formatZeilenfarben();

		SheetFreeze.from(xSheet, sheet.getWorkingSpreadsheet()).anzZeilen(3).doFreeze();
	}

	void insertHeaderInSheet(int headerColor) throws GenerateException {
		sheet.processBoxinfo("processbox.meldeliste.sortieren");

		sheet.getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet.getXSpreadSheet(), Position.from(0, ERSTE_HEADER_ZEILE),
						I18n.get("meldeliste.header.turniersystem", turnierSystem.getBezeichnung()))
				.setEndPosMergeSpaltePlus(2).setCharWeight(FontWeight.BOLD)
				.setHoriJustify(CellHoriJustify.LEFT).setVertJustify(CellVertJustify2.TOP)
				.setShrinkToFit(true).setCharColor("00599d"));

		Formation formation = konfigurationSheet.getMeldeListeFormation();
		int anzSpieler = formation.getAnzSpieler();
		int spaltenProSpieler = getSpaltenProSpieler();
		boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAktiv = konfigurationSheet.isMeldeListeVereinsnameAnzeigen();

		ColumnProperties colPropNr = ColumnProperties.from().setWidth(NR_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		ColumnProperties colPropName = ColumnProperties.from().setWidth(NAME_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.LEFT).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);

		sheet.getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet.getXSpreadSheet(), Position.from(getTeamNrSpalte(), ZWEITE_HEADER_ZEILE),
						I18n.get("column.header.nr"))
				.addColumnProperties(colPropNr)
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().doubleLn().forRight().toBorder())
				.setVertJustify(CellVertJustify2.CENTER)
				.setEndPosMergeZeilePlus(1));

		if (teamnameAktiv) {
			sheet.getSheetHelper().setStringValueInCell(StringCellValue
					.from(sheet.getXSpreadSheet(), Position.from(1, ZWEITE_HEADER_ZEILE),
							I18n.get("column.header.teamname"))
					.addColumnProperties(colPropName.setWidth(TEAMNAME_SPALTE_WIDTH))
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
					.setVertJustify(CellVertJustify2.CENTER)
					.setEndPosMergeZeilePlus(1));
		}

		ColumnProperties colPropSP = ColumnProperties.from().setWidth(NR_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		sheet.getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet.getXSpreadSheet(), Position.from(getSetzPositionSpalte(), ZWEITE_HEADER_ZEILE),
						I18n.get("column.header.setzposition"))
				.addColumnProperties(colPropSP)
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
				.setVertJustify(CellVertJustify2.CENTER)
				.setEndPosMergeZeilePlus(1)
				.setRotate90());

		ColumnProperties colPropAktiv = ColumnProperties.from().setWidth(AKTIV_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		sheet.getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet.getXSpreadSheet(), Position.from(getAktivSpalte(), ZWEITE_HEADER_ZEILE),
						I18n.get("column.header.aktiv"))
				.addColumnProperties(colPropAktiv)
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
				.setVertJustify(CellVertJustify2.CENTER)
				.setEndPosMergeZeilePlus(1)
				.setRotate90());

		for (int s = 0; s < anzSpieler; s++) {
			int vornameSpalte = getVornameSpalte(s);
			String spielerTitel = I18n.get("schweizer.meldeliste.header.spieler", s + 1);

			sheet.getSheetHelper().setStringValueInCell(StringCellValue
					.from(sheet.getXSpreadSheet(), Position.from(vornameSpalte, ZWEITE_HEADER_ZEILE), spielerTitel)
					.addColumnProperties(colPropName)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
					.setVertJustify(CellVertJustify2.CENTER)
					.setHoriJustify(CellHoriJustify.CENTER)
					.setEndPosMergeSpalte(vornameSpalte + spaltenProSpieler - 1));

			sheet.getSheetHelper().setStringValueInCell(StringCellValue
					.from(sheet.getXSpreadSheet(), Position.from(vornameSpalte, DRITTE_HEADER_ZEILE),
							I18n.get("column.header.vorname"))
					.addColumnProperties(colPropName)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().boldLn().forLeft().toBorder())
					.setVertJustify(CellVertJustify2.CENTER));

			sheet.getSheetHelper().setStringValueInCell(StringCellValue
					.from(sheet.getXSpreadSheet(), Position.from(getNachnameSpalte(s), DRITTE_HEADER_ZEILE),
							I18n.get("column.header.nachname"))
					.addColumnProperties(colPropName)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().toBorder())
					.setVertJustify(CellVertJustify2.CENTER));

			if (vereinsnameAktiv) {
				sheet.getSheetHelper().setStringValueInCell(StringCellValue
						.from(sheet.getXSpreadSheet(), Position.from(getVereinsnameSpalte(s), DRITTE_HEADER_ZEILE),
								I18n.get("column.header.vereinsname"))
						.addColumnProperties(colPropName.setWidth(VEREINSNAME_SPALTE_WIDTH))
						.setCellBackColor(headerColor)
						.setBorder(BorderFactory.from().allThin().toBorder())
						.setVertJustify(CellVertJustify2.CENTER));
			}
		}
	}

	void formatDatenSpalten() throws GenerateException {
		Formation formation = konfigurationSheet.getMeldeListeFormation();
		int anzSpieler = formation.getAnzSpieler();
		int letzteDatenZeile = getLetzteDatenZeileUseMin();

		RangePosition nrRange = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
				getTeamNrSpalte(), letzteDatenZeile);
		sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), nrRange,
				CellProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().doubleLn().forRight().toBorder()));

		String kondDoppeltNr = "COUNTIF(" + Position.from(getTeamNrSpalte(), 0).getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		ConditionalFormatHelper.from(sheet, nrRange).clear()
				.formulaIsText().styleIsFehler().applyAndDoReset()
				.formula1(kondDoppeltNr).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset()
				.formula1("0").formula2("" + MeldungenSpalte.MAX_ANZ_MELDUNGEN)
				.operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset();

		if (konfigurationSheet.isMeldeListeTeamnameAnzeigen()) {
			RangePosition teamnameRange = RangePosition.from(1, ERSTE_DATEN_ZEILE, 1, letzteDatenZeile);
			sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), teamnameRange,
					CellProperties.from().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()).setShrinkToFit(true));
		}

		for (int s = 0; s < anzSpieler; s++) {
			int ersteSpielSpalte = getVornameSpalte(s);
			int letzteSpielSpalte = konfigurationSheet.isMeldeListeVereinsnameAnzeigen()
					? getVereinsnameSpalte(s)
					: getNachnameSpalte(s);
			RangePosition spielerRange = RangePosition.from(ersteSpielSpalte, ERSTE_DATEN_ZEILE,
					letzteSpielSpalte, letzteDatenZeile);
			sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), spielerRange,
					CellProperties.from().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()).setShrinkToFit(true));
		}

		RangePosition spRange = RangePosition.from(getSetzPositionSpalte(), ERSTE_DATEN_ZEILE,
				getSetzPositionSpalte(), letzteDatenZeile);
		sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), spRange,
				CellProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));

		RangePosition aktivRange = RangePosition.from(getAktivSpalte(), ERSTE_DATEN_ZEILE,
				getAktivSpalte(), letzteDatenZeile);
		sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), aktivRange,
				CellProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));

		RangePosition editierbareRange = RangePosition.from(1, ERSTE_DATEN_ZEILE, getAktivSpalte(), letzteDatenZeile);
		EditierbaresZelleFormatHelper.anwenden(sheet, editierbareRange);
	}

	void formatZeilenfarben() throws GenerateException {
		Integer geradeColor = konfigurationSheet.getMeldeListeHintergrundFarbeGerade();
		Integer ungeradeColor = konfigurationSheet.getMeldeListeHintergrundFarbeUnGerade();

		int letzteDatenZeile = getLetzteDatenZeileUseMin();
		int letzteSpalte = getAktivSpalte();

		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteDatenZeile; zeile++) {
			RangePosition zeileRange = RangePosition.from(getTeamNrSpalte(), zeile, letzteSpalte, zeile);
			Integer color = ((zeile - ERSTE_DATEN_ZEILE) % 2 == 0) ? geradeColor : ungeradeColor;
			sheet.getSheetHelper().setPropertiesInRange(sheet.getXSpreadSheet(), zeileRange,
					CellProperties.from().setCellBackColor(color));
		}
	}

	// ---------------------------------------------------------------
	// Meldungen lesen
	// ---------------------------------------------------------------

	TeamMeldungen getAlleMeldungen() throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int letzteZeile = letzteZeileMitDaten(xSheet);
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(getVornameSpalte(0), zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				continue;
			}
			int setzPos = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getSetzPositionSpalte(), zeile));
			meldungen.addTeamWennNichtVorhanden(Team.from(nr).setSetzPos(setzPos));
		}
		return meldungen;
	}

	TeamMeldungen getAktiveMeldungen() throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int letzteZeile = letzteZeileMitDaten(xSheet);
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(getVornameSpalte(0), zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				continue;
			}
			int aktivWert = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getAktivSpalte(), zeile));
			if (aktivWert == AKTIV_WERT_NIMMT_TEIL) {
				int setzPos = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getSetzPositionSpalte(), zeile));
				meldungen.addTeamWennNichtVorhanden(Team.from(nr).setSetzPos(setzPos));
			}
		}
		return meldungen;
	}

	// ---------------------------------------------------------------
	// Formeln
	// ---------------------------------------------------------------

	String formulaSverweisSpielernamen(String spielrNrAdresse) {
		String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
		String letzteZelleAddress = Position.from(getErsterSpielerOffset(), 999).getAddressWith$();
		return "VLOOKUP(" + spielrNrAdresse + ";$'" + SheetNamen.meldeliste() + "'." + ersteZelleAddress + ":"
				+ letzteZelleAddress + ";2;0)";
	}

	String formulaSpielplanTeamName(String nrAdresse) {
		if (konfigurationSheet.getSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NR) {
			return nrAdresse;
		}
		return formulaSverweisSpielernamen(nrAdresse);
	}

	Map<Integer, String> leseTeamNamen() throws GenerateException {
		Map<Integer, String> result = new HashMap<>();
		XSpreadsheet mlSheet = sheet.getXSpreadSheet();
		if (mlSheet == null) {
			return result;
		}
		boolean zeigeTeamname = konfigurationSheet.isMeldeListeTeamnameAnzeigen();
		boolean zeigeVerein = konfigurationSheet.isMeldeListeVereinsnameAnzeigen();
		Formation formation = konfigurationSheet.getMeldeListeFormation();
		int anzSpieler = formation.getAnzSpieler();
		int ersterSpielerOffset = zeigeTeamname ? 2 : 1;
		int spaltenProSpieler = zeigeVerein ? 3 : 2;
		int maxSpalte = ersterSpielerOffset + anzSpieler * spaltenProSpieler - 1;

		var xDoc = sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		RangeData data = RangeHelper.from(mlSheet, xDoc,
				RangePosition.from(0, ERSTE_DATEN_ZEILE, maxSpalte, ERSTE_DATEN_ZEILE + 999)).getDataFromRange();

		for (RowData row : data) {
			if (row.isEmpty()) {
				break;
			}
			int nr = row.get(0).getIntVal(0);
			if (nr <= 0) {
				break;
			}
			String name = zeigeTeamname
					? (row.size() > 1 ? row.get(1).getStringVal() : "")
					: bauspielerNamenZusammen(row, anzSpieler, ersterSpielerOffset, spaltenProSpieler);
			result.put(nr, name != null ? name : "");
		}
		return result;
	}

	private String bauspielerNamenZusammen(RowData row, int anzSpieler, int ersterSpielerOffset, int spaltenProSpieler) {
		var sb = new StringBuilder();
		for (int s = 0; s < anzSpieler; s++) {
			int vorSpalte = ersterSpielerOffset + s * spaltenProSpieler;
			int nachSpalte = vorSpalte + 1;
			String vorname = vorSpalte < row.size() ? row.get(vorSpalte).getStringVal() : "";
			String nachname = nachSpalte < row.size() ? row.get(nachSpalte).getStringVal() : "";
			String spielerName = baueSpielerName(vorname, nachname);
			if (!spielerName.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(" / ");
				}
				sb.append(spielerName);
			}
		}
		return sb.toString();
	}

	private static String baueSpielerName(String vorname, String nachname) {
		String vn = vorname != null ? vorname.trim() : "";
		String nn = nachname != null ? nachname.trim() : "";
		if (vn.isEmpty() && nn.isEmpty()) {
			return "";
		}
		if (vn.isEmpty()) {
			return nn;
		}
		if (nn.isEmpty()) {
			return vn;
		}
		return vn + " " + nn;
	}

	// ---------------------------------------------------------------
	// Zeilen-Suche
	// ---------------------------------------------------------------

	int letzteZeileMitDaten(XSpreadsheet xSheet) throws GenerateException {
		int vornameSpalte = getVornameSpalte(0);
		int letzte = ERSTE_DATEN_ZEILE - 1;
		int maxZeile = ERSTE_DATEN_ZEILE + 500;
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= maxZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname != null && !vorname.isEmpty()) {
				letzte = zeile;
			}
		}
		return letzte;
	}

	int getLetzteDatenZeileUseMin() throws GenerateException {
		int minZeile = ERSTE_DATEN_ZEILE + MIN_ANZAHL_MELDUNGEN_ZEILEN - 1;
		int actualZeile = letzteZeileMitDaten(sheet.getXSpreadSheet()) + 10;
		return Math.max(minZeile, actualZeile);
	}

	int getErsteDatenZiele() {
		return ERSTE_DATEN_ZEILE;
	}

	int letzteZeileMitSpielerName() throws GenerateException {
		return letzteZeileMitDaten(sheet.getXSpreadSheet());
	}

	int getSpielerZeileNr(int spielerNr) throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int letzteZeile = letzteZeileMitDaten(xSheet);
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr == spielerNr) {
				return zeile;
			}
		}
		return -1;
	}

	int naechsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int maxZeile = ERSTE_DATEN_ZEILE + 500;
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= maxZeile; zeile++) {
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				return zeile;
			}
		}
		return maxZeile + 1;
	}

	int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int letzte = ERSTE_DATEN_ZEILE - 1;
		int maxZeile = ERSTE_DATEN_ZEILE + 500;
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= maxZeile; zeile++) {
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr > 0) {
				letzte = zeile;
			}
		}
		return letzte;
	}

	List<String> getSpielerNamenList() throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int letzteZeile = letzteZeileMitDaten(xSheet);
		List<String> namen = new ArrayList<>();
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(getVornameSpalte(0), zeile));
			if (vorname != null && !vorname.isEmpty()) {
				namen.add(vorname);
			}
		}
		return namen;
	}

	List<Integer> getSpielerNrList() throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int letzteZeile = letzteZeileMitDaten(xSheet);
		List<Integer> nrList = new ArrayList<>();
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr > 0) {
				nrList.add(nr);
			}
		}
		return nrList;
	}

	// ---------------------------------------------------------------
	// Sortieren
	// ---------------------------------------------------------------

	void doSort(int spalteNr, boolean aufsteigend) throws GenerateException {
		int letzteZeile = getLetzteDatenZeileUseMin();
		RangePosition rangeToSort = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
				getAktivSpalte(), letzteZeile);
		SortHelper.from(sheet, rangeToSort).spalteToSort(spalteNr).aufSteigendSortieren(aufsteigend).doSort();
	}

	// ---------------------------------------------------------------
	// Vollständige Aktualisierung (wie Schweizer-Muster)
	// ---------------------------------------------------------------

	void vollstaendigAktualisieren() throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		stringsBesinigen(xSheet);
		teamnummernVergeben(xSheet);
		nachTeamNrSortieren(xSheet);
		upDateSheet();
	}

	private void stringsBesinigen(XSpreadsheet xSheet) throws GenerateException {
		Formation formation = konfigurationSheet.getMeldeListeFormation();
		boolean teamnameAktiv = konfigurationSheet.isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAktiv = konfigurationSheet.isMeldeListeVereinsnameAnzeigen();
		int letzteZeile = letzteZeileMitDaten(xSheet) + MIN_ANZAHL_MELDUNGEN_ZEILEN;
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			if (teamnameAktiv) {
				bereinigeSpalte(xSheet, getTeamnameSpalte(), zeile);
			}
			for (int s = 0; s < formation.getAnzSpieler(); s++) {
				bereinigeSpalte(xSheet, getVornameSpalte(s), zeile);
				bereinigeSpalte(xSheet, getNachnameSpalte(s), zeile);
				if (vereinsnameAktiv) {
					bereinigeSpalte(xSheet, getVereinsnameSpalte(s), zeile);
				}
			}
		}
	}

	private void bereinigeSpalte(XSpreadsheet xSheet, int spalte, int zeile) throws GenerateException {
		if (spalte < 0) {
			return;
		}
		String original = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(spalte, zeile));
		if (original == null || original.isEmpty()) {
			return;
		}
		String bereinigt = original.replaceAll("[\\p{Cntrl}]", "").strip();
		if (!bereinigt.equals(original)) {
			sheet.getSheetHelper().setStringValueInCell(StringCellValue.from(xSheet, Position.from(spalte, zeile), bereinigt));
		}
	}

	private void teamnummernVergeben(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = letzteZeileMitDaten(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		int vornameSpalte = getVornameSpalte(0);
		RangePosition range = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
				getSetzPositionSpalte(), letzteZeile);
		SortHelper.from(sheet, range).spalteToSort(getTeamNrSpalte()).abSteigendSortieren().doSort();
		int letztNr = Math.max(0,
				sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE)));
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				letztNr++;
				sheet.getSheetHelper().setNumberValueInCell(
						NumberCellValue.from(xSheet, Position.from(getTeamNrSpalte(), zeile)).setValue(letztNr));
			}
		}
	}

	private void nachTeamNrSortieren(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = letzteZeileMitDaten(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		RangePosition range = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
				getSetzPositionSpalte(), letzteZeile);
		SortHelper.from(sheet, range).spalteToSort(getTeamNrSpalte()).aufSteigendSortieren(true).doSort();
	}

	// ---------------------------------------------------------------
	// MeldungenSpalte-Kompatibilitäts-Stub (für IMeldeliste-Interface)
	// ---------------------------------------------------------------

	MeldungenSpalte<TeamMeldungen, Team> getMeldungenSpalte() {
		return MeldungenSpalte.<TeamMeldungen, Team>builder()
				.spalteMeldungNameWidth(NAME_SPALTE_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE)
				.spielerNrSpalte(SPIELER_NR_SPALTE)
				.ersteMeldungNameSpalteOffset(getErsterSpielerOffset())
				.sheet(sheet)
				.minAnzZeilen(MIN_ANZAHL_MELDUNGEN_ZEILEN)
				.formation(konfigurationSheet.getMeldeListeFormation())
				.build();
	}

	// ---------------------------------------------------------------
	// Aktiv-Spalte setzen
	// ---------------------------------------------------------------

	void alleTeamsAktivieren() throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int letzteZeile = letzteZeileMitDaten(xSheet);
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(getVornameSpalte(0), zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				continue;
			}
			sheet.getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(xSheet, Position.from(getAktivSpalte(), zeile)).setValue(AKTIV_WERT_NIMMT_TEIL));
		}
	}

}
