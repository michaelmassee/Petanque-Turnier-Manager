/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Modaler Dialog, der beim Startup automatisch angezeigt wird, wenn eine neue
 * Plugin-Version verfügbar ist. Zeigt Logo, Versionsvergleich und Release-Notes,
 * mit den Aktionen "Update", "Abbruch" und "Für diese Version nicht mehr nachfragen".
 *
 * <p>Muss auf dem LO-Main-Thread aufgerufen werden (VCL/UNO-UI-Regel, siehe CLAUDE.md) –
 * Aufrufer ist {@link AutoUpdateStartupChecker#zeigeDialogAufMainThread()}, per
 * {@code LoMainThread.post} marshallt.
 */
final class UpdateVerfuegbarDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(UpdateVerfuegbarDialog.class);

    private static final String LOGO_DATEINAME = "petanqueturniermanager-logo-256px.png";

    enum Aktion {
        UPDATE, ABBRUCH, NICHT_MEHR_FUER_VERSION
    }

    private final ReleaseInfo release;
    private final String installierteVersion;
    private final XWindowPeer parentPeer;

    private @Nullable XDialog xDialog;
    private Aktion aktion = Aktion.ABBRUCH;

    private UpdateVerfuegbarDialog(XComponentContext xContext, ReleaseInfo release,
            String installierteVersion, XWindowPeer parentPeer) {
        super(xContext);
        this.release = release;
        this.installierteVersion = installierteVersion;
        this.parentPeer = parentPeer;
    }

    /**
     * Zeigt den Dialog und blockiert bis der Benutzer eine der drei Aktionen wählt.
     */
    static Aktion zeigen(XComponentContext xContext, ReleaseInfo release,
            String installierteVersion, XWindowPeer parentPeer) throws GenerateException {
        try {
            var dialog = new UpdateVerfuegbarDialog(xContext, release, installierteVersion, parentPeer);
            dialog.erstelleUndAusfuehren();
            return dialog.aktion;
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler im Update-Verfügbar-Dialog", e);
            throw new GenerateException(e.getMessage());
        }
    }

    @Override
    protected String getTitel() {
        return I18n.get("dialog.title.update.verfuegbar");
    }

    @Override
    protected int getBreite() {
        return 240;
    }

    @Override
    protected int getHoehe() {
        return 170;
    }

    @Override
    protected XWindowPeer holeParentPeer() {
        return parentPeer;
    }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog dialog) throws com.sun.star.uno.Exception {
        this.xDialog = dialog;
        XControlContainer xcc = Lo.qi(XControlContainer.class, dialog);

        bild(xMSF, cont, "imgLogo", ExtensionsHelper.from(xContext).getImageUrlDir() + LOGO_DATEINAME,
                8, 8, 32, 32);

        label(xMSF, cont, "lblHinweis", I18n.get("dialog.update.hinweis"), 48, 8, 184, 20);
        label(xMSF, cont, "lblVersionen",
                I18n.get("dialog.update.versionen", installierteVersion, release.tagName()),
                48, 30, 184, 12);

        textBereich(xMSF, cont, "txtReleaseNotes", releaseNotesText(), 8, 46, 224, 88);

        button(xMSF, cont, "btnUpdate", I18n.get("dialog.update.button.update"),
                8, 142, 70, 20, xcc, Aktion.UPDATE);
        button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"),
                85, 142, 70, 20, xcc, Aktion.ABBRUCH);
        button(xMSF, cont, "btnNichtMehrFuerVersion", I18n.get("dialog.update.button.nicht.mehr.fuer.version"),
                162, 142, 70, 20, xcc, Aktion.NICHT_MEHR_FUER_VERSION);
    }

    private String releaseNotesText() {
        String body = release.body();
        return body == null ? "" : body;
    }

    private void beiButtonGeklickt(Aktion gewaehlt) {
        this.aktion = gewaehlt;
        if (xDialog != null) {
            xDialog.endExecute();
        }
    }

    private void bild(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String imageUrl, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlImageControlModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("ImageURL", imageUrl);
        props.setPropertyValue("ScaleImage", Boolean.TRUE);
        props.setPropertyValue("Border", (short) 0);
        cont.insertByName(name, model);
    }

    private void label(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String text, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", text);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("MultiLine", Boolean.TRUE);
        cont.insertByName(name, model);
    }

    private void textBereich(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String text, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("MultiLine", Boolean.TRUE);
        props.setPropertyValue("ReadOnly", Boolean.TRUE);
        props.setPropertyValue("VScroll", Boolean.TRUE);
        props.setPropertyValue("Text", text);
        cont.insertByName(name, model);
    }

    private void button(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h,
            XControlContainer xcc, Aktion aktionBeiKlick) throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("PushButtonType", (short) PushButtonType.STANDARD_value);
        cont.insertByName(name, model);

        var ctrl = xcc.getControl(name);
        if (ctrl != null) {
            var btn = Lo.qi(XButton.class, ctrl);
            if (btn != null) {
                btn.addActionListener(new XActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        beiButtonGeklickt(aktionBeiKlick);
                    }

                    @Override
                    public void disposing(EventObject e) {
                        // nichts zu tun
                    }
                });
            }
        }
    }
}
