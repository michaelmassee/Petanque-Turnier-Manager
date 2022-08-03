package de.petanqueturniermanager.forme.korunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellung 27.07.2022 / Michael Massee
 */

public class VorrundenSheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(KoGruppeABSheet.class);

	public static final String SHEETNAME = "Ergebnisse aus Vorrunden";
	private static final String SHEET_COLOR = "98e2d7";

	public VorrundenSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem spielSystem) {
		super(workingSpreadsheet, spielSystem);
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
	protected IKonfigurationSheet getKonfigurationSheet() {
		return null;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).tabColor(SHEET_COLOR).hideGrid().setActiv()
				.create().isDidCreate()) {

		}

	}

}
