/*
* Erstellung : 01.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import java.util.List;
import java.util.stream.Collectors;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XCellRangeFormula;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.XCellRange;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.i18n.I18n;

public class RangListeSpalte {

	private final int rangListeSpalte;

	private final IRangliste iRanglisteSheet;

	public RangListeSpalte(int rangListeSpalte, IRangliste iRanglisteSheet) {
		this.rangListeSpalte = rangListeSpalte;
		this.iRanglisteSheet = iRanglisteSheet;
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
		return iRanglisteSheet.getSheetHelper();
	}

	public int getRangListeSpalte() {
		return rangListeSpalte;
	}

	public void insertHeaderInSheet(int headerColor) throws GenerateException {

		iRanglisteSheet.processBoxinfo("processbox.rangliste.spalten.einfuegen");

		int ersteZeile = getIRanglisteSheet().getErsteDatenZiele();

		// Properties für Daten
		ColumnProperties columnProperties = ColumnProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(1000).setCharWeight(FontWeight.BOLD).setCharHeight(11);

		StringCellValue celVal = StringCellValue.from(getSheet(), Position.from(rangListeSpalte, ersteZeile - 3), I18n.get("column.header.platz")).addColumnProperties(columnProperties)
				.setRotateAngle(27000).setVertJustify(CellVertJustify2.CENTER).setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor).setCharHeight(10)
				.setCharWeight(FontWeight.BOLD).setCharHeight(12).setEndPosMerge(Position.from(rangListeSpalte, ersteZeile - 1)).setShrinkToFit(true);
		getSheetHelper().setStringValueInCell(celVal); // spieler nr
	}

	public void upDateRanglisteSpalte() throws GenerateException {

		iRanglisteSheet.processBoxinfo("processbox.rangliste.aktualisieren");
		int letzteZeile = getIRanglisteSheet().getLetzteMitDatenZeileInSpielerNrSpalte();
		int ersteZeile = getIRanglisteSheet().getErsteDatenZiele();

		if (letzteZeile < ersteZeile) {
			return;
		}

		// =WENN(ZEILE()=4;1;WENN(UND(
		// INDIREKT(ADRESSE(ZEILE()-1;12;4))=INDIREKT(ADRESSE(ZEILE();12;4));
		// INDIREKT(ADRESSE(ZEILE()-1;14;4))=INDIREKT(ADRESSE(ZEILE();14;4));
		// INDIREKT(ADRESSE(ZEILE()-1;17;4))=INDIREKT(ADRESSE(ZEILE();17;4));
		// INDIREKT(ADRESSE(ZEILE()-1;15;4))=INDIREKT(ADRESSE(ZEILE();15;4)));
		// INDIREKT(ADRESSE(ZEILE()-1;SPALTE();4));ZEILE()-3))

		String ranglisteAdressPlusEinPlatzIndiekt = "INDIRECT(ADDRESS(ROW()-1;COLUMN();4))";

		List<Position> ranglisteSpalten = iRanglisteSheet.getRanglisteSpalten();

		String formula = "=IF(ROW()=" + (ersteZeile + 1) + ";1;IF(AND("
				+ ranglisteSpalten.stream().map(this::indirectFormula).collect(Collectors.joining(";"))
				+ ");" + ranglisteAdressPlusEinPlatzIndiekt + ";ROW()-" + ersteZeile + "))";

		// Formel als Block in alle Zeilen der Platz-Spalte schreiben.
		// fillAuto würde die Hintergrundfarbe der Quellzelle (Zebra) in alle Zielzellen kopieren
		// und so die zuvor aufgetragene Zebra-Färbung der Platz-Spalte überschreiben.
		RangePosition platzRange = RangePosition.from(rangListeSpalte, ersteZeile, rangListeSpalte, letzteZeile);
		int anzahlZeilen = letzteZeile - ersteZeile + 1;
		String[][] formulas = new String[anzahlZeilen][1];
		for (int i = 0; i < anzahlZeilen; i++) {
			SheetRunner.testDoCancelTask();
			formulas[i][0] = formula;
		}
		try {
			XCellRange xCellRange = getSheet().getCellRangeByPosition(platzRange.getStartSpalte(),
					platzRange.getStartZeile(), platzRange.getEndeSpalte(), platzRange.getEndeZeile());
			XCellRangeFormula xRangeFormula = Lo.qi(XCellRangeFormula.class, xCellRange);
			xRangeFormula.setFormulaArray(formulas);
		} catch (com.sun.star.lang.IndexOutOfBoundsException e) {
			throw new GenerateException("Platz-Spalte konnte nicht beschrieben werden: " + e.getMessage());
		}

		// Platz-Spalte: fett + dicke rechte Linie
		getSheetHelper().setPropertiesInRange(getSheet(), platzRange,
				CellProperties.from()
						.setCharWeight(FontWeight.BOLD)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forRight().toBorder()));
	}

	/**
	 * vergleiche wert in aktuelle zeile mit ein zeile oben
	 *
	 * @param pos
	 * @return INDIREKT(ADRESSE(ZEILE()-1;14;8))=INDIREKT(ADRESSE(ZEILE();14;8))
	 */
	private String indirectFormula(Position pos) {
		return "INDIRECT(ADDRESS(ROW()-1;" + (pos.getSpalte() + 1) + ";4))=" + "INDIRECT(ADDRESS(ROW();" + (pos.getSpalte() + 1) + ";4))";
	}

	private IRangliste getIRanglisteSheet() {
		return iRanglisteSheet;
	}

	protected XSpreadsheet getSheet() throws GenerateException {
		return getIRanglisteSheet().getXSpreadSheet();
	}

}
