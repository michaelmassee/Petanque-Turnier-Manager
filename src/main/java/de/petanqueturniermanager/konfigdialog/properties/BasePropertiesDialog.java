/**
 * Erstellung 07.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.AbstractWindowListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.config.AddConfigElementsToWindow;
import de.petanqueturniermanager.sidebar.layout.Layout;
import de.petanqueturniermanager.sidebar.layout.VerticalLayout;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
abstract class BasePropertiesDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(BasePropertiesDialog.class);

    private static final int DIALOG_MIN_HEIGHT = 30;
    private static final int DIALOG_MAX_HEIGHT = 600;
    private static final int DIALOG_WIDTH = 200;
    private static final int BORDER = 5;

    WorkingSpreadsheet currentSpreadsheet;
    Layout layout;

    public BasePropertiesDialog(WorkingSpreadsheet currentSpreadsheet) {
        super(checkNotNull(currentSpreadsheet).getxContext());
        this.currentSpreadsheet = currentSpreadsheet;
    }

    /**
     * Prüft das Turniersystem, blendet die ProcessBox aus und zeigt den Dialog.
     */
    public final void createDialog() throws com.sun.star.uno.Exception {
        TurnierSystem turnierSystemAusDocument = new DocumentPropertiesHelper(currentSpreadsheet)
                .getTurnierSystemAusDocument();
        if (turnierSystemAusDocument == TurnierSystem.KEIN) {
            MessageBox.from(currentSpreadsheet.getxContext(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.kein.turnier"))
                    .message(I18n.get("msg.text.kein.turnier")).show();
            return;
        }

        ProcessBox.from().hide(); // sonst überlappt
        erstelleUndAusfuehren();
    }

    @Override
    protected int getBreite() {
        return DIALOG_WIDTH;
    }

    @Override
    protected int getHoehe() {
        return DIALOG_MIN_HEIGHT;
    }

    @Override
    protected boolean istVeraenderbar() {
        return true;
    }

    @Override
    protected String getTitel() {
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(currentSpreadsheet)
                    .getTurnierSystemAusDocument();
            return ts.getBezeichnung() + "  " + getTitle();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return getTitle();
        }
    }

    @Override
    protected final void erstelleFelder(
            XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog
    ) throws com.sun.star.uno.Exception {

        // Window-Adapter für Resize (xDialog ist dasselbe UNO-Objekt wie xControl)
        XWindow xWindow = de.petanqueturniermanager.helper.Lo.qi(XWindow.class, xDialog);
        if (xWindow != null) {
            xWindow.addWindowListener(windowAdapter);
        }

        int margin = 2;
        layout = new VerticalLayout(0, margin);

        // Felder hinzufügen
        GuiFactoryCreateParam guiFactoryCreateParam = new GuiFactoryCreateParam(
                mcf, xContext, xToolkit, peer);
        List<ConfigProperty<?>> konfigProperties =
                KonfigurationSingleton.getKonfigProperties(currentSpreadsheet);

        AddConfigElementsToWindow addConfigElementsToWindow =
                new AddConfigElementsToWindow(guiFactoryCreateParam, currentSpreadsheet, layout);
        AtomicInteger anzElementen = new AtomicInteger(0);
        if (konfigProperties != null) {
            konfigProperties.stream().filter(getKonfigFieldFilter()).forEach(konfigprop -> {
                addConfigElementsToWindow.addPropToPanel(konfigprop);
                anzElementen.addAndGet(1);
            });
        }

        // Höhe anpassen
        int dialogHeight = Math.min(Math.max(layout.getHeight() / 2, DIALOG_MIN_HEIGHT), DIALOG_MAX_HEIGHT);
        try {
            dlgProps.setPropertyValue("Height", Integer.valueOf(dialogHeight));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * @return Titel-Suffix (ohne TurnierSystem-Bezeichnung)
     */
    protected abstract String getTitle();

    protected final void doLayout(WindowEvent windowEvent) {
        try {
            if (layout != null) {
                Rectangle posSizeParent = new Rectangle(BORDER, BORDER,
                        windowEvent.Width - (BORDER * 2),
                        windowEvent.Height - (BORDER * 2));
                layout.layout(posSizeParent);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private final AbstractWindowListener windowAdapter = new AbstractWindowListener() {
        @Override
        public void windowResized(WindowEvent windowEvent) {
            doLayout(windowEvent);
        }

        @Override
        public void disposing(EventObject event) {
            currentSpreadsheet = null;
            layout = null;
        }
    };

    protected abstract java.util.function.Predicate<ConfigProperty<?>> getKonfigFieldFilter();
}
