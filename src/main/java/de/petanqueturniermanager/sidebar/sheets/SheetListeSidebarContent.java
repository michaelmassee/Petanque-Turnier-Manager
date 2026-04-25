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
import com.sun.star.container.XNamed;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.ui.XSidebar;

import com.sun.star.beans.XPropertySet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;

/**
 * Listet alle PTM-verwalteten Tabellenblätter in Dokumentreihenfolge auf.
 * Klick auf einen Eintrag aktiviert das entsprechende Sheet.
 *
 * @author Michael Massee
 */
public class SheetListeSidebarContent extends BaseSidebarContent {

    private static final Logger logger = LogManager.getLogger(SheetListeSidebarContent.class);

    private static final int ZEILEN_HOEHE = 22;
    private static final int MIN_HOEHE = 60;
    private static final int MAX_HOEHE = 320;

    private XListBox sheetListBox;
    private List<XSpreadsheet> aktuelleSheets = new ArrayList<>();
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
        var xDoc = dokumentOderNull();
        if (xDoc == null) {
            sheetListeAufbauenLeer();
            requestLayout();
            return;
        }

        aktuelleSheets = new ArrayList<>(ptmSheetsSortiertNachPosition(xDoc));

        if (aktuelleSheets.isEmpty()) {
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
        int hoehe = Math.max(MIN_HOEHE, Math.min(aktuelleSheets.size() * ZEILEN_HOEHE, MAX_HOEHE));
        Map<String, Object> props = new HashMap<>();
        props.put(GuiFactory.V_SCROLL, true);
        XControl ctrl = GuiFactory.createListBox(
                getGuiFactoryCreateParam(),
                itemListener,
                new Rectangle(0, 0, 200, hoehe),
                props);
        if (ctrl == null) {
            logger.error("SheetListePanel: createListBox lieferte null");
            sheetListBox = null;
            return;
        }
        sheetListBox = Lo.qi(XListBox.class, ctrl);
        getLayout().addLayout(new ControlLayout(ctrl), 1);
        listBoxBefuellen();
        auswahlWiederherstellen();
        listBoxAktivierungAktualisieren();
    }

    private void listBoxBefuellen() {
        sheetListBox.removeItems((short) 0, sheetListBox.getItemCount());
        for (int i = 0; i < aktuelleSheets.size(); i++) {
            var named = Lo.qi(XNamed.class, aktuelleSheets.get(i));
            if (named != null) {
                sheetListBox.addItem(named.getName(), (short) i);
            }
        }
    }

    private void auswahlWiederherstellen() {
        if (gespeichertesSheet == null) {
            return;
        }
        for (int i = 0; i < aktuelleSheets.size(); i++) {
            var named = Lo.qi(XNamed.class, aktuelleSheets.get(i));
            if (named != null && gespeichertesSheet.equals(named.getName())) {
                sheetListBox.selectItemPos((short) i, true);
                return;
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
        if (sel >= 0 && sel < aktuelleSheets.size()) {
            var named = Lo.qi(XNamed.class, aktuelleSheets.get(sel));
            if (named != null) {
                gespeichertesSheet = named.getName();
            }
        }
    }

    // ── Sheet-Navigation ─────────────────────────────────────────────────────

    private final XItemListener itemListener = new XItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.Selected >= 0) {
                sheetOeffnen();
            }
        }

        @Override
        public void disposing(EventObject e) {
            // intentional no-op
        }
    };

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

    private void sheetOeffnen() {
        var lb = sheetListBox;
        if (lb == null) {
            return;
        }
        short idx = lb.getSelectedItemPos();
        if (idx < 0 || idx >= aktuelleSheets.size()) {
            logger.debug("sheetOeffnen: ungültiger Index {}", idx);
            return;
        }
        try {
            TurnierSheet.from(aktuelleSheets.get(idx), getCurrentSpreadsheet()).setActiv();
            logger.debug("sheetOeffnen: Sheet-Index {} aktiviert", idx);
        } catch (Exception e) {
            logger.error("Fehler beim Aktivieren des Sheets an Index {}", idx, e);
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    @Override
    protected void onDisposing(EventObject event) {
        SheetRunner.removeStateChangeListener(prozessZustandListener);
        sheetListBox = null;
        aktuelleSheets.clear();
        gespeichertesSheet = null;
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────

    private XSpreadsheetDocument dokumentOderNull() {
        var ws = getCurrentSpreadsheet();
        return ws != null ? ws.getWorkingSpreadsheetDocument() : null;
    }

    /**
     * Liefert alle PTM-verwalteten Sheets in Dokumentreihenfolge.
     * Identifikation über Named Ranges mit Prefix {@code __PTM_} (Score-Keys ausgeschlossen).
     */
    private List<XSpreadsheet> ptmSheetsSortiertNachPosition(XSpreadsheetDocument xDoc) {
        String[] allKeys = SheetMetadataHelper.getSchluesselMitPrefix(xDoc, "__PTM_");

        Set<String> ptmNamen = new HashSet<>();
        for (String key : allKeys) {
            if (key.startsWith("__PTM_SCORE_")) {
                continue;
            }
            SheetMetadataHelper.findeSheet(xDoc, key).ifPresent(s -> {
                var named = Lo.qi(XNamed.class, s);
                if (named != null) {
                    ptmNamen.add(named.getName());
                }
            });
        }

        if (ptmNamen.isEmpty()) {
            logger.debug("ptmSheetsSortiertNachPosition: keine PTM-Sheets gefunden");
            return List.of();
        }

        var helper = new SheetHelper(getCurrentSpreadsheet());
        int anz = helper.getAnzSheets();
        var ergebnis = new ArrayList<XSpreadsheet>(ptmNamen.size());
        for (int i = 0; i < anz; i++) {
            XSpreadsheet s = helper.getSheetByIdx(i);
            if (s == null) {
                continue;
            }
            var named = Lo.qi(XNamed.class, s);
            if (named != null && ptmNamen.contains(named.getName())) {
                ergebnis.add(s);
            }
        }

        logger.debug("ptmSheetsSortiertNachPosition: {} Sheets gefunden", ergebnis.size());
        return ergebnis;
    }
}
