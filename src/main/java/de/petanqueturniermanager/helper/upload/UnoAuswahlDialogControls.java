/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiServiceFactory;

import de.petanqueturniermanager.helper.Lo;

/**
 * Gemeinsame UNO-Control-Bausteine für einfache Auswahl-Dialoge
 * (Label + ListBox + OK/Abbrechen-Buttons), z. B. {@link FtpServerAuswahlDialog}
 * und {@link ExportFormatAuswahlDialog}.
 */
final class UnoAuswahlDialogControls {

    private UnoAuswahlDialogControls() {
    }

    static void label(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String text, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", text);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        cont.insertByName(name, model);
    }

    static void listBox(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String[] items, int vorauswahl, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("StringItemList", items);
        props.setPropertyValue("MultiSelection", Boolean.FALSE);
        props.setPropertyValue("Dropdown", Boolean.FALSE);
        if (items.length > 0) {
            props.setPropertyValue("SelectedItems", new short[] { (short) vorauswahl });
        }
        cont.insertByName(name, model);
    }

    static void button(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h, short typ) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("PushButtonType", typ);
        cont.insertByName(name, model);
    }
}
