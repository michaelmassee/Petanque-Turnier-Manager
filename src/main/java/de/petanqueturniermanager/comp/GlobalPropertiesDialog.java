/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Modaler UNO-Dialog zur Bearbeitung der Plugin-Konfiguration (GlobalProperties).
 */
public class GlobalPropertiesDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(GlobalPropertiesDialog.class);

    // Namen der Controls
    private static final String CTL_CB_AUTOSAVE    = "cbAutosave";
    private static final String CTL_CB_BACKUP      = "cbBackup";
    private static final String CTL_CB_NEW_VERSION = "cbNewVersion";
    private static final String CTL_CMB_LOGLEVEL   = "cmbLogLevel";

    // Gespeicherte Control-Referenzen für beiOkGeklickt()
    private XCheckBox      cbAutosave;
    private XCheckBox      cbBackup;
    private XCheckBox      cbNewVersion;
    private XTextComponent cmbLogLevel;

    public GlobalPropertiesDialog(XComponentContext xContext) {
        super(xContext);
    }

    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override
    protected String getTitel() {
        return "Plugin Konfiguration";
    }

    @Override
    protected int getBreite() {
        return 200;
    }

    @Override
    protected int getHoehe() {
        return 90;
    }

    @Override
    protected void erstelleFelder(
            XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog
    ) throws com.sun.star.uno.Exception {

        GlobalProperties gp = GlobalProperties.get();

        // --- Checkboxen ---
        fuegeCheckBoxEin(xMSF, cont, CTL_CB_AUTOSAVE,
                "Autosave nach jeder Aktion", 5, 5, 188, 12,
                gp.isAutoSave());
        fuegeCheckBoxEin(xMSF, cont, CTL_CB_BACKUP,
                "Backup vor wichtigen Generierungen", 5, 20, 188, 12,
                gp.isCreateBackup());
        fuegeCheckBoxEin(xMSF, cont, CTL_CB_NEW_VERSION,
                "Neue-Version-Prüfung immer aktiv (Entwicklungsmodus)", 5, 35, 188, 12,
                gp.isNewVersionCheckImmerTrue());

        // --- Log-Level ---
        fuegeFixedTextEin(xMSF, cont, "lblLogLevel", "Log-Level:", 5, 52, 60, 10);
        fuegeComboBoxEin(xMSF, cont, CTL_CMB_LOGLEVEL, new String[]{ "", "info", "debug" },
                70, 50, 120, 12, gp.getLogLevel().toLowerCase());

        // --- Buttons ---
        fuegeButtonEin(xMSF, cont, "btnOk",       "OK",        50,  70, 50, 14,
                (short) PushButtonType.OK_value);
        fuegeButtonEin(xMSF, cont, "btnAbbrechen", "Abbrechen", 110, 70, 70, 14,
                (short) PushButtonType.CANCEL_value);

        // Control-Referenzen für beiOkGeklickt() merken
        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        cbAutosave   = leseCheckBox(xcc, CTL_CB_AUTOSAVE);
        cbBackup     = leseCheckBox(xcc, CTL_CB_BACKUP);
        cbNewVersion = leseCheckBox(xcc, CTL_CB_NEW_VERSION);
        cmbLogLevel  = leseTextComponent(xcc, CTL_CMB_LOGLEVEL);
    }

    @Override
    protected void beiOkGeklickt() throws Exception {
        GlobalProperties gp = GlobalProperties.get();
        String gewaehlterLevel = (cmbLogLevel != null) ? cmbLogLevel.getText() : "";
        gp.speichern(
                cbAutosave   != null && cbAutosave.getState()   == 1,
                cbBackup     != null && cbBackup.getState()     == 1,
                cbNewVersion != null && cbNewVersion.getState() == 1,
                gewaehlterLevel
        );
        NewReleaseChecker.callbacksAusloesen();
        logger.info("Plugin-Konfiguration gespeichert");
    }

    // ---------------------------------------------------------------
    // Hilfsmethoden
    // ---------------------------------------------------------------

    private void fuegeCheckBoxEin(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h, boolean checked)
            throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlCheckBoxModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",     label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        props.setPropertyValue("State",     (short) (checked ? 1 : 0));
        cont.insertByName(name, model);
    }

    private void fuegeFixedTextEin(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",     label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        cont.insertByName(name, model);
    }

    private void fuegeComboBoxEin(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String[] items, int x, int y, int w, int h, String selected)
            throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlComboBoxModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX",      x);
        props.setPropertyValue("PositionY",      y);
        props.setPropertyValue("Width",          w);
        props.setPropertyValue("Height",         h);
        props.setPropertyValue("StringItemList", items);
        props.setPropertyValue("Text",           selected != null ? selected : "");
        props.setPropertyValue("Dropdown",       Boolean.TRUE);
        cont.insertByName(name, model);
    }

    private void fuegeButtonEin(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h, short pushButtonType)
            throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",          label);
        props.setPropertyValue("PositionX",      x);
        props.setPropertyValue("PositionY",      y);
        props.setPropertyValue("Width",          w);
        props.setPropertyValue("Height",         h);
        props.setPropertyValue("PushButtonType", pushButtonType);
        cont.insertByName(name, model);
    }

    private XCheckBox leseCheckBox(XControlContainer xcc, String name) {
        if (xcc == null) return null;
        XControl ctrl = xcc.getControl(name);
        return ctrl != null ? Lo.qi(XCheckBox.class, ctrl) : null;
    }

    private XTextComponent leseTextComponent(XControlContainer xcc, String name) {
        if (xcc == null) return null;
        XControl ctrl = xcc.getControl(name);
        return ctrl != null ? Lo.qi(XTextComponent.class, ctrl) : null;
    }
}
