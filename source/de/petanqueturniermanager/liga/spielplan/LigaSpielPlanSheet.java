/**
 * Erstellung 29.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.spielplan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.liga.meldeliste.AbstractLigaMeldeListeSheet;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_Update;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundePlan;

/**
 * @author Michael Massee
 *
 */
public class LigaSpielPlanSheet extends SuperMeleeSheet implements ISheet {

	private static final Logger LOGGER = LogManager.getLogger(SpielrundePlan.class);
	private static final String SHEET_COLOR = "b0f442";
	public static final String SHEET_NAMEN = "Liga Spielplan";

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
		generate(meldeListe.getAlleMeldungen());
	}

	public void generate(Meldungen meldungen) {
		processBoxinfo("Neue Liga SpielPlan");
		if (!NewSheet.from(getWorkingSpreadsheet(), SHEET_NAMEN).pos(DefaultSheetPos.LIGA_WORK).setForceCreate(true).setActiv().tabColor(SHEET_COLOR).create().isDidCreate()) {
			ProcessBox.from().info("Abbruch vom Benutzer, Liga SpielPlan wurde nicht erstellt");
			return;
		}

		// new LigaSpielPlan(meldungen);

	}

}
