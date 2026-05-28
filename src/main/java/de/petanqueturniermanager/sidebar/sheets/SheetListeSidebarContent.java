package de.petanqueturniermanager.sidebar.sheets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XCallback;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XRequestCallback;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNamed;
import com.sun.star.frame.XController;
import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebar;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.perflog.PerfLog;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;
import de.petanqueturniermanager.sidebar.layout.FuellendeControlLayout;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.BlattKnoten;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.GruppenKopf;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.SpieltagKopf;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.UnterGruppenKopf;

/**
 * Listet alle PTM-verwalteten Tabellenblätter als kollapsierbaren Baum nach Turniersystem.
 * Klick auf einen Gruppen-Header klappt die Gruppe auf/zu.
 * Klick auf ein Blatt aktiviert das entsprechende Sheet.
 *
 * @author Michael Massee
 */
public class SheetListeSidebarContent extends BaseSidebarContent {

    private static final Logger logger = LogManager.getLogger(SheetListeSidebarContent.class);

    private static final int ZEILEN_HOEHE = 22;
    private static final int MIN_HOEHE = 60;

    /** Trennzeichen zwischen Eintrags-Anzeigetexten in der Struktur-Signatur (in keinem Anzeigetext enthalten). */
    private static final char SIGNATUR_TRENNER = '\u0001';
    /** Signatur-Marker für „kein Turnier-Dokument" – verschieden von einer leeren Eintragsliste. */
    private static final String SIGNATUR_KEIN_DOKUMENT = "KEIN_DOKUMENT";

    private XListBox sheetListBox;
    private List<BlattBaumEintrag> baumEintraege;
    private Set<SheetGruppe> kollabierteGruppen;
    private Set<Integer> kollabierteSpielTage;
    private Set<String> kollabierteUnterGruppen;
    private SheetBaumOrganisierer organisierer;
    private String gespeichertesSheet = null;
    /**
     * Signatur der zuletzt <em>tatsächlich angezeigten</em> Blattstruktur (Reihenfolge,
     * Namen, Kollaps-Zustand). Dient dazu, den teuren Vollaufbau
     * ({@code window.dispose()} + Fenster-/ListBox-Neuaufbau) zu überspringen, wenn ein
     * {@code felderAktualisieren}/{@code listeNeuAufbauen}-Trigger feuert, ohne dass sich
     * die Struktur geändert hat (z.B. Ergebniseingabe, Tab-Wechsel – beides ändert keine
     * Blätter). {@code null} = noch nie aufgebaut → Vollaufbau erzwingen.
     */
    private String aktuelleStrukturSignatur = null;
    private XSelectionSupplier selectionSupplier = null;
    /** Kurzfristig {@code true} während programmatischer {@code selectItemPos}-Aufrufe. */
    private volatile boolean unterdrueckeItemListener = false;
    /**
     * Dispatcher zum Ausführen von Item-Aktionen nach dem aktuellen VCL-Event.
     * <p>
     * {@code itemStateChanged} feuert innerhalb von {@code pBox->Select()} – mitten im
     * VCL-Event-Stack. Direktes Aufrufen von {@code setActiveSheet()} oder
     * {@code window.dispose()} aus diesem Kontext führt zu VCL-Re-Entranz und SIGSEGV.
     * {@code AsyncCallback.addCallback()} postet die Aktion via
     * {@code Application::PostUserEvent}, die erst nach Abschluss des aktuellen
     * Events ausgeführt wird.
     */
    private XRequestCallback itemDispatcher;

    /**
     * Synchronisiert die Sidebar-Auswahl wenn der User per Klick auf einen Sheet-Tab wechselt.
     * <p>
     * {@code selectionChanged} feuert innerhalb des VCL-Event-Stacks. Die eigentliche
     * Synchronisierung ({@code selectItemPos}) wird daher via {@code itemDispatcher}
     * (PostUserEvent) auf nach dem laufenden Event verschoben – identisches Muster
     * wie in {@code itemStateChanged}.
     */
    private final XSelectionChangeListener tabWechselListener = new XSelectionChangeListener() {
        @Override
        public void selectionChanged(EventObject event) {
            if (SheetRunner.isRunning() || !isUiReady() || sheetListBox == null || itemDispatcher == null) {
                return;
            }
            int gen = getUiGeneration();
            itemDispatcher.addCallback((XCallback) aData -> {
                if (!isUiAlive(gen) || sheetListBox == null) {
                    return;
                }
                sidebarAuswahlSynchronisieren();
            }, null);
        }

        @Override
        public void disposing(EventObject event) {
            selectionSupplier = null;
        }
    };

