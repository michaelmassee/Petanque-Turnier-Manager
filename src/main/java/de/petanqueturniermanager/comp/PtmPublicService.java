/**
 * Erstellung : 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Öffentlicher UNO-Service für den Datenzugriff aus VB-Makros.
 * <p>
 * Aufruf aus LibreOffice Basic:
 * <pre>
 *   Dim oApi As Object
 *   oApi = createUnoService("de.petanqueturniermanager.PublicService")
 *   MsgBox "System: " & oApi.getTurniersystem()
 *   MsgBox "Runde: " & oApi.getAktuelleRunde()
 * </pre>
 */
public class PtmPublicService extends WeakBase implements XServiceInfo {

    private static final Logger logger = LogManager.getLogger(PtmPublicService.class);

    static final String IMPL_NAME = "de.petanqueturniermanager.comp.PtmPublicService";
    static final String SERVICE_NAME = "de.petanqueturniermanager.PublicService";
    private static final String[] SERVICE_NAMES = { SERVICE_NAME };

    private final XComponentContext xContext;

    public PtmPublicService(XComponentContext xContext) {
        this.xContext = xContext;
        PetanqueTurnierMngrSingleton.init(xContext);
    }

    // -------------------------------------------------------------------------
    // UNO Factory

    public static XSingleComponentFactory __getComponentFactory(String name) {
        if (name.equals(IMPL_NAME)) {
            return Factory.createComponentFactory(PtmPublicService.class, SERVICE_NAMES);
        }
        return null;
    }

    public static boolean __writeRegistryServiceInfo(XRegistryKey regKey) {
        return Factory.writeRegistryServiceInfo(IMPL_NAME, SERVICE_NAMES, regKey);
    }

    // -------------------------------------------------------------------------
    // XServiceInfo

    @Override
    public String getImplementationName() {
        return IMPL_NAME;
    }

    @Override
    public boolean supportsService(String name) {
        return Arrays.asList(SERVICE_NAMES).contains(name);
    }

    @Override
    public String[] getSupportedServiceNames() {
        return SERVICE_NAMES;
    }

    // -------------------------------------------------------------------------
    // Interner Dokument-Zugriff

    private DocumentPropertiesHelper getDocumentPropertiesHelper() {
        try {
            XSpreadsheetDocument doc = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
            if (doc != null) {
                return new DocumentPropertiesHelper(doc);
            }
        } catch (Exception e) {
            logger.error("getDocumentPropertiesHelper", e);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Öffentliche API

    /**
     * @return Name des aktiven Turniersystems, z.B. "Super Melee", "Schweizer System"
     */
    public String getTurniersystem() {
        DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
        if (hlpr != null) {
            return hlpr.getTurnierSystemAusDocument().getBezeichnung();
        }
        return TurnierSystem.KEIN.getBezeichnung();
    }

    /**
     * @return Aktuelle Rundennummer (0 wenn kein Turnier aktiv)
     */
    public int getAktuelleRunde() {
        DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
        if (hlpr != null) {
            return hlpr.getIntProperty("Spielrunde", 0);
        }
        return 0;
    }

    /**
     * @return Aktueller Spieltag (0 wenn kein Turnier aktiv oder kein Spieltag vorhanden)
     */
    public int getAktuellerSpieltag() {
        DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
        if (hlpr != null) {
            return hlpr.getIntProperty("Spieltag", 0);
        }
        return 0;
    }

    /**
     * @return true wenn gerade eine Hintergrund-Operation läuft
     */
    public boolean isOperationAktiv() {
        return SheetRunner.isRunning();
    }
}
