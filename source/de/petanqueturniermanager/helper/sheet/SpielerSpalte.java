/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.meldeliste.Formation;
import de.petanqueturniermanager.meldeliste.IMeldeliste;
import de.petanqueturniermanager.model.Meldungen;

public class SpielerSpalte {

	private static final Logger logger = LogManager.getLogger(SpielerSpalte.class);

	private final HashMap<Integer, Position> spielerZeileNummerCache = new HashMap<>();

	public static final int DEFAULT_SPALTE_NUMBER_WIDTH = 700;
	public static final int DEFAULT_SPIELER_NAME_WIDTH = 4000;
	public static final String HEADER_SPIELER_NR = "#";
	public static final String HEADER_SPIELER_NAME = "Name";
	private final Formation formation;
	private final int ersteDatenZiele; // Zeile 1 = 0
	private final int spielerNrSpalte; // Spalte A=0, B=1
	private final int spielerNameErsteSpalte;
	private final WeakRefHelper<ISheet> sheet;
	private final SheetHelper sheetHelper;
	private final WeakRefHelper<IMeldeliste> meldeliste;

	public SpielerSpalte(XComponentContext xContext, int ersteDatenZiele, int spielerNrSpalte, ISheet sheet,
			IMeldeliste meldeliste, Formation formation) {
		checkNotNull(xContext);
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
		this.sheetHelper = new SheetHelper(xContext);
		this.formation = formation;
	}

	private IMeldeliste getMeldeliste() {
		return this.meldeliste.getObject();
	}

	public int anzahlSpielerNamenSpalten() {
		switch (this.formation) {
		case TETE:
			return 1;
		case DOUBLETTE:
			return 2;
		case TRIPLETTE:
			return 3;
		case SUPERMELEE:
			return 1;
		default:
			break;
		}
		return 0;
	}

