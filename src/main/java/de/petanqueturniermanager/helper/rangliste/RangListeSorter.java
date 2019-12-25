/**
* Erstellung : 10.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import static de.petanqueturniermanager.helper.sheet.SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;
import static de.petanqueturniermanager.helper.sheet.SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS;
import static de.petanqueturniermanager.helper.sheet.SuperMeleeSummenSpalten.SPIELE_DIV_OFFS;
import static de.petanqueturniermanager.helper.sheet.SuperMeleeSummenSpalten.SPIELE_PLUS_OFFS;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableSortField;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XSortable;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SortHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;

public class RangListeSorter {

	private final WeakRefHelper<IRangliste> iRanglisteSheet;

	public RangListeSorter(IRangliste iRanglisteSheet) {
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
	private SheetHelper getSheetHelper() throws GenerateException {
		return iRanglisteSheet.get().getSheetHelper();
	}

	protected IRangliste getIRangliste() {
		return iRanglisteSheet.get();
	}

	public void insertManuelsortSpalten(boolean isVisible) throws GenerateException {
		// sortspalten for manuell sortieren
		int letzteDatenZeile = getIRangliste().getLetzteDatenZeile();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();

		ColumnProperties columnProperties = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).centerJustify().isVisible(isVisible);
		StringCellValue sortlisteVal = StringCellValue.from(getIRangliste().getXSpreadSheet(), Position.from(getIRangliste().getManuellSortSpalte(), ersteDatenZiele))
				.addColumnProperties(columnProperties);

		List<Position> ranglisteSpalten = getIRangliste().getRanglisteSpalten();

		for (Position ranglisteSpalte : ranglisteSpalten) {
			getSheetHelper().setFormulaInCell(sortlisteVal.setFillAutoDown(letzteDatenZeile).setValue(ranglisteSpalte.getAddress()));
			sortlisteVal.spaltePlusEins();
		}
	}

	public int validateSpalte() throws GenerateException {
		return getIRangliste().validateSpalte();
	}

	public void insertSortValidateSpalte(boolean isVisible) throws GenerateException {

		int letzteDatenZeile = getIRangliste().getLetzteDatenZeile();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();

		XSpreadsheet sheet = getIRangliste().getXSpreadSheet();

		// formula zusammenbauen
		// --------------------------------------------------------------------------

		StringCellValue platzPlatzEins = StringCellValue.from(sheet, Position.from(validateSpalte(), ersteDatenZiele), "x");
		List<Position> ranglisteSpalten = getIRangliste().getRanglisteSpalten();

		StringBuffer formulaBuff = new StringBuffer();

		// =WENN(L4>L3;"X1";"") &
		// WENN(UND(L4=L3;N4>N3);"X3";"") &
		// WENN(UND(L4=L3;N4=N3;Q4>Q3);"X4";"") &
		// WENN(UND(L4=L3;N4=N3;Q4=Q3;O4>O3);"X5";"")

		formulaBuff.append("IF(" + compareFormula(ranglisteSpalten.get(0), ">") + ";\"X1\";\"\")");
		for (int ranglisteCntr = 1; ranglisteCntr < ranglisteSpalten.size(); ranglisteCntr++) {
			// ----------------
			formulaBuff.append(" & IF(AND(");
			formulaBuff.append(compareFormula(ranglisteSpalten.get(0), "="));
			int restSpalte;
			for (restSpalte = 1; restSpalte < ranglisteCntr; restSpalte++) {
				formulaBuff.append(";");
				formulaBuff.append(compareFormula(ranglisteSpalten.get(restSpalte), "="));
			}

			formulaBuff.append(";");
			formulaBuff.append(compareFormula(ranglisteSpalten.get(restSpalte), ">"));
			formulaBuff.append(")");
			formulaBuff.append(";\"X" + (ranglisteCntr + 2) + "\";\"\")");
		}

		// ----------------
		// + " & IF(AND("
		// + compareFormula(summeSpielGewonnenZelle1,"=")
		// + ";" + compareFormula(summeSpielDiffZelle1,">")
		// + ")"
		// + ";\"X2\";\"\")"
		// // ----------------
		// + " & IF(AND("
		// + compareFormula(summeSpielGewonnenZelle1,"=")
		// + ";" + compareFormula(summeSpielDiffZelle1,"=")
		// + ";" + compareFormula(punkteDiffZelle1,">")
		// + ")"
		// + ";\"X3\";\"\")"
		// // ----------------
		// + " & IF(AND("
		// + compareFormula(summeSpielGewonnenZelle1,"=")
		// + ";" + compareFormula(summeSpielDiffZelle1,"=")
		// + ";" + compareFormula(punkteDiffZelle1,"=")
		// + ";" + compareFormula(punkteGewonnenZelle1,">")
		// + ")"
		// + ";\"X4\";\"\")"

		// erste Zelle wert
		FillAutoPosition fillAutoPosition = FillAutoPosition.from(platzPlatzEins.getPos()).zeile(letzteDatenZeile);
		getSheetHelper().setFormulaInCell(platzPlatzEins.setValue(formulaBuff.toString()).zeile(ersteDatenZiele).setFillAuto(fillAutoPosition));

		// Alle Nummer Bold
		getSheetHelper().setPropertiesInRange(sheet, RangePosition.from(platzPlatzEins.getPos(), fillAutoPosition),
				CellProperties.from().setCharWeight(FontWeight.BOLD).setCharColor(ColorHelper.CHAR_COLOR_RED));
		// --------------------------------------------------------------------------
		// Header am ende, wegen Bug ? Auto Fill und ausgeblendete Spalte
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
				.isVisible(isVisible);
		StringCellValue validateHeader = StringCellValue.from(sheet, Position.from(validateSpalte(), ersteDatenZiele - 1)).addColumnProperties(columnProperties).setValue("Err");

		if (isVisible) {
			validateHeader.setComment("Validate Spalte");
		}

		getSheetHelper().setStringValueInCell(validateHeader);
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

	/**
	 * Validate Spalte prüfen
	 *
	 * @return
	 * @throws GenerateException
	 */
	public void isErrorInSheet() throws GenerateException {
		int letzteDatenZeile = getIRangliste().getLetzteDatenZeile();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();
		XSpreadsheet sheet = getIRangliste().getXSpreadSheet();
		int validateSpalte = validateSpalte();
		Position valSpalteStart = Position.from(validateSpalte, ersteDatenZiele);
		Position valSpalteEnd = Position.from(validateSpalte, letzteDatenZeile);

		for (int zeile = valSpalteStart.getZeile(); zeile <= valSpalteEnd.getZeile(); zeile++) {
			String text = getSheetHelper().getTextFromCell(sheet, Position.from(validateSpalte, zeile));
			if (StringUtils.isNoneBlank(text)) {
				// error
				throw new GenerateException("Fehler in Spieltagrangliste, Fehler in Zeile " + zeile);
			}
		}
	}

	public void doSort() throws GenerateException {

		int ersteSpalte = getIRangliste().getErsteSpalte();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();
		int letzteSpalte = getIRangliste().getLetzteSpalte();
		int letzteDatenZeile = getIRangliste().getLetzteDatenZeile();

		getIRangliste().calculateAll(); // zum sortieren werte kalkulieren
		List<Position> ranglisteSpalten = getIRangliste().getRanglisteSpalten();

		int[] sortSpalten = new int[ranglisteSpalten.size()];

		int idx = 0;
		// erste sort spalte = erste spalte in range
		for (Position ranglistePos : ranglisteSpalten) {
			// Position im Sheet
			// Sortpos muss minus offset ersteSpalte sein
			sortSpalten[idx] = ranglistePos.getSpalte() - ersteSpalte;
			idx++;
		}
		RangePosition toSortRange = RangePosition.from(ersteSpalte, ersteDatenZiele, letzteSpalte, letzteDatenZeile);
		sortHelper(toSortRange, sortSpalten);
	}

	public void sortHelper(RangePosition toSortRange, int[] sortSpalten) throws GenerateException {
		SortHelper.from(getIRangliste().getXSpreadSheet(), toSortRange).abSteigendSortieren().spaltenToSort(sortSpalten).doSort();
	}

	/**
	 * mit bestehende spalten im sheet selbst sortieren.<br>
	 * sortieren in 2 schritten
	 *
	 * @throws GenerateException
	 */
	public void doSort_deprecated() throws GenerateException {
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

	@Deprecated
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
	@Deprecated
	private XCellRange getxCellRangeAlleDaten() throws GenerateException {
		int letzteDatenZeile = getIRangliste().getLetzteDatenZeile();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();
		int letzteSpalte = getIRangliste().getLetzteSpalte();
		XSpreadsheet sheet = getIRangliste().getXSpreadSheet();
		XCellRange xCellRange = null;
		try {
			if (letzteDatenZeile > ersteDatenZiele) { // daten vorhanden ?
				// (column, row, column, row)
				xCellRange = sheet.getCellRangeByPosition(0, ersteDatenZiele, letzteSpalte, letzteDatenZeile);
			}
		} catch (IndexOutOfBoundsException e) {
			getIRangliste().getLogger().error(e.getMessage(), e);
		}
		return xCellRange;
	}

}
