package de.petanqueturniermanager.webserver;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
import com.sun.star.awt.XRadioButton;
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
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.CompositeViewEintragRoh;
import de.petanqueturniermanager.comp.GlobalProperties.PanelEintragRoh;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.farbe.FarbwahlDialog;
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
    private static final int DIALOG_HOEHE = 346;
    private static final int ZEILE_H = 14;
    private static final int KOPF_Y = 5;
    private static final int KOPF_Y2 = 21;
    private static final int RAND_Y = 37;
    private static final int TRENN_Y1 = 54;
    private static final int AKTIONS_BTN_Y_OFFSET = 18;
    private static final int FOOTER_Y = 328;
    private static final int UEBERNEHMEN_X = 155;
    private static final int UEBERNEHMEN_W = 75;
    private static final int OK_X = 235;
    private static final int OK_W = 50;
    private static final int ABBRECHEN_X = 290;
    private static final int ABBRECHEN_W = 125;

    // ---- Rand-Zeile: X-Positionen ----
    private static final int RAND_LBL_X = 5;
    private static final int RAND_DICKE_X = 32;
    private static final int RAND_DICKE_W = 22;
    private static final int RAND_DICKE_EINHEIT_X = 55;
    private static final int RAND_ART_X = 70;
    private static final int RAND_ART_W = 65;
    private static final int RAND_FARBE_LBL_X = 140;
    private static final int RAND_FARBE_VORSCHAU_X = 172;
    private static final int RAND_FARBE_VORSCHAU_W = 20;
    private static final int RAND_FARBE_PICK_X = 194;
    private static final int RAND_FARBE_PICK_W = 20;
    private static final int RAND_TRANSP_LBL_X = 219;
    private static final int RAND_TRANSP_X = 258;
    private static final int RAND_TRANSP_W = 22;
    private static final int RAND_TRANSP_EINHEIT_X = 281;
    private static final int RAND_ANIMATION_X = 296;
    private static final int RAND_ANIMATION_W = 119;

    // ---- Vorschau-Konstanten ----
    private static final int VORSCHAU_X             = 5;
    private static final int VORSCHAU_BREITE        = 400;
    private static final int VORSCHAU_HOEHE         = 100;
    private static final int VORSCHAU_LABEL_ABSTAND = 10;
    private static final int VORSCHAU_MIN_GROESSE   = 10;

    // ---- UNO-Referenzen ----
    private XMultiServiceFactory xMSF;
    private XNameContainer cont;
    private XControlContainer xcc;
    private XDialog xDialog;
    private XWindowPeer dialogPeer;

    // ---- Dialog-Zustand ----
    private final CompositeViewEintragRoh initialerEintrag;
    private final int initialerPort;
    private final String[] komboBoxItems;
    /** Wird bei „Übernehmen" und „OK" mit dem validierten Eintrag aufgerufen. */
    private final Consumer<CompositeViewEintragRoh> anwendenCallback;

    /** Wurzelknoten des aktuellen Split-Baums. */
    private SplitKnoten wurzel;
    /** Sheet-Config pro Panel (Index = Panel-ID). */
    private final List<String> panelSheets = new ArrayList<>();
    /** Zoom pro Panel (Index = Panel-ID). */
    private final List<Integer> panelZooms = new ArrayList<>();
    /** Sichtbarer Anteil der Gesamttabelle pro Blatt-Panel in Prozent (Index = Panel-ID). */
    private final List<Integer> panelSichtbareTabellenanteile = new ArrayList<>();
    /** Horizontale Ausrichtung pro Panel (Index = Panel-ID); siehe {@link PanelAusrichtung}. */
    private final List<String> panelHAlign = new ArrayList<>();
    /** Vertikale Ausrichtung pro Panel (Index = Panel-ID); siehe {@link PanelAusrichtung}. */
    private final List<String> panelVAlign = new ArrayList<>();
    /** Blattname-Anzeigen-Flag pro Panel (Index = Panel-ID). */
    private final List<Boolean> panelBlattnameAnzeigen = new ArrayList<>();
    /** Anzeigemodus pro Panel (Index = Panel-ID). */
    private final List<PanelTyp> panelTypen = new ArrayList<>();
    /**
     * Externe URL pro Panel (Index = Panel-ID), nur für Modus {@link PanelTyp#URL}.
     * Wert bleibt erhalten, auch wenn der Modus gewechselt wird (UX: kein Datenverlust).
     */
    private final List<String> panelUrls = new ArrayList<>();
    /**
     * Lokaler Dateipfad pro Panel (Index = Panel-ID), nur für Modus {@link PanelTyp#STATISCHE_DATEI}.
     * Eigene Liste statt Teilen von {@link #panelUrls}: sonst übernimmt beim Moduswechsel URL↔Datei
     * der jeweils andere Modus den zuletzt eingegebenen Text unverändert (z.B. eine URL wird als
     * Dateipfad validiert und liefert die irreführende Fehlermeldung „Datei ist nicht lesbar").
     */
    private final List<String> panelDateien = new ArrayList<>();
    /** Index des aktuell ausgewählten Panels (-1 = keines). */
    private volatile int ausgewaehlterPanelIndex = 0;

    // ---- Rand-Zustand (Gesamtrahmen der View) ----
    private volatile int randDicke;
    private String randArt;
    private volatile int randFarbe;
    private volatile int randTransparenz;
    private String randAnimation;
    private XPropertySet randFarbeVorschauProps;

    private final List<String> dynamischeControlNamen = new ArrayList<>();

    /** Das konfigurierte Ergebnis – wird bei OK gesetzt. */
    private CompositeViewEintragRoh ergebnis = null;

    /**
     * Peer der aufrufenden Optionsseite. Ohne Parent würde der Dialog vom Fenster-Manager als
     * eigenständiges Top-Level-Fenster behandelt und wäre nicht modal gegenüber dem
     * Optionen-Dialog.
     */
    private final XWindowPeer parentPeer;

    /** Interne Validierungsausnahme. */
    private static final class UngueltigeEingabeException extends Exception {
        private static final long serialVersionUID = 1L;
        UngueltigeEingabeException(String meldung) { super(meldung); }
    }

    public CompositeViewDetailDialog(
            XComponentContext xContext,
            CompositeViewEintragRoh initialerEintrag,
            int initialerPort,
            String[] komboBoxItems,
            Consumer<CompositeViewEintragRoh> anwendenCallback,
            XWindowPeer parentPeer) {
        super(xContext);
        this.initialerEintrag = initialerEintrag;
        this.initialerPort = initialerPort;
        this.komboBoxItems = komboBoxItems;
        this.anwendenCallback = anwendenCallback;
        this.parentPeer = parentPeer;
    }

    @Override
    protected XWindowPeer holeParentPeer() {
        return parentPeer;
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
        this.dialogPeer = peer;

        initialisiereZustand();
        erstelleStatischeControls();
        aktualisiereDynamischeArea();
    }

    private void initialisiereZustand() {
        panelSheets.clear();
        panelZooms.clear();
        panelSichtbareTabellenanteile.clear();
        panelHAlign.clear();
        panelVAlign.clear();
        panelBlattnameAnzeigen.clear();
        panelTypen.clear();
        panelUrls.clear();
        panelDateien.clear();

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
                panelSichtbareTabellenanteile.add(p.sichtbarerTabellenAnteil());
                panelHAlign.add(p.horizontalAusrichtung());
                panelVAlign.add(p.vertikalAusrichtung());
                panelBlattnameAnzeigen.add(p.blattnameAnzeigen());
                PanelTyp typ = p.typ() != null ? p.typ() : PanelTyp.BLATT;
                panelTypen.add(typ);
                String externeUrl = p.externeUrl() != null ? p.externeUrl() : "";
                panelUrls.add(typ == PanelTyp.URL ? externeUrl : "");
                panelDateien.add(typ == PanelTyp.STATISCHE_DATEI ? externeUrl : "");
            }
        } else {
            // Neuer Eintrag: ein Panel, leerer Baum
            wurzel = new SplitBlatt(0);
            panelSheets.add(SheetResolverFactory.DEFAULT_SHEET_TYP);
            panelZooms.add(GlobalProperties.DEFAULT_ZOOM);
            panelSichtbareTabellenanteile.add(GlobalProperties.DEFAULT_SICHTBARER_TABELLENANTEIL);
            panelHAlign.add(PanelAusrichtung.KEIN);
            panelVAlign.add(PanelAusrichtung.KEIN);
            panelBlattnameAnzeigen.add(Boolean.FALSE);
            panelTypen.add(PanelTyp.BLATT);
            panelUrls.add("");
            panelDateien.add("");
        }
        ausgewaehlterPanelIndex = 0;

        var rand = initialerEintrag != null ? initialerEintrag.rand() : RandKonfiguration.KEINER;
        randDicke = rand.dicke();
        randArt = rand.art();
        randFarbe = rand.farbe();
        randTransparenz = rand.transparenz();
        randAnimation = rand.animation();
    }

    private void erstelleStatischeControls() throws com.sun.star.uno.Exception {
        // Kopfzeile: Port, Zoom, Aktiv, Header/Footer
        int aktiv = (initialerEintrag == null || initialerEintrag.aktiv()) ? 1 : 0;
        int zoom = initialerEintrag != null ? initialerEintrag.zoom() : GlobalProperties.DEFAULT_ZOOM;
        // Default = true (mit Header/Footer); für neue Einträge ebenfalls true.
        int mitHeaderFooter = (initialerEintrag == null || initialerEintrag.mitHeaderFooter()) ? 1 : 0;

        fuegeFixedTextEin("lblPort", "Port:", 5, KOPF_Y, 20, ZEILE_H);
        fuegeEditEin("txtPort", String.valueOf(initialerPort), 28, KOPF_Y, 40, ZEILE_H);
        fuegeFixedTextEin("lblZoom", I18n.get("webserver.konfig.tabelle.kopf.zoom") + ":", 80, KOPF_Y, 25, ZEILE_H);
        fuegeEditEin("txtZoom", String.valueOf(zoom), 108, KOPF_Y, 30, ZEILE_H);
        fuegeCheckBoxEin("cbAktiv", I18n.get("webserver.konfig.tabelle.kopf.aktiv"), 150, KOPF_Y, 60, ZEILE_H, aktiv == 1);
        fuegeCheckBoxEin("cbMitHeaderFooter", I18n.get("webserver.komposit.mit.header.footer"),
                215, KOPF_Y, 195, ZEILE_H, mitHeaderFooter == 1);

        // Zweite Kopfzeile: Name (optional)
        String name = initialerEintrag != null ? initialerEintrag.name() : "";
        fuegeFixedTextEin("lblName", I18n.get("webserver.composite.konfig.name.label"),
                5, KOPF_Y2, 30, ZEILE_H);
        fuegeEditEin("txtName", name, 36, KOPF_Y2, 374, ZEILE_H);

        erstelleRandZeile();

        // Übernehmen / OK / Abbrechen
        fuegeButtonEin("btnUebernehmen", I18n.get("webserver.composite.dialog.detail.uebernehmen"),
                UEBERNEHMEN_X, FOOTER_Y, UEBERNEHMEN_W, ZEILE_H, (short) PushButtonType.STANDARD_value);
        fuegeButtonEin("btnOk", I18n.get("dialog.ok"), OK_X, FOOTER_Y, OK_W, ZEILE_H, (short) PushButtonType.STANDARD_value);
        fuegeButtonEin("btnAbbrechen", I18n.get("dialog.abbrechen"), ABBRECHEN_X, FOOTER_Y, ABBRECHEN_W, ZEILE_H, (short) PushButtonType.CANCEL_value);
        registriereActionListenerStatisch("btnUebernehmen", this::beimUebernehmenKlick);
        registriereActionListenerStatisch("btnOk", this::beimOkKlick);
    }

    /**
     * Baut die statische Rand-Zeile (Gesamtrahmen der View: Dicke, Art, Farbe,
     * Transparenz, Animation) zwischen der Name-Zeile und der dynamischen Panel-Area.
     */
    private void erstelleRandZeile() throws com.sun.star.uno.Exception {
        fuegeFixedTextEin("lblRand", I18n.get("webserver.composite.konfig.rand.label"),
                RAND_LBL_X, RAND_Y, 25, ZEILE_H);
        fuegeEditEin("txtRandDicke", String.valueOf(randDicke), RAND_DICKE_X, RAND_Y, RAND_DICKE_W, ZEILE_H);
        fuegeFixedTextEin("lblRandDickeEinheit", "px", RAND_DICKE_EINHEIT_X, RAND_Y, 12, ZEILE_H);
        fuegeComboBoxEin("cbRandArt", RAND_ART_LABELS, RAND_ART_X, RAND_Y, RAND_ART_W, ZEILE_H, randArtKeyZuLabel(randArt));

        fuegeFixedTextEin("lblRandFarbe", I18n.get("webserver.composite.konfig.rand.farbe.label"),
                RAND_FARBE_LBL_X, RAND_Y, 32, ZEILE_H);
        randFarbeVorschauProps = fuegeColorVorschauEin("lblRandFarbeVorschau", randFarbe,
                RAND_FARBE_VORSCHAU_X, RAND_Y, RAND_FARBE_VORSCHAU_W, ZEILE_H);
        fuegeButtonEin("btnRandFarbe", "…", RAND_FARBE_PICK_X, RAND_Y - 1, RAND_FARBE_PICK_W, ZEILE_H + 2,
                (short) 0);
        registriereActionListenerStatisch("btnRandFarbe", this::oeffneRandFarbwahl);

        fuegeFixedTextEin("lblRandTransp", I18n.get("webserver.composite.konfig.rand.transparenz.label"),
                RAND_TRANSP_LBL_X, RAND_Y, 38, ZEILE_H);
        fuegeEditEin("txtRandTransp", String.valueOf(randTransparenz), RAND_TRANSP_X, RAND_Y, RAND_TRANSP_W, ZEILE_H);
        fuegeFixedTextEin("lblRandTranspEinheit", "%", RAND_TRANSP_EINHEIT_X, RAND_Y, 10, ZEILE_H);

        fuegeComboBoxEin("cbRandAnimation", RAND_ANIMATION_LABELS, RAND_ANIMATION_X, RAND_Y,
                RAND_ANIMATION_W, ZEILE_H, randAnimationKeyZuLabel(randAnimation));
    }

    private void oeffneRandFarbwahl() {
        var ergebnis = FarbwahlDialog.waehle(xContext, dialogPeer, randFarbe);
        if (ergebnis.isEmpty()) {
            return;
        }
        randFarbe = ergebnis.getAsInt();
        try {
            randFarbeVorschauProps.setPropertyValue("BackgroundColor", randFarbe);
        } catch (com.sun.star.uno.Exception e) {
            logger.error("Fehler beim Setzen der Randfarb-Vorschau", e);
        }
    }

    private final String[] RAND_ART_LABELS = {
            I18n.get("webserver.composite.konfig.rand.art.kein"),
            I18n.get("webserver.composite.konfig.rand.art.solid"),
            I18n.get("webserver.composite.konfig.rand.art.dashed"),
            I18n.get("webserver.composite.konfig.rand.art.dotted"),
            I18n.get("webserver.composite.konfig.rand.art.double"),
    };

    private final String[] RAND_ANIMATION_LABELS = {
            I18n.get("webserver.composite.konfig.rand.animation.keine"),
            I18n.get("webserver.composite.konfig.rand.animation.ameisen"),
            I18n.get("webserver.composite.konfig.rand.animation.pulsieren"),
            I18n.get("webserver.composite.konfig.rand.animation.farbwechsel"),
    };

    private String randArtKeyZuLabel(String key) {
        return switch (RandKonfiguration.normiereArt(key)) {
            case RandKonfiguration.ART_SOLID  -> RAND_ART_LABELS[1];
            case RandKonfiguration.ART_DASHED -> RAND_ART_LABELS[2];
            case RandKonfiguration.ART_DOTTED -> RAND_ART_LABELS[3];
            case RandKonfiguration.ART_DOUBLE -> RAND_ART_LABELS[4];
            default -> RAND_ART_LABELS[0];
        };
    }

    private String randArtLabelZuKey(String label) {
        if (label == null) return RandKonfiguration.ART_KEIN;
        if (label.equals(RAND_ART_LABELS[1])) return RandKonfiguration.ART_SOLID;
        if (label.equals(RAND_ART_LABELS[2])) return RandKonfiguration.ART_DASHED;
        if (label.equals(RAND_ART_LABELS[3])) return RandKonfiguration.ART_DOTTED;
        if (label.equals(RAND_ART_LABELS[4])) return RandKonfiguration.ART_DOUBLE;
        return RandKonfiguration.ART_KEIN;
    }

    private String randAnimationKeyZuLabel(String key) {
        return switch (RandKonfiguration.normiereAnimation(key)) {
            case RandKonfiguration.ANIMATION_AMEISEN     -> RAND_ANIMATION_LABELS[1];
            case RandKonfiguration.ANIMATION_PULSIEREN   -> RAND_ANIMATION_LABELS[2];
            case RandKonfiguration.ANIMATION_FARBWECHSEL -> RAND_ANIMATION_LABELS[3];
            default -> RAND_ANIMATION_LABELS[0];
        };
    }

    private String randAnimationLabelZuKey(String label) {
        if (label == null) return RandKonfiguration.ANIMATION_KEINE;
        if (label.equals(RAND_ANIMATION_LABELS[1])) return RandKonfiguration.ANIMATION_AMEISEN;
        if (label.equals(RAND_ANIMATION_LABELS[2])) return RandKonfiguration.ANIMATION_PULSIEREN;
        if (label.equals(RAND_ANIMATION_LABELS[3])) return RandKonfiguration.ANIMATION_FARBWECHSEL;
        return RandKonfiguration.ANIMATION_KEINE;
    }

    private void aktualisiereDynamischeArea() throws com.sun.star.uno.Exception {
        bereinigeDynamischeControls();

        // Aktions-Buttons (direkt unter dem Kopf)
        int aktionY = TRENN_Y1;
        fuegeButtonEinDyn("btnSplitH", I18n.get("webserver.composite.konfig.split.h"), 5, aktionY, 100, ZEILE_H);
        fuegeButtonEinDyn("btnSplitV", I18n.get("webserver.composite.konfig.split.v"), 109, aktionY, 100, ZEILE_H);
        fuegeButtonEinDyn("btnBlattLoeschen", I18n.get("webserver.composite.konfig.blatt.loeschen"), 213, aktionY, 80, ZEILE_H);
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
        int konfY = aktionY + AKTIONS_BTN_Y_OFFSET;
        fuegeFixedTextEinDyn("lblPanelKonfig", I18n.get("webserver.composite.konfig.bereich.panel"), 5, konfY, 150, ZEILE_H);

        int splitBreite = panelShareInSplit(wurzel, ausgewaehlterPanelIndex, "H");
        if (splitBreite >= 0) {
            fuegeFixedTextEinDyn("lblSplitBreite", I18n.get("webserver.composite.konfig.panel.splitbreite"), 170, konfY, 45, ZEILE_H);
            fuegeEditEinDyn("txtSplitBreite", String.valueOf(splitBreite), 217, konfY, 30, ZEILE_H);
        }
        int splitHoehe = panelShareInSplit(wurzel, ausgewaehlterPanelIndex, "V");
        if (splitHoehe >= 0) {
            fuegeFixedTextEinDyn("lblSplitHoehe", I18n.get("webserver.composite.konfig.panel.splithoehe"), 260, konfY, 45, ZEILE_H);
            fuegeEditEinDyn("txtSplitHoehe", String.valueOf(splitHoehe), 307, konfY, 30, ZEILE_H);
        }

        // ---- Modus-Auswahl: Blatt / URL / Timer / lokale Datei / Turnierstartseite ----
        int modusY = konfY + ZEILE_H;
        PanelTyp aktuellerTyp = ausgewaehlterPanelIndex < panelTypen.size() ? panelTypen.get(ausgewaehlterPanelIndex) : PanelTyp.BLATT;
        boolean istUrlModus   = aktuellerTyp == PanelTyp.URL;
        boolean istTimerModus = aktuellerTyp == PanelTyp.TIMER;
        boolean istDateiModus = aktuellerTyp == PanelTyp.STATISCHE_DATEI;
        boolean istStartseitenModus = aktuellerTyp == PanelTyp.TURNIERSTARTSEITE;
        fuegeRadioButtonEinDyn("rbBlatt", I18n.get("webserver.composite.konfig.panel.modus.blatt"), 5,   modusY, 45, ZEILE_H, !istUrlModus && !istTimerModus && !istDateiModus && !istStartseitenModus);
        fuegeRadioButtonEinDyn("rbUrl",   I18n.get("webserver.composite.konfig.panel.modus.url"),   55,  modusY, 80, ZEILE_H, istUrlModus);
        fuegeRadioButtonEinDyn("rbTimer", I18n.get("webserver.composite.konfig.panel.modus.timer"), 140, modusY, 45, ZEILE_H, istTimerModus);
        fuegeRadioButtonEinDyn("rbDatei", I18n.get("webserver.composite.konfig.panel.modus.datei"), 190, modusY, 90, ZEILE_H, istDateiModus);
        fuegeRadioButtonEinDyn("rbStartseite", I18n.get("webserver.composite.konfig.panel.modus.startseite"), 285, modusY, 125, ZEILE_H, istStartseitenModus);
        registriereActionListenerDyn("rbBlatt", () -> wechslePanelModus(PanelTyp.BLATT));
        registriereActionListenerDyn("rbUrl",   () -> wechslePanelModus(PanelTyp.URL));
        registriereActionListenerDyn("rbTimer", () -> wechslePanelModus(PanelTyp.TIMER));
        registriereActionListenerDyn("rbDatei", () -> wechslePanelModus(PanelTyp.STATISCHE_DATEI));
        registriereActionListenerDyn("rbStartseite", () -> wechslePanelModus(PanelTyp.TURNIERSTARTSEITE));

        int konfFelderY = modusY + ZEILE_H;
        int konfFelderY2 = konfFelderY + ZEILE_H;

        if (istTimerModus) {
            // ---- Timer-Modus ----
            int aktuellerZoom = ausgewaehlterPanelIndex < panelZooms.size() ? panelZooms.get(ausgewaehlterPanelIndex) : GlobalProperties.DEFAULT_ZOOM;
            fuegeFixedTextEinDyn("lblPanelZoom", I18n.get("webserver.composite.konfig.panel.zoom.label"), 5, konfFelderY, 25, ZEILE_H);
            fuegeEditEinDyn("txtPanelZoom", String.valueOf(aktuellerZoom), 33, konfFelderY, 35, ZEILE_H);
            fuegeAusrichtungsComboBoxen(75, konfFelderY);
            fuegeFixedTextEinDyn("lblTimerHinweis", I18n.get("webserver.composite.konfig.panel.timer.hinweis"), 5, konfFelderY2, 400, ZEILE_H);
        } else if (istStartseitenModus) {
            fuegeFixedTextEinDyn("lblStartseiteHinweis", I18n.get("webserver.composite.konfig.panel.startseite.hinweis"), 5, konfFelderY, 400, ZEILE_H);
        } else if (istUrlModus) {
            // ---- URL-Modus ----
            String aktuelleUrl = ausgewaehlterPanelIndex < panelUrls.size() ? panelUrls.get(ausgewaehlterPanelIndex) : "";
            fuegeFixedTextEinDyn("lblPanelUrl", I18n.get("webserver.composite.konfig.panel.url.label"), 5, konfFelderY, 20, ZEILE_H);
            fuegeEditEinDyn("tfUrl", aktuelleUrl, 28, konfFelderY, 300, ZEILE_H);
            int aktuellerZoom = ausgewaehlterPanelIndex < panelZooms.size() ? panelZooms.get(ausgewaehlterPanelIndex) : GlobalProperties.DEFAULT_ZOOM;
            fuegeFixedTextEinDyn("lblPanelZoom", I18n.get("webserver.composite.konfig.panel.zoom.label"), 333, konfFelderY, 25, ZEILE_H);
            fuegeEditEinDyn("txtPanelZoom", String.valueOf(aktuellerZoom), 361, konfFelderY, 35, ZEILE_H);
            fuegeFixedTextEinDyn("lblUrlHinweis", I18n.get("webserver.composite.konfig.panel.url.hinweis"), 5, konfFelderY2, 400, ZEILE_H);
        } else if (istDateiModus) {
            // ---- Lokale statische Datei ----
            String aktuelleDatei = ausgewaehlterPanelIndex < panelDateien.size() ? panelDateien.get(ausgewaehlterPanelIndex) : "";
            fuegeFixedTextEinDyn("lblPanelDatei", I18n.get("webserver.composite.konfig.panel.datei.label"), 5, konfFelderY, 25, ZEILE_H);
            fuegeEditEinDyn("tfUrl", aktuelleDatei, 33, konfFelderY, 230, ZEILE_H);
            fuegeButtonEinDyn("btnPanelDateiAuswaehlen", "...", 266, konfFelderY, 30, ZEILE_H);
            registriereActionListenerDyn("btnPanelDateiAuswaehlen", this::waehleLokaleDatei);
            int aktuellerZoom = ausgewaehlterPanelIndex < panelZooms.size() ? panelZooms.get(ausgewaehlterPanelIndex) : GlobalProperties.DEFAULT_ZOOM;
            fuegeFixedTextEinDyn("lblPanelZoom", I18n.get("webserver.composite.konfig.panel.zoom.label"), 300, konfFelderY, 25, ZEILE_H);
            fuegeEditEinDyn("txtPanelZoom", String.valueOf(aktuellerZoom), 328, konfFelderY, 35, ZEILE_H);
            fuegeFixedTextEinDyn("lblDateiHinweis", I18n.get("webserver.composite.konfig.panel.datei.hinweis"), 5, konfFelderY2, 400, ZEILE_H);
        } else {
            // ---- Blatt-Modus ----
            String aktuellesSheet = ausgewaehlterPanelIndex < panelSheets.size() ? panelSheets.get(ausgewaehlterPanelIndex) : "";
            fuegeFixedTextEinDyn("lblPanelSheet", I18n.get("webserver.composite.konfig.panel.sheet.label"), 5, konfFelderY, 25, ZEILE_H);
            fuegeComboBoxEinDyn("cbPanelSheet", ladeComboBoxItems(), 33, konfFelderY, 130, ZEILE_H, aktuellesSheet);

            int aktuellerZoom = ausgewaehlterPanelIndex < panelZooms.size() ? panelZooms.get(ausgewaehlterPanelIndex) : GlobalProperties.DEFAULT_ZOOM;
            fuegeFixedTextEinDyn("lblPanelZoom", I18n.get("webserver.composite.konfig.panel.zoom.label"), 170, konfFelderY, 25, ZEILE_H);
            fuegeEditEinDyn("txtPanelZoom", String.valueOf(aktuellerZoom), 198, konfFelderY, 30, ZEILE_H);

            fuegeAusrichtungsComboBoxen(232, konfFelderY);

            boolean aktuellBlattnameAnzeigen = ausgewaehlterPanelIndex < panelBlattnameAnzeigen.size() && panelBlattnameAnzeigen.get(ausgewaehlterPanelIndex);
            fuegeCheckBoxEinDyn("cbPanelBlattnameAnzeigen", I18n.get("webserver.composite.konfig.panel.blattname.label"), 5, konfFelderY2, 150, ZEILE_H, aktuellBlattnameAnzeigen);
            int aktuellerSichtbarerAnteil = ausgewaehlterPanelIndex < panelSichtbareTabellenanteile.size()
                    ? panelSichtbareTabellenanteile.get(ausgewaehlterPanelIndex)
                    : GlobalProperties.DEFAULT_SICHTBARER_TABELLENANTEIL;
            fuegeFixedTextEinDyn("lblPanelSichtbar", I18n.get("webserver.composite.konfig.panel.sichtbar.label"), 170, konfFelderY2, 35, ZEILE_H);
            fuegeEditEinDyn("txtPanelSichtbar", String.valueOf(aktuellerSichtbarerAnteil), 208, konfFelderY2, 25, ZEILE_H);
        }

        // ---- Layout-Vorschau ----
        int vorschauLabelY = konfFelderY2 + ZEILE_H + VORSCHAU_LABEL_ABSTAND;
        fuegeFixedTextEinDyn("lblVorschau", I18n.get("webserver.composite.konfig.bereich.vorschau"), 5, vorschauLabelY, 200, ZEILE_H);

        int vorschauY = vorschauLabelY + ZEILE_H;
        zeichneKnotenVorschau(wurzel, VORSCHAU_X, vorschauY, VORSCHAU_BREITE, VORSCHAU_HOEHE, new AtomicInteger());
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
        panelSichtbareTabellenanteile.add(GlobalProperties.DEFAULT_SICHTBARER_TABELLENANTEIL);
        panelHAlign.add(PanelAusrichtung.KEIN);
        panelVAlign.add(PanelAusrichtung.KEIN);
        panelBlattnameAnzeigen.add(Boolean.FALSE);
        panelTypen.add(PanelTyp.BLATT);
        panelUrls.add("");
        panelDateien.add("");
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
        panelSichtbareTabellenanteile.add(GlobalProperties.DEFAULT_SICHTBARER_TABELLENANTEIL);
        panelHAlign.add(PanelAusrichtung.KEIN);
        panelVAlign.add(PanelAusrichtung.KEIN);
        panelBlattnameAnzeigen.add(Boolean.FALSE);
        panelTypen.add(PanelTyp.BLATT);
        panelUrls.add("");
        panelDateien.add("");
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
        panelSichtbareTabellenanteile.remove(zuLoeschenderIndex);
        panelHAlign.remove(zuLoeschenderIndex);
        panelVAlign.remove(zuLoeschenderIndex);
        panelBlattnameAnzeigen.remove(zuLoeschenderIndex);
        panelTypen.remove(zuLoeschenderIndex);
        panelUrls.remove(zuLoeschenderIndex);
        panelDateien.remove(zuLoeschenderIndex);
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

    // ---- Übernehmen / OK-Klick ----

    private void beimUebernehmenKlick() {
        speicherePanelKonfiguration();
        try {
            var eintrag = validiereUndBaue();
            if (anwendenCallback != null) {
                anwendenCallback.accept(eintrag);
            }
        } catch (UngueltigeEingabeException e) {
            MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("webserver.composite.konfig.fehler.titel"))
                    .message(e.getMessage())
                    .show();
        }
        // Dialog bleibt offen
    }

    private void beimOkKlick() {
        speicherePanelKonfiguration();
        try {
            ergebnis = validiereUndBaue();
            if (anwendenCallback != null) {
                anwendenCallback.accept(ergebnis);
            }
            xDialog.endExecute();
        } catch (UngueltigeEingabeException e) {
            MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("webserver.composite.konfig.fehler.titel"))
                    .message(e.getMessage())
                    .show();
        }
    }

    private void speicherePanelKonfiguration() {
        if (ausgewaehlterPanelIndex < 0) return;

        wurzel = speichereSplitAnteil("txtSplitBreite", "H", wurzel);
        wurzel = speichereSplitAnteil("txtSplitHoehe", "V", wurzel);

        // Modus aus RadioButtons lesen und speichern (Priorität: Startseite > Datei > Timer > URL > Blatt)
        boolean istDatei = false;
        boolean istUrl = false;
        if (ausgewaehlterPanelIndex < panelTypen.size()) {
            XControl rbStartseiteCtrl = xcc.getControl("rbStartseite");
            XControl rbDateiCtrl = xcc.getControl("rbDatei");
            XControl rbTimerCtrl = xcc.getControl("rbTimer");
            XControl rbUrlCtrl   = xcc.getControl("rbUrl");
            boolean istStartseite = rbStartseiteCtrl != null && Lo.qi(XRadioButton.class, rbStartseiteCtrl).getState();
            istDatei = !istStartseite && rbDateiCtrl != null && Lo.qi(XRadioButton.class, rbDateiCtrl).getState();
            boolean istTimer = !istStartseite && !istDatei && rbTimerCtrl != null && Lo.qi(XRadioButton.class, rbTimerCtrl).getState();
            istUrl = !istStartseite && !istDatei && !istTimer && rbUrlCtrl != null && Lo.qi(XRadioButton.class, rbUrlCtrl).getState();
            panelTypen.set(ausgewaehlterPanelIndex,
                    istStartseite ? PanelTyp.TURNIERSTARTSEITE : istDatei ? PanelTyp.STATISCHE_DATEI : istTimer ? PanelTyp.TIMER : istUrl ? PanelTyp.URL : PanelTyp.BLATT);
        }

        // URL-/Datei-Feld lesen und speichern (unabhängig vom Modus – Wert bleibt beim Moduswechsel
        // innerhalb desselben Feldtyps erhalten). Eigene Listen für URL und Datei, damit ein Wechsel
        // zwischen den beiden Modi nicht den Text des jeweils anderen Modus übernimmt.
        XControl tfUrlCtrl = xcc.getControl("tfUrl");
        if (tfUrlCtrl != null) {
            String text = Lo.qi(XTextComponent.class, tfUrlCtrl).getText().trim();
            if (istDatei && ausgewaehlterPanelIndex < panelDateien.size()) {
                panelDateien.set(ausgewaehlterPanelIndex, text);
            } else if (istUrl && ausgewaehlterPanelIndex < panelUrls.size()) {
                panelUrls.set(ausgewaehlterPanelIndex, text);
            }
        }

        // Blatt-Felder lesen und speichern
        XControl sheetCtrl = xcc.getControl("cbPanelSheet");
        XControl zoomCtrl = xcc.getControl("txtPanelZoom");
        XControl sichtbarCtrl = xcc.getControl("txtPanelSichtbar");
        XControl hAlignCtrl = xcc.getControl("cbPanelHAlign");
        XControl vAlignCtrl = xcc.getControl("cbPanelVAlign");
        XControl blattnameCtrl = xcc.getControl("cbPanelBlattnameAnzeigen");
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
        if (sichtbarCtrl != null && ausgewaehlterPanelIndex < panelSichtbareTabellenanteile.size()) {
            try {
                int sichtbar = Integer.parseInt(Lo.qi(XTextComponent.class, sichtbarCtrl).getText().trim());
                panelSichtbareTabellenanteile.set(ausgewaehlterPanelIndex, sichtbar);
            } catch (NumberFormatException ignored) {}
        }
        if (hAlignCtrl != null && ausgewaehlterPanelIndex < panelHAlign.size()) {
            String label = Lo.qi(XTextComponent.class, hAlignCtrl).getText().trim();
            panelHAlign.set(ausgewaehlterPanelIndex, hAlignLabelZuKey(label));
        }
        if (vAlignCtrl != null && ausgewaehlterPanelIndex < panelVAlign.size()) {
            String label = Lo.qi(XTextComponent.class, vAlignCtrl).getText().trim();
            panelVAlign.set(ausgewaehlterPanelIndex, vAlignLabelZuKey(label));
        }
        if (blattnameCtrl != null && ausgewaehlterPanelIndex < panelBlattnameAnzeigen.size()) {
            panelBlattnameAnzeigen.set(ausgewaehlterPanelIndex, Lo.qi(XCheckBox.class, blattnameCtrl).getState() == 1);
        }
    }

    /** Speichert den aktuellen Panel-Zustand und wechselt den Modus, dann baut die Felder neu auf. */
    private void wechslePanelModus(PanelTyp neuerTyp) {
        speicherePanelKonfiguration();
        if (ausgewaehlterPanelIndex < panelTypen.size()) {
            panelTypen.set(ausgewaehlterPanelIndex, neuerTyp);
        }
        aktualisiereUndFange();
    }

    private void waehleLokaleDatei() {
        XControl tfUrlCtrl = xcc.getControl("tfUrl");
        String aktuellerPfad = tfUrlCtrl != null ? Lo.qi(XTextComponent.class, tfUrlCtrl).getText().trim() : "";
        try {
            String pfad = oeffneLokaleDateiPicker(aktuellerPfad);
            if (pfad == null) {
                return;
            }
            if (tfUrlCtrl != null) {
                Lo.qi(XTextComponent.class, tfUrlCtrl).setText(pfad);
            }
            if (ausgewaehlterPanelIndex >= 0 && ausgewaehlterPanelIndex < panelDateien.size()) {
                panelDateien.set(ausgewaehlterPanelIndex, pfad);
            }
        } catch (com.sun.star.uno.Exception e) {
            logger.warn("Datei-Picker konnte nicht geöffnet werden: {}", e.getMessage(), e);
            MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("webserver.composite.konfig.fehler.titel"))
                    .message(I18n.get("webserver.composite.konfig.panel.datei.fehler.ungueltig"))
                    .show();
        }
    }

    private String oeffneLokaleDateiPicker(String aktuellerPfad) throws com.sun.star.uno.Exception {
        XFilePicker3 picker = FilePicker.createWithMode(xContext, TemplateDescription.FILEOPEN_SIMPLE);
        picker.setTitle(I18n.get("webserver.composite.konfig.panel.datei.label"));
        if (aktuellerPfad != null && !aktuellerPfad.isBlank()) {
            Path pfad = pfadAusDateiText(aktuellerPfad);
            Path ordner = Files.isDirectory(pfad) ? pfad : pfad.getParent();
            if (ordner != null) {
                picker.setDisplayDirectory(ordner.toUri().toString());
            }
        }
        if (picker.execute() != ExecutableDialogResults.OK) {
            return null;
        }
        String[] dateien = picker.getFiles();
        if (dateien.length == 0) {
            return null;
        }
        return pfadAusDateiText(dateien[0]).toString();
    }

    private static Path pfadAusDateiText(String datei) {
        return datei.trim().startsWith("file:")
                ? Paths.get(URI.create(datei.trim()))
                : Path.of(datei.trim());
    }

    private SplitKnoten speichereSplitAnteil(String controlName, String richtung, SplitKnoten aktuellerBaum) {
        XControl splitCtrl = xcc.getControl(controlName);
        if (splitCtrl == null) {
            return aktuellerBaum;
        }
        try {
            String text = Lo.qi(XTextComponent.class, splitCtrl).getText().trim();
            int share = text.isEmpty()
                    ? restShareInSplit(aktuellerBaum, ausgewaehlterPanelIndex, richtung)
                    : Integer.parseInt(text);
            if (share >= 1 && share <= 99) {
                return aktualisierePanelShareInSplit(aktuellerBaum, ausgewaehlterPanelIndex, richtung, share);
            }
        } catch (NumberFormatException ignored) {}
        return aktuellerBaum;
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

        // Name (optional, leer erlaubt)
        XControl nameCtrl = xcc.getControl("txtName");
        String name = nameCtrl != null ? Lo.qi(XTextComponent.class, nameCtrl).getText().trim() : "";

        // Header/Footer global rendern – Default true (auch bei fehlendem Control)
        XControl mitHeaderFooterCtrl = xcc.getControl("cbMitHeaderFooter");
        boolean mitHeaderFooter = mitHeaderFooterCtrl == null
                || Lo.qi(XCheckBox.class, mitHeaderFooterCtrl).getState() == 1;

        RandKonfiguration rand = leseRandKonfiguration();

        // Panels
        if (panelSheets.isEmpty()) {
            throw new UngueltigeEingabeException(I18n.get("webserver.composite.konfig.fehler.kein.panel"));
        }
        List<PanelEintragRoh> panels = new ArrayList<>();
        for (int i = 0; i < panelSheets.size(); i++) {
            PanelTyp panelTyp = i < panelTypen.size() ? panelTypen.get(i) : PanelTyp.BLATT;
            String pHAlign = i < panelHAlign.size() ? panelHAlign.get(i) : PanelAusrichtung.KEIN;
            String pVAlign = i < panelVAlign.size() ? panelVAlign.get(i) : PanelAusrichtung.KEIN;
            if (panelTyp == PanelTyp.TIMER) {
                int pZoom = i < panelZooms.size() ? panelZooms.get(i) : GlobalProperties.DEFAULT_ZOOM;
                panels.add(new PanelEintragRoh(PanelTyp.TIMER, "", pZoom,
                        GlobalProperties.DEFAULT_SICHTBARER_TABELLENANTEIL, pHAlign, pVAlign, false, ""));
            } else if (panelTyp == PanelTyp.URL) {
                String url = i < panelUrls.size() ? panelUrls.get(i) : "";
                String urlFehler = validiereUrl(url);
                if (urlFehler != null) {
                    throw new UngueltigeEingabeException(urlFehler);
                }
                int pZoom = i < panelZooms.size() ? panelZooms.get(i) : GlobalProperties.DEFAULT_ZOOM;
                panels.add(new PanelEintragRoh(PanelTyp.URL, "", pZoom,
                        GlobalProperties.DEFAULT_SICHTBARER_TABELLENANTEIL,
                        PanelAusrichtung.KEIN, PanelAusrichtung.KEIN, false, url));
            } else if (panelTyp == PanelTyp.STATISCHE_DATEI) {
                String datei = i < panelDateien.size() ? panelDateien.get(i) : "";
                String dateiFehler = validiereLokaleDatei(datei);
                if (dateiFehler != null) {
                    throw new UngueltigeEingabeException(dateiFehler);
                }
                int pZoom = i < panelZooms.size() ? panelZooms.get(i) : GlobalProperties.DEFAULT_ZOOM;
                panels.add(new PanelEintragRoh(PanelTyp.STATISCHE_DATEI, "", pZoom,
                        GlobalProperties.DEFAULT_SICHTBARER_TABELLENANTEIL,
                        PanelAusrichtung.KEIN, PanelAusrichtung.KEIN, false, datei));
            } else if (panelTyp == PanelTyp.TURNIERSTARTSEITE) {
                panels.add(new PanelEintragRoh(PanelTyp.TURNIERSTARTSEITE, "", GlobalProperties.DEFAULT_ZOOM,
                        GlobalProperties.DEFAULT_SICHTBARER_TABELLENANTEIL,
                        PanelAusrichtung.KEIN, PanelAusrichtung.KEIN, false, ""));
            } else {
                int pZoom = i < panelZooms.size() ? panelZooms.get(i) : GlobalProperties.DEFAULT_ZOOM;
                int pSichtbarerTabellenAnteil = i < panelSichtbareTabellenanteile.size()
                        ? panelSichtbareTabellenanteile.get(i)
                        : GlobalProperties.DEFAULT_SICHTBARER_TABELLENANTEIL;
                if (pSichtbarerTabellenAnteil < 10 || pSichtbarerTabellenAnteil > 100) {
                    throw new UngueltigeEingabeException(I18n.get("webserver.composite.konfig.fehler.sichtbar.ungueltig"));
                }
                boolean pBlattnameAnzeigen = i < panelBlattnameAnzeigen.size() && panelBlattnameAnzeigen.get(i);
                panels.add(new PanelEintragRoh(PanelTyp.BLATT, panelSheets.get(i), pZoom,
                        pSichtbarerTabellenAnteil, pHAlign, pVAlign, pBlattnameAnzeigen, ""));
            }
        }

        // Layout serialisieren
        var gson = new GsonBuilder()
                .registerTypeAdapter(SplitKnoten.class, new SplitKnotenAdapter())
                .create();
        String layoutJson = gson.toJson(wurzel, SplitKnoten.class);

        return new CompositeViewEintragRoh(port, name, aktiv, zoom, mitHeaderFooter, layoutJson, panels, rand);
    }

    /** Liest und validiert die Rand-Controls (Dicke 0–{@value RandKonfiguration#MAX_DICKE}, Transparenz 0–100). */
    private RandKonfiguration leseRandKonfiguration() throws UngueltigeEingabeException {
        XControl dickeCtrl = xcc.getControl("txtRandDicke");
        String dickeStr = dickeCtrl != null ? Lo.qi(XTextComponent.class, dickeCtrl).getText().trim() : "";
        int dicke;
        try {
            dicke = dickeStr.isEmpty() ? 0 : Integer.parseInt(dickeStr);
        } catch (NumberFormatException e) {
            throw new UngueltigeEingabeException(I18n.get("webserver.composite.konfig.rand.fehler.dicke.ungueltig"));
        }
        if (dicke < 0 || dicke > RandKonfiguration.MAX_DICKE) {
            throw new UngueltigeEingabeException(I18n.get("webserver.composite.konfig.rand.fehler.dicke.ungueltig"));
        }

        XControl artCtrl = xcc.getControl("cbRandArt");
        String art = artCtrl != null ? randArtLabelZuKey(Lo.qi(XTextComponent.class, artCtrl).getText().trim())
                : RandKonfiguration.ART_KEIN;

        XControl transpCtrl = xcc.getControl("txtRandTransp");
        String transpStr = transpCtrl != null ? Lo.qi(XTextComponent.class, transpCtrl).getText().trim() : "";
        int transparenz;
        try {
            transparenz = transpStr.isEmpty() ? 0 : Integer.parseInt(transpStr);
        } catch (NumberFormatException e) {
            throw new UngueltigeEingabeException(I18n.get("webserver.composite.konfig.rand.fehler.transparenz.ungueltig"));
        }
        if (transparenz < 0 || transparenz > 100) {
            throw new UngueltigeEingabeException(I18n.get("webserver.composite.konfig.rand.fehler.transparenz.ungueltig"));
        }

        XControl animationCtrl = xcc.getControl("cbRandAnimation");
        String animation = animationCtrl != null
                ? randAnimationLabelZuKey(Lo.qi(XTextComponent.class, animationCtrl).getText().trim())
                : RandKonfiguration.ANIMATION_KEINE;

        return new RandKonfiguration(dicke, art, randFarbe, transparenz, animation);
    }

    // ---- Baum-Operationen (statisch für Testbarkeit) ----

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
        return knoten instanceof SplitBlatt(int panel) && panel == index;
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

    static int panelShareInSplit(SplitKnoten knoten, int panelIndex, String richtung) {
        return panelShareInSplit(knoten, panelIndex, richtung, 100.0, false);
    }

    private static int panelShareInSplit(SplitKnoten knoten, int panelIndex, String richtung,
            double anteil, boolean richtungGefunden) {
        return switch (knoten) {
            case SplitBlatt blatt -> blatt.panel() == panelIndex && richtungGefunden
                    ? (int) Math.round(anteil)
                    : -1;
            case SplitTeilung teilung -> {
                boolean linksEnthaelt = enthaeltPanel(teilung.links(), panelIndex);
                boolean rechtsEnthaelt = enthaeltPanel(teilung.rechts(), panelIndex);
                if (linksEnthaelt) {
                    boolean gleicheRichtung = teilung.richtung().equalsIgnoreCase(richtung);
                    yield panelShareInSplit(teilung.links(), panelIndex, richtung,
                            gleicheRichtung ? anteil * teilung.groesse() / 100.0 : anteil,
                            richtungGefunden || gleicheRichtung);
                }
                if (rechtsEnthaelt) {
                    boolean gleicheRichtung = teilung.richtung().equalsIgnoreCase(richtung);
                    yield panelShareInSplit(teilung.rechts(), panelIndex, richtung,
                            gleicheRichtung ? anteil * (100 - teilung.groesse()) / 100.0 : anteil,
                            richtungGefunden || gleicheRichtung);
                }
                yield -1;
            }
        };
    }

    static int restShareInSplit(SplitKnoten knoten, int panelIndex, String richtung) {
        SplitKnoten richtungsWurzel = richtungsWurzel(knoten, panelIndex, richtung, null);
        if (richtungsWurzel == null) {
            return -1;
        }
        int rest = 100 - summiereAnderePanelShares(richtungsWurzel, panelIndex, richtung, 100.0, false);
        return Math.max(0, Math.min(100, rest));
    }

    static SplitKnoten aktualisierePanelShareInSplit(SplitKnoten knoten, int panelIndex, String richtung, int panelShare) {
        return aktualisierePanelShareInSplitIntern(knoten, panelIndex, richtung, panelShare, 100.0).knoten();
    }

    private static AktualisierteSplitTeilung aktualisierePanelShareInSplitIntern(SplitKnoten knoten,
            int panelIndex, String richtung, int panelShare, double verfuegbarerAnteil) {
        if (panelShare < 1 || panelShare > 99) {
            throw new IllegalArgumentException("Panel-Anteil muss zwischen 1 und 99 liegen");
        }
        return switch (knoten) {
            case SplitBlatt blatt -> new AktualisierteSplitTeilung(blatt, false);
            case SplitTeilung teilung -> {
                boolean linksEnthaelt = enthaeltPanel(teilung.links(), panelIndex);
                boolean rechtsEnthaelt = enthaeltPanel(teilung.rechts(), panelIndex);
                if (linksEnthaelt) {
                    boolean gleicheRichtung = teilung.richtung().equalsIgnoreCase(richtung);
                    var linksAktualisiert = aktualisierePanelShareInSplitIntern(
                            teilung.links(), panelIndex, richtung, panelShare,
                            gleicheRichtung ? verfuegbarerAnteil * teilung.groesse() / 100.0 : verfuegbarerAnteil);
                    if (linksAktualisiert.aktualisiert()) {
                        yield new AktualisierteSplitTeilung(
                                new SplitTeilung(teilung.richtung(), teilung.groesse(), linksAktualisiert.knoten(), teilung.rechts()),
                                true);
                    }
                    if (gleicheRichtung) {
                        int lokalerAnteil = lokalerSplitAnteil(panelShare, verfuegbarerAnteil);
                        yield new AktualisierteSplitTeilung(
                                new SplitTeilung(teilung.richtung(), lokalerAnteil, teilung.links(), teilung.rechts()),
                                true);
                    }
                }
                if (rechtsEnthaelt) {
                    boolean gleicheRichtung = teilung.richtung().equalsIgnoreCase(richtung);
                    var rechtsAktualisiert = aktualisierePanelShareInSplitIntern(
                            teilung.rechts(), panelIndex, richtung, panelShare,
                            gleicheRichtung ? verfuegbarerAnteil * (100 - teilung.groesse()) / 100.0 : verfuegbarerAnteil);
                    if (rechtsAktualisiert.aktualisiert()) {
                        yield new AktualisierteSplitTeilung(
                                new SplitTeilung(teilung.richtung(), teilung.groesse(), teilung.links(), rechtsAktualisiert.knoten()),
                                true);
                    }
                    if (gleicheRichtung) {
                        int lokalerAnteil = lokalerSplitAnteil(panelShare, verfuegbarerAnteil);
                        yield new AktualisierteSplitTeilung(
                                new SplitTeilung(teilung.richtung(), 100 - lokalerAnteil, teilung.links(), teilung.rechts()),
                                true);
                    }
                }
                yield new AktualisierteSplitTeilung(teilung, false);
            }
        };
    }

    private static int lokalerSplitAnteil(int panelShare, double verfuegbarerAnteil) {
        int lokalerAnteil = (int) Math.round(panelShare * 100.0 / verfuegbarerAnteil);
        if (lokalerAnteil < 1 || lokalerAnteil > 99) {
            throw new IllegalArgumentException("Panel-Anteil passt nicht in den vorhandenen Split");
        }
        return lokalerAnteil;
    }

    private static SplitKnoten richtungsWurzel(SplitKnoten knoten, int panelIndex, String richtung,
            SplitKnoten aktuelleRichtungsWurzel) {
        return switch (knoten) {
            case SplitBlatt blatt -> blatt.panel() == panelIndex ? aktuelleRichtungsWurzel : null;
            case SplitTeilung teilung -> {
                boolean gleicheRichtung = teilung.richtung().equalsIgnoreCase(richtung);
                SplitKnoten naechsteRichtungsWurzel = gleicheRichtung && aktuelleRichtungsWurzel == null
                        ? teilung
                        : aktuelleRichtungsWurzel;
                if (enthaeltPanel(teilung.links(), panelIndex)) {
                    yield richtungsWurzel(teilung.links(), panelIndex, richtung, naechsteRichtungsWurzel);
                }
                if (enthaeltPanel(teilung.rechts(), panelIndex)) {
                    yield richtungsWurzel(teilung.rechts(), panelIndex, richtung, naechsteRichtungsWurzel);
                }
                yield null;
            }
        };
    }

    private static int summiereAnderePanelShares(SplitKnoten knoten, int panelIndex, String richtung,
            double anteil, boolean richtungGefunden) {
        return switch (knoten) {
            case SplitBlatt blatt -> blatt.panel() != panelIndex && richtungGefunden
                    ? (int) Math.round(anteil)
                    : 0;
            case SplitTeilung teilung -> {
                boolean gleicheRichtung = teilung.richtung().equalsIgnoreCase(richtung);
                int links = summiereAnderePanelShares(teilung.links(), panelIndex, richtung,
                        gleicheRichtung ? anteil * teilung.groesse() / 100.0 : anteil,
                        richtungGefunden || gleicheRichtung);
                int rechts = summiereAnderePanelShares(teilung.rechts(), panelIndex, richtung,
                        gleicheRichtung ? anteil * (100 - teilung.groesse()) / 100.0 : anteil,
                        richtungGefunden || gleicheRichtung);
                yield links + rechts;
            }
        };
    }

    private static boolean enthaeltPanel(SplitKnoten knoten, int panelIndex) {
        return switch (knoten) {
            case SplitBlatt blatt -> blatt.panel() == panelIndex;
            case SplitTeilung teilung -> enthaeltPanel(teilung.links(), panelIndex)
                    || enthaeltPanel(teilung.rechts(), panelIndex);
        };
    }

    private record AktualisierteSplitTeilung(SplitKnoten knoten, boolean aktualisiert) {
    }

    /**
     * Prüft ob die URL gültig ist: nicht leer, Schema muss http oder https sein.
     *
     * @return Fehlermeldung oder {@code null} wenn die URL gültig ist
     */
    private static String validiereUrl(String url) {
        if (url == null || url.isBlank()) {
            return I18n.get("webserver.composite.konfig.panel.url.fehler.leer");
        }
        try {
            var uri = new URI(url);
            var schema = uri.getScheme();
            if (!"http".equalsIgnoreCase(schema) && !"https".equalsIgnoreCase(schema)) {
                return I18n.get("webserver.composite.konfig.panel.url.fehler.schema");
            }
        } catch (URISyntaxException e) {
            return I18n.get("webserver.composite.konfig.panel.url.fehler.ungueltig");
        }
        return null;
    }

    /**
     * Prüft lokale statische Dateien: nicht leer, gültiger Pfad/file-URI, lesbare reguläre Datei.
     */
    private static String validiereLokaleDatei(String datei) {
        if (datei == null || datei.isBlank()) {
            return I18n.get("webserver.composite.konfig.panel.datei.fehler.leer");
        }
        try {
            Path pfad = pfadAusDateiText(datei);
            if (!Files.isRegularFile(pfad) || !Files.isReadable(pfad)) {
                return I18n.get("webserver.composite.konfig.panel.datei.fehler.nicht_lesbar", pfad);
            }
        } catch (IllegalArgumentException e) {
            return I18n.get("webserver.composite.konfig.panel.datei.fehler.ungueltig");
        }
        return null;
    }

    // ---- Ausrichtungs-ComboBoxen ----

    /**
     * Baut die beiden Ausrichtungs-ComboBoxen (horizontal + vertikal) für das aktuelle Panel
     * an Position {@code (xStart, y)}. Beide ComboBoxen sind 60 dp breit und folgen jeweils
     * einem kurzen Label.
     */
    private void fuegeAusrichtungsComboBoxen(int xStart, int y) throws com.sun.star.uno.Exception {
        String aktuellH = ausgewaehlterPanelIndex < panelHAlign.size()
                ? panelHAlign.get(ausgewaehlterPanelIndex) : PanelAusrichtung.KEIN;
        String aktuellV = ausgewaehlterPanelIndex < panelVAlign.size()
                ? panelVAlign.get(ausgewaehlterPanelIndex) : PanelAusrichtung.KEIN;

        fuegeFixedTextEinDyn("lblPanelHAlign", I18n.get("webserver.composite.konfig.panel.halign.label"),
                xStart, y, 12, ZEILE_H);
        fuegeComboBoxEinDyn("cbPanelHAlign", H_ALIGN_LABELS, xStart + 14, y, 55, ZEILE_H, hAlignKeyZuLabel(aktuellH));

        int xV = xStart + 75;
        fuegeFixedTextEinDyn("lblPanelVAlign", I18n.get("webserver.composite.konfig.panel.valign.label"),
                xV, y, 12, ZEILE_H);
        fuegeComboBoxEinDyn("cbPanelVAlign", V_ALIGN_LABELS, xV + 14, y, 55, ZEILE_H, vAlignKeyZuLabel(aktuellV));
    }

    private String[] hAlignLabels() {
        return new String[] {
                I18n.get("webserver.composite.konfig.panel.align.kein"),
                I18n.get("webserver.composite.konfig.panel.halign.links"),
                I18n.get("webserver.composite.konfig.panel.halign.mitte"),
                I18n.get("webserver.composite.konfig.panel.halign.rechts"),
        };
    }

    private String[] vAlignLabels() {
        return new String[] {
                I18n.get("webserver.composite.konfig.panel.align.kein"),
                I18n.get("webserver.composite.konfig.panel.valign.oben"),
                I18n.get("webserver.composite.konfig.panel.valign.mitte"),
                I18n.get("webserver.composite.konfig.panel.valign.unten"),
        };
    }

    private final String[] H_ALIGN_LABELS = hAlignLabels();
    private final String[] V_ALIGN_LABELS = vAlignLabels();

    private String hAlignKeyZuLabel(String key) {
        return switch (PanelAusrichtung.normiereHorizontal(key)) {
            case PanelAusrichtung.H_LINKS  -> H_ALIGN_LABELS[1];
            case PanelAusrichtung.H_MITTE  -> H_ALIGN_LABELS[2];
            case PanelAusrichtung.H_RECHTS -> H_ALIGN_LABELS[3];
            default -> H_ALIGN_LABELS[0];
        };
    }

    private String vAlignKeyZuLabel(String key) {
        return switch (PanelAusrichtung.normiereVertikal(key)) {
            case PanelAusrichtung.V_OBEN  -> V_ALIGN_LABELS[1];
            case PanelAusrichtung.V_MITTE -> V_ALIGN_LABELS[2];
            case PanelAusrichtung.V_UNTEN -> V_ALIGN_LABELS[3];
            default -> V_ALIGN_LABELS[0];
        };
    }

    private String hAlignLabelZuKey(String label) {
        if (label == null) return PanelAusrichtung.KEIN;
        if (label.equals(H_ALIGN_LABELS[1])) return PanelAusrichtung.H_LINKS;
        if (label.equals(H_ALIGN_LABELS[2])) return PanelAusrichtung.H_MITTE;
        if (label.equals(H_ALIGN_LABELS[3])) return PanelAusrichtung.H_RECHTS;
        return PanelAusrichtung.KEIN;
    }

    private String vAlignLabelZuKey(String label) {
        if (label == null) return PanelAusrichtung.KEIN;
        if (label.equals(V_ALIGN_LABELS[1])) return PanelAusrichtung.V_OBEN;
        if (label.equals(V_ALIGN_LABELS[2])) return PanelAusrichtung.V_MITTE;
        if (label.equals(V_ALIGN_LABELS[3])) return PanelAusrichtung.V_UNTEN;
        return PanelAusrichtung.KEIN;
    }

    /** Liefert das kompakte Vorschau-Suffix für die Ausrichtung, z.B. {@code " H:m V:o"} – leer wenn beide „kein". */
    private String ausrichtungsSuffix(int panelIndex) {
        String h = panelIndex < panelHAlign.size() ? panelHAlign.get(panelIndex) : PanelAusrichtung.KEIN;
        String v = panelIndex < panelVAlign.size() ? panelVAlign.get(panelIndex) : PanelAusrichtung.KEIN;
        boolean hAktiv = !PanelAusrichtung.KEIN.equals(h);
        boolean vAktiv = !PanelAusrichtung.KEIN.equals(v);
        if (!hAktiv && !vAktiv) return "";
        StringBuilder sb = new StringBuilder();
        if (hAktiv) sb.append(" H:").append(h.charAt(0));
        if (vAktiv) sb.append(" V:").append(v.charAt(0));
        return sb.toString();
    }

    // ---- ComboBox-Items ----

    private String[] ladeComboBoxItems() {
        List<String> geoeffnet = new ArrayList<>();
        XSpreadsheetDocument doc = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
        if (doc != null) {
            geoeffnet.addAll(Arrays.asList(doc.getSheets().getElementNames()));
            geoeffnet.sort(null);
        }
        Set<String> geoeffnetSet = new HashSet<>(geoeffnet);
        List<String> festeIds = Arrays.stream(komboBoxItems)
                .filter(t -> !geoeffnetSet.contains(t))
                .sorted()
                .collect(Collectors.toList());
        var result = new ArrayList<>(geoeffnet);
        result.addAll(festeIds);
        return result.toArray(String[]::new);
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

    private void fuegeComboBoxEin(String name, String[] items, int x, int y, int w, int h, String selected)
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
        props.setPropertyValue("LineCount",      (short) 20);
        cont.insertByName(name, model);
    }

    /** Fügt ein farbig hinterlegtes Vorschau-Label ein (z.B. für Rand-/Textfarbe) und liefert dessen Properties. */
    private XPropertySet fuegeColorVorschauEin(String name, int farbe, int x, int y, int w, int h)
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

    private void fuegeButtonEinDyn(String name, String label, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        fuegeButtonEin(name, label, x, y, w, h, (short) PushButtonType.STANDARD_value);
        dynamischeControlNamen.add(name);
    }

    private void fuegeEditEinDyn(String name, String text, int x, int y, int w, int h)
            throws com.sun.star.uno.Exception {
        fuegeEditEin(name, text, x, y, w, h);
        dynamischeControlNamen.add(name);
    }

    private void fuegeCheckBoxEinDyn(String name, String label, int x, int y, int w, int h, boolean checked)
            throws com.sun.star.uno.Exception {
        fuegeCheckBoxEin(name, label, x, y, w, h, checked);
        dynamischeControlNamen.add(name);
    }

    private void fuegeRadioButtonEinDyn(String name, String label, int x, int y, int w, int h, boolean selected)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlRadioButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",     label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        props.setPropertyValue("State",     (short) (selected ? 1 : 0));
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
        props.setPropertyValue("LineCount",      (short) 20);
        cont.insertByName(name, model);
        dynamischeControlNamen.add(name);
    }

    /**
     * Fügt einen flachen Button als dynamisches Control ein.
     * Dient als anklickbares Tile für die Layout-Vorschau.
     *
     * @param aktiv   Ob dieses Panel aktuell ausgewählt ist (erhält "▶ "-Prefix)
     * @param tooltip Vollständiger Sheet-Name für den HelpText/Tooltip
     */
    private void fuegeVorschauButtonEinDyn(String name, String label, int x, int y, int w, int h,
            boolean aktiv, String tooltip)
            throws com.sun.star.uno.Exception {
        var model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
        var props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("Label",     aktiv ? "▶ " + label : label);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width",     w);
        props.setPropertyValue("Height",    h);
        if (tooltip != null && !tooltip.isEmpty()) {
            props.setPropertyValue("HelpText", tooltip);
        }
        cont.insertByName(name, model);
        dynamischeControlNamen.add(name);
    }

    /**
     * Zeichnet den Split-Baum rekursiv als beschriftete GroupBox-Kacheln in den Vorschaubereich.
     * <p>
     * Jeder Blattknoten wird als bordiertes Rechteck dargestellt. Das aktive Panel erhält
     * ein "▶ "-Prefix und ist per Mausklick anwählbar.
     * Mindestgröße je Kachel: {@value VORSCHAU_MIN_GROESSE}px.
     *
     * @param zaehler  {@link AtomicInteger} für eindeutige Control-Namen
     */
    private void zeichneKnotenVorschau(SplitKnoten knoten, int x, int y, int w, int h,
            AtomicInteger zaehler)
            throws com.sun.star.uno.Exception {
        w = Math.max(w, VORSCHAU_MIN_GROESSE);
        h = Math.max(h, VORSCHAU_MIN_GROESSE);
        switch (knoten) {
            case SplitBlatt blatt -> {
                PanelTyp panelTyp = blatt.panel() < panelTypen.size() ? panelTypen.get(blatt.panel()) : PanelTyp.BLATT;
                String tooltip;
                String kurzName;
                String suffix;
                if (panelTyp == PanelTyp.URL) {
                    String url = blatt.panel() < panelUrls.size() ? panelUrls.get(blatt.panel()) : "";
                    tooltip = url;
                    kurzName = url != null && url.length() > 22 ? url.substring(0, 20) + "…" : url;
                    suffix = " " + I18n.get("webserver.composite.konfig.panel.vorschau.marker.url");
                } else if (panelTyp == PanelTyp.STATISCHE_DATEI) {
                    String datei = blatt.panel() < panelDateien.size() ? panelDateien.get(blatt.panel()) : "";
                    tooltip = datei;
                    kurzName = datei != null && datei.length() > 22 ? datei.substring(0, 20) + "…" : datei;
                    suffix = " " + I18n.get("webserver.composite.konfig.panel.vorschau.marker.datei");
                } else if (panelTyp == PanelTyp.TIMER) {
                    kurzName = I18n.get("webserver.composite.konfig.panel.modus.timer");
                    tooltip = kurzName;
                    int pZoom = blatt.panel() < panelZooms.size() ? panelZooms.get(blatt.panel()) : GlobalProperties.DEFAULT_ZOOM;
                    suffix = " [" + pZoom + "%" + ausrichtungsSuffix(blatt.panel()) + "]";
                } else if (panelTyp == PanelTyp.TURNIERSTARTSEITE) {
                    kurzName = I18n.get("webserver.composite.konfig.panel.modus.startseite");
                    tooltip = kurzName;
                    suffix = " " + I18n.get("webserver.composite.konfig.panel.vorschau.marker.startseite");
                } else {
                    String sheetName = blatt.panel() < panelSheets.size() ? panelSheets.get(blatt.panel()) : "?";
                    tooltip = sheetName;
                    kurzName = sheetName != null && sheetName.length() > 20 ? sheetName.substring(0, 18) + "…" : sheetName;
                    int pZoom = blatt.panel() < panelZooms.size() ? panelZooms.get(blatt.panel()) : GlobalProperties.DEFAULT_ZOOM;
                    suffix = " [" + pZoom + "%" + ausrichtungsSuffix(blatt.panel()) + "]";
                }
                String label = "P" + blatt.panel() + ": " + kurzName + suffix;
                boolean istAktiv = blatt.panel() == ausgewaehlterPanelIndex;
                String ctlName = "vorschauBox_" + zaehler.getAndIncrement();
                fuegeVorschauButtonEinDyn(ctlName, label, x, y, w, h, istAktiv, tooltip);
                final int panelId = blatt.panel();
                registriereActionListenerDyn(ctlName, () -> waehlePanel(panelId));
            }
            case SplitTeilung teilung -> {
                if (teilung.istHorizontal()) {
                    int linksBreite  = (int) (w * teilung.groesse() / 100.0f);
                    int rechtsBreite = w - linksBreite;
                    zeichneKnotenVorschau(teilung.links(),  x,              y, linksBreite,  h, zaehler);
                    zeichneKnotenVorschau(teilung.rechts(), x + linksBreite, y, rechtsBreite, h, zaehler);
                } else {
                    int obenHoehe  = (int) (h * teilung.groesse() / 100.0f);
                    int untenHoehe = h - obenHoehe;
                    zeichneKnotenVorschau(teilung.links(),  x, y,             w, obenHoehe,  zaehler);
                    zeichneKnotenVorschau(teilung.rechts(), x, y + obenHoehe, w, untenHoehe, zaehler);
                }
            }
        }
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
