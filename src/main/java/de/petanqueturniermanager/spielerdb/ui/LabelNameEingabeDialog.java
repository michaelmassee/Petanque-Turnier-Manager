package de.petanqueturniermanager.spielerdb.ui;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Schmaler Eingabe-Dialog für einen einzelnen Labelnamen.
 * Wird sowohl beim Anlegen als auch beim Umbenennen verwendet.
 */
final class LabelNameEingabeDialog extends AbstractUnoDialog {

    @Nullable private final String vorbelegt;
    @Nullable private String ergebnis;
    @Nullable private UnoControlsHelper controls;
    @Nullable private XDialog xDialog;

    LabelNameEingabeDialog(XComponentContext xContext, @Nullable String vorbelegt) {
        super(xContext);
        this.vorbelegt = vorbelegt;
    }

    Optional<String> zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
        return Optional.ofNullable(ergebnis);
    }

    @Override protected String getTitel() {
        return I18n.get(vorbelegt == null
                ? "spielerdb.label.titel.neu" : "spielerdb.label.titel.bearbeiten");
    }
    @Override protected int getBreite() { return 220; }
    @Override protected int getHoehe() { return 70; }

    @Override
    protected void erstelleFelder(XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog) throws com.sun.star.uno.Exception {

        this.xDialog = xDialog;
        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        this.controls = new UnoControlsHelper(xMSF, cont, xcc);

        controls.fixedText("lblName", I18n.get("spielerdb.label.name"), 8, 10, 60, 10);
        controls.edit("txtName", vorbelegt == null ? "" : vorbelegt, 75, 8, 135, 12);

        controls.button("btnOk", I18n.get("spielerdb.erfassen.btn.ok"),
                90, 45, 55, 14, (short) PushButtonType.STANDARD_value);
        controls.button("btnAbbrechen", I18n.get("spielerdb.erfassen.btn.abbrechen"),
                150, 45, 60, 14, (short) PushButtonType.CANCEL_value);
        controls.registriereActionListener("btnOk", this::beimOk);
    }

    private void beimOk() {
        UnoControlsHelper c = this.controls;
        XDialog dlg = this.xDialog;
        if (c == null || dlg == null) {
            return;
        }
        ergebnis = c.leseText("txtName").strip();
        dlg.endExecute();
    }
}
