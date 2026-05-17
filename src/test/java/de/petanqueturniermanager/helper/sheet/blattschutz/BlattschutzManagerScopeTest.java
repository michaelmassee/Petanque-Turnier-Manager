/*
 * Erstellung : 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.blattschutz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Unit-Tests für den thread-lokalen Command-Scope im {@link BlattschutzManager}:
 * Refcount-Verhalten, Lazy-Unprotect, Fail-fast und Robustness gegen Exceptions
 * im {@code doSchuetzen}-Pfad.
 */
class BlattschutzManagerScopeTest {

    private BlattschutzManager manager;
    private IBlattschutzKonfiguration konfigA;
    private IBlattschutzKonfiguration konfigB;
    private WorkingSpreadsheet ws;

    @BeforeEach
    void setUp() {
        manager = BlattschutzManager.get();
        manager.resetCallCounters();
        manager.resetScopeForTest();
        TurnierModus.get().setAktivForTest(false);

        konfigA = mock(IBlattschutzKonfiguration.class);
        when(konfigA.berechneSchutzInfos(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        konfigB = mock(IBlattschutzKonfiguration.class);
        when(konfigB.berechneSchutzInfos(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        ws = mock(WorkingSpreadsheet.class);
        when(ws.getWorkingSpreadsheetDocument()).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        manager.resetScopeForTest();
        manager.resetCallCounters();
        TurnierModus.get().setAktivForTest(false);
    }

    // ── Lazy: ohne Trigger gar kein Toggle ───────────────────────────────────

    @Test
    void scopeOhneTrigger_keinUnprotectKeinProtect() {
        manager.beginCommandScope(konfigA, ws);
        manager.endCommandScope();

        assertThat(manager.getUnprotectCallCount()).isZero();
        assertThat(manager.getProtectCallCount()).isZero();
    }

    @Test
    void scopeMitTrigger_genauEinUnprotectEinProtect() {
        manager.beginCommandScope(konfigA, ws);
        manager.ensureUnprotectedInScope();
        manager.ensureUnprotectedInScope(); // mehrfach Trigger → bleibt 1 unprotect
        manager.endCommandScope();

        assertThat(manager.getUnprotectCallCount()).isEqualTo(1);
        assertThat(manager.getProtectCallCount()).isEqualTo(1);
    }

    // ── Refcount: nested Scopes ──────────────────────────────────────────────

    @Test
    void nestedScopes_gleicheKonfig_einProtectAmAeusserenEnde() {
        manager.beginCommandScope(konfigA, ws);
        manager.beginCommandScope(konfigA, ws);
        manager.ensureUnprotectedInScope();
        manager.endCommandScope(); // inner: nichts
        assertThat(manager.getProtectCallCount()).isZero();
        manager.endCommandScope(); // outer: einmal schützen
        assertThat(manager.getProtectCallCount()).isEqualTo(1);
        assertThat(manager.getUnprotectCallCount()).isEqualTo(1);
    }

    // ── Fail-fast: nested Scope mit anderer Konfiguration ────────────────────

    @Test
    void nestedScopeAndereKonfig_wirftIllegalStateException() {
        manager.beginCommandScope(konfigA, ws);
        assertThatThrownBy(() -> manager.beginCommandScope(konfigB, ws))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Konfigurationswechsel");
        // Aufräumen, sonst leakt der Scope in andere Tests
        manager.endCommandScope();
    }

    // ── Fail-fast: Trigger ohne Scope bei aktivem Turnier-Modus ──────────────

    @Test
    void ensureUnprotectedOhneScope_aktiverTurnierModus_wirft() {
        TurnierModus.get().setAktivForTest(true);
        assertThatThrownBy(() -> manager.ensureUnprotectedInScope())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BlattschutzScope");
    }

    @Test
    void ensureUnprotectedOhneScope_inaktiverTurnierModus_istNoOp() {
        TurnierModus.get().setAktivForTest(false);
        manager.ensureUnprotectedInScope(); // darf nicht werfen

        assertThat(manager.getUnprotectCallCount()).isZero();
    }

    // ── Scope-aware entsperren()/schuetzen() im Scope: No-Op ─────────────────

    @Test
    void entsperrenSchuetzenInScope_sindNoOp() {
        manager.beginCommandScope(konfigA, ws);
        manager.entsperren(konfigA, ws);
        manager.schuetzen(konfigA, ws);
        manager.endCommandScope();

        // Weder echte unprotect noch protect, da kein Trigger
        assertThat(manager.getUnprotectCallCount()).isZero();
        assertThat(manager.getProtectCallCount()).isZero();
    }

    // ── Robustness: Scope-State wird auch bei Exception in doSchuetzen aufgeräumt
    // (verifiziert via Folge-beginCommandScope, der sonst Konfig-Konflikt würfe). ─

    @Test
    void exceptionInDoSchuetzen_threadLocalAufgeraeumt() {
        IBlattschutzKonfiguration werfendeKonfig = mock(IBlattschutzKonfiguration.class);
        // zelleStylesAktualisieren wirft → doSchuetzen propagiert
        org.mockito.Mockito.doThrow(new RuntimeException("simulierter Fehler"))
                .when(werfendeKonfig).zelleStylesAktualisieren(org.mockito.ArgumentMatchers.any());
        when(werfendeKonfig.berechneSchutzInfos(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        manager.beginCommandScope(werfendeKonfig, ws);
        manager.ensureUnprotectedInScope();
        assertThatThrownBy(() -> manager.endCommandScope())
                .isInstanceOf(RuntimeException.class);

        // ThreadLocal muss trotzdem geleert sein → neuer Scope mit anderer Konfig läuft sauber
        manager.beginCommandScope(konfigA, ws);
        manager.endCommandScope();
    }
}
