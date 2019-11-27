/**
 * Erstellung 21.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.forme.korunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.CadrageRechner;
import de.petanqueturniermanager.algorithmen.KoRundeTeamPaarungen;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamRangliste;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class CadrageSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(CadrageSheet.class);

	private static final String SHEETNAME = "Cadrage";
	private static final String SHEET_COLOR = "c12439";

	private static final String RANGLISTE_AUS_VORUNDE_SHEET = "RanglisteAusVorrunden";
	public static final String RANGLISTE_NACH_CADRAGE_SHEET = "RanglisteNachCadrage";

	private final Vorrunden vorrunden;
	private final Ranglisten ranglisten;
	private final SpielRundeInSheet spielRundeInSheet;

	/**
	 * @param workingSpreadsheet
	 */
	public CadrageSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SCHWEIZER_KO, "Cadrage");
		vorrunden = new Vorrunden(this);
		ranglisten = new Ranglisten(this);
		spielRundeInSheet = new SpielRundeInSheet(this);
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
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		vorrunden.getSheet(); // erstellen leer wenn nicht vorhanden
		NewSheet.from(getWorkingSpreadsheet(), SHEETNAME).tabColor(SHEET_COLOR).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create();
		cadrageErstellen();
		rangListeNachCadrageErstellen();
	}

	/**
	 * @throws GenerateException
	 *
	 */
	private void rangListeNachCadrageErstellen() throws GenerateException {
		NewSheet ranglisteNachCadrage = NewSheet.from(getWorkingSpreadsheet(), RANGLISTE_NACH_CADRAGE_SHEET).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create();
		for (int grpCntr = 0; grpCntr < 10; grpCntr++) {
			TeamRangliste gruppeAusRanglist = ranglisten.gruppeAusRanglisteEinlesen(grpCntr, RANGLISTE_AUS_VORUNDE_SHEET);
			if (gruppeAusRanglist.isEmpty()) {
				break; // fertig keine weitere gruppen
			}
			int anzTeamsinGruppeAusRangliste = gruppeAusRanglist.size();
			int anzCadrageTeams = new CadrageRechner(anzTeamsinGruppeAusRangliste).anzTeams();
			TeamRangliste ranglisteOhneCadrage = gruppeAusRanglist.ranglisteVonOben(anzTeamsinGruppeAusRangliste - anzCadrageTeams);
			Position posTeamNr = Position.from(grpCntr, 0);
			getSheetHelper().setActiveSheet(ranglisteNachCadrage.getSheet());
			for (Team team : ranglisteOhneCadrage.getTeamListe()) {
				NumberCellValue teamRangliste = NumberCellValue.from(ranglisteNachCadrage.getSheet(), posTeamNr).setValue((double) team.getNr());
				getSheetHelper().setValInCell(teamRangliste);
				posTeamNr.zeilePlusEins();
			}
		}
	}

	/**
	 * @throws GenerateException
	 *
	 */
	private void cadrageErstellen() throws GenerateException {
		for (int grpCntr = 0; grpCntr < 10; grpCntr++) {
			TeamRangliste gruppeAusRanglist = ranglisten.gruppeAusRanglisteEinlesen(grpCntr, RANGLISTE_AUS_VORUNDE_SHEET);
			if (gruppeAusRanglist.isEmpty()) {
				break; // fertig keine weitere gruppen
			}

			// anzahl cadrage teams ?
			// 2,4,8,16,32
			int anzTeamsinGruppeAusRangliste = gruppeAusRanglist.size();
			int anzCadrageTeams = new CadrageRechner(anzTeamsinGruppeAusRangliste).anzTeams();
			processBoxinfo("Gruppe:" + grpCntr + " Anz Cadrage:" + anzCadrageTeams);
			if (anzCadrageTeams > 0) {
				TeamRangliste cadrageRangliste = gruppeAusRanglist.ranglisteVonLetzte(anzCadrageTeams);
				vorrunden.vorRundenEinlesen(cadrageRangliste);
				KoRundeTeamPaarungen teamPaarungen = new KoRundeTeamPaarungen(cadrageRangliste);
				FormeSpielrunde spielRunde = teamPaarungen.generatSpielRunde();
				// Paarungen
				getSheetHelper().setActiveSheet(getXSpreadSheet());
				spielRundeInSheet.erstelleSpielRundeInSheet(grpCntr, getXSpreadSheet(), teamPaarungen, spielRunde);
			}
		}
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		// TODO Auto-generated method stub
		return null;
	}

}
