/**
 * Erstellung : 10.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.model.IMeldungen;

/**
 * wird in melde undrangliste verwendet
 * 
 * @param <MLD_LIST_TYPE>
 * @param <MLDTYPE>
 */

public class MeldungenSpalte<MLD_LIST_TYPE, MLDTYPE> { // <MLDTYPE> = meldelistetyp

	public static final int MAX_ANZ_MELDUNGEN = 999;

	private static final Logger logger = LogManager.getLogger(MeldungenSpalte.class);

	public static final int DEFAULT_SPALTE_NUMBER_WIDTH = 800;
	private static final String HEADER_SPIELER_NR = "Nr";
	private static final String HEADER_SPIELER_NAME = "Name";
	private final Formation formation;
	private final int ersteDatenZiele; // Zeile 1 = 0
	private final int meldungNrSpalte; // Spalte A=0, B=1
	private final int ersteMeldungNameSpalte; // spalte der erste Spieler im Team
	private final int letzteMeldungNameSpalte; // spalte der letze Spieler im Team
	private final int anzZeilenInHeader; // weiviele Zeilen sollen in header verwendet werden
	private final int spalteMeldungNameWidth;
	private final int minAnzZeilen;

	private final WeakRefHelper<ISheet> sheetWkRef;

	MeldungenSpalte(int ersteDatenZiele, int spielerNrSpalte, ISheet iSheet, Formation formation, int anzZeilenInHeader,
			int spalteMeldungNameWidth, int minAnzZeilen) {
		checkNotNull(iSheet);
		checkArgument(ersteDatenZiele > -1);
		checkArgument(spielerNrSpalte > -1);
		checkArgument(anzZeilenInHeader > 0);
		checkNotNull(formation);
		checkArgument(spalteMeldungNameWidth > 1);

		this.formation = checkNotNull(formation);
		this.ersteDatenZiele = ersteDatenZiele;
		this.anzZeilenInHeader = anzZeilenInHeader;
		this.spalteMeldungNameWidth = spalteMeldungNameWidth;
		this.meldungNrSpalte = spielerNrSpalte;
		this.ersteMeldungNameSpalte = spielerNrSpalte + 1;
		this.letzteMeldungNameSpalte = this.ersteMeldungNameSpalte + this.formation.getAnzSpieler() - 1;
		this.sheetWkRef = new WeakRefHelper<>(iSheet);
		this.minAnzZeilen = minAnzZeilen;
	}

	/**
	 * call getSheetHelper from ISheet<br>
	 * do not assign to Variable, while getter does SheetRunner.testDoCancelTask(); <br>
	 *
	 * @see SheetRunner#getSheetHelper()
	 * @return SheetHelper
	 * @throws GenerateException
	 */
	private SheetHelper getSheetHelper() throws GenerateException {
		return getISheet().getSheetHelper();
	}

	public int getAnzahlSpielerNamenSpalten() {
		return formation.getAnzSpieler();
	}

	/**
	 * Spieler nr und namen spalten formatieren <br>
	 * SpielerNr Gray CHAR_COLOR_GRAY_SPIELER_NR
	 * 
	 * @throws GenerateException
	 */

	public void formatSpielrNrUndNamenspalten() throws GenerateException {

		getISheet().processBoxinfo("Formatiere Meldungen Spalten");

		int letzteDatenZeile = getLetzteDatenZeileUseMin();

		// Spieler Nr
		// -------------------------------------
		RangePosition spielrNrdatenRange = RangePosition.from(meldungNrSpalte, ersteDatenZiele, meldungNrSpalte,
				letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getXSpreadsheet(), spielrNrdatenRange,
				CellProperties.from().centerJustify().setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
		// -------------------------------------

		// Namen
		RangePosition namenDataRange = RangePosition.from(ersteMeldungNameSpalte, ersteDatenZiele,
				letzteMeldungNameSpalte, letzteDatenZeile);

		getSheetHelper().setPropertiesInRange(getXSpreadsheet(), namenDataRange, CellProperties.from().centerJustify()
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()).setShrinkToFit(true));

	}

