/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetNew;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetNew;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Modaler Dialog zur Auswahl des Turniersystems beim Start eines neuen Turniers.
 * Zeigt alle verfügbaren Turniersysteme in einer ListBox an und startet
 * nach Bestätigung das gewählte System.
 */
public class TurnierSystemAuswahlDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(TurnierSystemAuswahlDialog.class);

    private static final String CTL_LISTBOX       = "lbTurnierSystem";
    private static final String CTL_BTN_OK        = "btnOk";
    private static final String CTL_BTN_ABBRECHEN = "btnAbbrechen";

    protected static final TurnierSystem[] AUSWAHL_SYSTEME = {
        TurnierSystem.SUPERMELEE,
        TurnierSystem.SCHWEIZER,
        TurnierSystem.MAASTRICHTER,
        TurnierSystem.POULE,
        TurnierSystem.LIGA,
        TurnierSystem.JGJ,
        TurnierSystem.KO,
        TurnierSystem.KASKADE,
        TurnierSystem.FORMULEX
    };

    private WorkingSpreadsheet ws;
    protected XListBox listBox;

    public TurnierSystemAuswahlDialog(WorkingSpreadsheet ws) {
        super(ws.getxContext());
        this.ws = ws;
    }

    /**
     * Konstruktor für Unterklassen, die kein bestehendes Dokument benötigen.
     * {@code ws} bleibt {@code null} – {@link #beiOkGeklickt()} muss in der Unterklasse
     * vollständig überschrieben werden.
     */
    protected TurnierSystemAuswahlDialog(XComponentContext xContext) {
        super(xContext);
        this.ws = null;
    }

    public void zeige() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override
    protected String getTitel() {
        return I18n.get("toolbar.start.dialog.titel");
    }

    @Override
    protected int getBreite() {
        return 160;
    }

    @Override
    protected int getHoehe() {
        return 110;
    }

    @Override
    protected void erstelleFelder(
            XMultiComponentFactory mcf, XMultiServiceFactory xMSF,
            XNameContainer cont, XToolkit xToolkit, XWindowPeer peer,
            XPropertySet dlgProps, XDialog xDialog
    ) throws com.sun.star.uno.Exception {

        // Beschriftung
        fuegeFixedTextEin(xMSF, cont, "lblBeschriftung",
                I18n.get("toolbar.start.dialog.beschriftung"),
                5, 5, 148, 10);

        // ListBox mit allen Turniersystemen
        String[] bezeichnungen = new String[AUSWAHL_SYSTEME.length];
        for (int i = 0; i < AUSWAHL_SYSTEME.length; i++) {
            bezeichnungen[i] = AUSWAHL_SYSTEME[i].getBezeichnung();
        }
        fuegeListBoxEin(xMSF, cont, CTL_LISTBOX, bezeichnungen, 5, 18, 148, 60);

        // Buttons
        fuegeButtonEin(xMSF, cont, CTL_BTN_OK,        I18n.get("toolbar.start.dialog.ok"),        35,  88, 50, 14,
                (short) PushButtonType.OK_value);
        fuegeButtonEin(xMSF, cont, CTL_BTN_ABBRECHEN, I18n.get("toolbar.start.dialog.abbrechen"), 90,  88, 65, 14,
                (short) PushButtonType.CANCEL_value);

        // ListBox-Referenz für beiOkGeklickt() merken
        XControlContainer xcc = Lo.qi(XControlContainer.class, xDialog);
        XControl ctrl = xcc.getControl(CTL_LISTBOX);
        listBox = ctrl != null ? Lo.qi(XListBox.class, ctrl) : null;
    }

    @Override
    protected void beiOkGeklickt() throws Exception {
        if (listBox == null) {
            return;
        }
        short ausgewaehltIndex = listBox.getSelectedItemPos();
        if (ausgewaehltIndex < 0 || ausgewaehltIndex >= AUSWAHL_SYSTEME.length) {
            return;
        }
        TurnierSystem gewaehltesTurnierSystem = AUSWAHL_SYSTEME[ausgewaehltIndex];

        if (isTurnierBereitsVorhanden()) {
            MessageBoxResult antwort = MessageBox.from(ws, MessageBoxTypeEnum.QUESTION_YES_NO)
                    .caption(I18n.get("toolbar.start.bestehendes.turnier.warnung.titel"))
                    .message(I18n.get("toolbar.start.bestehendes.turnier.warnung"))
                    .show();
            if (antwort != MessageBoxResult.YES) {
                logger.info("Turnier-Start abgebrochen (bestehendes Turnier nicht überschrieben).");
                return;
            }
        }

        logger.info("Turnier-Start: {}", gewaehltesTurnierSystem.getBezeichnung());
        starteNeueTurnierInDokument(ws, gewaehltesTurnierSystem);
    }

    private boolean isTurnierBereitsVorhanden() {
        return new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument() != TurnierSystem.KEIN;
    }

    protected static void starteNeueTurnierInDokument(WorkingSpreadsheet zielWs, TurnierSystem system)
            throws Exception {
        switch (system) {
            case SUPERMELEE   -> new MeldeListeSheet_New(zielWs).start();
            case SCHWEIZER    -> new SchweizerMeldeListeSheetNew(zielWs).start();
            case MAASTRICHTER -> new MaastrichterMeldeListeSheetNew(zielWs).start();
            case POULE        -> new PouleMeldeListeSheetNew(zielWs).start();
            case LIGA         -> new LigaMeldeListeSheetNew(zielWs).start();
            case JGJ          -> new JGJMeldeListeSheet_New(zielWs).start();
            case KO           -> new KoMeldeListeSheetNew(zielWs).start();
            case KASKADE      -> new KaskadeMeldeListeSheetNew(zielWs).start();
            case FORMULEX     -> new FormuleXMeldeListeSheetNew(zielWs).start();
            default           -> logger.warn("Unbekanntes Turniersystem für Start-Aktion: {}", system);
        }
    }

    // ---------------------------------------------------------------
    // Hilfsmethoden für Control-Erstellung
    // ---------------------------------------------------------------

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

    private void fuegeListBoxEin(XMultiServiceFactory xMSF, XNameContainer cont,
            String name, String[] items, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        Object model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX",      x);
        props.setPropertyValue("PositionY",      y);
        props.setPropertyValue("Width",          w);
        props.setPropertyValue("Height",         h);
        props.setPropertyValue("StringItemList", items);
        props.setPropertyValue("MultiSelection", Boolean.FALSE);
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
}
