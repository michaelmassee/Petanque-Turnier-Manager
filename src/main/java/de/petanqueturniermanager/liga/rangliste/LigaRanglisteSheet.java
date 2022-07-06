/**
 * Erstellung 09.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.rangliste;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.algorithmen.JederGegenJeden;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.rangliste.IRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_Update;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * @author Michael Massee
 */
public class LigaRanglisteSheet extends LigaSheet implements ISheet, IRangliste {

	private static final int MARGIN = 120;
	private static final Logger logger = LogManager.getLogger(LigaRanglisteSheet.class);
	public static final String SHEETNAME = "Rangliste";
	private static final String SHEET_COLOR = "d637e8";
	private static final int ERSTE_DATEN_ZEILE = 3; // Zeile 4
	private static final int TEAM_NR_SPALTE = 0; // Spalte A=0
	public static final int RANGLISTE_SPALTE = 2; // Spalte C=2

	private static final int ERSTE_SPIELTAG_SPALTE = TEAM_NR_SPALTE + 3; // nr + name + rangliste
	private static final int PUNKTE_NR_WIDTH = MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH;
	private static final int ANZ_SUMMEN_SPALTEN = 9; // Punkte +/- Spiele +/-/Diff SpielPunkte +/-/Diff + anz runden
	private static final int ERSTE_SORTSPALTE_OFFSET = 3; // zur letzte spalte = anz Spieltage

	private final MeldungenSpalte<TeamMeldungen, Team> meldungenSpalte;
	private final LigaMeldeListeSheet_Update meldeListe;
	private final RangListeSorter rangListeSorter;

