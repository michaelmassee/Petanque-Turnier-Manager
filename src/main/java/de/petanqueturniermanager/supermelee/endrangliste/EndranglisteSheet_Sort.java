package de.petanqueturniermanager.supermelee.endrangliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

public class EndranglisteSheet_Sort extends EndranglisteSheet {

	public EndranglisteSheet_Sort(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			String errorMsg = I18n.get("msg.text.keine.rangliste");
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.fehler.sortieren.rangliste"))
					.message(errorMsg).show();
		} else {
			TurnierSheet.from(sheet, getWorkingSpreadsheet()).setActiv();
			getRangListeSorter().doSort();
		}
	}

}