    /**
     * Reagiert auf SheetRunner-Zustandswechsel.
     * <p>
     * {@code SheetRunner} ruft {@code benachrichtigeListener()} aus seinem Worker-Thread
     * (siehe {@code SheetRunner.run()}). {@code listBoxAktivierungAktualisieren()} und vor
     * allem {@code listeNeuAufbauen()} ({@code window.dispose()} + Fenster-Neuaufbau) sind
     * VCL-Operationen und dürfen NICHT vom Worker-Thread laufen – das blockiert unter
     * Windows die SolarMutex und friert LibreOffice komplett ein. Die UI-Arbeit wird daher
     * via {@link LoMainThread#post} auf den LO-Main-Thread verschoben. Der Laufzustand wird
     * im Worker-Thread erfasst (zum Benachrichtigungszeitpunkt korrekt), die Aktion erst
     * danach auf dem Main-Thread ausgeführt.
     */
    private final Runnable prozessZustandListener = () -> {
        boolean laeuft = SheetRunner.isRunning();
        var ws = getCurrentSpreadsheet();
        if (ws == null) {
            return;
        }
        LoMainThread.post(ws.getxContext(), () -> {
            if (getCurrentSpreadsheet() == null) {
                return;
            }
            if (laeuft) {
                listBoxAktivierungAktualisieren();
            } else {
                listeNeuAufbauen();
            }
        });
    };

