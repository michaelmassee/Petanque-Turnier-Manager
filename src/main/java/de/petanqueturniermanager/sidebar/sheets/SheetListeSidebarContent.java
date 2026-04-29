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
import com.sun.star.awt.XControl;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNamed;
import com.sun.star.frame.XModel;
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
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
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

    private XListBox sheetListBox;
    private List<BlattBaumEintrag> baumEintraege;
    private Set<SheetGruppe> kollabierteGruppen;
    private Set<Integer> kollabierteSpielTage;
    private Set<String> kollabierteUnterGruppen;
    private SheetBaumOrganisierer organisierer;
    private String gespeichertesSheet = null;
    private String letzteAktiveSheetName = null;
    private XSelectionSupplier selectionSupplier = null;
    /**
     * Wird auf {@code true} gesetzt während programmatischer ListBox-Aktualisierungen
     * (z.B. {@code auswahlWiederherstellen()} nach {@code listeNeuAufbauen()}). Verhindert,
     * dass das dadurch ausgelöste {@code itemStateChanged} ein {@code setActiveSheet()}
     * triggert und so den vom User gewählten Tab überschreibt.
     */
    private boolean unterdrueckeItemListener = false;

    private final XSelectionChangeListener tabWechselListener = new XSelectionChangeListener() {
        @Override
        public void selectionChanged(EventObject event) {
            if (SheetRunner.isRunning() || sheetListBox == null) {
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
            if (sheetName.equals(letzteAktiveSheetName)) {
                return;
            }
            letzteAktiveSheetName = sheetName;
            sidebarAuswahlSynchronisieren(sheetName);
        }

        @Override
        public void disposing(EventObject event) {
            selectionSupplier = null;
        }
    };

    private final Runnable prozessZustandListener = () -> {
        if (SheetRunner.isRunning()) {
            listBoxAktivierungAktualisieren();
        } else {
            listeNeuAufbauen();
        }
    };

    public SheetListeSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
            XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, xSidebar);
        SheetRunner.addStateChangeListener(prozessZustandListener);
        tabWechselListenerRegistrieren(workingSpreadsheet);
    }

    // ── Aufbau ──────────────────────────────────────────────────────────────

    @Override
    protected void felderHinzufuegen() {
        // Lazy-Init: felderHinzufuegen() wird bereits aus super() aufgerufen,
        // bevor die Feld-Initialisierer dieser Klasse gelaufen sind.
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
            sheetListeAufbauenLeer();
            requestLayout();
            return;
        }

        heileVeralteteMetadaten(xDoc);
        baumEintraege = organisierer.baumAufbauen(xDoc, kollabierteGruppen, kollabierteSpielTage, kollabierteUnterGruppen);

        if (baumEintraege.isEmpty()) {
            sheetListeAufbauenLeer();
        } else {
            sheetListeAufbauenMitEintraegen();
        }
        requestLayout();
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
                    unterdrueckeItemListener = true;
                    try {
                        sheetListBox.selectItemPos((short) i, true);
                    } finally {
                        unterdrueckeItemListener = false;
                    }
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
        letzteAktiveSheetName = null;
        auswahlMerken();
        allesFelderEntfernenUndNeuFenster();
        felderHinzufuegen();
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
            if (unterdrueckeItemListener) {
                // Programmatischer selectItemPos-Aufruf (z.B. aus auswahlWiederherstellen
                // nach listeNeuAufbauen) – kein Sheet-Wechsel auslösen, sonst überschreibt
                // die Sidebar den vom User per Tab-Klick aktivierten Tab.
                return;
            }
            int idx = e.Selected;
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

        @Override
        public void disposing(EventObject e) {
            // intentional no-op
        }
    };

    private void spieltagToggle(int spieltagNr) {
        auswahlMerken();
        if (kollabierteSpielTage.contains(spieltagNr)) {
            kollabierteSpielTage.remove(spieltagNr);
        } else {
            kollabierteSpielTage.add(spieltagNr);
        }
        allesFelderEntfernenUndNeuFenster();
        felderHinzufuegen();
    }

    private void unterGruppeToggle(String id) {
        auswahlMerken();
        if (kollabierteUnterGruppen.contains(id)) {
            kollabierteUnterGruppen.remove(id);
        } else {
            kollabierteUnterGruppen.add(id);
        }
        allesFelderEntfernenUndNeuFenster();
        felderHinzufuegen();
    }

    private void gruppeToggle(SheetGruppe gruppe) {
        auswahlMerken();
        if (kollabierteGruppen.contains(gruppe)) {
            kollabierteGruppen.remove(gruppe);
        } else {
            kollabierteGruppen.add(gruppe);
        }
        allesFelderEntfernenUndNeuFenster();
        felderHinzufuegen();
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

    private void tabWechselListenerRegistrieren(WorkingSpreadsheet ws) {
        tabWechselListenerAbmelden();
        if (ws == null) {
            return;
        }
        var model = Lo.qi(XModel.class, ws.getWorkingSpreadsheetDocument());
        if (model == null) {
            return;
        }
        selectionSupplier = Lo.qi(XSelectionSupplier.class, model.getCurrentController());
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

    private void sidebarAuswahlSynchronisieren(String sheetName) {
        if (sheetListBox == null) {
            return;
        }
        for (int i = 0; i < baumEintraege.size(); i++) {
            if (baumEintraege.get(i) instanceof BlattKnoten knoten) {
                var named = Lo.qi(XNamed.class, knoten.sheet());
                if (named != null && sheetName.equals(named.getName())) {
                    unterdrueckeItemListener = true;
                    try {
                        sheetListBox.selectItemPos((short) i, true);
                    } finally {
                        unterdrueckeItemListener = false;
                    }
                    return;
                }
            }
        }
        unterdrueckeItemListener = true;
        try {
            sheetListBox.selectItemPos((short) -1, false);
        } finally {
            unterdrueckeItemListener = false;
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    @Override
    protected void onDisposing(EventObject event) {
        SheetRunner.removeStateChangeListener(prozessZustandListener);
        tabWechselListenerAbmelden();
        sheetListBox = null;
        baumEintraege.clear();
        gespeichertesSheet = null;
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
            case MAASTRICHTER -> heileMaastrichterMetadaten(xDoc);
            default -> { /* keine Migration nötig */ }
        }
    }

    private void heileLigaMetadaten(XSpreadsheetDocument xDoc) {
        SheetMetadataHelper.findeSheetUndHeile(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_MELDELISTE, SheetNamen.LEGACY_MELDELISTE);
        SheetMetadataHelper.findeSheetUndHeile(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN, SheetNamen.LEGACY_SPIELPLAN);
        SheetMetadataHelper.findeSheetUndHeile(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_DIREKTVERGLEICH, SheetNamen.LEGACY_DIREKTVERGLEICH);
        SheetMetadataHelper.findeSheetUndHeile(xDoc, SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE, SheetNamen.LEGACY_RANGLISTE);
    }

    /**
     * Entfernt veraltete Schweizer-Spielrunden-Schlüssel aus Maastrichter-Dokumenten.
     * Ältere Plugin-Versionen speicherten die Maastrichter-Vorrunden unter dem Schweizer-Schlüssel.
     * Die neuen Maastrichter-Schlüssel wurden bereits bei der ersten Aktion gespeichert;
     * die alten Schweizer-Schlüssel müssen manuell entfernt werden.
     */
    private void heileMaastrichterMetadaten(XSpreadsheetDocument xDoc) {
        for (int n = 1; n <= 10; n++) {
            SheetMetadataHelper.loescheSchluessel(xDoc, SheetMetadataHelper.schluesselSchweizerSpielrunde(n));
        }
    }
}
