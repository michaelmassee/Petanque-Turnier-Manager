package de.petanqueturniermanager.konfigdialog.properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.farbe.FarbwahlDialog;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.konfigdialog.AbstractUnoDialog;
import de.petanqueturniermanager.webserver.WebServerManager;

/**
 * Modaler Dialog zur Konfiguration der Turnier-Startseite (eigener Webserver-Port mit
 * Logo, Turniername und Live-Teilnehmerzahl).
 * <p>
 * Port + Aktiv-Flag liegen in {@link GlobalProperties} (per Maschine); Turnierlogo-URL
 * und Turniername als Document-Custom-Properties (per Datei).
 */
public class TurnierStartseiteDialog extends AbstractUnoDialog {

    private static final Logger logger = LogManager.getLogger(TurnierStartseiteDialog.class);

    public static final String DOC_PROP_TURNIERLOGO_URL          = "Turnierlogo Url";
    public static final String DOC_PROP_TURNIERBESCHREIBUNG      = "Turnierbeschreibung";
    public static final String DOC_PROP_BESCHREIBUNG_ANIMATION   = "Turnierbeschreibung Animation";
    public static final String DOC_PROP_BESCHREIBUNG_TEXTFARBE   = "Turnierbeschreibung Textfarbe";
    public static final int    DEFAULT_BESCHREIBUNG_TEXTFARBE    = 0x222222;
    public static final String ANIMATION_DEFAULT                 = "keine";
    /** Reihenfolge der Animations-Optionen in der ListBox (auch Frontend-CSS-Klassen-Suffix). */
    public static final String[] ANIMATION_KEYS = {
            "keine", "fade", "slide", "typewriter", "marquee", "pulse", "blink",
            "zoom", "wave", "glow", "flip", "bounce", "rotate"
    };

    private static final int DIALOG_BREITE = 360;
    private static final int DIALOG_HOEHE  = 260;

    private static final int LBL_X     = 5;
    private static final int LBL_W     = 80;
    private static final int FIELD_X   = 90;
    private static final int FIELD_W   = 220;
    private static final int LOGO_FIELD_W   = 195;
    private static final int LOGO_PICK_W    = 20;
    private static final int LOGO_PICK_GAP  = 5;
    private static final int CTRL_H    = 12;
    private static final int BESCHREIBUNG_H = 70;
    private static final int ZEILE1_Y  = 10;
    private static final int ZEILE2_Y  = 30;
    private static final int ZEILE3_Y  = 55;
    private static final int ZEILE4_Y  = 80;
    private static final int ZEILE5_Y  = 160;
    private static final int ZEILE6_Y  = 185;
    private static final int COLOR_VORSCHAU_W = 30;
    private static final int FARBE_PICK_GAP   = 5;
    private static final int FOOTER_Y  = 235;
    private static final int BTN_UEBERN_X  = 125;
    private static final int BTN_OK_X      = 200;
    private static final int BTN_ABBRUCH_X = 275;
    private static final int BTN_ACTION_W  = 70;
    private static final int BTN_H         = 14;

    private static final String CTRL_AKTIV          = "cbAktiv";
    private static final String CTRL_PORT           = "editPort";
    private static final String CTRL_ZOOM           = "editZoom";
    private static final String CTRL_LOGO           = "editLogo";
    private static final String CTRL_LOGO_PICK      = "btnLogoPick";
    private static final String CTRL_BESCHREIBUNG   = "editBeschreibung";
    private static final String CTRL_ANIMATION      = "lstAnimation";
    private static final String CTRL_TEXTFARBE_VORSCHAU = "lblTextfarbeVorschau";
    private static final String CTRL_TEXTFARBE_PICK     = "btnTextfarbePick";

    private final WorkingSpreadsheet currentSpreadsheet;
    private XMultiServiceFactory xMSF;
    private XNameContainer cont;
    private XControlContainer xcc;
    private XDialog xDialog;
    private XWindowPeer dialogPeer;
    private XPropertySet textfarbeVorschauProps;
    private int textfarbeInt;

    public TurnierStartseiteDialog(WorkingSpreadsheet currentSpreadsheet) {
        super(currentSpreadsheet.getxContext());
        this.currentSpreadsheet = currentSpreadsheet;
    }

    public void zeigen() throws com.sun.star.uno.Exception {
        erstelleUndAusfuehren();
    }

