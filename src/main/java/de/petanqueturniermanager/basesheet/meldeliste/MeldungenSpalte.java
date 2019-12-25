/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SearchHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.IMeldungen;

public class MeldungenSpalte<MLDTYPE> { // <MLDTYPE> = meldelistetyp

	public static final int MAX_ANZ_MELDUNGEN = 999;

	private static final Logger logger = LogManager.getLogger(MeldungenSpalte.class);

	public static final int DEFAULT_SPALTE_NUMBER_WIDTH = 800;
	private static final String HEADER_SPIELER_NR = "Nr";
	private static final String HEADER_SPIELER_NAME = "Name";
	private final Formation formation;
	private final int ersteDatenZiele; // Zeile 1 = 0
	private final int meldungNrSpalte; // Spalte A=0, B=1
	private final int meldungNameSpalte;
	private final int anzZeilenInHeader; // weiviele Zeilen sollen in header verwendet werden
	private final int spalteMeldungNameWidth;

	private final WeakRefHelper<ISheet> sheet;

	MeldungenSpalte(int ersteDatenZiele, int spielerNrSpalte, ISheet iSheet, Formation formation, int anzZeilenInHeader, int spalteMeldungNameWidth) {
		checkNotNull(iSheet);
		checkArgument(ersteDatenZiele > -1);
		checkArgument(spielerNrSpalte > -1);
		checkArgument(anzZeilenInHeader > 0);
		checkNotNull(formation);
		checkArgument(spalteMeldungNameWidth > 1);

		this.ersteDatenZiele = ersteDatenZiele;
		this.anzZeilenInHeader = anzZeilenInHeader;
		this.spalteMeldungNameWidth = spalteMeldungNameWidth;
		meldungNrSpalte = spielerNrSpalte;
		meldungNameSpalte = spielerNrSpalte + 1;
		sheet = new WeakRefHelper<>(iSheet);
		this.formation = formation;
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
		return getISheet().getSheetHelper();
	}

	public int getAnzahlSpielerNamenSpalten() {
		switch (formation) {
		case TETE:
			return 1;
		case DOUBLETTE:
			return 2;
		case TRIPLETTE:
			return 3;
		case MELEE:
			return 1;
		default:
			break;
		}
		return 0;
	}

