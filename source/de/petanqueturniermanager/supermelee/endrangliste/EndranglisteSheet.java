/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.endrangliste;

import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.helper.sheet.SummenSpalten;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class EndranglisteSheet extends SheetRunner implements IEndRangliste {
	private static final Logger logger = LogManager.getLogger(EndranglisteSheet.class);

	public static final int ERSTE_DATEN_ZEILE = 3; // Zeile 4
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0, B=1
	public static final int RANGLISTE_SPALTE = 2; // Spalte C=2
	public static final int ERSTE_SPIELTAG_SPALTE = 3; // Spalte D=3

	public static final int ERSTE_SORTSPALTE_OFFSET = 3; // zur letzte spalte = anz Spieltage

	public static final String SHEETNAME = "Endrangliste";
	public static final short SHEET_POS = (short) 2;
	public static final String SHEET_COLOR = "d637e8";

	private final SpieltagRanglisteSheet spieltagRanglisteSheet;
	private final SpielerSpalte spielerSpalte;
	private final MeldeListeSheet_New meldeListeSheetNew;
	private final KonfigurationSheet konfigurationSheet;
	private final EndRanglisteFormatter endRanglisteFormatter;
	private final RangListeSpalte rangListeSpalte;

	public EndranglisteSheet(XComponentContext xContext) {
		super(xContext);
		this.konfigurationSheet = new KonfigurationSheet(xContext);
		this.spieltagRanglisteSheet = new SpieltagRanglisteSheet(xContext);
		this.meldeListeSheetNew = new MeldeListeSheet_New(xContext);
		this.spielerSpalte = new SpielerSpalte(xContext, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this,
				this.meldeListeSheetNew, Formation.MELEE);
		this.endRanglisteFormatter = new EndRanglisteFormatter(xContext, this, getAnzSpaltenInSpieltag(),
				this.spielerSpalte, ERSTE_SPIELTAG_SPALTE, this.konfigurationSheet);
		this.rangListeSpalte = new RangListeSpalte(xContext, RANGLISTE_SPALTE, this);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(getxContext(), SHEETNAME).pos(SHEET_POS).tabColor(SHEET_COLOR).setActiv().create()) {
			upDateSheet();
		}
	}

	private void upDateSheet() throws GenerateException {
		Integer headerColor = this.konfigurationSheet.getRanglisteHeaderFarbe();
		spielerEinfügen();
		this.spielerSpalte.insertHeaderInSheet(headerColor);
		this.spielerSpalte.formatDaten();
		this.endRanglisteFormatter.updateHeader();

		spielTageEinfuegen();
		updateEndSummenSpalten();

		this.endRanglisteFormatter.formatDaten();
		this.rangListeSpalte.upDateRanglisteSpalte();
		this.rangListeSpalte.insertHeaderInSheet(headerColor);
		this.endRanglisteFormatter.formatDatenGeradeUngerade();

		// updateAnzSpieltageSpalte();
		// doSort();
		// ranglisteSpalte();
	}

	private void spielerEinfügen() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();

		HashSet<Integer> spielerNummer = new HashSet<>();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			List<Integer> spielerListe = this.spieltagRanglisteSheet.getSpielerNrList(SpielTagNr.from(spieltagCntr));
			spielerNummer.addAll(spielerListe);
		}
		this.spielerSpalte.alleSpielerNrEinfuegen(spielerNummer);
	}

	private void spielTageEinfuegen() throws GenerateException {
		// verwende fill down
		// =WENNNV(SVERWEIS(A4;$'2. Spieltag Rangliste'.$A4:$D1000;4;0);"")

		int anzSpieltage = getAnzahlSpieltage();
		int letzteDatenZeile = this.spielerSpalte.letzteDatenZeile();

		String verweisAufSpalteSpielerNr = "INDIRECT(ADDRESS(ROW();" + (SPIELER_NR_SPALTE + 1) + ";8))";

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			int spieltagSummeErsteSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * getAnzSpaltenInSpieltag());
			Position positionSumme = Position.from(spieltagSummeErsteSpalte, ERSTE_DATEN_ZEILE);
			StringCellValue strVal = StringCellValue.from(getSheet(), positionSumme);

			for (int summeSpalteCntr = 0; summeSpalteCntr < getAnzSpaltenInSpieltag(); summeSpalteCntr++) {
				String verweisAufSummeSpalte = this.spieltagRanglisteSheet.formulaSverweisAufSummeSpalte(
						SpielTagNr.from(spieltagCntr), summeSpalteCntr, verweisAufSpalteSpielerNr);
				strVal.setValue("IFNA(" + verweisAufSummeSpalte + ";\"\")");
				// kein filldown verwenden weil die Adresse in der SVERWEIS hochgezaehlt wird
				// for (int zeile = ERSTE_DATEN_ZEILE; zeile < letzteDatenZeile; zeile++) {
				this.getSheetHelper().setFormulaInCell(strVal.setFillAutoDown(letzteDatenZeile));
				// }
				strVal.spaltePlusEins();
			}
		}
	}

	// private List<SpielerEndranglisteErgebnis> getSpielerEndranglisteErgebnisse() {
	// XSpreadsheet xSheet = getEndranglisteSheet();
	//
	// // Daten zum Sportieren einlesen
	// int ersteSpalteEndsumme = ersteSpalteEndsumme();
	// int letzteZeile = letzteSpielerZeile();
	// List<SpielerEndranglisteErgebnis> spielerEndranglisteErgebnisse = new ArrayList<>();
	// for (int spielerZeilCntr = ERSTE_DATEN_ZEILE; spielerZeilCntr <= letzteZeile; spielerZeilCntr++) {
	// int spielerNr = this.sheetHelper.getIntFromCell(xSheet, SPIELER_NR_SPALTE, spielerZeilCntr);
	// if (spielerNr > -1) {
	// SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(spielerNr);
	// erg.setSpielPlus(this.sheetHelper.getIntFromCell(xSheet, ersteSpalteEndsumme + SPIELE_PLUS_OFFS,
	// spielerZeilCntr));
	// erg.setSpielMinus(this.sheetHelper.getIntFromCell(xSheet, ersteSpalteEndsumme + SPIELE_MINUS_OFFS,
	// spielerZeilCntr));
	// erg.setPunktePlus(this.sheetHelper.getIntFromCell(xSheet, ersteSpalteEndsumme + PUNKTE_PLUS_OFFS,
	// spielerZeilCntr));
	// erg.setPunkteMinus(this.sheetHelper.getIntFromCell(xSheet, ersteSpalteEndsumme + PUNKTE_MINUS_OFFS,
	// spielerZeilCntr));
	//
	// if (erg.isValid()) {
	// spielerEndranglisteErgebnisse.add(erg);
	// }
	// }
	// }
	// return spielerEndranglisteErgebnisse;
	// }
	//
	// private int ersteSortSpalte() {
	// return anzSpielTageSpalte() + ERSTE_SORTSPALTE_OFFSET;
	// }
	//
	// /**
	// * alle sortierbare daten, ohne header !
	// *
	// * @return
	// */
	//
	// private XCellRange getxCellRangeAlleDaten() {
	// XSpreadsheet xSheet = getEndranglisteSheet();
	// XCellRange xCellRange = null;
	// try {
	// if (letzteSpielerZeile() > ERSTE_DATEN_ZEILE) { // daten vorhanden ?
	// // (column, row, column, row)
	// xCellRange = xSheet.getCellRangeByPosition(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, ersteSortSpalte(),
	// letzteSpielerZeile());
	// }
	// } catch (IndexOutOfBoundsException e) {
	// logger.error(e.getMessage(), e);
	// return null;
	// }
	// return xCellRange;
	// }
	//
	// private void doSort() {
	// XSpreadsheet xSheet = getEndranglisteSheet();
	//
	// List<SpielerEndranglisteErgebnis> spielerEndranglisteErgebnisse = getSpielerEndranglisteErgebnisse();
	//
	// // Sortieren
	// spielerEndranglisteErgebnisse.sort(new Comparator<SpielerEndranglisteErgebnis>() {
	// @Override
	// public int compare(SpielerEndranglisteErgebnis o1, SpielerEndranglisteErgebnis o2) {
	// return o1.compareTo(o2);
	// }
	// });
	//
	// // Daten einfuegen
	// for (int ranglistePosition = 0; ranglistePosition < spielerEndranglisteErgebnisse.size(); ranglistePosition++) {
	// int spielerZeile = getSpielerZeileNr(spielerEndranglisteErgebnisse.get(ranglistePosition).getSpielerNr());
	// this.sheetHelper.setTextInCell(xSheet, ersteSortSpalte(), spielerZeile,
	// StringUtils.leftPad("" + (ranglistePosition + 1), 3, '0'));
	// }
	//
	// // spalte zeile (column, row, column, row)
	// XCellRange xCellRange = getxCellRangeAlleDaten();
	//
	// // XCellRange xCellRange = xSheet.getCellRangeByName("A4:AQ93");
	// // XPropertySet xPropSet = UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xCellRange);
	// // try {
	// // xPropSet.setPropertyValue("CellBackColor", Integer.valueOf(Integer.valueOf(0x888888)));
	// // } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
	// // | WrappedTargetException e) {
	// // e.printStackTrace();
	// // }
	//
	// XSortable xSortable = UnoRuntime.queryInterface(XSortable.class, xCellRange);
	//
	// // https://www.openoffice.org/api/docs/common/ref/com/sun/star/util/XSortable-xref.html
	// // https://wiki.openoffice.org/wiki/Documentation/DevGuide/Spreadsheets/Sorting#Table_Sort_Descriptor
	//
	// TableSortField[] aSortFields = new TableSortField[1];
	// TableSortField field1 = new TableSortField();
	// field1.Field = ersteSortSpalte(); // 0 = erste spalte, nur eine Spalte sortieren
	// field1.IsAscending = true; // erste oben
	// // Note – The FieldType member, that is used to select textual or numeric sorting in
	// // text documents is ignored in the spreadsheet application. In a spreadsheet, a cell
	// // always has a known type of text or value, which is used for sorting, with numbers
	// // sorted before text cells.
	// aSortFields[0] = field1;
	//
	// PropertyValue[] aSortDesc = new PropertyValue[2];
	// PropertyValue propVal = new PropertyValue();
	// propVal.Name = "SortFields";
	// propVal.Value = aSortFields;
	// aSortDesc[0] = propVal;
	//
	// aSortDesc[1] = new PropertyValue();
	// aSortDesc[1].Name = "NaturalSort";
	// aSortDesc[1].Value = new Boolean(true);
	//
	// xSortable.sort(aSortDesc);
	// }
	//
	// private void ranglisteSpalte() {
	// XSpreadsheet xSheet = getEndranglisteSheet();
	//
	// int letzteZeile = letzteSpielerZeile();
	// int ersteSpalteEndsumme = ersteSpalteEndsumme();
	//
	// // erste Zeile = 1 = erste Platz
	// this.sheetHelper.setValInCell(xSheet, RANGLISTE_SPALTE, ERSTE_DATEN_ZEILE, 1D);
	//
	// for (int spielerZeilCntr = ERSTE_DATEN_ZEILE + 1; spielerZeilCntr <= letzteZeile; spielerZeilCntr++) {
	//
	// // Rangliste_SPALTE
	//
	// String ranglisteAdressPlusEinPlatz = this.sheetHelper.getAddressFromColumnRow(RANGLISTE_SPALTE,
	// spielerZeilCntr - 1);
	// // mit ein position oben vergleichen
	// String summeSpielGewonnenZelle1 = this.sheetHelper
	// .getAddressFromColumnRow(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, spielerZeilCntr - 1);
	// // Aktuelle pos
	// String summeSpielGewonnenZelle2 = this.sheetHelper
	// .getAddressFromColumnRow(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, spielerZeilCntr);
	//
	// String summeSpielDiffZelle1 = this.sheetHelper
	// .getAddressFromColumnRow(ersteSpalteEndsumme + SPIELE_DIV_OFFS, spielerZeilCntr - 1);
	// String summeSpielDiffZelle2 = this.sheetHelper
	// .getAddressFromColumnRow(ersteSpalteEndsumme + SPIELE_DIV_OFFS, spielerZeilCntr);
	//
	// String punkteDiffZelle1 = this.sheetHelper.getAddressFromColumnRow(ersteSpalteEndsumme + PUNKTE_DIV_OFFS,
	// spielerZeilCntr - 1);
	// String punkteDiffZelle2 = this.sheetHelper.getAddressFromColumnRow(ersteSpalteEndsumme + PUNKTE_DIV_OFFS,
	// spielerZeilCntr);
	// String punkteGewonnenZelle1 = this.sheetHelper
	// .getAddressFromColumnRow(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, spielerZeilCntr - 1);
	// String punkteGewonnenZelle2 = this.sheetHelper
	// .getAddressFromColumnRow(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, spielerZeilCntr);
	//
	// // ' "=IF(AND(I" & currLine & "=I" & currLine-1 & ";F" & currLine & "=F" & currLine-1 & ";G" & currLine &
	// // "=G" & currLine-1 & ");B" & currLine-1 & ";" & teamCntr & ")"
	//
	// // =WENN(UND(AH76=AH77;AJ76=AJ77;AM76=AM77;AK76=AK77);C76;C76 + 1)
	//
	// String formula = "=IF(AND(" + summeSpielGewonnenZelle1 + "=" + summeSpielGewonnenZelle2 + ";"
	// + summeSpielDiffZelle1 + "=" + summeSpielDiffZelle2 + ";" + punkteDiffZelle1 + "="
	// + punkteDiffZelle2 + ";" + punkteGewonnenZelle1 + "=" + punkteGewonnenZelle2 + ");"
	// + ranglisteAdressPlusEinPlatz + ";" + ranglisteAdressPlusEinPlatz + "+1)";
	// this.sheetHelper.setFormulaInCell(xSheet, RANGLISTE_SPALTE, spielerZeilCntr, formula);
	// }
	// }
	//
	// private int anzSpielTageSpalte() {
	// int ersteSpalteEndsumme = ersteSpalteEndsumme();
	// return ersteSpalteEndsumme + ANZAHL_SPALTEN_IN_SUMME;
	// }
	//
	// /**
	// * Anzahl gespielte Spieltage<br>
	// * =ZÄHLENWENN(D4:AG4;"<>")/6
	// */
	// private void updateAnzSpieltageSpalte() {
	// int ersteSpalteEndsumme = ersteSpalteEndsumme();
	// int anzSpieltageSpalte = anzSpielTageSpalte();
	// int letzteSpieltagLetzteSpalte = ersteSpalteEndsumme - 1;
	// int letzteZeile = letzteSpielerZeile();
	//
	// for (int spielerZeilCntr = ERSTE_DATEN_ZEILE; spielerZeilCntr <= letzteZeile; spielerZeilCntr++) {
	// String ersteSpielTagErsteZelle = this.sheetHelper.getAddressFromColumnRow(ERSTE_SPIELTAG_SPALTE,
	// spielerZeilCntr);
	// String letzteSpielTagLetzteZelle = this.sheetHelper.getAddressFromColumnRow(letzteSpieltagLetzteSpalte,
	// spielerZeilCntr);
	//
	// String formula = "=COUNTIF(" + ersteSpielTagErsteZelle + ":" + letzteSpielTagLetzteZelle + ";\"<>\")/"
	// + ANZAHL_SPALTEN_IN_SUMME;
	// this.sheetHelper.setFormulaInCell(getEndranglisteSheet(), anzSpieltageSpalte, spielerZeilCntr, formula);
	// }
	// }
	//
	private void updateEndSummenSpalten() throws GenerateException {
		List<Integer> spielerNrList = this.spielerSpalte.getSpielerNrList();
		for (int spielerNr : spielerNrList) {
			endSummeSpalte(spielerNr);
		}
	}

	private void endSummeSpalte(int spielrNr) throws GenerateException {
		SpielTagNr schlechtesteSpielTag = schlechtesteSpieltag(spielrNr);
		int anzSpieltage = getAnzahlSpieltage();
		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);

		if (anzSpieltage < 2) {
			return;
		}

		StringCellValue[] endsummeFormula = new StringCellValue[ANZAHL_SPALTEN_IN_SUMME];
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
			endsummeFormula[summeSpalteCntr] = StringCellValue.from(getSheet(),
					Position.from(ersteSpalteEndsumme + summeSpalteCntr, spielerZeile));
		}

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			if (schlechtesteSpielTag.getNr() != spieltagCntr) {
				int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
				for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
					Position spielSummeSpalte = Position.from(ersteSpieltagSummeSpalte + summeSpalteCntr, spielerZeile);
					if (!endsummeFormula[summeSpalteCntr].isValueEmpty()) {
						endsummeFormula[summeSpalteCntr].appendValue(" + ");
					}
					endsummeFormula[summeSpalteCntr].appendValue(spielSummeSpalte.getAddress());
				}
			}
		}

		for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
			this.getSheetHelper().setFormulaInCell(endsummeFormula[summeSpalteCntr]);
		}
	}

	private SpielTagNr schlechtesteSpieltag(int spielrNr) throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();
		if (anzSpieltage < 2) {
			return null;
		}
		List<SpielerSpieltagErgebnis> spielerSpieltagErgebniss = spielerErgebnisseEinlesen(spielrNr);
		spielerSpieltagErgebniss.sort(new Comparator<SpielerSpieltagErgebnis>() {
			@Override
			public int compare(SpielerSpieltagErgebnis o1, SpielerSpieltagErgebnis o2) {
				// schlechteste oben
				return o1.reversedCompareTo(o2);
			}
		});
		return spielerSpieltagErgebniss.get(0).getSpielTag();
	}

	private List<SpielerSpieltagErgebnis> spielerErgebnisseEinlesen(int spielrNr) throws GenerateException {
		List<SpielerSpieltagErgebnis> spielerErgebnisse = new ArrayList<>();
		int anzSpieltage = getAnzahlSpieltage();

		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);

		XSpreadsheet sheet = getSheet();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {

			SpielTagNr spielTagNr = SpielTagNr.from(spieltagCntr);

			int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
			// summe vorhanden ?
			String spielPlus = getSheetHelper().getTextFromCell(sheet,
					Position.from(ersteSpieltagSummeSpalte, spielerZeile));
			if (StringUtils.isNotBlank(spielPlus)) {
				SpielerSpieltagErgebnis ergebniss = new SpielerSpieltagErgebnis(spielTagNr, spielrNr);
				ergebniss.setSpielPlus(NumberUtils.toInt(spielPlus));
				ergebniss.setSpielMinus(NumberUtils.toInt(this.getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + SPIELE_MINUS_OFFS, spielerZeile))));
				ergebniss.setPunktePlus(NumberUtils.toInt(this.getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_PLUS_OFFS, spielerZeile))));
				ergebniss.setPunkteMinus(NumberUtils.toInt(this.getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_MINUS_OFFS, spielerZeile))));
				spielerErgebnisse.add(ergebniss);
			} else {
				// nuller spieltag
				SpielerSpieltagErgebnis nullerSpielTag = new SpielerSpieltagErgebnis(spielTagNr, spielrNr);
				// nicht gespielten spieltage sind immer schlechter als gespielte spieltage
				nullerSpielTag.setSpielPlus(-1); // -1 Plus Punkte sind im normalen Spiel nicht möglich
				spielerErgebnisse.add(nullerSpielTag);
			}
		}
		return spielerErgebnisse;
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	public XSpreadsheet getSheet() {
		return getSheetHelper().newIfNotExist(SHEETNAME, SHEET_POS, SHEET_COLOR);
	}

	@Override
	public int getAnzahlSpieltage() throws GenerateException {
		return this.spieltagRanglisteSheet.countNumberOfSpieltage();
	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();
		return ERSTE_SPIELTAG_SPALTE + (anzSpieltage * ANZAHL_SPALTEN_IN_SUMME);
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		// plus 1 spalte fuer spieltag
		return getErsteSummeSpalte() + ANZAHL_SPALTEN_IN_SUMME;
	}

	private int getAnzSpaltenInSpieltag() {
		return SummenSpalten.ANZAHL_SPALTEN_IN_SUMME;
	}

	@Override
	public int letzteDatenZeile() throws GenerateException {
		return this.spielerSpalte.letzteDatenZeile();
	}

	@Override
	public int getErsteDatenZiele() throws GenerateException {
		return ERSTE_DATEN_ZEILE;
	}
}
