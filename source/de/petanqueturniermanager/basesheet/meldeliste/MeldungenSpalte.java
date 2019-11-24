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
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SearchHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.model.Meldungen;

public class MeldungenSpalte {

	public static final int MAX_ANZ_MELDUNGEN = 999;

	private static final Logger logger = LogManager.getLogger(MeldungenSpalte.class);

	// private final HashMap<Integer, Position> spielerZeileNummerCache = new HashMap<>();

	public static final int DEFAULT_SPALTE_NUMBER_WIDTH = 700;
	public static final int DEFAULT_SPIELER_NAME_WIDTH = 4000;
	private static final String HEADER_SPIELER_NR = "Nr";
	private static final String HEADER_SPIELER_NAME = "Name";
	private final Formation formation;
	private final int ersteDatenZiele; // Zeile 1 = 0
	private final int meldungNrSpalte; // Spalte A=0, B=1
	private final int meldungNameSpalte;
	private final WeakRefHelper<ISheet> sheet;

	MeldungenSpalte(int ersteDatenZiele, int spielerNrSpalte, ISheet iSheet, Formation formation) {
		checkNotNull(iSheet);
		checkArgument(ersteDatenZiele > -1);
		checkArgument(spielerNrSpalte > -1);
		checkNotNull(formation);

		this.ersteDatenZiele = ersteDatenZiele;
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

		ColumnProperties columnProperties = ColumnProperties.from().setWidth(DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue celVal = StringCellValue.from(getXSpreadsheet(), Position.from(meldungNrSpalte, getErsteDatenZiele() - 1), HEADER_SPIELER_NR)
				.setComment("Meldenummer (manuell nicht ändern)").addColumnProperties(columnProperties).setBorder(BorderFactory.from().allThin().toBorder())
				.setCellBackColor(headerColor);
		getSheetHelper().setStringValueInCell(celVal); // spieler nr

		celVal.addColumnProperties(columnProperties.setWidth(DEFAULT_SPIELER_NAME_WIDTH)).setComment(null).spalte(meldungNameSpalte).setValue(HEADER_SPIELER_NAME)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor);

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
		Position result = SearchHelper.from(getISheet()).searchLastEmptyInSpalte(RangePosition.from(meldungNrSpalte, getErsteDatenZiele(), meldungNrSpalte, MAX_ANZ_MELDUNGEN));
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
		Position resultFreieZelle = SearchHelper.from(getISheet())
				.searchLastNotEmptyInSpalte(RangePosition.from(meldungNameSpalte, getErsteDatenZiele(), meldungNameSpalte, MAX_ANZ_MELDUNGEN));
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
		Position result = SearchHelper.from(getISheet()).searchNachRegExprInSpalte(RangePosition.from(meldungNrSpalte, getErsteDatenZiele(), meldungNrSpalte, MAX_ANZ_MELDUNGEN),
				"^" + spielerNr + "$");
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

	public void alleSpieltagSpielerAusmeldelisteEinfuegen(IMeldeliste meldeliste) throws GenerateException {
		checkNotNull(meldeliste);
		// spieler einfuegen wenn nicht vorhanden
		Meldungen meldungen = meldeliste.getAktiveUndAusgesetztMeldungen();
		HashSet<Integer> spielerNummerList = new HashSet<>();
		meldungen.spieler().forEach((spieler) -> {
			spielerNummerList.add(spieler.getNr());
		});

		alleSpielerNrEinfuegen(spielerNummerList, meldeliste);
	}

	public void alleSpielerNrEinfuegen(Collection<Integer> spielerNummerList, IMeldeliste meldeliste) throws GenerateException {
		checkNotNull(meldeliste);
		checkNotNull(spielerNummerList);

		NumberCellValue celValSpielerNr = NumberCellValue.from(getXSpreadsheet(), Position.from(meldungNrSpalte, getErsteDatenZiele()), 0);

		for (int spielrNummer : spielerNummerList) {
			celValSpielerNr.setValue((double) spielrNummer);
			getSheetHelper().setValInCell(celValSpielerNr);
			celValSpielerNr.zeilePlusEins();
		}

		// filldown formula fuer name
		String verweisAufMeldeListeFormula = meldeliste.formulaSverweisSpielernamen("INDIRECT(ADDRESS(ROW();1;8))");
		StringCellValue strCelValSpielerName = StringCellValue.from(getXSpreadsheet(), Position.from(meldungNrSpalte, getErsteDatenZiele()));
		getSheetHelper().setFormulaInCell(strCelValSpielerName.spaltePlusEins().setValue(verweisAufMeldeListeFormula).setFillAutoDown(celValSpielerNr.getPos().getZeile() - 1));
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

		Position posSpielerNr = Position.from(meldungNrSpalte, ersteDatenZiele);

		if (letzteZeile >= ersteDatenZiele) {
			for (int spielerZeile = ersteDatenZiele; spielerZeile <= letzteZeile; spielerZeile++) {
				Integer spielerNr = getSheetHelper().getIntFromCell(sheet, posSpielerNr.zeile(spielerZeile));
				if (spielerNr > -1) {
					spielerNrList.add(spielerNr);
				}
			}
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
		private Formation formation;
		private int ersteDatenZiele;
		private int spielerNrSpalte;
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

		public Bldr sheet(ISheet iSheet) {
			this.iSheet = iSheet;
			return this;
		}

		public MeldungenSpalte build() {
			return new MeldungenSpalte(ersteDatenZiele, spielerNrSpalte, iSheet, formation);
		}
	}
}
