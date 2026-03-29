/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Delegate für die K.-O. Meldeliste.<br>
 * Spaltenstruktur: Nr | [Teamname] | Spieler-Blöcke (Vorname+Nachname[+Vereinsname]) | RNG | Aktiv<br>
 * 3 Header-Zeilen analog Schweizer Meldeliste.
 */
class KoListeDelegate implements MeldeListeKonstanten {

	/** Minimale Anzahl Datenzeilen (immer vorhanden). */
	static final int MIN_ANZAHL_MELDUNGEN_ZEILEN = 32;

	/** Dritte Header-Zeile (Spalten-Namen: Vorname, Nachname, Verein). */
	static final int DRITTE_HEADER_ZEILE = 2;

	/**
	 * Erste Daten-Zeile: 3 Header-Zeilen (überschreibt MeldeListeKonstanten.ERSTE_DATEN_ZEILE=2).
	 */
	static final int ERSTE_DATEN_ZEILE = 3;

	static final String HEADER_RANGLISTE = "RNG";
	static final String HEADER_AKTIV = "Aktiv";
	static final String HEADER_NR = "Nr";
	static final String HEADER_TEAMNAME = "Teamname";
	static final String HEADER_VORNAME = "Vorname";
	static final String HEADER_NACHNAME = "Nachname";
	static final String HEADER_VEREINSNAME = "Verein";

	static final int AKTIV_WERT_NIMMT_TEIL = 1;
	static final int AKTIV_WERT_AUSGESTIEGEN = 2;

	private static final int NR_SPALTE_WIDTH = 800;
	private static final int NAME_SPALTE_WIDTH = 3000;
	private static final int TEAMNAME_SPALTE_WIDTH = 3000;
	private static final int VEREINSNAME_SPALTE_WIDTH = 2500;
	private static final int RNG_SPALTE_WIDTH = 800;
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

	// ---------------------------------------------------------------
	// Spalten-Berechnung (abhängig von Konfiguration)
	// ---------------------------------------------------------------

	/** Spalte für die Team-Nummer (immer Spalte 0). */
	int getTeamNrSpalte() {
		return SPIELER_NR_SPALTE; // = 0
	}

	/** Spalte für den Teamnamen, oder -1 wenn deaktiviert. */
	int getTeamnameSpalte() throws GenerateException {
		return konfigurationSheet.isMeldeListeTeamnameAnzeigen() ? 1 : -1;
	}

	/** Anzahl Spalten pro Spieler: 2 (Vorname+Nachname) oder 3 (+Vereinsname). */
	int getSpaltenProSpieler() throws GenerateException {
		return konfigurationSheet.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
	}

	/** Index der ersten Spieler-Spalte (Vorname Spieler 1). */
	int getErsterSpielerOffset() throws GenerateException {
		return konfigurationSheet.isMeldeListeTeamnameAnzeigen() ? 2 : 1;
	}

	/** Vorname-Spalte für Spieler spielerIdx (0-basiert). */
	int getVornameSpalte(int spielerIdx) throws GenerateException {
		return getErsterSpielerOffset() + spielerIdx * getSpaltenProSpieler();
	}

	/** Nachname-Spalte für Spieler spielerIdx. */
	int getNachnameSpalte(int spielerIdx) throws GenerateException {
		return getVornameSpalte(spielerIdx) + 1;
	}

	/** Vereinsname-Spalte für Spieler spielerIdx, oder -1 wenn deaktiviert. */
	int getVereinsnameSpalte(int spielerIdx) throws GenerateException {
		if (!konfigurationSheet.isMeldeListeVereinsnameAnzeigen()) {
			return -1;
		}
		return getVornameSpalte(spielerIdx) + 2;
	}

	/** Letzte Spieler-Datenspalte (0-basiert, ohne RNG). */
	int getLetzteDataSpalte() throws GenerateException {
		Formation f = konfigurationSheet.getMeldeListeFormation();
		return getErsterSpielerOffset() + f.getAnzSpieler() * getSpaltenProSpieler() - 1;
	}

