package de.petanqueturniermanager.helper.sheetsync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sun.star.container.XNamed;
import com.sun.star.frame.XController;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.petanqueturniermanager.SheetRunner;

class SheetAktivierungsDispatcherTest {

    @AfterEach
    void suppressionFlagLeeren() {
        // Falls ein Test das globale Suppression-Flag gesetzt und (unerwartet) nicht
        // verbraucht hat: sauber leeren, damit Folgetests nicht beeinflusst werden.
        SheetRunner.consumeSelectionChangeSuppression();
    }

    @Test
    void selectionChanged_verteiltAnAlleHandler_undDedupProToken() {
        var handlerA = mock(SheetAktivierungsHandler.class);
        var handlerB = mock(SheetAktivierungsHandler.class);
        var dispatcher = new SheetAktivierungsDispatcher();
        dispatcher.registriere(handlerA).registriere(handlerB);
        var fix = registriere(dispatcher);

        wechsleAuf(fix, "A");   // echter Wechsel → dispatch
        wechsleAuf(fix, "A");   // gleicher Name (anderer Proxy) → dedup, kein dispatch
        wechsleAuf(fix, "B");   // erneuter Wechsel → dispatch

        verify(handlerA, times(2)).aufSheetAktiviert(any(), any(),
                eq(SheetAktivierungsHandler.TRIGGER_SELECTION), any());
        verify(handlerB, times(2)).aufSheetAktiviert(any(), any(),
                eq(SheetAktivierungsHandler.TRIGGER_SELECTION), any());
    }

    @Test
    void selectionChanged_suppression_schlucktGenauEinEvent() {
        var handler = mock(SheetAktivierungsHandler.class);
        var dispatcher = new SheetAktivierungsDispatcher();
        dispatcher.registriere(handler);
        var fix = registriere(dispatcher);

        SheetRunner.unterdrückeNaechstesSelectionChange();

        wechsleAuf(fix, "A");   // durch Suppression geschluckt (Token wird nachgezogen)
        wechsleAuf(fix, "A");   // gleicher Token → dedup
        wechsleAuf(fix, "B");   // Flag verbraucht → echter dispatch

        verify(handler, times(1)).aufSheetAktiviert(any(), any(),
                eq(SheetAktivierungsHandler.TRIGGER_SELECTION), any());
    }

    @Test
    void werfenderHandler_bricht_die_anderen_nicht_ab() {
        var vorher = mock(SheetAktivierungsHandler.class);
        var boese = mock(SheetAktivierungsHandler.class);
        var nachher = mock(SheetAktivierungsHandler.class);
        doThrow(new RuntimeException("boom")).when(boese)
                .aufSheetAktiviert(any(), any(), any(), any());
        var dispatcher = new SheetAktivierungsDispatcher();
        dispatcher.registriere(vorher).registriere(boese).registriere(nachher);
        var fix = registriere(dispatcher);

        wechsleAuf(fix, "A");

        verify(vorher, times(1)).aufSheetAktiviert(any(), any(), any(), any());
        verify(nachher, times(1)).aufSheetAktiviert(any(), any(), any(), any());
    }

    @Test
    void doppelteRegistrierungDesselbenDokuments_haengtNurEinenListenerAn() {
        var handler = mock(SheetAktivierungsHandler.class);
        var dispatcher = new SheetAktivierungsDispatcher();
        dispatcher.registriere(handler);

        var view = mock(XController.class, withSettings()
                .extraInterfaces(XSpreadsheetView.class, XSelectionSupplier.class));
        var xModel = mock(XModel.class, withSettings().extraInterfaces(XSpreadsheetDocument.class));
        var startSheet = sheet("Start");
        when(xModel.getCurrentController()).thenReturn(view);
        when(((XSpreadsheetView) view).getActiveSheet()).thenReturn(startSheet);

        dispatcher.onViewCreated(xModel);
        dispatcher.onNew(xModel);
        dispatcher.onLoadFinished(xModel);

        verify((XSelectionSupplier) view, times(1))
                .addSelectionChangeListener(any(XSelectionChangeListener.class));
    }

    @Test
    void aufFokusVerloren_wirdAnHandlerWeitergereicht() {
        var handler = mock(SheetAktivierungsHandler.class);
        var dispatcher = new SheetAktivierungsDispatcher();
        dispatcher.registriere(handler);

        dispatcher.onUnfocus(null);
        dispatcher.onViewClosed(null);

        verify(handler, times(2)).aufFokusVerloren();
        verify(handler, never()).aufSheetAktiviert(any(), any(), any(), any());
    }

    // ── Test-Infrastruktur ──────────────────────────────────────────────────

    private record Fixture(XSelectionChangeListener listener, XSpreadsheetView view,
            EventObject event) {
    }

    /** Simuliert onViewCreated und liefert den vom Dispatcher registrierten Selection-Listener. */
    private static Fixture registriere(SheetAktivierungsDispatcher dispatcher) {
        var view = mock(XController.class, withSettings()
                .extraInterfaces(XSpreadsheetView.class, XSelectionSupplier.class));
        var xModel = mock(XModel.class, withSettings().extraInterfaces(XSpreadsheetDocument.class));
        var startSheet = sheet("Start");
        when(xModel.getCurrentController()).thenReturn(view);
        var xView = (XSpreadsheetView) view;
        when(xView.getActiveSheet()).thenReturn(startSheet);

        dispatcher.onViewCreated(xModel);

        var cap = ArgumentCaptor.forClass(XSelectionChangeListener.class);
        verify((XSelectionSupplier) view).addSelectionChangeListener(cap.capture());
        var event = new EventObject();
        event.Source = view;
        return new Fixture(cap.getValue(), xView, event);
    }

    private static void wechsleAuf(Fixture fix, String sheetName) {
        var s = sheet(sheetName);
        when(fix.view().getActiveSheet()).thenReturn(s);
        fix.listener().selectionChanged(fix.event());
    }

    private static XSpreadsheet sheet(String name) {
        var s = mock(XSpreadsheet.class, withSettings().extraInterfaces(XNamed.class));
        when(((XNamed) s).getName()).thenReturn(name);
        return s;
    }
}
