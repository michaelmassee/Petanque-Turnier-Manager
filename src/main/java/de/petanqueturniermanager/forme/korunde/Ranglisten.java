/**
 * Erstellung 27.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.forme.korunde;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
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
public class Ranglisten {

	private final WeakRefHelper<ISheet> parentSheet;

	public Ranglisten(ISheet parentSheet) {
		this.parentSheet = new WeakRefHelper<>(parentSheet);
	}

	public TeamRangliste gruppeAusRanglisteEinlesen(int grpNr, String ranglisteSheetName) throws GenerateException {

		int ersteTeamNrZeile = 0;
		TeamRangliste teamList = new TeamRangliste();

		XSpreadsheet rangliste = getSheetHelper().findByName(ranglisteSheetName);

		if (null != rangliste) {
			getSheetHelper().setActiveSheet(rangliste);
		} else {
			rangliste = NewSheet.from(parentSheet.get(), ranglisteSheetName).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create().getSheet();
		}

		processBoxinfo("Rangliste einlesen");
		Position teamNrPos = Position.from(grpNr, ersteTeamNrZeile);
		int mxCntr = 0;
		while (mxCntr < 9999) {
			mxCntr++; // no endless loop !
			Integer cellNumTeam = getSheetHelper().getIntFromCell(rangliste, teamNrPos);
			if (cellNumTeam > 0) {
				teamList.add(Team.from(cellNumTeam));
			} else {
				break;
			}
			teamNrPos.zeilePlusEins();
		}
		return teamList;
	}

	/**
	 * @param string
	 */
	private void processBoxinfo(String string) {
		parentSheet.get().processBoxinfo(string);

	}

	/**
	 * @return
	 * @throws GenerateException
	 */
	private SheetHelper getSheetHelper() throws GenerateException {
		return parentSheet.get().getSheetHelper();
	}

}
