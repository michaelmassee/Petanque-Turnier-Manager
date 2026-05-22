/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Modaler Dialog zur Auswahl des Turniersystems für ein neues Turnier
 * in einer neuen Calc-Datei.
 * <p>
 * Zeigt dieselbe Auswahlliste wie {@link TurnierSystemAuswahlDialog},
 * erstellt nach Bestätigung jedoch ein neues, sichtbares Calc-Dokument
 * und initialisiert die Meldeliste dort. Das aufrufende Dokument bleibt
 * unverändert; der Fokus wird anschließend explizit auf das neu erstellte
 * Dokument gelegt.
 */
public class TurnierSystemNeueDateiAuswahlDialog extends TurnierSystemAuswahlDialog {

    private static final Logger logger = LogManager.getLogger(TurnierSystemNeueDateiAuswahlDialog.class);

    public TurnierSystemNeueDateiAuswahlDialog(WorkingSpreadsheet aufrufendesWs) {
        super(aufrufendesWs);
    }

    @Override
    protected String getTitel() {
        return I18n.get("toolbar.neu_in_neuer_datei.dialog.titel");
    }

    @Override
    protected void beiOkGeklickt() throws Exception {
        if (listBox == null) {
            return;
        }
        short ausgewaehltIndex = listBox.getSelectedItemPos();
        if (ausgewaehltIndex < 0 || ausgewaehltIndex >= AUSWAHL_SYSTEME.length) {
            return;
        }
        TurnierSystem gewaehltesTurnierSystem = AUSWAHL_SYSTEME[ausgewaehltIndex];
        logger.info("[FOKUS-TRACE] beiOkGeklickt: System={} aufrufendesWs.doc={}",
                gewaehltesTurnierSystem.getBezeichnung(),
                de.petanqueturniermanager.comp.ProtocolHandler.beschreibeDokument(
                        ws == null ? null : ws.getWorkingSpreadsheetDocument()));

        var xContext = ws.getxContext();
        XComponentLoader loader = Lo.qi(XComponentLoader.class,
                xContext.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", xContext));

        XSpreadsheetDocument neuesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
        if (neuesDokument == null) {
            logger.error("Neues Calc-Dokument konnte nicht erstellt werden.");
            return;
        }
        logger.info("[FOKUS-TRACE] neues Dokument erstellt: {}",
                de.petanqueturniermanager.comp.ProtocolHandler.beschreibeDokument(neuesDokument));
        WorkingSpreadsheet neuesWs = new WorkingSpreadsheet(xContext, neuesDokument);
        starteNeueTurnierInDokument(neuesWs, gewaehltesTurnierSystem);
        fokussiereDokument(neuesDokument);
    }

    /**
     * Aktiviert den Frame des neu erstellten Dokuments und bringt sein Top-Window
     * in den Vordergrund. Ohne diesen expliziten Schritt fällt der Window-Manager
     * bei mehreren offenen Calc-Fenstern nach Dialog-Schließen häufig auf das
     * zuerst geöffnete Dokument zurück.
     */
    private static void fokussiereDokument(XSpreadsheetDocument doc) {
        XModel xModel = Lo.qi(XModel.class, doc);
        if (xModel == null) {
            logger.warn("[FOKUS-TRACE] fokussiereDokument: xModel==null");
            return;
        }
        XController controller = xModel.getCurrentController();
        if (controller == null) {
            logger.warn("[FOKUS-TRACE] fokussiereDokument: controller==null");
            return;
        }
        XFrame frame = controller.getFrame();
        if (frame == null) {
            logger.warn("[FOKUS-TRACE] fokussiereDokument: frame==null");
            return;
        }
        logger.info("[FOKUS-TRACE] fokussiereDokument: activate+toFront auf frame#{} (doc={})",
                System.identityHashCode(frame),
                de.petanqueturniermanager.comp.ProtocolHandler.beschreibeDokument(doc));
        frame.activate();
        XTopWindow topWindow = Lo.qi(XTopWindow.class, frame.getContainerWindow());
        if (topWindow != null) {
            topWindow.toFront();
            logger.info("[FOKUS-TRACE] fokussiereDokument: toFront() OK");
        } else {
            logger.warn("[FOKUS-TRACE] fokussiereDokument: ContainerWindow nicht XTopWindow");
        }
    }
}
