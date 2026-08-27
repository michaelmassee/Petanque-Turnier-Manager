/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XEventListener;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.ptmonline.PtmOnlineConfig;

/**
 * Schlanker, eigenstaendiger Dialog fuer Base-URL und API-Key von PTM-Online. Bewusst
 * kein {@code BasePropertiesDialog} (das generiert Dialoge aus {@code ConfigProperty}-Listen,
 * die in der {@code .ods}-Datei landen) — der API-Key darf nicht mit der Turnierdatei geteilt
 * werden und liegt daher separat in {@link PtmOnlineConfig}.
 */
public final class PtmOnlineConfigDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(PtmOnlineConfigDialog.class);

    private static final int DIALOG_BREITE = 220;
    private static final int DIALOG_HOEHE = 90;
    private static final int LABEL_X = 8;
    private static final int LABEL_W = 55;
    private static final int FELD_X = 66;
    private static final int FELD_W = 146;
    private static final int APIKEY_FELD_W = 100;
    private static final int ZEILE_H = 16;

    private final PtmOnlineConfig config;
    @Nullable private final XWindowPeer parentPeer;

    @Nullable private XControlContainer xcc;
    @Nullable private XDialog xDialog;
    private boolean gespeichert;

    public PtmOnlineConfigDialog(XComponentContext xContext, PtmOnlineConfig config, @Nullable XWindowPeer parentPeer) {
        super(xContext);
        this.config = config;
        this.parentPeer = parentPeer;
    }

    @Override
    protected XWindowPeer holeParentPeer() {
        return parentPeer;
    }

    /**
     * Zeigt den Dialog. Liefert {@code true} wenn die Eingaben erfolgreich gespeichert wurden.
     */
    public boolean zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
        return gespeichert;
    }

    @Override
    protected String getTitel() {
        return I18n.get("ptmonline.config.dialog.titel");
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
            XPropertySet dlgProps, XDialog dialog) throws com.sun.star.uno.Exception {
        this.xDialog = dialog;
        this.xcc = Lo.qi(XControlContainer.class, dialog);
        Lo.qi(com.sun.star.lang.XComponent.class, dialog).addEventListener(new XEventListener() {
            @Override
            public void disposing(EventObject e) {
                // nichts zu tun – Dialog ist kurzlebig, kein Hintergrund-Worker offen
            }
        });

        int y = 8;
        label(xMSF, cont, "lblBaseUrl", I18n.get("ptmonline.config.label.baseurl"), LABEL_X, y, LABEL_W, 10);
        textFeld(xMSF, cont, "txtBaseUrl", config.getBaseUrl(), FELD_X, y - 2, FELD_W, 12, false);

        y += ZEILE_H;
        label(xMSF, cont, "lblApiKey", I18n.get("ptmonline.config.label.apikey"), LABEL_X, y, LABEL_W, 10);
        textFeld(xMSF, cont, "txtApiKey", config.getApiKey(), FELD_X, y - 2, APIKEY_FELD_W, 12, true);
        button(xMSF, cont, "btnApiKeyAnzeigen", I18n.get("ptmonline.config.label.apikey.anzeigen"),
                FELD_X + APIKEY_FELD_W + 4, y - 2, FELD_W - APIKEY_FELD_W - 4, 12,
                (short) PushButtonType.STANDARD_value);

        y += ZEILE_H + 8;
        button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), FELD_X + FELD_W - 135, y, 55, ZEILE_H,
                (short) PushButtonType.STANDARD_value);
        button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), FELD_X + FELD_W - 75, y, 75, ZEILE_H,
                (short) PushButtonType.CANCEL_value);

        registriereKlick(xcc, "btnOk", this::beimOkGeklickt);
        registriereKlick(xcc, "btnApiKeyAnzeigen", () -> beimApiKeyAnzeigenGeklickt(xcc));
    }

    private void beimOkGeklickt() {
        if (xDialog == null || xcc == null) {
            return;
        }
        String baseUrl = text(xcc, "txtBaseUrl");
        String apiKey = text(xcc, "txtApiKey");
        if (baseUrl.isBlank()) {
            zeigeFehler(I18n.get("ptmonline.config.fehler.baseurl_leer"));
            return;
        }
        try {
            config.save(baseUrl, apiKey);
            gespeichert = true;
            xDialog.endExecute();
        } catch (IOException e) {
            logger.error("PTM-Online Konfiguration konnte nicht gespeichert werden", e);
            zeigeFehler(I18n.get("ptmonline.config.fehler.speichern", e.getMessage() == null ? "" : e.getMessage()));
        }
    }

    /**
     * Schaltet bei jedem Klick die Maskierung (EchoChar) des API-Key-Felds um. Vorbild:
     * {@code FtpServerDetailDialog.beimPasswortAnzeigenGeklickt} — gleicher vcl-Repaint-Workaround.
     */
    private static void beimApiKeyAnzeigenGeklickt(XControlContainer xcc) {
        var feldCtrl = xcc.getControl("txtApiKey");
        var buttonCtrl = xcc.getControl("btnApiKeyAnzeigen");
        if (feldCtrl == null || buttonCtrl == null) {
            return;
        }
        var feldProps = Lo.qi(XPropertySet.class, feldCtrl.getModel());
        var buttonProps = Lo.qi(XPropertySet.class, buttonCtrl.getModel());
        if (feldProps == null || buttonProps == null) {
            return;
        }
        try {
            short aktuellerEchoChar = (short) feldProps.getPropertyValue("EchoChar");
            boolean sichtbar = aktuellerEchoChar == 0;
            feldProps.setPropertyValue("EchoChar", sichtbar ? (short) '*' : (short) 0);
            erzwingeRepaint(feldCtrl);
            buttonProps.setPropertyValue("Label", I18n.get(
                    sichtbar ? "ptmonline.config.label.apikey.anzeigen" : "ptmonline.config.label.apikey.verbergen"));
        } catch (Exception e) {
            // Anzeige-Umschaltung ist rein kosmetisch, Fehler ignorieren
        }
    }

    private static void erzwingeRepaint(XControl ctrl) {
        var fenster = Lo.qi(XWindow.class, ctrl.getPeer());
        if (fenster == null) {
            return;
        }
        fenster.setVisible(false);
        fenster.setVisible(true);
    }

    private static void registriereKlick(XControlContainer xcc, String name, Runnable aktion) {
        var ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return;
        }
        var btn = Lo.qi(XButton.class, ctrl);
        if (btn == null) {
            return;
        }
        btn.addActionListener(new XActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aktion.run();
            }

            @Override
            public void disposing(EventObject e) {
                // nichts zu tun
            }
        });
    }

    private void zeigeFehler(String meldung) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }

    // ---- UNO-Control-Hilfsmethoden (identisch zu FtpServerDetailDialog) ----

    private static String text(XControlContainer xcc, String name) {
        var ctrl = xcc.getControl(name);
        if (ctrl == null) {
            return "";
        }
        var tc = Lo.qi(XTextComponent.class, ctrl);
        return tc == null ? "" : tc.getText().trim();
    }

    private static void label(XMultiServiceFactory xMSF, XNameContainer cont,
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

    private static void textFeld(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String text, int x, int y, int w, int h, boolean maskiert)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Text", text);
        props.setPropertyValue("MultiLine", Boolean.FALSE);
        if (maskiert) {
            props.setPropertyValue("EchoChar", (short) '*');
        }
        cont.insertByName(name, model);
    }

    private static void button(XMultiServiceFactory xMSF, XNameContainer cont,
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
