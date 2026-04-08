package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.GsonBuilder;
import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

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
 * Modaler Dialog zum Erstellen und Bearbeiten eines einzelnen Composite Views.
 * <p>
 * Bietet einen minimalen visuellen Builder für den Split-Baum:
 * <ul>
 *   <li>Blatt-Knoten werden als anklickbare Buttons dargestellt</li>
 *   <li>[Split H] / [Split V] teilt das ausgewählte Blatt</li>
 *   <li>[Blatt löschen] entfernt das ausgewählte Blatt (nicht wenn nur eines übrig)</li>
 * </ul>
 * Gibt den konfigurierten {@link CompositeViewEintragRoh} zurück, oder {@code null} bei Abbruch.
 */
public class CompositeViewDetailDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(CompositeViewDetailDialog.class);

    // ---- Layout-Konstanten ----
    private static final int DIALOG_BREITE = 420;
    private static final int DIALOG_HOEHE = 330;
    private static final int ZEILE_H = 14;
    private static final int KOPF_Y = 5;
    private static final int TRENN_Y1 = 22;
    private static final int LAYOUT_BEREICH_Y = 26;
    private static final int PANEL_BTN_H = 14;
    private static final int PANEL_BTN_W = 80;
    private static final int PANEL_BTN_X_START = 5;
    private static final int AKTIONS_BTN_Y_OFFSET = 18;
    private static final int TRENN_Y2_OFFSET = 34;
    private static final int PANEL_KONFIG_Y_OFFSET = 38;
    private static final int FOOTER_Y = 312;
    private static final int OK_X = 235;
    private static final int OK_W = 50;
    private static final int ABBRECHEN_X = 290;
    private static final int ABBRECHEN_W = 125;

    // ---- UNO-Referenzen ----
    private XMultiServiceFactory xMSF;
    private XNameContainer cont;
    private XControlContainer xcc;
    private XDialog xDialog;

    // ---- Dialog-Zustand ----
    private final CompositeViewEintragRoh initialerEintrag;
    private final int initialerPort;
    private final String[] komboBoxItems;

    /** Wurzelknoten des aktuellen Split-Baums. */
    private SplitKnoten wurzel;
    /** Sheet-Config pro Panel (Index = Panel-ID). */
    private final List<String> panelSheets = new ArrayList<>();
    /** Zoom pro Panel (Index = Panel-ID). */
    private final List<Integer> panelZooms = new ArrayList<>();
    /** Index des aktuell ausgewählten Panels (-1 = keines). */
    private int ausgewaehlterPanelIndex = 0;

    private final List<String> dynamischeControlNamen = new ArrayList<>();

    /** Das konfigurierte Ergebnis – wird bei OK gesetzt. */
    private CompositeViewEintragRoh ergebnis = null;

    /** Interne Validierungsausnahme. */
    private static final class UngueltigeEingabeException extends Exception {
        UngueltigeEingabeException(String meldung) { super(meldung); }
    }

    public CompositeViewDetailDialog(
            XComponentContext xContext,
            CompositeViewEintragRoh initialerEintrag,
            int initialerPort,
            String[] komboBoxItems) {
        super(xContext);
        this.initialerEintrag = initialerEintrag;
        this.initialerPort = initialerPort;
        this.komboBoxItems = komboBoxItems;
    }

    /**
     * Öffnet den Dialog und gibt den konfigurierten Eintrag zurück, oder {@code null} bei Abbruch.
     */
    public CompositeViewEintragRoh zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
        return ergebnis;
    }

    @Override
    protected String getTitel() {
        return I18n.get("webserver.composite.dialog.detail.titel");
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

        initialisiereZustand();
        erstelleStatischeControls();
        aktualisiereDynamischeArea();
    }

    private void initialisiereZustand() {
        panelSheets.clear();
        panelZooms.clear();

        if (initialerEintrag != null && !initialerEintrag.panels().isEmpty()) {
            // Bestehenden Eintrag laden
            var gson = new GsonBuilder()
                    .registerTypeAdapter(SplitKnoten.class, new SplitKnotenAdapter())
                    .create();
            try {
                wurzel = gson.fromJson(initialerEintrag.layoutJson(), SplitKnoten.class);
            } catch (Exception e) {
                logger.warn("Layout-JSON ungültig, verwende Standard: {}", e.getMessage());
                wurzel = null;
            }
            if (wurzel == null) {
                wurzel = new SplitBlatt(0);
            }
            for (var p : initialerEintrag.panels()) {
                panelSheets.add(p.sheetConfig());
                panelZooms.add(p.zoom());
            }
        } else {
            // Neuer Eintrag: ein Panel, leerer Baum
            wurzel = new SplitBlatt(0);
            panelSheets.add(SheetResolverFactory.DEFAULT_SHEET_TYP);
            panelZooms.add(GlobalProperties.DEFAULT_ZOOM);
        }
        ausgewaehlterPanelIndex = 0;
    }

    private void erstelleStatischeControls() throws com.sun.star.uno.Exception {
        // Kopfzeile: Port, Zoom, Aktiv
        int aktiv = (initialerEintrag == null || initialerEintrag.aktiv()) ? 1 : 0;
        int zoom = initialerEintrag != null ? initialerEintrag.zoom() : GlobalProperties.DEFAULT_ZOOM;

        fuegeFixedTextEin("lblPort", "Port:", 5, KOPF_Y, 20, ZEILE_H);
        fuegeEditEin("txtPort", String.valueOf(initialerPort), 28, KOPF_Y, 40, ZEILE_H);
        fuegeFixedTextEin("lblZoom", I18n.get("webserver.konfig.tabelle.kopf.zoom") + ":", 80, KOPF_Y, 25, ZEILE_H);
        fuegeEditEin("txtZoom", String.valueOf(zoom), 108, KOPF_Y, 30, ZEILE_H);
        fuegeCheckBoxEin("cbAktiv", I18n.get("webserver.konfig.tabelle.kopf.aktiv"), 150, KOPF_Y, 60, ZEILE_H, aktiv == 1);

        fuegeFixedTextEin("lblLayout", I18n.get("webserver.composite.konfig.bereich.layout"), 5, TRENN_Y1, 200, ZEILE_H);

        // OK/Abbrechen
        fuegeButtonEin("btnOk", I18n.get("dialog.ok"), OK_X, FOOTER_Y, OK_W, ZEILE_H, (short) PushButtonType.STANDARD_value);
        fuegeButtonEin("btnAbbrechen", I18n.get("dialog.abbrechen"), ABBRECHEN_X, FOOTER_Y, ABBRECHEN_W, ZEILE_H, (short) PushButtonType.CANCEL_value);
        registriereActionListenerStatisch("btnOk", this::beimOkKlick);
    }

    private void aktualisiereDynamischeArea() throws com.sun.star.uno.Exception {
        bereinigeDynamischeControls();

        // Panel-Buttons für alle Blattknoten zeichnen
        var blaetter = sammleBlaetter(wurzel);
        int panelBtnY = LAYOUT_BEREICH_Y;
        for (int i = 0; i < blaetter.size(); i++) {
            int panelId = blaetter.get(i);
            String sheetName = panelId < panelSheets.size() ? panelSheets.get(panelId) : "?";
            String label = ausgewaehlterPanelIndex == panelId
                    ? I18n.get("webserver.composite.konfig.panel.label.ausgewaehlt", panelId, sheetName)
                    : I18n.get("webserver.composite.konfig.panel.label", panelId, sheetName);
            int x = PANEL_BTN_X_START + i * (PANEL_BTN_W + 4);
            if (x + PANEL_BTN_W > DIALOG_BREITE - 5) {
                // Zweite Zeile wenn nötig
                panelBtnY += PANEL_BTN_H + 2;
                x = PANEL_BTN_X_START;
            }
            fuegeButtonEinDyn("panelBtn_" + panelId, label, x, panelBtnY, PANEL_BTN_W, PANEL_BTN_H,
                    (short) PushButtonType.STANDARD_value);
            final int pid = panelId;
            registriereActionListenerDyn("panelBtn_" + pid, () -> waehlePanel(pid));
        }

        // Aktions-Buttons
        int aktionY = panelBtnY + AKTIONS_BTN_Y_OFFSET;
        fuegeButtonEinDyn("btnSplitH",
                I18n.get("webserver.composite.konfig.split.h"),
                5, aktionY, 100, ZEILE_H, (short) PushButtonType.STANDARD_value);
        fuegeButtonEinDyn("btnSplitV",
                I18n.get("webserver.composite.konfig.split.v"),
                109, aktionY, 100, ZEILE_H, (short) PushButtonType.STANDARD_value);
        fuegeButtonEinDyn("btnBlattLoeschen",
                I18n.get("webserver.composite.konfig.blatt.loeschen"),
                213, aktionY, 80, ZEILE_H, (short) PushButtonType.STANDARD_value);
        registriereActionListenerDyn("btnSplitH", this::splitzeHorizontal);
        registriereActionListenerDyn("btnSplitV", this::splitzeVertikal);
        registriereActionListenerDyn("btnBlattLoeschen", this::loescheBlatt);

        // [Blatt löschen] deaktivieren wenn nur ein Panel vorhanden
        if (panelSheets.size() <= 1) {
            XControl delBtn = xcc.getControl("btnBlattLoeschen");
            if (delBtn != null) {
                Lo.qi(XPropertySet.class, delBtn.getModel()).setPropertyValue("Enabled", Boolean.FALSE);
            }
        }

        // Panel-Konfiguration für das ausgewählte Panel
        int konfY = aktionY + TRENN_Y2_OFFSET;
        fuegeFixedTextEinDyn("lblPanelKonfig",
                I18n.get("webserver.composite.konfig.bereich.panel"),
                5, konfY - 14, 200, ZEILE_H);

        fuegeFixedTextEinDyn("lblPanelSheet",
                I18n.get("webserver.composite.konfig.panel.sheet.label"),
                5, konfY, 25, ZEILE_H);
        String aktuellesSheet = ausgewaehlterPanelIndex < panelSheets.size()
                ? panelSheets.get(ausgewaehlterPanelIndex) : "";
        fuegeComboBoxEinDyn("cbPanelSheet", ladeComboBoxItems(), 33, konfY, 150, ZEILE_H, aktuellesSheet);

        fuegeFixedTextEinDyn("lblPanelZoom",
                I18n.get("webserver.composite.konfig.panel.zoom.label"),
                190, konfY, 25, ZEILE_H);
        int aktuellerZoom = ausgewaehlterPanelIndex < panelZooms.size()
                ? panelZooms.get(ausgewaehlterPanelIndex) : GlobalProperties.DEFAULT_ZOOM;
        fuegeEditEinDyn("txtPanelZoom", String.valueOf(aktuellerZoom), 218, konfY, 35, ZEILE_H);
    }

    // ---- Panel-Builder-Aktionen ----

    private void waehlePanel(int panelId) {
        speicherePanelKonfiguration();
        ausgewaehlterPanelIndex = panelId;
        try {
            aktualisiereDynamischeArea();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler beim Wechseln des Panels: {}", e.getMessage(), e);
        }
    }

    private void splitzeHorizontal() {
        speicherePanelKonfiguration();
        int neuerPanelIndex = panelSheets.size();
        panelSheets.add(SheetResolverFactory.DEFAULT_SHEET_TYP);
        panelZooms.add(GlobalProperties.DEFAULT_ZOOM);
        wurzel = ersetzeBlatt(wurzel, ausgewaehlterPanelIndex,
                SplitTeilung.horizontal(new SplitBlatt(ausgewaehlterPanelIndex), new SplitBlatt(neuerPanelIndex)));
        ausgewaehlterPanelIndex = neuerPanelIndex;
        aktualisiereUndFange();
    }

    private void splitzeVertikal() {
        speicherePanelKonfiguration();
        int neuerPanelIndex = panelSheets.size();
        panelSheets.add(SheetResolverFactory.DEFAULT_SHEET_TYP);
        panelZooms.add(GlobalProperties.DEFAULT_ZOOM);
        wurzel = ersetzeBlatt(wurzel, ausgewaehlterPanelIndex,
                SplitTeilung.vertikal(new SplitBlatt(ausgewaehlterPanelIndex), new SplitBlatt(neuerPanelIndex)));
        ausgewaehlterPanelIndex = neuerPanelIndex;
        aktualisiereUndFange();
    }

    private void loescheBlatt() {
        if (panelSheets.size() <= 1) return;
        speicherePanelKonfiguration();
        int zuLoeschenderIndex = ausgewaehlterPanelIndex;
        wurzel = entferneBlattAusBaum(wurzel, zuLoeschenderIndex);
        // Panel-Config entfernen und Indizes im Baum anpassen
        panelSheets.remove(zuLoeschenderIndex);
        panelZooms.remove(zuLoeschenderIndex);
        wurzel = renumeriereNachLoeschen(wurzel, zuLoeschenderIndex);
        ausgewaehlterPanelIndex = Math.min(ausgewaehlterPanelIndex, panelSheets.size() - 1);
        aktualisiereUndFange();
    }

    private void aktualisiereUndFange() {
        try {
            aktualisiereDynamischeArea();
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler beim Aktualisieren der Layout-Area: {}", e.getMessage(), e);
        }
    }

    // ---- OK-Klick ----

    private void beimOkKlick() {
        speicherePanelKonfiguration();
        try {
            var eintrag = validiereUndBaue();
            ergebnis = eintrag;
            xDialog.endExecute();
        } catch (UngueltigeEingabeException e) {
            MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("webserver.composite.konfig.fehler.titel"))
                    .message(e.getMessage())
                    .show();
        }
    }

    private void speicherePanelKonfiguration() {
        XControl sheetCtrl = xcc.getControl("cbPanelSheet");
        XControl zoomCtrl = xcc.getControl("txtPanelZoom");
        if (sheetCtrl != null && ausgewaehlterPanelIndex < panelSheets.size()) {
            panelSheets.set(ausgewaehlterPanelIndex,
                    Lo.qi(XTextComponent.class, sheetCtrl).getText().trim());
        }
        if (zoomCtrl != null && ausgewaehlterPanelIndex < panelZooms.size()) {
            try {
                int z = Integer.parseInt(Lo.qi(XTextComponent.class, zoomCtrl).getText().trim());
                panelZooms.set(ausgewaehlterPanelIndex, z);
            } catch (NumberFormatException ignored) {}
        }
    }

    private CompositeViewEintragRoh validiereUndBaue() throws UngueltigeEingabeException {
        // Port
        XControl portCtrl = xcc.getControl("txtPort");
        String portStr = portCtrl != null ? Lo.qi(XTextComponent.class, portCtrl).getText().trim() : "";
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new UngueltigeEingabeException(
                    I18n.get("webserver.composite.konfig.fehler.port.ungueltig", 1, portStr));
        }
        if (port < 1 || port > 65535) {
            throw new UngueltigeEingabeException(
                    I18n.get("webserver.composite.konfig.fehler.port.ungueltig", 1, portStr));
        }

        // Zoom
        XControl zoomCtrl = xcc.getControl("txtZoom");
        String zoomStr = zoomCtrl != null ? Lo.qi(XTextComponent.class, zoomCtrl).getText().trim() : "";
        int zoom;
        try {
            zoom = zoomStr.isEmpty() ? GlobalProperties.DEFAULT_ZOOM : Integer.parseInt(zoomStr);
        } catch (NumberFormatException e) {
            throw new UngueltigeEingabeException(I18n.get("webserver.composite.konfig.fehler.zoom.ungueltig"));
        }
        if (zoom < 10 || zoom > 500) {
            throw new UngueltigeEingabeException(I18n.get("webserver.composite.konfig.fehler.zoom.ungueltig"));
        }

        // Aktiv
        XControl aktivCtrl = xcc.getControl("cbAktiv");
        boolean aktiv = aktivCtrl != null && Lo.qi(XCheckBox.class, aktivCtrl).getState() == 1;

        // Panels
        if (panelSheets.isEmpty()) {
            throw new UngueltigeEingabeException(I18n.get("webserver.composite.konfig.fehler.kein.panel"));
        }
        List<PanelEintragRoh> panels = new ArrayList<>();
        for (int i = 0; i < panelSheets.size(); i++) {
            int pZoom = i < panelZooms.size() ? panelZooms.get(i) : GlobalProperties.DEFAULT_ZOOM;
            panels.add(new PanelEintragRoh(panelSheets.get(i), pZoom));
        }

        // Layout serialisieren
        var gson = new GsonBuilder()
                .registerTypeAdapter(SplitKnoten.class, new SplitKnotenAdapter())
                .create();
        String layoutJson = gson.toJson(wurzel, SplitKnoten.class);

        return new CompositeViewEintragRoh(port, aktiv, zoom, layoutJson, panels);
    }

    // ---- Baum-Operationen (statisch für Testbarkeit) ----

    /**
     * Sammelt alle Panel-Indices aus den Blattknoten in Inorder-Reihenfolge.
     */
    static List<Integer> sammleBlaetter(SplitKnoten knoten) {
        List<Integer> result = new ArrayList<>();
        sammleBlaetterRekursiv(knoten, result);
        return result;
    }

    private static void sammleBlaetterRekursiv(SplitKnoten knoten, List<Integer> result) {
        switch (knoten) {
            case SplitBlatt blatt -> result.add(blatt.panel());
            case SplitTeilung teilung -> {
                sammleBlaetterRekursiv(teilung.links(), result);
                sammleBlaetterRekursiv(teilung.rechts(), result);
            }
        }
    }

    /**
     * Ersetzt den Blattknoten mit dem gegebenen Panel-Index durch einen neuen Knoten.
     */
    static SplitKnoten ersetzeBlatt(SplitKnoten knoten, int panelIndex, SplitKnoten neuerKnoten) {
        return switch (knoten) {
            case SplitBlatt blatt -> blatt.panel() == panelIndex ? neuerKnoten : blatt;
            case SplitTeilung teilung -> new SplitTeilung(
                    teilung.richtung(), teilung.groesse(),
                    ersetzeBlatt(teilung.links(), panelIndex, neuerKnoten),
                    ersetzeBlatt(teilung.rechts(), panelIndex, neuerKnoten));
        };
    }

    /**
     * Entfernt den Blattknoten mit dem gegebenen Panel-Index aus dem Baum.
     * Die Eltern-Teilung wird durch das verbleibende Geschwister ersetzt.
     */
    static SplitKnoten entferneBlattAusBaum(SplitKnoten knoten, int panelIndex) {
        return switch (knoten) {
            case SplitBlatt blatt -> blatt; // Wurzel-Blatt: nicht löschen (Guard oben)
            case SplitTeilung teilung -> {
                boolean linksIstZiel = istBlattMitIndex(teilung.links(), panelIndex);
                boolean rechtsIstZiel = istBlattMitIndex(teilung.rechts(), panelIndex);
                if (linksIstZiel) {
                    yield teilung.rechts();
                } else if (rechtsIstZiel) {
                    yield teilung.links();
                } else {
                    yield new SplitTeilung(
                            teilung.richtung(), teilung.groesse(),
                            entferneBlattAusBaum(teilung.links(), panelIndex),
                            entferneBlattAusBaum(teilung.rechts(), panelIndex));
                }
            }
        };
    }

    private static boolean istBlattMitIndex(SplitKnoten knoten, int index) {
        return knoten instanceof SplitBlatt b && b.panel() == index;
    }

    /**
     * Renumeriert alle Panel-Indices im Baum nach dem Löschen eines Panels:
     * Alle Indices > {@code geloeschtIndex} werden um 1 verringert.
     */
    static SplitKnoten renumeriereNachLoeschen(SplitKnoten knoten, int geloeschtIndex) {
        return switch (knoten) {
            case SplitBlatt blatt -> blatt.panel() > geloeschtIndex
                    ? new SplitBlatt(blatt.panel() - 1)
                    : blatt;
            case SplitTeilung teilung -> new SplitTeilung(
                    teilung.richtung(), teilung.groesse(),
                    renumeriereNachLoeschen(teilung.links(), geloeschtIndex),
                    renumeriereNachLoeschen(teilung.rechts(), geloeschtIndex));
        };
    }

    // ---- ComboBox-Items ----

    private String[] ladeComboBoxItems() {
        var items = new LinkedHashSet<String>();
        XSpreadsheetDocument doc = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
        if (doc != null) {
            items.addAll(Arrays.asList(doc.getSheets().getElementNames()));
        }
        items.addAll(Arrays.asList(komboBoxItems));
        return items.toArray(String[]::new);
    }

    // ---- Control-Hilfsmethoden: statische Controls (kein Tracking) ----

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

    private void fuegeEditEin(String name, String text, int x, int y, int w, int h)
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

    private void fuegeButtonEin(String name, String label, int x, int y, int w, int h, short pushButtonType)
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
    }

    private void registriereActionListenerStatisch(String ctlName, Runnable aktion) {
        XControl ctrl = xcc.getControl(ctlName);
        if (ctrl == null) return;
        Lo.qi(XButton.class, ctrl).addActionListener(new com.sun.star.awt.XActionListener() {
            @Override public void actionPerformed(ActionEvent e) { aktion.run(); }
            @Override public void disposing(EventObject e) {}
        });
    }

    // ---- Control-Hilfsmethoden: dynamische Controls (mit Tracking) ----

    private void bereinigeDynamischeControls() {
        for (String name : dynamischeControlNamen) {
            entferneControl(name);
        }
        dynamischeControlNamen.clear();
    }

    private void fuegeFixedTextEinDyn(String name, String label, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        fuegeFixedTextEin(name, label, x, y, w, h);
        dynamischeControlNamen.add(name);
    }

    private void fuegeButtonEinDyn(String name, String label, int x, int y, int w, int h, short pushButtonType)
            throws com.sun.star.uno.Exception {
        fuegeButtonEin(name, label, x, y, w, h, pushButtonType);
        dynamischeControlNamen.add(name);
    }

    private void fuegeEditEinDyn(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        fuegeEditEin(name, text, x, y, w, h);
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

    private void registriereActionListenerDyn(String ctlName, Runnable aktion) {
        XControl ctrl = xcc.getControl(ctlName);
        if (ctrl == null) return;
        Lo.qi(XButton.class, ctrl).addActionListener(new com.sun.star.awt.XActionListener() {
            @Override public void actionPerformed(ActionEvent e) { aktion.run(); }
            @Override public void disposing(EventObject e) {}
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