	/**
	 * @param workingSpreadsheet
	 * @param logPrefix
	 * @throws GenerateException
	 */
	public LigaRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Liga-RanglisteSheet");
		meldungenSpalte = MeldungenSpalte.Builder().spalteMeldungNameWidth(LIGA_MELDUNG_NAME_WIDTH)
				.ersteDatenZiele(ERSTE_DATEN_ZEILE).spielerNrSpalte(TEAM_NR_SPALTE).sheet(this)
				.formation(Formation.TETE).anzZeilenInHeader(2).build();
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
		rangListeSorter = new RangListeSorter(this);
	}

	@VisibleForTesting
	LigaMeldeListeSheet_Update initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new LigaMeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
	}

	protected RangListeSorter getRangListeSorter() {
		return rangListeSorter;
	}

	private TeamMeldungen getAlleMeldungen() throws GenerateException {
		return meldeListe.getAlleMeldungen(); // inhalt kann sich später ändern
	}

	private JederGegenJeden newJederGegenJeden() throws GenerateException {
		return new JederGegenJeden(getAlleMeldungen());
	}

	/**
	 * @throws GenerateException
	 */
	public void upDateSheet() throws GenerateException {
		meldeListe.upDateSheet();

		getxCalculatable().enableAutomaticCalculation(false); // speed up
		if (!getAlleMeldungen().isValid()) {
			processBoxinfo("Abbruch, ungültige Anzahl von Melungen.");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Neue Liga-SpielPlan")
					.message("Ungültige anzahl von Melungen").show();
			return;
		}

		if (!NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.LIGA_ENDRANGLISTE).setForceCreate(true).setActiv()
				.hideGrid().tabColor(SHEET_COLOR).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Liga SpielPlan wurde nicht erstellt");
			return;
		}

		meldungenSpalte.alleAktiveUndAusgesetzteMeldungenAusmeldelisteEinfuegen(meldeListe);
		int headerBackColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		meldungenSpalte.insertHeaderInSheet(headerBackColor);
		spieltageFormulaEinfuegen(newJederGegenJeden());
		summenSpaltenEinfuegen();

		RangListeSpalte rangListeSpalte = new RangListeSpalte(RANGLISTE_SPALTE, this);
		rangListeSpalte.upDateRanglisteSpalte();
		rangListeSpalte.insertHeaderInSheet(headerBackColor);
		boolean zeigeArbeitsSpalten = getKonfigurationSheet().zeigeArbeitsSpalten();
		rangListeSorter.insertSortValidateSpalte(zeigeArbeitsSpalten);
		rangListeSorter.insertManuelsortSpalten(zeigeArbeitsSpalten);
		rangListeSorter.doSort();

		insertHeader();
		formatData();
		meldungenSpalte.formatDaten();
		addFooter();
		printBereichDefinieren();
		SheetFreeze.from(getTurnierSheet()).anzZeilen(3).anzSpalten(3).doFreeze();
	}

	private void printBereichDefinieren() throws GenerateException {
		processBoxinfo("Print-Bereich");
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(printBereichRangePosition());
	}

	public RangePosition printBereichRangePosition() throws GenerateException {
		Position linksOben = Position.from(0, 0);
		Position rechtsUnten = Position.from(getLetzteSpalte(), getFooterZeile());
		return RangePosition.from(linksOben, rechtsUnten);
	}

	public StringCellValue addFooter() throws GenerateException {
		processBoxinfo("Fußzeile einfügen");

		int ersteFooterZeile = getFooterZeile();
		StringCellValue stringVal = StringCellValue.from(this, Position.from(TEAM_NR_SPALTE, ersteFooterZeile))
				.setHoriJustify(CellHoriJustify.LEFT).setCharHeight(8);
		getSheetHelper().setStringValueInCell(stringVal.setValue(
				"Reihenfolge zur Ermittlung der Platzierung: 1. Punkte +, 2. Spiele +, 3. Spielpunkte Δ, 4. Direktvergleich"));
		return stringVal;
	}

	private int getFooterZeile() throws GenerateException {
		return getLetzteDatenZeile() + 2;
	}

	/**
	 * @throws GenerateException
	 */
	private void insertHeader() throws GenerateException {
		int headerBackColor = getKonfigurationSheet().getRanglisteHeaderFarbe();
		int anzGesamtRunden = anzGesamtRunden();
		RangeData data = new RangeData();
		RowData headerZeile3 = data.newRow();

		for (int rundeCntr = 0; rundeCntr < anzGesamtRunden; rundeCntr++) {
			// 6 spalten pro Runde(Spieltag)
			for (int i = 0; i < 3; i++) {
				headerZeile3.newString("+");
				headerZeile3.newString("-");
			}
		}
		// ------------------------------------------------------------------------------------------------------------------------------------------------------------
		// 3 header zeile
		// ------------------------------------------------------------------------------------------------------------------------------------------------------------
		// summen spalten
		headerZeile3.newString("+");
		headerZeile3.newString("-");
		headerZeile3.newString("+");
		headerZeile3.newString("-");
		headerZeile3.newString("Δ"); // Spiele diff Delta Δ
		headerZeile3.newString("+");
		headerZeile3.newString("-");
		headerZeile3.newString("Δ"); // Spiele diff Delta Δ

		TableBorder2 borderHeader3 = BorderFactory.from().allThin().boldLn().forBottom().toBorder();
		RangeProperties rangePropZeile3 = RangeProperties.from().centerJustify().setBorder(borderHeader3)
				.setCellBackColor(headerBackColor).margin(MARGIN);
		RangeHelper.from(this, data.getRangePosition(Position.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE - 1)))
				.setDataInRange(data).setRangeProperties(rangePropZeile3);

		// ------------------------------------------------------------------------------------------------------------------------------------------------------------
		// 2 header zeile
		// ------------------------------------------------------------------------------------------------------------------------------------------------------------
		// Runden
		Position header2Pos = Position.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE - 2);
		CellProperties headerProp = CellProperties.from().setAllThinBorder().margin(MARGIN).centerJustify()
				.setCellBackColor(headerBackColor).setShrinkToFit(true);
		StringCellValue header2val = StringCellValue.from(getXSpreadSheet()).setPos(header2Pos)
				.setEndPosMergeSpaltePlus(1).setCellProperties(headerProp);
		for (int rundeCntr = 0; rundeCntr < anzGesamtRunden; rundeCntr++) {
			header2val.setValue("Punkte");
			getSheetHelper().setStringValueInCell(header2val);

			header2val.spaltePlus(2);
			header2val.setValue("Spiele");
			getSheetHelper().setStringValueInCell(header2val);

			header2val.spaltePlus(2);
			header2val.setValue("SpPnkte");
			getSheetHelper().setStringValueInCell(header2val);

			header2val.spaltePlus(2);
		}

		// 2 header zeile
		// Summen
		header2val.setValue("Punkte");
		getSheetHelper().setStringValueInCell(header2val);

		header2val.spaltePlus(2);
		header2val.setValue("Spiele").setEndPosMergeSpaltePlus(2);
		getSheetHelper().setStringValueInCell(header2val);

		header2val.spaltePlus(3);
		header2val.setValue("Spielpunkte");
		getSheetHelper().setStringValueInCell(header2val);

		// ------------------------------------------------------------------------------------------------------------------------------------------------------------
		// 1 header zeile
		// ------------------------------------------------------------------------------------------------------------------------------------------------------------
		Position header1Pos = Position.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE - 3);
		StringCellValue header1val = StringCellValue.from(getXSpreadSheet()).setPos(header1Pos)
				.setEndPosMergeSpaltePlus(5).setCellProperties(headerProp);
		for (int rundeCntr = 0; rundeCntr < anzGesamtRunden; rundeCntr++) {
			header1val.setValue("Runde " + (rundeCntr + 1));
			getSheetHelper().setStringValueInCell(header1val);
			header1val.spaltePlus(6);
		}

		// 1 header zeile
		// summen
		header1val.setValue("Summen").setEndPosMergeSpaltePlus(7);
		getSheetHelper().setStringValueInCell(header1val);
		// Begegnungen

		StringCellValue begegnungenHeader = StringCellValue.from(getXSpreadSheet());
		begegnungenHeader.setPos(header1val.getPos()).spaltePlus(8);
		begegnungenHeader.setValue("Begegn.").setRotate90().setEndPosMergeZeilePlus(2).centerJustify()
				.setBorder(borderHeader3).setCellBackColor(headerBackColor).setShrinkToFit(true).setShrinkToFit(true)
				.setComment("Die Anzahl an gespielten Begegnungen");
		getSheetHelper().setStringValueInCell(begegnungenHeader);

	}

	/**
	 * @param jederGegenJedenf
	 * @throws GenerateException
	 */
	private void formatData() throws GenerateException {

		int anzGesamtRunden = anzGesamtRunden();
		int ersteSummeSpalte = getErsteSummeSpalte();
		int letzteDatenZeile = getLetzteDatenZeile();

		RangeProperties rangeProp = RangeProperties.from()
				.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder()).centerJustify().margin(MARGIN);
		RangeHelper.from(this, allDatenRange()).setRangeProperties(rangeProp);

		RanglisteGeradeUngeradeFormatHelper.from(this, allDatenRange())
				.geradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeGerade())
				.ungeradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade()).endeSpaltePlus(-1) // ende Spalte ist anz begegnungen
				.validateSpalte(validateSpalte()).apply();

		// Spalte Begegnungen
		RangePosition begegnungenSpalte = RangePosition.from(ersteSummeSpalte + (ANZ_SUMMEN_SPALTEN - 1),
				ERSTE_DATEN_ZEILE, ersteSummeSpalte + (ANZ_SUMMEN_SPALTEN - 1), ERSTE_DATEN_ZEILE + (anzZeilen() - 1));
		// "AND(" + FORMULA_ISEVEN_ROW + ";" + FORMULA_CURRENT_CELL + "=" + val + ")";
		String maxFormula = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<MAX(" + begegnungenSpalte.getAddressWith$()
				+ ")";
		String orangeFormulaEven = "AND(" + ConditionalFormatHelper.FORMULA_ISEVEN_ROW + ";" + maxFormula + ")";
		String orangeFormulaOdd = "AND(" + ConditionalFormatHelper.FORMULA_ISODD_ROW + ";" + maxFormula + ")";
		RanglisteGeradeUngeradeFormatHelper.from(this, begegnungenSpalte)
				.geradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeGerade())
				.ungeradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade())
				.orangeCharFormulaGerade(orangeFormulaEven).orangeCharFormulaUnGerade(orangeFormulaOdd).apply();

		// Runden
		RangePosition rundenErsteSpalteRange = RangePosition.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE - 3,
				ERSTE_SPIELTAG_SPALTE, letzteDatenZeile);
		for (int rundeCntr = 0; rundeCntr < anzGesamtRunden; rundeCntr++) {
			RangeHelper.from(this, rundenErsteSpalteRange).setRangeProperties(
					RangeProperties.from().setBorder(BorderFactory.from().boldLn().forLeft().toBorder()));
			rundenErsteSpalteRange.spaltePlus(6); // runde = 6 spalten
		}

		// Horizontal
		// Summen
		RangeHelper
				.from(this, ersteSummeSpalte, ERSTE_DATEN_ZEILE - 3, ersteSummeSpalte + ANZ_SUMMEN_SPALTEN - 1,
						letzteDatenZeile)
				.setRangeProperties(RangeProperties.from()
						.setBorder(BorderFactory.from().boldLn().forLeft().forRight().toBorder()));

		// doppelte trenn linien blöcke
		RangePosition trennPos = RangePosition.from(ersteSummeSpalte + 2, ERSTE_DATEN_ZEILE - 2, ersteSummeSpalte + 2,
				letzteDatenZeile);
		RangeHelper.from(this, trennPos).setRangeProperties(
				RangeProperties.from().setBorder(BorderFactory.from().doubleLn().forLeft().toBorder()));
		RangeHelper.from(this, trennPos.spaltePlus(3)).setRangeProperties(
				RangeProperties.from().setBorder(BorderFactory.from().doubleLn().forLeft().toBorder()));
		RangeHelper.from(this, trennPos.spaltePlus(3)).setRangeProperties(
				RangeProperties.from().setBorder(BorderFactory.from().doubleLn().forLeft().toBorder()));

	}

	private RangePosition allDatenRange() throws GenerateException {
		int ersteSummeSpalte = getErsteSummeSpalte();
		return RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, ersteSummeSpalte + (ANZ_SUMMEN_SPALTEN - 1),
				ERSTE_DATEN_ZEILE + (anzZeilen() - 1));
	}

	private int anzZeilen() throws GenerateException {
		return newJederGegenJeden().getAnzMeldungen();
	}

	private int anzGesamtRunden() throws GenerateException {
		return newJederGegenJeden().anzRunden() * 2; // *2 weil hin und rückrunde
	}

	private void summenSpaltenEinfuegen() throws GenerateException {
		int anzRunden = anzGesamtRunden();
		int ersteSummeSpalte = getErsteSummeSpalte();
		int autoFillDownZeilePlus = anzZeilen() - 1;
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(PUNKTE_NR_WIDTH + 100);
		ColumnProperties columnPropertiesSpielPnktSmn = ColumnProperties.from().setWidth(1100);

		int endSummeSpalteOffs = 0;
		for (int summeSpalte = 0; summeSpalte < 6; summeSpalte++) {
			Position summePos = Position.from(ersteSummeSpalte + endSummeSpalteOffs, ERSTE_DATEN_ZEILE);
			String summenFormulaSummeStr = "";
			for (int rndCnt = 0; rndCnt < anzRunden; rndCnt++) {
				Position punktePlusPos = Position.from(ERSTE_SPIELTAG_SPALTE + summeSpalte + (rndCnt * 6),
						ERSTE_DATEN_ZEILE);
				summenFormulaSummeStr += punktePlusPos.getAddress() + ((rndCnt + 1 < anzRunden) ? "+" : "");
			}
			StringCellValue summenFormulaSumme = StringCellValue.from(getXSpreadSheet()).setPos(summePos)
					.setFillAutoDownZeilePlus(autoFillDownZeilePlus).setValue(summenFormulaSummeStr)
					.setColumnProperties(columnProperties);

			if (endSummeSpalteOffs > 4) {
				summenFormulaSumme.setColumnProperties(columnPropertiesSpielPnktSmn);
			}

			getSheetHelper().setFormulaInCell(summenFormulaSumme);
			endSummeSpalteOffs++;

			// Spiele (Siege) Diff einfuegen ?
			if (endSummeSpalteOffs == 4) {
				Position summeSpielePlusPos = Position.from((ersteSummeSpalte + endSummeSpalteOffs) - 2,
						ERSTE_DATEN_ZEILE);
				Position summeSpieleMinusPos = Position.from((ersteSummeSpalte + endSummeSpalteOffs) - 1,
						ERSTE_DATEN_ZEILE);
				String summenFormulaDiffSpieleStr = summeSpielePlusPos.getAddress() + "-"
						+ summeSpieleMinusPos.getAddress();
				StringCellValue summenFormulaDiffSpiele = StringCellValue.from(getXSpreadSheet()).setPos(summePos)
						.spaltePlusEins().setFillAutoDownZeilePlus(autoFillDownZeilePlus)
						.setValue(summenFormulaDiffSpieleStr).setColumnProperties(columnProperties);
				getSheetHelper().setFormulaInCell(summenFormulaDiffSpiele);
				endSummeSpalteOffs++;
			}
		}

		// Spielpunkte summen diff

		Position summeSpielPnktPlusPos = Position.from((ersteSummeSpalte + endSummeSpalteOffs) - 2, ERSTE_DATEN_ZEILE);
		Position summeSpielPnktMinusPos = Position.from((ersteSummeSpalte + endSummeSpalteOffs) - 1, ERSTE_DATEN_ZEILE);
		String summenFormulaDiffSpielPnktStr = summeSpielPnktPlusPos.getAddress() + "-"
				+ summeSpielPnktMinusPos.getAddress();
		StringCellValue summenFormulaDiffSpielPnkt = StringCellValue.from(getXSpreadSheet())
				.setPos(summeSpielPnktMinusPos.spaltePlusEins()).setFillAutoDownZeilePlus(autoFillDownZeilePlus)
				.setValue(summenFormulaDiffSpielPnktStr).setColumnProperties(columnPropertiesSpielPnktSmn);
		getSheetHelper().setFormulaInCell(summenFormulaDiffSpielPnkt);

		// anz gespielte Runden
		Position summePunktPlusPos = Position.from(summeSpielPnktPlusPos).spaltePlus(-5);
		Position summePunktMinusPos = Position.from(summePunktPlusPos).spaltePlus(1);

		String summenAnzRundenFormula = summePunktPlusPos.getAddress() + "+" + summePunktMinusPos.getAddress();
		StringCellValue summenAnzRunden = StringCellValue.from(getXSpreadSheet())
				.setPos(summeSpielPnktMinusPos.spaltePlusEins()).setFillAutoDownZeilePlus(autoFillDownZeilePlus)
				.setValue(summenFormulaDiffSpielPnktStr).setColumnProperties(columnProperties)
				.setValue(summenAnzRundenFormula);
		getSheetHelper().setFormulaInCell(summenAnzRunden);
	}

	private void spieltageFormulaEinfuegen(JederGegenJeden jederGegenJeden) throws GenerateException {

		// =WENNNV(INDEX($'Liga Spielplan'.F3:F5;VERGLEICH(A3;$'Liga Spielplan'.O3:O5;0)); INDEX($'Liga Spielplan'.G3:G5;VERGLEICH(A3;$'Liga Spielplan'.P3:P5;0)))

		int erstePaarungSpieltagZeileInSpielplan = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		Position spielTagFormulaPos = Position.from(ERSTE_SPIELTAG_SPALTE, ERSTE_DATEN_ZEILE);

		int anzRunden = jederGegenJeden.anzRunden();
		int anzPaarungen = jederGegenJeden.anzPaarungen();

		for (int hinruckrunde = 0; hinruckrunde < 2; hinruckrunde++) {
			for (int rndCnt = 0; rndCnt < anzRunden; rndCnt++) {
				spieltagEinfuegen(erstePaarungSpieltagZeileInSpielplan, Position.from(spielTagFormulaPos),
						anzPaarungen);
				// 6 Spalten pro Spieltag
				spielTagFormulaPos.spaltePlus(6);
				erstePaarungSpieltagZeileInSpielplan += anzPaarungen;
			}
		}
	}

	private void spieltagEinfuegen(int erstePaarungSpieltagZeileInSpielplan, Position spielTagFormulaPos,
			int anzPaarungen) throws GenerateException {
		int[] spalteA = new int[] { LigaSpielPlanSheet.PUNKTE_A_SPALTE, LigaSpielPlanSheet.SPIELE_A_SPALTE,
				LigaSpielPlanSheet.SPIELPNKT_A_SPALTE };
		int[] spalteB = new int[] { LigaSpielPlanSheet.PUNKTE_B_SPALTE, LigaSpielPlanSheet.SPIELE_B_SPALTE,
				LigaSpielPlanSheet.SPIELPNKT_B_SPALTE };

		for (int idx = 0; idx < spalteA.length; idx++) {
			verweisAufErgbnisseEinfuegen(spalteA[idx], spalteB[idx], erstePaarungSpieltagZeileInSpielplan, anzPaarungen,
					Position.from(spielTagFormulaPos));
			spielTagFormulaPos.spaltePlus(2);
		}
	}

	private void verweisAufErgbnisseEinfuegen(int spalteA, int spalteB, int startZeile, int anzPaarungen,
			Position startFormulaPos) throws GenerateException {
		Position ersteTeamNrPos = Position.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE);

		Position punkteAStartPos = Position.from(spalteA, startZeile);
		Position punkteAEndePos = Position.from(punkteAStartPos).zeilePlus(anzPaarungen - 1);
		Position punkteBStartPos = Position.from(spalteB, startZeile);
		Position punkteBEndePos = Position.from(punkteBStartPos).zeilePlus(anzPaarungen - 1);

		String rangeStrAPlus = punkteAStartPos.getAddressWith$() + ":" + punkteAEndePos.getAddressWith$();
		String rangeStrBPlus = punkteBStartPos.getAddressWith$() + ":" + punkteBEndePos.getAddressWith$();

		Position teamNrAStartPos = Position.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE, startZeile);
		Position teamNrAEndePos = Position.from(teamNrAStartPos).zeilePlus(anzPaarungen - 1);
		Position teamNrBStartPos = Position.from(LigaSpielPlanSheet.TEAM_B_NR_SPALTE, startZeile);
		Position teamNrBEndePos = Position.from(teamNrBStartPos).zeilePlus(anzPaarungen - 1);

		String rangeStrATeamNr = teamNrAStartPos.getAddressWith$() + ":" + teamNrAEndePos.getAddressWith$();
		String rangeStrBTeamNr = teamNrBStartPos.getAddressWith$() + ":" + teamNrBEndePos.getAddressWith$();

		// @formatter:off
        String formulaPunktePlus = "IFNA(" + "INDEX($'" + LigaSpielPlanSheet.SHEET_NAMEN +"'." + rangeStrAPlus + ";MATCH("
                + ersteTeamNrPos.getAddress() + ";$'" + LigaSpielPlanSheet.SHEET_NAMEN +"'." + rangeStrATeamNr + ";0)" + ");"
                + "INDEX($'" + LigaSpielPlanSheet.SHEET_NAMEN +"'." + rangeStrBPlus + ";MATCH(" + ersteTeamNrPos.getAddress() + ";$'" + LigaSpielPlanSheet.SHEET_NAMEN +"'."
                + rangeStrBTeamNr + ";0)" + ")" + ")";

        String formulaPunkteMinus = "IFNA(" + "INDEX($'" + LigaSpielPlanSheet.SHEET_NAMEN +"'." + rangeStrBPlus + ";MATCH("
                + ersteTeamNrPos.getAddress() + ";$'" + LigaSpielPlanSheet.SHEET_NAMEN +"'." + rangeStrATeamNr + ";0)" + ");"
                + "INDEX($'" + LigaSpielPlanSheet.SHEET_NAMEN +"'." + rangeStrAPlus + ";MATCH(" + ersteTeamNrPos.getAddress() + ";$'" + LigaSpielPlanSheet.SHEET_NAMEN +"'."
                + rangeStrBTeamNr + ";0)" + ")" + ")";
        // @formatter:on
		boolean isvisable = getKonfigurationSheet().zeigeArbeitsSpalten();
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(PUNKTE_NR_WIDTH).isVisible(isvisable);

		StringCellValue spielTagFormula = StringCellValue.from(getXSpreadSheet()).setPos(startFormulaPos)
				.setValue(formulaPunktePlus).setFillAutoDownZeilePlus(anzZeilen() - 1)
				.addColumnProperties(columnProperties);
		getSheetHelper().setFormulaInCell(spielTagFormula);

		spielTagFormula.spaltePlusEins().setFillAutoDownZeilePlus(anzZeilen() - 1).setValue(formulaPunkteMinus);
		getSheetHelper().setFormulaInCell(spielTagFormula);

	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		return ERSTE_SPIELTAG_SPALTE + (anzGesamtRunden() * 6); // 6 = Anzahl summen spalte pro spieltag;
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		return getErsteSummeSpalte() + ANZ_SUMMEN_SPALTEN - 1;
	}

	@Override
	public int getLetzteDatenZeile() throws GenerateException {
		return getErsteDatenZiele() + anzZeilen() - 1;
	}

	@Override
	public int getErsteDatenZiele() throws GenerateException {
		return ERSTE_DATEN_ZEILE;
	}

	@Override
	public int getManuellSortSpalte() throws GenerateException {
		return getLetzteSpalte() + ERSTE_SORTSPALTE_OFFSET;
	}

	@Override
	public List<Position> getRanglisteSpalten() throws GenerateException {
		int punktespalte = getErsteSummeSpalte();
		Position sort1 = Position.from(punktespalte, getErsteDatenZiele());
		Position sort2 = Position.from(punktespalte + 2, getErsteDatenZiele());
		Position sort3 = Position.from(punktespalte + 7, getErsteDatenZiele());

		Position[] arraylist = new Position[] { sort1, sort2, sort3 };
		return Arrays.asList(arraylist);
	}

	@Override
	public int validateSpalte() throws GenerateException {
		int anzSortSpalten = getRanglisteSpalten().size();
		return getManuellSortSpalte() + anzSortSpalten + 1;
	}

	@Override
	public int getErsteSpalte() throws GenerateException {
		return TEAM_NR_SPALTE;
	}

	@Override
	public void calculateAll() {
		getxCalculatable().calculateAll();
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

}
