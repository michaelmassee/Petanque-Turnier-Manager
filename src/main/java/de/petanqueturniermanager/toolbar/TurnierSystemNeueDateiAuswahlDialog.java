/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XComponentLoader;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Modaler Dialog zur Auswahl des Turniersystems für ein neues Turnier
 * in einer neuen Calc-Datei.
 * <p>
 * Zeigt dieselbe Auswahlliste wie {@link TurnierSystemAuswahlDialog},
 * erstellt nach Bestätigung jedoch ein neues, sichtbares Calc-Dokument
 * und initialisiert die Meldeliste dort. Das aufrufende Dokument bleibt
 * unverändert.
 */
public class TurnierSystemNeueDateiAuswahlDialog extends TurnierSystemAuswahlDialog {

    private static final Logger logger = LogManager.getLogger(TurnierSystemNeueDateiAuswahlDialog.class);

    public TurnierSystemNeueDateiAuswahlDialog(XComponentContext xContext) {
        super(xContext);
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
        logger.info("Neues Turnier in neuer Datei: {}", gewaehltesTurnierSystem.getBezeichnung());

        XComponentLoader loader = Lo.qi(XComponentLoader.class,
                xContext.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", xContext));

        XSpreadsheetDocument neuesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
        if (neuesDokument == null) {
            logger.error("Neues Calc-Dokument konnte nicht erstellt werden.");
            return;
        }
        WorkingSpreadsheet neuesWs = new WorkingSpreadsheet(xContext, neuesDokument);
        starteNeueTurnierInDokument(neuesWs, gewaehltesTurnierSystem);
    }
}
