/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static com.google.common.base.Preconditions.*;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.rangliste.ISpielTagRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.rangliste.RanglisteFormatter;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.IEndSummeSpalten;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractSupermeleeMeldeListeSheet;
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

	private final SpielerSpalte spielerSpalte;
	private final AbstractSupermeleeMeldeListeSheet meldeliste;
	private final SpielrundeSheet_Update aktuelleSpielrundeSheet;
	private final RangListeSpalte rangListeSpalte;
	private final RanglisteFormatter ranglisteFormatter;
	private final KonfigurationSheet konfigurationSheet;
	private final RangListeSorter rangListeSorter;
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
		this.rangListeSorter = new RangListeSorter(xContext, this);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpieltagNr(this.getKonfigurationSheet().getAktiveSpieltag());
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
		this.rangListeSorter.insertSortValidateSpalte();
		this.rangListeSorter.insertManuelsortSpalten();
		this.spielerSpalte.insertHeaderInSheet(headerColor);
		this.spielerSpalte.formatDaten();
		this.ranglisteFormatter.updateHeader();
		this.getRangListeSpalte().upDateRanglisteSpalte();
		this.getRangListeSpalte().insertHeaderInSheet(headerColor);
		this.ranglisteFormatter.formatDaten();
		this.ranglisteFormatter.formatDatenGeradeUngerade();
		ergebnisseEinfuegen();
		nichtgespielteRundenFuellen();
		this.rangListeSorter.doSort();
		this.ranglisteFormatter.addFooter();
	}

	protected void updateSummenSpalten() throws GenerateException {
		int anzSpielRunden = this.aktuelleSpielrundeSheet.countNumberOfSpielRunden(getSpieltagNr());
		if (anzSpielRunden < 1) {
			return;
		}
		XSpreadsheet sheet = getSheet();
		int letzteDatenzeile = this.spielerSpalte.getLetzteDatenZeile();
		List<Position> plusPunktPos = new ArrayList<>();
		for (int spielRunde = 1; spielRunde <= anzSpielRunden; spielRunde++) {
			SheetRunner.testDoCancelTask();
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

		int letzteDatenzeile = this.spielerSpalte.getLetzteDatenZeile();

		int nichtgespieltPlus = this.getKonfigurationSheet().getNichtGespielteRundePlus();
		int nichtgespieltMinus = this.getKonfigurationSheet().getNichtGespielteRundeMinus();

		NumberCellValue punktePlus = NumberCellValue.from(sheet, Position.from(1, 1), nichtgespieltPlus)
				.setComment("Nicht gespielte Runde (" + nichtgespieltPlus + ":" + nichtgespieltMinus + ")");
		NumberCellValue punkteMinus = NumberCellValue.from(punktePlus).setValue((double) nichtgespieltMinus)
				.setComment(null);

		for (int zeileCntr = ERSTE_DATEN_ZEILE; zeileCntr <= letzteDatenzeile; zeileCntr++) {
			SheetRunner.testDoCancelTask();
			for (int spielRunde = 1; spielRunde <= anzSpielRunden; spielRunde++) {
				SheetRunner.testDoCancelTask();
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
				SheetRunner.testDoCancelTask();

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

	public int countNumberOfSpieltage() throws GenerateException {
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

	/**
	 * spalte mit sortierdaten rangliste
	 *
	 * @return
	 * @throws GenerateException
	 */
	@Override
	public int getManuellSortSpalte() throws GenerateException {
		return getLetzteSpalte() + ERSTE_SORTSPALTE_OFFSET;
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

	public void clearAll() throws GenerateException {
		int letzteDatenzeile = this.spielerSpalte.getLetzteDatenZeile();
		if (letzteDatenzeile >= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			RangePosition range = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, getManuellSortSpalte(),
					letzteDatenzeile);
			this.getSheetHelper().clearRange(getSpieltagSheet(), range);
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
	// Delegates
	// --------------------------

	@Override
	public int getLetzteDatenZeile() throws GenerateException {
		return this.spielerSpalte.getLetzteDatenZeile();
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
		return this.getSheetHelper().newIfNotExist(getSheetName(spielTagNr), DefaultSheetPos.SUPERMELEE_WORK);
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
		return getErsteSummeSpalte() + PUNKTE_DIV_OFFS;
	}

	public boolean isErrorInSheet() throws GenerateException {
		return this.rangListeSorter.isErrorInSheet();
	}

	protected RangListeSorter getRangListeSorter() {
		return this.rangListeSorter;
	}
}
