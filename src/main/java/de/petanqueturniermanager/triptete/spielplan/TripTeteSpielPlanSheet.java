package de.petanqueturniermanager.triptete.spielplan;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.triptete.TripTetePaarungen;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
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
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;

/**
 * Trip-Tête-Spielplan: Round-Robin-Paarungen, pro Begegnung drei Partien
 * (Triplette / Doublette / Tête-à-tête) in einer Zeile.
 */
public class TripTeteSpielPlanSheet extends SheetRunner implements ISheet {

	public static String sheetName() {
		return SheetNamen.spielplan();
	}

	private static final String METADATA_SCHLUESSEL = SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN;
	public static final String LEGACY_SHEET_NAMEN = SheetNamen.LEGACY_SPIELPLAN;

	private static final int ERSTE_HEADER_ZEILE = 0;
	public static final int ERSTE_DATEN_ZEILE = ERSTE_HEADER_ZEILE + 2;

	public static final int SPIEL_NR_SPALTE     = 0;
	public static final int BAHN_TRI_SPALTE     = 1;
	public static final int BAHN_DOU_SPALTE     = 2;
	public static final int BAHN_TETE_SPALTE    = 3;
	public static final int NAME_A_SPALTE       = 4;
	public static final int NAME_B_SPALTE       = 5;
	public static final int TRI_A_SPALTE        = 6;
	public static final int TRI_B_SPALTE        = 7;
	public static final int DOU_A_SPALTE        = 8;
	public static final int DOU_B_SPALTE        = 9;
	public static final int TETE_A_SPALTE       = 10;
	public static final int TETE_B_SPALTE       = 11;
	public static final int PUNKTE_A            = 12;
	public static final int PUNKTE_B            = 13;
	public static final int SIEGE_A             = 14;
	public static final int SIEGE_B             = 15;
	public static final int SP_PUNKTE_A         = 16;
	public static final int SP_PUNKTE_B         = 17;

	public static final int TEAM_A_NR_SPALTE    = 19;
	public static final int TEAM_B_NR_SPALTE    = TEAM_A_NR_SPALTE + 1;

	private static final int PUNKTE_NR_WIDTH = AbstractSuperMeleeRanglisteFormatter.ENDSUMME_NUMBER_WIDTH;

	private final TripTeteKonfigurationSheet konfigurationSheet;
	private final TripTeteMeldeListeSheetUpdate meldeListe;

