/**
* Erstellung : 10.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import static com.google.common.base.Preconditions.*;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableSortField;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XSortable;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ErrorMessageBox;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;

public class RangListeSorter {

	private final SheetHelper sheetHelper;
	private final WeakRefHelper<IRangliste> iRanglisteSheet;
	private final ErrorMessageBox errMsg;

	public RangListeSorter(XComponentContext xContext, IRangliste iRanglisteSheet) {
		checkNotNull(xContext);
		this.sheetHelper = new SheetHelper(xContext);
		this.iRanglisteSheet = new WeakRefHelper<IRangliste>(iRanglisteSheet);
		this.errMsg = new ErrorMessageBox(xContext);
	}

	protected IRangliste getIRangliste() throws GenerateException {
		return this.iRanglisteSheet.getObject();
	}

	public void insertManuelsortSpalten() throws GenerateException {
		// sortspalten for manuell sortieren

		int letzteDatenZeile = getIRangliste().getLetzteDatenZeile();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();

		StringCellValue sortlisteVal = StringCellValue
				.from(getIRangliste().getSheet(),
						Position.from(getIRangliste().getManuellSortSpalte(), ersteDatenZiele - 1))
				.setColumnWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setSpalteHoriJustify(CellHoriJustify.CENTER);

		int ersteSpalteEndsumme = getIRangliste().getErsteSummeSpalte();
		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ersteDatenZiele);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ersteDatenZiele);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ersteDatenZiele);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ersteDatenZiele);

		StringCellValue sortSpalte = StringCellValue.from(sortlisteVal).zeile(ersteDatenZiele);
		StringCellValue headerVal = StringCellValue.from(sortlisteVal).zeile(ersteDatenZiele - 1);

		this.sheetHelper.setTextInCell(headerVal.spaltePlusEins().setValue("S+"));
		this.sheetHelper.setFormulaInCell(sortSpalte.spaltePlusEins().setFillAutoDown(letzteDatenZeile)
				.setValue(summeSpielGewonnenZelle1.getAddress()));

		this.sheetHelper.setTextInCell(headerVal.spaltePlusEins().setValue("SΔ"));
		this.sheetHelper.setFormulaInCell(sortSpalte.spaltePlusEins().setFillAutoDown(letzteDatenZeile)
				.setValue(summeSpielDiffZelle1.getAddress()));

		this.sheetHelper.setTextInCell(headerVal.spaltePlusEins().setValue("PΔ"));
		this.sheetHelper.setFormulaInCell(
				sortSpalte.spaltePlusEins().setFillAutoDown(letzteDatenZeile).setValue(punkteDiffZelle1.getAddress()));

		this.sheetHelper.setTextInCell(headerVal.spaltePlusEins().setValue("P+"));
		this.sheetHelper.setFormulaInCell(sortSpalte.spaltePlusEins().setFillAutoDown(letzteDatenZeile)
				.setValue(punkteGewonnenZelle1.getAddress()));

	}

	private int validateSpalte() throws GenerateException {
		return getIRangliste().getManuellSortSpalte() + PUNKTE_DIV_OFFS;
	}

	public void insertSortValidateSpalte() throws GenerateException {

		int letzteDatenZeile = getIRangliste().getLetzteDatenZeile();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();

		XSpreadsheet sheet = getIRangliste().getSheet();

		StringCellValue validateHeader = StringCellValue
				.from(sheet, Position.from(validateSpalte(), ersteDatenZiele - 1)).setComment("Validate Spalte")
				.setColumnWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setSpalteHoriJustify(CellHoriJustify.CENTER)
				.setValue("Err");

		this.sheetHelper.setTextInCell(validateHeader);

		// formula zusammenbauen
		// --------------------------------------------------------------------------
		// SummenSpalten
		int ersteSpalteEndsumme = getIRangliste().getErsteSummeSpalte();

		StringCellValue platzPlatzEins = StringCellValue.from(sheet, Position.from(validateSpalte(), ersteDatenZiele),
				"x");

		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ersteDatenZiele);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ersteDatenZiele);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ersteDatenZiele);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ersteDatenZiele);

		// 1 = 1 zeile oben
		// 2 = aktuelle zeile
		// if (a2>a1) {
		// ERR
		// }
		// if (a1==a2 && b2>b1 ) {
		// ERR
		// }
		// if (a1==a2 && b1==b2 && c2>c1 ) {
		// ERR
		// }
		// if (a1==a2 && b1==b2 && c1==c2 && d2>d1) {
		// ERR
		// }

		//@formatter:off
//		String formula = "IF(ROW()=" + (ersteZeile + 1) + ";\"\";" // erste zeile ignorieren
		String formula = "IF(" + compareFormula(summeSpielGewonnenZelle1,">") + ";\"X1\";\"\")"
				// ----------------
				+ " & IF(AND("
				+ compareFormula(summeSpielGewonnenZelle1,"=")
				+ ";" + compareFormula(summeSpielDiffZelle1,">")
				+ ")"
				+ ";\"X2\";\"\")"
				// ----------------
				+ " & IF(AND("
				+ compareFormula(summeSpielGewonnenZelle1,"=")
				+ ";" + compareFormula(summeSpielDiffZelle1,"=")
				+ ";" + compareFormula(punkteDiffZelle1,">")
				+ ")"
				+ ";\"X3\";\"\")"
				// ----------------
				+ " & IF(AND("
				+ compareFormula(summeSpielGewonnenZelle1,"=")
				+ ";" + compareFormula(summeSpielDiffZelle1,"=")
				+ ";" + compareFormula(punkteDiffZelle1,"=")
				+ ";" + compareFormula(punkteGewonnenZelle1,">")
				+ ")"
				+ ";\"X4\";\"\")"
				;
		//@formatter:on

		// erste Zelle wert
		FillAutoPosition fillAutoPosition = FillAutoPosition.from(platzPlatzEins.getPos()).zeile(letzteDatenZeile);
		this.sheetHelper.setFormulaInCell(
				platzPlatzEins.setValue(formula).zeile(ersteDatenZiele).setFillAuto(fillAutoPosition));

		// Alle Nummer Bold
		this.sheetHelper.setPropertiesInRange(sheet, RangePosition.from(platzPlatzEins.getPos(), fillAutoPosition),
				CellProperties.from().setCharWeight(FontWeight.BOLD).setCharColor(ColorHelper.CHAR_COLOR_RED));
		// --------------------------------------------------------------------------
	}

	/**
	 * vergleiche wert in aktuelle zeile mit eine zeile oben<br>
	 * 1 = 1 zeile oben zeile -1 <br>
	 * 2 = aktuelle zeile
	 *
	 * @param pos
	 * @return a2>a1
	 */
	private String compareFormula(Position pos, String operator) {
		return pos.getAddress() + operator + Position.from(pos).zeilePlus(-1).getAddress();
	}

	public boolean isErrorInSheet() throws GenerateException {
		int letzteDatenZeile = getIRangliste().getLetzteDatenZeile();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();
		XSpreadsheet sheet = getIRangliste().getSheet();
		int validateSpalte = validateSpalte();
		Position valSpalteStart = Position.from(validateSpalte, ersteDatenZiele);
		Position valSpalteEnd = Position.from(validateSpalte, letzteDatenZeile);

		for (int zeile = valSpalteStart.getZeile(); zeile <= valSpalteEnd.getZeile(); zeile++) {
			String text = this.sheetHelper.getTextFromCell(sheet, Position.from(validateSpalte, zeile));
			if (StringUtils.isNoneBlank(text)) {
				// error
				this.errMsg.showOk("Fehler in Spieltagrangliste", "Fehler in Zeile " + zeile);
				return true;
			}
		}
		return false;
	}

	/**
	 * mit bestehende spalten im sheet selbst sortieren.<br>
	 * sortieren in 2 schritten
	 *
	 * @throws GenerateException
	 */
	public void doSort() throws GenerateException {
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();

		int ersteSpalteEndsumme = getIRangliste().getErsteSummeSpalte();
		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ersteDatenZiele);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ersteDatenZiele);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ersteDatenZiele);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ersteDatenZiele);

		// spalte zeile (column, row, column, row)
		XCellRange xCellRange = getxCellRangeAlleDaten();

		if (xCellRange == null) {
			return;
		}

		XSortable xSortable = UnoRuntime.queryInterface(XSortable.class, xCellRange);

		// nur 3 felder sind sortierbar !, deswegen die 4 spalte zuerst einzel sortieren.
		// wenn einzel sortieren dann von letzte bis erst spalte !
		{
			TableSortField[] aSortFields = new TableSortField[1];
			TableSortField field4 = new TableSortField();
			field4.Field = punkteGewonnenZelle1.getSpalte(); // 0 = erste spalte
			field4.IsAscending = false; // false= meiste Punkte oben
			aSortFields[0] = field4;

			PropertyValue[] sortDescr = sortDescr(aSortFields);
			xSortable.sort(sortDescr);
		}

		{
			TableSortField[] aSortFields = new TableSortField[3]; // MAX 3 felder
			TableSortField field1 = new TableSortField();
			field1.Field = summeSpielGewonnenZelle1.getSpalte();
			field1.IsAscending = false; // false = meiste Punkte oben
			aSortFields[0] = field1;

			TableSortField field2 = new TableSortField();
			field2.Field = summeSpielDiffZelle1.getSpalte(); // 0 = erste spalte
			field2.IsAscending = false; // false =meiste Punkte oben
			aSortFields[1] = field2;

			TableSortField field3 = new TableSortField();
			field3.Field = punkteDiffZelle1.getSpalte(); // 0 = erste spalte
			field3.IsAscending = false; // false = meiste Punkte oben
			aSortFields[2] = field3;

			PropertyValue[] sortDescr = sortDescr(aSortFields);
			xSortable.sort(sortDescr);
		}

	}

	private PropertyValue[] sortDescr(TableSortField[] sortFields) {

		PropertyValue[] aSortDesc = new PropertyValue[2];
		PropertyValue propVal = new PropertyValue();
		propVal.Name = "SortFields";
		propVal.Value = sortFields;
		aSortDesc[0] = propVal;

		// specifies if cell formats are moved with the contents they belong to.
		aSortDesc[1] = new PropertyValue();
		aSortDesc[1].Name = "BindFormatsToContent";
		aSortDesc[1].Value = false;

		return aSortDesc;
	}

	/**
	 * alle sortierbare daten, ohne header !
	 *
	 * @return
	 * @throws GenerateException
	 */
	private XCellRange getxCellRangeAlleDaten() throws GenerateException {
		int letzteDatenZeile = getIRangliste().getLetzteDatenZeile();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();
		XSpreadsheet sheet = getIRangliste().getSheet();
		XCellRange xCellRange = null;
		try {
			if (letzteDatenZeile > ersteDatenZiele) { // daten vorhanden ?
				// (column, row, column, row)
				xCellRange = sheet.getCellRangeByPosition(0, ersteDatenZiele, getIRangliste().getLetzteSpalte(),
						letzteDatenZeile);
			}
		} catch (IndexOutOfBoundsException e) {
			getIRangliste().getLogger().error(e.getMessage(), e);
		}
		return xCellRange;
	}

}
