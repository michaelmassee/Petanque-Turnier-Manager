/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.IMeldeliste;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;

public class SpielerSpalte {

	private static final Logger logger = LogManager.getLogger(SpielerSpalte.class);

	private final HashMap<Integer, Position> spielerZeileNummerCache = new HashMap<>();

	public static final int DEFAULT_SPALTE_NUMBER_WIDTH = 700;
	public static final int DEFAULT_SPIELER_NAME_WIDTH = 4000;
	public static final String HEADER_SPIELER_NR = "Nr";
	public static final String HEADER_SPIELER_NAME = "Name";
	private final Formation formation;
	private final int ersteDatenZiele; // Zeile 1 = 0
	private final int spielerNrSpalte; // Spalte A=0, B=1
	private final int spielerNameErsteSpalte;
	private final WeakRefHelper<ISheet> sheet;
	private final WeakRefHelper<IMeldeliste> meldeliste;

	public SpielerSpalte(int ersteDatenZiele, int spielerNrSpalte, ISheet sheet, IMeldeliste meldeliste, Formation formation) {
		checkNotNull(sheet);
		checkNotNull(meldeliste);
		checkArgument(ersteDatenZiele > -1);
		checkArgument(spielerNrSpalte > -1);
		checkNotNull(formation);

		this.ersteDatenZiele = ersteDatenZiele;
		this.spielerNrSpalte = spielerNrSpalte;
		this.spielerNameErsteSpalte = spielerNrSpalte + 1;
		this.sheet = new WeakRefHelper<ISheet>(sheet);
		this.meldeliste = new WeakRefHelper<IMeldeliste>(meldeliste);
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
		return this.sheet.getObject().getSheetHelper();
	}

	private IMeldeliste getMeldeliste() {
		return this.meldeliste.getObject();
	}

	public int getAnzahlSpielerNamenSpalten() {
		switch (this.formation) {
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

		this.sheet.getObject().processBoxinfo("Formatiere Spieler Spalten");

		int letzteDatenZeile = getLetzteDatenZeile();
		if (letzteDatenZeile < this.ersteDatenZiele) {
			// keine Daten
			return;
		}

		// Spieler Nr
		// -------------------------------------
		RangePosition spielrNrdatenRange = RangePosition.from(this.spielerNrSpalte, this.ersteDatenZiele, this.spielerNrSpalte, letzteDatenZeile);

		this.getSheetHelper().setPropertiesInRange(getSheet(), spielrNrdatenRange, CellProperties.from().setVertJustify(CellVertJustify2.CENTER)
				.setCharColor(ColorHelper.CHAR_COLOR_SPIELER_NR).setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
		// -------------------------------------

		// Spieler Namen
		RangePosition datenRange = RangePosition.from(this.spielerNameErsteSpalte, this.ersteDatenZiele, this.spielerNameErsteSpalte, letzteDatenZeile);

		this.getSheetHelper().setPropertiesInRange(getSheet(), datenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER).setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()).setShrinkToFit(true));

	}

	public void insertHeaderInSheet(int headerColor) throws GenerateException {

		this.sheet.getObject().processBoxinfo("Spieler Spalten Header");

		CellProperties columnProperties = CellProperties.from().setWidth(DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue celVal = StringCellValue.from(this.getSheet(), Position.from(this.spielerNrSpalte, this.getErsteDatenZiele() - 1), HEADER_SPIELER_NR)
				.setComment("Meldenummer (manuell nicht ändern)").addColumnProperties(columnProperties).setBorder(BorderFactory.from().allThin().toBorder())
				.setCellBackColor(headerColor);
		this.getSheetHelper().setTextInCell(celVal); // spieler nr

		celVal.addColumnProperties(columnProperties.setWidth(DEFAULT_SPIELER_NAME_WIDTH)).setComment(null).spalte(this.spielerNameErsteSpalte).setValue(HEADER_SPIELER_NAME)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor);

		for (int anzSpieler = 0; anzSpieler < getAnzahlSpielerNamenSpalten(); anzSpieler++) {
			this.getSheetHelper().setTextInCell(celVal);
			celVal.spaltePlusEins();
		}
	}

	public int getLetzteDatenZeile() throws GenerateException {
		return neachsteFreieDatenZeile() - 1;
	}

	/**
	 * funktioniert nach spieler nr<br>
	 * erste zeile = 0
	 *
	 * @throws GenerateException
	 */
	public int neachsteFreieDatenZeile() throws GenerateException {
		for (int zeileCntr = 999; zeileCntr >= this.getErsteDatenZiele(); zeileCntr--) {
			Integer cellNum = this.getSheetHelper().getIntFromCell(this.getSheet(), Position.from(this.spielerNrSpalte, zeileCntr));
			if (cellNum > 0) {
				return zeileCntr + 1;
			}
		}
		return this.getErsteDatenZiele();
	}

