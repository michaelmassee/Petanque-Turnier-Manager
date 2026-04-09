package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.CompositeViewEintragRoh;
import de.petanqueturniermanager.comp.GlobalProperties.PanelEintragRoh;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Modaler Dialog zur Verwaltung der Composite Views.
 * <p>
 * Listet alle vorhandenen Composite Views auf und erlaubt Hinzufügen, Bearbeiten und Löschen.
 * Über [Bearbeiten] bzw. [+ Hinzufügen] wird der {@link CompositeViewDetailDialog} geöffnet.
 */
public class CompositeViewListeDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(CompositeViewListeDialog.class);

    // ---- Layout-Konstanten ----
    private static final int DIALOG_BREITE = 370;
    private static final int DIALOG_HOEHE = 250;
    private static final int MAX_ZEILEN = 8;
    private static final int ZEILE_H = 14;
    private static final int PORT_X = 5;
    private static final int PORT_W = 40;
    private static final int LAYOUT_X = 49;
    private static final int LAYOUT_W = 80;
    private static final int ZOOM_X = 133;
    private static final int ZOOM_W = 35;
    private static final int AKTIV_X = 172;
    private static final int AKTIV_W = 25;
    private static final int EDIT_X = 201;
    private static final int EDIT_W = 60;
    private static final int DEL_X = 265;
    private static final int DEL_W = 18;
    private static final int ZEILE_Y_START = 33;
    private static final int ZEILE_ABSTAND = 16;
    private static final int FOOTER_Y = 230;
    private static final int OK_X = 215;
    private static final int OK_W = 50;
    private static final int ABBRECHEN_X = 270;
    private static final int ABBRECHEN_W = 95;

    // ---- UNO-Referenzen ----
    private XMultiServiceFactory xMSF;
    private XNameContainer cont;
    private XControlContainer xcc;
    private XDialog xDialog;
    private XWindowPeer dialogPeer;

    // ---- Dialog-Zustand ----
    private List<CompositeViewEintragRoh> eintraege = new ArrayList<>();
    private final List<String> dynamischeControlNamen = new ArrayList<>();
    private String[] komboBoxItems;
    private String[] panelKomboBoxItems;

    /** Interne Exception für Validierungsfehler. */
    private static final class UngueltigeEingabeException extends Exception {
        UngueltigeEingabeException(String meldung) {
            super(meldung);
        }
    }

    public CompositeViewListeDialog(XComponentContext xContext) {
        super(xContext);
    }

    public void zeigen(XWindowPeer parentPeer) throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override
    protected String getTitel() {
        return I18n.get("webserver.composite.dialog.liste.titel");
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
        this.xDialog = xDialog;
        this.xcc = Lo.qi(XControlContainer.class, xDialog);
        this.dialogPeer = peer;

        eintraege = new ArrayList<>(GlobalProperties.get().getCompositeViewEintraege());
        komboBoxItems = SheetResolverFactory.SHEET_TYPEN;
        panelKomboBoxItems = ladePanelKomboBoxItems();

        erstelleStatischeControls();
        aktualisiereZeilenArea();
    }

    private void erstelleStatischeControls() throws com.sun.star.uno.Exception {
        fuegeFixedTextEin("lblKopfPort",
                I18n.get("webserver.konfig.tabelle.kopf.port"),
                PORT_X, 20, PORT_W, 10);
        fuegeFixedTextEin("lblKopfPanel0",
                I18n.get("webserver.composite.konfig.tabelle.kopf.panel0"),
                LAYOUT_X, 20, LAYOUT_W, 10);
        fuegeFixedTextEin("lblKopfZoom",
                I18n.get("webserver.konfig.tabelle.kopf.zoom"),
                ZOOM_X, 20, ZOOM_W, 10);
        fuegeFixedTextEin("lblKopfAktiv",
                I18n.get("webserver.konfig.tabelle.kopf.aktiv"),
                AKTIV_X, 20, AKTIV_W + EDIT_W + DEL_W, 10);
    }

    private void aktualisiereZeilenArea() throws com.sun.star.uno.Exception {
        bereinigeDynamischeControls();
        erstelleZeilenControls();
        erstelleFooterControls();
    }

    private void bereinigeDynamischeControls() {
        for (String name : dynamischeControlNamen) {
            entferneControl(name);
        }
        dynamischeControlNamen.clear();
    }

    private void erstelleZeilenControls() throws com.sun.star.uno.Exception {
        for (int i = 0; i < eintraege.size(); i++) {
            int y = ZEILE_Y_START + i * ZEILE_ABSTAND;
            var e = eintraege.get(i);
            String panel0Sheet = e.panels().isEmpty() ? "" : e.panels().get(0).sheetConfig();
            fuegeEditEinDyn("portRow_" + i, String.valueOf(e.port()), PORT_X, y, PORT_W, ZEILE_H);
            fuegeComboBoxEinDyn("panel0Row_" + i, panelKomboBoxItems, LAYOUT_X, y, LAYOUT_W, ZEILE_H, panel0Sheet);
            fuegeEditEinDyn("zoomRow_" + i, String.valueOf(e.zoom()), ZOOM_X, y, ZOOM_W, ZEILE_H);
            fuegeCheckBoxEinDyn("aktivRow_" + i, "", AKTIV_X, y, AKTIV_W, ZEILE_H, e.aktiv());
            fuegeButtonEinDyn("editRow_" + i,
                    I18n.get("webserver.composite.konfig.btn.bearbeiten"),
                    EDIT_X, y, EDIT_W, ZEILE_H, (short) PushButtonType.STANDARD_value);
            fuegeButtonEinDyn("delRow_" + i,
                    I18n.get("webserver.konfig.btn.zeile.loeschen"),
                    DEL_X, y, DEL_W, ZEILE_H, (short) PushButtonType.STANDARD_value);
        }
        for (int i = 0; i < eintraege.size(); i++) {
            final int idx = i;
            registriereActionListener("editRow_" + idx, () -> bearbeiteZeile(idx));
            registriereActionListener("delRow_" + idx, () -> loescheZeile(idx));
        }
    }

    private void erstelleFooterControls() throws com.sun.star.uno.Exception {
        int addY = ZEILE_Y_START + eintraege.size() * ZEILE_ABSTAND + 4;
        fuegeButtonEinDyn("btnHinzufuegen",
                I18n.get("webserver.composite.konfig.btn.hinzufuegen"),
                5, addY, 80, ZEILE_H, (short) PushButtonType.STANDARD_value);
        fuegeButtonEinDyn("btnOk",
                I18n.get("dialog.ok"),
                OK_X, FOOTER_Y, OK_W, ZEILE_H, (short) PushButtonType.STANDARD_value);
        fuegeButtonEinDyn("btnAbbrechen",
                I18n.get("dialog.abbrechen"),
                ABBRECHEN_X, FOOTER_Y, ABBRECHEN_W, ZEILE_H, (short) PushButtonType.CANCEL_value);

        if (eintraege.size() >= MAX_ZEILEN) {
            XControl addBtn = xcc.getControl("btnHinzufuegen");
            if (addBtn != null) {
                Lo.qi(XPropertySet.class, addBtn.getModel()).setPropertyValue("Enabled", Boolean.FALSE);
            }
        }

        registriereActionListener("btnHinzufuegen", this::fuegeZeileHinzu);
        registriereActionListener("btnOk", this::beimOkKlick);
    }

    // ---- Aktionen ----

    private void fuegeZeileHinzu() {
        try {
            leseZeilenDatenAusControls();
            var detailDialog = new CompositeViewDetailDialog(xContext, null, berechneNaechstenFreienPort(), komboBoxItems);
            var neuerEintrag = detailDialog.zeigen();
            if (neuerEintrag != null) {
                eintraege.add(neuerEintrag);
                aktualisiereZeilenArea();
            }
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler beim Hinzufügen eines Composite Views: {}", e.getMessage(), e);
        }
    }

    private void bearbeiteZeile(int idx) {
        try {
            leseZeilenDatenAusControls();
            var eintrag = eintraege.get(idx);
            var detailDialog = new CompositeViewDetailDialog(xContext, eintrag, eintrag.port(), komboBoxItems);
            var geaenderterEintrag = detailDialog.zeigen();
            if (geaenderterEintrag != null) {
                eintraege.set(idx, geaenderterEintrag);
                aktualisiereZeilenArea();
            }
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler beim Bearbeiten des Composite Views: {}", e.getMessage(), e);
        }
    }

    private void loescheZeile(int idx) {
        try {
            leseZeilenDatenAusControls();
            eintraege.remove(idx);
            aktualisiereZeilenArea();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler beim Löschen des Composite Views: {}", e.getMessage(), e);
        }
    }

    private void beimOkKlick() {
        leseZeilenDatenAusControls();
        try {
            validiereEintraege();
            GlobalProperties.get().speichernCompositeViews(eintraege);
            WebServerManager.get().konfigurationGeaendert();
            logger.info("Composite Views gespeichert: {} Einträge", eintraege.size());
            xDialog.endExecute();
        } catch (UngueltigeEingabeException e) {
            MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("webserver.composite.konfig.fehler.titel"))
                    .message(e.getMessage())
                    .show();
        }
    }

    private void leseZeilenDatenAusControls() {
        for (int i = 0; i < eintraege.size(); i++) {
            XControl portCtrl = xcc.getControl("portRow_" + i);
            XControl panel0Ctrl = xcc.getControl("panel0Row_" + i);
            XControl zoomCtrl = xcc.getControl("zoomRow_" + i);
            XControl aktivCtrl = xcc.getControl("aktivRow_" + i);
            if (portCtrl == null) break;

            String portStr = Lo.qi(XTextComponent.class, portCtrl).getText().trim();
            String zoomStr = Lo.qi(XTextComponent.class, zoomCtrl).getText().trim();
            boolean aktiv = Lo.qi(XCheckBox.class, aktivCtrl).getState() == 1;

            // Port: 0 signalisiert leer/ungültig für die Validierung
            int port = 0;
            try { port = portStr.isEmpty() ? 0 : Integer.parseInt(portStr); } catch (NumberFormatException ignored) {}
            var alt = eintraege.get(i);
            int zoom = alt.zoom();
            try { zoom = zoomStr.isEmpty() ? GlobalProperties.DEFAULT_ZOOM : Integer.parseInt(zoomStr); } catch (NumberFormatException ignored) {}

            List<PanelEintragRoh> neuePanels = aktualisierePanel0(alt.panels(), panel0Ctrl);
            eintraege.set(i, new CompositeViewEintragRoh(port, aktiv, zoom, alt.layoutJson(), neuePanels));
        }
    }

    private List<PanelEintragRoh> aktualisierePanel0(List<PanelEintragRoh> panels, XControl panel0Ctrl) {
        if (panel0Ctrl == null || panels.isEmpty()) {
            return panels;
        }
        String neuesSheet = Lo.qi(XTextComponent.class, panel0Ctrl).getText().trim();
        var neuePanels = new ArrayList<>(panels);
        neuePanels.set(0, new PanelEintragRoh(neuesSheet, panels.get(0).zoom(), panels.get(0).zentriert()));
        return neuePanels;
    }

    private void validiereEintraege() throws UngueltigeEingabeException {
        Set<Integer> bekannte = new HashSet<>();
        for (int i = 0; i < eintraege.size(); i++) {
            var e = eintraege.get(i);
            int nr = i + 1;
            if (e.port() == 0) {
                throw new UngueltigeEingabeException(
                        I18n.get("webserver.composite.konfig.fehler.port.leer", nr));
            }
            if (e.port() < 1 || e.port() > 65535) {
                throw new UngueltigeEingabeException(
                        I18n.get("webserver.composite.konfig.fehler.port.ungueltig", nr, e.port()));
            }
            if (!bekannte.add(e.port())) {
                throw new UngueltigeEingabeException(
                        I18n.get("webserver.composite.konfig.fehler.port.duplikat", e.port()));
            }
            if (e.panels().isEmpty()) {
                throw new UngueltigeEingabeException(
                        I18n.get("webserver.composite.konfig.fehler.kein.panel"));
            }
        }
    }

    // ---- Hilfsmethoden ----

    private String[] ladePanelKomboBoxItems() {
        var items = new LinkedHashSet<String>();
        XSpreadsheetDocument doc = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
        if (doc != null) {
            items.addAll(Arrays.asList(doc.getSheets().getElementNames()));
        }
        items.addAll(Arrays.asList(SheetResolverFactory.SHEET_TYPEN));
        return items.toArray(String[]::new);
    }

    private int berechneNaechstenFreienPort() {
        Set<Integer> belegt = new HashSet<>();
        for (var e : eintraege) {
            belegt.add(e.port());
        }
        // Auch bestehende Einzel-Ports berücksichtigen
        for (var p : GlobalProperties.get().getPortEintraege()) {
            belegt.add(p.port());
        }
        int kandidat = 9100;
        while (belegt.contains(kandidat)) {
            kandidat++;
        }
        return kandidat;
    }

    // ---- Control-Hilfsmethoden ----

    private void fuegeFixedTextEin(String name, String label, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",     label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        cont.insertByName(name, model);
    }

    private void fuegeFixedTextEinDyn(String name, String label, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        fuegeFixedTextEin(name, label, x, y, w, h);
        dynamischeControlNamen.add(name);
    }

    private void fuegeEditEinDyn(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        props.setPropertyValue("Text",      text != null ? text : "");
        props.setPropertyValue("MultiLine", Boolean.FALSE);
        cont.insertByName(name, model);
        dynamischeControlNamen.add(name);
    }

    private void fuegeComboBoxEinDyn(String name, String[] items, int x, int y, int w, int h, String selected)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlComboBoxModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX",      x);
        props.setPropertyValue("PositionY",      y);
        props.setPropertyValue("Width",          w);
        props.setPropertyValue("Height",         h);
        props.setPropertyValue("StringItemList", items);
        props.setPropertyValue("Text",           selected != null ? selected : "");
        props.setPropertyValue("Dropdown",       Boolean.TRUE);
        cont.insertByName(name, model);
        dynamischeControlNamen.add(name);
    }

    private void fuegeCheckBoxEinDyn(String name, String label, int x, int y, int w, int h, boolean checked)
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
        dynamischeControlNamen.add(name);
    }

    private void fuegeButtonEinDyn(String name, String label, int x, int y, int w, int h, short pushButtonType)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",          label);
        props.setPropertyValue("PositionX",      x);
        props.setPropertyValue("PositionY",      y);
        props.setPropertyValue("Width",          w);
        props.setPropertyValue("Height",         h);
        props.setPropertyValue("PushButtonType", pushButtonType);
        cont.insertByName(name, model);
        dynamischeControlNamen.add(name);
    }

    private void registriereActionListener(String ctlName, Runnable aktion) {
        XControl ctrl = xcc.getControl(ctlName);
        if (ctrl == null) return;
        Lo.qi(XButton.class, ctrl).addActionListener(new com.sun.star.awt.XActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { aktion.run(); }
            @Override
            public void disposing(EventObject e) {}
        });
    }

    private void entferneControl(String name) {
        try {
            cont.removeByName(name);
        } catch (NoSuchElementException | WrappedTargetException e) {
            // ignorieren
        }
    }
}
