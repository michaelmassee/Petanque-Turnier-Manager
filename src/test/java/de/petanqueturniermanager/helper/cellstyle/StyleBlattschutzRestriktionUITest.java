/**
 * Erstellung 16.04.2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.cellstyle;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.Lo;

/**
 * Dokumentiert die LO-interne Einschränkung beim Zusammenspiel von Zell-/Seitenstilen
 * und Sheet-Schutz.<br>
 * <br>
 * LO-Quellcode: {@code sc/source/ui/unoobj/styleuno.cxx},
 * {@code ScStyleObj::setPropertyValue_Impl}:
 *
 * <pre>
 * //  cell styles cannot be modified if any sheet is protected
 * if ( eFamily == SfxStyleFamily::Para &amp;&amp; lcl_AnyTabProtected( pDocShell-&gt;GetDocument() ) )
 *     throw uno::RuntimeException();
 * </pre>
 *
 * Der Check ist explizit auf {@code SfxStyleFamily::Para} (= Zellstile) beschränkt.
 * {@code SfxStyleFamily::Page} (= Seitenstile) ist davon nicht betroffen.
 */
public class StyleBlattschutzRestriktionUITest extends BaseCalcUITest {

    private static final String TEST_ZELLSTIL_NAME = "PTM_TestZellstil";
    private static final String TEST_SEITENSTIL_NAME = "PTM_TestSeitenstil";

    /**
     * Fakt 1: {@code setPropertyValue} auf einem Zellstil wirft
     * {@code com.sun.star.uno.RuntimeException}, sobald irgendein Sheet
     * tab-geschützt ist – auch wenn der Style selbst nicht geschützt ist.
     */
    @Test
    void zellstilAendernWirftRuntimeExceptionBeiGeschuetztemSheet() throws Exception {
        XPropertySet zellstilPropSet = zellstilAnlegen(TEST_ZELLSTIL_NAME);

        XProtectable xProtectable = sheetSchuetzen();
        try {
            assertThatThrownBy(() -> zellstilPropSet.setPropertyValue("CharColor", Integer.valueOf(0xFF0000)))
                    .isInstanceOf(com.sun.star.uno.RuntimeException.class);
        } finally {
            xProtectable.unprotect("");
        }
    }

    /**
     * Fakt 2: {@code setPropertyValue} auf einem Seitenstil funktioniert
     * problemlos, auch wenn ein Sheet tab-geschützt ist. Die LO-Einschränkung
     * gilt nur für {@code SfxStyleFamily::Para} (Zellstile).
     */
    @Test
    void seitenstilAendernFunktioniertBeiGeschuetztemSheet() throws Exception {
        XPropertySet seitenstilPropSet = seitenstilAnlegen(TEST_SEITENSTIL_NAME);

        XProtectable xProtectable = sheetSchuetzen();
        try {
            assertThatCode(() -> seitenstilPropSet.setPropertyValue("TopMargin", Integer.valueOf(1800)))
                    .doesNotThrowAnyException();
        } finally {
            xProtectable.unprotect("");
        }
    }

    // -------------------------------------------------------------------------

    private XPropertySet zellstilAnlegen(String stilName) throws Exception {
        XStyleFamiliesSupplier xFamiliesSupplier = Lo.qi(XStyleFamiliesSupplier.class, doc);
        XNameContainer xCellStylesNA = Lo.qi(XNameContainer.class,
                xFamiliesSupplier.getStyleFamilies().getByName("CellStyles"));
        XMultiServiceFactory xDocServiceManager = Lo.qi(XMultiServiceFactory.class, doc);
        Object cellStyle = xDocServiceManager.createInstance("com.sun.star.style.CellStyle");
        xCellStylesNA.insertByName(stilName, cellStyle);
        return Lo.qi(XPropertySet.class, cellStyle);
    }

    private XPropertySet seitenstilAnlegen(String stilName) throws Exception {
        XStyleFamiliesSupplier xFamiliesSupplier = Lo.qi(XStyleFamiliesSupplier.class, doc);
        XNameContainer xPageStylesNA = Lo.qi(XNameContainer.class,
                xFamiliesSupplier.getStyleFamilies().getByName("PageStyles"));
        XMultiServiceFactory xDocServiceManager = Lo.qi(XMultiServiceFactory.class, doc);
        Object pageStyle = xDocServiceManager.createInstance("com.sun.star.style.PageStyle");
        xPageStylesNA.insertByName(stilName, pageStyle);
        return Lo.qi(XPropertySet.class, pageStyle);
    }

    private XProtectable sheetSchuetzen() {
        XSpreadsheet xSheet = sheetHlp.getSheetByIdx(0);
        XProtectable xProtectable = Lo.qi(XProtectable.class, xSheet);
        xProtectable.protect("");
        return xProtectable;
    }

}