	/**
	 * funktioniert nach spieler name<br>
	 * return 0 wenn kein Spieler vorhanden
	 *
	 * @throws GenerateException
	 */
	public int letzteZeileMitSpielerName() throws GenerateException {
		for (int zeileCntr = 999; zeileCntr >= this.getErsteDatenZiele(); zeileCntr--) {
			String cellText = this.getSheetHelper().getTextFromCell(this.getSheet(), Position.from(this.spielerNameErsteSpalte, zeileCntr));
			if (!StringUtils.isBlank(cellText)) {
				return zeileCntr;
			}
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

		// in Cache ?
		Position spielerNrPosAusCache = this.spielerZeileNummerCache.get(spielerNr);
		if (spielerNrPosAusCache != null) {
			// noch korrekt ?
			int spielrNrAusSheet = this.getSheetHelper().getIntFromCell(this.getSheet(), spielerNrPosAusCache);
			if (spielrNrAusSheet == spielerNr) {
				// wert aus cache
				return spielerNrPosAusCache.getZeile();
			}
		}

		// neu suchen in sheet
		for (int zeileCntr = this.getErsteDatenZiele(); zeileCntr < 999; zeileCntr++) {
			Position spielerNrPos = Position.from(this.spielerNrSpalte, zeileCntr);
			String cellText = this.getSheetHelper().getTextFromCell(this.getSheet(), spielerNrPos);
			if (!StringUtils.isBlank(cellText)) {
				if (NumberUtils.toInt(cellText) == spielerNr) {
					this.spielerZeileNummerCache.put(spielerNr, spielerNrPos);
					return zeileCntr;
				}
			}
		}
		return -1;
	}

	public void alleSpieltagSpielerEinfuegen() throws GenerateException {
		// spieler einfuegen wenn nicht vorhanden
		Meldungen meldungen = getMeldeliste().getAktiveUndAusgesetztMeldungen();
		HashSet<Integer> spielerNummerList = new HashSet<>();
		meldungen.spieler().forEach((spieler) -> {
			spielerNummerList.add(spieler.getNr());
		});

		alleSpielerNrEinfuegen(spielerNummerList);
	}

	public void alleSpielerNrEinfuegen(Collection<Integer> spielerNummerList) throws GenerateException {
		NumberCellValue celValSpielerNr = NumberCellValue.from(this.getSheet(), Position.from(this.spielerNrSpalte, getErsteDatenZiele()), 0);

		for (int spielrNummer : spielerNummerList) {
			celValSpielerNr.setValue((double) spielrNummer);
			this.getSheetHelper().setValInCell(celValSpielerNr);
			celValSpielerNr.zeilePlusEins();
		}

		// filldown formula
		String verweisAufMeldeListeFormula = getMeldeliste().formulaSverweisSpielernamen("INDIRECT(ADDRESS(ROW();1;8))");
		StringCellValue strCelValSpielerName = StringCellValue.from(getSheet(), Position.from(this.spielerNrSpalte, getErsteDatenZiele()));
		this.getSheetHelper()
				.setFormulaInCell(strCelValSpielerName.spaltePlusEins().setValue(verweisAufMeldeListeFormula).setFillAutoDown(celValSpielerNr.getPos().getZeile() - 1));
	}

	public void fehlendeSpieltagSpielerEinfuegen() throws GenerateException {
		// spieler einfuegen wenn nicht vorhanden
		Meldungen meldungen = getMeldeliste().getAktiveUndAusgesetztMeldungen();

		for (Spieler spieler : meldungen.spieler()) {
			spielerEinfuegenWennNichtVorhanden(spieler.getNr());
		}
	}

	public void spielerEinfuegenWennNichtVorhanden(int spielerNr) throws GenerateException {
		if (getSpielerZeileNr(spielerNr) == -1) {
			// spieler noch nicht vorhanden
			int freieZeile = neachsteFreieDatenZeile();

			NumberCellValue celValSpielerNr = NumberCellValue.from(this.getSheet(), Position.from(this.spielerNrSpalte, freieZeile), spielerNr);
			this.getSheetHelper().setValInCell(celValSpielerNr);
			String spielrNrAddress = this.getSheetHelper().getAddressFromColumnRow(celValSpielerNr.getPos());
			String verweisAufMeldeListeFormula = getMeldeliste().formulaSverweisSpielernamen(spielrNrAddress);
			StringCellValue strCelVal = StringCellValue.from(celValSpielerNr);
			this.getSheetHelper().setFormulaInCell(strCelVal.setShrinkToFit(true).spaltePlusEins().setValue(verweisAufMeldeListeFormula));
		}
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
		int letzteZeile = this.getLetzteDatenZeile();
		XSpreadsheet sheet = getSheet();

		Position posSpielerNr = Position.from(this.spielerNrSpalte, this.ersteDatenZiele);

		if (letzteZeile >= this.ersteDatenZiele) {
			for (int spielerZeile = this.ersteDatenZiele; spielerZeile <= letzteZeile; spielerZeile++) {
				Integer spielerNr = this.getSheetHelper().getIntFromCell(sheet, posSpielerNr.zeile(spielerZeile));
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
		int letzteZeile = this.getLetzteDatenZeile();
		XSpreadsheet sheet = getSheet();

		Position posSpielerName = Position.from(getSpielerNameErsteSpalte(), this.ersteDatenZiele);

		if (letzteZeile >= this.ersteDatenZiele) {
			for (int spielerZeile = this.ersteDatenZiele; spielerZeile <= letzteZeile; spielerZeile++) {
				String spielerName = this.getSheetHelper().getTextFromCell(sheet, posSpielerName.zeile(spielerZeile));
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
				XCell xCell = this.getSheet().getCellByPosition(this.spielerNrSpalte, zeile);
				return this.getSheetHelper().getAddressFromCellAsString(xCell);
			} catch (IndexOutOfBoundsException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return spielrAdr;
	}

	public String formulaCountSpieler() throws GenerateException {
		String ersteZelle = Position.from(this.spielerNrSpalte, this.ersteDatenZiele).getAddress();
		String letzteZelle = Position.from(this.spielerNrSpalte, getLetzteDatenZeile()).getAddress();

		return "COUNTIF(" + ersteZelle + ":" + letzteZelle + ";\">=0\")"; // nur zahlen
	}

	public int getErsteDatenZiele() {
		return this.ersteDatenZiele;
	}

	public int getSpielerNrSpalte() {
		return this.spielerNrSpalte;
	}

	public int getSpielerNameErsteSpalte() {
		return this.spielerNameErsteSpalte;
	}

	private final XSpreadsheet getSheet() throws GenerateException {
		return this.sheet.getObject().getSheet();
	}
}
