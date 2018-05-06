/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.endrangliste;

import static com.google.common.base.Preconditions.*;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.TableSortField;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XSortable;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.IMitSpielerSpalte;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerEndranglisteErgebnis;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractMeldeListeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class EndranglisteSheet extends Thread implements IMitSpielerSpalte, ISheet {

	private static final Logger logger = LogManager.getLogger(EndranglisteSheet.class);

	public static final int ERSTE_DATEN_ZEILE = 3; // Zeile 4
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0, B=1
	public static final int RANGLISTE_SPALTE = 2; // Spalte C=2
	public static final int ERSTE_SPIELTAG_SPALTE = 3; // Spalte D=3

	public static final int ERSTE_SORTSPALTE_OFFSET = 3; // zur letzte spalte = anz Spieltage

	public static final String SHEETNAME = "Endrangliste";

	private final SheetHelper sheetHelper;
	private final SpieltagRanglisteSheet spieltagSumme;
	private final SpielerSpalte spielerSpalte;
	private final AbstractMeldeListeSheet mittelhessenRundeMeldeliste;

	public EndranglisteSheet(XComponentContext xContext) {
		checkNotNull(xContext);
		this.sheetHelper = new SheetHelper(xContext);
		this.spieltagSumme = new SpieltagRanglisteSheet(xContext);
		this.mittelhessenRundeMeldeliste = new MeldeListeSheet_Update(xContext);
		this.spielerSpalte = new SpielerSpalte(xContext, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this,
				this.mittelhessenRundeMeldeliste, Formation.MELEE);
	}

	@Override
	public void run() {
		if (!SheetRunner.isRunning) {
			SheetRunner.isRunning = true;
			try {
				update();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				SheetRunner.isRunning = false;
			}
		}
	}

	private void update() {
		// TODO spieltag fehlt, umstellen auf List mit mehrere Spieltage!!!
		throw new NotImplementedException();
		// this.sheetHelper.setActiveSheet(getEndranglisteSheet());
		// clearAll();
		// int anzSpieltage = this.spieltagSumme.countNumberOfSpieltage();
		//
		// for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
		// // fehlende Spieler einfuegen
		// // TODO spieltag fehlt, umstellen auf List mit mehrere Spieltage!!!
		// List<Integer> spielerListe = this.spieltagSumme.getSpielerNrList(); // 1 = erste Spieltag
		// for (int spielrNr : spielerListe) {
		// spielerEinfuegenWennNichtVorhanden(spielrNr);
		// spielTageEinfuegen(spielrNr, spieltagCntr);
		// }
		// }
		// endSummeSpalten();
		// updateAnzSpieltageSpalte();
		// doSort();
		// ranglisteSpalte();
	}

	@Deprecated
	public XSpreadsheet getEndranglisteSheet() {
		return getSheet();
	}

	public void clearAll() {
		RangePosition range = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, ersteSortSpalte(),
				letzteSpielerZeile());
		this.sheetHelper.clearRange(getEndranglisteSheet(), range);
	}

	@Override
	public List<Integer> getSpielerNrList() {
		List<Integer> spielerNrlist = new ArrayList<>();

		XSpreadsheet sheet = getEndranglisteSheet();
		if (sheet != null) {
			for (int zeileCntr = ERSTE_DATEN_ZEILE; zeileCntr < 999; zeileCntr++) {
				String cellText = this.sheetHelper.getTextFromCell(sheet, Position.from(SPIELER_NR_SPALTE, zeileCntr));
				// Checks if a CharSequence is empty (""), null or whitespace only.
				if (!StringUtils.isBlank(cellText)) {
					if (NumberUtils.isParsable(cellText)) {
						spielerNrlist.add(Integer.parseInt(cellText));
					}
				} else {
					// keine weitere daten
					break;
				}
			}
		}
		return spielerNrlist;
	}

	private List<SpielerEndranglisteErgebnis> getSpielerEndranglisteErgebnisse() {
		XSpreadsheet xSheet = getEndranglisteSheet();

		// Daten zum Sportieren einlesen
		int ersteSpalteEndsumme = ersteSpalteEndsumme();
		int letzteZeile = letzteSpielerZeile();
		List<SpielerEndranglisteErgebnis> spielerEndranglisteErgebnisse = new ArrayList<>();
		for (int spielerZeilCntr = ERSTE_DATEN_ZEILE; spielerZeilCntr <= letzteZeile; spielerZeilCntr++) {
			int spielerNr = this.sheetHelper.getIntFromCell(xSheet, SPIELER_NR_SPALTE, spielerZeilCntr);
			if (spielerNr > -1) {
				SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(spielerNr);
				erg.setSpielPlus(this.sheetHelper.getIntFromCell(xSheet, ersteSpalteEndsumme + SPIELE_PLUS_OFFS,
						spielerZeilCntr));
				erg.setSpielMinus(this.sheetHelper.getIntFromCell(xSheet, ersteSpalteEndsumme + SPIELE_MINUS_OFFS,
						spielerZeilCntr));
				erg.setPunktePlus(this.sheetHelper.getIntFromCell(xSheet, ersteSpalteEndsumme + PUNKTE_PLUS_OFFS,
						spielerZeilCntr));
				erg.setPunkteMinus(this.sheetHelper.getIntFromCell(xSheet, ersteSpalteEndsumme + PUNKTE_MINUS_OFFS,
						spielerZeilCntr));

				if (erg.isValid()) {
					spielerEndranglisteErgebnisse.add(erg);
				}
			}
		}
		return spielerEndranglisteErgebnisse;
	}

	private int ersteSortSpalte() {
		return anzSpielTageSpalte() + ERSTE_SORTSPALTE_OFFSET;
	}

	/**
	 * alle sortierbare daten, ohne header !
	 *
	 * @return
	 */

	private XCellRange getxCellRangeAlleDaten() {
		XSpreadsheet xSheet = getEndranglisteSheet();
		XCellRange xCellRange = null;
		try {
			if (letzteSpielerZeile() > ERSTE_DATEN_ZEILE) { // daten vorhanden ?
				// (column, row, column, row)
				xCellRange = xSheet.getCellRangeByPosition(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, ersteSortSpalte(),
						letzteSpielerZeile());
			}
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		return xCellRange;
	}

	private void doSort() {
		XSpreadsheet xSheet = getEndranglisteSheet();

		List<SpielerEndranglisteErgebnis> spielerEndranglisteErgebnisse = getSpielerEndranglisteErgebnisse();

		// Sortieren
		spielerEndranglisteErgebnisse.sort(new Comparator<SpielerEndranglisteErgebnis>() {
			@Override
			public int compare(SpielerEndranglisteErgebnis o1, SpielerEndranglisteErgebnis o2) {
				return o1.compareTo(o2);
			}
		});

		// Daten einfuegen
		for (int ranglistePosition = 0; ranglistePosition < spielerEndranglisteErgebnisse.size(); ranglistePosition++) {
			int spielerZeile = getSpielerZeileNr(spielerEndranglisteErgebnisse.get(ranglistePosition).getSpielerNr());
			this.sheetHelper.setTextInCell(xSheet, ersteSortSpalte(), spielerZeile,
					StringUtils.leftPad("" + (ranglistePosition + 1), 3, '0'));
		}

		// spalte zeile (column, row, column, row)
		XCellRange xCellRange = getxCellRangeAlleDaten();

		// XCellRange xCellRange = xSheet.getCellRangeByName("A4:AQ93");
		// XPropertySet xPropSet = UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xCellRange);
		// try {
		// xPropSet.setPropertyValue("CellBackColor", Integer.valueOf(Integer.valueOf(0x888888)));
		// } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
		// | WrappedTargetException e) {
		// e.printStackTrace();
		// }

		XSortable xSortable = UnoRuntime.queryInterface(XSortable.class, xCellRange);

		// https://www.openoffice.org/api/docs/common/ref/com/sun/star/util/XSortable-xref.html
		// https://wiki.openoffice.org/wiki/Documentation/DevGuide/Spreadsheets/Sorting#Table_Sort_Descriptor

		TableSortField[] aSortFields = new TableSortField[1];
		TableSortField field1 = new TableSortField();
		field1.Field = ersteSortSpalte(); // 0 = erste spalte, nur eine Spalte sortieren
		field1.IsAscending = true; // erste oben
		// Note – The FieldType member, that is used to select textual or numeric sorting in
		// text documents is ignored in the spreadsheet application. In a spreadsheet, a cell
		// always has a known type of text or value, which is used for sorting, with numbers
		// sorted before text cells.
		aSortFields[0] = field1;

		PropertyValue[] aSortDesc = new PropertyValue[2];
		PropertyValue propVal = new PropertyValue();
		propVal.Name = "SortFields";
		propVal.Value = aSortFields;
		aSortDesc[0] = propVal;

		aSortDesc[1] = new PropertyValue();
		aSortDesc[1].Name = "NaturalSort";
		aSortDesc[1].Value = new Boolean(true);

		xSortable.sort(aSortDesc);
	}

	private void ranglisteSpalte() {
		XSpreadsheet xSheet = getEndranglisteSheet();

		int letzteZeile = letzteSpielerZeile();
		int ersteSpalteEndsumme = ersteSpalteEndsumme();

		// erste Zeile = 1 = erste Platz
		this.sheetHelper.setValInCell(xSheet, RANGLISTE_SPALTE, ERSTE_DATEN_ZEILE, 1D);

		for (int spielerZeilCntr = ERSTE_DATEN_ZEILE + 1; spielerZeilCntr <= letzteZeile; spielerZeilCntr++) {

			// Rangliste_SPALTE

			String ranglisteAdressPlusEinPlatz = this.sheetHelper.getAddressFromColumnRow(RANGLISTE_SPALTE,
					spielerZeilCntr - 1);
			// mit ein position oben vergleichen
			String summeSpielGewonnenZelle1 = this.sheetHelper
					.getAddressFromColumnRow(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, spielerZeilCntr - 1);
			// Aktuelle pos
			String summeSpielGewonnenZelle2 = this.sheetHelper
					.getAddressFromColumnRow(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, spielerZeilCntr);

			String summeSpielDiffZelle1 = this.sheetHelper
					.getAddressFromColumnRow(ersteSpalteEndsumme + SPIELE_DIV_OFFS, spielerZeilCntr - 1);
			String summeSpielDiffZelle2 = this.sheetHelper
					.getAddressFromColumnRow(ersteSpalteEndsumme + SPIELE_DIV_OFFS, spielerZeilCntr);

			String punkteDiffZelle1 = this.sheetHelper.getAddressFromColumnRow(ersteSpalteEndsumme + PUNKTE_DIV_OFFS,
					spielerZeilCntr - 1);
			String punkteDiffZelle2 = this.sheetHelper.getAddressFromColumnRow(ersteSpalteEndsumme + PUNKTE_DIV_OFFS,
					spielerZeilCntr);
			String punkteGewonnenZelle1 = this.sheetHelper
					.getAddressFromColumnRow(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, spielerZeilCntr - 1);
			String punkteGewonnenZelle2 = this.sheetHelper
					.getAddressFromColumnRow(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, spielerZeilCntr);

			// ' "=IF(AND(I" & currLine & "=I" & currLine-1 & ";F" & currLine & "=F" & currLine-1 & ";G" & currLine &
			// "=G" & currLine-1 & ");B" & currLine-1 & ";" & teamCntr & ")"

			// =WENN(UND(AH76=AH77;AJ76=AJ77;AM76=AM77;AK76=AK77);C76;C76 + 1)

			String formula = "=IF(AND(" + summeSpielGewonnenZelle1 + "=" + summeSpielGewonnenZelle2 + ";"
					+ summeSpielDiffZelle1 + "=" + summeSpielDiffZelle2 + ";" + punkteDiffZelle1 + "="
					+ punkteDiffZelle2 + ";" + punkteGewonnenZelle1 + "=" + punkteGewonnenZelle2 + ");"
					+ ranglisteAdressPlusEinPlatz + ";" + ranglisteAdressPlusEinPlatz + "+1)";
			this.sheetHelper.setFormulaInCell(xSheet, RANGLISTE_SPALTE, spielerZeilCntr, formula);
		}
	}

	private int anzSpielTageSpalte() {
		int ersteSpalteEndsumme = ersteSpalteEndsumme();
		return ersteSpalteEndsumme + ANZAHL_SPALTEN_IN_SUMME;
	}

	/**
	 * Anzahl gespielte Spieltage<br>
	 * =ZÄHLENWENN(D4:AG4;"<>")/6
	 */
	private void updateAnzSpieltageSpalte() {
		int ersteSpalteEndsumme = ersteSpalteEndsumme();
		int anzSpieltageSpalte = anzSpielTageSpalte();
		int letzteSpieltagLetzteSpalte = ersteSpalteEndsumme - 1;
		int letzteZeile = letzteSpielerZeile();

		for (int spielerZeilCntr = ERSTE_DATEN_ZEILE; spielerZeilCntr <= letzteZeile; spielerZeilCntr++) {
			String ersteSpielTagErsteZelle = this.sheetHelper.getAddressFromColumnRow(ERSTE_SPIELTAG_SPALTE,
					spielerZeilCntr);
			String letzteSpielTagLetzteZelle = this.sheetHelper.getAddressFromColumnRow(letzteSpieltagLetzteSpalte,
					spielerZeilCntr);

			String formula = "=COUNTIF(" + ersteSpielTagErsteZelle + ":" + letzteSpielTagLetzteZelle + ";\"<>\")/"
					+ ANZAHL_SPALTEN_IN_SUMME;
			this.sheetHelper.setFormulaInCell(getEndranglisteSheet(), anzSpieltageSpalte, spielerZeilCntr, formula);
		}
	}

	private void endSummeSpalten() {
		List<Integer> spielerNrList = getSpielerNrList();
		for (int spielerNr : spielerNrList) {
			endSummeSpalte(spielerNr);
		}
	}

	private int ersteSpalteEndsumme() {
		int anzSpieltage = this.spieltagSumme.countNumberOfSpieltage();
		return ERSTE_SPIELTAG_SPALTE + (anzSpieltage * ANZAHL_SPALTEN_IN_SUMME);
	}

	private void endSummeSpalte(int spielrNr) {
		int schlechtesteSpielTag = schlechtesteSpieltag(spielrNr);
		int anzSpieltage = this.spieltagSumme.countNumberOfSpieltage();
		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);

		if (anzSpieltage < 2) {
			return;
		}

		String[] endsummeFormula = new String[ANZAHL_SPALTEN_IN_SUMME];

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			if (schlechtesteSpielTag != spieltagCntr) {
				int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
				for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
					String spielSummeSpalte = this.sheetHelper
							.getAddressFromColumnRow(ersteSpieltagSummeSpalte + summeSpalteCntr, spielerZeile);
					if (endsummeFormula[summeSpalteCntr] == null) {
						endsummeFormula[summeSpalteCntr] = "=";
					} else {
						endsummeFormula[summeSpalteCntr] += " + ";
					}
					endsummeFormula[summeSpalteCntr] += spielSummeSpalte;
				}
			}
		}

		int ersteSpalteEndsumme = ersteSpalteEndsumme();
		for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
			this.sheetHelper.setFormulaInCell(getEndranglisteSheet(), ersteSpalteEndsumme + summeSpalteCntr,
					spielerZeile, endsummeFormula[summeSpalteCntr]);
		}
	}

	private int schlechtesteSpieltag(int spielrNr) {
		int anzSpieltage = this.spieltagSumme.countNumberOfSpieltage();
		if (anzSpieltage < 2) {
			return 0;
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

	private List<SpielerSpieltagErgebnis> spielerErgebnisseEinlesen(int spielrNr) {
		List<SpielerSpieltagErgebnis> spielerErgebnisse = new ArrayList<>();
		int anzSpieltage = this.spieltagSumme.countNumberOfSpieltage();

		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);

		XSpreadsheet sheet = getSheet();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
			// summe vorhanden ?
			String spielPlus = this.sheetHelper.getTextFromCell(sheet,
					Position.from(ersteSpieltagSummeSpalte, spielerZeile));
			if (StringUtils.isNotBlank(spielPlus)) {
				SpielerSpieltagErgebnis ergebniss = new SpielerSpieltagErgebnis(spieltagCntr, spielrNr);
				ergebniss.setSpielPlus(NumberUtils.toInt(spielPlus));
				ergebniss.setSpielMinus(NumberUtils.toInt(this.sheetHelper.getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + SPIELE_MINUS_OFFS, spielerZeile))));
				ergebniss.setPunktePlus(NumberUtils.toInt(this.sheetHelper.getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_PLUS_OFFS, spielerZeile))));
				ergebniss.setPunkteMinus(NumberUtils.toInt(this.sheetHelper.getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_MINUS_OFFS, spielerZeile))));
				spielerErgebnisse.add(ergebniss);
			} else {
				// nuller spieltag
				spielerErgebnisse.add(new SpielerSpieltagErgebnis(spieltagCntr, spielrNr));
			}
		}
		return spielerErgebnisse;
	}

	private void spielTageEinfuegen(int spielrNr, int spieltag) {

		int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltag - 1) * ANZAHL_SPALTEN_IN_SUMME);

		// Spieltage aktualisieren
		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);
		String spielrNrAdresse = this.spielerSpalte.getSpielrNrAddressNachZeile(spielerZeile);
		String verweisAufSpielPlus = this.spieltagSumme.formulaSverweisAufSpielePlus(spieltag, spielrNrAdresse);
		if (verweisAufSpielPlus != null) {
			this.sheetHelper.setFormulaInCell(getEndranglisteSheet(), ersteSpieltagSummeSpalte, spielerZeile,
					verweisAufSpielPlus);
			// restliche summen spalten einfuegen
			for (int summeSpalteCntr = 1; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
				String verweisAufSummeSpalte = this.spieltagSumme.formulaSverweisAufSummeSpalte(spieltag,
						summeSpalteCntr, spielrNrAdresse);
				if (verweisAufSummeSpalte != null) {
					this.sheetHelper.setFormulaInCell(getEndranglisteSheet(),
							ersteSpieltagSummeSpalte + summeSpalteCntr, spielerZeile, verweisAufSummeSpalte);
				}
			}
		}
	}

	/**
	 * Erste Zeile = 0
	 *
	 * @return
	 */
	public int letzteSpielerZeile() {
		return this.spielerSpalte.neachsteFreieDatenZeile() - 1;
	}

	// Delegates
	// --------------------------
	@Override
	public int neachsteFreieDatenZeile() {
		return this.spielerSpalte.neachsteFreieDatenZeile();
	}

	@Override
	public int getSpielerZeileNr(int spielerNr) {
		return this.spielerSpalte.getSpielerZeileNr(spielerNr);
	}

	@Override
	public void spielerEinfuegenWennNichtVorhanden(int spielerNr) {
		this.spielerSpalte.spielerEinfuegenWennNichtVorhanden(spielerNr);
	}

	@Override
	public int letzteDatenZeile() {
		return this.spielerSpalte.letzteDatenZeile();
	}

	@Override
	public int getErsteDatenZiele() {
		return this.spielerSpalte.getErsteDatenZiele();
	}

	@Override
	public XSpreadsheet getSheet() {
		return this.sheetHelper.newIfNotExist(SHEETNAME, (short) 0);
	}

	@Override
	public List<String> getSpielerNamenList() {
		return this.spielerSpalte.getSpielerNamenList();
	}

}
