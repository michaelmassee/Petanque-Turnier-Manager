/**
 * Erstellung : 10.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
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
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.fehler.sortieren.rangliste"))
					.message(I18n.get("msg.text.keine.rangliste")).show();
		} else {
			getSheetHelper().setActiveSheet(sheet);

			if (!istDieAnzahlSpieltageInDerRanglisteGleichMitDerAnzahlderSpieltagesheets()) {
				String errorMsg = "Die Anzahl der Spielrunden in der Rangliste stimt nicht überein mit der Anzahl der gespielten Speilrunden.";
				MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
						.caption(I18n.get("msg.caption.fehler.sortieren.rangliste")).message(errorMsg).show();
			} else {
				getRangListeSorter().doSort();
			}
		}
	}

	/**
	 * Testet ob die Anzahle der Spielrunden mit der anzahl der Spielrundensheets übereinstimmt
	 * 
	 * @return true wenn gleich
	 * @throws GenerateException
	 */
	@VisibleForTesting
	boolean istDieAnzahlSpieltageInDerRanglisteGleichMitDerAnzahlderSpieltagesheets() throws GenerateException {
		int anzSpielrundenSheets = getAktuelleSpielrundeSheet().countNumberOfSpielRundenSheets(getSpieltagNr());
		int numberOfSpielrundenInSheet = countNumberOfSpielrundenInSheet();
		return anzSpielrundenSheets == numberOfSpielrundenInSheet;
	}

}
