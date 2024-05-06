package de.petanqueturniermanager.basesheet.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;

import com.sun.star.sheet.ConditionOperator;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;

/**
 * Erstellung 31.03.2024 / Michael Massee
 */

public class SpielrundeHelper {

	private static int DEFAULT_HEADER_CHAR_HEIGHT = 14;
	private static int DEFAULT_NR_CHAR_HEIGHT = 16;

	private final ISheet sheet;
	private final int headerCharHeight;
	private final int nrCharHeight;
	private final boolean mitHeader;
	private final SpielrundeHintergrundFarbeGeradeStyle spielrundeHintergrundFarbeGeradeStyle;
	private final SpielrundeHintergrundFarbeUnGeradeStyle spielrundeHintergrundFarbeUnGeradeStyle;

	public SpielrundeHelper(ISheet sheet, SpielrundeHintergrundFarbeGeradeStyle spielrundeHintergrundFarbeGeradeStyle,
			SpielrundeHintergrundFarbeUnGeradeStyle spielrundeHintergrundFarbeUnGeradeStyle) {
		this(sheet, DEFAULT_HEADER_CHAR_HEIGHT, DEFAULT_NR_CHAR_HEIGHT, true, spielrundeHintergrundFarbeGeradeStyle,
				spielrundeHintergrundFarbeUnGeradeStyle);
	}

	public SpielrundeHelper(ISheet sheet, int headerCharHeight, int nrCharHeight, boolean mitHeader,
			SpielrundeHintergrundFarbeGeradeStyle spielrundeHintergrundFarbeGeradeStyle,
			SpielrundeHintergrundFarbeUnGeradeStyle spielrundeHintergrundFarbeUnGeradeStyle) {
		this.sheet = checkNotNull(sheet);
		this.headerCharHeight = headerCharHeight;
		this.nrCharHeight = nrCharHeight;
		this.mitHeader = mitHeader;
		this.spielrundeHintergrundFarbeGeradeStyle = checkNotNull(spielrundeHintergrundFarbeGeradeStyle);
		this.spielrundeHintergrundFarbeUnGeradeStyle = checkNotNull(spielrundeHintergrundFarbeUnGeradeStyle);
	}

