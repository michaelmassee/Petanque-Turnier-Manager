/**
 * Erstellung 29.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.spielplan;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.GeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SearchHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_Update;
import de.petanqueturniermanager.model.LigaSpielPlan;
import de.petanqueturniermanager.model.SpielErgebnis;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.supermelee.AbstractSuperMeleeRanglisteFormatter;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundePlan;

/**
 * @author Michael Massee
 *
 */
public class LigaSpielPlanSheet extends LigaSheet implements ISheet {

	private static final Logger LOGGER = LogManager.getLogger(SpielrundePlan.class);
	private static final String SHEET_COLOR = "b0f442";
	public static final String SHEET_NAMEN = "Liga Spielplan";

	private static final int ERSTE_SPIELTAG_HEADER_ZEILE = 0; // Zeile 0
	public static final int ERSTE_SPIELTAG_DATEN_ZEILE = ERSTE_SPIELTAG_HEADER_ZEILE + 2; // Zeile 2
	private static final int SPIEL_NR_SPALTE = 0; // Spalte A
	private static final int KW_SPALTE = SPIEL_NR_SPALTE + 1;
	private static final int DATUM_SPALTE = KW_SPALTE + 1;
	private static final int NAME_A_SPALTE = DATUM_SPALTE + 1;
	private static final int NAME_B_SPALTE = NAME_A_SPALTE + 1;
	public static final int PUNKTE_A_SPALTE = NAME_B_SPALTE + 1;
	public static final int PUNKTE_B_SPALTE = PUNKTE_A_SPALTE + 1;
	public static final int SPIELE_A_SPALTE = PUNKTE_B_SPALTE + 1;
	public static final int SPIELE_B_SPALTE = SPIELE_A_SPALTE + 1;
	public static final int SPIELPNKT_A_SPALTE = SPIELE_B_SPALTE + 1;
	public static final int SPIELPNKT_B_SPALTE = SPIELPNKT_A_SPALTE + 1;

	private static final int PUNKTE_NR_WIDTH = AbstractSuperMeleeRanglisteFormatter.ENDSUMME_NUMBER_WIDTH;

	private static final String NR_HINRUNDE_PREFIX = "HR-";
	private static final String NR_RUECKRUNDE_PREFIX = "RR-";

	// Arbeitsspalten
	public static final int TEAM_A_NR_SPALTE = 14; // Zeile 0
	public static final int TEAM_B_NR_SPALTE = TEAM_A_NR_SPALTE + 1; // Zeile 0

	private final LigaMeldeListeSheet_Update meldeListe;

