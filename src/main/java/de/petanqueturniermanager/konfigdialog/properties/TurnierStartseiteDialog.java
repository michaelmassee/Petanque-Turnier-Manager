package de.petanqueturniermanager.konfigdialog.properties;

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

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.webserver.WebServerManager;

/**
 * Modaler Dialog zur Konfiguration der Turnier-Startseite (eigener Webserver-Port mit
 * Logo, Turniername und Live-Teilnehmerzahl).
 * <p>
 * Port + Aktiv-Flag liegen in {@link GlobalProperties} (per Maschine); Turnierlogo-URL
 * und Turniername als Document-Custom-Properties (per Datei).
 */
public class TurnierStartseiteDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(TurnierStartseiteDialog.class);

    public static final String DOC_PROP_TURNIERLOGO_URL = "Turnierlogo Url";
    public static final String DOC_PROP_TURNIERNAME     = "Turniername";

    private static final int DIALOG_BREITE = 320;
    private static final int DIALOG_HOEHE  = 150;

    private static final int LBL_X     = 5;
    private static final int LBL_W     = 80;
    private static final int FIELD_X   = 90;
    private static final int FIELD_W   = 220;
    private static final int CTRL_H    = 12;
    private static final int ZEILE1_Y  = 10;
    private static final int ZEILE2_Y  = 30;
    private static final int ZEILE3_Y  = 55;
    private static final int ZEILE4_Y  = 80;
    private static final int FOOTER_Y  = 125;
    private static final int BTN_OK_X      = 160;
    private static final int BTN_ABBRUCH_X = 235;
    private static final int BTN_ACTION_W  = 70;
    private static final int BTN_H         = 14;

    private static final String CTRL_AKTIV       = "cbAktiv";
    private static final String CTRL_PORT        = "editPort";
    private static final String CTRL_LOGO        = "editLogo";
    private static final String CTRL_TURNIERNAME = "editTurniername";

    private final WorkingSpreadsheet currentSpreadsheet;
    private XMultiServiceFactory xMSF;
    private XNameContainer cont;
    private XControlContainer xcc;

    public TurnierStartseiteDialog(WorkingSpreadsheet currentSpreadsheet) {
        super(currentSpreadsheet.getxContext());
        this.currentSpreadsheet = currentSpreadsheet;
    }

    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override
    protected String getTitel() {
        return I18n.get("konfiguration.startseite.dialog.titel");
    }

    @Override
    protected int getBreite() {
        return DIALOG_BREITE;
    }

    @Override
    protected int getHoehe() {
        return DIALOG_HOEHE;
    }

    @Override
    protected void erstelleFelder(
            XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog
    ) throws com.sun.star.uno.Exception {
        this.xMSF = xMSF;
        this.cont = cont;
        this.xcc  = Lo.qi(XControlContainer.class, xDialog);

        var gp = GlobalProperties.get();
        var docProps = new DocumentPropertiesHelper(currentSpreadsheet);

        fuegeCheckBox(CTRL_AKTIV, I18n.get("konfiguration.startseite.aktiv.label"),
                LBL_X, ZEILE1_Y, LBL_W + FIELD_W, CTRL_H, gp.isStartseiteAktiv());

        fuegeLabel("lblPort", I18n.get("konfiguration.startseite.port.label"),
                LBL_X, ZEILE2_Y, LBL_W, CTRL_H);
        fuegeEdit(CTRL_PORT, String.valueOf(gp.getStartseitePort()),
                FIELD_X, ZEILE2_Y, 50, CTRL_H);

        fuegeLabel("lblLogo", I18n.get("konfiguration.startseite.logo.label"),
                LBL_X, ZEILE3_Y, LBL_W, CTRL_H);
        fuegeEdit(CTRL_LOGO, docProps.getStringProperty(DOC_PROP_TURNIERLOGO_URL, ""),
                FIELD_X, ZEILE3_Y, FIELD_W, CTRL_H);

        fuegeLabel("lblTurniername", I18n.get("konfiguration.startseite.turniername.label"),
                LBL_X, ZEILE4_Y, LBL_W, CTRL_H);
        fuegeEdit(CTRL_TURNIERNAME, docProps.getStringProperty(DOC_PROP_TURNIERNAME, ""),
                FIELD_X, ZEILE4_Y, FIELD_W, CTRL_H);

        fuegeButton("btnOk", I18n.get("dialog.ok"),
                BTN_OK_X, FOOTER_Y, BTN_ACTION_W, BTN_H, (short) PushButtonType.OK_value);
        fuegeButton("btnAbbrechen", I18n.get("dialog.abbrechen"),
                BTN_ABBRUCH_X, FOOTER_Y, BTN_ACTION_W, BTN_H, (short) PushButtonType.CANCEL_value);
    }

    @Override
    protected void beiOkGeklickt() {
        boolean aktiv = leseCheckBox();
        String portText = leseFeld(CTRL_PORT);
        String logo = leseFeld(CTRL_LOGO);
        String turniername = leseFeld(CTRL_TURNIERNAME);

        int port;
        try {
            port = Integer.parseInt(portText.trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port außerhalb des Bereichs");
            }
        } catch (NumberFormatException e) {
            zeigeFehler(I18n.get("konfiguration.startseite.fehler.port"));
            return;
        }

        GlobalProperties.get().speichernStartseite(port, aktiv);
        var docProps = new DocumentPropertiesHelper(currentSpreadsheet);
        docProps.setStringProperty(DOC_PROP_TURNIERLOGO_URL, logo);
        docProps.setStringProperty(DOC_PROP_TURNIERNAME, turniername);
        WebServerManager.get().konfigurationGeaendert();
        logger.info("Turnier-Startseite gespeichert: aktiv={}, Port={}", aktiv, port);
    }

    // ── UNO-Helper ────────────────────────────────────────────────────────────

    private void fuegeLabel(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",     text);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        cont.insertByName(name, model);
    }

    private void fuegeEdit(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Text",      text != null ? text : "");
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        props.setPropertyValue("MultiLine", Boolean.FALSE);
        cont.insertByName(name, model);
    }

    private void fuegeCheckBox(String name, String label, int x, int y, int w, int h, boolean checked)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlCheckBoxModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",     label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        props.setPropertyValue("State",     (short) (checked ? 1 : 0));
        cont.insertByName(name, model);
    }

    private void fuegeButton(String name, String label, int x, int y, int w, int h, short typ)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",          label);
        props.setPropertyValue("PositionX",      x);
        props.setPropertyValue("PositionY",      y);
        props.setPropertyValue("Width",          w);
        props.setPropertyValue("Height",         h);
        props.setPropertyValue("PushButtonType", typ);
        cont.insertByName(name, model);
    }

    private boolean leseCheckBox() {
        XControl ctrl = xcc.getControl(CTRL_AKTIV);
        if (ctrl == null) return false;
        var cb = Lo.qi(XCheckBox.class, ctrl);
        return cb != null && cb.getState() == 1;
    }

    private String leseFeld(String name) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) return "";
        var tc = Lo.qi(XTextComponent.class, ctrl);
        return tc != null ? tc.getText().trim() : "";
    }

    private void zeigeFehler(String meldung) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("konfiguration.startseite.dialog.titel"))
                .message(meldung)
                .show();
    }
}