	/** RNG-Spalte (Rangliste/Setzreihenfolge) – direkt nach der letzten Spieler-Spalte. */
	int getRanglisteSpalte() throws GenerateException {
		return getLetzteDataSpalte() + 1;
	}

	/** Aktiv/Inaktiv-Spalte – direkt nach der RNG-Spalte. */
	int getAktivSpalte() throws GenerateException {
		return getRanglisteSpalte() + 1;
	}

	// ---------------------------------------------------------------
	// Sheet-Aufbau
	// ---------------------------------------------------------------

	void upDateSheet() throws GenerateException {
		sheet.processBoxinfo("processbox.ko.meldeliste.aktualisieren");
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		TurnierSheet.from(xSheet, sheet.getWorkingSpreadsheet()).setActiv();

		insertHeaderInSheet(konfigurationSheet.getMeldeListeHeaderFarbe());
		formatZeilenfarben();
		formatDatenSpalten();

		SheetFreeze.from(xSheet, sheet.getWorkingSpreadsheet()).anzZeilen(3).doFreeze();

		XSpreadsheet xKonfigSheet = sheet.getSheetHelper().findByName(SheetNamen.koKonfiguration());
		if (xKonfigSheet != null) {
			renderKonfigurationsZuZellen(xKonfigSheet);
		}
	}

