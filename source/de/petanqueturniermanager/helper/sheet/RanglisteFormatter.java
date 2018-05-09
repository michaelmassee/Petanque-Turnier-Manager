/**
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

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
import de.petanqueturniermanager.konfiguration.IPropertiesSpalte;

public class RanglisteFormatter {

	public static final int ERSTE_KOPFDATEN_ZEILE = 0; // Zeile 1
	public static final int ZWEITE_KOPFDATEN_ZEILE = 1; // Zeile 2
	public static final int DRITTE_KOPFDATEN_ZEILE = 2; // Zeile 3

	private final WeakRefHelper<IRangliste> ranglisteWkRef;
	private final WeakRefHelper<SpielerSpalte> spielerSpalteWkRef;
	private final WeakRefHelper<IPropertiesSpalte> propertiesSpaltewkRef;
	private final int anzSpaltenInSpielrunde;
	private final int ersteSpielRundeSpalte;
	private final SheetHelper sheetHelper;

	public RanglisteFormatter(XComponentContext xContext, IRangliste rangliste, int anzSpaltenInSpielrunde,
			SpielerSpalte spielerSpalte, int ersteSpielRundeSpalte, IPropertiesSpalte propertiesSpalte) {
		checkNotNull(rangliste);
		checkNotNull(spielerSpalte);
		checkNotNull(xContext);
		this.sheetHelper = new SheetHelper(xContext);
		this.ranglisteWkRef = new WeakRefHelper<IRangliste>(rangliste);
		this.spielerSpalteWkRef = new WeakRefHelper<SpielerSpalte>(spielerSpalte);
		this.propertiesSpaltewkRef = new WeakRefHelper<IPropertiesSpalte>(propertiesSpalte);
		this.anzSpaltenInSpielrunde = anzSpaltenInSpielrunde;
		this.ersteSpielRundeSpalte = ersteSpielRundeSpalte;
	}

	public void updateHeader() throws GenerateException {
		IRangliste rangliste = this.ranglisteWkRef.getObject();
		int anzRunden = rangliste.getAnzahlRunden();
		if (anzRunden < 1) {
			return;
		}
		XSpreadsheet sheet = rangliste.getSheet();
		int ersteSummeSpalte = rangliste.getErsteSummeSpalte();

		IPropertiesSpalte propertiesSpalte = this.propertiesSpaltewkRef.getObject();
		Integer headerColor = propertiesSpalte.getRanglisteHeaderFarbe();

		// -------------------------
		// spielrunden spalten
		// -------------------------
		StringCellValue headerPlus = StringCellValue
				.from(sheet, Position.from(this.ersteSpielRundeSpalte, DRITTE_KOPFDATEN_ZEILE), "+")
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setColumnWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
				.setCellBackColor(headerColor);
		StringCellValue headerMinus = StringCellValue.from(headerPlus).setValue("-");

		TableBorder2 borderPlus = BorderFactory.from().allThin().boldLn().forLeft().toBorder();
		headerPlus.setBorder(borderPlus);

		for (int spielRunde = 1; spielRunde <= anzRunden; spielRunde++) {
			int plusSpalte = this.ersteSpielRundeSpalte + ((spielRunde - 1) * 2);
			this.sheetHelper
					.setTextInCell(headerPlus.spalte(plusSpalte).setComment("Spielrunde " + spielRunde + " Punkte +"));
			this.sheetHelper.setTextInCell(
					headerMinus.spalte(plusSpalte + 1).setComment("Spielrunde " + spielRunde + " Punkte -"));

			// Runden Counter
			StringCellValue headerRndCounter = StringCellValue.from(headerPlus).setValue(spielRunde + ". Rnd")
					.zeile(ZWEITE_KOPFDATEN_ZEILE).setEndPosMergeSpaltePlus(1).setColumnWidth(0).setComment(null);
			this.sheetHelper.setTextInCell(headerRndCounter);
		}
		// -------------------------

		// summen spalten
		// DRITTE_KOPFDATEN_ZEILE
		// -------------------------
		StringCellValue headerSumme = StringCellValue.from(sheet, Position.from(0, DRITTE_KOPFDATEN_ZEILE), "+")
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setColumnWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
				.setCellBackColor(headerColor);

		this.sheetHelper.setTextInCell(headerSumme.spalte(ersteSummeSpalte + SPIELE_PLUS_OFFS).setValue("+")
				.setComment("Summe Spiele +").setBorder(borderPlus));
		this.sheetHelper.setTextInCell(headerSumme.spalte(ersteSummeSpalte + SPIELE_MINUS_OFFS).setValue("-")
				.setComment("Summe Spiele -").setBorder(BorderFactory.from().allThin().toBorder()));
		this.sheetHelper.setTextInCell(headerSumme.spalte(ersteSummeSpalte + SPIELE_DIV_OFFS).setValue("Δ")
				.setComment("Summe Spiele Differenz")
				.setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder()));
		this.sheetHelper.setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_PLUS_OFFS).setValue("+")
				.setComment("Summe Punkte +").setBorder(BorderFactory.from().allThin().toBorder()));
		this.sheetHelper.setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_MINUS_OFFS).setValue("-")
				.setComment("Summe Punkte -").setBorder(BorderFactory.from().allThin().toBorder()));
		this.sheetHelper.setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_DIV_OFFS).setValue("Δ")
				.setComment("Summe Punkte Differenz")
				.setBorder(BorderFactory.from().allThin().boldLn().forRight().toBorder()));
		// -------------------------
		// summen spalten
		// ZWEITE_KOPFDATEN_ZEILE
		// -------------------------
		headerSumme = StringCellValue
				.from(sheet, Position.from(ersteSummeSpalte + SPIELE_PLUS_OFFS, ZWEITE_KOPFDATEN_ZEILE), "Spiele")
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setEndPosMergeSpaltePlus(2).setCellBackColor(headerColor);
		this.sheetHelper.setTextInCell(headerSumme
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().doubleLn().forRight().toBorder()));
		this.sheetHelper.setTextInCell(headerSumme.spalte(ersteSummeSpalte + PUNKTE_PLUS_OFFS).setValue("Punkte")
				.setEndPosMergeSpaltePlus(2).setBorder(BorderFactory.from().allThin().boldLn().forRight().toBorder()));
		// -------------------------
		// summen spalten
		// ERSTE_KOPFDATEN_ZEILE
		headerSumme = StringCellValue
				.from(sheet, Position.from(ersteSummeSpalte + SPIELE_PLUS_OFFS, ERSTE_KOPFDATEN_ZEILE), "Summe")
				.setSpalteHoriJustify(CellHoriJustify.CENTER).setEndPosMergeSpaltePlus(5)
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder())
				.setCellBackColor(headerColor);
		this.sheetHelper.setTextInCell(headerSumme);
	}

	public void formatDaten() throws GenerateException {

		IRangliste rangliste = this.ranglisteWkRef.getObject();
		SpielerSpalte spielerSpalte = this.spielerSpalteWkRef.getObject();
		IPropertiesSpalte propertiesSpalte = this.propertiesSpaltewkRef.getObject();

		int anzRunden = rangliste.getAnzahlRunden();
		if (anzRunden < 1) {
			return;
		}

		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.letzteDatenZeile();
		int ersteSummeSpalte = rangliste.getErsteSummeSpalte();

		for (int spielRunde = 1; spielRunde <= anzRunden; spielRunde++) {
			Position posPunktePlusStart = Position.from(
					this.ersteSpielRundeSpalte + ((spielRunde - 1) * this.anzSpaltenInSpielrunde), ersteDatenZeile);
			Position posPunkteMinusEnd = Position.from(posPunktePlusStart).spaltePlusEins().zeile(letzteDatenZeile);
			RangePosition datenRange = RangePosition.from(posPunktePlusStart, posPunkteMinusEnd);
			TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder();
			this.sheetHelper.setPropertyInRange(rangliste.getSheet(), datenRange, TABLE_BORDER2, border);
		}
		// summe spalten spiele
		// +,-,div
		Position posSummeSpieleStart = Position.from(ersteSummeSpalte, ersteDatenZeile);
		Position posSummeSpieleEnd = Position.from(posSummeSpieleStart).spaltePlus(2).zeile(letzteDatenZeile);
		RangePosition summeSpieleRange = RangePosition.from(posSummeSpieleStart, posSummeSpieleEnd);
		TableBorder2 border = BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder();
		this.sheetHelper.setPropertyInRange(rangliste.getSheet(), summeSpieleRange, TABLE_BORDER2, border);

		// summe spalten punkte
		// +,-,div
		Position posSummePunkteStart = Position.from(posSummeSpieleEnd).zeile(ersteDatenZeile).spaltePlusEins();
		Position posSummePunkteEnd = Position.from(posSummePunkteStart).spaltePlus(2).zeile(letzteDatenZeile);
		RangePosition summePunkteRange = RangePosition.from(posSummePunkteStart, posSummePunkteEnd);
		border = BorderFactory.from().allThin().boldLn().forTop().forRight().doubleLn().forLeft().toBorder();
		this.sheetHelper.setPropertyInRange(rangliste.getSheet(), summePunkteRange, TABLE_BORDER2, border);

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		Integer geradeColor = propertiesSpalte.getRanglisteHintergrundFarbeGerade();
		Integer unGeradeColor = propertiesSpalte.getRanglisteHintergrundFarbeUnGerade();

		for (int zeileCntr = ersteDatenZeile; zeileCntr <= letzteDatenZeile; zeileCntr++) {
			RangePosition datenRange = RangePosition.from(0, zeileCntr, posSummePunkteEnd.getSpalte(), zeileCntr);
			if ((zeileCntr & 1) == 0) {
				if (unGeradeColor != null) {
					this.sheetHelper.setPropertyInRange(rangliste.getSheet(), datenRange, CELL_BACK_COLOR,
							unGeradeColor);
				}
			} else {
				if (geradeColor != null) {
					this.sheetHelper.setPropertyInRange(rangliste.getSheet(), datenRange, CELL_BACK_COLOR, geradeColor);
				}
			}
		}
	}
}