	public void formatDaten() throws GenerateException {

		getISheet().processBoxinfo("Formatiere Meldungen Spalten");

		int letzteDatenZeile = getLetzteDatenZeile();
		if (letzteDatenZeile < ersteDatenZiele) {
			// keine Daten
			return;
		}

		// Spieler Nr
		// -------------------------------------
		RangePosition spielrNrdatenRange = RangePosition.from(meldungNrSpalte, ersteDatenZiele, meldungNrSpalte, letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getXSpreadsheet(), spielrNrdatenRange, CellProperties.from().setVertJustify(CellVertJustify2.CENTER)
				.setCharColor(ColorHelper.CHAR_COLOR_SPIELER_NR).setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
		// -------------------------------------

		// Spieler Namen
		RangePosition datenRange = RangePosition.from(meldungNameSpalte, ersteDatenZiele, meldungNameSpalte, letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getXSpreadsheet(), datenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER).setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()).setShrinkToFit(true));

	}

	public void insertHeaderInSheet(int headerColor) throws GenerateException {

		getISheet().processBoxinfo("Meldungen Spalten Header");

		ColumnProperties columnProperties = ColumnProperties.from().setWidth(DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER)
				.setVertJustify(CellVertJustify2.CENTER).margin(MeldeListeKonstanten.CELL_MARGIN);
		StringCellValue celVal = StringCellValue.from(getXSpreadsheet(), Position.from(meldungNrSpalte, getErsteDatenZiele() - anzZeilenInHeader), HEADER_SPIELER_NR)
				.setComment("Meldenummer (manuell nicht ändern)").addColumnProperties(columnProperties).setBorder(BorderFactory.from().allThin().toBorder())
				.setCellBackColor(headerColor).setVertJustify(CellVertJustify2.CENTER);

		if (anzZeilenInHeader > 1) {
			celVal.setEndPosMergeZeilePlus(1);
		}
		getSheetHelper().setStringValueInCell(celVal); // spieler nr
		// --------------------------------------------------------------------------------------------

		celVal.addColumnProperties(columnProperties.setWidth(spalteMeldungNameWidth)).setComment(null).spalte(meldungNameSpalte).setValue(HEADER_SPIELER_NAME)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor);

		if (anzZeilenInHeader > 1) {
			// weil spalte sich geändert hat
			celVal.setEndPosMergeZeilePlus(1);
		}

		for (int anzSpieler = 0; anzSpieler < getAnzahlSpielerNamenSpalten(); anzSpieler++) {
			getSheetHelper().setStringValueInCell(celVal);
			celVal.spaltePlusEins();
		}
	}

	/**
	 * funktioniert nach meldung nr
	 *
	 * @return
	 * @throws GenerateException
	 */
	public int getLetzteDatenZeile() throws GenerateException {
		return neachsteFreieDatenOhneSpielerNrZeile() - 1;
	}

	/**
	 * funktioniert nach spieler nr<br>
	 * return ersteDatenzeile wenn kein vorhanden
	 *
	 * @throws GenerateException
	 */
	public int neachsteFreieDatenOhneSpielerNrZeile() throws GenerateException {
		Position result = SearchHelper.from(getISheet(), RangePosition.from(meldungNrSpalte, getErsteDatenZiele(), meldungNrSpalte, MAX_ANZ_MELDUNGEN)).searchLastEmptyInSpalte();
		if (result != null) {
			return result.getZeile();
		}
		return getErsteDatenZiele();
	}

	/**
	 * funktioniert nach spieler name<br>
	 * return 0 wenn kein Spieler vorhanden
	 *
	 * @throws GenerateException
	 */
	public int letzteZeileMitSpielerName() throws GenerateException {
		Position resultFreieZelle = SearchHelper.from(getISheet(), RangePosition.from(meldungNameSpalte, getErsteDatenZiele(), meldungNameSpalte, MAX_ANZ_MELDUNGEN))
				.searchLastNotEmptyInSpalte();
		if (resultFreieZelle != null) {
			return resultFreieZelle.getZeile();
		}
		return 0;
	}

	/**
	 * return -1 wenn not found
	 *
	 * @throws GenerateException
	 */
	public int getSpielerZeileNr(int spielerNr) throws GenerateException {
		checkArgument(spielerNr > 0);
		// muss in komplette spalte wert stehen. Deswegen mit ^ und $
		Position result = SearchHelper.from(getISheet(), RangePosition.from(meldungNrSpalte, getErsteDatenZiele(), meldungNrSpalte, MAX_ANZ_MELDUNGEN))
				.searchNachRegExprInSpalte("^" + spielerNr + "$");
		if (result != null) {
			// Validieren !
			Integer intFromCell = getSheetHelper().getIntFromCell(getXSpreadsheet(), result);
			if (intFromCell != spielerNr) {
				throw new GenerateException("Fehler beim Suchen von Spieler Nr. " + spielerNr + " ResultPos:" + result);
			}
			return result.getZeile();
		}
		return -1;
	}

	public void alleAktiveUndAusgesetzteMeldungenAusmeldelisteEinfuegen(IMeldeliste<MLDTYPE> meldeliste) throws GenerateException {
		checkNotNull(meldeliste);
		// spieler einfuegen wenn nicht vorhanden
		IMeldungen<MLDTYPE> meldungen = meldeliste.getAktiveUndAusgesetztMeldungen();
		HashSet<Integer> spielerNummerList = new HashSet<>();
		meldungen.getMeldungen().forEach((meldung) -> {
			spielerNummerList.add(meldung.getNr());
		});
		alleSpielerNrEinfuegen(spielerNummerList, meldeliste);
	}

	public void alleSpielerNrEinfuegen(Collection<Integer> spielerNummerList, IMeldeliste<MLDTYPE> meldeliste) throws GenerateException {
		checkNotNull(meldeliste);
		checkNotNull(spielerNummerList);

		if (spielerNummerList.isEmpty()) {
			return; // nichts zu tun
		}

		RangeData spielrNrData = new RangeData();
		spielerNummerList.stream().forEachOrdered(nr -> {
			spielrNrData.newRow().newInt(nr);
		});

		Position startPosNr = Position.from(meldungNrSpalte, getErsteDatenZiele());
		RangeHelper.from(getISheet(), spielrNrData.getRangePosition(startPosNr)).setDataInRange(spielrNrData);

		// filldown formula fuer name
		String verweisAufMeldeListeFormula = meldeliste.formulaSverweisSpielernamen("INDIRECT(ADDRESS(ROW();1;4))");
		StringCellValue strCelValSpielerName = StringCellValue.from(getXSpreadsheet(), Position.from(meldungNrSpalte, getErsteDatenZiele()));
		getSheetHelper().setFormulaInCell(strCelValSpielerName.spaltePlusEins().setValue(verweisAufMeldeListeFormula).setFillAutoDown(getLetzteDatenZeile()));
	}

	/**
	 * @param spielrNr aus der meldeliste
	 * @return null when not found
	 * @throws GenerateException
	 */
	public String getSpielrNrAddressNachSpielrNr(int spielrNr) throws GenerateException {
		return getSpielrNrAddressNachZeile(getSpielerZeileNr(spielrNr));
	}

	/**
	 *
	 * @return sorted spielernr
	 * @throws GenerateException
	 */

	public List<Integer> getSpielerNrList() throws GenerateException {
		List<Integer> spielerNrList = new ArrayList<>();
		int letzteZeile = getLetzteDatenZeile();
		XSpreadsheet sheet = getXSpreadsheet();

		// // letzte Zeile ?
		// RangePosition searchRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, 9999);
		// Position lastNotEmptyPos = SearchHelper.from(spieltagSheet, searchRange).searchLastNotEmptyInSpalte();
		//
		// // daten in array einlesen
		// RangePosition spielNrRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, lastNotEmptyPos.getZeile());
		// RangeData dataFromRange = RangeHelper.from(spieltagSheet, spielNrRange).getDataFromRange();
		//
		// for (RowData zeile : dataFromRange) {
		// int spielerNr = zeile.get(0).getIntVal(-1);
		// if (spielerNr < 1) {
		// break; // fertig
		// }
		// spielerNrlist.add(spielerNr);
		// }
		if (letzteZeile >= ersteDatenZiele) {
			RangePosition spielNrRange = RangePosition.from(meldungNrSpalte, ersteDatenZiele, meldungNrSpalte, letzteZeile);
			RangeData dataFromRange = RangeHelper.from(sheet, spielNrRange).getDataFromRange();

			for (RowData zeile : dataFromRange) {
				int spielerNr = zeile.get(0).getIntVal(-1);
				if (spielerNr < 1) {
					break; // fertig
				}
				spielerNrList.add(spielerNr);
			}

			// Position posSpielerNr = Position.from(meldungNrSpalte, ersteDatenZiele);
			// for (int spielerZeile = ersteDatenZiele; spielerZeile <= letzteZeile; spielerZeile++) {
			// Integer spielerNr = getSheetHelper().getIntFromCell(sheet, posSpielerNr.zeile(spielerZeile));
			// if (spielerNr > -1) {
			// spielerNrList.add(spielerNr);
			// }
			// }
		}
		java.util.Collections.sort(spielerNrList);
		return spielerNrList;
	}

	/**
	 *
	 * @return sorted spielrname
	 * @throws GenerateException
	 */
	public List<String> getSpielerNamenList() throws GenerateException {
		List<String> spielerNamen = new ArrayList<>();
		int letzteZeile = getLetzteDatenZeile();
		XSpreadsheet sheet = getXSpreadsheet();

		Position posSpielerName = Position.from(getSpielerNameErsteSpalte(), ersteDatenZiele);

		if (letzteZeile >= ersteDatenZiele) {
			for (int spielerZeile = ersteDatenZiele; spielerZeile <= letzteZeile; spielerZeile++) {
				String spielerName = getSheetHelper().getTextFromCell(sheet, posSpielerName.zeile(spielerZeile));
				if (StringUtils.isNotBlank(spielerName)) {
					spielerNamen.add(spielerName);
				}
			}
		}

		java.util.Collections.sort(spielerNamen);

		return spielerNamen;
	}

	public String getSpielrNrAddressNachZeile(int zeile) throws GenerateException {
		String spielrAdr = null;
		if (zeile > -1) {
			try {
				XCell xCell = getXSpreadsheet().getCellByPosition(meldungNrSpalte, zeile);
				return getSheetHelper().getAddressFromCellAsString(xCell);
			} catch (IndexOutOfBoundsException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return spielrAdr;
	}

	public String formulaCountSpieler() throws GenerateException {
		String ersteZelle = Position.from(meldungNrSpalte, ersteDatenZiele).getAddress();
		String letzteZelle = Position.from(meldungNrSpalte, getLetzteDatenZeile()).getAddress();

		return "COUNTIF(" + ersteZelle + ":" + letzteZelle + ";\">=0\")"; // nur zahlen
	}

	public int getErsteDatenZiele() {
		return ersteDatenZiele;
	}

	public int getSpielerNrSpalte() {
		return meldungNrSpalte;
	}

	public int getSpielerNameErsteSpalte() {
		return meldungNameSpalte;
	}

	private final XSpreadsheet getXSpreadsheet() throws GenerateException {
		return getISheet().getXSpreadSheet();
	}

	private final ISheet getISheet() {
		return sheet.get();
	}

	public static final Bldr Builder() {
		return new Bldr();
	}

	public static class Bldr {

		private static final int DEFAULT_MELDUNG_NAME_WIDTH = 4000;

		private Formation formation;
		private int ersteDatenZiele;
		private int spielerNrSpalte;
		private int anzZeilenInHeader = 1; // default ein zeile
		private int spalteMeldungNameWidth = DEFAULT_MELDUNG_NAME_WIDTH;

		private ISheet iSheet;

		public Bldr formation(Formation formation) {
			this.formation = formation;
			return this;
		}

		public Bldr ersteDatenZiele(int ersteDatenZiele) {
			this.ersteDatenZiele = ersteDatenZiele;
			return this;
		}

		public Bldr spielerNrSpalte(int spielerNrSpalte) {
			this.spielerNrSpalte = spielerNrSpalte;
			return this;
		}

		public Bldr anzZeilenInHeader(int anzZeilenInHeader) {
			this.anzZeilenInHeader = anzZeilenInHeader;
			return this;
		}

		public Bldr spalteMeldungNameWidth(int spalteMeldungNameWidth) {
			this.spalteMeldungNameWidth = spalteMeldungNameWidth;
			return this;
		}

		public Bldr sheet(ISheet iSheet) {
			this.iSheet = iSheet;
			return this;
		}

		public <T> MeldungenSpalte<T> build() {
			return new MeldungenSpalte<>(ersteDatenZiele, spielerNrSpalte, iSheet, formation, anzZeilenInHeader, spalteMeldungNameWidth);
		}
	}
}