/**
 * Erstellung 21.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.melee.korunde;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.model.Team;

/**
 * @author Michael Massee
 *
 */
public class KoGruppeABSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(KoGruppeABSheet.class);

	// public static final String SHEETNAME = "KO Runde";
	public static final String CADRAGESHEETNAME = "Cadrage";
	private static final String SHEET_COLOR = "98e2d7";
	public static final String VORRUNDEN_SHEET = "VorRunden";
	public static final String RANGLISTE_AUS_VORUNDE_SHEET = "Rangliste";
	public static final String RANGLISTE_NACH_CADRAGE_SHEET = "RanglisteNachCadrage";

	/**
	 * @param workingSpreadsheet
	 */
	public KoGruppeABSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "KO Gruppe AB");
	}

	@Override
	protected void updateKonfigurationSheet() throws GenerateException {
		// nichts tun hier
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().findByName(CADRAGESHEETNAME);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		NewSheet.from(getWorkingSpreadsheet(), CADRAGESHEETNAME).tabColor(SHEET_COLOR).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create();
		HashSet<Team> teamListeGegenEinanderGespielt = vorRundenEinlesen();
		cadrageErstellen(teamListeGegenEinanderGespielt);
	}

	/**
	 * @throws GenerateException
	 *
	 */
	private void cadrageErstellen(HashSet<Team> teamListeGegenEinanderGespielt) throws GenerateException {
		for (int grpCntr = 0; grpCntr < 10; grpCntr++) {
			ArrayList<Team> gruppeAusRangliste = gruppeAusRanglisteEinlesen(grpCntr);
			if (gruppeAusRangliste.isEmpty()) {
				break; // fertig keine gruppen
			}

			// anzahl cadrage teams ?
			// 2,4,8,16,32
			gruppeAusRangliste.size();

		}
	}

	/**
	 * @param grpNr 0 = Gruppe A = Spalte A , 1 = B
	 * @throws GenerateException
	 */

	private ArrayList<Team> gruppeAusRanglisteEinlesen(int grpNr) throws GenerateException {

		int ersteTeamNrZeile = 0;
		ArrayList<Team> teamList = new ArrayList(); // Arrayliste weil reihenfolge ist wichtig !

		XSpreadsheet rangliste = getSheetHelper().findByName(RANGLISTE_AUS_VORUNDE_SHEET);

		if (null != rangliste) {
			getSheetHelper().setActiveSheet(rangliste);
		} else {
			NewSheet.from(getWorkingSpreadsheet(), RANGLISTE_AUS_VORUNDE_SHEET).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create();
		}

		processBoxinfo("Rangliste einlesen");
		Position teamNrPos = Position.from(grpNr, ersteTeamNrZeile);
		int mxCntr = 0;
		while (mxCntr < 9999) {
			mxCntr++; // no endless loop !
			Integer cellNumTeam = getSheetHelper().getIntFromCell(rangliste, teamNrPos);
			if (cellNumTeam > 0) {
				teamList.add(new Team(cellNumTeam));
			} else {
				break;
			}
			teamNrPos.zeilePlusEins();
		}
		return teamList;
	}

	private HashSet<Team> vorRundenEinlesen() throws GenerateException {

		int headerZeile = 0;
		int ersteTeamNrZeile = 1;
		String rndHeader = "Rnd";

		// vorrunden einlesen
		XSpreadsheet vorRunden = getSheetHelper().findByName(VORRUNDEN_SHEET);

		if (null != vorRunden) {
			getSheetHelper().setActiveSheet(vorRunden);
		} else {
			NewSheet.from(getWorkingSpreadsheet(), VORRUNDEN_SHEET).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create();
		}

		processBoxinfo("Vor-Runden Team Paarungen einlesen");

		HashSet<Team> teamList = new HashSet<>();

		Position headerPos = Position.from(0, headerZeile);
		for (int spalteCnt = 0; spalteCnt < 20; spalteCnt += 3) {
			SheetRunner.testDoCancelTask();
			headerPos.spalte(spalteCnt);
			String header = getSheetHelper().getTextFromCell(vorRunden, headerPos);
			if (StringUtils.isNotEmpty(header) && StringUtils.startsWithIgnoreCase(header, rndHeader)) {
				processBoxinfo("Spalte " + header);
				// header vorhanden
				// team paarungen einlesen
				for (int zeileCntr = ersteTeamNrZeile; zeileCntr < 999; zeileCntr++) {
					// Team Nr ?
					Integer cellNumTeamA = getSheetHelper().getIntFromCell(vorRunden, Position.from(spalteCnt, zeileCntr));
					if (cellNumTeamA > 0) {
						final Team teamA = new Team(cellNumTeamA);
						Team teamA_AusListe = teamList.stream().filter(team -> teamA.equals(team)).findFirst().orElse(teamA);
						teamList.add(teamA_AusListe);
						Integer cellNumTeamB = getSheetHelper().getIntFromCell(vorRunden, Position.from(spalteCnt + 1, zeileCntr));
						if (cellNumTeamB > 0) {
							final Team teamB = new Team(cellNumTeamB);
							Team teamB_AusListe = teamList.stream().filter(team -> teamB.equals(team)).findFirst().orElse(teamB);
							teamList.add(teamB_AusListe);
							teamB_AusListe.addGegner(teamA_AusListe); // gegenseitig eintragen als gegner
						}
					} else {
						break;
					}
				}
			} else {
				break;
			}
		}
		return teamList;
	}
}
