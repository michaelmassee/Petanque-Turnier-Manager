/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static com.google.common.base.Preconditions.*;
import static de.petanqueturniermanager.helper.cellvalue.CellProperties.*;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableSortField;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XSortable;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ErrorMessageBox;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.IEndSummeSpalten;
import de.petanqueturniermanager.helper.sheet.ISpielTagRangliste;
import de.petanqueturniermanager.helper.sheet.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.RanglisteFormatter;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractMeldeListeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.spielrunde.SpielerSpielrundeErgebnis;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Update;

public class SpieltagRanglisteSheet extends SheetRunner implements IEndSummeSpalten, ISpielTagRangliste {

	private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheet.class);

	public static final String KOPFDATEN_SUMME = "Summe";
	public static final String KOPFDATEN_SUMME_SPIELE = "Spiele";
	public static final String KOPFDATEN_SUMME_PUNKTE = "Punkte";

	public static final int RANGLISTE_SPALTE = 2; // Spalte C=2
	public static final int ERSTE_SPIELRUNDE_SPALTE = 3; // Spalte D=3

	public static final int ANZAHL_SPALTEN_IN_SPIELRUNDE = 2;

	public static final int ERSTE_SORTSPALTE_OFFSET = 2; // zur letzte spalte = PUNKTE_DIV_OFFS

	public static final int ERSTE_DATEN_ZEILE = 3; // Zeile 4
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0, B=1
	public static final String SHEETNAME_SUFFIX = "Spieltag Rangliste";
	public static final short SHEET_POS = 2; // an welche Position neu einfuegen

	private final SpielerSpalte spielerSpalte;
	private final AbstractMeldeListeSheet meldeliste;
	private final SpielrundeSheet_Update aktuelleSpielrundeSheet;
	private final RangListeSpalte rangListeSpalte;
	private final RanglisteFormatter ranglisteFormatter;
	private final KonfigurationSheet konfigurationSheet;
	private SpielTagNr spieltagNr = null;

	public SpieltagRanglisteSheet(XComponentContext xContext) {
		super(xContext);
		this.meldeliste = new MeldeListeSheet_Update(xContext);
		this.konfigurationSheet = new KonfigurationSheet(xContext);
		this.spielerSpalte = new SpielerSpalte(xContext, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this, this.meldeliste,
				Formation.MELEE);
		this.aktuelleSpielrundeSheet = new SpielrundeSheet_Update(xContext);
		this.rangListeSpalte = new RangListeSpalte(xContext, RANGLISTE_SPALTE, this);
		this.ranglisteFormatter = new RanglisteFormatter(xContext, this, ANZAHL_SPALTEN_IN_SPIELRUNDE,
				this.spielerSpalte, ERSTE_SPIELRUNDE_SPALTE, this.getKonfigurationSheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		this.spieltagNr = this.getKonfigurationSheet().getAktiveSpieltag();
		generate();
	}

	public void generate() throws GenerateException {
		this.meldeliste.setSpielTag(getSpieltagNr());
		this.aktuelleSpielrundeSheet.setSpielTag(getSpieltagNr());
		// neu erstellen
		Integer headerColor = this.getKonfigurationSheet().getRanglisteHeaderFarbe();

		getSheetHelper().removeSheet(getSheetName(this.getSpieltagNr()));
		XSpreadsheet sheet = getSheet();
		getSheetHelper().setActiveSheet(sheet);
		this.spielerSpalte.alleSpieltagSpielerEinfuegen();
		updateSummenSpalten();
		insertSortValidateSpalte();
		insertManuelsortSpalten();
		this.spielerSpalte.insertHeaderInSheet(headerColor);
		this.spielerSpalte.formatDaten();
		this.ranglisteFormatter.updateHeader();
		this.getRangListeSpalte().upDateRanglisteSpalte();
		this.getRangListeSpalte().insertHeaderInSheet(headerColor);
		this.ranglisteFormatter.formatDaten();
		this.ranglisteFormatter.formatDatenGeradeUngerade();
		ergebnisseEinfuegen();
		nichtgespielteRundenFuellen();
		doSort();
		footer();
	}

	protected void updateSummenSpalten() throws GenerateException {
		int anzSpielRunden = this.aktuelleSpielrundeSheet.countNumberOfSpielRunden(getSpieltagNr());
		if (anzSpielRunden < 1) {
			return;
		}
		XSpreadsheet sheet = getSheet();
		int letzteDatenzeile = this.spielerSpalte.letzteDatenZeile();
		List<Position> plusPunktPos = new ArrayList<>();
		for (int spielRunde = 1; spielRunde <= anzSpielRunden; spielRunde++) {
			plusPunktPos.add(Position.from(ERSTE_SPIELRUNDE_SPALTE + ((spielRunde - 1) * 2), ERSTE_DATEN_ZEILE - 1));
		}

		//@formatter:off
			// =WENN(D4>E4;1;0) + .....
			String formulaSpielePlus = plusPunktPos.stream()
					.map(posPlus -> "IF(" +
									// +1 auf die aktuelle zeile
									posPlus.zeilePlusEins().getAddress() +
									">" +
									// position minus punkte
									Position.from(posPlus).spaltePlusEins().getAddress() +
									";1;0)")
					.collect(Collectors.joining(" + "));

			String formulaSpieleMinus = plusPunktPos.stream()
					.map(posPlus -> "IF(" +
									// position minus punkte
									Position.from(posPlus).spaltePlusEins().getAddress() +
									">" +
									posPlus.getAddress() +
									";1;0)")
					.collect(Collectors.joining(" + "));

			String formulaPunktePlus = plusPunktPos.stream()
					.map(posPlus -> posPlus.getAddress())
					.collect(Collectors.joining(" + "));

			String formulaPunkteMinus = plusPunktPos.stream()
					.map(posPlus -> Position.from(posPlus).spaltePlusEins().getAddress())
					.collect(Collectors.joining(" + "));

			//@formatter:on

		FillAutoPosition fillAutoPosition = FillAutoPosition.from(getErsteSummeSpalte(), letzteDatenzeile);
		StringCellValue valspielSumme = StringCellValue
				.from(sheet, Position.from(getErsteSummeSpalte(), ERSTE_DATEN_ZEILE), formulaSpielePlus)
				.setFillAuto(fillAutoPosition);
		getSheetHelper().setFormulaInCell(valspielSumme);

		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(formulaSpieleMinus)
				.setFillAuto(fillAutoPosition.spaltePlusEins()));
		// div spalte
		String spieleDivFormula = Position.from(valspielSumme.getPos()).spaltePlus(-1).getAddress() + "-"
				+ Position.from(valspielSumme.getPos()).getAddress();
		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(spieleDivFormula)
				.setFillAuto(fillAutoPosition.spaltePlusEins()));

		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(formulaPunktePlus)
				.setFillAuto(fillAutoPosition.spaltePlusEins()));
		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(formulaPunkteMinus)
				.setFillAuto(fillAutoPosition.spaltePlusEins()));
		// div spalte
		String punkteDivFormula = Position.from(valspielSumme.getPos()).spaltePlus(-1).getAddress() + "-"
				+ Position.from(valspielSumme.getPos()).getAddress();
		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(punkteDivFormula)
				.setFillAuto(fillAutoPosition.spaltePlusEins()));
	}

	private void nichtgespielteRundenFuellen() throws GenerateException {

		int anzSpielRunden = this.aktuelleSpielrundeSheet.countNumberOfSpielRunden(getSpieltagNr());
		if (anzSpielRunden < 1) {
			return;
		}
		XSpreadsheet sheet = getSheet();

		int letzteDatenzeile = this.spielerSpalte.letzteDatenZeile();

		int nichtgespieltPlus = this.getKonfigurationSheet().getNichtGespielteRundePlus();
		int nichtgespieltMinus = this.getKonfigurationSheet().getNichtGespielteRundeMinus();

		NumberCellValue punktePlus = NumberCellValue.from(sheet, Position.from(1, 1), nichtgespieltPlus)
				.setComment("Nicht gespielte Runde (" + nichtgespieltPlus + ":" + nichtgespieltMinus + ")");
		NumberCellValue punkteMinus = NumberCellValue.from(punktePlus).setValue((double) nichtgespieltMinus)
				.setComment(null);

		for (int zeileCntr = ERSTE_DATEN_ZEILE; zeileCntr <= letzteDatenzeile; zeileCntr++) {
			for (int spielRunde = 1; spielRunde <= anzSpielRunden; spielRunde++) {
				Position punktePlusPos = Position.from(ERSTE_SPIELRUNDE_SPALTE + ((spielRunde - 1) * 2), zeileCntr);
				if (getSheetHelper().getIntFromCell(sheet, punktePlusPos) < 0) {
					getSheetHelper().setValInCell(punktePlus.setPos(punktePlusPos));
					getSheetHelper().setValInCell(punkteMinus.setPos(punktePlusPos.spaltePlusEins()));
				}
			}
		}
	}

	private void ergebnisseEinfuegen() throws GenerateException {

		XSpreadsheet sheet = getSheet();
		int anzSpielRunden = this.aktuelleSpielrundeSheet.countNumberOfSpielRunden(getSpieltagNr());

		for (int spielRunde = 1; spielRunde <= anzSpielRunden; spielRunde++) {
			// $ = absolute wegen sortieren
			String formulaSheetName = "=$'" + this.aktuelleSpielrundeSheet.getSheetName(getSpieltagNr(), spielRunde)
					+ "'.";
			Position posPunktePlus = Position
					.from(ERSTE_SPIELRUNDE_SPALTE + ((spielRunde - 1) * ANZAHL_SPALTEN_IN_SPIELRUNDE), 0);

			List<SpielerSpielrundeErgebnis> ergebnisse = this.aktuelleSpielrundeSheet.ergebnisseEinlesen(spielRunde)
					.getSpielerSpielrundeErgebnis();

			for (SpielerSpielrundeErgebnis ergebnis : ergebnisse) {

				int spielerNr = ergebnis.getSpielerNr();
				// zeile finden
				int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielerNr);
				if (spielerZeile >= ERSTE_DATEN_ZEILE) {
					Position posSpielerPunktePlus = Position.from(posPunktePlus).zeile(spielerZeile);
					// Formula auf plus punkte eintragen
					StringCellValue spielerPlus = StringCellValue.from(sheet, posSpielerPunktePlus,
							formulaSheetName + ergebnis.getPositionPlusPunkte().getAddressWith$());
					getSheetHelper().setFormulaInCell(spielerPlus);
					// Formula auf minus punkte eintragen
					StringCellValue spielerMinus = StringCellValue.from(spielerPlus).spaltePlusEins()
							.setValue(formulaSheetName + ergebnis.getPositionMinusPunkte().getAddressWith$());
					getSheetHelper().setFormulaInCell(spielerMinus);
				}
			}
		}
	}

	private void footer() throws GenerateException {

		int ersteFooterZeile = this.spielerSpalte.neachsteFreieDatenZeile();
		StringCellValue stringVal = StringCellValue
				.from(this.getSheet(), Position.from(SPIELER_NR_SPALTE, ersteFooterZeile)).setCharHeight(7)
				.setHoriJustify(CellHoriJustify.LEFT);

		String anzSpielerFormula = "\"Anzahl Spieler: \" & " + this.spielerSpalte.formulaCountSpieler();
		getSheetHelper().setFormulaInCell(stringVal.setValue(anzSpielerFormula));

		getSheetHelper().setTextInCell(stringVal.zeilePlusEins().setValue(
				"Reihenfolge zur Ermittlung der Platzierung: 1. Spiele +, 2. Spiele Δ, 3. Punkte Δ, 4. Punkte +"));

		getSheetHelper().setRowProperty(this.getSheet(), stringVal.getPos().getZeile(), HEIGHT, 300);

		int nichtgespieltPlus = this.getKonfigurationSheet().getNichtGespielteRundePlus();
		int nichtgespieltMinus = this.getKonfigurationSheet().getNichtGespielteRundeMinus();
		getSheetHelper().setTextInCell(stringVal.zeilePlusEins().setValue(
				"Nicht gespielten Runden werden mit " + nichtgespieltPlus + ":" + nichtgespieltMinus + " gewertet"));
		getSheetHelper().setRowProperty(this.getSheet(), stringVal.getPos().getZeile(), HEIGHT, 300);

	}

	public String getSheetName(SpielTagNr spieltagNr) throws GenerateException {
		return spieltagNr.getNr() + ". " + SHEETNAME_SUFFIX;
	}

	@Deprecated
	public XSpreadsheet getSpieltagSheet() throws GenerateException {
		return getSheet();
	}

	/**
	 * @param spieltagNr = welchen Spieltag ?
	 * @param spielrNrAdresse = die Adresse vom Spielrnr im Anfragende Sheet
	 * @return null when not found <br>
	 * =VLOOKUP<br>
	 * =SVERWEIS(A65;$'5. Spieltag Summe'.$A3:$Q1000;12;0)
	 * @throws GenerateException
	 *
	 */
	public String formulaSverweisAufSpielePlus(SpielTagNr spieltagNr, String spielrNrAdresse) throws GenerateException {
		return formulaSverweisAufSummeSpalte(spieltagNr, 0, spielrNrAdresse);
	}

	/**
	 *
	 * @param spieltagNr = welchen Spieltag ?
	 * @param summeSpalte = erste spalte = 0 = SpielePlusSpalte
	 * @param spielrNrAdresse = die Adresse vom Spielrnr im Anfragende Sheet
	 * @return null when not found <br>
	 * =VLOOKUP<br>
	 * =SVERWEIS(A65;$'5. Spieltag Summe'.$A3:$Q1000;12;0)
	 * @throws GenerateException
	 *
	 */
	public String formulaSverweisAufSummeSpalte(SpielTagNr spieltagNr, int summeSpalte, String spielrNrAdresse)
			throws GenerateException {
		int ersteSummeSpalte = getErsteSummeSpalte(spieltagNr);

		if (ersteSummeSpalte > -1) {
			// gefunden
			int returnSpalte = ersteSummeSpalte + summeSpalte;

			String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
			String letzteZelleAddress = Position.from(returnSpalte, 999).getAddressWith$();

			return "VLOOKUP(" + spielrNrAdresse + ";$'" + getSheetName(spieltagNr) + "'." + ersteZelleAddress + ":"
					+ letzteZelleAddress + ";" + (returnSpalte + 1) + // erste spalte = 1
					";0)";
		}
		return null;
	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		return getErsteSummeSpalte(getSpieltagNr());
	}

	public int getErsteSummeSpalte(SpielTagNr spieltag) throws GenerateException {
		checkNotNull(spieltag);
		int anzSpielRunden = this.aktuelleSpielrundeSheet.countNumberOfSpielRunden(spieltag);
		return ERSTE_SPIELRUNDE_SPALTE + (anzSpielRunden * 2);
	}

	// /**
	// * @return
	// * @throws GenerateException
	// */
	// @Override
	// public List<Integer> getSpielerNrList() throws GenerateException {
	// return getSpielerNrList(getSpieltagNr());
	// }

	public List<Integer> getSpielerNrList(SpielTagNr spielTagNr) throws GenerateException {
		checkNotNull(spielTagNr);

		List<Integer> spielerNrlist = new ArrayList<>();

		XSpreadsheet spieltagSheet = getSheet(spielTagNr);
		if (spieltagSheet != null) {
			for (int zeileCntr = ERSTE_DATEN_ZEILE; zeileCntr < 999; zeileCntr++) {
				String cellText = this.getSheetHelper().getTextFromCell(spieltagSheet,
						Position.from(SPIELER_NR_SPALTE, zeileCntr));
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

	public int countNumberOfSpieltage() {
		int anz = 0;

		XSpreadsheets sheets = this.getSheetHelper().getSheets();

		if (sheets != null && sheets.hasElements()) {
			String[] sheetNames = this.getSheetHelper().getSheets().getElementNames();
			for (String sheetName : sheetNames) {
				if (sheetName.contains(SHEETNAME_SUFFIX)) {
					anz++;
				}
			}
		}
		return anz;
	}

	private int letzteDatenSpalte() throws GenerateException {
		int ersteSummeSpalte = getErsteSummeSpalte();
		return ersteSummeSpalte + PUNKTE_DIV_OFFS;
	}

	/**
	 * spalte mit sortierdaten rangliste
	 *
	 * @return
	 * @throws GenerateException
	 */
	private int sortSpalte() throws GenerateException {
		return letzteDatenSpalte() + ERSTE_SORTSPALTE_OFFSET;
	}

	private int validateSpalte() throws GenerateException {
		return sortSpalte() + 5;
	}

	public List<SpielerSpieltagErgebnis> spielTagErgebnisseEinlesen() throws GenerateException {
		List<SpielerSpieltagErgebnis> spielTagErgebnisse = new ArrayList<>();

		SpielTagNr spieltagNr = getSpieltagNr();

		for (int spielerNr : this.spielerSpalte.getSpielerNrList()) {
			SpielerSpieltagErgebnis erg = spielerErgebnisseEinlesen(spieltagNr, spielerNr);
			if (erg != null) {
				spielTagErgebnisse.add(erg);
			}
		}

		return spielTagErgebnisse;
	}

	public SpielerSpieltagErgebnis spielerErgebnisseEinlesen(SpielTagNr spieltag, int spielrNr)
			throws GenerateException {
		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);

		if (spielerZeile < ERSTE_DATEN_ZEILE) {
			return null;
		}

		XSpreadsheet spieltagSheet = getSheet();
		if (spieltagSheet == null) {
			return null;
		}

		int ersteSpieltagSummeSpalte = getErsteSummeSpalte();
		Position spielePlusSumme = Position.from(ersteSpieltagSummeSpalte, spielerZeile);
		SpielerSpieltagErgebnis erg = SpielerSpieltagErgebnis.from(spieltag, spielrNr);

		erg.setSpielPlus(this.getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme))
				.setPosSpielPlus(spielePlusSumme);

		erg.setSpielMinus(this.getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlusEins()))
				.setPosSpielMinus(spielePlusSumme);

		erg.setPunktePlus(this.getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlus(2)))
				.setPosPunktePlus(spielePlusSumme);

		erg.setPunkteMinus(this.getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlusEins()))
				.setPosPunkteMinus(spielePlusSumme);

		return erg;
	}

	private void insertSortValidateSpalte() throws GenerateException {

		XSpreadsheet sheet = getSheet();

		StringCellValue validateHeader = StringCellValue
				.from(sheet, Position.from(validateSpalte(), ERSTE_DATEN_ZEILE - 1)).setComment("Validate Spalte")
				.setColumnWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setSpalteHoriJustify(CellHoriJustify.CENTER)
				.setValue("Err");

		this.getSheetHelper().setTextInCell(validateHeader);

		// formula zusammenbauen
		// --------------------------------------------------------------------------
		// SummenSpalten
		int letzteZeile = this.spielerSpalte.letzteDatenZeile();
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		int ersteZeile = this.spielerSpalte.getErsteDatenZiele();

		StringCellValue platzPlatzEins = StringCellValue.from(getSheet(), Position.from(validateSpalte(), ersteZeile),
				"x");

		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ersteZeile);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ersteZeile);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ersteZeile);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ersteZeile);

		// 1 = 1 zeile oben
		// 2 = aktuelle zeile
		// if (a2>a1) {
		// ERR
		// }
		// if (a1==a2 && b2>b1 ) {
		// ERR
		// }
		// if (a1==a2 && b1==b2 && c2>c1 ) {
		// ERR
		// }
		// if (a1==a2 && b1==b2 && c1==c2 && d2>d1) {
		// ERR
		// }

		//@formatter:off
