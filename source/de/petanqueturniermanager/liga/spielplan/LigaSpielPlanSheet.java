/**
 * Erstellung 29.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.spielplan;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.rangliste.AbstractRanglisteFormatter;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.liga.meldeliste.AbstractLigaMeldeListeSheet;
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
	private static final int PUNKTE_NR_WIDTH = AbstractRanglisteFormatter.ENDSUMME_NUMBER_WIDTH;

	private static final String NR_HINRUNDE_PREFIX = "HR-";
	private static final String NR_RUECKRUNDE_PREFIX = "RR-";

	// Arbeitsspalten
	private static final int TEAM_A_NR_SPALTE = 14; // Zeile 0
	private static final int TEAM_B_NR_SPALTE = TEAM_A_NR_SPALTE + 1; // Zeile 0

	/**
	 * @param workingSpreadsheet
	 */
	public LigaSpielPlanSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
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
		AbstractLigaMeldeListeSheet meldeListe = new LigaMeldeListeSheet_Update(getWorkingSpreadsheet());
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
		insertSpieltageDaten(spielPlanHRunde, spielPlanRRunde);
	}

	private void insertDatenHeaderUndSpalteBreite() throws GenerateException {

		// Header zusammen bauen
		// -----------------------------------------------------------------
		Position headerPos = Position.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_HEADER_ZEILE);
		StringCellValue stValHeader = StringCellValue.from(getXSpreadSheet(), headerPos).setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
				.setShrinkToFit(true);
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Nr.").setEndPosMergeZeilePlus(1));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("KW").spaltePlusEins().setEndPosMergeZeilePlus(1));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Datum").spaltePlusEins().setEndPosMergeZeilePlus(1));

		// header erste Zeile
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Name").spaltePlusEins().setEndPosMergeSpaltePlus(1));
		Position namePos = Position.from(stValHeader.getPos()); // Pos Merken
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Punkte").spaltePlus(2).setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Spiele").spaltePlus(2).setEndPosMergeSpaltePlus(1));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("SpPunkte").spaltePlus(2).setEndPosMergeSpaltePlus(1));

		// header zweite Zeile
		stValHeader.setPos(namePos).setEndPosMerge(null).zeilePlusEins().spaltePlus(-1);
		ColumnProperties colProp = ColumnProperties.from().setWidth(LIGA_MELDUNG_NAME_WIDTH);
		// name
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Heim").spaltePlusEins().setColumnProperties(colProp));
		getSheetHelper().setStringValueInCell(stValHeader.setValue("Gast").spaltePlusEins());

		// Punkte
		stValHeader.getColumnProperties().setWidth(PUNKTE_NR_WIDTH);
		for (int i = 0; i < 3; i++) {
			getSheetHelper().setStringValueInCell(stValHeader.setValue("H").spaltePlusEins());
			getSheetHelper().setStringValueInCell(stValHeader.setValue("G").spaltePlusEins());
		}
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
		RangeHelper.from(getXSpreadSheet(), rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	private void insertSpieltageDaten(List<List<TeamPaarung>> spielPlanHRunde, List<List<TeamPaarung>> spielPlanRRunde) throws GenerateException {
		// -----------------------------------------------------------------
		RangeData rangeData = new RangeData();
		int spielCntr = 1;

		// hinrunde
		for (List<TeamPaarung> spielTag : spielPlanHRunde) {
			for (TeamPaarung teamPaarung : spielTag) {
				addNeueTeamPaarungZeile(NR_HINRUNDE_PREFIX, teamPaarung, rangeData, spielCntr);
				spielCntr++;
			}
		}

		// rückrunde
		for (List<TeamPaarung> spielTag : spielPlanRRunde) {
			for (TeamPaarung teamPaarung : spielTag) {
				addNeueTeamPaarungZeile(NR_RUECKRUNDE_PREFIX, teamPaarung, rangeData, spielCntr);
				spielCntr++;
			}
		}
		Position startPos = Position.from(SPIEL_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE);
		RangeHelper.from(getXSpreadSheet(), rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}

	private void addNeueTeamPaarungZeile(String prefix, TeamPaarung teamPaarung, RangeData rangeData, int spielCntr) {
		RowData teamPaarungData = rangeData.newRow();
		teamPaarungData.newString(prefix + spielCntr);
		teamPaarungData.newEmpty();
		teamPaarungData.newEmpty();
		teamPaarungData.newInt(teamPaarung.getA().getNr());
		teamPaarungData.newInt(teamPaarung.getOptionalB().isPresent() ? teamPaarung.getB().getNr() : 0);
	}

}
