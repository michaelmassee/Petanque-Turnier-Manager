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

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.rangliste.AbstractRanglisteFormatter;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SearchHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_Update;
import de.petanqueturniermanager.model.LigaSpielPlan;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
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
	private static final int ERSTE_SPIELTAG_DATEN_ZEILE = ERSTE_SPIELTAG_HEADER_ZEILE + 2; // Zeile 2
	private static final int SPIEL_NR_SPALTE = 0; // Spalte A
	private static final int KW_SPALTE = SPIEL_NR_SPALTE + 1;
	private static final int DATUM_SPALTE = KW_SPALTE + 1;
	private static final int NAME_A_SPALTE = DATUM_SPALTE + 1;
	private static final int NAME_B_SPALTE = NAME_A_SPALTE + 1;
	private static final int PUNKTE_A_SPALTE = NAME_B_SPALTE + 1;
	private static final int PUNKTE_B_SPALTE = PUNKTE_A_SPALTE + 1;
	private static final int SPIELE_A_SPALTE = PUNKTE_B_SPALTE + 1;
	private static final int SPIELE_B_SPALTE = SPIELE_A_SPALTE + 1;
	private static final int SPIELPNKT_A_SPALTE = SPIELE_B_SPALTE + 1;
	private static final int SPIELPNKT_B_SPALTE = SPIELPNKT_A_SPALTE + 1;

	private static final int PUNKTE_NR_WIDTH = AbstractRanglisteFormatter.ENDSUMME_NUMBER_WIDTH;

	private static final String NR_HINRUNDE_PREFIX = "HR-";
	private static final String NR_RUECKRUNDE_PREFIX = "RR-";

	// Arbeitsspalten
	private static final int TEAM_A_NR_SPALTE = 14; // Zeile 0
	private static final int TEAM_B_NR_SPALTE = TEAM_A_NR_SPALTE + 1; // Zeile 0

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
		processBoxinfo("Neue Liga SpielPlan");
		if (!NewSheet.from(getWorkingSpreadsheet(), SHEET_NAMEN).pos(DefaultSheetPos.LIGA_WORK).setForceCreate(true).setActiv().hideGrid().tabColor(SHEET_COLOR).create()
				.isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Liga SpielPlan wurde nicht erstellt");
			return;
		}

		LigaSpielPlan ligaSpielPlan = new LigaSpielPlan(meldungen);
		List<List<TeamPaarung>> spielPlanHRunde = ligaSpielPlan.schufflePlan().getSpielPlan();
		List<List<TeamPaarung>> spielPlanRRunde = ligaSpielPlan.schufflePlan().getSpielPlan();

		insertDatenHeaderUndSpalteBreite();
		insertArbeitsspalten(spielPlanHRunde, spielPlanRRunde);
		insertSpieltageDaten(spielPlanHRunde);
		insertFormulaTeamNamen();
		formatieren();

	}

	private void formatieren() throws GenerateException {
		// erstmal ein Grid
		int letzteSpielZeile = letzteSpielZeile();
		RangePosition allData = RangePosition.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE, SPIELPNKT_B_SPALTE, letzteSpielZeile);
		RangeProperties rangeProp = RangeProperties.from().setBorder(BorderFactory.from().allThin().toBorder()).setHoriJustify(CellHoriJustify.CENTER)
				.setVertJustify(CellVertJustify2.CENTER).setShrinkToFit(true).topMargin(150).bottomMargin(150);
		RangeHelper.from(getXSpreadSheet(), allData).setRangeProperties(rangeProp);
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

				RowData teamPaarungData = rangeData.newRow();
				teamPaarungData.newInt(teamPaarung.getA().getNr());
				teamPaarungData.newInt(teamPaarung.getOptionalB().isPresent() ? teamPaarung.getB().getNr() : 0);
			}
		}
		Position startPos = Position.from(TEAM_A_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE);
		RangeHelper.from(getXSpreadSheet(), rangeData.getRangePosition(startPos)).setDataInRange(rangeData)
				.setRangeProperties(RangeProperties.from().setBorder(BorderFactory.from().allThin().toBorder()));
	}

	private void insertSpieltageDaten(List<List<TeamPaarung>> spielPlanHRunde) throws GenerateException {
		// -----------------------------------------------------------------
		RangeData rangeData = new RangeData();
		int spielCntr = 1;

		int anzSpieltage = spielPlanHRunde.size();
		int anzTeamPaarungen = spielPlanHRunde.get(0).size();

		// hinrunde
		for (int i = 1; i <= anzSpieltage * anzTeamPaarungen; i++) {
			RowData teamPaarungData = rangeData.newRow();
			teamPaarungData.newString(NR_HINRUNDE_PREFIX + spielCntr++);
		}

		// rückrunde
		for (int i = 1; i <= anzSpieltage * anzTeamPaarungen; i++) {
			RowData teamPaarungData = rangeData.newRow();
			teamPaarungData.newString(NR_RUECKRUNDE_PREFIX + spielCntr++);
		}

		Position startPos = Position.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE);
		RangeHelper.from(getXSpreadSheet(), rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	private void insertFormulaTeamNamen() throws GenerateException {
		int letzteSpielZeile = letzteSpielZeile();
		// fill down fuer name
		// erste nr reicht, weil beim filldown zeilenr automatisch hoch
		String formulaNameA = meldeListe.formulaSverweisSpielernamen(Position.from(TEAM_A_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress());
		StringCellValue nameAFormula = StringCellValue.from(getXSpreadSheet()).setValue(formulaNameA).setPos(Position.from(NAME_A_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE))
				.setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(nameAFormula);

		String formulaNameB = meldeListe.formulaSverweisSpielernamen(Position.from(TEAM_B_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE).getAddress());
		StringCellValue nameBFormula = StringCellValue.from(getXSpreadSheet()).setValue(formulaNameB).setPos(Position.from(NAME_B_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE))
				.setFillAutoDown(letzteSpielZeile);
		getSheetHelper().setFormulaInCell(nameBFormula);

	}

	private int letzteSpielZeile() throws GenerateException {
		return SearchHelper.from(getXSpreadSheet()).searchLastNotEmptyInSpalte(RangePosition.from(SPIEL_NR_SPALTE, 0, SPIEL_NR_SPALTE, 999)).getZeile();
	}

	/**
	 * @return the meldeListe
	 */
	protected final LigaMeldeListeSheet_Update getMeldeListe() {
		return meldeListe;
	}
}
