/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import com.sun.star.awt.PushButtonType;
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
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

public final class KiNeuesTurnierDialog extends AbstractUnoDialog {

    private static final String CTL_WUNSCH = "KiTurnierWunsch";
    private final WorkingSpreadsheet ws;
    private XTextComponent wunschText;

    public KiNeuesTurnierDialog(WorkingSpreadsheet ws) {
        super(ws.getxContext());
        this.ws = ws;
    }

    public void zeige() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override
    protected XWindowPeer holeParentPeer() {
        return ws.getContainerWindowPeer();
    }

    @Override
    protected String getTitel() {
        return I18n.get("ki.dialog.titel");
    }

    @Override
    protected int getBreite() {
        return 250;
    }

    @Override
    protected int getHoehe() {
        return 155;
    }

    @Override
    protected boolean istVeraenderbar() {
        return true;
    }

    @Override
    protected void erstelleFelder(
            XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog) throws com.sun.star.uno.Exception {

        fuegeFixedTextEin(xMSF, cont, "KiHinweis", I18n.get("ki.dialog.hinweis"), 6, 6, 235, 18);
        fuegeTextAreaEin(xMSF, cont, CTL_WUNSCH, 6, 28, 235, 92);
        fuegeButtonEin(xMSF, cont, "btnOk", I18n.get("ki.dialog.planen"), 105, 132, 60, 14,
                (short) PushButtonType.OK_value);
        fuegeButtonEin(xMSF, cont, "btnAbbrechen", I18n.get("toolbar.start.dialog.abbrechen"), 172, 132, 68, 14,
                (short) PushButtonType.CANCEL_value);

        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        XControl ctrl = xcc.getControl(CTL_WUNSCH);
        wunschText = ctrl == null ? null : Lo.qi(XTextComponent.class, ctrl);
    }

    @Override
    protected void beiOkGeklickt() {
        String wunsch = wunschText == null ? "" : wunschText.getText();
        if (wunsch.isBlank()) {
            MessageBox.from(ws, MessageBoxTypeEnum.WARN_OK)
                    .caption(I18n.get("ki.dialog.titel"))
                    .message(I18n.get("ki.dialog.leerer.wunsch"))
                    .show();
            return;
        }
        KiOptionen optionen = GlobalProperties.get().getKiOptionen();
        if (optionen.apiKey().isBlank()) {
            MessageBox.from(ws, MessageBoxTypeEnum.WARN_OK)
                    .caption(I18n.get("ki.dialog.titel"))
                    .message(I18n.get("ki.dialog.api.key.fehlt"))
                    .show();
            return;
        }
        Thread worker = new Thread(() -> planeUndPoste(optionen, wunsch), "PTM-KI-NeuesTurnier");
        worker.start();
    }

    private void planeUndPoste(KiOptionen optionen, String wunsch) {
        try {
            KiPlan plan = new KiAssistentService(optionen).planeNeuesTurnier(ws, wunsch);
            LoMainThread.post(ws.getxContext(), () -> KiActionRegistry.ausfuehrenNachBestaetigung(ws, plan));
        } catch (Exception e) {
            LoMainThread.post(ws.getxContext(), () -> MessageBox.from(ws, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("ki.dialog.titel"))
                    .message(I18n.get("ki.dialog.fehler", e.getMessage()))
                    .show());
        }
    }

    private static void fuegeFixedTextEin(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", Integer.valueOf(x));
        props.setPropertyValue("PositionY", Integer.valueOf(y));
        props.setPropertyValue("Width", Integer.valueOf(w));
        props.setPropertyValue("Height", Integer.valueOf(h));
        props.setPropertyValue("MultiLine", Boolean.TRUE);
        cont.insertByName(name, model);
    }

    private static void fuegeTextAreaEin(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", Integer.valueOf(x));
        props.setPropertyValue("PositionY", Integer.valueOf(y));
        props.setPropertyValue("Width", Integer.valueOf(w));
        props.setPropertyValue("Height", Integer.valueOf(h));
        props.setPropertyValue("MultiLine", Boolean.TRUE);
        props.setPropertyValue("VScroll", Boolean.TRUE);
        cont.insertByName(name, model);
    }

    private static void fuegeButtonEin(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String label, int x, int y, int w, int h, short pushButtonType)
            throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label", label);
        props.setPropertyValue("PositionX", Integer.valueOf(x));
        props.setPropertyValue("PositionY", Integer.valueOf(y));
        props.setPropertyValue("Width", Integer.valueOf(w));
        props.setPropertyValue("Height", Integer.valueOf(h));
        props.setPropertyValue("PushButtonType", Short.valueOf(pushButtonType));
        cont.insertByName(name, model);
    }
}
