/**
* Erstellung : 01.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

import java.lang.ref.WeakReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.AbstractCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

public class RangListeSpalte {
	private static final Logger logger = LogManager.getLogger(RangListeSpalte.class);

	private final WeakReference<IMitSpielerSpalte> spielerSpalte;
	private final int rangListeSpalte;
	private final SheetHelper sheetHelper;
	private final WeakReference<IEndSummeSpalten> endsummenspalten;
	private final WeakReference<ISheet> sheet;

	public RangListeSpalte(XComponentContext xContext, int rangListeSpalte, IMitSpielerSpalte spielerSpalte,
			IEndSummeSpalten endsummeSpaltenSheet, ISheet sheet) {
		this.rangListeSpalte = rangListeSpalte;
		this.sheetHelper = new SheetHelper(xContext);
		this.spielerSpalte = new WeakReference<IMitSpielerSpalte>(spielerSpalte);
		this.endsummenspalten = new WeakReference<IEndSummeSpalten>(endsummeSpaltenSheet);
		this.sheet = new WeakReference<ISheet>(sheet);
	}

	public int getRangListeSpalte() {
		return this.rangListeSpalte;
	}

	private IMitSpielerSpalte getMitSpielerSpalte() {
		return this.spielerSpalte.get();
	}

	private IEndSummeSpalten getEndSummeSpalten() {
		return this.endsummenspalten.get();
	}

	public void insertHeaderInSheet() {
		int ersteZeile = getMitSpielerSpalte().getErsteDatenZiele();

		StringCellValue celVal = StringCellValue
				.from(getSheet(), Position.from(this.rangListeSpalte, ersteZeile - 1), "Platz")
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setSetColumnWidth(1000).setCharWeight(FontWeight.BOLD);
		this.sheetHelper.setTextInCell(celVal); // spieler nr
	}

	public void upDateRanglisteSpalte() {

		// SummenSpalten
		int letzteZeile = getMitSpielerSpalte().letzteDatenZeile();
		int ersteSpalteEndsumme = getEndSummeSpalten().getErsteSummeSpalte();
		int ersteZeile = getMitSpielerSpalte().getErsteDatenZiele();

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

		// Alle Nummer Bold
		this.sheetHelper.setPropertyInRange(getSheet(), RangePosition.from(platzPlatzEins.getPos(), fillAutoPosition),
				AbstractCellValue.CHAR_WEIGHT, FontWeight.BOLD);
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

	private final XSpreadsheet getSheet() {
		if (!this.sheet.isEnqueued()) {
			return this.sheet.get().getSheet();
		}
		// darf nicht passieren
		throw new NullPointerException("Weakref ISheet is Null");
	}

}