//		String formula = "IF(ROW()=" + (ersteZeile + 1) + ";\"\";" // erste zeile ignorieren
		String formula = "IF(" + compareFormula(summeSpielGewonnenZelle1,">") + ";\"X1\";\"\")"
				// ----------------
				+ " & IF(AND("
				+ compareFormula(summeSpielGewonnenZelle1,"=")
				+ ";" + compareFormula(summeSpielDiffZelle1,">")
				+ ")"
				+ ";\"X2\";\"\")"
				// ----------------
				+ " & IF(AND("
				+ compareFormula(summeSpielGewonnenZelle1,"=")
				+ ";" + compareFormula(summeSpielDiffZelle1,"=")
				+ ";" + compareFormula(punkteDiffZelle1,">")
				+ ")"
				+ ";\"X3\";\"\")"
				// ----------------
				+ " & IF(AND("
				+ compareFormula(summeSpielGewonnenZelle1,"=")
				+ ";" + compareFormula(summeSpielDiffZelle1,"=")
				+ ";" + compareFormula(punkteDiffZelle1,"=")
				+ ";" + compareFormula(punkteGewonnenZelle1,">")
				+ ")"
				+ ";\"X4\";\"\")"
				;
		//@formatter:on

		// erste Zelle wert
		FillAutoPosition fillAutoPosition = FillAutoPosition.from(platzPlatzEins.getPos()).zeile(letzteZeile);
		this.getSheetHelper()
				.setFormulaInCell(platzPlatzEins.setValue(formula).zeile(ersteZeile).setFillAuto(fillAutoPosition));

		// Alle Nummer Bold
		this.getSheetHelper().setPropertiesInRange(getSheet(),
				RangePosition.from(platzPlatzEins.getPos(), fillAutoPosition),
				CellProperties.from().setCharWeight(FontWeight.BOLD).setCharColor(ColorHelper.CHAR_COLOR_RED));
		// --------------------------------------------------------------------------
	}

	/**
	 * vergleiche wert in aktuelle zeile mit eine zeile oben<br>
	 * 1 = 1 zeile oben zeile -1 <br>
	 * 2 = aktuelle zeile
	 *
	 * @param pos
	 * @return a2>a1
	 */
	private String compareFormula(Position pos, String operator) {
		return pos.getAddress() + operator + Position.from(pos).zeilePlus(-1).getAddress();
	}

	public boolean isErrorInSheet() throws GenerateException {
		XSpreadsheet sheet = getSheet();
		int validateSpalte = validateSpalte();
		Position valSpalteStart = Position.from(validateSpalte, ERSTE_DATEN_ZEILE);
		Position valSpalteEnd = Position.from(validateSpalte, this.spielerSpalte.letzteDatenZeile());

		for (int zeile = valSpalteStart.getZeile(); zeile <= valSpalteEnd.getZeile(); zeile++) {
			String text = this.getSheetHelper().getTextFromCell(sheet, Position.from(validateSpalte, zeile));
			if (StringUtils.isNoneBlank(text)) {
				// error
				ErrorMessageBox errMsg = new ErrorMessageBox(getxContext());
				errMsg.showOk("Fehler in Spieltagrangliste", "Fehler in Zeile " + zeile);
				return true;
			}
		}
		return false;
	}

	private void insertManuelsortSpalten() throws GenerateException {
		// sortspalten for manuell sortieren

		StringCellValue sortlisteVal = StringCellValue
				.from(getSheet(), Position.from(sortSpalte(), ERSTE_DATEN_ZEILE - 1))
				.setColumnWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setSpalteHoriJustify(CellHoriJustify.CENTER);

		int ersteSpalteEndsumme = getErsteSummeSpalte();
		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ERSTE_DATEN_ZEILE);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ERSTE_DATEN_ZEILE);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ERSTE_DATEN_ZEILE);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ERSTE_DATEN_ZEILE);

		StringCellValue sortSpalte = StringCellValue.from(sortlisteVal).zeile(ERSTE_DATEN_ZEILE);
		StringCellValue headerVal = StringCellValue.from(sortlisteVal).zeile(ERSTE_DATEN_ZEILE - 1);

		this.getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("S+"));
		this.getSheetHelper().setFormulaInCell(sortSpalte.spaltePlusEins().setFillAutoDown(letzteDatenZeile())
				.setValue(summeSpielGewonnenZelle1.getAddress()));

		this.getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("SΔ"));
		this.getSheetHelper().setFormulaInCell(sortSpalte.spaltePlusEins().setFillAutoDown(letzteDatenZeile())
				.setValue(summeSpielDiffZelle1.getAddress()));

		this.getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("PΔ"));
		this.getSheetHelper().setFormulaInCell(sortSpalte.spaltePlusEins().setFillAutoDown(letzteDatenZeile())
				.setValue(punkteDiffZelle1.getAddress()));

		this.getSheetHelper().setTextInCell(headerVal.spaltePlusEins().setValue("P+"));
		this.getSheetHelper().setFormulaInCell(sortSpalte.spaltePlusEins().setFillAutoDown(letzteDatenZeile())
				.setValue(punkteGewonnenZelle1.getAddress()));

	}

	/**
	 * mit bestehende spalten im sheet selbst sortieren.<br>
	 * sortieren in 2 schritten
	 *
	 * @throws GenerateException
	 */
	protected void doSort() throws GenerateException {

		int ersteSpalteEndsumme = getErsteSummeSpalte();
		Position summeSpielGewonnenZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_PLUS_OFFS, ERSTE_DATEN_ZEILE);
		Position summeSpielDiffZelle1 = Position.from(ersteSpalteEndsumme + SPIELE_DIV_OFFS, ERSTE_DATEN_ZEILE);
		Position punkteDiffZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_DIV_OFFS, ERSTE_DATEN_ZEILE);
		Position punkteGewonnenZelle1 = Position.from(ersteSpalteEndsumme + PUNKTE_PLUS_OFFS, ERSTE_DATEN_ZEILE);

		// spalte zeile (column, row, column, row)
		XCellRange xCellRange = getxCellRangeAlleDaten();

		if (xCellRange == null) {
			return;
		}

		XSortable xSortable = UnoRuntime.queryInterface(XSortable.class, xCellRange);

		// nur 3 felder sind sortierbar !, deswegen die 4 spalte zuerst einzel sortieren.
		// wenn einzel sortieren dann von letzte bis erst spalte !
		{
			TableSortField[] aSortFields = new TableSortField[1];
			TableSortField field4 = new TableSortField();
			field4.Field = punkteGewonnenZelle1.getSpalte(); // 0 = erste spalte
			field4.IsAscending = false; // false= meiste Punkte oben
			aSortFields[0] = field4;

			PropertyValue[] sortDescr = sortDescr(aSortFields);
			xSortable.sort(sortDescr);
		}

		{
			TableSortField[] aSortFields = new TableSortField[3]; // MAX 3 felder
			TableSortField field1 = new TableSortField();
			field1.Field = summeSpielGewonnenZelle1.getSpalte();
			field1.IsAscending = false; // false = meiste Punkte oben
			aSortFields[0] = field1;

			TableSortField field2 = new TableSortField();
			field2.Field = summeSpielDiffZelle1.getSpalte(); // 0 = erste spalte
			field2.IsAscending = false; // false =meiste Punkte oben
			aSortFields[1] = field2;

			TableSortField field3 = new TableSortField();
			field3.Field = punkteDiffZelle1.getSpalte(); // 0 = erste spalte
			field3.IsAscending = false; // false = meiste Punkte oben
			aSortFields[2] = field3;

			PropertyValue[] sortDescr = sortDescr(aSortFields);
			xSortable.sort(sortDescr);
		}

	}

	private PropertyValue[] sortDescr(TableSortField[] sortFields) {

		PropertyValue[] aSortDesc = new PropertyValue[2];
		PropertyValue propVal = new PropertyValue();
		propVal.Name = "SortFields";
		propVal.Value = sortFields;
		aSortDesc[0] = propVal;

		// specifies if cell formats are moved with the contents they belong to.
		aSortDesc[1] = new PropertyValue();
		aSortDesc[1].Name = "BindFormatsToContent";
		aSortDesc[1].Value = false;

		return aSortDesc;
	}

	/**
	 * alle sortierbare daten, ohne header !
	 *
	 * @return
	 * @throws GenerateException
	 */
	private XCellRange getxCellRangeAlleDaten() throws GenerateException {
		XSpreadsheet sheet = getSheet();
		int letzteDatenzeile = this.spielerSpalte.letzteDatenZeile();
		XCellRange xCellRange = null;
		try {
			if (letzteDatenzeile > ERSTE_DATEN_ZEILE) { // daten vorhanden ?
				// (column, row, column, row)
				xCellRange = sheet.getCellRangeByPosition(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, sortSpalte(),
						letzteDatenzeile);
			}
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xCellRange;
	}

	public void clearAll() throws GenerateException {
		int letzteDatenzeile = this.spielerSpalte.letzteDatenZeile();
		if (letzteDatenzeile >= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			RangePosition range = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, sortSpalte(),
					letzteDatenzeile);
			this.getSheetHelper().clearRange(getSpieltagSheet(), range);
		}
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}
	// Delegates
	// --------------------------

	@Override
	public int letzteDatenZeile() throws GenerateException {
		return this.spielerSpalte.letzteDatenZeile();
	}

	@Override
	public int getErsteDatenZiele() {
		return this.spielerSpalte.getErsteDatenZiele();
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheet(getSpieltagNr());
	}

	public XSpreadsheet getSheet(SpielTagNr spielTagNr) throws GenerateException {
		return this.getSheetHelper().newIfNotExist(getSheetName(spielTagNr), SHEET_POS);
	}

	public RangListeSpalte getRangListeSpalte() {
		return this.rangListeSpalte;
	}

	@Override
	public int getAnzahlRunden() throws GenerateException {
		return this.aktuelleSpielrundeSheet.countNumberOfSpielRunden(getSpieltagNr());
	}

	public SpielTagNr getSpieltagNr() throws GenerateException {
		return this.spieltagNr;
	}

	public void setSpieltagNr(SpielTagNr spieltagNr) {
		this.spieltagNr = spieltagNr;
	}

	protected KonfigurationSheet getKonfigurationSheet() {
		return this.konfigurationSheet;
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		return getErsteSummeSpalte() + ANZAHL_SPALTEN_IN_SUMME - 1;
	}
}
