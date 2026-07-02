package de.petanqueturniermanager.helper.sheetsync;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

/**
 * Regel, die auf die Aktivierung eines Sheets (Tab-Wechsel oder Fenster-Fokus) reagiert.
 * <p>
 * Handler registrieren <b>keinen</b> eigenen {@link com.sun.star.view.XSelectionChangeListener}
 * mehr. Stattdessen hält der {@link SheetAktivierungsDispatcher} genau einen Selection-Listener
 * pro Controller, ermittelt aktives Sheet und Token <b>einmal</b> und ruft danach alle Handler
 * in-process auf. Das ersetzt den früheren Fan-out aus dutzenden einzelnen UNO-Listenern
 * (ein Tab-Wechsel = ein Cross-Bridge-Callback statt einem pro Handler).
 * <p>
 * Der Handler entscheidet selbst per {@code zielSheetMatch}, ob ihn das aktivierte Sheet
 * betrifft, und plant seine Arbeit ausschließlich entprellt über den {@link SheetSyncDebouncer}
 * – niemals synchron im Callback (Threading-Regeln, siehe CLAUDE.md).
 */
public interface SheetAktivierungsHandler {

    /** Trigger: Sheet-Wechsel innerhalb desselben Fensters. */
    String TRIGGER_SELECTION = "selectionChanged";

    /** Trigger: das Dokument-Fenster hat den Fokus erhalten. */
    String TRIGGER_FOCUS = "onFocus";

    /**
     * Wird vom Dispatcher aufgerufen, sobald {@code xSheet} im {@code xDoc} aktiviert wurde.
     *
     * @param trigger einer der {@code TRIGGER_*}-Werte
     * @param traceId Korrelations-Kennung für das auslösende Event (nur Logging)
     */
    void aufSheetAktiviert(XSpreadsheetDocument xDoc, XSpreadsheet xSheet, String trigger,
            String traceId);

    /**
     * Signalisiert, dass das Dokument-Fenster den Fokus verloren hat bzw. geschlossen/entladen
     * wurde. Handler mit Fokus-Dedup setzen hier ihren Merker zurück. Default: nichts zu tun.
     */
    default void aufFokusVerloren() {
    }
}
