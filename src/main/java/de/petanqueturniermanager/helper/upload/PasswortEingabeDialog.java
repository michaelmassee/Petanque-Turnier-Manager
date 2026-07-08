/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Modaler Dialog zur einmaligen Eingabe des FTP/SFTP-Passworts für den laufenden Upload
 * (Server-Name, Passwortfeld mit maskierten Zeichen). Das Passwort wird nur für diesen
 * Upload-Lauf verwendet, nicht dauerhaft gespeichert — dauerhafte Zugangsdaten werden auf
 * der Options-Seite „FTP-Server" gepflegt ({@link de.petanqueturniermanager.comp.GlobalProperties.FtpServerEintrag}).
 *
 * <p>Kann aus einem Worker-Thread aufgerufen werden — der Dialog wird via
 * {@link LoMainThread#post} auf den LO-Main-Thread marshalled und blockiert
 * den aufrufenden Thread bis der Benutzer den Dialog schließt.
 */
final class PasswortEingabeDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(PasswortEingabeDialog.class);

    private final String host;
    private final WorkingSpreadsheet ws;

    @Nullable private XControlContainer xcc;
    @Nullable private XDialog xDialog;
    @Nullable private String passwort;

    private PasswortEingabeDialog(XComponentContext xContext, String host, WorkingSpreadsheet ws) {
        super(xContext);
        this.host = host;
        this.ws = ws;
    }

    /**
     * Zeigt den Passwort-Dialog und blockiert bis der Benutzer antwortet.
     *
     * @return eingegebenes Passwort, oder {@link Optional#empty()} bei Abbruch
     */
    static Optional<String> zeigen(WorkingSpreadsheet ws, String host) throws GenerateException {
        var future = new CompletableFuture<Optional<String>>();
        LoMainThread.post(ws.getxContext(), () -> {
            try {
                var dialog = new PasswortEingabeDialog(ws.getxContext(), host, ws);
                dialog.erstelleUndAusfuehren();
                Optional<String> ergebnis = dialog.passwort != null && !dialog.passwort.isBlank()
                        ? Optional.of(dialog.passwort)
                        : Optional.empty();
                future.complete(ergebnis);
            } catch (Exception e) {
                logger.error("Fehler im Passwort-Dialog", e);
                future.completeExceptionally(e);
            }
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new GenerateException(cause != null ? cause.getMessage() : e.getMessage());
        }
    }

    @Override
    protected String getTitel() {
        return I18n.get("upload.passwort.dialog.titel");
    }

    @Override
    protected int getBreite() {
        return 240;
    }

    @Override
    protected int getHoehe() {
        return 80;
    }

    @Override
    protected XWindowPeer holeParentPeer() {
        return ws.getContainerWindowPeer();
    }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog dialog) throws com.sun.star.uno.Exception {
        this.xDialog = dialog;
        this.xcc = Lo.qi(XControlContainer.class, dialog);

        label(xMSF, cont, "lblServer",    I18n.get("upload.passwort.dialog.server", host), 8, 8,  224, 10);
        label(xMSF, cont, "lblPasswort",  I18n.get("upload.passwort.dialog.label"),         8, 26,  60, 10);
        passwortFeld(xMSF, cont, "txtPasswort",  75, 24, 155, 12);
        button(xMSF, cont, "btnOk",       I18n.get("upload.passwort.dialog.ok"),            95, 50,  55, 14, (short) PushButtonType.STANDARD_value);
        button(xMSF, cont, "btnAbbrechen",I18n.get("upload.passwort.dialog.abbrechen"),    155, 50,  80, 14, (short) PushButtonType.CANCEL_value);

        var okCtrl = xcc != null ? xcc.getControl("btnOk") : null;
        if (okCtrl != null) {
            var btn = Lo.qi(XButton.class, okCtrl);
            if (btn != null) {
                btn.addActionListener(new XActionListener() {
                    @Override public void actionPerformed(ActionEvent e) { beimOkGeklickt(); }
                    @Override public void disposing(EventObject e) { /* leer */ }
                });
            }
        }
    }

    private void beimOkGeklickt() {
        if (xcc == null || xDialog == null) {
            return;
        }
        var passwortCtrl = xcc.getControl("txtPasswort");
        if (passwortCtrl != null) {
            var tc = Lo.qi(XTextComponent.class, passwortCtrl);
            passwort = tc != null ? tc.getText() : "";
        }
        xDialog.endExecute();
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

    private static void passwortFeld(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Text", "");
        props.setPropertyValue("MultiLine", Boolean.FALSE);
        props.setPropertyValue("EchoChar", (short) '*');
        cont.insertByName(name, model);
    }

    private static void button(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String labelText, int x, int y, int w, int h, short typ)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", labelText);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("PushButtonType", typ);
        cont.insertByName(name, model);
    }
}
