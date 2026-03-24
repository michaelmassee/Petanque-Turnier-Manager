package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;

/**
 * Regressionstest: NewSheet.create() muss den PageStyle auch dann korrekt setzen,
 * wenn die LibreOffice-Locale nicht Deutsch ist.
 * <p>
 * Bug: applytoSheet() rief iSheet.getXSpreadSheet() → findeSheetUndHeile() auf, bevor
 * die Sheet-Metadaten geschrieben waren. Bei nicht-deutscher Locale (z.B. "Entry List"
 * statt "Meldeliste") fand der Legacy-Fallback das Sheet nicht → null → NPE.
 * Fix: NewSheet.create() übergibt das frisch angelegte Sheet direkt an applytoSheet(XSpreadsheet).
 */
public class NewSheetPageStyleUITest extends BaseCalcUITest {

	@Test
	void meldelisteErstellenSetzPageStyleKorrekt() throws GenerateException {
		// Sheet anlegen – Regression: ohne Fix NPE bei nicht-deutscher LibreOffice-Locale
		var meldeListeSheetNew = new MeldeListeSheet_New(wkingSpreadsheet);
		meldeListeSheetNew.createMeldelisteWithParams(SuperMeleeMode.Triplette);

		// Sheet muss existieren
		XSpreadsheet xSheet = sheetHlp.findByName(SheetNamen.meldeliste());
		assertThat(xSheet).as("Meldeliste-Sheet muss nach createMeldelisteWithParams existieren").isNotNull();

		// PageStyle muss auf "PetTurnMngr" gesetzt sein
		XPropertySet xPropSet = Lo.qi(XPropertySet.class, xSheet);
		assertThat(xPropSet).as("XPropertySet des Meldeliste-Sheets muss verfügbar sein").isNotNull();

		String pageStyleName = liesPageStyleName(xPropSet);
		assertThat(pageStyleName).as("PageStyle muss von NewSheet.create() korrekt auf das neu angelegte Sheet gesetzt worden sein")
				.isEqualTo(PageStyle.PETTURNMNGR.getName());
	}

	private String liesPageStyleName(XPropertySet xPropSet) throws GenerateException {
		try {
			return (String) xPropSet.getPropertyValue("PageStyle");
		} catch (Exception e) {
			throw new GenerateException("PageStyle-Property konnte nicht gelesen werden: " + e.getMessage());
		}
	}
}
