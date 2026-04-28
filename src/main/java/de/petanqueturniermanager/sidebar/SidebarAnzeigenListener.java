package de.petanqueturniermanager.sidebar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;

/**
 * Aktiviert das PTM-Sidebar-Deck automatisch, wenn der Turniermodus aktiv ist.
 * <p>
 * Zwei Auslöser:
 * <ul>
 *   <li>{@link #onLoad} – beim Laden eines Dokuments mit gespeichertem Turniermodus</li>
 *   <li>{@link #zeigePtmSidebar(WorkingSpreadsheet)} – beim manuellen Einschalten des Turniermodus
 *       (aufgerufen von {@code TurnierModus.aktivierenIntern()})</li>
 * </ul>
 * <p>
 * Intern wird {@code .uno:SidebarDeck} dispatcht. Das entspricht intern {@code Sidebar::ShowDeck()},
 * das die Sidebar per {@code ShowChildWindow} synchron einblendet, bevor es auf das Deck umschaltet.
 * Der frühere Ansatz via {@code XSidebarProvider.setVisible()} war fehlerhaft, weil der zugehörige
 * {@code .uno:Sidebar}-Dispatch asynchron in die Event-Queue eingereiht wird und der
 * {@code SidebarController} daher bei den unmittelbar folgenden {@code showDecks()}/{@code getDecks()}
 * Aufrufen noch nicht existiert.
 */
public class SidebarAnzeigenListener implements IGlobalEventListener {

    private static final Logger logger = LogManager.getLogger(SidebarAnzeigenListener.class);

    static final String PTM_DECK_ID = "PetanqueTurnierManagerDeck";

    @Override
    public void onLoad(Object source) {
        try {
            var xModel = Lo.qi(XModel.class, source);
            if (xModel == null) return;
            var xSpreadsheetDocument = Lo.qi(XSpreadsheetDocument.class, xModel);
            if (xSpreadsheetDocument == null) return;
            var hlpr = new DocumentPropertiesHelper(xSpreadsheetDocument);
            if (!hlpr.getTurnierModusAusDocument()) return;
            zeigePtmSidebarFuerController(xModel.getCurrentController());
        } catch (Exception e) {
            logger.error("Fehler beim automatischen Anzeigen der PTM-Sidebar", e);
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Zeigt das PTM-Sidebar-Deck für das aktive Dokument des {@link WorkingSpreadsheet}.
     * Wird von {@code TurnierModus.aktivierenIntern()} aufgerufen.
     */
    public static void zeigePtmSidebar(WorkingSpreadsheet ws) {
        try {
            var pv = new PropertyValue();
            pv.Name = "SidebarDeck";
            pv.Value = PTM_DECK_ID;
            ws.executeDispatch(".uno:SidebarDeck", "_self", 0, new PropertyValue[]{pv});
            logger.debug("PTM-Sidebar-Deck '{}' aktiviert", PTM_DECK_ID);
        } catch (Exception e) {
            logger.error("Fehler beim Anzeigen der PTM-Sidebar", e);
        }
    }

    // -------------------------------------------------------------------------

    private static void zeigePtmSidebarFuerController(XController controller) {
        if (controller == null) return;
        var frame = controller.getFrame();
        if (frame == null) return;
        try {
            var context = PetanqueTurnierMngrSingleton.getContext();
            if (context == null) {
                logger.warn("Kein Komponentenkontext – PTM-Sidebar kann nicht aktiviert werden");
                return;
            }
            var dispatchHelper = Lo.qi(XDispatchHelper.class,
                    context.getServiceManager().createInstanceWithContext(
                            "com.sun.star.frame.DispatchHelper", context));
            if (dispatchHelper == null) {
                logger.warn("XDispatchHelper nicht verfügbar");
                return;
            }
            var dispatchProvider = Lo.qi(XDispatchProvider.class, frame);
            if (dispatchProvider == null) {
                logger.warn("XDispatchProvider nicht verfügbar");
                return;
            }
            var pv = new PropertyValue();
            pv.Name = "SidebarDeck";
            pv.Value = PTM_DECK_ID;
            dispatchHelper.executeDispatch(dispatchProvider, ".uno:SidebarDeck", "_self", 0, new PropertyValue[]{pv});
            logger.debug("PTM-Sidebar-Deck '{}' via Dispatch aktiviert", PTM_DECK_ID);
        } catch (Exception e) {
            logger.error("Fehler beim Aktivieren des PTM-Sidebar-Decks", e);
        }
    }
}
