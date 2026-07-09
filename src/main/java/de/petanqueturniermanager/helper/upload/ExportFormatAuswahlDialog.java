/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
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
 * Modaler Dialog zur Auswahl des Export-Formats vor dem Verzeichnis-Export.
 * Vorselektiert das zuletzt für dieses Dokument verwendete Format.
 *
 * <p>Kann aus einem Worker-Thread aufgerufen werden — der Dialog wird via
 * {@link LoMainThread#post} auf den LO-Main-Thread marshalled und blockiert
 * den aufrufenden Thread bis der Benutzer den Dialog schließt.
 */
final class ExportFormatAuswahlDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(ExportFormatAuswahlDialog.class);

    private static final ExportFormat[] FORMATE = ExportFormat.values();

    private final ExportFormat letztesFormat;
    private final WorkingSpreadsheet ws;

    private XControlContainer xcc;
    private XDialog xDialog;
    private ExportFormat ausgewaehlt;

    private ExportFormatAuswahlDialog(XComponentContext xContext, ExportFormat letztesFormat, WorkingSpreadsheet ws) {
        super(xContext);
        this.letztesFormat = letztesFormat;
        this.ws = ws;
    }

    /**
     * Zeigt den Auswahl-Dialog und blockiert bis der Benutzer antwortet.
     *
     * @return gewähltes Format, oder {@link Optional#empty()} bei Abbruch
     */
    static Optional<ExportFormat> zeigen(WorkingSpreadsheet ws, ExportFormat letztesFormat) throws GenerateException {
        var future = new CompletableFuture<Optional<ExportFormat>>();
        LoMainThread.post(ws.getxContext(), () -> {
            try {
                var dialog = new ExportFormatAuswahlDialog(ws.getxContext(), letztesFormat, ws);
                dialog.erstelleUndAusfuehren();
                future.complete(Optional.ofNullable(dialog.ausgewaehlt));
            } catch (Exception e) {
                logger.error("Fehler im Export-Format-Auswahl-Dialog", e);
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
        return I18n.get("export.format.dialog.titel");
    }

    @Override
    protected int getBreite() {
        return 220;
    }

    @Override
    protected int getHoehe() {
        return 90;
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

        UnoAuswahlDialogControls.label(xMSF, cont, "lblFormat", I18n.get("export.format.dialog.label"), 8, 8, 204, 10);

        String[] items = new String[FORMATE.length];
        int vorauswahl = 0;
        for (int i = 0; i < FORMATE.length; i++) {
            items[i] = FORMATE[i].anzeigeName();
            if (FORMATE[i] == letztesFormat) {
                vorauswahl = i;
            }
        }
        UnoAuswahlDialogControls.listBox(xMSF, cont, "lstFormat", items, vorauswahl, 8, 20, 204, 45);

        UnoAuswahlDialogControls.button(xMSF, cont, "btnOk", I18n.get("dialog.ok"), 40, 70, 55, 14,
                (short) PushButtonType.STANDARD_value);
        UnoAuswahlDialogControls.button(xMSF, cont, "btnAbbrechen", I18n.get("dialog.abbrechen"), 105, 70, 75, 14,
                (short) PushButtonType.CANCEL_value);

        var okCtrl = xcc.getControl("btnOk");
        if (okCtrl != null) {
            var btn = Lo.qi(XButton.class, okCtrl);
            if (btn != null) {
                btn.addActionListener(new XActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        beimOkGeklickt();
                    }

                    @Override
                    public void disposing(EventObject e) {
                        // nichts zu tun
                    }
                });
            }
        }
    }

    private void beimOkGeklickt() {
        if (xcc == null || xDialog == null) {
            return;
        }
        var listCtrl = xcc.getControl("lstFormat");
        if (listCtrl != null) {
            XListBox listBox = Lo.qi(XListBox.class, listCtrl);
            short pos = listBox == null ? -1 : listBox.getSelectedItemPos();
            if (pos >= 0 && pos < FORMATE.length) {
                ausgewaehlt = FORMATE[pos];
            }
        }
        xDialog.endExecute();
    }
}