	public TripTeteSpielPlanSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.TRIPTETE);
		konfigurationSheet = new TripTeteKonfigurationSheet(workingSpreadsheet);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
	}

	@Override
	protected TripTeteKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@VisibleForTesting
	TripTeteMeldeListeSheetUpdate initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new TripTeteMeldeListeSheetUpdate(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), METADATA_SCHLUESSEL, LEGACY_SHEET_NAMEN);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	public TripTeteMeldeListeSheetUpdate getMeldeListe() {
		return meldeListe;
	}

	@Override
	protected void doRun() throws GenerateException {
		meldeListe.upDateSheet();
		generate(meldeListe.getAlleMeldungen());
	}

	public void generate(TeamMeldungen meldungen) throws GenerateException {
		if (!meldungen.isValid()) {
			processBoxinfo("processbox.abbruch");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.triptete.spielplan"))
					.message(I18n.get("msg.text.ungueltige.anzahl.meldungen")).show();
			return;
		}

		if (!NewSheet.from(this, sheetName(), METADATA_SCHLUESSEL)
				.pos(DefaultSheetPos.TRIPTETE_WORK).setForceCreate(true).setActiv().hideGrid()
				.tabColor(konfigurationSheet.getSpielrundeTabFarbe()).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Trip-Tête-Spielplan wurde nicht erstellt");
			return;
		}

		List<List<TeamPaarung>> spielPlan = TripTetePaarungen.jederGegenJeden(meldungen);

		getxCalculatable().enableAutomaticCalculation(false);
		try {
			insertDatenHeaderUndSpalteBreite();
			insertArbeitsspalten(spielPlan);
			insertSpielNrSpalte(spielPlan);
			insertBahnenSpalten(spielPlan);
			insertFormulaTeamNamen();
			insertFormulaWertSpalten();
			formatieren(spielPlan);
			printBereichDefinieren();
			SheetFreeze.from(getTurnierSheet()).anzZeilen(2).doFreeze();
		} finally {
			getxCalculatable().enableAutomaticCalculation(true);
		}
	}

	private void printBereichDefinieren() throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(printBereichRangePosition());
	}

	public RangePosition printBereichRangePosition() throws GenerateException {
		List<List<TeamPaarung>> spielPlan = TripTetePaarungen.jederGegenJeden(meldeListe.getAlleMeldungen());
		int anzZeilen = spielPlan.size() * spielPlan.get(0).size() - 1;
		Position rechtsUnten = Position.from(SP_PUNKTE_B, ERSTE_DATEN_ZEILE + anzZeilen);
		return RangePosition.from(Position.from(0, 0), rechtsUnten);
	}

	private void insertDatenHeaderUndSpalteBreite() throws GenerateException {
		Position headerPos = Position.from(SPIEL_NR_SPALTE, ERSTE_HEADER_ZEILE);
		ColumnProperties colPropSchmal = ColumnProperties.from().setWidth(1100).centerJustify().setShrinkToFit(true);
		StringCellValue stValHeader = StringCellValue.from(getXSpreadSheet(), headerPos)
				.setColumnProperties(colPropSchmal);

		// Nr. – überspannt beide Headerzeilen
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("column.header.nr")).setEndPosMergeZeilePlus(1));

		// Bahn-Gruppe: „Bahn" in Zeile 0 über 3 Spalten; Zeile 1 erhält Kürzel Tri/Dou/Tête
		colPropSchmal.setWidth(900);
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("column.header.bahn")).spalte(BAHN_TRI_SPALTE)
						.setEndPosMergeSpaltePlus(2));

		// Team A / Team B – Zeile 0: „Team", Zeile 1: „A" / „B" (kein Zeilenmerge)
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("column.header.team")).spalte(NAME_A_SPALTE)
						.setEndPosMerge(null));
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("column.header.team")).spalte(NAME_B_SPALTE));

		// Partie-Gruppen-Header (Zeile 0, gemerged über je 2 Spalten)
		ColumnProperties colPropPunkt = ColumnProperties.from().setWidth(PUNKTE_NR_WIDTH).centerJustify();
		stValHeader.setColumnProperties(colPropPunkt);
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("enum.formation.triplette")).spalte(TRI_A_SPALTE)
						.setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("enum.formation.doublette")).spalte(DOU_A_SPALTE)
						.setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("enum.formation.tete")).spalte(TETE_A_SPALTE)
						.setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("column.header.punkte")).spalte(PUNKTE_A)
						.setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("column.header.siege")).spalte(SIEGE_A)
						.setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("column.header.sp.punkte")).spalte(SP_PUNKTE_A)
						.setEndPosMergeSpaltePlus(1));

		// Name-Spaltenbreite
		ColumnProperties colPropName = ColumnProperties.from().setWidth(6000);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), NAME_A_SPALTE, colPropName);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), NAME_B_SPALTE, colPropName);

		// Zeile 1: Bahn-Kürzel + H/G-Unterheader
		stValHeader.setEndPosMerge(null).zeilePlusEins();
		stValHeader.setColumnProperties(ColumnProperties.from().setWidth(900).centerJustify().setShrinkToFit(true));
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("spielplan.bahn.triplette")).spalte(BAHN_TRI_SPALTE));
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("spielplan.bahn.doublette")).spalte(BAHN_DOU_SPALTE));
		getSheetHelper().setStringValueInCell(
				stValHeader.setValue(I18n.get("spielplan.bahn.tete")).spalte(BAHN_TETE_SPALTE));
		stValHeader.setColumnProperties(ColumnProperties.from().setWidth(PUNKTE_NR_WIDTH).centerJustify());
		String heim = "A";
		String gast = "B";
		getSheetHelper().setStringValueInCell(stValHeader.setValue(heim).spalte(TRI_A_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(gast).spalte(TRI_B_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(heim).spalte(DOU_A_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(gast).spalte(DOU_B_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(heim).spalte(TETE_A_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(gast).spalte(TETE_B_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(heim).spalte(PUNKTE_A));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(gast).spalte(PUNKTE_B));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(heim).spalte(SIEGE_A));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(gast).spalte(SIEGE_B));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(heim).spalte(SP_PUNKTE_A));
		getSheetHelper().setStringValueInCell(stValHeader.setValue(gast).spalte(SP_PUNKTE_B));

		// Name-Spalten Zeile 1: A / B (ohne ColumnProperties, Breite bleibt bei 6000)
		StringCellValue nameZeile1 = StringCellValue.from(getXSpreadSheet(),
				Position.from(NAME_A_SPALTE, ERSTE_HEADER_ZEILE + 1)).setValue(heim);
		getSheetHelper().setStringValueInCell(nameZeile1);
		getSheetHelper().setStringValueInCell(nameZeile1.setValue(gast).spalte(NAME_B_SPALTE));
	}

	private void insertSpielNrSpalte(List<List<TeamPaarung>> spielPlan) throws GenerateException {
		RangeData rangeData = new RangeData();
		int nr = 1;
		for (List<TeamPaarung> runde : spielPlan) {
			for (TeamPaarung ignored : runde) {
				RowData row = rangeData.addNewRow();
				row.newInt(nr++);
			}
		}
		Position startPos = Position.from(SPIEL_NR_SPALTE, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	private void insertBahnenSpalten(List<List<TeamPaarung>> spielPlan) throws GenerateException {
		// Pro Runde werden 2 physische Bahnen pro Begegnung vergeben:
		//   Bahn-Tri (ungerade): Triplette läuft hier, danach Doublette (sequenziell)
		//   Bahn-Dou (ungerade): dieselbe Bahn wie Tri – Doublette folgt Triplette
		//   Bahn-Tête (gerade):  parallel zur Doublette, separate Bahn
		RangeData rangeData = new RangeData();
		for (List<TeamPaarung> runde : spielPlan) {
			int bahnInRunde = 1;
			for (TeamPaarung ignored : runde) {
				int bahnTri = 2 * bahnInRunde - 1;
				int bahnTete = 2 * bahnInRunde;
				RowData row = rangeData.addNewRow();
				row.newInt(bahnTri);
				row.newInt(bahnTri);  // Doublette auf gleicher Bahn wie Triplette (sequenziell)
				row.newInt(bahnTete); // Tête auf separater Parallelbahn
				bahnInRunde++;
			}
		}
		Position startPos = Position.from(BAHN_TRI_SPALTE, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	private void insertArbeitsspalten(List<List<TeamPaarung>> spielPlan) throws GenerateException {
		RangeData rangeData = new RangeData();
		for (List<TeamPaarung> runde : spielPlan) {
			for (TeamPaarung tp : runde) {
				SheetRunner.testDoCancelTask();
				RowData row = rangeData.addNewRow();
				row.newInt(tp.getA().getNr());
				row.newInt(tp.getOptionalB().isPresent() ? tp.getB().getNr() : 0);
			}
		}
		Position startPos = Position.from(TEAM_A_NR_SPALTE, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData)
				.setRangeProperties(RangeProperties.from().centerJustify()
						.setBorder(BorderFactory.from().allThin().toBorder()));

		ColumnProperties hidden = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH)
				.isVisible(false);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), TEAM_A_NR_SPALTE, hidden);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), TEAM_B_NR_SPALTE, hidden);
	}

	private void insertFormulaTeamNamen() throws GenerateException {
		int letzteSpielZeile = letzteSpielZeile();

		String formelA = freispielName(meldeListe.formulaSverweisSpielernamen(
				Position.from(TEAM_A_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddress()));
		StringCellValue nameAFormel = StringCellValue.from(getXSpreadSheet())
				.setValue(formelA)
				.setPos(Position.from(NAME_A_SPALTE, ERSTE_DATEN_ZEILE))
				.setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(nameAFormel);

		String formelB = freispielName(meldeListe.formulaSverweisSpielernamen(
				Position.from(TEAM_B_NR_SPALTE, ERSTE_DATEN_ZEILE).getAddress()));
		StringCellValue nameBFormel = StringCellValue.from(getXSpreadSheet())
				.setValue(formelB)
				.setPos(Position.from(NAME_B_SPALTE, ERSTE_DATEN_ZEILE))
				.setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(nameBFormel);
	}

	private String freispielName(String formulaName) {
		return "WENNNV(" + formulaName + ";\"" + I18n.get("spielplan.freispiel.name") + "\")";
	}

	private void insertFormulaWertSpalten() throws GenerateException {
		int letzteSpielZeile = letzteSpielZeile();

		// Siege: Σ Partie-Siege (muss vor Punkte berechnet werden, da Punkte darauf verweist)
		StringCellValue siegeA = StringCellValue.from(getXSpreadSheet()).setValue(siegeFormel(true))
				.setPos(Position.from(SIEGE_A, ERSTE_DATEN_ZEILE)).setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(siegeA);
		StringCellValue siegeB = StringCellValue.from(getXSpreadSheet()).setValue(siegeFormel(false))
				.setPos(Position.from(SIEGE_B, ERSTE_DATEN_ZEILE)).setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(siegeB);

		// Punkte: Begegnungssieg = 1 wenn Siege >= 2
		Position siegeAPos = Position.from(SIEGE_A, ERSTE_DATEN_ZEILE);
		Position siegeBPos = Position.from(SIEGE_B, ERSTE_DATEN_ZEILE);
		StringCellValue punkteA = StringCellValue.from(getXSpreadSheet())
				.setValue("WENN(" + siegeAPos.getAddress() + ">=2;1;0)")
				.setPos(Position.from(PUNKTE_A, ERSTE_DATEN_ZEILE)).setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(punkteA);
		StringCellValue punkteB = StringCellValue.from(getXSpreadSheet())
				.setValue("WENN(" + siegeBPos.getAddress() + ">=2;1;0)")
				.setPos(Position.from(PUNKTE_B, ERSTE_DATEN_ZEILE)).setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(punkteB);

		// spPunkte: Σ Spielpunkte (Triplette + Doublette + Tête)
		StringCellValue spA = StringCellValue.from(getXSpreadSheet()).setValue(spPunkteFormel(true))
				.setPos(Position.from(SP_PUNKTE_A, ERSTE_DATEN_ZEILE)).setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(spA);
		StringCellValue spB = StringCellValue.from(getXSpreadSheet()).setValue(spPunkteFormel(false))
				.setPos(Position.from(SP_PUNKTE_B, ERSTE_DATEN_ZEILE)).setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(spB);
	}

	/** Σ Partie-Siege: für jede der drei Partien 1 wenn eigene > gegnerische Spielpunkte, sonst 0. */
	private String siegeFormel(boolean fuerA) {
		Position triA = Position.from(TRI_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position triB = Position.from(TRI_B_SPALTE, ERSTE_DATEN_ZEILE);
		Position douA = Position.from(DOU_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position douB = Position.from(DOU_B_SPALTE, ERSTE_DATEN_ZEILE);
		Position teteA = Position.from(TETE_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position teteB = Position.from(TETE_B_SPALTE, ERSTE_DATEN_ZEILE);
		String aAdr = fuerA ? triA.getAddress() : triB.getAddress();
		String bAdr = fuerA ? triB.getAddress() : triA.getAddress();
		String aDouAdr = fuerA ? douA.getAddress() : douB.getAddress();
		String bDouAdr = fuerA ? douB.getAddress() : douA.getAddress();
		String aTeteAdr = fuerA ? teteA.getAddress() : teteB.getAddress();
		String bTeteAdr = fuerA ? teteB.getAddress() : teteA.getAddress();
		return "WENN(" + aAdr + ">" + bAdr + ";1;0)"
				+ "+WENN(" + aDouAdr + ">" + bDouAdr + ";1;0)"
				+ "+WENN(" + aTeteAdr + ">" + bTeteAdr + ";1;0)";
	}

	/** Σ Spielpunkte: Summe aller drei Partie-Spielpunkte für ein Team. */
	private String spPunkteFormel(boolean fuerA) {
		Position triA = Position.from(TRI_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position triB = Position.from(TRI_B_SPALTE, ERSTE_DATEN_ZEILE);
		Position douA = Position.from(DOU_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position douB = Position.from(DOU_B_SPALTE, ERSTE_DATEN_ZEILE);
		Position teteA = Position.from(TETE_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position teteB = Position.from(TETE_B_SPALTE, ERSTE_DATEN_ZEILE);
		if (fuerA) {
			return triA.getAddress() + "+" + douA.getAddress() + "+" + teteA.getAddress();
		}
		return triB.getAddress() + "+" + douB.getAddress() + "+" + teteB.getAddress();
	}

	private void formatieren(List<List<TeamPaarung>> spielPlan) throws GenerateException {
		int letzteSpielZeile = letzteSpielZeile();
		RangePosition allDataMitHeader = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_HEADER_ZEILE,
				SP_PUNKTE_B, letzteSpielZeile);
		RangeProperties rangeProp = RangeProperties.from().setBorder(BorderFactory.from().allThin().toBorder())
				.centerJustify().setShrinkToFit(true).topMargin(110).bottomMargin(110).setCharHeight(10);
		RangeHelper.from(this, allDataMitHeader).setRangeProperties(rangeProp);

		// gerade/ungerade
		RangePosition runden = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_DATEN_ZEILE, SP_PUNKTE_B, letzteSpielZeile);
		Integer farbeGerade = getKonfigurationSheet().getSpielPlanHintergrundFarbeGerade();
		Integer farbeUngerade = getKonfigurationSheet().getSpielPlanHintergrundFarbeUnGerade();
		RanglisteGeradeUngeradeFormatHelper.from(this, runden).geradeFarbe(farbeGerade)
				.ungeradeFarbe(farbeUngerade).apply();

		// Editierbare Zellen: Bahnen + Ergebnisse aller drei Partien
		EditierbaresZelleFormatHelper.anwenden(this, RangePosition.from(
				BAHN_TRI_SPALTE, ERSTE_DATEN_ZEILE, BAHN_TETE_SPALTE, letzteSpielZeile));
		EditierbaresZelleFormatHelper.anwenden(this, RangePosition.from(
				TRI_A_SPALTE, ERSTE_DATEN_ZEILE, TETE_B_SPALTE, letzteSpielZeile));

		// Trenner: zwischen Runden (= je anzPaarungen Zeilen) ein dicker Strich
		RangeProperties trenner = RangeProperties.from()
				.setBorder(BorderFactory.from().doubleLn().forBottom().toBorder());
		int anzRunden = spielPlan.size();
		int anzPaarungen = spielPlan.get(0).size();
		Position trennerPos = Position.from(SPIEL_NR_SPALTE, ERSTE_DATEN_ZEILE);
		for (int i = 1; i < anzRunden; i++) {
			trennerPos.zeilePlus(anzPaarungen - 1);
			RangePosition trennerRange = RangePosition.from(SPIEL_NR_SPALTE, trennerPos.getZeile(),
					SP_PUNKTE_B, trennerPos.getZeile());
			RangeHelper.from(this, trennerRange).setRangeProperties(trenner);
			trennerPos.zeilePlusEins();
		}

		// Vertikale Trenner
		RangeProperties vTrennerBold = RangeProperties.from()
				.setBorder(BorderFactory.from().boldLn().forRight().toBorder());
		RangePosition vRange = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_HEADER_ZEILE, SPIEL_NR_SPALTE, letzteSpielZeile);
		RangeHelper.from(this, vRange.spalte(BAHN_TETE_SPALTE)).setRangeProperties(vTrennerBold);
		RangeHelper.from(this, vRange.spalte(NAME_B_SPALTE)).setRangeProperties(vTrennerBold);
		RangeHelper.from(this, vRange.spalte(TRI_B_SPALTE)).setRangeProperties(vTrennerBold);
		RangeHelper.from(this, vRange.spalte(DOU_B_SPALTE)).setRangeProperties(vTrennerBold);
		RangeHelper.from(this, vRange.spalte(TETE_B_SPALTE)).setRangeProperties(vTrennerBold);
		RangeHelper.from(this, vRange.spalte(PUNKTE_B)).setRangeProperties(vTrennerBold);
		RangeHelper.from(this, vRange.spalte(SIEGE_B)).setRangeProperties(vTrennerBold);

		// Header-Farbe
		RangePosition headerRange = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_HEADER_ZEILE, SP_PUNKTE_B,
				ERSTE_HEADER_ZEILE + 1);
		RangeHelper.from(this, headerRange).setRangeProperties(
				RangeProperties.from().setCellBackColor(getKonfigurationSheet().getSpielPlanHeaderFarbe()));
	}

	private int letzteSpielZeile() throws GenerateException {
		return RangeSearchHelper.from(this, RangePosition.from(SPIEL_NR_SPALTE, 0, SPIEL_NR_SPALTE, 999))
				.searchLastNotEmptyInSpalte().getZeile();
	}
}
