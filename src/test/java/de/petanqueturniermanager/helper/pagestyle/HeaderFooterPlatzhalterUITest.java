package de.petanqueturniermanager.helper.pagestyle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.sheet.XHeaderFooterContent;
import com.sun.star.style.XStyleFamiliesSupplier;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;

/**
 * Regressionstest für {@link PageStyleDef}: Platzhalter-Tokens ({@code {SEITE}}, {@code {SEITEN}})
 * müssen beim Schreiben in den PageStyle durch echte UNO-Textfelder ersetzt werden, nicht als
 * Literal-Text stehen bleiben.
 * <p>
 * Prüfmethode: {@code XHeaderFooterContent.getXXXText().getString()} liefert bei echten
 * {@code com.sun.star.text.TextField.PageNumber}/{@code PageCount}-Feldern LO-intern immer die
 * Dummy-Werte 1/99 (siehe {@code ScHeaderFooterTextObj::FillDummyFieldData}), unabhängig vom
 * tatsächlichen Druck-Kontext. Bliebe der Text hingegen ein reiner Literal-String (Bug: Token
 * nicht aufgelöst), stünde dort wörtlich "{SEITE}"/"{SEITEN}".
 */
public class HeaderFooterPlatzhalterUITest extends BaseCalcUITest {

	@Test
	void seiteUndSeitenTokenWerdenAlsEchteUnoFelderGeschrieben() throws GenerateException {
		var meldeListeSheetNew = new MeldeListeSheet_New(wkingSpreadsheet);
		meldeListeSheetNew.createMeldelisteWithParams(SuperMeleeMode.Triplette);

		PageStyleHelper.from(meldeListeSheetNew, PageStyle.PETTURNMNGR)
				.setFooterCenter("Seite {SEITE} von {SEITEN}")
				.create();

		String footerCenterText = liesFooterCenterText(PageStyle.PETTURNMNGR.getName());

		assertThat(footerCenterText)
				.as("Bei echten UNO-Feldern liefert getString() die LO-Dummy-Werte 1/99, nicht die rohen Tokens")
				.isEqualTo("Seite 1 von 99");
	}

	private String liesFooterCenterText(String pageStyleName) throws GenerateException {
		try {
			XStyleFamiliesSupplier familienSupplier = Lo.qi(XStyleFamiliesSupplier.class,
					wkingSpreadsheet.getWorkingSpreadsheetDocument());
			XNameAccess familienNA = familienSupplier.getStyleFamilies();
			Object pageStylesObj = familienNA.getByName("PageStyles");
			XNameAccess pageStylesNA = Lo.qi(XNameAccess.class, pageStylesObj);
			XPropertySet styleProps = Lo.qi(XPropertySet.class, pageStylesNA.getByName(pageStyleName));
			Object footerProp = styleProps.getPropertyValue(PageProperties.RIGHTPAGE_FOOTER_CONTENT);
			XHeaderFooterContent footerContent = Lo.qi(XHeaderFooterContent.class, footerProp);
			return footerContent.getCenterText().getString();
		} catch (Exception e) {
			throw new GenerateException("Fußzeile konnte nicht gelesen werden: " + e.getMessage());
		}
	}
}
