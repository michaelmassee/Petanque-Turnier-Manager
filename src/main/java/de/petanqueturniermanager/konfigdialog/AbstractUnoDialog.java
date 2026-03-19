/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog;

import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;

/**
 * Abstrakte Basisklasse für modale UNO-Dialoge.
 * Kapselt den Dialog-Boilerplate (Modell, Control, Toolkit, Peer) und
 * ruft nach OK {@link #beiOkGeklickt()} auf.
 */
public abstract class AbstractUnoDialog {

    protected final XComponentContext xContext;

    protected AbstractUnoDialog(XComponentContext xContext) {
        this.xContext = xContext;
    }

    /**
     * Erstellt den UNO-Dialog, zeigt ihn an und ruft bei OK {@link #beiOkGeklickt()} auf.
     */
    protected final void erstelleUndAusfuehren() throws com.sun.star.uno.Exception {
        XMultiComponentFactory mcf = xContext.getServiceManager();

        // Dialog-Modell anlegen
        Object dialogModel = mcf.createInstanceWithContext(
                "com.sun.star.awt.UnoControlDialogModel", xContext);
        XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
        dlgProps.setPropertyValue("PositionX", Integer.valueOf(50));
        dlgProps.setPropertyValue("PositionY", Integer.valueOf(50));
        dlgProps.setPropertyValue("Width",    Integer.valueOf(getBreite()));
        dlgProps.setPropertyValue("Height",   Integer.valueOf(getHoehe()));
        dlgProps.setPropertyValue("Moveable", Boolean.TRUE);
        dlgProps.setPropertyValue("Sizeable", Boolean.valueOf(istVeraenderbar()));
        dlgProps.setPropertyValue("Title",    getTitel());

        // Dialog-Control anlegen
        Object dialog = mcf.createInstanceWithContext(
                "com.sun.star.awt.UnoControlDialog", xContext);
        XControl xControl = Lo.qi(XControl.class, dialog);
        xControl.setModel(Lo.qi(XControlModel.class, dialogModel));
        XDialog xDialog = Lo.qi(XDialog.class, dialog);

        // Toolkit + Peer erzeugen
        Object toolkit = mcf.createInstanceWithContext(
                "com.sun.star.awt.Toolkit", xContext);
        XToolkit xToolkit = Lo.qi(XToolkit.class, toolkit);
        XWindow xWindow = Lo.qi(XWindow.class, xControl);
        xWindow.setVisible(false);
        xControl.createPeer(xToolkit, null);
        XWindowPeer windowPeer = xControl.getPeer();

        // Steuerelemente durch Unterklasse hinzufügen
        erstelleFelder(
                mcf,
                Lo.qi(XMultiServiceFactory.class, dialogModel),
                Lo.qi(XNameContainer.class, dialogModel),
                xToolkit, windowPeer, dlgProps, xDialog);

        // Dialog ausführen – 1 = OK, 0 = Abbrechen/X
        short ergebnis = xDialog.execute();
        if (ergebnis == 1) {
            try {
                beiOkGeklickt();
            } catch (com.sun.star.uno.Exception e) {
                throw e;
            } catch (Exception e) {
                throw new com.sun.star.uno.Exception(e.getMessage(), e);
            }
        }

        Lo.qi(XComponent.class, dialog).dispose();
    }

    /** Titel der Titelleiste. */
    protected abstract String getTitel();

    /** Breite des Dialogs in UNO-Einheiten. */
    protected abstract int getBreite();

    /** Anfangshöhe des Dialogs in UNO-Einheiten. */
    protected abstract int getHoehe();

    /** Ob der Dialog vom Benutzer in der Größe verändert werden kann. Standard: false. */
    protected boolean istVeraenderbar() {
        return false;
    }

    /**
     * Unterklassen fügen hier ihre UNO-Steuerelemente zum Dialog hinzu.
     *
     * @param mcf       XMultiComponentFactory für das Erzeugen von Diensten
     * @param xMSF      XMultiServiceFactory des Dialog-Modells (zum Erzeugen von Control-Modellen)
     * @param cont      XNameContainer des Dialog-Modells (zum Einfügen von Control-Modellen)
     * @param xToolkit  Toolkit für Peer-Erzeugung
     * @param peer      Fenster-Peer des Dialogs
     * @param dlgProps  Properties des Dialog-Modells
     * @param xDialog   Der laufende Dialog (z.B. für endExecute())
     */
    protected abstract void erstelleFelder(
            XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog
    ) throws com.sun.star.uno.Exception;

    /**
     * Wird nach Klick auf OK aufgerufen. Standard-Implementierung ist leer.
     */
    protected void beiOkGeklickt() throws Exception {
        // Standard: keine Aktion
    }
}