	/**
	 * @param workingSpreadsheet
	 */
	public LigaSpielPlanSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
	}

	@VisibleForTesting
	LigaMeldeListeSheet_Update initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new LigaMeldeListeSheet_Update(workingSpreadsheet);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEET_NAMEN);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public Logger getLogger() {
		return LOGGER;
	}

	@Override
	protected void doRun() throws GenerateException {
		meldeListe.upDateSheet();
		generate(meldeListe.getAlleMeldungen());
	}

	public void generate(TeamMeldungen meldungen) throws GenerateException {
		if (!meldungen.isValid()) {
			processBoxinfo("Abbruch, ungültige anzahl von Melungen.");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Neue Liga-SpielPlan").message("Ungültige anzahl von Melungen").show();
			return;
		}

		if (!NewSheet.from(getWorkingSpreadsheet(), SHEET_NAMEN).pos(DefaultSheetPos.LIGA_WORK).setForceCreate(true).setActiv().hideGrid().tabColor(SHEET_COLOR).create()
				.isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Liga SpielPlan wurde nicht erstellt");
			return;
		}

		LigaSpielPlan ligaSpielPlan = new LigaSpielPlan(meldungen);
		// nur einmal schufflePlan ! damit beim wechsel von hr nach rr keine 2 x hintereinander
		List<List<TeamPaarung>> spielPlanHRunde = ligaSpielPlan.schufflePlan().getSpielPlanClone();
		List<List<TeamPaarung>> spielPlanRRunde = ligaSpielPlan.flipTeams().getSpielPlanClone();

		insertDatenHeaderUndSpalteBreite();
		insertArbeitsspalten(spielPlanHRunde, spielPlanRRunde);
		insertSpieltageDaten(spielPlanHRunde);
		insertFormulaTeamNamen();
		insertFormulaPunkte();
		formatieren(spielPlanHRunde);
	}

	private void formatieren(List<List<TeamPaarung>> spielPlanHRunde) throws GenerateException {
		// erstmal ein Grid
		int letzteSpielZeile = letzteSpielZeile();
		RangePosition allDataMitHeader = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE, SPIELPNKT_B_SPALTE, letzteSpielZeile);
		RangeProperties rangeProp = RangeProperties.from().setBorder(BorderFactory.from().allThin().toBorder()).centerJustify().setShrinkToFit(true).topMargin(110)
				.bottomMargin(110).setCharHeight(12);
		RangeHelper.from(getXSpreadSheet(), allDataMitHeader).setRangeProperties(rangeProp);

		// gerade ungerade
		RangePosition runden = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE, SPIELPNKT_B_SPALTE, letzteSpielZeile);
		Integer spielPlanHintergrundFarbeGerade = getKonfigurationSheet().getSpielPlanHintergrundFarbeGerade();
		Integer spielPlanHintergrundFarbeUnGerade = getKonfigurationSheet().getSpielPlanHintergrundFarbeUnGerade();
		GeradeUngeradeFormatHelper.from(this, runden).geradeFarbe(spielPlanHintergrundFarbeGerade).ungeradeFarbe(spielPlanHintergrundFarbeUnGerade).apply();

		RangeProperties horTrennerDouble = RangeProperties.from().setBorder(BorderFactory.from().doubleLn().forBottom().toBorder());
		RangeProperties horTrennerBoldBottom = RangeProperties.from().setBorder(BorderFactory.from().boldLn().forBottom().toBorder());
		RangeProperties horTrennerBoldTop = RangeProperties.from().setBorder(BorderFactory.from().boldLn().forTop().toBorder());

		// Header
		RangePosition headerHorTrenner = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE + 2, SPIELPNKT_B_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE + 2);
		RangeHelper.from(getXSpreadSheet(), headerHorTrenner).setRangeProperties(horTrennerBoldTop);

		// Spieltag/Runde Trenner
		RangePosition trennerPos = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE, SPIELPNKT_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE);
		int anzRunden = spielPlanHRunde.size();
		int anzPaarungen = spielPlanHRunde.get(0).size();

		// Hinrunde
		for (int i = 1; i < anzRunden; i++) {
			trennerPos.zeilePlus(anzPaarungen - 1);
			RangeHelper.from(getXSpreadSheet(), trennerPos).setRangeProperties(horTrennerDouble);
			trennerPos.zeilePlusEins();
		}
		trennerPos.zeilePlus(anzPaarungen - 1);
		RangeHelper.from(getXSpreadSheet(), trennerPos).setRangeProperties(horTrennerBoldBottom);
		trennerPos.zeilePlusEins();
		// Rueckrunde
		for (int i = 1; i < anzRunden; i++) {
			trennerPos.zeilePlus(anzPaarungen - 1);
			RangeHelper.from(getXSpreadSheet(), trennerPos).setRangeProperties(horTrennerDouble);
			trennerPos.zeilePlusEins();
		}

		// Vertikal
		RangeProperties vertTrennerBoldRight = RangeProperties.from().setBorder(BorderFactory.from().boldLn().forRight().toBorder());
		RangeProperties vertTrennerBoldLeft = RangeProperties.from().setBorder(BorderFactory.from().boldLn().forLeft().toBorder());
		RangeProperties vertTrennerDoubleLeft = RangeProperties.from().setBorder(BorderFactory.from().doubleLn().forLeft().toBorder());

		RangePosition vertikal = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE, SPIEL_NR_SPALTE, letzteSpielZeile);
		RangeHelper.from(getXSpreadSheet(), vertikal.spalte(SPIEL_NR_SPALTE)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(getXSpreadSheet(), vertikal.spalte(DATUM_SPALTE)).setRangeProperties(vertTrennerBoldRight);
		RangeHelper.from(getXSpreadSheet(), vertikal.spalte(NAME_B_SPALTE)).setRangeProperties(vertTrennerDoubleLeft);
		RangeHelper.from(getXSpreadSheet(), vertikal.spalte(PUNKTE_A_SPALTE)).setRangeProperties(vertTrennerBoldLeft);
		RangeHelper.from(getXSpreadSheet(), vertikal.spalte(SPIELPNKT_B_SPALTE + 1)).setRangeProperties(vertTrennerBoldLeft);

		// header Farbe
		RangePosition headerRange = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE, SPIELPNKT_B_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE + 1);
		RangeHelper.from(getXSpreadSheet(), headerRange).setRangeProperties(RangeProperties.from().setCellBackColor(getKonfigurationSheet().getSpielPlanHeaderFarbe()));

	}

	private void insertDatenHeaderUndSpalteBreite() throws GenerateException {

		// Header zusammen bauen
		// -----------------------------------------------------------------

		Position headerPos = Position.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE);
		StringCellValue stValHeader = StringCellValue.from(getXSpreadSheet(), headerPos).setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
				.setShrinkToFit(true);
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Nr.").setEndPosMergeZeilePlus(1));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("KW").spalte(KW_SPALTE).setEndPosMergeZeilePlus(1));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Datum").spalte(DATUM_SPALTE).setEndPosMergeZeilePlus(1));

		// header erste Zeile
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Name").spalte(NAME_A_SPALTE).setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Punkte").spalte(PUNKTE_A_SPALTE).setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Spiele").spalte(SPIELE_A_SPALTE).setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("SpPunkte").spalte(SPIELPNKT_A_SPALTE).setEndPosMergeSpaltePlus(1));

		// header zweite Zeile
		ColumnProperties colProp = ColumnProperties.from().setWidth(LIGA_MELDUNG_NAME_WIDTH);
		stValHeader.setEndPosMerge(null).zeilePlusEins().setColumnProperties(colProp);
		// name
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Heim").spalte(NAME_A_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Gast").spalte(NAME_B_SPALTE));

		// Punkte
		stValHeader.getColumnProperties().setWidth(PUNKTE_NR_WIDTH);

		getSheetHelper().setStringValueInCell(stValHeader.setValue("H").spalte(PUNKTE_A_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("G").spalte(PUNKTE_B_SPALTE));

		getSheetHelper().setStringValueInCell(stValHeader.setValue("H").spalte(SPIELE_A_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("G").spalte(SPIELE_B_SPALTE));

		getSheetHelper().setStringValueInCell(stValHeader.setValue("H").spalte(SPIELPNKT_A_SPALTE));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("G").spalte(SPIELPNKT_B_SPALTE));
	}

	private void insertArbeitsspalten(List<List<TeamPaarung>> spielPlanHRunde, List<List<TeamPaarung>> spielPlanRRunde) throws GenerateException {
		RangeData rangeData = new RangeData();

		List<List<TeamPaarung>> alleSpieltage = new ArrayList<>();
		alleSpieltage.addAll(spielPlanHRunde);
		alleSpieltage.addAll(spielPlanRRunde);

		// hin und rr runde
		for (List<TeamPaarung> spielTag : alleSpieltage) {
			for (TeamPaarung teamPaarung : spielTag) {
				SheetRunner.testDoCancelTask();
				RowData teamPaarungData = rangeData.newRow();
				teamPaarungData.newInt(teamPaarung.getA().getNr());
				teamPaarungData.newInt(teamPaarung.getOptionalB().isPresent() ? teamPaarung.getB().getNr() : 0);
			}
		}
		Position startPos = Position.from(TEAM_A_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE);
		RangeHelper.from(getXSpreadSheet(), rangeData.getRangePosition(startPos)).setDataInRange(rangeData)
				.setRangeProperties(RangeProperties.from().centerJustify().setBorder(BorderFactory.from().allThin().toBorder()));

		boolean zeigeArbeitsSpalten = getKonfigurationSheet().zeigeArbeitsSpalten();
		ColumnProperties spalteBreite = ColumnProperties.from().setWidth(MeldungenSpalte.DEFAULT_SPALTE_NUMBER_WIDTH).isVisible(zeigeArbeitsSpalten);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), TEAM_A_NR_SPALTE, spalteBreite);
		getSheetHelper().setColumnProperties(getXSpreadSheet(), TEAM_B_NR_SPALTE, spalteBreite);

	}

	private void insertSpieltageDaten(List<List<TeamPaarung>> spielPlanHRunde) throws GenerateException {
		// -----------------------------------------------------------------
		RangeData rangeData = new RangeData();
		// int spielCntr = 1;

		int anzSpieltage = spielPlanHRunde.size();
		int anzTeamPaarungen = spielPlanHRunde.get(0).size();

		// hinrunde
		for (int i = 1; i <= anzSpieltage * anzTeamPaarungen; i++) {
			RowData teamPaarungData = rangeData.newRow();
			teamPaarungData.newString(NR_HINRUNDE_PREFIX + i);
		}

		// rückrunde
		for (int i = 1; i <= anzSpieltage * anzTeamPaarungen; i++) {
			RowData teamPaarungData = rangeData.newRow();
			teamPaarungData.newString(NR_RUECKRUNDE_PREFIX + i);
		}

		Position startPos = Position.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE);
		RangeHelper.from(getXSpreadSheet(), rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	private void insertFormulaPunkte() throws GenerateException {
		int letzteSpielZeile = letzteSpielZeile();

		// http://www.ooowiki.de/DeutschEnglischCalcFunktionen.html
		// erste nr reicht, weil beim filldown zeilenr automatisch hoch
		{
			String formulaHeimPunkteStr = "WENN(" + Position.from(SPIELE_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress() + ">"
					+ Position.from(SPIELE_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress() + ";1;0";

			StringCellValue formulaHeimPunkte = StringCellValue.from(getXSpreadSheet()).setValue(formulaHeimPunkteStr)
					.setPos(Position.from(PUNKTE_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE)).setFillAutoDown(letzteSpielZeile);
			getSheetHelper().setFormulaInCell(formulaHeimPunkte);
		}

		// ------------------------------------------------------------------------------------
		{
			String formulaGastPunkteStr = "WENN(" + Position.from(SPIELE_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress() + ">"
					+ Position.from(SPIELE_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress() + ";1;0";

			StringCellValue formulaGastPunkte = StringCellValue.from(getXSpreadSheet()).setValue(formulaGastPunkteStr)
					.setPos(Position.from(PUNKTE_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE)).setFillAutoDown(letzteSpielZeile);
			getSheetHelper().setFormulaInCell(formulaGastPunkte);
		}

	}

	private void insertFormulaTeamNamen() throws GenerateException {
		int letzteSpielZeile = letzteSpielZeile();
		// fill down fuer name
		// erste nr reicht, weil beim filldown zeilenr automatisch hoch
		String formulaNameA = freispielName(meldeListe.formulaSverweisSpielernamen(Position.from(TEAM_A_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress()));
		StringCellValue nameAFormula = StringCellValue.from(getXSpreadSheet()).setValue(formulaNameA).setPos(Position.from(NAME_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE))
				.setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(nameAFormula);

		String formulaNameB = freispielName(meldeListe.formulaSverweisSpielernamen(Position.from(TEAM_B_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress()));
		StringCellValue nameBFormula = StringCellValue.from(getXSpreadSheet()).setValue(formulaNameB).setPos(Position.from(NAME_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE))
				.setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(nameBFormula);
	}

	private String freispielName(String formulaName) {
		return "WENNNV(" + formulaName + ";\"Freispiel\")";
	}

	private int letzteSpielZeile() throws GenerateException {
		return SearchHelper.from(getXSpreadSheet(), RangePosition.from(SPIEL_NR_SPALTE, 0, SPIEL_NR_SPALTE, 999)).searchLastNotEmptyInSpalte().getZeile();
	}

	/**
	 * @return the meldeListe
	 */
	protected final LigaMeldeListeSheet_Update getMeldeListe() {
		return meldeListe;
	}

	/**
	 * @param list
	 * @throws GenerateException
	 */
	public void spielErgebnisseEinlesen(List<List<List<SpielErgebnis>>> list) throws GenerateException {
		RangeData rangeData = new RangeData();

		for (List<List<SpielErgebnis>> runde : list) {
			for (List<SpielErgebnis> begegnung : runde) {
				SheetRunner.testDoCancelTask();
				RowData ergebnisseRow = rangeData.newRow();
				int spieleTeamA = 0;
				int spieleTeamB = 0;
				int spielPunkteA = 0;
				int spielPunkteB = 0;

				for (SpielErgebnis erg : begegnung) {
					spieleTeamA += erg.siegA() ? 1 : 0;
					spieleTeamB += erg.siegB() ? 1 : 0;
					spielPunkteA += erg.getSpielPunkteA();
					spielPunkteB += erg.getSpielPunkteB();
				}
				ergebnisseRow.newInt(spieleTeamA);
				ergebnisseRow.newInt(spieleTeamB);
				ergebnisseRow.newInt(spielPunkteA);
				ergebnisseRow.newInt(spielPunkteB);
			}
		}
		// --------------------
		Position startPosSpiele = Position.from(SPIELE_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE);
		RangeHelper.from(getXSpreadSheet(), rangeData.getRangePosition(startPosSpiele)).setDataInRange(rangeData);
	}
}
