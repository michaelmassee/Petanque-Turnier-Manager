/**
 * Erstellung : 10.05.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.rangliste;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;

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
		int letzteDatenZeile = getIRangliste().getLetzteMitDatenZeileInSpielerNrSpalte();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();

		ColumnProperties columnProperties = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).centerJustify().isVisible(isVisible);
		StringCellValue sortlisteVal = StringCellValue
				.from(getIRangliste().getXSpreadSheet(),
						Position.from(getIRangliste().getManuellSortSpalte(), ersteDatenZiele))
				.addColumnProperties(columnProperties);

		List<Position> ranglisteSpalten = getIRangliste().getRanglisteSpalten();

		for (Position ranglisteSpalte : ranglisteSpalten) {
			getSheetHelper().setFormulaInCell(
					sortlisteVal.setFillAutoDown(letzteDatenZeile).setValue(ranglisteSpalte.getAddress()));
			sortlisteVal.spaltePlusEins();
		}
	}

	public int validateSpalte() throws GenerateException {
		return getIRangliste().validateSpalte();
	}

	public void insertSortValidateSpalte(boolean isVisible) throws GenerateException {

		int letzteDatenZeile = getIRangliste().getLetzteMitDatenZeileInSpielerNrSpalte();
		int ersteDatenZiele = getIRangliste().getErsteDatenZiele();

		XSpreadsheet sheet = getIRangliste().getXSpreadSheet();

		// formula zusammenbauen
		// --------------------------------------------------------------------------

		StringCellValue platzPlatzEins = StringCellValue.from(sheet, Position.from(validateSpalte(), ersteDatenZiele),
				"x");
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
		getSheetHelper().setFormulaInCell(
				platzPlatzEins.setValue(formulaBuff.toString()).zeile(ersteDatenZiele).setFillAuto(fillAutoPosition));

		// Alle Nummer Bold
		getSheetHelper().setPropertiesInRange(sheet, RangePosition.from(platzPlatzEins.getPos(), fillAutoPosition),
				CellProperties.from().setCharWeight(FontWeight.BOLD).setCharColor(ColorHelper.CHAR_COLOR_RED));
		// --------------------------------------------------------------------------
		// Header am ende, wegen Bug ? Auto Fill und ausgeblendete Spalte
		ColumnProperties columnProperties = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
				.isVisible(isVisible);
		StringCellValue validateHeader = StringCellValue
				.from(sheet, Position.from(validateSpalte(), ersteDatenZiele - 1)).addColumnProperties(columnProperties)
				.setValue("Err"); // Ueberschrift Validate Spalte

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
		int letzteDatenZeile = getIRangliste().getLetzteMitDatenZeileInSpielerNrSpalte();
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
		int letzteDatenZeile = getIRangliste().getLetzteMitDatenZeileInSpielerNrSpalte();

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

	private void sortHelper(RangePosition toSortRange, int[] sortSpalten) throws GenerateException {
		SortHelper.from(getIRangliste(), toSortRange).abSteigendSortieren().spaltenToSort(sortSpalten).doSort();
	}

}
