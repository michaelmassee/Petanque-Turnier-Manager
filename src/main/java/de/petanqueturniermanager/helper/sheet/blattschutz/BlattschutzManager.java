/*
 * Erstellung : 2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet.blattschutz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;
import com.sun.star.util.XProtectable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Zentraler Singleton für Sheet-Schutz (Blattschutz) im Turnier-Modus.
 * <p>
 * Orchestriert die korrekte Reihenfolge:
 * <ol>
 *   <li>CellStyles aktualisieren (vor jedem protect – LO-Einschränkung)</li>
 *   <li>Sheet ggf. entsperren (idempotent, verhindert UNO-Exception)</li>
 *   <li>Zellschutz auf editierbaren Bereichen freigeben</li>
 *   <li>Sheet sperren</li>
 * </ol>
 * <h2>Command-Scope (lazy unprotect)</h2>
 * Ein {@link SheetRunner}-Lauf öffnet via {@link #beginCommandScope} einen
 * thread-lokalen Scope. Innerhalb dieses Scopes wird {@link #entsperren} und
 * {@link #schuetzen} stillgelegt; das echte Entsperren passiert erst beim
 * ersten Aufruf von {@link #ensureUnprotectedInScope} (typischerweise aus
 * {@code ConditionalFormatHelper}, {@code RangeHelper.clearRange} oder
 * {@code RangeHelper.setDataInRange}). Beim {@link #endCommandScope} läuft
 * <em>immer</em> ein abschließendes {@code doSchuetzen()}, auch wenn im Scope
 * kein {@code ensureUnprotectedInScope}-Trigger gefeuert hat. Damit werden
 * auch Doc-Struktur-Mutationen abgedeckt, die kein {@code ensureUnprotectedInScope}
 * aufrufen (z.B. {@code NewSheet.forceCreate()}, das ein Sheet entfernt und
 * neu, ungeschützt anlegt). Pro Kommando also höchstens ein entsperren-Aufruf
 * (lazy) und genau ein schützen-Aufruf am Ende.
 * <p>
 * Hinweis: Auch {@code setDataInRange} triggert den Lazy-Unprotect. Auf Sheets
 * mit editierbarem Datenbereich (Zellen mit {@code IsLocked=false}) wäre das
 * UNO-seitig nicht zwingend nötig, vermeidet aber Sonderfall-Logik – Rangliste-
 * Sheets sind im Turnier-Modus voll gesperrt und müssen für jeden Daten-Write
 * entsperrt sein.
 */
public class BlattschutzManager {

    private static final Logger logger = LogManager.getLogger(BlattschutzManager.class);
    private static final BlattschutzManager INSTANCE = new BlattschutzManager();

    /**
     * Thread-lokaler Scope-Zustand eines laufenden Kommandos. Ein Scope hält
     * die Konfiguration und das Spreadsheet fest und merkt sich, ob im Verlauf
     * lazy bereits entsperrt wurde (damit weitere {@code ensureUnprotectedInScope}-
     * Calls No-Ops sind).
     */
    private static final class ScopeState {
        final IBlattschutzKonfiguration konfig;
        final WorkingSpreadsheet ws;
        int refCount;
        boolean wurdeEntsperrt;

        ScopeState(IBlattschutzKonfiguration k, WorkingSpreadsheet w) {
            konfig = k;
            ws = w;
            refCount = 1;
        }
    }

    private static final ThreadLocal<ScopeState> SCOPE = new ThreadLocal<>();

    // Test-Counter: zählen die echten doEntsperren/doSchuetzen-Aufrufe.
    private final AtomicInteger unprotectCount = new AtomicInteger();
    private final AtomicInteger protectCount = new AtomicInteger();

    private BlattschutzManager() {
    }

    public static BlattschutzManager get() {
        return INSTANCE;
    }

    // ── Command-Scope ────────────────────────────────────────────────────────

