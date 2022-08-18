/**
 * Erstellung : 10.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.FillAutoPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.rangliste.ISpielTagRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeRangListeSorter;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.spielrunde.AbstractSpielrundeSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Update;

public class SpieltagRanglisteSheet extends AbstractSpieltagRangliste implements ISpielTagRangliste {

	private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheet.class);

	public static final String KOPFDATEN_SUMME = "Summe";
	public static final String KOPFDATEN_SUMME_SPIELE = "Spiele";
	public static final String KOPFDATEN_SUMME_PUNKTE = "Punkte";

	public static final int ANZAHL_SPALTEN_IN_SPIELRUNDE = 2;

	public static final int ERSTE_SORTSPALTE_OFFSET = 2; // zur letzte spalte = PUNKTE_DIV_OFFS

	private final SpielrundeSheet_Update aktuelleSpielrundeSheet;
	private final RangListeSpalte rangListeSpalte;
	private final SpieltagRanglisteFormatter ranglisteFormatter;
	private final SuperMeleeRangListeSorter rangListeSorter;

	public SpieltagRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Spieltag Rangliste");

		aktuelleSpielrundeSheet = new SpielrundeSheet_Update(workingSpreadsheet);
		rangListeSpalte = new RangListeSpalte(RANGLISTE_SPALTE, this);
		ranglisteFormatter = new SpieltagRanglisteFormatter(this, ANZAHL_SPALTEN_IN_SPIELRUNDE, getSpielerSpalte(),
				ERSTE_SPIELRUNDE_SPALTE, getKonfigurationSheet());
		rangListeSorter = new SuperMeleeRangListeSorter(this);
	}

	@Override
	protected void doRun() throws GenerateException {
		getxCalculatable().enableAutomaticCalculation(false); // speed up
		generate(getKonfigurationSheet().getAktiveSpieltag());
	}

	public void generate(SpielTagNr spielTagNr) throws GenerateException {
		setSpieltagNr(spielTagNr);

		MeldeListeSheet_Update meldeliste = new MeldeListeSheet_Update(getWorkingSpreadsheet());
		meldeliste.setSpielTag(getSpieltagNr());
		aktuelleSpielrundeSheet.setSpielTag(getSpieltagNr());

		int anzSpielRunden = aktuelleSpielrundeSheet.countNumberOfSpielRundenSheets(getSpieltagNr());

		if (anzSpielRunden == 0) {
			MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK).caption("Spieltagrangliste")
					.message("Keine Spielrunden vorhanden").show();
			return;
		}

		// neu erstellen
		NewSheet.from(this, getSheetName(getSpieltagNr())).pos(DefaultSheetPos.SUPERMELEE_WORK).hideGrid().setActiv()
				.forceCreate().spielTagPageStyle(getSpieltagNr()).create();

		Integer headerColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		getSpielerSpalte().alleAktiveUndAusgesetzteMeldungenAusmeldelisteEinfuegen(meldeliste);
		getSpielerSpalte().insertHeaderInSheet(headerColor);
		ranglisteFormatter.updateHeader();

		boolean zeigeArbeitsSpalten = getKonfigurationSheet().zeigeArbeitsSpalten();
		rangListeSorter.insertSortValidateSpalte(zeigeArbeitsSpalten);
		rangListeSorter.insertManuelsortSpalten(zeigeArbeitsSpalten);
		ergebnisseFormulaEinfuegen();
		updateSummenSpalten();
		getSpielerSpalte().formatDaten();
		getRangListeSpalte().upDateRanglisteSpalte();
		getRangListeSpalte().insertHeaderInSheet(headerColor);
		ranglisteFormatter.formatDaten();
		ranglisteFormatter.formatDatenErrorGeradeUngerade(validateSpalte());
		getxCalculatable().calculate();
		rangListeSorter.doSort();
		Position footerPos = ranglisteFormatter.addFooter().getPos();
		printBereichDefinieren(footerPos);
		processBoxinfo("Header festsetzen");
		SheetFreeze.from(getTurnierSheet()).anzZeilen(3).anzSpalten(3).doFreeze();
	}

	private void printBereichDefinieren(Position footerPos) throws GenerateException {
		processBoxinfo("Print-Bereich");
		Position rechtsUnten = Position.from(getLetzteSpalte(), footerPos.getZeile());
		Position linksOben = Position.from(SPIELER_NR_SPALTE,
				AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	/**
	 * Die Anzahl an Spielrunden im Rangliste-Sheet zahlen
	 * 
	 * @return
	 * @throws GenerateException
	 */

	public int countNumberOfSpielrundenInSheet() throws GenerateException {
		return ranglisteFormatter.countAnzahlRunden();
	}

	protected void updateSummenSpalten() throws GenerateException {

		processBoxinfo("Summenspalten Aktualisieren");

		int anzSpielRunden = aktuelleSpielrundeSheet.countNumberOfSpielRundenSheets(getSpieltagNr());
		if (anzSpielRunden < 1) {
			return;
		}
		XSpreadsheet sheet = getXSpreadSheet();
		int letzteDatenzeile = getSpielerSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();
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

	private void ergebnisseFormulaEinfuegen() throws GenerateException {

		processBoxinfo("Spieltag(e) Ergebnisse Einfuegen");

		XSpreadsheet sheet = getXSpreadSheet();
		int anzSpielRunden = aktuelleSpielrundeSheet.countNumberOfSpielRundenSheets(getSpieltagNr());

		int nichtgespieltPlus = getKonfigurationSheet().getNichtGespielteRundePlus();
		int nichtgespieltMinus = getKonfigurationSheet().getNichtGespielteRundeMinus();
		int letzteDatenzeile = getSpielerSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();

		String verweisAufSpalteSpielerNr = "INDIRECT(ADDRESS(ROW();" + (SPIELER_NR_SPALTE + 1) + ";4))";

		// VLOOKUP Matrix fuer plus Punkte
		// $M$4:$O$1004
		int ersteSpalteVertikaleErgebnisse = AbstractSpielrundeSheet.ERSTE_SPALTE_VERTIKALE_ERGEBNISSE;
		int spielrundeSheetErsteDatenzeile = AbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
		Position erstePos = Position.from(ersteSpalteVertikaleErgebnisse, spielrundeSheetErsteDatenzeile);
		Position letztePosPlusPunkte = Position.from(AbstractSpielrundeSheet.SPALTE_VERTIKALE_ERGEBNISSE_PLUS,
				1000 + spielrundeSheetErsteDatenzeile);
		Position letztePosMinusPunkte = Position.from(AbstractSpielrundeSheet.SPALTE_VERTIKALE_ERGEBNISSE_MINUS + 2,
				1000 + spielrundeSheetErsteDatenzeile);
		String suchMatrixPlusPunkte = erstePos.getAddressWith$() + ":" + letztePosPlusPunkte.getAddressWith$();
		String suchMatrixMinusPunkte = erstePos.getAddressWith$() + ":" + letztePosMinusPunkte.getAddressWith$();

		// IFNA(VLOOKUP)
		for (int spielRunde = 1; spielRunde <= anzSpielRunden; spielRunde++) {
			// $ = absolute wegen sortieren
			String formulaSheetName = "$'"
					+ aktuelleSpielrundeSheet.getSheetName(getSpieltagNr(), SpielRundeNr.from(spielRunde)) + "'.";
			{
				// plus spalte
				// =WENNNV(SVERWEIS(INDIREKT(ADRESSE(ZEILE();1;8));$'1.1. Spielrunde'.$S$3:$T$1003;2;0);0)
				Position posPunktePlus = Position.from(
						ERSTE_SPIELRUNDE_SPALTE + ((spielRunde - 1) * ANZAHL_SPALTEN_IN_SPIELRUNDE), ERSTE_DATEN_ZEILE);
				String formulaPlusPunkte = "IFNA(VLOOKUP(" + verweisAufSpalteSpielerNr + ";" + formulaSheetName
						+ suchMatrixPlusPunkte + ";2;0);" + nichtgespieltPlus + ")";
				StringCellValue spielerPlus = StringCellValue.from(sheet, posPunktePlus).setValue(formulaPlusPunkte)
						.setFillAutoDown(letzteDatenzeile);
				getSheetHelper().setFormulaInCell(spielerPlus);
			}

			{
				// minus spalte
				// =WENNNV(SVERWEIS(INDIREKT(ADRESSE(ZEILE();1;8));$'1.1. Spielrunde'.$S$3:$U$1003;3;0);13)
				Position minusPunktePlus = Position.from(
						ERSTE_SPIELRUNDE_SPALTE + ((spielRunde - 1) * ANZAHL_SPALTEN_IN_SPIELRUNDE) + 1,
						ERSTE_DATEN_ZEILE);
				String formulaMinusPunkte = "IFNA(VLOOKUP(" + verweisAufSpalteSpielerNr + ";" + formulaSheetName
						+ suchMatrixMinusPunkte + ";3;0);" + nichtgespieltMinus + ")";
				StringCellValue spielerMinus = StringCellValue.from(sheet, minusPunktePlus).setValue(formulaMinusPunkte)
						.setFillAutoDown(letzteDatenzeile);
				getSheetHelper().setFormulaInCell(spielerMinus);
			}

		}
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
			// erste spalte = 1
			return "VLOOKUP(" + spielrNrAdresse + ";$'" + getSheetName(spieltagNr) + "'." + ersteZelleAddress + ":"
					+ letzteZelleAddress + ";" + (returnSpalte + 1) + ";0)";
		}
		return null;
	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		return getErsteSummeSpalte(getSpieltagNr());
	}

	public int getErsteSummeSpalte(SpielTagNr spieltag) throws GenerateException {
		checkNotNull(spieltag);
		int anzSpielRunden = aktuelleSpielrundeSheet.countNumberOfSpielRundenSheets(spieltag);
		return ERSTE_SPIELRUNDE_SPALTE + (anzSpielRunden * 2);
	}

	public List<Integer> getSpielerNrList(SpielTagNr spielTagNr) throws GenerateException {
		setSpieltagNr(checkNotNull(spielTagNr));

		List<Integer> spielerNrlist = new ArrayList<>();
		// letzte Zeile ?
		RangePosition searchRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, 9999);
		Position lastNotEmptyPos = RangeSearchHelper.from(this, searchRange).searchLastNotEmptyInSpalte();

		// daten in array einlesen
		RangePosition spielNrRange = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE,
				lastNotEmptyPos.getZeile());
		RangeData dataFromRange = RangeHelper.from(this, spielNrRange).getDataFromRange();

		for (RowData zeile : dataFromRange) {
			int spielerNr = zeile.get(0).getIntVal(-1);
			if (spielerNr < 1) {
				break; // fertig
			}
			spielerNrlist.add(spielerNr);
		}
		return spielerNrlist;
	}

	public int countNumberOfRanglisten() throws GenerateException {
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

		for (int spielerNr : getSpielerSpalte().getSpielerNrList()) {
			SpielerSpieltagErgebnis erg = spielerErgebnisseEinlesen(spieltagNr, spielerNr);
			if (erg != null) {
				spielTagErgebnisse.add(erg);
			}
		}

		return spielTagErgebnisse;
	}

	public SpielerSpieltagErgebnis spielerErgebnisseEinlesen(SpielTagNr spieltag, int spielrNr)
			throws GenerateException {
		int spielerZeile = getSpielerSpalte().getSpielerZeileNr(spielrNr);

		if (spielerZeile < ERSTE_DATEN_ZEILE) {
			return null;
		}

		XSpreadsheet spieltagSheet = getXSpreadSheet();
		if (spieltagSheet == null) {
			return null;
		}

		int ersteSpieltagSummeSpalte = getErsteSummeSpalte();
		Position spielePlusSumme = Position.from(ersteSpieltagSummeSpalte, spielerZeile);
		SpielerSpieltagErgebnis erg = SpielerSpieltagErgebnis.from(spieltag, spielrNr);

		erg.setSpielPlus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme))
				.setPosSpielPlus(spielePlusSumme);

		erg.setSpielMinus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlusEins()))
				.setPosSpielMinus(spielePlusSumme);

		erg.setPunktePlus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlus(2)))
				.setPosPunktePlus(spielePlusSumme);

		erg.setPunkteMinus(getSheetHelper().getIntFromCell(spieltagSheet, spielePlusSumme.spaltePlusEins()))
				.setPosPunkteMinus(spielePlusSumme);

		return erg;
	}

	public void clearAll() throws GenerateException {
		int letzteDatenzeile = getSpielerSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();
		if (letzteDatenzeile >= ERSTE_DATEN_ZEILE) { // daten vorhanden ?
			RangePosition range = RangePosition.from(SPIELER_NR_SPALTE, ERSTE_DATEN_ZEILE, getManuellSortSpalte(),
					letzteDatenzeile);
			RangeHelper.from(this, range).clearRange();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
	// Delegates
	// --------------------------

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return getSpielerSpalte().getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int getErsteDatenZiele() {
		return getSpielerSpalte().getErsteDatenZiele();
	}

	@Override
	public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
		return getSpielerSpalte().sucheLetzteZeileMitSpielerNummer();
	}

	public RangListeSpalte getRangListeSpalte() {
		return rangListeSpalte;
	}

	@Override
	public int getAnzahlRunden() throws GenerateException {
		return aktuelleSpielrundeSheet.countNumberOfSpielRundenSheets(getSpieltagNr());
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		return getErsteSummeSpalte() + PUNKTE_DIV_OFFS;
	}

	public void isErrorInSheet() throws GenerateException {
		rangListeSorter.isErrorInSheet();
	}

	protected SuperMeleeRangListeSorter getRangListeSorter() {
		return rangListeSorter;
	}

	@Override
	public List<Position> getRanglisteSpalten() throws GenerateException {
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		return getRanglisteSpalten(ersteSpalteEndsumme, ERSTE_DATEN_ZEILE);
	}

	@Override
	public int validateSpalte() throws GenerateException {
		return getManuellSortSpalte() + PUNKTE_DIV_OFFS;
	}

	@Override
	public int getErsteSpalte() throws GenerateException {
		return SPIELER_NR_SPALTE;
	}

	@Override
	public void calculateAll() {
		getxCalculatable().calculateAll();
	}

	public SpielrundeSheet_Update getAktuelleSpielrundeSheet() {
		return aktuelleSpielrundeSheet;
	}

}