    public SheetListeSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
            XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, xSidebar);
        // sheetListBox wird erst durch felderHinzufuegen() gesetzt — hier loggen war
        // immer null (UR_UNINIT_READ).
        SheetRunner.addStateChangeListener(prozessZustandListener);
        tabWechselListenerRegistrieren(workingSpreadsheet);
    }

    // ── Aufbau ──────────────────────────────────────────────────────────────

    @Override
    protected void felderHinzufuegen() {
        if (itemDispatcher == null) {
            try {
                var xContext = getCurrentSpreadsheet().getxContext();
                var asyncCallback = xContext.getServiceManager()
                        .createInstanceWithContext("com.sun.star.awt.AsyncCallback", xContext);
                itemDispatcher = Lo.qi(XRequestCallback.class, asyncCallback);
            } catch (Exception e) {
                logger.warn("felderHinzufuegen: AsyncCallback-Service nicht verfügbar – Item-Aktionen ohne Defer", e);
            }
        }
        if (organisierer == null) {
            organisierer = new SheetBaumOrganisierer();
        }
        if (kollabierteGruppen == null) {
            kollabierteGruppen = new HashSet<>();
        }
        if (kollabierteSpielTage == null) {
            kollabierteSpielTage = new HashSet<>();
        }
        if (kollabierteUnterGruppen == null) {
            kollabierteUnterGruppen = new HashSet<>();
        }
        if (baumEintraege == null) {
            baumEintraege = new ArrayList<>();
        }
        var xDoc = dokumentOderNull();
        if (xDoc == null) {
            aktuelleStrukturSignatur = SIGNATUR_KEIN_DOKUMENT;
            sheetListeAufbauenLeer();
            return;
        }

        heileVeralteteMetadaten(xDoc);
        baumEintraege = organisierer.baumAufbauen(xDoc, kollabierteGruppen, kollabierteSpielTage, kollabierteUnterGruppen);
        aktuelleStrukturSignatur = signaturAus(baumEintraege);

        if (baumEintraege.isEmpty()) {
            sheetListeAufbauenLeer();
        } else {
            logger.debug("felderHinzufuegen: {} Baum-Einträge gefunden, baue ListBox auf", baumEintraege.size());
            sheetListeAufbauenMitEintraegen();
        }
    }

    private void sheetListeAufbauenLeer() {
        logger.debug("SheetListePanel: kein Turnier-Dokument oder keine PTM-Sheets");
        sheetListBox = null;
        XControl leerLabel = GuiFactory.createLabel(getGuiFactoryCreateParam(),
                I18n.get("sidebar.sheets.leer"),
                new Rectangle(0, 0, 200, 40), null);
        if (leerLabel != null) {
            getLayout().addLayout(new ControlLayout(leerLabel), 1);
        }
    }

    private void sheetListeAufbauenMitEintraegen() {
        Map<String, Object> props = new HashMap<>();
        props.put(GuiFactory.V_SCROLL, true);
        XControl ctrl = GuiFactory.createListBox(
                getGuiFactoryCreateParam(),
                itemListener,
                new Rectangle(0, 0, 200, MIN_HOEHE),
                props);
        if (ctrl == null) {
            logger.error("SheetListePanel: createListBox lieferte null");
            sheetListBox = null;
            return;
        }
        sheetListBox = Lo.qi(XListBox.class, ctrl);
        getLayout().addLayout(new FuellendeControlLayout(ctrl, MIN_HOEHE), 1);
        listBoxBefuellen();
        auswahlWiederherstellen();
        listBoxAktivierungAktualisieren();
    }

    @Override
    public LayoutSize getHeightForWidth(int breite) {
        int bevorzugt = baumEintraege == null || baumEintraege.isEmpty()
                ? MIN_HOEHE
                : Math.max(MIN_HOEHE, baumEintraege.size() * ZEILEN_HOEHE);
        return new LayoutSize(MIN_HOEHE, -1, bevorzugt);
    }

    private void listBoxBefuellen() {
        sheetListBox.removeItems((short) 0, sheetListBox.getItemCount());
        for (int i = 0; i < baumEintraege.size(); i++) {
            sheetListBox.addItem(baumEintraege.get(i).anzeigeText(), (short) i);
        }
    }

    private void auswahlWiederherstellen() {
        if (gespeichertesSheet == null || sheetListBox == null) {
            return;
        }
        for (int i = 0; i < baumEintraege.size(); i++) {
            if (baumEintraege.get(i) instanceof BlattKnoten knoten) {
                var named = Lo.qi(XNamed.class, knoten.sheet());
                if (named != null && gespeichertesSheet.equals(named.getName())) {
                    // uiZustand ist AUFBAU → itemStateChanged ignoriert selectItemPos automatisch
                    sheetListBox.selectItemPos((short) i, true);
                    return;
                }
            }
        }
    }

    // ── Aktualisierung ───────────────────────────────────────────────────────

    @Override
    protected void felderAktualisieren(ITurnierEvent event) {
        listeNeuAufbauen();
    }

    private void listeNeuAufbauen() {
        if (kannVollaufbauUeberspringen()) {
            // Struktur unverändert – der teure window.dispose()/Neuaufbau ist unnötig.
            // Nur den Aktivierungs-Zustand (Enabled) der ListBox nachziehen.
            PerfLog.log(logger, "[SIDEBAR-TIMING] {} listeNeuAufbauen: Struktur unverändert, Vollaufbau übersprungen, thread={}",
                    getClass().getSimpleName(), Thread.currentThread().getName());
            listBoxAktivierungAktualisieren();
            return;
        }
        auswahlMerken();
        allesFelderEntfernenUndNeuFenster();
    }

    /**
     * Prüft, ob sich die anzuzeigende Blattstruktur gegenüber der zuletzt aufgebauten
     * geändert hat. Baut dazu den Kandidaten-Baum (rein datenseitig, keine VCL-Operationen)
     * und vergleicht dessen Signatur mit {@link #aktuelleStrukturSignatur}. Stimmen sie
     * überein, kann der teure Fenster-Neuaufbau entfallen.
     */
    private boolean kannVollaufbauUeberspringen() {
        if (sheetListBox == null || organisierer == null || aktuelleStrukturSignatur == null) {
            return false;
        }
        var xDoc = dokumentOderNull();
        if (xDoc == null) {
            return false; // Vollaufbau baut die Leer-Ansicht auf
        }
        heileVeralteteMetadaten(xDoc);
        var kandidat = organisierer.baumAufbauen(xDoc, kollabierteGruppen, kollabierteSpielTage, kollabierteUnterGruppen);
        return aktuelleStrukturSignatur.equals(signaturAus(kandidat));
    }

    /**
     * Bildet eine Struktur-Signatur aus den Anzeigetexten der Baum-Einträge. Der
     * Anzeigetext kodiert bereits Reihenfolge, Blattnamen und – über die Auf-/Zuklappen-
     * Pfeile der Kopf-Einträge – den Kollaps-Zustand. Ändert sich irgendetwas davon,
     * ändert sich die Signatur.
     */
    private static String signaturAus(List<BlattBaumEintrag> eintraege) {
        var sb = new StringBuilder(eintraege.size() * 24);
        for (BlattBaumEintrag eintrag : eintraege) {
            sb.append(eintrag.anzeigeText()).append(SIGNATUR_TRENNER);
        }
        return sb.toString();
    }

    private void auswahlMerken() {
        gespeichertesSheet = null;
        var lb = sheetListBox;
        if (lb == null) {
            return;
        }
        short sel = lb.getSelectedItemPos();
        if (sel >= 0 && sel < baumEintraege.size()) {
            if (baumEintraege.get(sel) instanceof BlattKnoten knoten) {
                var named = Lo.qi(XNamed.class, knoten.sheet());
                if (named != null) {
                    gespeichertesSheet = named.getName();
                }
            }
        }
    }

    // ── Sheet-Navigation ─────────────────────────────────────────────────────

    private final XItemListener itemListener = new XItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (!isUiReady() || sheetListBox == null || unterdrueckeItemListener) {
                return;
            }
            int idx = e.Selected;
            if (idx < 0 || idx >= baumEintraege.size()) {
                logger.debug("itemStateChanged: idx={} außerhalb [0,{})", idx, baumEintraege.size());
                return;
            }
            logger.debug("itemStateChanged: idx={}, eintrag={}", idx, baumEintraege.get(idx).getClass().getSimpleName());
            // Alle Item-Aktionen werden via PostUserEvent auf den VCL-Hauptthread nach
            // Abschluss des aktuellen Events verschoben. Direktaufrufe aus itemStateChanged
            // (innerhalb von pBox->Select()) verursachen VCL-Re-Entranz und SIGSEGV, weil
            // setActiveSheet() und window.dispose() VCL-Operationen sind.
            if (itemDispatcher != null) {
                int gen = getUiGeneration();
                itemDispatcher.addCallback((XCallback) aData -> {
                    if (!isUiAlive(gen) || sheetListBox == null) {
                        return;
                    }
                    logger.debug("itemDispatcher: verarbeiteItemAuswahl({})", idx);
                    verarbeiteItemAuswahl(idx);
                }, null);
            } else {
                verarbeiteItemAuswahl(idx);
            }
        }

        @Override
        public void disposing(EventObject e) {
            // intentional no-op
        }
    };

    private void verarbeiteItemAuswahl(int idx) {
        if (idx < 0 || idx >= baumEintraege.size()) {
            return;
        }
        switch (baumEintraege.get(idx)) {
            case GruppenKopf kopf -> gruppeToggle(kopf.gruppe());
            case SpieltagKopf kopf -> spieltagToggle(kopf.spieltagNr());
            case UnterGruppenKopf kopf -> unterGruppeToggle(kopf.id());
            case BlattKnoten knoten -> sheetAktivieren(knoten);
        }
    }

    private void spieltagToggle(int spieltagNr) {
        auswahlMerken();
        if (kollabierteSpielTage.contains(spieltagNr)) {
            kollabierteSpielTage.remove(spieltagNr);
        } else {
            kollabierteSpielTage.add(spieltagNr);
        }
        allesFelderEntfernenUndNeuFenster();
    }

    private void unterGruppeToggle(String id) {
        auswahlMerken();
        if (kollabierteUnterGruppen.contains(id)) {
            kollabierteUnterGruppen.remove(id);
        } else {
            kollabierteUnterGruppen.add(id);
        }
        allesFelderEntfernenUndNeuFenster();
    }

    private void gruppeToggle(SheetGruppe gruppe) {
        auswahlMerken();
        if (kollabierteGruppen.contains(gruppe)) {
            kollabierteGruppen.remove(gruppe);
        } else {
            kollabierteGruppen.add(gruppe);
        }
        allesFelderEntfernenUndNeuFenster();
    }

    private void listBoxAktivierungAktualisieren() {
        var lb = sheetListBox;
        if (lb == null) {
            return;
        }
        var props = Lo.qi(XPropertySet.class, lb);
        if (props == null) {
            return;
        }
        try {
            props.setPropertyValue(GuiFactory.ENABLED, !SheetRunner.isRunning());
        } catch (Exception e) {
            logger.error("Fehler beim Setzen von Enabled auf der SheetListBox", e);
        }
    }

    private void sheetAktivieren(BlattKnoten knoten) {
        try {
            TurnierSheet.from(knoten.sheet(), getCurrentSpreadsheet()).setActiv();
            logger.debug("sheetAktivieren: Sheet '{}' aktiviert", knoten.metadatenSchluessel());
        } catch (Exception e) {
            logger.error("Fehler beim Aktivieren des Sheets '{}'", knoten.metadatenSchluessel(), e);
        }
    }

    // ── Tab-Synchronisierung ─────────────────────────────────────────────────

    @Override
    protected void onSpreadsheetGewechselt(WorkingSpreadsheet neuesSpreadsheet) {
        tabWechselListenerRegistrieren(neuesSpreadsheet);
    }

    @Override
    protected void nachControllerWechsel(Object source) {
        tabWechselListenerRegistrieren(getCurrentSpreadsheet());
    }

    private void tabWechselListenerRegistrieren(WorkingSpreadsheet ws) {
        tabWechselListenerAbmelden();
        if (ws == null) {
            return;
        }
        var model = Lo.qi(XModel.class, ws.getWorkingSpreadsheetDocument());
        if (model == null) {
            return;
        }
        XController controller;
        try {
            controller = model.getCurrentController();
        } catch (DisposedException e) {
            logger.debug("Model bereits disposed, Listener-Registrierung übersprungen");
            return;
        }
        selectionSupplier = Lo.qi(XSelectionSupplier.class, controller);
        if (selectionSupplier != null) {
            selectionSupplier.addSelectionChangeListener(tabWechselListener);
        }
    }

    private void tabWechselListenerAbmelden() {
        if (selectionSupplier != null) {
            try {
                selectionSupplier.removeSelectionChangeListener(tabWechselListener);
            } catch (Exception e) {
                logger.warn("Fehler beim Abmelden des TabWechselListeners", e);
            }
            selectionSupplier = null;
        }
    }

    private void sidebarAuswahlSynchronisieren() {
        if (!isUiReady() || sheetListBox == null) {
            return;
        }
        var ws = getCurrentSpreadsheet();
        if (ws == null) {
            return;
        }
        var aktuellesSheet = ws.getWorkingSpreadsheetView().getActiveSheet();
        if (aktuellesSheet == null) {
            return;
        }
        var named = Lo.qi(XNamed.class, aktuellesSheet);
        if (named == null) {
            return;
        }
        var sheetName = named.getName();
        for (int i = 0; i < baumEintraege.size(); i++) {
            if (baumEintraege.get(i) instanceof BlattKnoten knoten) {
                var knotenNamed = Lo.qi(XNamed.class, knoten.sheet());
                if (knotenNamed != null && sheetName.equals(knotenNamed.getName())) {
                    selectItemPosProgrammatisch((short) i, true);
                    return;
                }
            }
        }
        selectItemPosProgrammatisch((short) -1, false);
    }

    private void selectItemPosProgrammatisch(short pos, boolean selected) {
        unterdrueckeItemListener = true;
        try {
            sheetListBox.selectItemPos(pos, selected);
        } finally {
            unterdrueckeItemListener = false;
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    @Override
    protected void vorFensterDispose() {
        sheetListBox = null;
    }

    @Override
    protected void onDisposing(EventObject event) {
        SheetRunner.removeStateChangeListener(prozessZustandListener);
        tabWechselListenerAbmelden();
        sheetListBox = null;
        itemDispatcher = null;
        if (baumEintraege != null) {
            baumEintraege.clear();
        }
        gespeichertesSheet = null;
        aktuelleStrukturSignatur = null;
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────

    private XSpreadsheetDocument dokumentOderNull() {
        var ws = getCurrentSpreadsheet();
        return ws != null ? ws.getWorkingSpreadsheetDocument() : null;
    }

    /**
     * Heilt veraltete PTM-Metadaten in Dokumenten, die mit älteren Plugin-Versionen erstellt wurden.
     * Wird aufgerufen bevor der Blatt-Baum aufgebaut wird, damit der Baum die Sheets korrekt findet.
     */
    private void heileVeralteteMetadaten(XSpreadsheetDocument xDoc) {
        var ws = getCurrentSpreadsheet();
        if (ws == null) {
            return;
        }
        var system = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        switch (system) {
            case LIGA -> heileLigaMetadaten(xDoc);
            // Maastrichter-Altdokumente (Vorrunden unter Schweizer-Schlüssel) werden nicht mehr
            // gesondert geheilt: der schreib-seitige Purge in SheetMetadataHelper entfernt fremde
            // Identitäts-Schlüssel auf demselben Blatt bei der nächsten Vorrunden-Schreibung, und
            // SheetBaumOrganisierer dedupliziert die Anzeige bereits beim Aufbau.
            default -> { /* keine Migration nötig */ }
        }
    }

    private void heileLigaMetadaten(XSpreadsheetDocument xDoc) {
        SheetMetadataHelper.findeSheetUndHeile(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE, SheetNamen.LEGACY_MELDELISTE);
        SheetMetadataHelper.findeSheetUndHeile(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN, SheetNamen.LEGACY_SPIELPLAN);
        SheetMetadataHelper.findeSheetUndHeile(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_DIREKTVERGLEICH, SheetNamen.LEGACY_DIREKTVERGLEICH);
        SheetMetadataHelper.findeSheetUndHeile(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE, SheetNamen.LEGACY_RANGLISTE);
    }
}