	public void formatDaten() {
		int letzteDatenZeile = letzteDatenZeile();
		if (letzteDatenZeile < this.ersteDatenZiele) {
			// keine Daten
			return;
		}

		// Spieler Nr
		// -------------------------------------
		RangePosition spielrNrdatenRange = RangePosition.from(this.spielerNrSpalte, this.ersteDatenZiele,
				this.spielerNrSpalte, letzteDatenZeile);

		this.sheetHelper.setPropertiesInRange(getSheet(), spielrNrdatenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER)
						.setCharColor(ColorHelper.CHAR_COLOR_SPIELER_NR)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().forLeft().toBorder()));
		// -------------------------------------

		// Spieler Namen
		RangePosition datenRange = RangePosition.from(this.spielerNameErsteSpalte, this.ersteDatenZiele,
				this.spielerNameErsteSpalte, letzteDatenZeile);

		this.sheetHelper.setPropertiesInRange(getSheet(), datenRange,
				CellProperties.from().setVertJustify(CellVertJustify2.CENTER)
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()));

	}

	public void insertHeaderInSheet(int headerColor) {

		StringCellValue celVal = StringCellValue
				.from(this.getSheet(), Position.from(this.spielerNrSpalte, this.getErsteDatenZiele() - 1),
						HEADER_SPIELER_NR)
				.setComment("Meldenummer (manuell nicht ändern)").setSpalteHoriJustify(CellHoriJustify.CENTER)
				.setSetColumnWidth(DEFAULT_SPALTE_NUMBER_WIDTH).setBorder(BorderFactory.from().allThin().toBorder())
				.setCellBackColor(headerColor);
		this.sheetHelper.setTextInCell(celVal); // spieler nr

		celVal.setSetColumnWidth(DEFAULT_SPIELER_NAME_WIDTH).setComment(null).spalte(this.spielerNameErsteSpalte)
				.setValue(HEADER_SPIELER_NAME).setBorder(BorderFactory.from().allThin().toBorder())
				.setCellBackColor(headerColor);

		for (int anzSpieler = 0; anzSpieler < anzahlSpielerNamenSpalten(); anzSpieler++) {
			this.sheetHelper.setTextInCell(celVal);
			celVal.spaltePlusEins();
		}
	}

	public int letzteDatenZeile() {
		return neachsteFreieDatenZeile() - 1;
	}

	/**
	 * funktioniert nach spieler nr<br>
	 * erste zeile = 0
	 */
	public int neachsteFreieDatenZeile() {
		for (int zeileCntr = 999; zeileCntr >= this.getErsteDatenZiele(); zeileCntr--) {
			String cellText = this.sheetHelper.getTextFromCell(this.getSheet(),
					Position.from(this.spielerNrSpalte, zeileCntr));
			if (!StringUtils.isBlank(cellText)) {
				if (NumberUtils.isParsable(cellText)) {
					return zeileCntr + 1;
				}
			}
		}
		return this.getErsteDatenZiele();
	}

	/**
	 * funktioniert nach spieler name<br>
	 * return 0 wenn kein Spieler vorhanden
	 */
	public int letzteZeileMitSpielerName() {
		for (int zeileCntr = 999; zeileCntr >= this.getErsteDatenZiele(); zeileCntr--) {
			String cellText = this.sheetHelper.getTextFromCell(this.getSheet(),
					Position.from(this.spielerNameErsteSpalte, zeileCntr));
			if (!StringUtils.isBlank(cellText)) {
				return zeileCntr;
			}
		}
		return 0;
	}

	/**
	 * return -1 wenn not found
	 */
	public int getSpielerZeileNr(int spielerNr) {
		checkArgument(spielerNr > 0);

		// in Cache ?
		Position spielerNrPosAusCache = this.spielerZeileNummerCache.get(spielerNr);
		if (spielerNrPosAusCache != null) {
			// noch korrekt ?
			int spielrNrAusSheet = this.sheetHelper.getIntFromCell(this.getSheet(), spielerNrPosAusCache);
			if (spielrNrAusSheet == spielerNr) {
				// wert aus cache
				return spielerNrPosAusCache.getZeile();
			}
		}

		// neu suchen in sheet
		for (int zeileCntr = this.getErsteDatenZiele(); zeileCntr < 999; zeileCntr++) {
			Position spielerNrPos = Position.from(this.spielerNrSpalte, zeileCntr);
			String cellText = this.sheetHelper.getTextFromCell(this.getSheet(), spielerNrPos);
			if (!StringUtils.isBlank(cellText)) {
				if (NumberUtils.toInt(cellText) == spielerNr) {
					this.spielerZeileNummerCache.put(spielerNr, spielerNrPos);
					return zeileCntr;
				}
			}
		}
		return -1;
	}

	public void alleSpieltagSpielerEinfuegen() {
		// spieler einfuegen wenn nicht vorhanden
		Meldungen meldungen = getMeldeliste().getAktiveUndAusgesetztMeldungenAktuellenSpielTag();

		NumberCellValue celValSpielerNr = NumberCellValue.from(this.getSheet(),
				Position.from(this.spielerNrSpalte, getErsteDatenZiele()), 0);

		// =SVERWEIS(INDIREKT(ADRESSE(ZEILE();1;8));Meldeliste.$A3:$B1000;2;0)
		// INDIRECT(ADDRESS(ROW()-1;COLUMN();8))"

		String verweisAufMeldeListeFormula = getMeldeliste()
				.formulaSverweisSpielernamen("INDIRECT(ADDRESS(ROW();1;8))");

		// Hinweis: Fill Down nicht verwenden
		meldungen.spieler().forEach((spieler) -> {
			celValSpielerNr.setValue((double) spieler.getNr());
			this.sheetHelper.setValInCell(celValSpielerNr);
			StringCellValue strCelVal = StringCellValue.from(celValSpielerNr);
			this.sheetHelper.setFormulaInCell(
					strCelVal.spaltePlusEins().setShrinkToFit(true).setValue(verweisAufMeldeListeFormula));
			celValSpielerNr.zeilePlusEins();
		});
	}

	public void fehlendeSpieltagSpielerEinfuegen() {
		// spieler einfuegen wenn nicht vorhanden
		Meldungen meldungen = getMeldeliste().getAktiveUndAusgesetztMeldungenAktuellenSpielTag();

		meldungen.spieler().forEach((spieler) -> {
			spielerEinfuegenWennNichtVorhanden(spieler.getNr());
		});
	}

	public void spielerEinfuegenWennNichtVorhanden(int spielerNr) {
		if (getSpielerZeileNr(spielerNr) == 0) {
			// spieler noch nicht vorhanden
			int freieZeile = neachsteFreieDatenZeile();

			NumberCellValue celValSpielerNr = NumberCellValue.from(this.getSheet(),
					Position.from(this.spielerNrSpalte, freieZeile), spielerNr);
			this.sheetHelper.setValInCell(celValSpielerNr);
			String spielrNrAddress = this.sheetHelper.getAddressFromColumnRow(celValSpielerNr.getPos());
			String verweisAufMeldeListeFormula = getMeldeliste().formulaSverweisSpielernamen(spielrNrAddress);
			StringCellValue strCelVal = StringCellValue.from(celValSpielerNr);
			this.sheetHelper.setFormulaInCell(
					strCelVal.setShrinkToFit(true).spaltePlusEins().setValue(verweisAufMeldeListeFormula));
		}
	}

	/**
	 * @param spielrNr aus der meldeliste
	 * @return null when not found
	 */
	public String getSpielrNrAddressNachSpielrNr(int spielrNr) {
		return getSpielrNrAddressNachZeile(findSpielerZeileNachSpielrNr(spielrNr));
	}

	public List<Integer> getSpielerNrList() {
		List<Integer> spielerNrList = new ArrayList<>();
		int letzteZeile = this.letzteDatenZeile();
		XSpreadsheet sheet = getSheet();

		Position posSpielerNr = Position.from(this.spielerNrSpalte, this.ersteDatenZiele);

		if (letzteZeile >= this.ersteDatenZiele) {
			for (int spielerZeile = this.ersteDatenZiele; spielerZeile <= letzteZeile; spielerZeile++) {
				Integer spielerNr = this.sheetHelper.getIntFromCell(sheet, posSpielerNr.zeile(spielerZeile));
				if (spielerNr > -1) {
					spielerNrList.add(spielerNr);
				}
			}
		}
		return spielerNrList;
	}

	public List<String> getSpielerNamenList() {
		List<String> spielerNamen = new ArrayList<>();
		int letzteZeile = this.letzteDatenZeile();
		XSpreadsheet sheet = getSheet();

		Position posSpielerName = Position.from(getSpielerNameSpalte(), this.ersteDatenZiele);

		if (letzteZeile >= this.ersteDatenZiele) {
			for (int spielerZeile = this.ersteDatenZiele; spielerZeile <= letzteZeile; spielerZeile++) {
				String spielerName = this.sheetHelper.getTextFromCell(sheet, posSpielerName.zeile(spielerZeile));
				if (StringUtils.isNotBlank(spielerName)) {
					spielerNamen.add(spielerName);
				}
			}
		}
		return spielerNamen;
	}

	public String getSpielrNrAddressNachZeile(int zeile) {
		String spielrAdr = null;
		if (zeile > -1) {
			try {
				XCell xCell = this.getSheet().getCellByPosition(this.spielerNrSpalte, zeile);
				return this.sheetHelper.getAddressFromCellAsString(xCell);
			} catch (IndexOutOfBoundsException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return spielrAdr;
	}

	/**
	 * Achtung: die spielrnr spalte kann luecken haben
	 *
	 * @param spielerNr
	 * @return -1 when not found
	 */
	@Deprecated
	public int findSpielerZeileNachSpielrNr(int spielerNr) {
		return getSpielerZeileNr(spielerNr);
	}

	// for (int zeileCntr = this.getErsteDatenZiele(); zeileCntr < 999; zeileCntr++) {
	// // alle durchsuchen
	// String cellText = this.sheetHelper.getTextFromCell(this.getSheet(),
	// Position.from(this.spielerNrSpalte, zeileCntr));
	// if (!StringUtils.isBlank(cellText)) {
	// int nrInCell = NumberUtils.toInt(cellText, -1);
	// if (nrInCell == spielerNr) {
	// return zeileCntr;
	// }
	// }
	// }
	// return -1;
	// }

	public String formulaCountSpieler() {
		String ersteZelle = Position.from(this.spielerNrSpalte, this.ersteDatenZiele).getAddress();
		String letzteZelle = Position.from(this.spielerNrSpalte, letzteDatenZeile()).getAddress();

		return "COUNTIF(" + ersteZelle + ":" + letzteZelle + ";\">=0\")"; // nur zahlen
	}

	public int getErsteDatenZiele() {
		return this.ersteDatenZiele;
	}

	public int getSpielerNrSpalte() {
		return this.spielerNrSpalte;
	}

	public int getSpielerNameSpalte() {
		return this.spielerNameErsteSpalte;
	}

	private final XSpreadsheet getSheet() {
		return this.sheet.getObject().getSheet();
	}
}
