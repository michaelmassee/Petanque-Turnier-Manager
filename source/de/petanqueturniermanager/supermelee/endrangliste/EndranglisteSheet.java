/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.endrangliste;

import static de.petanqueturniermanager.helper.sheet.SummenSpalten.*;

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
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.RanglisteHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.StreichSpieltagHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.StreichSpieltagHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.rangliste.AbstractRanglisteFormatter;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SpielerSpalte;
import de.petanqueturniermanager.helper.sheet.SummenSpalten;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.ergebnis.SpielerSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class EndranglisteSheet extends SheetRunner implements IEndRangliste {
	private static final Logger logger = LogManager.getLogger(EndranglisteSheet.class);

	public static final int ERSTE_DATEN_ZEILE = 3; // Zeile 4
	public static final int SPIELER_NR_SPALTE = 0; // Spalte A=0, B=1
	public static final int RANGLISTE_SPALTE = 2; // Spalte C=2
	public static final int ERSTE_SPIELTAG_SPALTE = 3; // Spalte D=3

	public static final int ERSTE_SORTSPALTE_OFFSET = 3; // zur letzte spalte = anz Spieltage

	public static final String SHEETNAME = "Endrangliste";
	public static final String SHEET_COLOR = "d637e8";

	private final SpieltagRanglisteSheet spieltagRanglisteSheet;
	private final SpielerSpalte spielerSpalte;
	private final MeldeListeSheet_New meldeListeSheetNew;
	private final KonfigurationSheet konfigurationSheet;
	private final EndRanglisteFormatter endRanglisteFormatter;
	private final RangListeSpalte rangListeSpalte;
	private final RangListeSorter rangListeSorter;

	public EndranglisteSheet(XComponentContext xContext) {
		super(xContext, "Endrangliste");
		this.konfigurationSheet = new KonfigurationSheet(xContext);
		this.spieltagRanglisteSheet = new SpieltagRanglisteSheet(xContext);
		this.meldeListeSheetNew = new MeldeListeSheet_New(xContext);
		this.spielerSpalte = new SpielerSpalte(xContext, ERSTE_DATEN_ZEILE, SPIELER_NR_SPALTE, this, this.meldeListeSheetNew, Formation.MELEE);
		this.endRanglisteFormatter = new EndRanglisteFormatter(this, getAnzSpaltenInSpieltag(), this.spielerSpalte, ERSTE_SPIELTAG_SPALTE, this.konfigurationSheet);
		this.rangListeSpalte = new RangListeSpalte(xContext, RANGLISTE_SPALTE, this);
		this.rangListeSorter = new RangListeSorter(xContext, this);
	}

	@Override
	protected void doRun() throws GenerateException {
		SpielTagNr spieltagNr = this.konfigurationSheet.getAktiveSpieltag();
		if (NewSheet.from(getxContext(), SHEETNAME).pos(DefaultSheetPos.SUPERMELEE_ENDRANGLISTE).tabColor(SHEET_COLOR).setActiv().forceCreate().spielTagPageStyle(spieltagNr)
				.create().isDidCreate()) {
			getxCalculatable().enableAutomaticCalculation(false); // speed up
			upDateSheet();
		}
	}

	private void upDateSheet() throws GenerateException {

		int anzahlSpieltage = getAnzahlSpieltage();
		if (anzahlSpieltage < 2) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Fehler").message("Ungültige anzahl von Spieltage. " + anzahlSpieltage).show();
			return;
		}

		Integer headerColor = this.konfigurationSheet.getRanglisteHeaderFarbe();

		spielerEinfügen();
		this.spielerSpalte.insertHeaderInSheet(headerColor);
		this.spielerSpalte.formatDaten();
		this.endRanglisteFormatter.updateHeader();

		spielTageEinfuegen();
		getxCalculatable().calculate();
		updateEndSummenSpalten();

		this.rangListeSorter.insertSortValidateSpalte();
		this.rangListeSorter.insertManuelsortSpalten();

		this.endRanglisteFormatter.formatDaten();
		this.rangListeSpalte.upDateRanglisteSpalte();
		this.rangListeSpalte.insertHeaderInSheet(headerColor);

		updateAnzSpieltageSpalte();
		getxCalculatable().calculate();
		formatDatenGeradeUngeradeMitStreichSpieltag();
		formatSchlechtesteSpieltagSpalte();
		getxCalculatable().calculate();
		this.rangListeSorter.doSort();
		this.endRanglisteFormatter.addFooter();
	}

	private void formatDatenGeradeUngeradeMitStreichSpieltag() throws GenerateException {

		processBoxinfo("Formatiere gerade Ungerade Zeilen");

		// gerade / ungrade hintergrund farbe
		// CellBackColor
		int spielerNrSpalte = this.spielerSpalte.getSpielerNrSpalte();
		int ersteDatenZeile = this.spielerSpalte.getErsteDatenZiele();
		int letzteDatenZeile = this.spielerSpalte.getLetzteDatenZeile();
		int letzteSpalte = getLetzteSpalte();

		Integer streichSpieltag_geradeColor = this.konfigurationSheet.getRanglisteHintergrundFarbe_StreichSpieltag_Gerade();
		Integer streichSpieltag_unGeradeColor = this.konfigurationSheet.getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade();
		StreichSpieltagHintergrundFarbeGeradeStyle streichSpieltagHintergrundFarbeGeradeStyle = new StreichSpieltagHintergrundFarbeGeradeStyle(streichSpieltag_geradeColor);
		StreichSpieltagHintergrundFarbeUnGeradeStyle streichSpieltagHintergrundFarbeUnGeradeStyle = new StreichSpieltagHintergrundFarbeUnGeradeStyle(streichSpieltag_unGeradeColor);

		Integer geradeColor = this.konfigurationSheet.getRanglisteHintergrundFarbeGerade();
		Integer unGeradeColor = this.konfigurationSheet.getRanglisteHintergrundFarbeUnGerade();
		RanglisteHintergrundFarbeGeradeStyle ranglisteHintergrundFarbeGeradeStyle = new RanglisteHintergrundFarbeGeradeStyle(geradeColor);
		RanglisteHintergrundFarbeUnGeradeStyle ranglisteHintergrundFarbeUnGeradeStyle = new RanglisteHintergrundFarbeUnGeradeStyle(unGeradeColor);

		RangePosition datenRange = RangePosition.from(spielerNrSpalte, ersteDatenZeile, letzteSpalte, letzteDatenZeile);

		// Formula fuer sort error, komplette zeile rot einfärben wenn fehler meldung
		// Achtung spalte plus 1 weil A ist nicht 0 sondern 1
		String formulaSortError = "LEN(TRIM(INDIRECT(ADDRESS(ROW();" + (this.rangListeSorter.validateSpalte() + 1) + "))))>0";

		ConditionalFormatHelper.from(this, datenRange).clear().
		// -----------------------------
		// Formula fuer sort error, komplette zeile rot einfärben wenn fehler meldung
				formula1(formulaSortError).operator(ConditionOperator.FORMULA).style(new FehlerStyle()).applyNew().
				// ------------------------
				// Formula fuer streichspieltag
				// UND(INDIREKT(ADRESSE(ZEILE();13;4;))=AUFRUNDEN((SPALTE()-1)/3);ISTGERADE(ZEILE()))
				formula1(getFormulastreichSpieltag(true)).operator(ConditionOperator.FORMULA).style(streichSpieltagHintergrundFarbeGeradeStyle).applyNew().
				// ---------------------
				formula1(getFormulastreichSpieltag(false)).operator(ConditionOperator.FORMULA).style(streichSpieltagHintergrundFarbeUnGeradeStyle).applyNew().
				// --------------------------
				formulaIsEvenRow().style(ranglisteHintergrundFarbeGeradeStyle).applyNew().
				// ---------------------------
				formulaIsOddRow().style(ranglisteHintergrundFarbeUnGeradeStyle).apply();

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
		String drittePruefung = verweisAufStreichSpalte + "=ROUNDUP((COLUMN()-" + ERSTE_SPIELTAG_SPALTE + ")/" + anzSpaltenInSpieltag + ";0)";
		// 4. prüfen ob gerade oder ungerade zeile
		String viertePruefung = isCondition + "(ROW())";
		return "AND(" + erstePruefung + ";" + zweitePruefung + ";" + drittePruefung + ";" + viertePruefung + ")";
	}

	private void formatSchlechtesteSpieltagSpalte() throws GenerateException {

		processBoxinfo("Formatiere Streichspieltag");

		int schlechtesteSpielTageSpalte = getSchlechtesteSpielTageSpalte();
		NumberCellValue numberCellValueSchlechtesteSpielTag = NumberCellValue.from(getSheet(), schlechtesteSpielTageSpalte, ERSTE_DATEN_ZEILE);

		// Header Streichspieltag
		Position startStreichspieltag = Position.from(getSchlechtesteSpielTageSpalte(), AbstractRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position endStreichspieltag = Position.from(startStreichspieltag).zeilePlus(2);

		CellProperties columnProperties = CellProperties.from().setWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue headerStreichspieltag = StringCellValue.from(getSheet(), startStreichspieltag).setEndPosMerge(endStreichspieltag).setCharWeight(FontWeight.LIGHT)
				.setRotateAngle(27000).setVertJustify(CellVertJustify2.CENTER).setValue("Streich").setCellBackColor(this.endRanglisteFormatter.getHeaderFarbe())
				.setBorder(BorderFactory.from().allBold().toBorder()).setComment("Streich-Spieltag").setColumnProperties(columnProperties);
		getSheetHelper().setTextInCell(headerStreichspieltag);
		// Daten
		RangePosition rangPos = RangePosition.from(getSchlechtesteSpielTageSpalte(), ERSTE_DATEN_ZEILE, getSchlechtesteSpielTageSpalte(), getLetzteDatenZeile());
		CellProperties celRangeProp = CellProperties.from().setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER);
		getSheetHelper().setPropertiesInRange(getSheet(), rangPos, celRangeProp);

		for (Integer spielerNr : this.spielerSpalte.getSpielerNrList()) {
			SheetRunner.testDoCancelTask();
			SpielTagNr spielTagNr = schlechtesteSpieltag(spielerNr);
			int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielerNr);
			if (spielTagNr != null && spielerZeile > 0) {
				getSheetHelper().setValInCell(numberCellValueSchlechtesteSpielTag.zeile(spielerZeile).setValue((double) spielTagNr.getNr()));
			}
		}
	}

	private void spielerEinfügen() throws GenerateException {
		int anzSpieltage = getAnzahlSpieltage();

		HashSet<Integer> spielerNummer = new HashSet<>();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			List<Integer> spielerListe = this.spieltagRanglisteSheet.getSpielerNrList(SpielTagNr.from(spieltagCntr));
			spielerNummer.addAll(spielerListe);
		}
		this.spielerSpalte.alleSpielerNrEinfuegen(spielerNummer);
	}

	private int getSpielTagErsteSummeSpalte(SpielTagNr nr) throws GenerateException {
		return ERSTE_SPIELTAG_SPALTE + ((nr.getNr() - 1) * getAnzSpaltenInSpieltag());
	}

	private void spielTageEinfuegen() throws GenerateException {

		processBoxinfo("Spieltage Einfuegen");

		// verwende fill down
		// =WENNNV(SVERWEIS(A4;$'2. Spieltag Rangliste'.$A4:$D1000;4;0);"")

		int anzSpieltage = getAnzahlSpieltage();
		int letzteDatenZeile = this.spielerSpalte.getLetzteDatenZeile();

		String verweisAufSpalteSpielerNr = "INDIRECT(ADDRESS(ROW();" + (SPIELER_NR_SPALTE + 1) + ";8))";

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			int spieltagSummeErsteSpalte = getSpielTagErsteSummeSpalte(SpielTagNr.from(spieltagCntr));
			Position positionSumme = Position.from(spieltagSummeErsteSpalte, ERSTE_DATEN_ZEILE);
			StringCellValue strVal = StringCellValue.from(getSheet(), positionSumme);

			for (int summeSpalteCntr = 0; summeSpalteCntr < getAnzSpaltenInSpieltag(); summeSpalteCntr++) {
				SheetRunner.testDoCancelTask();
				String verweisAufSummeSpalte = this.spieltagRanglisteSheet.formulaSverweisAufSummeSpalte(SpielTagNr.from(spieltagCntr), summeSpalteCntr, verweisAufSpalteSpielerNr);
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
		int letzteZeile = getLetzteDatenZeile();

		Position ersteSpielTagErsteZelle = Position.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE);
		Position letzteSpielTagLetzteZelle = Position.from(letzteSpieltagLetzteSpalte, ERSTE_DATEN_ZEILE);

		String formula = "=COUNTIF(" + ersteSpielTagErsteZelle.getAddress() + ":" + letzteSpielTagLetzteZelle.getAddress() + ";\"<>\")/" + ANZAHL_SPALTEN_IN_SUMME;

		// letzte Spalte ist anzahl spieltage
		CellProperties celColumProp = CellProperties.from().setWidth(SpielerSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).setHoriJustify(CellHoriJustify.CENTER);
		StringCellValue formulaVal = StringCellValue.from(getSheet(), Position.from(anzSpielTageSpalte(), ERSTE_DATEN_ZEILE)).setValue(formula).setFillAutoDown(letzteZeile)
				.setColumnProperties(celColumProp);
		getSheetHelper().setFormulaInCell(formulaVal);

		// Spalte formatieren
		// Header AnzahlTage
		Position start = Position.from(anzSpielTageSpalte(), AbstractRanglisteFormatter.ERSTE_KOPFDATEN_ZEILE);
		Position end = Position.from(start).zeilePlus(2);
		StringCellValue headerAnzTage = StringCellValue.from(getSheet(), start).setEndPosMerge(end).setCharWeight(FontWeight.LIGHT).setRotateAngle(27000)
				.setVertJustify(CellVertJustify2.CENTER).setValue("Tage").setCellBackColor(this.endRanglisteFormatter.getHeaderFarbe())
				.setBorder(BorderFactory.from().allBold().toBorder()).setComment("Gespielte Tage");
		getSheetHelper().setTextInCell(headerAnzTage);

		// Daten
		RangePosition rangPos = RangePosition.from(formulaVal.getPos(), formulaVal.getFillAuto());
		CellProperties celRangeProp = CellProperties.from().setBorder(BorderFactory.from().allThin().boldLn().forLeft().forTop().forRight().toBorder());
		getSheetHelper().setPropertiesInRange(getSheet(), rangPos, celRangeProp);

	}

	private void updateEndSummenSpalten() throws GenerateException {

		processBoxinfo("Summen Spalten Aktualisieren");

		List<Integer> spielerNrList = this.spielerSpalte.getSpielerNrList();
		for (int spielerNr : spielerNrList) {
			endSummeSpalte(spielerNr);
		}
	}

	private void endSummeSpalte(int spielrNr) throws GenerateException {
		SpielTagNr schlechtesteSpielTag = schlechtesteSpieltag(spielrNr);
		int anzSpieltage = getAnzahlSpieltage();
		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);

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

			StringCellValue endsummeFormula = StringCellValue.from(getSheet(), Position.from(ersteSpalteEndsumme + summeSpalteCntr, spielerZeile));
			endsummeFormula.setValue("SUM(" + felderList[summeSpalteCntr] + ")");
			getSheetHelper().setFormulaInCell(endsummeFormula);
		}
	}

	private SpielTagNr schlechtesteSpieltag(int spielrNr) throws GenerateException {

		int anzSpieltage = getAnzahlSpieltage();
		if (anzSpieltage < 2) {
			return null;
		}
		List<SpielerSpieltagErgebnis> spielerSpieltagErgebniss = spielerErgebnisseEinlesen(spielrNr);
		spielerSpieltagErgebniss.sort(new Comparator<SpielerSpieltagErgebnis>() {
			@Override
			public int compare(SpielerSpieltagErgebnis o1, SpielerSpieltagErgebnis o2) {
				// schlechteste oben
				return o1.reversedCompareTo(o2);
			}
		});
		if (spielerSpieltagErgebniss.size() > 0) {
			return spielerSpieltagErgebniss.get(0).getSpielTag();
		}
		return null;
	}

	private List<SpielerSpieltagErgebnis> spielerErgebnisseEinlesen(int spielrNr) throws GenerateException {
		List<SpielerSpieltagErgebnis> spielerErgebnisse = new ArrayList<>();
		int anzSpieltage = getAnzahlSpieltage();

		int spielerZeile = this.spielerSpalte.getSpielerZeileNr(spielrNr);

		XSpreadsheet sheet = getSheet();

		for (int spieltagCntr = 1; spieltagCntr <= anzSpieltage; spieltagCntr++) {
			SheetRunner.testDoCancelTask();

			SpielTagNr spielTagNr = SpielTagNr.from(spieltagCntr);

			int ersteSpieltagSummeSpalte = ERSTE_SPIELTAG_SPALTE + ((spieltagCntr - 1) * ANZAHL_SPALTEN_IN_SUMME);
			// summe vorhanden ?
			String spielPlus = getSheetHelper().getTextFromCell(sheet, Position.from(ersteSpieltagSummeSpalte, spielerZeile));
			if (StringUtils.isNotBlank(spielPlus)) {
				SpielerSpieltagErgebnis ergebniss = new SpielerSpieltagErgebnis(spielTagNr, spielrNr);
				ergebniss.setSpielPlus(NumberUtils.toInt(spielPlus));
				ergebniss.setSpielMinus(NumberUtils.toInt(getSheetHelper().getTextFromCell(sheet, Position.from(ersteSpieltagSummeSpalte + SPIELE_MINUS_OFFS, spielerZeile))));
				ergebniss.setPunktePlus(NumberUtils.toInt(getSheetHelper().getTextFromCell(sheet, Position.from(ersteSpieltagSummeSpalte + PUNKTE_PLUS_OFFS, spielerZeile))));
				ergebniss.setPunkteMinus(NumberUtils.toInt(getSheetHelper().getTextFromCell(sheet, Position.from(ersteSpieltagSummeSpalte + PUNKTE_MINUS_OFFS, spielerZeile))));
				spielerErgebnisse.add(ergebniss);
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
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public int getAnzahlSpieltage() throws GenerateException {
		return this.spieltagRanglisteSheet.countNumberOfSpieltage();
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
		return SummenSpalten.ANZAHL_SPALTEN_IN_SUMME;
	}

	@Override
	public int getLetzteDatenZeile() throws GenerateException {
		return this.spielerSpalte.getLetzteDatenZeile();
	}

	@Override
	public int getErsteDatenZiele() throws GenerateException {
		return ERSTE_DATEN_ZEILE;
	}

	@Override
	public int getManuellSortSpalte() throws GenerateException {
		return getLetzteSpalte() + ERSTE_SORTSPALTE_OFFSET;
	}

	protected RangListeSorter getRangListeSorter() {
		return this.rangListeSorter;
	}

}