    /**
     * Öffnet einen thread-lokalen Lazy-Scope für die Dauer eines Kommandos.
     * Das Entsperren wird hier <em>nicht</em> ausgeführt – erst der erste
     * Aufruf von {@link #ensureUnprotectedInScope()} entsperrt.
     *
     * @throws IllegalStateException wenn bereits ein Scope mit anderer
     *         {@link IBlattschutzKonfiguration} offen ist (echter Bug)
     */
    public void beginCommandScope(IBlattschutzKonfiguration konfig, WorkingSpreadsheet ws) {
        ScopeState state = SCOPE.get();
        if (state == null) {
            SCOPE.set(new ScopeState(konfig, ws));
            return;
        }
        if (!state.konfig.equals(konfig)) {
            throw new IllegalStateException(
                    "BlattschutzScope: Konfigurationswechsel in nested scope nicht erlaubt – "
                            + "äußerer Scope: " + state.konfig.getClass().getSimpleName()
                            + ", innerer Scope: " + konfig.getClass().getSimpleName());
        }
        state.refCount++;
    }

    /**
     * Schließt den thread-lokalen Scope. Beim Schließen des äußersten Scopes
     * läuft <em>immer</em> ein abschließendes {@code doSchuetzen()} – auch
     * wenn im Scope kein {@code ensureUnprotectedInScope}-Trigger gefeuert
     * hat. Damit sind Doc-Struktur-Mutationen abgedeckt, die kein Lazy-Trigger
     * auslösen (z.B. {@code NewSheet.forceCreate()}, das ein Sheet entfernt
     * und ungeschützt neu anlegt). Der ThreadLocal wird auch dann aufgeräumt,
     * wenn das Schützen wirft – damit der nächste Lauf in einem Thread-Pool
     * sauber startet.
     */
    public void endCommandScope() {
        ScopeState state = SCOPE.get();
        if (state == null) {
            return;
        }
        if (--state.refCount > 0) {
            return;
        }
        try {
            // doSchuetzen() toggelt Protect/Unprotect und aktualisiert CellStyles → das setzt in
            // LibreOffice das Modified-Flag. Hat der Lauf selbst nichts geändert (Dokument vor dem
            // Schützen unverändert – z.B. ein idempotenter Formatierer-Lauf beim Tab-Wechsel), darf
            // das bloße Wiederherstellen des Schutzes das Dokument NICHT als geändert markieren –
            // sonst Phantom-Speicherung/„Speichern?"-Abfrage. Bei echten Änderungen war das Flag
            // bereits vor doSchuetzen true und bleibt erhalten.
            var xDoc = state.ws.getWorkingSpreadsheetDocument();
            if (xDoc == null) {
                doSchuetzen(state.konfig, state.ws);
            } else {
                new DocumentPropertiesHelper(xDoc).ohneModifiedFlag(() -> doSchuetzen(state.konfig, state.ws));
            }
        } finally {
            SCOPE.remove();
        }
    }

    /**
     * Wird von Style-/Conditional-Format-mutierenden Operationen
     * ({@code ConditionalFormatHelper}, {@code RangeHelper.clearRange}) vor
     * der eigentlichen UNO-Operation gerufen. Entsperrt einmalig im laufenden
     * Scope.
     * <p>
     * Wird kein Scope geführt aber der Turnier-Modus ist aktiv, ist das ein
     * Programmierfehler (Style-Mutation außerhalb eines {@code SheetRunner})
     * – es wird {@link IllegalStateException} geworfen. Ist der Turnier-Modus
     * nicht aktiv, sind Sheets ohnehin ungeschützt und es passiert nichts.
     * <p>
     * <b>Hinweis (future-proofing):</b> Der {@code TurnierModus.istAktiv()}-Check
     * verlässt sich auf den globalen Singleton-State; bei perspektivisch
     * mehreren parallel geöffneten Dokumenten mit unterschiedlichem Modus
     * wäre die alleinige Autorität über {@code state != null} robuster.
     * Heute genügt der Check.
     */
    public void ensureUnprotectedInScope() {
        ScopeState state = SCOPE.get();
        if (state == null) {
            if (TurnierModus.get().istAktiv()) {
                throw new IllegalStateException(
                        "Style/CF-Operation außerhalb eines aktiven BlattschutzScopes – "
                                + "diese Operation muss innerhalb eines SheetRunner.run() laufen.");
            }
            return;
        }
        if (!state.wurdeEntsperrt) {
            doEntsperren(state.konfig, state.ws);
            state.wurdeEntsperrt = true;
        }
    }

