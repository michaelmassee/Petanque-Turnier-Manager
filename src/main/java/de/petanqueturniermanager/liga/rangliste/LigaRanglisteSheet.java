/*
 * Erstellung 09.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.rangliste;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.rangliste.IRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncSignaturStore;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetUpdate;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * @author Michael Massee
 */
public class LigaRanglisteSheet extends SheetRunner implements ISheet, IRangliste {

	private static final int MARGIN = 120;
	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE;

	static final int ERSTE_DATEN_ZEILE = 2;
	static final int TEAM_NR_SPALTE = 0;
	static final int TEAM_NAME_SPALTE = 1;
	public static final int RANGLISTE_SPALTE = 2;
	static final int PUNKTE_PLUS_SPALTE = 3;
	private static final int PUNKTE_MINUS_SPALTE = 4;
	static final int SPIELE_PLUS_SPALTE = 5;
	private static final int SPIELE_MINUS_SPALTE = 6;
	private static final int SPIELE_DIFF_SPALTE = 7;
	static final int SP_PUNKTE_PLUS_SPALTE = 8;
	private static final int SP_PUNKTE_MINUS_SPALTE = 9;
	static final int SP_PUNKTE_DIFF_SPALTE = 10;
	private static final int BEGEGNUNGEN_SPALTE = 11;

	private static final int COL_WIDTH_NR = 800;
	private static final int COL_WIDTH_NAME = 7000;
	private static final int COL_WIDTH_RANGLISTE = 800;
	private static final int COL_WIDTH_DATA = 1400;
	private static final int MAX_SPIELPLAN_ZEILEN = 5000;

	private final LigaKonfigurationSheet konfigurationSheet;
	private final LigaMeldeListeSheetUpdate meldeListe;
	private final RangListeSorter rangListeSorter;

