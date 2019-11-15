/**
 * Erstellung 27.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.forme.korunde;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamRangliste;

/**
 * @author Michael Massee
 *
 */
public class Vorrunden {

	private static final String VORRUNDEN_SHEET = "VorRunden";
	private final WeakRefHelper<SheetRunner> parentSheet;
	private final String RNDHEADER = "Rnd";

	public Vorrunden(SheetRunner parentSheet) {
		this.parentSheet = new WeakRefHelper<>(parentSheet);
	}

	public XSpreadsheet getSheet() throws GenerateException {
		XSpreadsheet vorRunden = getSheetHelper().findByName(VORRUNDEN_SHEET);

		if (null != vorRunden) {
			getSheetHelper().setActiveSheet(vorRunden);
		} else {
			vorRunden = NewSheet.from(getWorkingSpreadsheet(), VORRUNDEN_SHEET).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create().getSheet();
		}

		return vorRunden;
	}

	public void vorRundenEinlesen(TeamRangliste rangliste) throws GenerateException {

		int headerZeile = 0;
		int ersteTeamNrZeile = 1;

		// vorrunden einlesen
		XSpreadsheet vorRunden = getSheet();

		processBoxinfo("Vor-Runden Team Paarungen einlesen");

		ImmutableList<Team> teamList = rangliste.getTeamListe();

		Position headerPos = Position.from(0, headerZeile);
		for (int spalteCnt = 0; spalteCnt < 20; spalteCnt += 2) {
			SheetRunner.testDoCancelTask();
			headerPos.spalte(spalteCnt);
			String header = getSheetHelper().getTextFromCell(vorRunden, headerPos);
			if (StringUtils.isNotEmpty(header) && StringUtils.startsWithIgnoreCase(header, RNDHEADER)) {
				// header vorhanden
				// team paarungen einlesen
				for (int zeileCntr = ersteTeamNrZeile; zeileCntr < 999; zeileCntr++) {
					// Team Nr ?
					Integer cellNumTeamA = getSheetHelper().getIntFromCell(vorRunden, Position.from(spalteCnt, zeileCntr));
					if (cellNumTeamA > 0) {
						final Team teamA = new Team(cellNumTeamA);
						Team teamA_AusListe = teamList.stream().filter(team -> teamA.equals(team)).findFirst().orElse(null);
						if (teamA_AusListe != null) {
							Integer cellNumTeamB = getSheetHelper().getIntFromCell(vorRunden, Position.from(spalteCnt + 1, zeileCntr));
							if (cellNumTeamB > 0) {
								final Team teamB = new Team(cellNumTeamB);
								Team teamB_AusListe = teamList.stream().filter(team -> teamB.equals(team)).findFirst().orElse(null);
								if (teamB_AusListe != null) {
									teamB_AusListe.addGegner(teamA_AusListe); // gegenseitig eintragen als gegner
								}
							}
						}
					} else {
						break;
					}
				}
			} else {
				break;
			}
		}
	}

	/**
	 * @param string
	 */
	private void processBoxinfo(String string) {
		parentSheet.get().processBoxinfo(string);

	}

	/**
	 * @return
	 */
	private WorkingSpreadsheet getWorkingSpreadsheet() {
		return parentSheet.get().getWorkingSpreadsheet();
	}

	/**
	 * @return
	 * @throws GenerateException
	 */
	private SheetHelper getSheetHelper() throws GenerateException {
		return parentSheet.get().getSheetHelper();
	}

}