    /**
     * Convenience-Wrapper für Aufrufer, die Sheets <em>außerhalb</em> eines
     * {@link de.petanqueturniermanager.SheetRunner} mutieren – typischerweise
     * modale UNO-Dialoge aus dem {@code ProtocolHandler}/Dispatcher-Pfad (z.B.
     * Spieler-DB-Übernahme).
     * <p>
     * Liefert einen {@link AutoCloseable}, der {@link #beginCommandScope} öffnet
     * und beim {@code close} {@link #endCommandScope} schließt. Ist der
     * Turnier-Modus inaktiv oder kein Schutz-Mapping für {@code ts} registriert,
     * ist das Ergebnis ein no-op – der Aufrufer-Code bleibt unverändert.
     * <p>
     * Nutzung als try-with-resources:
     * <pre>
     * try (var ignored = BlattschutzManager.get().scopeFuer(ts, ws)) {
     *     ... Sheet-Schreibvorgang ...
     * }
     * </pre>
     */
    public BlattschutzScope scopeFuer(TurnierSystem ts, WorkingSpreadsheet ws) {
        if (ts == null || ts == TurnierSystem.KEIN || !TurnierModus.get().istAktiv()) {
            return () -> { };
        }
        IBlattschutzKonfiguration konfig = BlattschutzRegistry.fuer(ts).orElse(null);
        if (konfig == null) {
            return () -> { };
        }
        beginCommandScope(konfig, ws);
        return this::endCommandScope;
    }

    /**
     * AutoCloseable ohne checked Exception – passt zu try-with-resources im
     * Dispatcher-Pfad, der weder {@link Exception} fängt noch deklariert.
     */
    @FunctionalInterface
    public interface BlattschutzScope extends AutoCloseable {
        @Override
        void close();
    }

    // ── Public API (scope-aware) ─────────────────────────────────────────────

    /**
     * Aktiviert den Blattschutz für alle in der Konfiguration definierten Sheets.
     * <p>
     * Reihenfolge ist zwingend: Styles zuerst → Zellschutz setzen → Sheet sperren.
     * <p>
     * Innerhalb eines aktiven {@linkplain #beginCommandScope Command-Scopes} ist
     * dieser Aufruf ein No-Op – das Schützen passiert dann einmalig im
     * {@link #endCommandScope}.
     */
    public void schuetzen(IBlattschutzKonfiguration konfiguration, WorkingSpreadsheet ws) {
        if (SCOPE.get() != null) {
            logger.debug("schuetzen(): innerhalb Scope – als No-Op ignoriert");
            return;
        }
        doSchuetzen(konfiguration, ws);
    }

    /**
     * Entfernt den Blattschutz von allen in der Konfiguration definierten Sheets.
     * <p>
     * Cell-Level-Schutz muss nicht zurückgesetzt werden –
     * LibreOffice ignoriert {@code IsLocked} bei ungeschützten Sheets.
     * <p>
     * Innerhalb eines aktiven {@linkplain #beginCommandScope Command-Scopes} ist
     * dieser Aufruf ein No-Op – das Entsperren passiert lazy beim ersten
     * {@link #ensureUnprotectedInScope}.
     */
    public void entsperren(IBlattschutzKonfiguration konfiguration, WorkingSpreadsheet ws) {
        if (SCOPE.get() != null) {
            logger.debug("entsperren(): innerhalb Scope – als No-Op ignoriert");
            return;
        }
        doEntsperren(konfiguration, ws);
    }