	public void insertHeaderInSheet(int headerColor) throws GenerateException {

		getISheet().processBoxinfo("Meldungen Spalten Header");

		ColumnProperties columnProperties = ColumnProperties.from().setWidth(DEFAULT_SPALTE_NUMBER_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.margin(MeldeListeKonstanten.CELL_MARGIN);
		StringCellValue celVal = StringCellValue
				.from(getXSpreadsheet(), Position.from(meldungNrSpalte, getErsteDatenZiele() - anzZeilenInHeader),
						HEADER_SPIELER_NR)
				.setComment("Meldenummer (manuell nicht ändern)").addColumnProperties(columnProperties)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor)
				.setVertJustify(CellVertJustify2.CENTER);

		if (anzZeilenInHeader > 1) {
			celVal.setEndPosMergeZeilePlus(1);
		}
		getSheetHelper().setStringValueInCell(celVal); // spieler nr
		// --------------------------------------------------------------------------------------------

		celVal.addColumnProperties(columnProperties.setWidth(spalteMeldungNameWidth)).setComment(null)
				.spalte(ersteMeldungNameSpalte).setValue(HEADER_SPIELER_NAME)
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
	 * rückwärts suche in der Spalte Spiele Nr nach regex ^\d<br>
	 * wenn not found dann erste Daten Zeile.
	 * 
	 * @return Zeile
	 * @throws GenerateException
	 */

	public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
		Map<String, Object> searchProp = new HashMap<>();
		searchProp.put(RangeSearchHelper.SEARCH_BACKWARDS, true);
		Position result = RangeSearchHelper
				.from(getISheet(),
						RangePosition.from(meldungNrSpalte, getErsteDatenZiele(), meldungNrSpalte, MAX_ANZ_MELDUNGEN))
				.searchNachRegExprInSpalte("^\\d", searchProp);
		if (result != null) {
			return result.getZeile();
		}
		return getErsteDatenZiele();
	}

