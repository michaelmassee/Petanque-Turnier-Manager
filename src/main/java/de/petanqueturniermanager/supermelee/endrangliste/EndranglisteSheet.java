/**
 * Erstellung : 10.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.endrangliste;

import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.ANZAHL_SPALTEN_IN_SUMME;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_DIV_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_MINUS_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.PUNKTE_PLUS_OFFS;
import static de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten.SPIELE_MINUS_OFFS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.FontWeight;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.StreichSpieltagHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.StreichSpieltagHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SuperMeleeRangListeSorter;
import de.petanqueturniermanager.supermelee.SuperMeleeSummenSpalten;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class EndranglisteSheet extends SuperMeleeSheet implements IEndRangliste {
	private static final Logger logger = LogManager.getLogger(EndranglisteSheet.class);

	public static final int ERSTE_DATEN_ZEILE = 3; // Zeile 4
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0, B=1
	public static final int RANGLISTE_SPALTE = 2; // Spalte C=2
	public static final int ERSTE_SPIELTAG_SPALTE = 3; // Spalte D=3

	public static final int ERSTE_SORTSPALTE_OFFSET = 3; // zur letzte spalte = anz Spieltage

	public static final String SHEETNAME = "Endrangliste";
	public static final String SHEET_COLOR = "d637e8";

	private final SpieltagRanglisteSheet spieltagRanglisteSheet;
	private final MeldungenSpalte<SpielerMeldungen, Spieler> spielerSpalte;
	private final EndRanglisteFormatter endRanglisteFormatter;
	private final RangListeSpalte rangListeSpalte;
	private final SuperMeleeRangListeSorter rangListeSorter;

	public EndranglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Endrangliste");
		spieltagRanglisteSheet = new SpieltagRanglisteSheet(workingSpreadsheet);
		spielerSpalte = MeldungenSpalte.Builder().ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(SPIELER_NR_SPALTE)
				.anzZeilenInHeader(2).sheet(this).formation(Formation.SUPERMELEE)
				.spalteMeldungNameWidth(SUPER_MELEE_MELDUNG_NAME_WIDTH).build();
		endRanglisteFormatter = new EndRanglisteFormatter(this, getAnzSpaltenInSpieltag(), spielerSpalte,
				ERSTE_SPIELTAG_SPALTE, getKonfigurationSheet());
		rangListeSpalte = new RangListeSpalte(RANGLISTE_SPALTE, this);
		rangListeSorter = new SuperMeleeRangListeSorter(this);
	}

	@Override
	protected void doRun() throws GenerateException {
		SpielTagNr spieltagNr = getKonfigurationSheet().getAktiveSpieltag();
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.SUPERMELEE_ENDRANGLISTE).tabColor(SHEET_COLOR).setActiv()
				.hideGrid().forceCreate().spielTagPageStyle(spieltagNr).create().isDidCreate()) {
			getxCalculatable().enableAutomaticCalculation(false); // speed up
			upDateSheet();
		}
	}

	private void upDateSheet() throws GenerateException {

		int anzahlSpieltage = getAnzahlSpieltage();
		if (anzahlSpieltage < 2) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Fehler")
					.message("Ungültige anzahl von Spieltage. " + anzahlSpieltage).show();
			return;
		}

		Integer headerColor = getKonfigurationSheet().getRanglisteHeaderFarbe();

		spielerEinfuegen();
		spielerSpalte.insertHeaderInSheet(headerColor);
		spielerSpalte.formatDaten();
		endRanglisteFormatter.updateHeader();

		spielTageEinfuegen();
		getxCalculatable().calculate();
		updateEndSummenSpalten();

		boolean zeigeArbeitsSpalten = getKonfigurationSheet().zeigeArbeitsSpalten();
		rangListeSorter.insertSortValidateSpalte(zeigeArbeitsSpalten);
		rangListeSorter.insertManuelsortSpalten(zeigeArbeitsSpalten);

		endRanglisteFormatter.formatDaten();
		rangListeSpalte.upDateRanglisteSpalte();
		rangListeSpalte.insertHeaderInSheet(headerColor);

		updateAnzSpieltageSpalte();
		getxCalculatable().calculate();
		formatDatenGeradeUngeradeMitStreichSpieltag();
		formatSchlechtesteSpieltagSpalte();
		getxCalculatable().calculate();
		rangListeSorter.doSort();
		Position footerPos = endRanglisteFormatter.addFooter().getPos();
		printBereichDefinieren(footerPos);
		processBoxinfo("Header festsetzen");
		SheetFreeze.from(getTurnierSheet()).anzZeilen(3).anzSpalten(3).doFreeze();
	}

	private void printBereichDefinieren(Position footerPos) throws GenerateException {
		processBoxinfo("Print-Bereich");
		Position linksOben = Position.from(SPIELER_NR_SPALTE,
				AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position rechtsUnten = Position.from(getLetzteSpalte(), footerPos.getZeile());
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}

	private void formatDatenGeradeUngeradeMitStreichSpieltag() throws GenerateException {

		processBoxinfo("Formatiere gerade Ungerade Zeilen");

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		int spielerNrSpalte = spielerSpalte.getSpielerNrSpalte();
		int ersteDatenZeile = spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = spielerSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();
		int letzteSpalte = getLetzteSpalte();

		Integer streichSpieltag_geradeColor = getKonfigurationSheet()
				.getRanglisteHintergrundFarbe_StreichSpieltag_Gerade();
		Integer streichSpieltag_unGeradeColor = getKonfigurationSheet()
				.getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade();
		StreichSpieltagHintergrundFarbeGeradeStyle streichSpieltagHintergrundFarbeGeradeStyle = new StreichSpieltagHintergrundFarbeGeradeStyle(
				streichSpieltag_geradeColor);
		StreichSpieltagHintergrundFarbeUnGeradeStyle streichSpieltagHintergrundFarbeUnGeradeStyle = new StreichSpieltagHintergrundFarbeUnGeradeStyle(
				streichSpieltag_unGeradeColor);

		Integer geradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeGerade();
		Integer unGeradeColor = getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade();
		RanglisteHintergrundFarbeGeradeStyle ranglisteHintergrundFarbeGeradeStyle = new RanglisteHintergrundFarbeGeradeStyle(
				geradeColor);
		RanglisteHintergrundFarbeUnGeradeStyle ranglisteHintergrundFarbeUnGeradeStyle = new RanglisteHintergrundFarbeUnGeradeStyle(
				unGeradeColor);

		RangePosition datenRange = RangePosition.from(spielerNrSpalte, ersteDatenZeile, letzteSpalte, letzteDatenZeile);

		// Formula fuer sort error, komplette zeile rot einfärben wenn fehler meldung
		// Achtung spalte plus 1 weil A ist nicht 0 sondern 1
		String formulaSortError = "LEN(TRIM(INDIRECT(ADDRESS(ROW();" + (rangListeSorter.validateSpalte() + 1)
				+ "))))>0";

		ConditionalFormatHelper.from(this, datenRange).clear().
		// -----------------------------
		// Formula fuer sort error, komplette zeile rot einfärben wenn fehler meldung
				formula1(formulaSortError).operator(ConditionOperator.FORMULA).style(new FehlerStyle())
				.applyAndDoReset().
				// ------------------------
				// Formula fuer streichspieltag
				// UND(INDIREKT(ADRESSE(ZEILE();13;4;))=AUFRUNDEN((SPALTE()-1)/3);ISTGERADE(ZEILE()))
				formula1(getFormulastreichSpieltag(true)).operator(ConditionOperator.FORMULA)
				.style(streichSpieltagHintergrundFarbeGeradeStyle).applyAndDoReset().
				// ---------------------
				formula1(getFormulastreichSpieltag(false)).operator(ConditionOperator.FORMULA)
				.style(streichSpieltagHintergrundFarbeUnGeradeStyle).applyAndDoReset().
				// --------------------------
				formulaIsEvenRow().style(ranglisteHintergrundFarbeGeradeStyle).applyAndDoReset().
				// ---------------------------
				formulaIsOddRow().style(ranglisteHintergrundFarbeUnGeradeStyle).applyAndDoReset();

	}

	private String getFormulastreichSpieltag(boolean iseven) throws GenerateException {
		// 13 = streichspieltag spalte
		// -1 = offset spalten links
		// /3 = anzahl spalten in spieltag
		// UND(INDIREKT(ADRESSE(ZEILE();13;4;))=AUFRUNDEN((SPALTE()-1)/3;0);ISTGERADE(ZEILE()))
		int schlechtesteSpielTageSpalte = getSchlechtesteSpielTageSpalte() + 1; // In Formula erste spalte ab 1
		int anzSpaltenInSpieltag = getAnzSpaltenInSpieltag();
		int anzahlSpieltage = getAnzahlSpieltage();

		String isCondition = "";
		if (iseven) {
			isCondition = "ISEVEN";
		} else {
			isCondition = "ISODD";
		}

		String verweisAufStreichSpalte = "INDIRECT(ADDRESS(ROW();" + schlechtesteSpielTageSpalte + ";4))";
		// 1. prüfen ob wert in steichspieltag spalte > 0
		String erstePruefung = verweisAufStreichSpalte + ">0";
		// 2. prüfen ob wert in steichspieltag spalte < anzahlSpieltage
		String zweitePruefung = verweisAufStreichSpalte + "<" + (anzahlSpieltage + 1);
		// 3. prüfen ob wert in steichspieltag spalte == aktuelle spalte block
		String drittePruefung = verweisAufStreichSpalte + "=ROUNDUP((COLUMN()-" + ERSTE_SPIELTAG_SPALTE + ")/"
				+ anzSpaltenInSpieltag + ";0)";
		// 4. prüfen ob gerade oder ungerade zeile
		String viertePruefung = isCondition + "(ROW())";
		return "AND(" + erstePruefung + ";" + zweitePruefung + ";" + drittePruefung + ";" + viertePruefung + ")";
	}

	private void formatSchlechtesteSpieltagSpalte() throws GenerateException {

		processBoxinfo("Formatiere Streichspieltag");

		int schlechtesteSpielTageSpalte = getSchlechtesteSpielTageSpalte();
		NumberCellValue numberCellValueSchlechtesteSpielTag = NumberCellValue.from(getXSpreadSheet(),
				schlechtesteSpielTageSpalte, ERSTE_DATEN_ZEILE);

		// Header Streichspieltag
		Position startStreichspieltag = Position.from(getSchlechtesteSpielTageSpalte(),
				AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position endStreichspieltag = Position.from(startStreichspieltag).zeilePlus(2);

		ColumnProperties columnProperties = ColumnProperties.from()
				.setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerStreichspieltag = StringCellValue.from(getXSpreadSheet(), startStreichspieltag)
				.setEndPosMerge(endStreichspieltag).setCharWeight(FontWeight.LIGHT).setRotateAngle(27000)
				.setVertJustify(CellVertJustify2.CENTER).setValue("Streich")
				.setCellBackColor(endRanglisteFormatter.getHeaderFarbe())
				.setBorder(BorderFactory.from().allBold().toBorder()).setComment("Streich-Spieltag")
				.setColumnProperties(columnProperties);
		getSheetHelper().setStringValueInCell(headerStreichspieltag);
		// Daten
		RangePosition rangPos = RangePosition.from(getSchlechtesteSpielTageSpalte(), ERSTE_DATEN_ZEILE,
				getSchlechtesteSpielTageSpalte(), getLetzteMitDatenZeileInSpielerNrSpalte());
		CellProperties celRangeProp = CellProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER);
		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), rangPos, celRangeProp);

		for (Integer spielerNr : spielerSpalte.getSpielerNrList()) {
			SheetRunner.testDoCancelTask();
			SpielTagNr spielTagNr = schlechtesteSpieltag(spielerNr);
			int spielerZeile = spielerSpalte.getSpielerZeileNr(spielerNr);
			if (spielTagNr != null && spielerZeile > 0) {
				getSheetHelper().setValInCell(
						numberCellValueSchlechtesteSpielTag.zeile(spielerZeile).setValue((double) spielTagNr.getNr()));
			}
		}
	}

	private void spielerEinfuegen() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();

		HashSet<Integer> spielerNummer = new HashSet<>();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			List<Integer> spielerListe = spieltagRanglisteSheet.getSpielerNrList(SpielTagNr.from(spieltagCntr));
			spielerNummer.addAll(spielerListe);
		}
		spielerSpalte.alleSpielerNrEinfuegen(spielerNummer, new MeldeListeSheet_New(getWorkingSpreadsheet()));
	}

	private int getSpielTagErsteSummeSpalte(SpielTagNr nr) {
		return ERSTE_SPIELTAG_SPALTE + ((nr.getNr() - 1) * getAnzSpaltenInSpieltag());
	}

	private void spielTageEinfuegen() throws GenerateException {

		processBoxinfo("Spieltage Einfuegen");

		// verwende fill down
		// =WENNNV(SVERWEIS(A4;$'2. Spieltag Rangliste'.$A4:$D1000;4;0);"")

		int anzSpieltage = getAnzahlSpieltage();
		int letzteDatenZeile = spielerSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();

		String verweisAufSpalteSpielerNr = "INDIRECT(ADDRESS(ROW();" + (SPIELER_NR_SPALTE + 1) + ";4))";

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			int spieltagSummeErsteSpalte = getSpielTagErsteSummeSpalte(SpielTagNr.from(spieltagCntr));
			Position positionSumme = Position.from(spieltagSummeErsteSpalte, ERSTE_DATEN_ZEILE);
			StringCellValue strVal = StringCellValue.from(getXSpreadSheet(), positionSumme);

			for (int summeSpalteCntr = 0; summeSpalteCntr < getAnzSpaltenInSpieltag(); summeSpalteCntr++) {
				SheetRunner.testDoCancelTask();
				String verweisAufSummeSpalte = spieltagRanglisteSheet.formulaSverweisAufSummeSpalte(
						SpielTagNr.from(spieltagCntr), summeSpalteCntr, verweisAufSpalteSpielerNr);
				strVal.setValue("IFNA(" + verweisAufSummeSpalte + ";\"\")");
				getSheetHelper().setFormulaInCell(strVal.setFillAutoDown(letzteDatenZeile));
				strVal.spaltePlusEins();
			}
		}
	}

	private int anzSpielTageSpalte() throws GenerateException {
		int ersteSpalteEndsumme = getErsteSummeSpalte();
		return ersteSpalteEndsumme + ANZAHL_SPALTEN_IN_SUMME;
	}

	private int getSchlechtesteSpielTageSpalte() throws GenerateException {
		return anzSpielTageSpalte() + 1;
	}

	/**
	 * Anzahl gespielte Spieltage<br>
	 * =ZÄHLENWENN(D4:AG4;"<>")/6
	 *
	 * @throws GenerateException
	 */
	private void updateAnzSpieltageSpalte() throws GenerateException {

		processBoxinfo("Aktualisiere Anzahl Spieltage Spalte");

		int ersteSpalteEndsumme = getErsteSummeSpalte();
		int letzteSpieltagLetzteSpalte = ersteSpalteEndsumme - 1;
		int letzteZeile = getLetzteMitDatenZeileInSpielerNrSpalte();

		Position ersteSpielTagErsteZelle = Position.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE);
		Position letzteSpielTagLetzteZelle = Position.from(letzteSpieltagLetzteSpalte, ERSTE_DATEN_ZEILE);

		String formula = "=COUNTIF(" + ersteSpielTagErsteZelle.getAddress() + ":"
				+ letzteSpielTagLetzteZelle.getAddress() + ";\"<>\")/" + ANZAHL_SPALTEN_IN_SUMME;

		// letzte Spalte ist anzahl spieltage
		ColumnProperties celColumProp = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
				.setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue formulaVal = StringCellValue
				.from(getXSpreadSheet(), Position.from(anzSpielTageSpalte(), ERSTE_DATEN_ZEILE)).setValue(formula)
				.setFillAutoDown(letzteZeile).setColumnProperties(celColumProp);
		getSheetHelper().setFormulaInCell(formulaVal);

		// Spalte formatieren
		// Header AnzahlTage
		Position start = Position.from(anzSpielTageSpalte(),
				AbstractSuperMeleeRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position end = Position.from(start).zeilePlus(2);
		StringCellValue headerAnzTage = StringCellValue.from(getXSpreadSheet(), start).setEndPosMerge(end)
				.setCharWeight(FontWeight.LIGHT).setRotateAngle(27000).setVertJustify(CellVertJustify2.CENTER)
				.setValue("Tage").setCellBackColor(endRanglisteFormatter.getHeaderFarbe())
				.setBorder(BorderFactory.from().allBold().toBorder()).setComment("Gespielte Tage");
		getSheetHelper().setStringValueInCell(headerAnzTage);

		// Daten
		RangePosition rangPos = RangePosition.from(formulaVal.getPos(), formulaVal.getFillAuto());
		CellProperties celRangeProp = CellProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder());
		getSheetHelper().setPropertiesInRange(getXSpreadSheet(), rangPos, celRangeProp);

	}

	private void updateEndSummenSpalten() throws GenerateException {

		processBoxinfo("Summen Spalten Aktualisieren");

		List<Integer> spielerNrList = spielerSpalte.getSpielerNrList();
		for (int spielerNr : spielerNrList) {
			endSummeSpalte(spielerNr);
		}
	}

	private void endSummeSpalte(int spielrNr) throws GenerateException {
		SpielTagNr schlechtesteSpielTag = schlechtesteSpieltag(spielrNr);
		int anzSpieltage = getAnzahlSpieltage();
		int spielerZeile = spielerSpalte.getSpielerZeileNr(spielrNr);

		if (anzSpieltage < 2) {
			return;
		}

		String[] felderList = new String[ANZAHL_SPALTEN_IN_SUMME];

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			if (schlechtesteSpielTag.getNr() != spieltagCntr) {
				int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
				for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
					SheetRunner.testDoCancelTask();
					Position spielSummeSpalte = Position.from(ersteSpieltagSummeSpalte + summeSpalteCntr, spielerZeile);

					if (felderList[summeSpalteCntr] == null) {
						felderList[summeSpalteCntr] = "";
					}

					if (!felderList[summeSpalteCntr].isEmpty()) {
						felderList[summeSpalteCntr] += ";";
					}
					felderList[summeSpalteCntr] += spielSummeSpalte.getAddress();
				}
			}
		}

		int ersteSpalteEndsumme = getErsteSummeSpalte();

		for (int summeSpalteCntr = 0; summeSpalteCntr < ANZAHL_SPALTEN_IN_SUMME; summeSpalteCntr++) {
			SheetRunner.testDoCancelTask();

			StringCellValue endsummeFormula = StringCellValue.from(getXSpreadSheet(),
					Position.from(ersteSpalteEndsumme + summeSpalteCntr, spielerZeile));
			endsummeFormula.setValue("SUM(" + felderList[summeSpalteCntr] + ")");
			getSheetHelper().setFormulaInCell(endsummeFormula);
		}
	}

	private SpielTagNr schlechtesteSpieltag(int spielrNr) throws GenerateException {

		int anzSpieltage = getAnzahlSpieltage();
		if (anzSpieltage < 2) {
			return null;
		}
		List<SpielerSpieltagErgebnis> spielerSpieltagErgebnisse = spielerErgebnisseEinlesen(spielrNr);
		spielerSpieltagErgebnisse.sort(new Comparator<SpielerSpieltagErgebnis>() {
			@Override
			public int compare(SpielerSpieltagErgebnis o1, SpielerSpieltagErgebnis o2) {
				// schlechteste oben
				return o1.reversedCompareTo(o2);
			}
		});
		if (spielerSpieltagErgebnisse.size() > 0) {
			return spielerSpieltagErgebnisse.get(0).getSpielTag();
		}
		return null;
	}

	private List<SpielerSpieltagErgebnis> spielerErgebnisseEinlesen(int spielrNr) throws GenerateException {
		List<SpielerSpieltagErgebnis> spielerErgebnisse = new ArrayList<>();
		int anzSpieltage = getAnzahlSpieltage();

		int spielerZeile = spielerSpalte.getSpielerZeileNr(spielrNr);

		XSpreadsheet sheet = getXSpreadSheet();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();

			SpielTagNr spielTagNr = SpielTagNr.from(spieltagCntr);

			int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
			// summe vorhanden ?
			String spielPlus = getSheetHelper().getTextFromCell(sheet,
					Position.from(ersteSpieltagSummeSpalte, spielerZeile));
			if (StringUtils.isNotBlank(spielPlus)) {
				SpielerSpieltagErgebnis ergebnis = new SpielerSpieltagErgebnis(spielTagNr, spielrNr);
				ergebnis.setSpielPlus(NumberUtils.toInt(spielPlus));
				ergebnis.setSpielMinus(NumberUtils.toInt(getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + SPIELE_MINUS_OFFS, spielerZeile))));
				ergebnis.setPunktePlus(NumberUtils.toInt(getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_PLUS_OFFS, spielerZeile))));
				ergebnis.setPunkteMinus(NumberUtils.toInt(getSheetHelper().getTextFromCell(sheet,
						Position.from(ersteSpieltagSummeSpalte + PUNKTE_MINUS_OFFS, spielerZeile))));
				spielerErgebnisse.add(ergebnis);
			} else {
				// nuller spieltag
				SpielerSpieltagErgebnis nullerSpielTag = new SpielerSpieltagErgebnis(spielTagNr, spielrNr);
				// nicht gespielten spieltage sind immer schlechter als gespielte spieltage
				nullerSpielTag.setSpielPlus(-1); // -1 Plus Punkte sind im normalen Spiel nicht möglich
				spielerErgebnisse.add(nullerSpielTag);
			}
		}
		return spielerErgebnisse;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public int getAnzahlSpieltage() throws GenerateException {
		return spieltagRanglisteSheet.countNumberOfRanglisten();
	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();
		return ERSTE_SPIELTAG_SPALTE + (anzSpieltage * ANZAHL_SPALTEN_IN_SUMME);
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		return getSchlechtesteSpielTageSpalte();
	}

	private int getAnzSpaltenInSpieltag() {
		return SuperMeleeSummenSpalten.ANZAHL_SPALTEN_IN_SUMME;
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return spielerSpalte.getLetzteMitDatenZeileInSpielerNrSpalte();
	}

	@Override
	public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
		return spielerSpalte.sucheLetzteZeileMitSpielerNummer();
	}

	@Override
	public int getErsteDatenZiele() throws GenerateException {
		return ERSTE_DATEN_ZEILE;
	}

	@Override
	public int getManuellSortSpalte() throws GenerateException {
		return getLetzteSpalte() + ERSTE_SORTSPALTE_OFFSET;
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

}
