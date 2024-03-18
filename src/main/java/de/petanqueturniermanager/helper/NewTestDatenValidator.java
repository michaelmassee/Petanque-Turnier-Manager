package de.petanqueturniermanager.helper;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Erstellung 19.06.2022 / Michael Massee
 */

import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * prüfen ob bereits <test>daten vorhanden bevor alles überschrieben wird
 * 
 * @author michael
 *
 */
public class NewTestDatenValidator {

	private final WorkingSpreadsheet wkSheet;
	private final SheetHelper sheetHelper;
	private final TurnierSystem turnierSystem;
	private String nextLogPrefix;

	private NewTestDatenValidator(WorkingSpreadsheet wkSheet, SheetHelper sheetHelper, TurnierSystem turnierSystem) {
		this.wkSheet = wkSheet;
		this.sheetHelper = sheetHelper;
		this.turnierSystem = turnierSystem;
		nextLogPrefix = null;
	}

	public static NewTestDatenValidator from(WorkingSpreadsheet wkSheet, SheetHelper sheetHelper,
			TurnierSystem turnierSystem) {
		return new NewTestDatenValidator(wkSheet, sheetHelper, turnierSystem);
	}

	/**
	 * @param ProcessBox logprefix
	 * @return
	 */
	public NewTestDatenValidator prefix(String nextLogPrefix) {
		this.nextLogPrefix = nextLogPrefix;
		return this;
	}

	/**
	 * 
	 * @return true to continue
	 */
	public final boolean validate() {
		if (isTurnierVorhanden()) {
			MessageBoxResult result = MessageBox.from(wkSheet.getxContext(), MessageBoxTypeEnum.WARN_YES_NO)
					.caption("Daten sind bereits vorhanden")
					.message("Achtung: Turnier-Daten sind bereits vorhanden.\r\nLöschen und neu erstellen ?").show();
			if (MessageBoxResult.NO == result) {
				ProcessBox.from().prefix(nextLogPrefix).info("Erstelle Testdaten, Abbruch durch Benutzer.");
				return false;
			}
		}
		return true;
	}

	public final boolean isTurnierVorhanden() {
		TurnierSystem turnierSystemAusDocument = new DocumentPropertiesHelper(wkSheet).getTurnierSystemAusDocument();
		return turnierSystemAusDocument == turnierSystem
				&& sheetHelper.findByName(MeldeListeKonstanten.SHEETNAME) != null;
	}

}