	public LigaRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.LIGA, "Liga-RanglisteSheet");
		konfigurationSheet = new LigaKonfigurationSheet(workingSpreadsheet);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
		rangListeSorter = new RangListeSorter(this);
	}

	@Override
	protected LigaKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	LigaMeldeListeSheetUpdate initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new LigaMeldeListeSheetUpdate(workingSpreadsheet);
	}

	protected LigaMeldeListeSheetUpdate getMeldeListe() {
		return meldeListe;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, SheetNamen.LEGACY_RANGLISTE);
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
	}

	protected RangListeSorter getRangListeSorter() {
		return rangListeSorter;
	}

	public TeamMeldungen getAlleMeldungen() throws GenerateException {
		return meldeListe.getAlleMeldungen();
	}

	public void upDateSheet() throws GenerateException {
		meldeListe.upDateSheet();

		getxCalculatable().enableAutomaticCalculation(false);
		TeamMeldungen aktiveMeldungen = getAlleMeldungen();
		if (!aktiveMeldungen.isValid()) {
			processBoxinfo("processbox.abbruch");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption(I18n.get("msg.caption.liga.spielplan"))
					.message(I18n.get("msg.text.ungueltige.anzahl.meldungen")).show();
			return;
		}

		if (!NewSheet.from(this, SheetNamen.rangliste(), METADATA_SCHLUESSEL)
				.pos(DefaultSheetPos.LIGA_ENDRANGLISTE).setForceCreate(true).setActiv()
				.hideGrid().tabColor(getKonfigurationSheet().getRanglisteTabFarbe()).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Liga Rangliste wurde nicht erstellt");
			return;
		}

		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			return;
		}

		berechnungUndSchreiben(sheet, meldeListe, aktiveMeldungen);

		if (SheetRunner.isRunning()) {
			getSheetHelper().setActiveSheet(sheet);
			SheetRunner.unterdrückeNaechstesSelectionChange();
		}
		SheetSyncSignaturStore.commitVollaufbau(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				METADATA_SCHLUESSEL,
				new EingabeSignatur(SignaturQuellen::fuerLiga));
	}

	protected void berechnungUndSchreiben(XSpreadsheet sheet, LigaMeldeListeSheetUpdate meldeliste,
			TeamMeldungen aktiveMeldungen) throws GenerateException {
		processBoxinfo("processbox.rangliste.einfuegen");

		insertHeader(sheet);
		setzeSpaltenBreiten(sheet);

		Map<Integer, String> teamNamen = meldeliste.leseTeamNamenMap();
		List<LigaTeamStats> sortiert = berechneUndSortiere(aktiveMeldungen, teamNamen);

		schreibeDatenAlsWerte(sheet, sortiert);

		if (!sortiert.isEmpty()) {
			new RangListeSpalte(RANGLISTE_SPALTE, this).upDateRanglisteSpalte();
			rangListeSorter.insertSortValidateSpalte(true);
			rangListeSorter.insertManuelsortSpalten(false);
			rangListeSorter.doSort();
			formatiereZebraStreifen(sheet, sortiert.size());
			formatiereBegegnungenSpalte(sheet, sortiert.size());
		}

		addFooter();
		printBereichDefinieren();
		SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).anzSpalten(RANGLISTE_SPALTE + 1).doFreeze();
		getxCalculatable().calculateAll();
	}

	private List<LigaTeamStats> berechneUndSortiere(TeamMeldungen aktiveMeldungen,
			Map<Integer, String> teamNamen) throws GenerateException {
		Map<Integer, LigaTeamStats> statsMap = leseSpielplanStats(aktiveMeldungen, teamNamen);
		List<LigaTeamStats> daten = new ArrayList<>();
		for (Team team : aktiveMeldungen.teams()) {
			daten.add(statsMap.getOrDefault(team.getNr(), new LigaTeamStats(team.getNr(),
					teamNamen.getOrDefault(team.getNr(), ""), 0, 0, 0, 0, 0, 0)));
		}
		daten.sort(Comparator.comparingInt(LigaTeamStats::punktePlus).reversed()
				.thenComparing(Comparator.comparingInt(LigaTeamStats::spielePlus).reversed())
				.thenComparing(Comparator.comparingInt(LigaTeamStats::spPunktePlus).reversed())
				.thenComparing(Comparator.comparingInt(LigaTeamStats::spPunkteDiff).reversed()));
		return daten;
	}

	private Map<Integer, LigaTeamStats> leseSpielplanStats(TeamMeldungen aktiveMeldungen,
			Map<Integer, String> teamNamen) throws GenerateException {
		Map<Integer, LigaTeamStats> statsMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new LigaTeamStats(team.getNr(),
					teamNamen.getOrDefault(team.getNr(), ""), 0, 0, 0, 0, 0, 0));
		}

		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		XSpreadsheet spielplanSheet = SheetMetadataHelper.findeSheetUndHeile(
				xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN, LigaSpielPlanSheet.LEGACY_SHEET_NAMEN);
		if (spielplanSheet == null) {
			return statsMap;
		}

		int startZeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		RangeData teamNrData = RangeHelper.from(spielplanSheet, xDoc,
				RangePosition.from(LigaSpielPlanSheet.TEAM_A_NR_SPALTE, startZeile,
						LigaSpielPlanSheet.TEAM_B_NR_SPALTE, startZeile + MAX_SPIELPLAN_ZEILEN))
				.getDataFromRange();

		// PUNKTE_A(8) bis SPIELPNKT_B(13) = 6 Spalten
		RangeData ergebnisData = RangeHelper.from(spielplanSheet, xDoc,
				RangePosition.from(LigaSpielPlanSheet.PUNKTE_A_SPALTE, startZeile,
						LigaSpielPlanSheet.SPIELPNKT_B_SPALTE, startZeile + MAX_SPIELPLAN_ZEILEN))
				.getDataFromRange();

		int freispielSpPktPlus = konfigurationSheet.getFreispielPunktePlus();
		int freispielSpPktMinus = konfigurationSheet.getFreispielPunkteMinus();

		for (int i = 0; i < teamNrData.size(); i++) {
			RowData teamNrZeile = teamNrData.get(i);
			if (teamNrZeile.size() < 2) {
				break;
			}
			int nrA = teamNrZeile.get(0).getIntVal(0);
			if (nrA <= 0) {
				continue;
			}
			int nrB = teamNrZeile.get(1).getIntVal(0);
			RowData erg = ergebnisData.get(i);
			if (erg.size() < 6) {
				continue;
			}
			int pktA = erg.get(0).getIntVal(0);
			int pktB = erg.get(1).getIntVal(0);
			int spA = erg.get(2).getIntVal(0);
			int spB = erg.get(3).getIntVal(0);
			int spPktA = erg.get(4).getIntVal(0);
			int spPktB = erg.get(5).getIntVal(0);

			if (nrB <= 0) {
				// Freispiel: immer ein Sieg für Team A; Spielpunkte aus Konfiguration
				statsMap.computeIfPresent(nrA, (k, s) -> s.plus(1, 0, spA, 0, freispielSpPktPlus, freispielSpPktMinus));
				continue;
			}

			if (pktA <= 0 && pktB <= 0 && spA <= 0 && spB <= 0 && spPktA <= 0 && spPktB <= 0) {
				continue; // noch nicht gespielt
			}

			statsMap.computeIfPresent(nrA, (k, s) -> s.plus(pktA, pktB, spA, spB, spPktA, spPktB));
			statsMap.computeIfPresent(nrB, (k, s) -> s.plus(pktB, pktA, spB, spA, spPktB, spPktA));
		}
		return statsMap;
	}

	private void schreibeDatenAlsWerte(XSpreadsheet sheet, List<LigaTeamStats> sortiert) throws GenerateException {
		if (sortiert.isEmpty()) {
			return;
		}
		int letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() - 1;

		// Block 1: Nr, Name
		RangeData block1 = new RangeData();
		for (LigaTeamStats s : sortiert) {
			RowData row = block1.addNewRow();
			row.newInt(s.teamNr());
			row.newString(s.teamName());
		}
		RangeHelper.from(this, RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NAME_SPALTE, letzteZeile))
				.setDataInRange(block1);

		// Block 2: Statistik (9 Spalten)
		RangeData block2 = new RangeData();
		for (LigaTeamStats s : sortiert) {
			RowData row = block2.addNewRow();
			row.newInt(s.punktePlus());
			row.newInt(s.punkteMinus());
			row.newInt(s.spielePlus());
			row.newInt(s.spieleMinus());
			row.newInt(s.spieleDiff());
			row.newInt(s.spPunktePlus());
			row.newInt(s.spPunkteMinus());
			row.newInt(s.spPunkteDiff());
			row.newInt(s.begegnungen());
		}
		RangeHelper.from(this, block2.getRangePosition(Position.from(PUNKTE_PLUS_SPALTE, ERSTE_DATEN_ZEILE)))
				.setDataInRange(block2);

		// Nr-Spalte: grau + doppelte rechte Linie
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE, letzteZeile),
				CellProperties.from()
						.setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR)
						.setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder())
						.margin(MARGIN));

		// Name-Spalte: linksbündig
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(TEAM_NAME_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NAME_SPALTE, letzteZeile),
				CellProperties.from().setAllThinBorder().setHoriJustify(CellHoriJustify.LEFT).margin(MARGIN));

		// Statistik-Spalten: zentriert, thin border, bold-top
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(RANGLISTE_SPALTE, ERSTE_DATEN_ZEILE, BEGEGNUNGEN_SPALTE, letzteZeile),
				CellProperties.from().setAllThinBorder()
						.setBorder(BorderFactory.from().allThin().boldLn().forTop().toBorder())
						.setHoriJustify(CellHoriJustify.CENTER).margin(MARGIN));

		// Nr-Spalte: durchgängig doppelte rechte Linie
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE - 2, TEAM_NR_SPALTE, letzteZeile),
				CellProperties.from().setBorder(BorderFactory.from().doubleLn().forRight().toBorder()));

		// Platz-Spalte: durchgängig dicke rechte Linie
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(RANGLISTE_SPALTE, ERSTE_DATEN_ZEILE - 2, RANGLISTE_SPALTE, letzteZeile),
				CellProperties.from().setBorder(BorderFactory.from().boldLn().forRight().toBorder()));

		// Begegnungen-Spalte: durchgängig dicke linke Linie
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(BEGEGNUNGEN_SPALTE, ERSTE_DATEN_ZEILE - 2, BEGEGNUNGEN_SPALTE, letzteZeile),
				CellProperties.from().setBorder(BorderFactory.from().boldLn().forLeft().toBorder()));

		// Doppelte Trennlinien: Spiele-Block (col 5) und SpPunkte-Block (col 8)
		for (int spalte : new int[] { SPIELE_PLUS_SPALTE, SP_PUNKTE_PLUS_SPALTE }) {
			getSheetHelper().setPropertiesInRange(sheet,
					RangePosition.from(spalte, ERSTE_DATEN_ZEILE - 2, spalte, letzteZeile),
					CellProperties.from().setBorder(BorderFactory.from().doubleLn().forLeft().toBorder()));
		}
	}

	private void formatiereZebraStreifen(XSpreadsheet sheet, int anzTeams) throws GenerateException {
		int letzteZeile = ERSTE_DATEN_ZEILE + anzTeams - 1;
		RangePosition datenRange = RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, SP_PUNKTE_DIFF_SPALTE, letzteZeile);
		RanglisteGeradeUngeradeFormatHelper.from(this, datenRange)
				.geradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeGerade())
				.ungeradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeUnGerade())
				.validateSpalte(validateSpalte())
				.apply();
	}

	private void formatiereBegegnungenSpalte(XSpreadsheet sheet, int anzTeams) throws GenerateException {
		int letzteZeile = ERSTE_DATEN_ZEILE + anzTeams - 1;
		RangePosition begegnungenSpalte = RangePosition.from(BEGEGNUNGEN_SPALTE, ERSTE_DATEN_ZEILE,
				BEGEGNUNGEN_SPALTE, letzteZeile);
		String maxFormula = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<MAX(" + begegnungenSpalte.getAddressWith$() + ")";
		String orangeFormulaGerade = "AND(" + ConditionalFormatHelper.FORMULA_ISEVEN_ROW + ";" + maxFormula + ")";
		String orangeFormulaUngerade = "AND(" + ConditionalFormatHelper.FORMULA_ISODD_ROW + ";" + maxFormula + ")";
		RanglisteGeradeUngeradeFormatHelper.from(this, begegnungenSpalte)
				.geradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeGerade())
				.ungeradeFarbe(konfigurationSheet.getRanglisteHintergrundFarbeUnGerade())
				.orangeCharFormulaGerade(orangeFormulaGerade)
				.orangeCharFormulaUnGerade(orangeFormulaUngerade)
				.apply();
	}

	private void setzeSpaltenBreiten(XSpreadsheet sheet) throws GenerateException {
		int[][] breiten = {
				{ TEAM_NR_SPALTE, COL_WIDTH_NR },
				{ TEAM_NAME_SPALTE, COL_WIDTH_NAME },
				{ RANGLISTE_SPALTE, COL_WIDTH_RANGLISTE },
				{ PUNKTE_PLUS_SPALTE, COL_WIDTH_DATA },
				{ PUNKTE_MINUS_SPALTE, COL_WIDTH_DATA },
				{ SPIELE_PLUS_SPALTE, COL_WIDTH_DATA },
				{ SPIELE_MINUS_SPALTE, COL_WIDTH_DATA },
				{ SPIELE_DIFF_SPALTE, COL_WIDTH_DATA },
				{ SP_PUNKTE_PLUS_SPALTE, COL_WIDTH_DATA },
				{ SP_PUNKTE_MINUS_SPALTE, COL_WIDTH_DATA },
				{ SP_PUNKTE_DIFF_SPALTE, COL_WIDTH_DATA },
				{ BEGEGNUNGEN_SPALTE, COL_WIDTH_DATA },
		};
		for (int[] bw : breiten) {
			getSheetHelper().setColumnProperties(sheet, bw[0],
					ColumnProperties.from().setWidth(bw[1]).setHoriJustify(CellHoriJustify.CENTER));
		}
	}

	private void insertHeader(XSpreadsheet sheet) throws GenerateException {
		int headerFarbe = konfigurationSheet.getRanglisteHeaderFarbe();
		TableBorder2 borderBottom = BorderFactory.from().allThin().boldLn().forBottom().toBorder();

		// Zeile 0: Nr, Name – je 2 Zeilen hoch
		for (int col : new int[] { TEAM_NR_SPALTE, TEAM_NAME_SPALTE }) {
			getSheetHelper().setStringValueInCell(StringCellValue
					.from(sheet, Position.from(col, 0),
							col == TEAM_NR_SPALTE ? I18n.get("column.header.nr") : I18n.get("column.header.name"))
					.setCellProperties(CellProperties.from()
							.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
							.setCellBackColor(headerFarbe).margin(MARGIN).centerJustify().setShrinkToFit(true))
					.setEndPosMergeZeilePlus(1));
		}

		// Zeile 0: Rang – 2 Zeilen hoch, 90°
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(RANGLISTE_SPALTE, 0), I18n.get("column.header.platz"))
				.setCellProperties(CellProperties.from().setAllThinBorder()
						.setCellBackColor(headerFarbe).margin(MARGIN).centerJustify().setShrinkToFit(true)
						.setBorder(BorderFactory.from().allThin().boldLn().forBottom().forRight().toBorder()))
				.setEndPosMergeZeilePlus(1).setRotate90());

		// Zeile 0: Punkte-Gruppe (2 Spalten breit)
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(PUNKTE_PLUS_SPALTE, 0), I18n.get("column.header.punkte"))
				.setCellProperties(CellProperties.from().setAllThinBorder()
						.setCellBackColor(headerFarbe).margin(MARGIN).centerJustify().setShrinkToFit(true))
				.setEndPosMergeSpaltePlus(1));

		// Zeile 0: Spiele-Gruppe (3 Spalten breit)
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(SPIELE_PLUS_SPALTE, 0), I18n.get("column.header.spiele"))
				.setCellProperties(CellProperties.from().setAllThinBorder()
						.setCellBackColor(headerFarbe).margin(MARGIN).centerJustify().setShrinkToFit(true))
				.setEndPosMergeSpaltePlus(2));

		// Zeile 0: Spielpunkte-Gruppe (3 Spalten breit)
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(SP_PUNKTE_PLUS_SPALTE, 0), I18n.get("column.header.spielpunkte"))
				.setCellProperties(CellProperties.from().setAllThinBorder()
						.setCellBackColor(headerFarbe).margin(MARGIN).centerJustify().setShrinkToFit(true))
				.setEndPosMergeSpaltePlus(2));

		// Zeile 0: Begegnungen – 2 Zeilen hoch, 90°
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(BEGEGNUNGEN_SPALTE, 0), I18n.get("column.header.begegn"))
				.setCellProperties(CellProperties.from().setAllThinBorder()
						.setCellBackColor(headerFarbe).margin(MARGIN).centerJustify().setShrinkToFit(true))
				.setEndPosMergeZeilePlus(1).setRotate90());

		// Zeile 1: Sub-Header +/-/Δ
		RangeData subHeader = new RangeData();
		RowData subRow = subHeader.addNewRow();
		subRow.newString("+");
		subRow.newString("-");
		subRow.newString("+");
		subRow.newString("-");
		subRow.newString("Δ");
		subRow.newString("+");
		subRow.newString("-");
		subRow.newString("Δ");
		RangeHelper.from(this, subHeader.getRangePosition(Position.from(PUNKTE_PLUS_SPALTE, 1)))
				.setDataInRange(subHeader)
				.setRangeProperties(RangeProperties.from().centerJustify()
						.setBorder(borderBottom).setCellBackColor(headerFarbe).margin(MARGIN).setShrinkToFit(true));
	}

	private void printBereichDefinieren() throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(printBereichRangePosition());
	}

	public RangePosition printBereichRangePosition() throws GenerateException {
		return RangePosition.from(0, 0, getLetzteSpalte(), getFooterZeile());
	}

	public StringCellValue addFooter() throws GenerateException {
		processBoxinfo("processbox.fusszeile.einfuegen");
		int ersteFooterZeile = getFooterZeile();
		StringCellValue stringVal = StringCellValue.from(this, Position.from(TEAM_NR_SPALTE, ersteFooterZeile))
				.setHoriJustify(CellHoriJustify.LEFT).setCharHeight(8)
				.setEndPosMergeSpalte(getLetzteSpalte());
		getSheetHelper().setStringValueInCell(stringVal.setValue(
				I18n.get("liga.rangliste.reihenfolge.platzierung")));
		return stringVal;
	}

	private int getFooterZeile() throws GenerateException {
		return getLetzteMitDatenZeileInSpielerNrSpalte() + 2;
	}

	// ─── IRangliste ──────────────────────────────────────────────────────────

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		return PUNKTE_PLUS_SPALTE;
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		return BEGEGNUNGEN_SPALTE;
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return sucheLetzteZeileMitSpielerNummer();
	}

	@Override
	public int getErsteDatenZiele() throws GenerateException {
		return ERSTE_DATEN_ZEILE;
	}

	@Override
	public int getManuellSortSpalte() throws GenerateException {
		return getLetzteSpalte() + 3;
	}

	@Override
	public List<Position> getRanglisteSpalten() throws GenerateException {
		return List.of(
				Position.from(PUNKTE_PLUS_SPALTE, ERSTE_DATEN_ZEILE),
				Position.from(SPIELE_PLUS_SPALTE, ERSTE_DATEN_ZEILE),
				Position.from(SP_PUNKTE_PLUS_SPALTE, ERSTE_DATEN_ZEILE),
				Position.from(SP_PUNKTE_DIFF_SPALTE, ERSTE_DATEN_ZEILE));
	}

	@Override
	public int validateSpalte() throws GenerateException {
		return getManuellSortSpalte() + getRanglisteSpalten().size() + 1;
	}

	@Override
	public int getErsteSpalte() throws GenerateException {
		return TEAM_NR_SPALTE;
	}

	@Override
	public void calculateAll() {
		// via calculateAll() am Ende von berechnungUndSchreiben() erledigt
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
		var searchProp = new HashMap<String, Object>();
		searchProp.put(RangeSearchHelper.SEARCH_BACKWARDS, true);
		Position result = RangeSearchHelper.from(this,
				RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE + 999))
				.searchNachRegExprInSpalte("^\\d", searchProp);
		return result != null ? result.getZeile() : ERSTE_DATEN_ZEILE;
	}

}