	/**
	 * Schreibt alle KO-Konfigurationseigenschaften als 2-spaltige Tabelle in das Konfigurationssheet.
	 */
	private void renderKonfigurationsZuZellen(XSpreadsheet xKonfigSheet) throws GenerateException {
		int headerFarbe = Integer.parseInt(BasePropertiesSpalte.HEADER_BACK_COLOR.replace("#", ""), 16);

		// Kopfzeile
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue.from(xKonfigSheet, Position.from(0, 0), "Eigenschaft")
						.setCharWeight(FontWeight.BOLD)
						.setCellBackColor(headerFarbe));
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue.from(xKonfigSheet, Position.from(1, 0), "Wert")
						.setCharWeight(FontWeight.BOLD)
						.setCellBackColor(headerFarbe));

		List<ConfigProperty<?>> props = de.petanqueturniermanager.ko.konfiguration.KoPropertiesSpalte.KONFIG_PROPERTIES;
		int zeile = 1;
		for (ConfigProperty<?> prop : props) {
			String beschreibung = prop.getDescription() != null ? prop.getDescription() : prop.getKey();
			// Nur erste Zeile der Beschreibung (keine Zeilenumbrüche)
			beschreibung = beschreibung.split("[\r\n]")[0];

			String wert;
			if (prop.getType() == ConfigPropertyType.COLOR) {
				int farbwert = konfigurationSheet.getKoPropertiesSpalte().readIntProperty(prop.getKey());
				wert = String.format("#%06X", farbwert & 0xFFFFFF);
			} else if (prop.getType() == ConfigPropertyType.INTEGER) {
				wert = String.valueOf(konfigurationSheet.getKoPropertiesSpalte().readIntProperty(prop.getKey()));
			} else {
				wert = konfigurationSheet.getKoPropertiesSpalte().readStringProperty(prop.getKey());
			}

			sheet.getSheetHelper().setStringValueInCell(
					StringCellValue.from(xKonfigSheet, Position.from(0, zeile), beschreibung));
			sheet.getSheetHelper().setStringValueInCell(
					StringCellValue.from(xKonfigSheet, Position.from(1, zeile), wert));
			zeile++;
		}
	}

	private void insertHeaderInSheet(int headerColor) throws GenerateException {
		sheet.processBoxinfo("processbox.ko.meldeliste.einfuegen");

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

		// Team-Nr Spalte: über Zeile 1+2 (ZWEITE + DRITTE Header-Zeile) gemergt
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue.from(sheet.getXSpreadSheet(), Position.from(getTeamNrSpalte(), ZWEITE_HEADER_ZEILE),
						HEADER_NR)
						.addColumnProperties(colPropNr)
						.setCellBackColor(headerColor)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().doubleLn().forRight()
								.toBorder())
						.setVertJustify(CellVertJustify2.CENTER)
						.setCharWeight(FontWeight.BOLD)
						.setComment(I18n.get("ko.meldeliste.comment.startnummer"))
						.setEndPosMergeZeilePlus(1));

		// Teamname-Spalte (optional): über Zeile 1+2 gemergt
		if (teamnameAktiv) {
			sheet.getSheetHelper().setStringValueInCell(
					StringCellValue.from(sheet.getXSpreadSheet(), Position.from(1, ZWEITE_HEADER_ZEILE), HEADER_TEAMNAME)
							.addColumnProperties(colPropName.setWidth(TEAMNAME_SPALTE_WIDTH))
							.setCellBackColor(headerColor)
							.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
							.setVertJustify(CellVertJustify2.CENTER)
							.setCharWeight(FontWeight.BOLD)
							.setEndPosMergeZeilePlus(1));
		}

		// Spieler-Blöcke
		for (int s = 0; s < anzSpieler; s++) {
			int vornameSpalte = getVornameSpalte(s);
			String spielerTitel = "Spieler " + (s + 1);

			// Zeile 1 (ZWEITE_HEADER_ZEILE): Block-Titel "Spieler n" über alle Spieler-Spalten
			sheet.getSheetHelper().setStringValueInCell(
					StringCellValue.from(sheet.getXSpreadSheet(), Position.from(vornameSpalte, ZWEITE_HEADER_ZEILE),
							spielerTitel)
							.addColumnProperties(colPropName)
							.setCellBackColor(headerColor)
							.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
							.setVertJustify(CellVertJustify2.CENTER)
							.setHoriJustify(CellHoriJustify.CENTER)
							.setCharWeight(FontWeight.BOLD)
							.setEndPosMergeSpalte(vornameSpalte + spaltenProSpieler - 1));

			// Zeile 2 (DRITTE_HEADER_ZEILE): Vorname
			sheet.getSheetHelper().setStringValueInCell(
					StringCellValue.from(sheet.getXSpreadSheet(), Position.from(vornameSpalte, DRITTE_HEADER_ZEILE),
							HEADER_VORNAME)
							.addColumnProperties(colPropName)
							.setCellBackColor(headerColor)
							.setBorder(BorderFactory.from().allThin().boldLn().forLeft().toBorder())
							.setVertJustify(CellVertJustify2.CENTER));

			// Zeile 2: Nachname
			sheet.getSheetHelper().setStringValueInCell(
					StringCellValue.from(sheet.getXSpreadSheet(), Position.from(getNachnameSpalte(s), DRITTE_HEADER_ZEILE),
							HEADER_NACHNAME)
							.addColumnProperties(colPropName)
							.setCellBackColor(headerColor)
							.setBorder(BorderFactory.from().allThin().toBorder())
							.setVertJustify(CellVertJustify2.CENTER));

			// Zeile 2: Vereinsname (optional)
			if (vereinsnameAktiv) {
				sheet.getSheetHelper().setStringValueInCell(
						StringCellValue.from(sheet.getXSpreadSheet(),
								Position.from(getVereinsnameSpalte(s), DRITTE_HEADER_ZEILE), HEADER_VEREINSNAME)
								.addColumnProperties(colPropName.setWidth(VEREINSNAME_SPALTE_WIDTH))
								.setCellBackColor(headerColor)
								.setBorder(BorderFactory.from().allThin().toBorder())
								.setVertJustify(CellVertJustify2.CENTER));
			}
		}

		// RNG-Spalte (Rangliste): über Zeile 1+2 gemergt
		ColumnProperties colPropRng = ColumnProperties.from().setWidth(RNG_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue.from(sheet.getXSpreadSheet(), Position.from(getRanglisteSpalte(), ZWEITE_HEADER_ZEILE),
						HEADER_RANGLISTE)
						.addColumnProperties(colPropRng)
						.setCellBackColor(headerColor)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
						.setVertJustify(CellVertJustify2.CENTER)
						.setCharWeight(FontWeight.BOLD)
						.setComment(I18n.get("ko.meldeliste.comment.setzreihenfolge"))
						.setRotate90()
						.setEndPosMergeZeilePlus(1));

		// Aktiv-Spalte: über Zeile 1+2 gemergt
		ColumnProperties colPropAktiv = ColumnProperties.from().setWidth(AKTIV_SPALTE_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue.from(sheet.getXSpreadSheet(), Position.from(getAktivSpalte(), ZWEITE_HEADER_ZEILE),
						HEADER_AKTIV)
						.addColumnProperties(colPropAktiv)
						.setCellBackColor(headerColor)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder())
						.setVertJustify(CellVertJustify2.CENTER)
						.setCharWeight(FontWeight.BOLD)
						.setComment(I18n.get("schweizer.meldeliste.comment.aktiv"))
						.setRotate90()
						.setEndPosMergeZeilePlus(1));

		// Zeile 0 (ERSTE_HEADER_ZEILE): System-Info – nach allen Spalten-Properties setzen,
		// damit addColumnProperties() die Ausrichtung dieser Zeile nicht überschreibt
		sheet.getSheetHelper().setStringValueInCell(
				StringCellValue
						.from(sheet.getXSpreadSheet(), Position.from(0, ERSTE_HEADER_ZEILE),
								I18n.get("meldeliste.header.turniersystem", TurnierSystem.KO.getBezeichnung()))
						.setEndPosMergeSpaltePlus(2)
						.setCharWeight(FontWeight.BOLD)
						.setHoriJustify(CellHoriJustify.LEFT)
						.setVertJustify(CellVertJustify2.TOP)
						.setShrinkToFit(true)
						.setCharColor("00599d"));
	}

	private int getLetzteDatenZeileUseMin() throws GenerateException {
		int minZeile = ERSTE_DATEN_ZEILE + MIN_ANZAHL_MELDUNGEN_ZEILEN - 1;
		int actualZeile = letzteZeileMitDaten(sheet.getXSpreadSheet()) + 10;
		return Math.max(minZeile, actualZeile);
	}

	private void formatDatenSpalten() throws GenerateException {
		sheet.processBoxinfo("processbox.ko.meldeliste.formatieren");
		Formation formation = konfigurationSheet.getMeldeListeFormation();
		int anzSpieler = formation.getAnzSpieler();
		int letzteDatenZeile = getLetzteDatenZeileUseMin();

		// Zeilenfarben hier nochmals holen, damit sie nach den Fehler-Bedingungen angehängt
		// werden können (Priorität: Fehler > Zeilenfarbe).
		MeldungenHintergrundFarbeGeradeStyle farbeGerade = konfigurationSheet.getMeldeListeHintergrundFarbeGeradeStyle();
		MeldungenHintergrundFarbeUnGeradeStyle farbeUngerade = konfigurationSheet
				.getMeldeListeHintergrundFarbeUnGeradeStyle();

		// Nr-Spalte: doppelte Linie rechts
		RangePosition nrRange = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
				getTeamNrSpalte(), letzteDatenZeile);
		RangeHelper.from(sheet, nrRange).setRangeProperties(
				RangeProperties.from().setBorder(
						BorderFactory.from().allThin().boldLn().forTop().forLeft().doubleLn().forRight().toBorder()));

		// Bedingte Formatierung Nr-Spalte: Text=rot, Duplikat=rot, außerhalb [1,999]=rot,
		// danach Zeilenfarbe (niedrigste Priorität)
		String kondDoppeltNr = "COUNTIF(" + Position.from(getTeamNrSpalte(), 0).getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		ConditionalFormatHelper.from(sheet, nrRange).clear()
				.formulaIsText().styleIsFehler().applyAndDoReset()
				.formula1(kondDoppeltNr).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset()
				.formula1("0").formula2("" + MeldungenSpalte.MAX_ANZ_MELDUNGEN)
				.operator(ConditionOperator.NOT_BETWEEN).styleIsFehler().applyAndDoReset()
				.formulaIsEvenRow().style(farbeGerade).applyAndDoReset()
				.formulaIsOddRow().style(farbeUngerade).applyAndDoReset();

		// Teamname-Spalte (optional)
		if (konfigurationSheet.isMeldeListeTeamnameAnzeigen()) {
			RangePosition teamnameRange = RangePosition.from(1, ERSTE_DATEN_ZEILE, 1, letzteDatenZeile);
			RangeHelper.from(sheet, teamnameRange).setRangeProperties(
					RangeProperties.from().setBorder(
							BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
		}

		// Spieler-Spalten (Vorname + Nachname [+ Vereinsname])
		for (int s = 0; s < anzSpieler; s++) {
			int ersteSpielerSpalte = getVornameSpalte(s);
			int letzteSpielerSpalte = konfigurationSheet.isMeldeListeVereinsnameAnzeigen()
					? getVereinsnameSpalte(s)
					: getNachnameSpalte(s);
			RangePosition spielerRange = RangePosition.from(ersteSpielerSpalte, ERSTE_DATEN_ZEILE,
					letzteSpielerSpalte, letzteDatenZeile);
			RangeHelper.from(sheet, spielerRange).setRangeProperties(
					RangeProperties.from().setBorder(
							BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
		}

		// RNG-Spalte
		RangePosition rngRange = RangePosition.from(getRanglisteSpalte(), ERSTE_DATEN_ZEILE,
				getRanglisteSpalte(), letzteDatenZeile);
		RangeHelper.from(sheet, rngRange).setRangeProperties(
				RangeProperties.from().setBorder(
						BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));

		// Bedingte Formatierung RNG-Spalte (Pflichtfeld):
		// - Text-Wert → rot
		// - Zeile hat Spielernamen aber RNG ist leer (≤ 0) → rot
		// - Duplikat (Wert > 0, aber mehrfach vorhanden) → rot
		int vornameSpalteNr = getVornameSpalte(0) + 1; // ADDRESS ist 1-basiert
		String vornameRef = "INDIRECT(ADDRESS(ROW();" + vornameSpalteNr + "))";
		String kondRngLeer = "AND(" + vornameRef + "<>\"\";" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<=0)";
		String kondDoppeltRng = "AND(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ">0;COUNTIF("
				+ Position.from(getRanglisteSpalte(), 0).getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1)";
		ConditionalFormatHelper.from(sheet, rngRange).clear()
				.formulaIsText().styleIsFehler().applyAndDoReset()
				.formula1(kondRngLeer).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset()
				.formula1(kondDoppeltRng).operator(ConditionOperator.FORMULA).styleIsFehler().applyAndDoReset()
				.formulaIsEvenRow().style(farbeGerade).applyAndDoReset()
				.formulaIsOddRow().style(farbeUngerade).applyAndDoReset();

		// Aktiv-Spalte: nur Formatierung (kein Default-Wert – wird nur bei neuen Sheets gesetzt)
		RangePosition aktivRange = RangePosition.from(getAktivSpalte(), ERSTE_DATEN_ZEILE,
				getAktivSpalte(), letzteDatenZeile);
		RangeHelper.from(sheet, aktivRange).setRangeProperties(
				RangeProperties.from().centerJustify()
						.setBorder(BorderFactory.from().allThin().toBorder()));

		// Bedingte Formatierung Aktiv-Spalte: rot wenn nicht leer AND nicht 1 AND nicht 2
		String kondAktivUngueltig = "AND(NOT(ISBLANK(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + "));"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>1;"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>2)";
		ConditionalFormatHelper.from(sheet, aktivRange).clear()
				.formula1(kondAktivUngueltig).operator(ConditionOperator.FORMULA)
				.styleIsFehler().applyAndDoReset()
				.formulaIsEvenRow().style(farbeGerade).applyAndDoReset()
				.formulaIsOddRow().style(farbeUngerade).applyAndDoReset();
	}

	private void formatZeilenfarben() throws GenerateException {
		MeldungenHintergrundFarbeGeradeStyle farbeGerade = konfigurationSheet.getMeldeListeHintergrundFarbeGeradeStyle();
		MeldungenHintergrundFarbeUnGeradeStyle farbeUngerade = konfigurationSheet
				.getMeldeListeHintergrundFarbeUnGeradeStyle();
		int letzteDatenZeile = getLetzteDatenZeileUseMin();

		RangePosition datenRange = RangePosition.from(getTeamNrSpalte(), ERSTE_DATEN_ZEILE,
				getAktivSpalte(), letzteDatenZeile);
		ConditionalFormatHelper.from(sheet, datenRange).clear().formulaIsEvenRow().style(farbeGerade).applyAndDoReset();
		ConditionalFormatHelper.from(sheet, datenRange).formulaIsOddRow().style(farbeUngerade).applyAndDoReset();
	}

	// ---------------------------------------------------------------
	// Daten einlesen
	// ---------------------------------------------------------------

	/** Liefert alle aktiven Teams aus der Meldeliste, sortiert nach Nr. */
	TeamMeldungen getAktiveMeldungen() throws GenerateException {
		sheet.processBoxinfo("processbox.ko.meldeliste.einlesen");
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int vornameSpalte = getVornameSpalte(0);
		int aktivSpalte = getAktivSpalte();
		int letzteZeile = letzteZeileMitDaten(xSheet);

		TeamMeldungen meldungen = new TeamMeldungen();
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				continue;
			}
			int aktiv = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(aktivSpalte, zeile));
			if (aktiv == AKTIV_WERT_NIMMT_TEIL) {
				meldungen.addTeamWennNichtVorhanden(Team.from(nr));
			}
		}
		return meldungen;
	}

	/**
	 * Liefert alle aktiven Teams, aufsteigend nach Rang (RNG) sortiert.<br>
	 * RNG ist Pflichtfeld – vor dem Aufruf muss {@link #validiereRangSpalte} geprüft werden.
	 */
	TeamMeldungen getMeldungenSortiertNachRangliste() throws GenerateException {
		sheet.processBoxinfo("processbox.ko.meldeliste.sortieren");
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int vornameSpalte = getVornameSpalte(0);
		int rngSpalte = getRanglisteSpalte();
		int aktivSpalte = getAktivSpalte();
		int letzteZeile = letzteZeileMitDaten(xSheet);

		record TeamZeile(int nr, int rng) {}
		List<TeamZeile> alleTeams = new ArrayList<>();

		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				continue;
			}
			int aktiv = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(aktivSpalte, zeile));
			if (aktiv != AKTIV_WERT_NIMMT_TEIL) {
				continue;
			}
			int rng = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(rngSpalte, zeile));
			alleTeams.add(new TeamZeile(nr, rng));
		}

		alleTeams.sort(Comparator.comparingInt(TeamZeile::rng));

		TeamMeldungen meldungen = new TeamMeldungen();
		for (TeamZeile t : alleTeams) {
			meldungen.addTeamWennNichtVorhanden(Team.from(t.nr()));
		}
		return meldungen;
	}

	/**
	 * Prüft ob alle aktiven Teams einen gültigen, eindeutigen Rang haben.
	 *
	 * @return null wenn OK, sonst eine deutsche Fehlermeldung.
	 */
	String validiereRangSpalte() throws GenerateException {
		XSpreadsheet xSheet = sheet.getXSpreadSheet();
		int vornameSpalte = getVornameSpalte(0);
		int rngSpalte = getRanglisteSpalte();
		int aktivSpalte = getAktivSpalte();
		int letzteZeile = letzteZeileMitDaten(xSheet);

		java.util.Set<Integer> bereitsVergeben = new java.util.HashSet<>();
		int anzOhneRang = 0;
		int anzDuplikat = 0;

		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int aktiv = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(aktivSpalte, zeile));
			if (aktiv != AKTIV_WERT_NIMMT_TEIL) {
				continue;
			}
			int rng = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(rngSpalte, zeile));
			if (rng <= 0) {
				anzOhneRang++;
			} else if (!bereitsVergeben.add(rng)) {
				anzDuplikat++;
			}
		}

		if (anzOhneRang > 0) {
			return "Die Rang-Spalte ist nicht vollständig befüllt (" + anzOhneRang
					+ " Team(s) ohne Rang).\nBitte alle aktiven Teams mit einer eindeutigen Rangnummer versehen.";
		}
		if (anzDuplikat > 0) {
			return "Die Rang-Spalte enthält " + anzDuplikat
					+ " doppelte Rangnummer(n).\nBitte eindeutige Rangnummern vergeben.";
		}
		return null;
	}

	/**
	 * Letzte Zeile mit einem nicht-leeren Vorname (Spieler 1) ab ERSTE_DATEN_ZEILE.
	 * Gibt ERSTE_DATEN_ZEILE - 1 zurück wenn keine Daten vorhanden.
	 */
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

	// ---------------------------------------------------------------
	// Feld-Bereinigung und -Prüfung
	// ---------------------------------------------------------------

	/**
	 * Trimmt alle Textspalten (Teamname, Vorname, Nachname, Vereinsname) von Steuerzeichen
	 * und führenden/abschließenden Leerzeichen.
	 */
	void stringsBesinigen(XSpreadsheet xSheet) throws GenerateException {
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

	/**
	 * Prüft auf doppelte Team-Nummern.
	 * Wirft {@link GenerateException} mit Fehlermeldung wenn Duplikate gefunden werden.
	 */
	void pruefeAufDoppelteTeamNr(XSpreadsheet xSheet) throws GenerateException {
		int letzteZeile = letzteZeileMitDaten(xSheet);
		if (letzteZeile < ERSTE_DATEN_ZEILE) {
			return;
		}
		int vornameSpalte = getVornameSpalte(0);
		Map<Integer, List<Integer>> alleNrn = new LinkedHashMap<>();
		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String vorname = sheet.getSheetHelper().getTextFromCell(xSheet, Position.from(vornameSpalte, zeile));
			if (vorname == null || vorname.isEmpty()) {
				continue;
			}
			int nr = sheet.getSheetHelper().getIntFromCell(xSheet, Position.from(getTeamNrSpalte(), zeile));
			if (nr <= 0) {
				continue;
			}
			alleNrn.computeIfAbsent(nr, k -> new ArrayList<>()).add(zeile);
		}
		Map<Integer, List<Integer>> duplikate = new LinkedHashMap<>();
		for (Map.Entry<Integer, List<Integer>> entry : alleNrn.entrySet()) {
			if (entry.getValue().size() > 1) {
				duplikate.put(entry.getKey(), entry.getValue());
			}
		}
		if (duplikate.isEmpty()) {
			return;
		}
		StringBuilder sb = new StringBuilder("Meldeliste wurde nicht aktualisiert.\nDoppelte Startnummern:");
		for (Map.Entry<Integer, List<Integer>> entry : duplikate.entrySet()) {
			sb.append("\nNr. ").append(entry.getKey()).append(": Zeilen ");
			List<Integer> zeilen = entry.getValue();
			for (int i = 0; i < zeilen.size(); i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(zeilen.get(i) + 1); // 0-basiert → 1-basiert für Benutzer
			}
		}
		throw new GenerateException(sb.toString());
	}

	// ---------------------------------------------------------------
	// Weiterleitungs-Methoden (Rückwärtskompatibilität)
	// ---------------------------------------------------------------

	int getErsteDatenZeile() {
		return ERSTE_DATEN_ZEILE;
	}

	int getNrSpalte() {
		return getTeamNrSpalte();
	}
}
