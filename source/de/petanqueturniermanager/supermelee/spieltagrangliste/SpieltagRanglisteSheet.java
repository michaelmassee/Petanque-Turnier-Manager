/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.sheet.SummenSpalten.PUNKTE_DIV_OFFS;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.rangliste.AbstractRanglisteFormatter;
import de.petanqueturniermanager.helper.rangliste.ISpielTagRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.rangliste.RanglisteFormatter;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.IEndSummeSpalten;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.meldeliste.AbstractSupermeleeMeldeListeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.spielrunde.AbstractSpielrundeSheet;
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

	public SpieltagRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Spieltag Rangliste");
		meldeliste = new MeldeListeSheet_Update(workingSpreadsheet);
		konfigurationSheet = new KonfigurationSheet(workingSpreadsheet);
		spielerSpalte = new SpielerSpalte(ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this, meldeliste, Formation.MELEE);
		aktuelleSpielrundeSheet = new SpielrundeSheet_Update(workingSpreadsheet);
		rangListeSpalte = new RangListeSpalte(RANGLISTE_SPALTE, this);
		ranglisteFormatter = new RanglisteFormatter(this, ANZAHL_SPALTEN_IN_SPIELRUNDE, spielerSpalte, ERSTE_SPIELRUNDE_SPALTE, getKonfigurationSheet());
		rangListeSorter = new RangListeSorter(this);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpieltagNr(getKonfigurationSheet().getAktiveSpieltag());
		getxCalculatable().enableAutomaticCalculation(false); // speed up
		generate();
	}

	public void generate() throws GenerateException {

		meldeliste.setSpielTag(getSpieltagNr());
		aktuelleSpielrundeSheet.setSpielTag(getSpieltagNr());
		// neu erstellen
		NewSheet.from(getWorkingSpreadsheet(), getSheetName(getSpieltagNr())).pos(DefaultSheetPos.SUPERMELEE_WORK).setActiv().forceCreate().spielTagPageStyle(spieltagNr).create();

		Integer headerColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		spielerSpalte.alleSpieltagSpielerEinfuegen();
		spielerSpalte.insertHeaderInSheet(headerColor);
		ranglisteFormatter.updateHeader();
		rangListeSorter.insertSortValidateSpalte();
		rangListeSorter.insertManuelsortSpalten();
		ergebnisseFormulaEinfuegen();
		updateSummenSpalten();
		spielerSpalte.formatDaten();
		getRangListeSpalte().upDateRanglisteSpalte();
		getRangListeSpalte().insertHeaderInSheet(headerColor);
		ranglisteFormatter.formatDaten();
		ranglisteFormatter.formatDatenErrorGeradeUngerade(rangListeSorter.validateSpalte());
		getxCalculatable().calculate();
		rangListeSorter.doSort();
		Position footerPos = ranglisteFormatter.addFooter().getPos();
		printBereichDefinieren(footerPos);
	}

	private void printBereichDefinieren(Position footerPos) throws GenerateException {
		processBoxinfo("Print-Bereich");
		Position rechtsUnten = Position.from(getLetzteSpalte(), footerPos.getZeile());
		Position linksOben = Position.from(SPIELER_NR_SPALTE, AbstractRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		PrintArea.from(getSheet()).setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	protected void updateSummenSpalten() throws GenerateException {

		processBoxinfo("Summenspalten Aktualisieren");

		int anzSpielRunden = aktuelleSpielrundeSheet.countNumberOfSpielRunden(getSpieltagNr());
		if (anzSpielRunden < 1) {
			return;
		}
		XSpreadsheet sheet = getSheet();
		int letzteDatenzeile = spielerSpalte.getLetzteDatenZeile();
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
		StringCellValue valspielSumme = StringCellValue.from(sheet, Position.from(getErsteSummeSpalte(), ERSTE_DATEN_ZEILE), formulaSpielePlus).setFillAuto(fillAutoPosition);
		getSheetHelper().setFormulaInCell(valspielSumme);

		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(formulaSpieleMinus).setFillAuto(fillAutoPosition.spaltePlusEins()));
		// div spalte
		String spieleDivFormula = Position.from(valspielSumme.getPos()).spaltePlus(-1).getAddress() + "-" + Position.from(valspielSumme.getPos()).getAddress();
		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(spieleDivFormula).setFillAuto(fillAutoPosition.spaltePlusEins()));

		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(formulaPunktePlus).setFillAuto(fillAutoPosition.spaltePlusEins()));
		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(formulaPunkteMinus).setFillAuto(fillAutoPosition.spaltePlusEins()));
		// div spalte
		String punkteDivFormula = Position.from(valspielSumme.getPos()).spaltePlus(-1).getAddress() + "-" + Position.from(valspielSumme.getPos()).getAddress();
		getSheetHelper().setFormulaInCell(valspielSumme.spaltePlusEins().setValue(punkteDivFormula).setFillAuto(fillAutoPosition.spaltePlusEins()));
	}

	private void ergebnisseFormulaEinfuegen() throws GenerateException {

		processBoxinfo("Spieltag(e) Ergebnisse Einfuegen");

		XSpreadsheet sheet = getSheet();
		int anzSpielRunden = aktuelleSpielrundeSheet.countNumberOfSpielRunden(getSpieltagNr());

		int nichtgespieltPlus = getKonfigurationSheet().getNichtGespielteRundePlus();
		int nichtgespieltMinus = getKonfigurationSheet().getNichtGespielteRundeMinus();
		int letzteDatenzeile = spielerSpalte.getLetzteDatenZeile();

		String verweisAufSpalteSpielerNr = "INDIRECT(ADDRESS(ROW();" + (SPIELER_NR_SPALTE + 1) + ";8))";

		// VLOOKUP Matrix fuer plus Punkte
		// $M$4:$O$1004
		int ersteSpalteVertikaleErgebnisse = AbstractSpielrundeSheet.ERSTE_SPALTE_VERTIKALE_ERGEBNISSE;
		int spielrundeSheetErsteDatenzeile = AbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
		Position erstePos = Position.from(ersteSpalteVertikaleErgebnisse, spielrundeSheetErsteDatenzeile);
		Position letztePosPlusPunkte = Position.from(ersteSpalteVertikaleErgebnisse + 1, 1000 + spielrundeSheetErsteDatenzeile);
		Position letztePosMinusPunkte = Position.from(ersteSpalteVertikaleErgebnisse + 2, 1000 + spielrundeSheetErsteDatenzeile);
		String suchMatrixPlusPunkte = erstePos.getAddressWith$() + ":" + letztePosPlusPunkte.getAddressWith$();
		String suchMatrixMinusPunkte = erstePos.getAddressWith$() + ":" + letztePosMinusPunkte.getAddressWith$();

		// IFNA(VLOOKUP)
		for (int spielRunde = 1; spielRunde <= anzSpielRunden; spielRunde++) {
			// $ = absolute wegen sortieren
			String formulaSheetName = "$'" + aktuelleSpielrundeSheet.getSheetName(getSpieltagNr(), SpielRundeNr.from(spielRunde)) + "'.";
			{
				// plus spalte
				// =WENNNV(SVERWEIS(INDIREKT(ADRESSE(ZEILE();1;8));$'1.1. Spielrunde'.$S$3:$T$1003;2;0);0)
				Position posPunktePlus = Position.from(ERSTE_SPIELRUNDE_SPALTE + ((spielRunde - 1) * ANZAHL_SPALTEN_IN_SPIELRUNDE), ERSTE_DATEN_ZEILE);
				String formulaPlusPunkte = "IFNA(VLOOKUP(" + verweisAufSpalteSpielerNr + ";" + formulaSheetName + suchMatrixPlusPunkte + ";2;0);" + nichtgespieltPlus + ")";
				StringCellValue spielerPlus = StringCellValue.from(sheet, posPunktePlus).setValue(formulaPlusPunkte).setFillAutoDown(letzteDatenzeile);
				getSheetHelper().setFormulaInCell(spielerPlus);
			}

			{
				// minus spalte
				// =WENNNV(SVERWEIS(INDIREKT(ADRESSE(ZEILE();1;8));$'1.1. Spielrunde'.$S$3:$U$1003;3;0);13)
				Position minusPunktePlus = Position.from(ERSTE_SPIELRUNDE_SPALTE + ((spielRunde - 1) * ANZAHL_SPALTEN_IN_SPIELRUNDE) + 1, ERSTE_DATEN_ZEILE);
				String formulaMinusPunkte = "IFNA(VLOOKUP(" + verweisAufSpalteSpielerNr + ";" + formulaSheetName + suchMatrixMinusPunkte + ";3;0);" + nichtgespieltMinus + ")";
				StringCellValue spielerMinus = StringCellValue.from(sheet, minusPunktePlus).setValue(formulaMinusPunkte).setFillAutoDown(letzteDatenzeile);
				getSheetHelper().setFormulaInCell(spielerMinus);
			}

		}
	}

	public String getSheetName(SpielTagNr spieltagNr) throws GenerateException {
		return spieltagNr.getNr() + ". " + SHEETNAME_SUFFIX;
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
	public String formulaSverweisAufSummeSpalte(SpielTagNr spieltagNr, int summeSpalte, String spielrNrAdresse) throws GenerateException {
		int ersteSummeSpalte = getErsteSummeSpalte(spieltagNr);

		if (ersteSummeSpalte > -1) {
			// gefunden
			int returnSpalte = ersteSummeSpalte + summeSpalte;

			String ersteZelleAddress = Position.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddressWith$();
			String letzteZelleAddress = Position.from(returnSpalte, 999).getAddressWith$();
			// erste spalte = 1
			return "VLOOKUP(" + spielrNrAdresse + ";$'" + getSheetName(spieltagNr) + "'." + ersteZelleAddress + ":" + letzteZelleAddress + ";" + (returnSpalte + 1) + ";0)";
		}
		return null;
	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		return getErsteSummeSpalte(getSpieltagNr());
	}

	public int getErsteSummeSpalte(SpielTagNr spieltag) throws GenerateException {
		checkNotNull(spieltag);
		int anzSpielRunden = aktuelleSpielrundeSheet.countNumberOfSpielRunden(spieltag);
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
				String cellText = getSheetHelper().getTextFromCell(spieltagSheet, Position.from(SPIELER_NR_SPALTE, zeileCntr));
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

		XSpreadsheets sheets = getSheetHelper().getSheets();

		if (sheets != null && sheets.hasElements()) {
			String[] sheetNames = getSheetHelper().getSheets().getElementNames();
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

		for (int spielerNr : spielerSpalte.getSpielerNrList()) {
			SpielerSpieltagErgebnis erg = spielerErgebnisseEinlesen(spieltagNr, spielerNr);
			if (erg != null) {
				spielTagErgebnisse.add(erg);
			}
		}

		return spielTagErgebnisse;
	}

	public SpielerSpieltagErgebnis spielerErgebnisseEinlesen(SpielTagNr spieltag, int spielrNr) throws GenerateException {
		int spielerZeile = spielerSpalte.getSpielerZeileNr(spielrNr);

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

		erg.setSpielPlus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme)).setPosSpielPlus(spielePlusSumme);

		erg.setSpielMinus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlusEins())).setPosSpielMinus(spielePlusSumme);

		erg.setPunktePlus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlus(2))).setPosPunktePlus(spielePlusSumme);

		erg.setPunkteMinus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlusEins())).setPosPunkteMinus(spielePlusSumme);

		return erg;
	}

	public void clearAll() throws GenerateException {
		int letzteDatenzeile = spielerSpalte.getLetzteDatenZeile();
		if (letzteDatenzeile >= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			RangePosition range = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, getManuellSortSpalte(), letzteDatenzeile);
			getSheetHelper().clearRange(getSheet(), range);
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
		return spielerSpalte.getLetzteDatenZeile();
	}

	@Override
	public int getErsteDatenZiele() {
		return spielerSpalte.getErsteDatenZiele();
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheet(getSpieltagNr());
	}

	public XSpreadsheet getSheet(SpielTagNr spielTagNr) throws GenerateException {
		return getSheetHelper().findByName(getSheetName(spielTagNr));
	}

	public RangListeSpalte getRangListeSpalte() {
		return rangListeSpalte;
	}

	@Override
	public int getAnzahlRunden() throws GenerateException {
		return aktuelleSpielrundeSheet.countNumberOfSpielRunden(getSpieltagNr());
	}

	public SpielTagNr getSpieltagNr() throws GenerateException {
		checkNotNull(spieltagNr, "spieltagNr==null");
		return spieltagNr;
	}

	public void setSpieltagNr(SpielTagNr spieltagNr) throws GenerateException {
		checkNotNull(spieltagNr, "spieltagNr==null");
		ProcessBox.from().spielTag(spieltagNr);
		this.spieltagNr = spieltagNr;
	}

	protected KonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		return getErsteSummeSpalte() + PUNKTE_DIV_OFFS;
	}

	public void isErrorInSheet() throws GenerateException {
		rangListeSorter.isErrorInSheet();
	}

	protected RangListeSorter getRangListeSorter() {
		return rangListeSorter;
	}
}
