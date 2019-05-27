/**
 * Erstellung 21.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.melee.korunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.KoRundeTeamPaarungen;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.TeamRangliste;

/**
 * @author Michael Massee
 *
 */
public class KoGruppeABSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(KoGruppeABSheet.class);

	public static final String SHEETNAME = "KO Runde";
	private static final String SHEET_COLOR = "98e2d7";

	private final Vorrunden vorrunden;
	private final Ranglisten ranglisten;
	private final SpielRundeInSheet spielRundeInSheet;

	/**
	 * @param workingSpreadsheet
	 */
	public KoGruppeABSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "KO Gruppe AB");
		vorrunden = new Vorrunden(this);
		ranglisten = new Ranglisten(this);
		spielRundeInSheet = new SpielRundeInSheet(this);
	}

	@Override
	protected void updateKonfigurationSheet() throws GenerateException {
		// nichts tun hier
	}

	@Override
	public XSpreadsheet getSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		NewSheet.from(getWorkingSpreadsheet(), SHEETNAME).tabColor(SHEET_COLOR).pos(DefaultSheetPos.MELEE_WORK).forceCreate().setActiv().create();
		koRundeErstellen();
		getSheetHelper().setActiveSheet(getSheet());
	}

	/**
	 * @throws GenerateException
	 *
	 */
	private void koRundeErstellen() throws GenerateException {
		for (int grpCntr = 0; grpCntr < 10; grpCntr++) {
			TeamRangliste gruppeAusRanglist = ranglisten.gruppeAusRanglisteEinlesen(grpCntr, CadrageSheet.RANGLISTE_NACH_CADRAGE_SHEET);
			if (gruppeAusRanglist.isEmpty()) {
				break; // fertig keine weitere gruppen
			}
			vorrunden.vorRundenEinlesen(gruppeAusRanglist); // gegner eintragen
			KoRundeTeamPaarungen teamPaarungen = new KoRundeTeamPaarungen(gruppeAusRanglist);
			FormeSpielrunde spielRunde = teamPaarungen.generatSpielRunde();
			getSheetHelper().setActiveSheet(getSheet());
			spielRundeInSheet.erstelleSpielRundeInSheet(grpCntr, getSheet(), teamPaarungen, spielRunde);
		}
	}
}