    @Override
    protected String getTitel() {
        return I18n.get("konfiguration.startseite.dialog.titel");
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
        this.xcc  = Lo.qi(XControlContainer.class, xDialog);
        this.xDialog = xDialog;
        this.dialogPeer = peer;

        var gp = GlobalProperties.get();
        var docProps = new DocumentPropertiesHelper(currentSpreadsheet);

        fuegeCheckBox(CTRL_AKTIV, I18n.get("konfiguration.startseite.aktiv.label"),
                LBL_X, ZEILE1_Y, LBL_W + FIELD_W, CTRL_H, gp.isStartseiteAktiv());

        fuegeLabel("lblPort", I18n.get("konfiguration.startseite.port.label"),
                LBL_X, ZEILE2_Y, LBL_W, CTRL_H);
        fuegeEdit(CTRL_PORT, String.valueOf(gp.getStartseitePort()),
                FIELD_X, ZEILE2_Y, 50, CTRL_H);
        fuegeLabel("lblZoom", I18n.get("webserver.konfig.tabelle.kopf.zoom") + " (%)",
                170, ZEILE2_Y, 60, CTRL_H);
        fuegeEdit(CTRL_ZOOM, String.valueOf(gp.getStartseiteZoom()),
                235, ZEILE2_Y, 40, CTRL_H);

        fuegeLabel("lblLogo", I18n.get("konfiguration.startseite.logo.label"),
                LBL_X, ZEILE3_Y, LBL_W, CTRL_H);
        fuegeEdit(CTRL_LOGO, docProps.getStringProperty(DOC_PROP_TURNIERLOGO_URL, ""),
                FIELD_X, ZEILE3_Y, LOGO_FIELD_W, CTRL_H);
        fuegeButton(CTRL_LOGO_PICK, "…",
                FIELD_X + LOGO_FIELD_W + LOGO_PICK_GAP, ZEILE3_Y - 1, LOGO_PICK_W, CTRL_H + 2, (short) 0);
        registriereButtonAktion(CTRL_LOGO_PICK, this::oeffneDateiAuswahl);

        fuegeLabel("lblBeschreibung", I18n.get("konfiguration.startseite.beschreibung.label"),
                LBL_X, ZEILE4_Y, LBL_W, CTRL_H);
        fuegeMehrzeiligesEdit(CTRL_BESCHREIBUNG,
                docProps.getStringProperty(DOC_PROP_TURNIERBESCHREIBUNG, ""),
                FIELD_X, ZEILE4_Y, FIELD_W, BESCHREIBUNG_H);

        fuegeLabel("lblAnimation", I18n.get("konfiguration.startseite.beschreibung.animation.label"),
                LBL_X, ZEILE5_Y, LBL_W, CTRL_H);
        String aktuelleAnimation = docProps.getStringProperty(DOC_PROP_BESCHREIBUNG_ANIMATION, ANIMATION_DEFAULT);
        fuegeListBox(CTRL_ANIMATION, animationLabels(), animationIndex(aktuelleAnimation),
                FIELD_X, ZEILE5_Y, FIELD_W, CTRL_H);

        textfarbeInt = docProps.getIntProperty(DOC_PROP_BESCHREIBUNG_TEXTFARBE, DEFAULT_BESCHREIBUNG_TEXTFARBE)
                & 0xFFFFFF;
        fuegeLabel("lblTextfarbe", I18n.get("konfiguration.startseite.beschreibung.textfarbe.label"),
                LBL_X, ZEILE6_Y, LBL_W, CTRL_H);
        textfarbeVorschauProps = fuegeColorVorschau(CTRL_TEXTFARBE_VORSCHAU, textfarbeInt,
                FIELD_X, ZEILE6_Y, COLOR_VORSCHAU_W, CTRL_H);
        fuegeButton(CTRL_TEXTFARBE_PICK, "…",
                FIELD_X + COLOR_VORSCHAU_W + FARBE_PICK_GAP, ZEILE6_Y - 1,
                LOGO_PICK_W, CTRL_H + 2, (short) 0);
        registriereButtonAktion(CTRL_TEXTFARBE_PICK, this::oeffneTextfarbwahl);

        fuegeButton("btnUebernehmen", I18n.get("dialog.uebernehmen"),
                BTN_UEBERN_X, FOOTER_Y, BTN_ACTION_W, BTN_H, (short) PushButtonType.STANDARD_value);
        fuegeButton("btnOk", I18n.get("dialog.ok"),
                BTN_OK_X, FOOTER_Y, BTN_ACTION_W, BTN_H, (short) PushButtonType.STANDARD_value);
        fuegeButton("btnAbbrechen", I18n.get("dialog.abbrechen"),
                BTN_ABBRUCH_X, FOOTER_Y, BTN_ACTION_W, BTN_H, (short) PushButtonType.CANCEL_value);
        registriereButtonAktion("btnUebernehmen", this::beimUebernehmenKlick);
        registriereButtonAktion("btnOk", () -> {
            if (speichernUndAnwenden()) {
                this.xDialog.endExecute();
            }
        });
    }

    private void beimUebernehmenKlick() {
        // Übernehmen: Speichern + sofort sichtbar machen, Dialog bleibt offen.
        speichernUndAnwenden();
    }

