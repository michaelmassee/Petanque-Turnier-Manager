/**
* Erstellung : 01.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;

public class RangListeSpalte {

	private final int rangListeSpalte;
	private final SheetHelper sheetHelper;
	private final WeakRefHelper<IRangliste> iRanglisteSheet;

	public RangListeSpalte(XComponentContext xContext, int rangListeSpalte, IRangliste iRanglisteSheet) {
		this.rangListeSpalte = rangListeSpalte;
		this.sheetHelper = new SheetHelper(xContext);
		this.iRanglisteSheet = new WeakRefHelper<IRangliste>(iRanglisteSheet);
	}

	public int getRangListeSpalte() {
		return this.rangListeSpalte;
	}

	public void insertHeaderInSheet(int headerColor) throws GenerateException {
		int ersteZeile = getIRanglisteSheet().getErsteDatenZiele();

		// Properties für Daten
		CellProperties columnProperties = CellProperties.from().setHoriJustify(CellHoriJustify.CENTER).setWidth(1000)
				.setCharWeight(FontWeight.BOLD).setCharHeight(11);

		StringCellValue celVal = StringCellValue
				.from(getSheet(), Position.from(this.rangListeSpalte, ersteZeile - 2), "Platz")
				.addColumnProperties(columnProperties).setRotateAngle(27000).setVertJustify(CellVertJustify2.CENTER)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor).setCharHeight(10)
				.setCharWeight(FontWeight.NORMAL).setEndPosMerge(Position.from(this.rangListeSpalte, ersteZeile - 1));
		this.sheetHelper.setTextInCell(celVal); // spieler nr
	}

	public void upDateRanglisteSpalte() throws GenerateException {

		// SummenSpalten
		int letzteZeile = getIRanglisteSheet().getLetzteDatenZeile();
		int ersteSpalteEndsumme = getIRanglisteSheet().getErsteSummeSpalte();
		int ersteZeile = getIRanglisteSheet().getErsteDatenZiele();

		StringCellValue platzPlatzEins = StringCellValue.from(getSheet(),
				Position.from(this.rangListeSpalte, ersteZeile), "x");

		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ersteZeile);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ersteZeile);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ersteZeile);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ersteZeile);

		// =WENN(ZEILE()=4;1;WENN(UND(
		// INDIREKT(ADRESSE(ZEILE()-1;14;8))=INDIREKT(ADRESSE(ZEILE();14;8));
		// INDIREKT(ADRESSE(ZEILE()-1;16;8))=INDIREKT(ADRESSE(ZEILE();16;8));
		// INDIREKT(ADRESSE(ZEILE()-1;19;8))=INDIREKT(ADRESSE(ZEILE();19;8));
		// INDIREKT(ADRESSE(ZEILE()-1;17;8))=INDIREKT(ADRESSE(ZEILE();17;8)));
		// INDIREKT(ADRESSE(ZEILE()-1;SPALTE();8));INDIREKT(ADRESSE(ZEILE()-1;SPALTE();8))+1))

		String ranglisteAdressPlusEinPlatzIndiekt = "INDIRECT(ADDRESS(ROW()-1;COLUMN();8))";
		String formula = "IF(ROW()=" + (ersteZeile + 1) + ";1;" + "IF(AND(" + indirectFormula(summeSpielGewonnenZelle1)
				+ ";" + indirectFormula(summeSpielDiffZelle1) + ";" + indirectFormula(punkteDiffZelle1) + ";"
				+ indirectFormula(punkteGewonnenZelle1) + ");" + ranglisteAdressPlusEinPlatzIndiekt + ";"
				+ ranglisteAdressPlusEinPlatzIndiekt + "+1))";

		// erste Zelle wert
		FillAutoPosition fillAutoPosition = FillAutoPosition.from(platzPlatzEins.getPos()).zeile(letzteZeile);
		this.sheetHelper
				.setFormulaInCell(platzPlatzEins.setValue(formula).zeile(ersteZeile).setFillAuto(fillAutoPosition));

		// Border
		this.sheetHelper.setPropertiesInRange(getSheet(), RangePosition.from(platzPlatzEins.getPos(), fillAutoPosition),
				CellProperties.from().setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()));
	}

	/**
	 * vergleiche wert in aktuelle zeile mit ein zeile oben
	 *
	 * @param pos
	 * @return INDIREKT(ADRESSE(ZEILE()-1;14;8))=INDIREKT(ADRESSE(ZEILE();14;8))
	 */
	private String indirectFormula(Position pos) {
		return "INDIRECT(ADDRESS(ROW()-1;" + (pos.getSpalte() + 1) + ";8))=" + "INDIRECT(ADDRESS(ROW();"
				+ (pos.getSpalte() + 1) + ";8))";
	}

	private IRangliste getIRanglisteSheet() {
		return this.iRanglisteSheet.getObject();
	}

	protected XSpreadsheet getSheet() throws GenerateException {
		return getIRanglisteSheet().getSheet();
	}

}
