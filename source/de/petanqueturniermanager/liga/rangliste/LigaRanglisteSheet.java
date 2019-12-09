/**
 * Erstellung 09.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;

/**
 * @author Michael Massee
 *
 */
public class LigaRanglisteSheet extends LigaSheet implements ISheet {

	private static final Logger logger = LogManager.getLogger(LigaRanglisteSheet.class);
	private static final String SHEETNAME = "Rangliste";
	private static final String SHEET_COLOR = "d637e8";

	/**
	 * @param workingSpreadsheet
	 * @param logPrefix
	 */
	public LigaRanglisteSheet(WorkingSpreadsheet workingSpreadsheet, String logPrefix) {
		super(workingSpreadsheet, logPrefix);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(getWorkingSpreadsheet(), SHEETNAME).pos(DefaultSheetPos.LIGA_ENDRANGLISTE).tabColor(SHEET_COLOR).setActiv().hideGrid().forceCreate().create()
				.isDidCreate()) {
			getxCalculatable().enableAutomaticCalculation(false); // speed up
			upDateSheet();
		}

	}

	/**
	 *
	 */
	private void upDateSheet() {
		// TODO Auto-generated method stub

	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

}
