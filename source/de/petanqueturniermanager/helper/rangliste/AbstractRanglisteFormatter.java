/**
* Erstellung : 10.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import static com.google.common.base.Preconditions.*;
import static de.petanqueturniermanager.helper.cellvalue.CellProperties.*;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.konfiguration.IPropertiesSpalte;

abstract public class AbstractRanglisteFormatter {

	public static final int ERSTE_KOPFDATEN_ZEILE = 0;
	public static final int ZWEITE_KOPFDATEN_ZEILE = 1;
	public static final int DRITTE_KOPFDATEN_ZEILE = 2;

	private final WeakRefHelper<SpielerSpalte> spielerSpalteWkRef;
	private final WeakRefHelper<IPropertiesSpalte> propertiesSpaltewkRef;
	private final WeakRefHelper<IRangliste> iRanglisteSheet;
	private final SheetHelper sheetHelper;

	public AbstractRanglisteFormatter(XComponentContext xContext, SpielerSpalte spielerSpalte,
			IPropertiesSpalte propertiesSpalte, IRangliste iRanglisteSheet) {
		checkNotNull(xContext);
		checkNotNull(spielerSpalte);
		checkNotNull(propertiesSpalte);
		this.sheetHelper = new SheetHelper(xContext);
		this.spielerSpalteWkRef = new WeakRefHelper<SpielerSpalte>(spielerSpalte);
		this.propertiesSpaltewkRef = new WeakRefHelper<IPropertiesSpalte>(propertiesSpalte);
		this.iRanglisteSheet = new WeakRefHelper<IRangliste>(iRanglisteSheet);

	}

	protected TableBorder2 borderThinLeftBold() {
		return BorderFactory.from().allThin().boldLn().forLeft().toBorder();
	}

	protected XSpreadsheet getSheet() throws GenerateException {
		return this.iRanglisteSheet.getObject().getSheet();
	}

	/**
	 * Zweite Zeile, Spalte spiele + punkte von 6er spalten block
	 *
	 * @param ersteSummeSpalte
	 * @throws GenerateException
	 */
	protected void formatZweiteZeileSpielTagSpalten(int ersteSummeSpalte) throws GenerateException {

		StringCellValue headerSumme = StringCellValue
				.from(getSheet(), Position.from(ersteSummeSpalte + SPIELE_PLUS_OFFS, ZWEITE_KOPFDATEN_ZEILE), "Spiele")
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setEndPosMergeSpaltePlus(2)
				.setCellBackColor(getHeaderFarbe());
		this.getSheetHelper().setTextInCell(headerSumme
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().doubleLn().forRight().toBorder()));
		this.getSheetHelper().setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_PLUS_OFFS).setValue("Punkte")
				.setEndPosMergeSpaltePlus(2).setBorder(BorderFactory.from().allThin().boldLn().forRight().toBorder()));
	}

	/**
	 * Komplette Spieltage block mit 6 Spalten. (+,-,div) Spiele + Punkte
	 *
	 * @param ersteSummeSpalte
	 * @throws GenerateException
	 */

	protected void formatDritteZeileSpielTagSpalten(int ersteSummeSpalte) throws GenerateException {

		StringCellValue headerSumme = StringCellValue.from(getSheet(), Position.from(0, DRITTE_KOPFDATEN_ZEILE), "+")
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setColumnWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
				.setCellBackColor(getHeaderFarbe());

		this.getSheetHelper().setTextInCell(headerSumme.spalte(ersteSummeSpalte + SPIELE_PLUS_OFFS).setValue("+")
				.setComment("Summe Spiele +").setBorder(borderThinLeftBold()));
		this.getSheetHelper().setTextInCell(headerSumme.spalte(ersteSummeSpalte + SPIELE_MINUS_OFFS).setValue("-")
				.setComment("Summe Spiele -").setBorder(BorderFactory.from().allThin().toBorder()));
		this.getSheetHelper()
				.setTextInCell(headerSumme.spalte(ersteSummeSpalte + SPIELE_DIV_OFFS).setValue("Δ")
						.setComment("Summe Spiele Differenz")
						.setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder()));
		this.getSheetHelper().setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_PLUS_OFFS).setValue("+")
				.setComment("Summe Punkte +").setBorder(BorderFactory.from().allThin().toBorder()));
		this.getSheetHelper().setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_MINUS_OFFS).setValue("-")
				.setComment("Summe Punkte -").setBorder(BorderFactory.from().allThin().toBorder()));
		this.getSheetHelper()
				.setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_DIV_OFFS).setValue("Δ")
						.setComment("Summe Punkte Differenz")
						.setBorder(BorderFactory.from().allThin().boldLn().forRight().toBorder()));

	}

	protected void formatErsteZeileSummeSpalte(int summeSpalte) throws GenerateException {
		StringCellValue headerSumme = StringCellValue
				.from(getSheet(), Position.from(summeSpalte + SPIELE_PLUS_OFFS, ERSTE_KOPFDATEN_ZEILE), "Summe")
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setEndPosMergeSpaltePlus(5)
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder())
				.setCellBackColor(getHeaderFarbe());
		this.getSheetHelper().setTextInCell(headerSumme);

	}

	protected void formatEndSummen(int erstEndsummeSpalte) throws GenerateException {
		formatErsteZeileSummeSpalte(erstEndsummeSpalte); // ERSTE SPALTE
		formatZweiteZeileSpielTagSpalten(erstEndsummeSpalte); // ZWEITE_KOPFDATEN_ZEILE
		formatDritteZeileSpielTagSpalten(erstEndsummeSpalte);// DRITTE_KOPFDATEN_ZEILE
	}

	public void formatDatenGeradeUngerade() throws GenerateException {
		SpielerSpalte spielerSpalte = this.getSpielerSpalteWkRef().getObject();
		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.letzteDatenZeile();
		int letzteSpalte = this.iRanglisteSheet.getObject().getLetzteSpalte();

		IPropertiesSpalte propertiesSpalte = this.getPropertiesSpaltewkRef().getObject();
		// gerade / ungrade hintergrund farbe
		// CellBackColor
		Integer geradeColor = propertiesSpalte.getRanglisteHintergrundFarbeGerade();
		Integer unGeradeColor = propertiesSpalte.getRanglisteHintergrundFarbeUnGerade();

		for (int zeileCntr = ersteDatenZeile; zeileCntr <= letzteDatenZeile; zeileCntr++) {
			RangePosition datenRange = RangePosition.from(0, zeileCntr, letzteSpalte, zeileCntr);
			if ((zeileCntr & 1) == 0) {
				if (unGeradeColor != null) {
					this.getSheetHelper().setPropertyInRange(getSheet(), datenRange, CELL_BACK_COLOR, unGeradeColor);
				}
			} else {
				if (geradeColor != null) {
					this.getSheetHelper().setPropertyInRange(getSheet(), datenRange, CELL_BACK_COLOR, geradeColor);
				}
			}
		}
	}

	/**
	 * Datenbereich 6er Block
	 *
	 * @throws GenerateException
	 */

	protected void formatDatenSpielTagSpalten(int ersteSummeSpalte) throws GenerateException {

		SpielerSpalte spielerSpalte = this.getSpielerSpalteWkRef().getObject();

		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.letzteDatenZeile();

		// summe spalten spiele
		// +,-,div
		Position posSummeSpieleStart = Position.from(ersteSummeSpalte, ersteDatenZeile);
		Position posSummeSpieleEnd = Position.from(posSummeSpieleStart).spaltePlus(2).zeile(letzteDatenZeile);
		RangePosition summeSpieleRange = RangePosition.from(posSummeSpieleStart, posSummeSpieleEnd);
		TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder();
		this.getSheetHelper().setPropertyInRange(getSheet(), summeSpieleRange, TABLE_BORDER2, border);

		// summe spalten punkte
		// +,-,div
		Position posSummePunkteStart = Position.from(posSummeSpieleEnd).zeile(ersteDatenZeile).spaltePlusEins();
		Position posSummePunkteEnd = Position.from(posSummePunkteStart).spaltePlus(2).zeile(letzteDatenZeile);
		RangePosition summePunkteRange = RangePosition.from(posSummePunkteStart, posSummePunkteEnd);
		border = BorderFactory.from().allThin().boldLn().forTop().forRight().doubleLn().forLeft().toBorder();
		this.getSheetHelper().setPropertyInRange(getSheet(), summePunkteRange, TABLE_BORDER2, border);

	}

	protected WeakRefHelper<SpielerSpalte> getSpielerSpalteWkRef() {
		return this.spielerSpalteWkRef;
	}

	protected WeakRefHelper<IPropertiesSpalte> getPropertiesSpaltewkRef() {
		return this.propertiesSpaltewkRef;
	}

	protected SheetHelper getSheetHelper() {
		return this.sheetHelper;
	}

	protected Integer getHeaderFarbe() throws GenerateException {
		Integer headerColor = getPropertiesSpaltewkRef().getObject().getRanglisteHeaderFarbe();
		return headerColor;
	}

}