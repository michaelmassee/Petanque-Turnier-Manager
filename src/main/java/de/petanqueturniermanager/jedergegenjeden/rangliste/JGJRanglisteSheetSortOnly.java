package de.petanqueturniermanager.jedergegenjeden.rangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

/**
 * Erstellung 20.06.2022 / Michael Massee
 */

public class JGJRanglisteSheetSortOnly extends JGJRanglisteSheet {

	public JGJRanglisteSheetSortOnly(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			String errorMsg = "Keine Rangliste vorhanden.";
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Fehler beim Sortieren von Rangliste")
					.message(errorMsg).show();
		} else {
			getSheetHelper().setActiveSheet(sheet);
			getRangListeSorter().doSort();
		}
	}

}
