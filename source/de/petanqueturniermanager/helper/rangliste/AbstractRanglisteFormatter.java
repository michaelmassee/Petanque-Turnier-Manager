/**
* Erstellung : 10.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.PUNKTE_DIV_OFFS;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.PUNKTE_MINUS_OFFS;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.PUNKTE_PLUS_OFFS;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.SPIELE_DIV_OFFS;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.SPIELE_MINUS_OFFS;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.SPIELE_PLUS_OFFS;

import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.supermelee.konfiguration.ISuperMeleePropertiesSpalte;

abstract public class AbstractRanglisteFormatter {

	private static final int ENDSUMME_NUMBER_WIDTH = MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH + 110;
	public static final int ERSTE_KOPFDATEN_ZEILE = 0;
	public static final int ZWEITE_KOPFDATEN_ZEILE = 1;
	public static final int DRITTE_KOPFDATEN_ZEILE = 2;

	private final WeakRefHelper<MeldungenSpalte> spielerSpalteWkRef;
	private final WeakRefHelper<ISuperMeleePropertiesSpalte> propertiesSpaltewkRef;
	private final WeakRefHelper<IRangliste> iRanglisteSheet;

	public AbstractRanglisteFormatter(MeldungenSpalte spielerSpalte, ISuperMeleePropertiesSpalte propertiesSpalte, IRangliste iRanglisteSheet) {
		checkNotNull(spielerSpalte);
		checkNotNull(propertiesSpalte);
		spielerSpalteWkRef = new WeakRefHelper<>(spielerSpalte);
		propertiesSpaltewkRef = new WeakRefHelper<>(propertiesSpalte);
		this.iRanglisteSheet = new WeakRefHelper<>(iRanglisteSheet);

	}

	/**
	 * call getSheetHelper from ISheet<br>
	 * do not assign to Variable, while getter does SheetRunner.testDoCancelTask(); <br>
	 *
	 * @see SheetRunner#getSheetHelper()
	 *
	 * @return SheetHelper
	 * @throws GenerateException
	 */
	protected SheetHelper getSheetHelper() throws GenerateException {
		return iRanglisteSheet.get().getSheetHelper();
	}

	protected TableBorder2 borderThinLeftBold() {
		return BorderFactory.from().allThin().boldLn().forLeft().toBorder();
	}

	protected XSpreadsheet getSheet() throws GenerateException {
		return iRanglisteSheet.get().getSheet();
	}

	/**
	 * Zweite Zeile, Spalte spiele + punkte von 6er spalten block
	 *
	 * @param ersteSummeSpalte
	 * @throws GenerateException
	 */
	protected void formatZweiteZeileSpielTagSpalten(int ersteSummeSpalte) throws GenerateException {

		ColumnProperties columnProperties = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerSumme = StringCellValue.from(getSheet(), Position.from(ersteSummeSpalte + SPIELE_PLUS_OFFS, ZWEITE_KOPFDATEN_ZEILE), "Spiele")
				.setEndPosMergeSpaltePlus(2).setCellBackColor(getHeaderFarbe()).addColumnProperties(columnProperties);
		getSheetHelper().setTextInCell(headerSumme.setBorder(BorderFactory.from().allThin().boldLn().forLeft().doubleLn().forRight().toBorder()));
		getSheetHelper().setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_PLUS_OFFS).setValue("Punkte").setEndPosMergeSpaltePlus(2)
				.setBorder(BorderFactory.from().allThin().boldLn().forRight().toBorder()));
	}

	/**
	 * Komplette Spieltage block mit 6 Spalten. (+,-,div) Spiele + Punkte
	 *
	 * @param ersteSummeSpalte
	 * @throws GenerateException
	 */

	protected void formatDritteZeileSpielTagSpalten(int ersteSummeSpalte, int spalteWidth) throws GenerateException {

		ColumnProperties columnProperties = ColumnProperties.from().setWidth(spalteWidth).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerSumme = StringCellValue.from(getSheet(), Position.from(0, DRITTE_KOPFDATEN_ZEILE), "+").setCellBackColor(getHeaderFarbe())
				.setColumnProperties(columnProperties);

		getSheetHelper().setTextInCell(headerSumme.spalte(ersteSummeSpalte + SPIELE_PLUS_OFFS).setValue("+").setComment("Summe Spiele +").setBorder(borderThinLeftBold()));
		getSheetHelper().setTextInCell(
				headerSumme.spalte(ersteSummeSpalte + SPIELE_MINUS_OFFS).setValue("-").setComment("Summe Spiele -").setBorder(BorderFactory.from().allThin().toBorder()));
		getSheetHelper().setTextInCell(headerSumme.spalte(ersteSummeSpalte + SPIELE_DIV_OFFS).setValue("Δ").setComment("Summe Spiele Differenz")
				.setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder()));
		getSheetHelper().setTextInCell(
				headerSumme.spalte(ersteSummeSpalte + PUNKTE_PLUS_OFFS).setValue("+").setComment("Summe Punkte +").setBorder(BorderFactory.from().allThin().toBorder()));
		getSheetHelper().setTextInCell(
				headerSumme.spalte(ersteSummeSpalte + PUNKTE_MINUS_OFFS).setValue("-").setComment("Summe Punkte -").setBorder(BorderFactory.from().allThin().toBorder()));
		getSheetHelper().setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_DIV_OFFS).setValue("Δ").setComment("Summe Punkte Differenz")
				.setBorder(BorderFactory.from().allThin().boldLn().forRight().toBorder()));

	}

	protected void formatErsteZeileSummeSpalte(int summeSpalte) throws GenerateException {
		ColumnProperties columnProperties = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerSumme = StringCellValue.from(getSheet(), Position.from(summeSpalte + SPIELE_PLUS_OFFS, ERSTE_KOPFDATEN_ZEILE), "Summe")
				.setColumnProperties(columnProperties).setEndPosMergeSpaltePlus(5).setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder())
				.setCellBackColor(getHeaderFarbe());
		getSheetHelper().setTextInCell(headerSumme);
	}

	protected void formatEndSummen(int erstEndsummeSpalte) throws GenerateException {
		formatErsteZeileSummeSpalte(erstEndsummeSpalte); // ERSTE SPALTE
		formatZweiteZeileSpielTagSpalten(erstEndsummeSpalte); // ZWEITE_KOPFDATEN_ZEILE
		// Letzte Summe block etwas breitere Spalten
		formatDritteZeileSpielTagSpalten(erstEndsummeSpalte, ENDSUMME_NUMBER_WIDTH);// DRITTE_KOPFDATEN_ZEILE
	}

	public void formatDatenGeradeUngerade_Old() throws GenerateException {
		MeldungenSpalte spielerSpalte = getSpielerSpalteWkRef().get();
		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.getLetzteDatenZeile();
		int letzteSpalte = iRanglisteSheet.get().getLetzteSpalte();

		ISuperMeleePropertiesSpalte propertiesSpalte = getPropertiesSpaltewkRef().get();
		// gerade / ungrade hintergrund farbe
		// CellBackColor
		Integer geradeColor = propertiesSpalte.getRanglisteHintergrundFarbeGerade();
		Integer unGeradeColor = propertiesSpalte.getRanglisteHintergrundFarbeUnGerade();

		for (int zeileCntr = ersteDatenZeile; zeileCntr <= letzteDatenZeile; zeileCntr++) {
			RangePosition datenRange = RangePosition.from(0, zeileCntr, letzteSpalte, zeileCntr);
			if ((zeileCntr & 1) == 0) {
				if (unGeradeColor != null) {
					getSheetHelper().setPropertyInRange(getSheet(), datenRange, ICommonProperties.CELL_BACK_COLOR, unGeradeColor);
				}
			} else {
				if (geradeColor != null) {
					getSheetHelper().setPropertyInRange(getSheet(), datenRange, ICommonProperties.CELL_BACK_COLOR, geradeColor);
				}
			}
		}
	}

	public void formatDatenErrorGeradeUngerade(int validateSpalteNr) throws GenerateException {

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		IRangliste sheet = iRanglisteSheet.get();

		MeldungenSpalte spielerSpalte = getSpielerSpalteWkRef().get();
		int spielerNrSpalte = spielerSpalte.getSpielerNrSpalte();
		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.getLetzteDatenZeile();
		int letzteSpalte = sheet.getLetzteSpalte();

		ISuperMeleePropertiesSpalte propertiesSpalte = getPropertiesSpaltewkRef().get();
		Integer geradeColor = propertiesSpalte.getRanglisteHintergrundFarbeGerade();
		Integer unGeradeColor = propertiesSpalte.getRanglisteHintergrundFarbeUnGerade();

		RanglisteHintergrundFarbeGeradeStyle ranglisteHintergrundFarbeGeradeStyle = new RanglisteHintergrundFarbeGeradeStyle(geradeColor);
		RanglisteHintergrundFarbeUnGeradeStyle ranglisteHintergrundFarbeUnGeradeStyle = new RanglisteHintergrundFarbeUnGeradeStyle(unGeradeColor);
		RangePosition datenRange = RangePosition.from(spielerNrSpalte, ersteDatenZeile, letzteSpalte, letzteDatenZeile);

		// formula when in validate spalte ein error
		// Formula fuer sort error, komplette zeile rot einfärben wenn fehler meldung
		// Achtung spalte plus 1 weil A ist nicht 0 sondern 1
		String formulaSortError = "LEN(TRIM(INDIRECT(ADDRESS(ROW();" + (validateSpalteNr + 1) + "))))>0";

		ConditionalFormatHelper.from(sheet, datenRange).clear().
		// -----------------------------
		// Formula fuer sort error, komplette zeile rot einfärben wenn fehler meldung
				formula1(formulaSortError).operator(ConditionOperator.FORMULA).style(new FehlerStyle()).applyNew().
				// ----------------------------------------------
				formulaIsEvenRow().style(ranglisteHintergrundFarbeGeradeStyle).applyNew().
				// ---------------------
				formulaIsOddRow().style(ranglisteHintergrundFarbeUnGeradeStyle).apply();
	}

	/**
	 * Datenbereich 6er Block
	 *
	 * @throws GenerateException
	 */

	protected void formatDatenSpielTagSpalten(int ersteSummeSpalte) throws GenerateException {

		MeldungenSpalte spielerSpalte = getSpielerSpalteWkRef().get();

		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.getLetzteDatenZeile();

		// summe spalten spiele
		// +,-,div
		Position posSummeSpieleStart = Position.from(ersteSummeSpalte, ersteDatenZeile);
		Position posSummeSpieleEnd = Position.from(posSummeSpieleStart).spaltePlus(2).zeile(letzteDatenZeile);
		RangePosition summeSpieleRange = RangePosition.from(posSummeSpieleStart, posSummeSpieleEnd);
		TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder();
		getSheetHelper().setPropertyInRange(getSheet(), summeSpieleRange, ICommonProperties.TABLE_BORDER2, border);

		// summe spalten punkte
		// +,-,div
		Position posSummePunkteStart = Position.from(posSummeSpieleEnd).zeile(ersteDatenZeile).spaltePlusEins();
		Position posSummePunkteEnd = Position.from(posSummePunkteStart).spaltePlus(2).zeile(letzteDatenZeile);
		RangePosition summePunkteRange = RangePosition.from(posSummePunkteStart, posSummePunkteEnd);
		border = BorderFactory.from().allThin().boldLn().forTop().forRight().doubleLn().forLeft().toBorder();
		getSheetHelper().setPropertyInRange(getSheet(), summePunkteRange, ICommonProperties.TABLE_BORDER2, border);

	}

	protected WeakRefHelper<MeldungenSpalte> getSpielerSpalteWkRef() {
		return spielerSpalteWkRef;
	}

	protected WeakRefHelper<ISuperMeleePropertiesSpalte> getPropertiesSpaltewkRef() {
		return propertiesSpaltewkRef;
	}

	public Integer getHeaderFarbe() throws GenerateException {
		Integer headerColor = getPropertiesSpaltewkRef().get().getRanglisteHeaderFarbe();
		return headerColor;
	}

	public StringCellValue addFooter() throws GenerateException {

		MeldungenSpalte spielerSpalte = getSpielerSpalteWkRef().get();

		int ersteFooterZeile = spielerSpalte.neachsteFreieDatenZeile();
		StringCellValue stringVal = StringCellValue.from(getSheet(), Position.from(spielerSpalte.getSpielerNrSpalte(), ersteFooterZeile)).setHoriJustify(CellHoriJustify.LEFT)
				.setCharHeight(8);

		String anzSpielerFormula = "\"Anzahl Spieler: \" & " + spielerSpalte.formulaCountSpieler();
		getSheetHelper().setFormulaInCell(stringVal.setValue(anzSpielerFormula));

		stringVal.addRowProperty(ICommonProperties.HEIGHT, 350);

		getSheetHelper().setTextInCell(stringVal.zeilePlusEins().setValue("Reihenfolge zur Ermittlung der Platzierung: 1. Spiele +, 2. Spiele Δ, 3. Punkte Δ, 4. Punkte +"));

		return stringVal;
	}
}