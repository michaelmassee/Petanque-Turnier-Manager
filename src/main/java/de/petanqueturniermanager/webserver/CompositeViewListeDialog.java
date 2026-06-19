package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.CompositeViewEintragRoh;
import de.petanqueturniermanager.comp.GlobalProperties.PanelEintragRoh;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.OnProperiesChangedEvent;
import de.petanqueturniermanager.comp.turnierevent.TurnierEventType;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;

/**
 * Modaler Dialog zur Verwaltung der Webserver-Konfiguration.
 * <p>
 * Verwaltet die allgemeinen Webserver-Flags, den Regie-Port und alle Composite Views.
 * Über [Bearbeiten] bzw. [+ Hinzufügen] wird für Composite Views der {@link CompositeViewDetailDialog} geöffnet.
 */
public class CompositeViewListeDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(CompositeViewListeDialog.class);

    // ---- Layout-Konstanten ----
    private static final int DIALOG_BREITE = 400;
    private static final int DIALOG_HOEHE = 285;
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
    private static final int KOPFZEILE_Y = 58;
    private static final int ZEILE_Y_START = 71;
    private static final int ZEILE_ABSTAND = 16;
    private static final int FOOTER_Y = 265;
    private static final int OK_X = 245;
    private static final int OK_W = 50;
    private static final int ABBRECHEN_X = 300;
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
    private XCheckBox cbAktiv;
    private XCheckBox cbRegieAktiv;
    private final WorkingSpreadsheet ws;

    /** Aktives Turniersystem des Dokuments – steuert die ComboBox-Filterung; {@code null} = alle. */
    private final TurnierSystem aktivesSystem;

    /** Interne Exception für Validierungsfehler. */
    private static final class UngueltigeEingabeException extends Exception {
        private static final long serialVersionUID = 1L;
        UngueltigeEingabeException(String meldung) {
            super(meldung);
        }
    }

    public CompositeViewListeDialog(WorkingSpreadsheet ws) {
        super(ws.getxContext());
        this.ws = ws;
        var doc = ws.getWorkingSpreadsheetDocument();
        this.aktivesSystem = doc == null
                ? null
                : new DocumentPropertiesHelper(doc).getTurnierSystemAusDocument();
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
        komboBoxItems = SheetResolverFactory.sheetTypenFuer(aktivesSystem);

        erstelleStatischeControls();
        aktualisiereZeilenArea();
    }

    private void erstelleStatischeControls() throws com.sun.star.uno.Exception {
        fuegeCheckBoxEin("cbAktiv",
                I18n.get("webserver.konfig.cb.aktiv"),
                5, 5, 200, 12, GlobalProperties.get().isWebserverAktiv());
        cbAktiv = leseCheckBox("cbAktiv");

        fuegeFixedTextEin("lblRegieBereich",
                I18n.get("webserver.regie.konfig.bereich"),
                5, 23, 120, 10);
        fuegeCheckBoxEin("cbRegieAktiv",
                I18n.get("webserver.regie.konfig.aktiv"),
                130, 21, 60, ZEILE_H, GlobalProperties.get().isWebserverRegieAktiv());
        cbRegieAktiv = leseCheckBox("cbRegieAktiv");
        fuegeFixedTextEin("lblRegiePort",
                I18n.get("webserver.regie.konfig.port"),
                205, 23, 25, 10);
        fuegeEditEin("txtRegiePort",
                String.valueOf(GlobalProperties.get().getWebserverRegiePort()),
                235, 21, 40, ZEILE_H);

        fuegeFixedTextEin("lblCompositeBereich",
                I18n.get("webserver.composite.konfig.bereich.views"),
                5, 42, 200, 10);

        fuegeFixedTextEin("lblKopfPort",
                I18n.get("webserver.konfig.tabelle.kopf.port"),
                PORT_X, KOPFZEILE_Y, PORT_W, 10);
        fuegeFixedTextEin("lblKopfName",
                I18n.get("webserver.composite.konfig.tabelle.kopf.name"),
                LAYOUT_X, KOPFZEILE_Y, LAYOUT_W, 10);
        fuegeFixedTextEin("lblKopfZoom",
                I18n.get("webserver.konfig.tabelle.kopf.zoom"),
                ZOOM_X, KOPFZEILE_Y, ZOOM_W, 10);
        fuegeFixedTextEin("lblKopfAktiv",
                I18n.get("webserver.konfig.tabelle.kopf.aktiv"),
                AKTIV_X, KOPFZEILE_Y, AKTIV_W + EDIT_W + DEL_W, 10);
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
            String anzeigeText;
            if (!e.name().isBlank()) {
                anzeigeText = e.name();
            } else {
                anzeigeText = e.panels().isEmpty() ? "" : e.panels().get(0).sheetConfig();
            }
            fuegeFixedTextEinDyn("portRow_" + i, String.valueOf(e.port()), PORT_X, y, PORT_W, ZEILE_H);
            fuegeFixedTextEinDyn("nameRow_" + i, anzeigeText, LAYOUT_X, y, LAYOUT_W, ZEILE_H);
            fuegeFixedTextEinDyn("zoomRow_" + i, String.valueOf(e.zoom()), ZOOM_X, y, ZOOM_W, ZEILE_H);
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
            var tempIdx = new int[]{-1};
            Consumer<CompositeViewEintragRoh> callback = e -> {
                if (tempIdx[0] == -1) {
                    eintraege.add(e);
                    tempIdx[0] = eintraege.size() - 1;
                } else {
                    eintraege.set(tempIdx[0], e);
                }
                speichernUndAktualisieren();
            };
            var detailDialog = new CompositeViewDetailDialog(
                    xContext, null, berechneNaechstenFreienPort(), komboBoxItems, callback);
            var neuerEintrag = detailDialog.zeigen();
            if (neuerEintrag != null && tempIdx[0] == -1) {
                // OK ohne vorheriges Anwenden: normaler Add-Pfad
                eintraege.add(neuerEintrag);
            }
            aktualisiereZeilenArea();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler beim Hinzufügen eines Composite Views: {}", e.getMessage(), e);
        }
    }

    private void bearbeiteZeile(int idx) {
        try {
            leseZeilenDatenAusControls();
            var eintrag = eintraege.get(idx);
            Consumer<CompositeViewEintragRoh> callback = geaendert -> {
                eintraege.set(idx, geaendert);
                speichernUndAktualisieren();
            };
            var detailDialog = new CompositeViewDetailDialog(
                    xContext, eintrag, eintrag.port(), komboBoxItems, callback);
            var geaenderterEintrag = detailDialog.zeigen();
            if (geaenderterEintrag != null) {
                eintraege.set(idx, geaenderterEintrag); // idempotent falls Callback schon gesetzt hat
            }
            aktualisiereZeilenArea();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler beim Bearbeiten des Composite Views: {}", e.getMessage(), e);
        }
    }

    private void speichernUndAktualisieren() {
        try {
            speichereAlleKonfigurationen();
            WebServerManager.get().konfigurationGeaendert();
            benachrichtigeCompositeViewKonfigurationGeaendert();
        } catch (UngueltigeEingabeException e) {
            zeigeValidierungsFehler(e);
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
            speichereAlleKonfigurationen();
            WebServerManager.get().konfigurationGeaendert();
            benachrichtigeCompositeViewKonfigurationGeaendert();
            logger.info("Webserver-Konfiguration gespeichert: {} Composite-Views", eintraege.size());
            xDialog.endExecute();
        } catch (UngueltigeEingabeException e) {
            zeigeValidierungsFehler(e);
        }
    }

    private void speichereAlleKonfigurationen() throws UngueltigeEingabeException {
        validiereEintraege();
        boolean aktiv = cbAktiv != null && cbAktiv.getState() == 1;
        boolean regieAktiv = cbRegieAktiv != null && cbRegieAktiv.getState() == 1;
        int regiePort = leseRegiePort();
        GlobalProperties.get().speichernCompositeViews(aktiv, eintraege);
        GlobalProperties.get().speichernWebserverRegie(
                regieAktiv,
                regiePort,
                GlobalProperties.get().getWebserverRegieZiele());
    }

    private void benachrichtigeCompositeViewKonfigurationGeaendert() {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return;
        }
        PetanqueTurnierMngrSingleton.triggerTurnierEventListener(TurnierEventType.PropertiesChanged,
                new OnProperiesChangedEvent(doc)
                        .addChanged("webserver_composite_views", "", ""));
    }

    private void leseZeilenDatenAusControls() {
        for (int i = 0; i < eintraege.size(); i++) {
            XControl aktivCtrl = xcc.getControl("aktivRow_" + i);
            if (aktivCtrl == null) break;
            boolean aktiv = Lo.qi(XCheckBox.class, aktivCtrl).getState() == 1;
            var alt = eintraege.get(i);
            eintraege.set(i, new CompositeViewEintragRoh(alt.port(), alt.name(), aktiv, alt.zoom(), alt.mitHeaderFooter(), alt.layoutJson(), alt.panels()));
        }
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
        boolean regieAktiv = cbRegieAktiv != null && cbRegieAktiv.getState() == 1;
        if (regieAktiv) {
            int regiePort = leseRegiePort();
            if (bekannte.contains(regiePort)) {
                throw new UngueltigeEingabeException(
                        I18n.get("webserver.regie.konfig.fehler.port.duplikat", regiePort));
            }
        }
    }

    // ---- Hilfsmethoden ----

    private int berechneNaechstenFreienPort() {
        Set<Integer> belegt = new HashSet<>();
        for (var e : eintraege) {
            belegt.add(e.port());
        }
        int kandidat = 9100;
        while (belegt.contains(kandidat)) {
            kandidat++;
        }
        return kandidat;
    }

    private int leseRegiePort() throws UngueltigeEingabeException {
        XControl portCtrl = xcc.getControl("txtRegiePort");
        String portStr = portCtrl != null ? Lo.qi(XTextComponent.class, portCtrl).getText().trim() : "";
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new UngueltigeEingabeException(
                    I18n.get("webserver.regie.konfig.fehler.port.ungueltig", portStr));
        }
        if (port < 1 || port > 65535) {
            throw new UngueltigeEingabeException(
                    I18n.get("webserver.regie.konfig.fehler.port.ungueltig", portStr));
        }
        return port;
    }

    private void zeigeValidierungsFehler(UngueltigeEingabeException e) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("webserver.composite.konfig.fehler.titel"))
                .message(e.getMessage())
                .show();
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

    private void fuegeEditEin(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Text",      text);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        cont.insertByName(name, model);
    }

    private void fuegeCheckBoxEinDyn(String name, String label, int x, int y, int w, int h, boolean checked)
            throws com.sun.star.uno.Exception {
        fuegeCheckBoxEin(name, label, x, y, w, h, checked);
        dynamischeControlNamen.add(name);
    }

    private void fuegeCheckBoxEin(String name, String label, int x, int y, int w, int h, boolean checked)
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

    private XCheckBox leseCheckBox(String name) {
        XControl ctrl = xcc.getControl(name);
        return ctrl != null ? Lo.qi(XCheckBox.class, ctrl) : null;
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
