/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

public class SpieltagRanglisteSheet_SortOnly extends SpieltagRanglisteSheet {

	public SpieltagRanglisteSheet_SortOnly(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpieltagNr(getKonfigurationSheet().getAktiveSpieltag());
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