	/**
	 * Sucht von unten nach der erste nicht leere zelle - 1<br>
	 * Spalte spielerNr <br>
	 * Achtung wenn bereits footer daten vorhanden, und oder wietere Daten unter der letzte Spielnr<br>
	 * 
	 * @return
	 * @throws GenerateException
	 */
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return neachsteFreieDatenZeileInSpielerNrSpalte() - 1;
	}

	/**
	 * Letzte Datenzeile incl minimum anzahl von meldungen zielen
	 * 
	 * @return
	 * @throws GenerateException
	 */

	public int getLetzteDatenZeileUseMin() throws GenerateException {
		// erste  Zeile = 0
		int letztDatenZeile = getLetzteMitDatenZeileInSpielerNrSpalte();
		int anzZeilen = letztDatenZeile - ersteDatenZiele + 1;

		if (anzZeilen < minAnzZeilen) {
			letztDatenZeile = ersteDatenZiele + minAnzZeilen - 1; // weil 1 und letzte zeile mit daten
		}

		return letztDatenZeile;

	}

	/**
	 * Sucht von unten nach der erste nicht leere zelle<br>
	 * Spalte spielerNr <br>
	 * Achtung wenn bereits footer daten vorhanden, und oder wietere Daten unter der letzte Spielnr<br>
	 * return ersteDatenzeile wenn kein vorhanden
	 *
	 * @throws GenerateException
	 */
	public int neachsteFreieDatenZeileInSpielerNrSpalte() throws GenerateException {
		Position result = RangeSearchHelper
				.from(getISheet(),
						RangePosition.from(meldungNrSpalte, getErsteDatenZiele(), meldungNrSpalte, MAX_ANZ_MELDUNGEN))
				.searchLastEmptyInSpalte();
		if (result != null) {
			return result.getZeile();
		}
		return getErsteDatenZiele();
	}

	/**
	 * funktioniert nach spieler name<br>
	 * Achtung wenn Footer oder weitere Daten nach der letzte Spielername vorhanden <br>
	 * return 0 wenn kein Spieler vorhanden
	 *
	 * @throws GenerateException
	 */
	public int letzteZeileMitSpielerName() throws GenerateException {
		Position resultFreieZelle = RangeSearchHelper.from(getISheet(), RangePosition.from(ersteMeldungNameSpalte,
				getErsteDatenZiele(), ersteMeldungNameSpalte, MAX_ANZ_MELDUNGEN)).searchLastNotEmptyInSpalte();
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
		Position result = RangeSearchHelper
				.from(getISheet(),
						RangePosition.from(meldungNrSpalte, getErsteDatenZiele(), meldungNrSpalte, MAX_ANZ_MELDUNGEN))
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

	public void alleAktiveUndAusgesetzteMeldungenAusmeldelisteEinfuegen(IMeldeliste<MLD_LIST_TYPE, MLDTYPE> meldeliste)
			throws GenerateException {
		checkNotNull(meldeliste);
		// spieler einfuegen wenn nicht vorhanden
		IMeldungen<MLD_LIST_TYPE, MLDTYPE> meldungen = meldeliste.getAktiveUndAusgesetztMeldungen();
		HashSet<Integer> spielerNummerList = new HashSet<>();
		meldungen.getMeldungen().forEach((meldung) -> {
			spielerNummerList.add(meldung.getNr());
		});
		alleSpielerNrEinfuegen(spielerNummerList, meldeliste);
	}

	public void alleSpielerNrEinfuegen(Collection<Integer> spielerNummerList,
			IMeldeliste<MLD_LIST_TYPE, MLDTYPE> meldeliste) throws GenerateException {
		checkNotNull(meldeliste);
		checkNotNull(spielerNummerList);

		if (spielerNummerList.isEmpty()) {
			return; // nichts zu tun
		}

		RangeData spielrNrData = new RangeData();
		spielerNummerList.stream().forEachOrdered(nr -> {
			spielrNrData.addNewRow().newInt(nr);
		});

		Position startPosNr = Position.from(meldungNrSpalte, getErsteDatenZiele());
		RangeHelper.from(getISheet(), spielrNrData.getRangePosition(startPosNr)).setDataInRange(spielrNrData);

		// filldown formula fuer name
		String verweisAufMeldeListeFormula = meldeliste.formulaSverweisSpielernamen("INDIRECT(ADDRESS(ROW();1;4))");
		StringCellValue strCelValSpielerName = StringCellValue.from(getXSpreadsheet(),
				Position.from(meldungNrSpalte, getErsteDatenZiele()));
		getSheetHelper().setFormulaInCell(strCelValSpielerName.spaltePlusEins().setValue(verweisAufMeldeListeFormula)
				.setFillAutoDown(getLetzteMitDatenZeileInSpielerNrSpalte()));
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
	 * Sucht in der Spalte SpielerNr inclusive alle weitere Daten in der gleiche Spalte nach gültige nummer.
	 * 
	 * @return sorted spielernr
	 * @throws GenerateException
	 */

	public List<Integer> getSpielerNrList() throws GenerateException {
		List<Integer> spielerNrList = new ArrayList<>();
		int letzteZeile = getLetzteMitDatenZeileInSpielerNrSpalte();

		if (letzteZeile >= ersteDatenZiele) {
			RangePosition spielNrRange = RangePosition.from(meldungNrSpalte, ersteDatenZiele, meldungNrSpalte,
					letzteZeile);
			RangeData dataFromRange = RangeHelper.from(sheetWkRef, spielNrRange).getDataFromRange();
			for (RowData zeile : dataFromRange) {
				int spielerNr = zeile.get(0).getIntVal(-1);
				if (spielerNr < 1) {
					break; // fertig
				}
				spielerNrList.add(spielerNr);
			}
		}
		java.util.Collections.sort(spielerNrList);
		return spielerNrList;
	}

	/**
	 * 
	 * @return sorted spielername
	 * @throws GenerateException
	 */
	public List<String> getSpielerNamenList() throws GenerateException {
		List<String> spielerNamen = new ArrayList<>();
		int letzteZeile = getLetzteMitDatenZeileInSpielerNrSpalte();
		XSpreadsheet sheet = getXSpreadsheet();

		Position posSpielerName = Position.from(getErsteMeldungNameSpalte(), ersteDatenZiele);

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
		String letzteZelle = Position.from(meldungNrSpalte, getLetzteMitDatenZeileInSpielerNrSpalte()).getAddress();

		return "COUNTIF(" + ersteZelle + ":" + letzteZelle + ";\">=0\")"; // nur zahlen
	}

	public int getErsteDatenZiele() {
		return ersteDatenZiele;
	}

	public int getSpielerNrSpalte() {
		return meldungNrSpalte;
	}

	public int getErsteMeldungNameSpalte() {
		return ersteMeldungNameSpalte;
	}

	private final XSpreadsheet getXSpreadsheet() throws GenerateException {
		return getISheet().getXSpreadSheet();
	}

	private final ISheet getISheet() {
		return sheetWkRef.get();
	}

	public int getLetzteMeldungNameSpalte() {
		return letzteMeldungNameSpalte;
	}

	public static final Bldr builder() {
		return new Bldr();
	}

	public static class Bldr {

		private static final int DEFAULT_MELDUNG_NAME_WIDTH = 4000;

		private Formation formation;
		private int ersteDatenZiele;
		private int spielerNrSpalte;
		private int anzZeilenInHeader = 1; // default ein zeile
		private int spalteMeldungNameWidth = DEFAULT_MELDUNG_NAME_WIDTH;
		private int minAnzZeilen = 1;

		private ISheet iSheet;

		public Bldr formation(Formation formation) {
			this.formation = checkNotNull(formation);
			return this;
		}

		public Bldr ersteDatenZiele(int ersteDatenZiele) {
			this.ersteDatenZiele = ersteDatenZiele;
			return this;
		}

		public Bldr minAnzZeilen(int minAnzZeilen) {
			this.minAnzZeilen = minAnzZeilen;
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
			this.iSheet = checkNotNull(iSheet);
			return this;
		}

		public <TL, T> MeldungenSpalte<TL, T> build() {
			return new MeldungenSpalte<>(ersteDatenZiele, spielerNrSpalte, iSheet, formation, anzZeilenInHeader,
					spalteMeldungNameWidth, minAnzZeilen);
		}
	}

}
