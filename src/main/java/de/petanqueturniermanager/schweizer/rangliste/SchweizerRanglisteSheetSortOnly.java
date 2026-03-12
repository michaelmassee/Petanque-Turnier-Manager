package de.petanqueturniermanager.schweizer.rangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

/**
 * Sortiert die bestehende Schweizer Rangliste neu, ohne sie komplett neu aufzubauen.
 */
public class SchweizerRanglisteSheetSortOnly extends SchweizerRanglisteSheet {

	public SchweizerRanglisteSheetSortOnly(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public void doRun() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption("Fehler beim Sortieren der Rangliste")
					.message("Keine Rangliste vorhanden.").show();
		} else {
			getSheetHelper().setActiveSheet(sheet);
			getRangListeSorter().doSort();
		}
	}
}