    // ── Interne Implementierung (ohne Scope-Check) ──────────────────────────

    private void doSchuetzen(IBlattschutzKonfiguration konfiguration, WorkingSpreadsheet ws) {
        protectCount.incrementAndGet();
        // Schritt 1: CellStyles sichern BEVOR irgendein Sheet geschützt wird
        konfiguration.zelleStylesAktualisieren(ws);

        // Schritt 2: Schutz-Infos einmalig berechnen, ergänzt um system-übergreifende Sheets
        var infos = mitGlobalenSchutzInfos(konfiguration.berechneSchutzInfos(ws), ws);

        // Schritt 3: Pro Sheet entsperren → Zellschutz → sperren
        for (var info : infos) {
            try {
                entsperreSheet(info.sheet());
                for (var range : info.editierbareBereich()) {
                    setzeZellSchutzFreigegeben(info.sheet(), range);
                }
                schuetzeSheet(info.sheet());
            } catch (Exception e) {
                logger.warn("Sheet konnte nicht gesperrt werden: {}", e.getMessage(), e);
            }
        }
    }

    private void doEntsperren(IBlattschutzKonfiguration konfiguration, WorkingSpreadsheet ws) {
        unprotectCount.incrementAndGet();
        var infos = mitGlobalenSchutzInfos(konfiguration.berechneSchutzInfos(ws), ws);
        for (var info : infos) {
            try {
                entsperreSheet(info.sheet());
            } catch (Exception e) {
                logger.warn("Sheet konnte nicht entsperrt werden: {}", e.getMessage(), e);
            }
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Ergänzt die system-spezifischen Schutz-Infos um Sheets, die in jedem
     * Turniersystem im Turnier-Modus vollständig gesperrt sein müssen
     * (z.B. das Teilnehmer-Sheet). So muss diese Regel nicht in jeder
     * {@link IBlattschutzKonfiguration} dupliziert werden.
     */
    private List<SheetSchutzInfo> mitGlobalenSchutzInfos(List<SheetSchutzInfo> systemInfos, WorkingSpreadsheet ws) {
        var alle = new ArrayList<>(systemInfos);
        var xDoc = ws.getWorkingSpreadsheetDocument();
        SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.SCHLUESSEL_TEILNEHMER)
                .ifPresent(sheet -> alle.add(SheetSchutzInfo.vollGesperrt(sheet)));
        return alle;
    }

    private void schuetzeSheet(XSpreadsheet sheet) {
        Lo.qi(XProtectable.class, sheet).protect("");
    }

    private void entsperreSheet(XSpreadsheet sheet) {
        var xProt = Lo.qi(XProtectable.class, sheet);
        if (xProt.isProtected()) {
            xProt.unprotect("");
        }
    }

    /**
     * Fallback-Absicherung direkt am Schreibpunkt (z.B. {@code RangeHelper.setDataInRange}):
     * führt {@code schreibvorgang} garantiert auf einem physisch entsperrten Sheet aus.
     * <p>
     * Hintergrund: {@code TurnierModus.istAktiv()} ist ein <b>globaler</b> Singleton-Flag, der
     * pro Prozess (nicht pro Dokument) geführt wird. Bei mehreren gleichzeitig geöffneten
     * Dokumenten mit unterschiedlichem Kiosk-Zustand – oder wenn eine frühere Entsperr-Operation
     * fehlgeschlagen ist (siehe {@code doSchuetzen}/{@code doEntsperren}, die Fehler pro Sheet nur
     * loggen statt zu werfen) – kann der physische Blattschutz eines konkreten Sheets vom globalen
     * Flag abweichen. In diesem Fall würde {@link #ensureUnprotectedInScope()} No-Op bleiben und
     * der nachfolgende {@code setDataArray()}-Aufruf mit einer {@code RuntimeException} scheitern.
     * <p>
     * Prüft den physischen Zustand <em>immer</em> direkt am übergebenen Sheet – auch innerhalb
     * eines {@linkplain #beginCommandScope Command-Scopes}. Ist das Sheet dort bereits (via
     * {@code ensureUnprotectedInScope}) entsperrt, ist {@code warGeschuetzt} false und es
     * passiert kein zusätzliches Toggle. Ist dieses konkrete Sheet abweichend vom Scope-Zustand
     * dennoch noch gesperrt (z.B. weil es nicht Teil der {@code berechneSchutzInfos()} der
     * aktiven Konfiguration ist), wird es für die Dauer des Schreibvorgangs entsperrt und
     * danach exakt wiederhergestellt – {@code endCommandScope} schützt es ohnehin nicht, da es
     * außerhalb der Konfiguration liegt.
     */
    public void mitFallbackEntsperrt(XSpreadsheet sheet, Runnable schreibvorgang) {
        var xProt = Lo.qi(XProtectable.class, sheet);
        boolean warGeschuetzt = xProt.isProtected();
        if (warGeschuetzt) {
            xProt.unprotect("");
        }
        try {
            schreibvorgang.run();
        } finally {
            if (warGeschuetzt) {
                xProt.protect("");
            }
        }
    }

    /**
     * Setzt {@code CellProtection.IsLocked = false} auf dem angegebenen Bereich,
     * so dass die Zellen trotz Sheet-Schutz editierbar bleiben.
     * <p>
     * Schreibt bewusst ein neues {@link CellProtection}-Objekt mit allen Flags
     * auf ihren Standardwerten ({@code false}) – außer {@code IsLocked}, das
     * explizit auf {@code false} gesetzt wird. Das Lesen des alten Werts wird
     * vermieden, da {@code getPropertyValue} auf Mehr-Zellen-Bereichen mit
     * ambiguem Inhalt eine Exception werfen kann, die den Schreibvorgang
     * verhindert.
     */
    private void setzeZellSchutzFreigegeben(XSpreadsheet sheet, RangePosition range) {
        try {
            var xRange = sheet.getCellRangeByPosition(
                    range.getStartSpalte(), range.getStartZeile(),
                    range.getEndeSpalte(), range.getEndeZeile());
            var props = Lo.qi(XPropertySet.class, xRange);

            var cp = new CellProtection();
            cp.IsLocked = false;  // Zelle bleibt editierbar trotz Sheet-Schutz
            props.setPropertyValue("CellProtection", cp);

            // Verifikation: LO kann setPropertyValue still ignorieren wenn nLockCount > 0
            var result = (CellProtection) props.getPropertyValue("CellProtection");
            if (result.IsLocked) {
                logger.warn("IsLocked=false konnte nicht gesetzt werden für Bereich {} " +
                        "(LO nLockCount aktiv?) – wird beim nächsten formatDaten() korrigiert.", range);
            }
        } catch (Exception e) {
            logger.error("Zellschutz für Bereich {} konnte nicht gesetzt werden: {}",
                    range, e.getMessage(), e);
        }
    }

    // ── Test-Hooks (package-private) ─────────────────────────────────────────

    /** Nur für Tests: Anzahl echter {@code doSchuetzen}-Aufrufe seit dem letzten Reset. */
    int getProtectCallCount() {
        return protectCount.get();
    }

    /** Nur für Tests: Anzahl echter {@code doEntsperren}-Aufrufe seit dem letzten Reset. */
    int getUnprotectCallCount() {
        return unprotectCount.get();
    }

    /** Nur für Tests: setzt die Call-Counter zurück. */
    void resetCallCounters() {
        protectCount.set(0);
        unprotectCount.set(0);
    }

    /** Nur für Tests: räumt den ThreadLocal-Scope auf (z. B. zwischen Test-Methoden). */
    void resetScopeForTest() {
        SCOPE.remove();
    }

}
