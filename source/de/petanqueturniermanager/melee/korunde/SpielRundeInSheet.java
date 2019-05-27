/**
 * Erstellung 27.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.melee.korunde;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.KoRundeTeamPaarungen;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.TeamPaarung;

/**
 * @author Michael Massee
 *
 */
public class SpielRundeInSheet {

	private static final int HEADERZEILE = 0; // = A
	private static final int ERSTEZEILE = 1; // = B

	private final WeakRefHelper<SheetRunner> parentSheet;

	public SpielRundeInSheet(SheetRunner parentSheet) {
		this.parentSheet = new WeakRefHelper<>(parentSheet);
	}

	public void erstelleSpielRundeInSheet(int grpCntr, XSpreadsheet spreadsheet, KoRundeTeamPaarungen teamPaarungen, FormeSpielrunde spielRunde) throws GenerateException {

		Position posHeader = Position.from(grpCntr * 2, HEADERZEILE);
		String gruppeLetter = String.valueOf((char) ((grpCntr + 1) + 64));
		StringCellValue header = StringCellValue.from(spreadsheet, posHeader).setEndPosMergeSpaltePlus(1).setValue("Gruppe " + gruppeLetter);
		getSheetHelper().setTextInCell(header);

		Position posPaarungen = Position.from(grpCntr * 2, ERSTEZEILE);
		for (TeamPaarung teamPaarung : spielRunde.getTeamPaarungen()) {
			NumberCellValue teamA = NumberCellValue.from(spreadsheet, posPaarungen).setValue((double) teamPaarung.getA().getNr());
			getSheetHelper().setValInCell(teamA);
			NumberCellValue teamB = NumberCellValue.from(teamA).spaltePlusEins().setValue((double) teamPaarung.getB().getNr());
			getSheetHelper().setValInCell(teamB);
			posPaarungen.zeilePlusEins();
		}

		if (teamPaarungen.isDoppelteGespieltePaarungenVorhanden()) {
			// FEHLER wenn nicht alle paarungen ausgelost werden konnte !
			StringCellValue.from(spreadsheet, posPaarungen).setValue(teamPaarungen.getDoppelteGespieltePaarungen());
		}
	}

	/**
	 * @return
	 * @throws GenerateException
	 */
	private SheetHelper getSheetHelper() throws GenerateException {
		return parentSheet.getObject().getSheetHelper();
	}

}