    /**
     * Liest, validiert und speichert die Eingaben und benachrichtigt den WebServer
     * über die geänderte Konfiguration. Bei Validierungsfehler wird eine Fehlerbox
     * gezeigt und {@code false} zurückgegeben.
     */
    private boolean speichernUndAnwenden() {
        boolean aktiv = leseCheckBox();
        String portText = leseFeld(CTRL_PORT);
        String zoomText = leseFeld(CTRL_ZOOM);
        String logo = leseFeld(CTRL_LOGO);
        String beschreibung = leseFeld(CTRL_BESCHREIBUNG);

        int port;
        try {
            port = Integer.parseInt(portText.trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("Port außerhalb des Bereichs");
            }
        } catch (NumberFormatException e) {
            zeigeFehler(I18n.get("konfiguration.startseite.fehler.port"));
            return false;
        }

        int zoom;
        try {
            zoom = Integer.parseInt(zoomText.trim());
            if (zoom < 10 || zoom > 500) {
                throw new NumberFormatException("Zoom außerhalb des Bereichs");
            }
        } catch (NumberFormatException e) {
            zeigeFehler(I18n.get("webserver.composite.konfig.fehler.zoom.ungueltig"));
            return false;
        }

        String animation = ANIMATION_KEYS[Math.max(0, leseListBoxIndex(CTRL_ANIMATION))];

        GlobalProperties.get().speichernStartseite(port, aktiv, zoom);
        var docProps = new DocumentPropertiesHelper(currentSpreadsheet);
        docProps.setStringProperty(DOC_PROP_TURNIERLOGO_URL, logo);
        docProps.setStringProperty(DOC_PROP_TURNIERBESCHREIBUNG, beschreibung);
        docProps.setStringProperty(DOC_PROP_BESCHREIBUNG_ANIMATION, animation);
        docProps.setIntProperty(DOC_PROP_BESCHREIBUNG_TEXTFARBE, textfarbeInt);
        WebServerManager.get().konfigurationGeaendert();
        logger.info("Turnier-Startseite gespeichert: aktiv={}, Port={}, Zoom={}%, Textfarbe=#{}",
                aktiv, port, zoom, String.format("%06x", textfarbeInt & 0xFFFFFF));
        return true;
    }

    private void oeffneTextfarbwahl() {
        var ergebnis = FarbwahlDialog.waehle(xContext, dialogPeer, textfarbeInt);
        if (ergebnis.isEmpty()) {
            return;
        }
        textfarbeInt = ergebnis.getAsInt();
        try {
            textfarbeVorschauProps.setPropertyValue("BackgroundColor", textfarbeInt);
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler beim Setzen der Textfarb-Vorschau", e);
        }
    }

    private XPropertySet fuegeColorVorschau(String name, int farbe, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",           "");
        props.setPropertyValue("PositionX",       x);
        props.setPropertyValue("PositionY",       y);
        props.setPropertyValue("Width",           w);
        props.setPropertyValue("Height",          h);
        props.setPropertyValue("BackgroundColor", farbe);
        props.setPropertyValue("Border",          (short) 2);
        props.setPropertyValue("BorderColor",     0x000000);
        cont.insertByName(name, model);
        return props;
    }

    // ── UNO-Helper ────────────────────────────────────────────────────────────

    private void fuegeLabel(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",     text);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        cont.insertByName(name, model);
    }

    private void fuegeEdit(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Text",      text != null ? text : "");
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        props.setPropertyValue("MultiLine", Boolean.FALSE);
        cont.insertByName(name, model);
    }

    private void fuegeMehrzeiligesEdit(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Text",      text != null ? text : "");
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        props.setPropertyValue("MultiLine",  Boolean.TRUE);
        props.setPropertyValue("VScroll",    Boolean.TRUE);
        props.setPropertyValue("AutoVScroll", Boolean.TRUE);
        cont.insertByName(name, model);
    }

    private void fuegeCheckBox(String name, String label, int x, int y, int w, int h, boolean checked)
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

    private void fuegeListBox(String name, String[] items, int selectedIndex, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX",      x);
        props.setPropertyValue("PositionY",      y);
        props.setPropertyValue("Width",          w);
        props.setPropertyValue("Height",         h);
        props.setPropertyValue("Dropdown",       Boolean.TRUE);
        props.setPropertyValue("StringItemList", items);
        if (selectedIndex >= 0 && selectedIndex < items.length) {
            props.setPropertyValue("SelectedItems", new short[] { (short) selectedIndex });
        }
        cont.insertByName(name, model);
    }