	/**
	 * enweder einfach ein laufende nummer, oder jenachdem was in der konfig steht die Spielbahnnummer<br>
	 * property getSpielrundeSpielbahn<br>
	 * X = nur ein laufende paarungen nummer<br>
	 * L = Spielbahn -> leere Spalte<br>
	 * N = Spielbahn -> durchnumeriert<br>
	 * R = Spielbahn -> random<br>
	 *
	 * @throws GenerateException
	 */
	public void datenErsteSpalte(SpielrundeSpielbahn spielrundeSpielbahnFlagAusKonfig, int erstZeile, int letzteZeile,
			int nrSpalte, int headerZeile, int headerZeile2, Integer headerColor) throws GenerateException {

		sheet.processBoxinfo("Erste Spalte Daten einfügen");

		Position posHeaderZelle1 = Position.from(nrSpalte, headerZeile);
		//Position posHeaderZelle2 = Position.from(nrSpalte, headerZeile2);
		Position posErsteDatenZelle = Position.from(nrSpalte, erstZeile);
		Position posLetztDatenZelle = Position.from(nrSpalte, letzteZeile);
		RangePosition rangeErsteSpalte = RangePosition.from(posErsteDatenZelle, posLetztDatenZelle);

		// header
		// -------------------------
		// spalte paarungen Nr oder Spielbahn-Nummer
		// -------------------------
		ColumnProperties columnProperties = ColumnProperties.from().setVertJustify(CellVertJustify2.CENTER)
				.setHoriJustify(CellHoriJustify.CENTER);
		if (spielrundeSpielbahnFlagAusKonfig == SpielrundeSpielbahn.X) {
			if (mitHeader) {
				StringCellValue headerValue = StringCellValue.from(sheet, posHeaderZelle1).setRotateAngle(0)
						.setVertJustify(CellVertJustify2.BOTTOM).centerHoriJustify().setCellBackColor(headerColor)
						.setCharHeight(headerCharHeight).setEndPosMergeZeilePlus(1).setValue("#").setShrinkToFit(true)
						.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
				sheet.getSheetHelper().setStringValueInCell(headerValue);
			}

			columnProperties.setWidth(500); // Paarungen cntr
			sheet.getSheetHelper().setColumnProperties(sheet, nrSpalte, columnProperties);
		} else {
			// Spielbahn Spalte header
			if (mitHeader) {
				StringCellValue headerValue = StringCellValue.from(sheet, posHeaderZelle1).setRotateAngle(27000)
						.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
						.setCellBackColor(headerColor).setCharHeight(headerCharHeight).setEndPosMergeZeilePlus(1)
						.setValue("Bahn").setShrinkToFit(true)
						.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
						.setComment("Spielbahn");
				sheet.getSheetHelper().setStringValueInCell(headerValue);
			}
			columnProperties.setWidth(900);
			sheet.getSheetHelper().setColumnProperties(sheet, nrSpalte, columnProperties);
		}

		RangePosition nbrRangeOhneHeader = RangePosition.from(posErsteDatenZelle, posLetztDatenZelle);
		sheet.getSheetHelper().setPropertiesInRange(sheet, nbrRangeOhneHeader,
				CellProperties.from().setCharHeight(nrCharHeight).setBorder(BorderFactory.from().allThin().toBorder()));

		int anzPaarungen = letzteZeile - erstZeile + 1;

		// Daten
		if (spielrundeSpielbahnFlagAusKonfig == SpielrundeSpielbahn.X
				|| spielrundeSpielbahnFlagAusKonfig == SpielrundeSpielbahn.N) {
			StringCellValue formulaCellValue = StringCellValue.from(sheet, posErsteDatenZelle);
			formulaCellValue.setValue("=ROW()-" + erstZeile).setFillAutoDown(letzteZeile);
			sheet.getSheetHelper().setFormulaInCell(formulaCellValue);
		} else if (spielrundeSpielbahnFlagAusKonfig == SpielrundeSpielbahn.R) {
			// R = Spielbahn -> random x 
			// anzahl paarungen ?
			ArrayList<Integer> bahnnummern = new ArrayList<>();
			// fill
			for (int i = 1; i <= anzPaarungen; i++) {
				bahnnummern.add(i);
			}
			// mishen
			Collections.shuffle(bahnnummern);
			StringCellValue stringCellValue = StringCellValue.from(sheet, posErsteDatenZelle);
			for (Integer bahnnr : bahnnummern) {
				SheetRunner.testDoCancelTask();
				if (bahnnr > 0) { // es kann sein das wir lücken haben, = teampaarungen ohne bahnnummer
					stringCellValue.setValue(bahnnr);
					sheet.getSheetHelper().setStringValueInCell(stringCellValue);
				}
				stringCellValue.zeilePlusEins();
			}
		} else if (spielrundeSpielbahnFlagAusKonfig == SpielrundeSpielbahn.L) {
			Position itteratepos = Position.from(posErsteDatenZelle); // orginal nicht verändern
			// leere spalte
			for (int i = 1; i <= anzPaarungen; i++) {
				sheet.getSheetHelper().clearValInCell(sheet.getXSpreadSheet(), itteratepos);
				itteratepos.zeilePlusEins();
			}
		}

		// Formatiere Daten Spalte
		// erste Spalte, mit prüfung auf doppelte nummer

		FehlerStyle fehlerStyle = new FehlerStyle();
		String conditionfindDoppelt = "COUNTIF(" + posErsteDatenZelle.getSpalteAddressWith$() + ";"
				+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
		String conditionNotEmpty = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>\"\"";
		String formulaFindDoppelteSpielrNr = "AND(" + conditionfindDoppelt + ";" + conditionNotEmpty + ")";
		ConditionalFormatHelper.from(sheet, rangeErsteSpalte).clear().formula1(formulaFindDoppelteSpielrNr)
				.operator(ConditionOperator.FORMULA).style(fehlerStyle).applyAndDoReset();

		ConditionalFormatHelper.from(sheet, rangeErsteSpalte).formulaIsEvenRow()
				.style(spielrundeHintergrundFarbeGeradeStyle).applyAndDoReset();
		ConditionalFormatHelper.from(sheet, rangeErsteSpalte).formulaIsOddRow()
				.style(spielrundeHintergrundFarbeUnGeradeStyle).applyAndDoReset();

	}

	public void formatiereGeradeUngradeSpielpaarungen(ISheet iSheet, int startSpalte, int startZeile, int endSpalte,
			int endZeile) throws GenerateException {
		RangePosition datenRangeOhneErsteSpalteOhneErgebnis = RangePosition.from(startSpalte, startZeile, endSpalte,
				endZeile);
		formatiereGeradeUngradeSpielpaarungen(iSheet, datenRangeOhneErsteSpalteOhneErgebnis,
				spielrundeHintergrundFarbeGeradeStyle, spielrundeHintergrundFarbeUnGeradeStyle);
	}

	public void formatiereGeradeUngradeSpielpaarungen(ISheet iSheet,
			RangePosition datenRangeOhneErsteSpalteOhneErgebnis,
			SpielrundeHintergrundFarbeGeradeStyle spielrundeHintergrundFarbeGeradeStyle,
			SpielrundeHintergrundFarbeUnGeradeStyle spielrundeHintergrundFarbeUnGeradeStyle) throws GenerateException {

		// gerade / ungrade hintergrund farbe
		// CellBackColor

		ConditionalFormatHelper.from(iSheet, datenRangeOhneErsteSpalteOhneErgebnis).clear().formulaIsEvenRow()
				.style(spielrundeHintergrundFarbeGeradeStyle).applyAndDoReset();
		ConditionalFormatHelper.from(iSheet, datenRangeOhneErsteSpalteOhneErgebnis).formulaIsOddRow()
				.operator(ConditionOperator.FORMULA).style(spielrundeHintergrundFarbeUnGeradeStyle).applyAndDoReset();

	}

}
