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
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;
import de.petanqueturniermanager.sidebar.layout.FuellendeControlLayout;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.BlattKnoten;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.GruppenKopf;
import de.petanqueturniermanager.sidebar.sheets.BlattBaumEintrag.SpieltagKopf;

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
    private SheetBaumOrganisierer organisierer;
    private String gespeichertesSheet = null;
    private final Runnable prozessZustandListener = this::listBoxAktivierungAktualisieren;

    public SheetListeSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
            XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, xSidebar);
        SheetRunner.addStateChangeListener(prozessZustandListener);
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
        if (baumEintraege == null) {
            baumEintraege = new ArrayList<>();
        }
        var xDoc = dokumentOderNull();
        if (xDoc == null) {
            sheetListeAufbauenLeer();
            requestLayout();
            return;
        }

        baumEintraege = organisierer.baumAufbauen(xDoc, kollabierteGruppen, kollabierteSpielTage);

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
                    sheetListBox.selectItemPos((short) i, true);
                    return;
                }
            }
        }
    }

    // ── Aktualisierung ───────────────────────────────────────────────────────

    @Override
    protected void felderAktualisieren(ITurnierEvent event) {
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
            int idx = e.Selected;
            if (idx < 0 || idx >= baumEintraege.size()) {
                return;
            }
            switch (baumEintraege.get(idx)) {
                case GruppenKopf kopf -> gruppeToggle(kopf.gruppe());
                case SpieltagKopf kopf -> spieltagToggle(kopf.spieltagNr());
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

    // ── Cleanup ──────────────────────────────────────────────────────────────

    @Override
    protected void onDisposing(EventObject event) {
        SheetRunner.removeStateChangeListener(prozessZustandListener);
        sheetListBox = null;
        baumEintraege.clear();
        gespeichertesSheet = null;
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────

    private XSpreadsheetDocument dokumentOderNull() {
        var ws = getCurrentSpreadsheet();
        return ws != null ? ws.getWorkingSpreadsheetDocument() : null;
    }
}