    private int leseListBoxIndex(String name) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) return -1;
        var lb = Lo.qi(XListBox.class, ctrl);
        return lb != null ? lb.getSelectedItemPos() : -1;
    }

    private static String[] animationLabels() {
        // Schlüssel bewusst als Literale aufgeführt, damit der I18n-Referenzdatei-Test
        // sie als verwendet erkennt (Pattern matched nur vollständige String-Literale).
        return new String[] {
                I18n.get("konfiguration.startseite.beschreibung.animation.option.keine"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.fade"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.slide"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.typewriter"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.marquee"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.pulse"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.blink"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.zoom"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.wave"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.glow"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.flip"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.bounce"),
                I18n.get("konfiguration.startseite.beschreibung.animation.option.rotate")
        };
    }

    private static int animationIndex(String key) {
        for (int i = 0; i < ANIMATION_KEYS.length; i++) {
            if (ANIMATION_KEYS[i].equals(key)) return i;
        }
        return 0;
    }

    private void fuegeButton(String name, String label, int x, int y, int w, int h, short typ)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",          label);
        props.setPropertyValue("PositionX",      x);
        props.setPropertyValue("PositionY",      y);
        props.setPropertyValue("Width",          w);
        props.setPropertyValue("Height",         h);
        props.setPropertyValue("PushButtonType", typ);
        cont.insertByName(name, model);
    }

    private boolean leseCheckBox() {
        XControl ctrl = xcc.getControl(CTRL_AKTIV);
        if (ctrl == null) return false;
        var cb = Lo.qi(XCheckBox.class, ctrl);
        return cb != null && cb.getState() == 1;
    }

    private String leseFeld(String name) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) return "";
        var tc = Lo.qi(XTextComponent.class, ctrl);
        return tc != null ? tc.getText().trim() : "";
    }

    private void setzeFeld(String name, String text) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) return;
        var tc = Lo.qi(XTextComponent.class, ctrl);
        if (tc != null) {
            tc.setText(text == null ? "" : text);
        }
    }

    private void registriereButtonAktion(String name, Runnable aktion) {
        XControl ctrl = xcc.getControl(name);
        if (ctrl == null) return;
        Lo.qi(XButton.class, ctrl).addActionListener(new com.sun.star.awt.XActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aktion.run();
            }
            @Override
            public void disposing(EventObject e) {
                // kein Aufräumen nötig
            }
        });
    }

    /**
     * Öffnet den nativen LO-FilePicker zur Auswahl der Logo-Datei. Bewusst der
     * UNO-Picker (kein {@code javax.swing.JFileChooser}) — der Swing-Dialog
     * hängt innerhalb des UNO-Modal-Dialogs (Modal-/Z-Order-/Event-Loop-
     * Konflikt mit weld-Modality, LO friert ein). Dasselbe Problem-Muster wie
     * beim Farbwahl-Dialog (siehe {@link FarbwahlDialog}).
     */
    private void oeffneDateiAuswahl() {
        try {
            XFilePicker3 picker = FilePicker.createWithMode(xContext,
                    TemplateDescription.FILEOPEN_SIMPLE);
            picker.setTitle(I18n.get("konfiguration.startseite.logo.picker.titel"));
            picker.appendFilter(I18n.get("konfiguration.startseite.logo.picker.filter"),
                    "*.png;*.jpg;*.jpeg;*.gif;*.svg;*.webp");

            String aktuell = leseFeld(CTRL_LOGO);
            if (aktuell != null && !aktuell.isBlank() && !aktuell.startsWith("http")) {
                try {
                    var pfad = java.nio.file.Path.of(aktuell);
                    var eltern = pfad.getParent();
                    if (eltern != null && java.nio.file.Files.isDirectory(eltern)) {
                        picker.setDisplayDirectory(eltern.toUri().toURL().toExternalForm());
                    }
                    var dateiname = pfad.getFileName();
                    if (dateiname != null) {
                        picker.setDefaultName(dateiname.toString());
                    }
                } catch (java.net.MalformedURLException | RuntimeException ignored) {
                    // ungültiger Pfad → Default-Verzeichnis
                }
            }

            if (picker.execute() != ExecutableDialogResults.OK) {
                return;
            }
            String[] dateien = picker.getFiles();
            if (dateien.length == 0) {
                return;
            }
            setzeFeld(CTRL_LOGO, urlAlsPfad(dateien[0]));
        } catch (RuntimeException e) {
            logger.error("Fehler bei Datei-Auswahl", e);
        }
    }

    private static String urlAlsPfad(String url) {
        try {
            return java.nio.file.Path.of(java.net.URI.create(url)).toString();
        } catch (IllegalArgumentException | java.nio.file.FileSystemNotFoundException e) {
            return url;
        }
    }

    private void zeigeFehler(String meldung) {
        MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("konfiguration.startseite.dialog.titel"))
                .message(meldung)
                .show();
    }
}
