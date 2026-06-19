package de.petanqueturniermanager.helper.sheetsync;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XModel;
import com.sun.star.container.XNamed;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.perflog.PerfLog;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * Lauscht auf Sheet-Tab-Wechsel und OS-Fokusereignisse und löst einen Sheet-Sync
 * <b>nur dann</b> aus, wenn sich die semantisch relevanten Eingangsdaten seit dem letzten
 * Rebuild geändert haben.
 * <p>
 * Erkennung über kanonische SHA-256-Signatur ({@link EingabeSignatur}), Vergleich
 * mit persistiertem Hash im Dokument ({@link SheetSyncSignaturStore}). Trigger werden vor
 * dem Hash-Check über den {@link SheetSyncDebouncer} entkoppelt, damit Event-Stürme
 * zu einem Check zusammengezogen werden und der Hash-Check nicht im UI-Thread läuft.
 * <p>
 * Generisch nutzbar für jede Sheet-Art, deren Daten aus einer Eingabe-Quelle abgeleitet
 * werden (Rangliste aus Meldeliste+Spielrunden, Teilnehmerliste aus Meldeliste, …).
 * <p>
 * Architekturregeln siehe {@code turniersysteme/RANGLISTE_LISTENER.md}.
 * <p>
 * Zusätzlich {@link ITurnierEventListener}: Ein {@code PropertiesChanged}-Event (z.B.
 * Wechsel des Sortiermodus im Konfig-Dialog) ist nicht aus Sheet-Zellen ableitbar und
 * löst daher keinen Tab-Wechsel/Fokus-Trigger aus. Reagiert der Listener auf das Event,
 * wird – sofern das Ziel-Sheet gerade aktiv ist – sofort ein Hash-Check geplant; bei
 * geänderter Signatur (Sortier-Zusatzkontext) folgt der Re-Sync live.
 */
public final class SheetSyncListener implements IGlobalEventListener, ITurnierEventListener {

    private static final Logger logger = LogManager.getLogger(SheetSyncListener.class);

    /** Safety-Revalidation: nach diesem Intervall wird der gespeicherte Hash neu bestätigt. */
    private static final Duration VERIFY_INTERVAL = Duration.ofMinutes(10);

    private final XComponentContext xContext;
    private final BiPredicate<XSpreadsheetDocument, XSpreadsheet> zielSheetMatch;
    private final TurnierSystem erwartesTurnierSystem;
    private final BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory;
    /** Signatur-Engine pro (Doc, Ziel-Sheet). Spieltag-Variante: dynamisch pro Spieltag-Nr. */
    private final BiFunction<XSpreadsheetDocument, XSpreadsheet, EingabeSignatur> signaturLieferant;
    /** Persistenz-Schlüssel pro (Doc, Ziel-Sheet). Spieltag-Variante: inkl. Nr. */
    private final BiFunction<XSpreadsheetDocument, XSpreadsheet, String> schluesselLieferant;

    /** Bereits registrierte Dokumente – verhindert Doppelregistrierung. */
    private final Set<XSpreadsheetDocument> registriert =
            Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * forceNextCheck pro (Doc-IdentityHash + Schlüssel): gesetzt nach erschöpften
     * Transient-Retries oder bei abgelaufener Safety-Revalidation. Beim nächsten Trigger
     * wird der Skip-Pfad umgangen.
     */
    private final ConcurrentHashMap<String, Boolean> forceNextCheck = new ConcurrentHashMap<>();

    // ── Factory-Methoden ────────────────────────────────────────────────────

    /**
     * Erzeugt einen Listener für Sheets mit festem Named-Range-Schlüssel.
     */
    public static SheetSyncListener fuerSchluessel(
            XComponentContext xContext,
            String namedRangeKey,
            TurnierSystem erwartesTurnierSystem,
            EingabeSignatur signatur,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
        return new SheetSyncListener(xContext,
                (xDoc, sheet) -> SheetMetadataHelper.istRegistriertesSheet(xDoc, sheet, namedRangeKey),
                erwartesTurnierSystem,
                (xDoc, sheet) -> signatur,
                (xDoc, sheet) -> namedRangeKey,
                runnerFactory);
    }

    /**
     * Erzeugt einen Listener für Supermelee-Spieltag-Ranglisten (dynamische Schlüssel).
     * Pro Spieltag-Nr gibt es eine eigene Signatur und einen eigenen Persistenz-Schlüssel.
     *
     * @param signaturProSpieltagNr  Funktion {@code spieltagNr → EingabeSignatur}.
     *                               Wird intern pro Nr gecached.
     */
    public static SheetSyncListener fuerSpieltagRangliste(
            XComponentContext xContext,
            TurnierSystem erwartesTurnierSystem,
            IntFunction<EingabeSignatur> signaturProSpieltagNr,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
        return fuerSpieltagSheet(xContext, erwartesTurnierSystem,
                SheetMetadataHelper::findeSpieltagNr,
                "SUPERMELEE_SPIELTAG_",
                signaturProSpieltagNr, runnerFactory);
    }

    /**
     * Erzeugt einen Listener für Supermelee-Spieltag-Teilnehmerlisten.
     * Pro Spieltag-Nr gibt es eine eigene Signatur und einen eigenen Persistenz-Schlüssel.
     */
    public static SheetSyncListener fuerSpieltagTeilnehmer(
            XComponentContext xContext,
            TurnierSystem erwartesTurnierSystem,
            IntFunction<EingabeSignatur> signaturProSpieltagNr,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
        return fuerSpieltagSheet(xContext, erwartesTurnierSystem,
                SheetMetadataHelper::findeTeilnehmerSpieltagNr,
                "SUPERMELEE_TEILNEHMER_",
                signaturProSpieltagNr, runnerFactory);
    }

    /**
     * Generischer Spieltag-Sheet-Listener: matcht alle Sheets, für die der
     * {@code spieltagNrLookup} eine Nr liefert, und persistiert pro Nr.
     */
    public static SheetSyncListener fuerSpieltagSheet(
            XComponentContext xContext,
            TurnierSystem erwartesTurnierSystem,
            BiFunction<XSpreadsheetDocument, XSpreadsheet, Optional<SpielTagNr>> spieltagNrLookup,
            String persistenzPrefix,
            IntFunction<EingabeSignatur> signaturProSpieltagNr,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
        ConcurrentHashMap<Integer, EingabeSignatur> cache = new ConcurrentHashMap<>();
        BiFunction<XSpreadsheetDocument, XSpreadsheet, Optional<Integer>> nrLookup =
                (xDoc, sheet) -> spieltagNrLookup.apply(xDoc, sheet).map(SpielTagNr::getNr);
        return new SheetSyncListener(xContext,
                (xDoc, sheet) -> nrLookup.apply(xDoc, sheet).isPresent(),
                erwartesTurnierSystem,
                (xDoc, sheet) -> nrLookup.apply(xDoc, sheet)
                        .map(n -> cache.computeIfAbsent(n, signaturProSpieltagNr::apply))
                        .orElse(null),
                (xDoc, sheet) -> nrLookup.apply(xDoc, sheet)
                        .map(n -> persistenzPrefix + n).orElse(null),
                runnerFactory);
    }

    SheetSyncListener(XComponentContext xContext,
            BiPredicate<XSpreadsheetDocument, XSpreadsheet> zielSheetMatch,
            TurnierSystem erwartesTurnierSystem,
            BiFunction<XSpreadsheetDocument, XSpreadsheet, EingabeSignatur> signaturLieferant,
            BiFunction<XSpreadsheetDocument, XSpreadsheet, String> schluesselLieferant,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
        this.xContext = checkNotNull(xContext);
        this.zielSheetMatch = checkNotNull(zielSheetMatch);
        this.erwartesTurnierSystem = checkNotNull(erwartesTurnierSystem);
        this.signaturLieferant = checkNotNull(signaturLieferant);
        this.schluesselLieferant = checkNotNull(schluesselLieferant);
        this.runnerFactory = checkNotNull(runnerFactory);
    }

    private boolean istPassendesDokument(XSpreadsheetDocument xDoc) {
        return new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument() == erwartesTurnierSystem;
    }

    // ── Dokument-Laden ──────────────────────────────────────────────────────

    @Override
    public void onLoadFinished(Object source) {
        registriereListener(source);
    }

    @Override
    public void onNew(Object source) {
        registriereListener(source);
    }

    @Override
    public void onViewCreated(Object source) {
        registriereListener(source);
    }

    private void registriereListener(Object source) {
        try {
            XModel xModel = Lo.qi(XModel.class, source);
            if (xModel == null) return;
            XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
            if (xDoc == null) return;

            synchronized (registriert) {
                if (registriert.contains(xDoc)) return;
                registriert.add(xDoc);
            }

            registriereSelectionChangeListener(xModel, xDoc);

        } catch (RuntimeException t) {
            logger.error("Fehler beim Registrieren der Listener", t);
        }
    }

    private void registriereSelectionChangeListener(XModel xModel, XSpreadsheetDocument xDoc) {
        var controller = xModel.getCurrentController();
        if (controller == null) return;
        boolean istSpreadsheetView = Lo.qi(XSpreadsheetView.class, controller) != null;
        logger.debug("registriereSelectionChangeListener: controller-typ={}",
                istSpreadsheetView ? "ScTabViewShell" : "Druckvorschau/Sonstige");
        if (!istSpreadsheetView) return;
        XSelectionSupplier selSupplier = Lo.qi(XSelectionSupplier.class, controller);
        if (selSupplier == null) return;

        selSupplier.addSelectionChangeListener(new XSelectionChangeListener() {
            private XSpreadsheet letztesSheet = null;

            @Override
            public void selectionChanged(EventObject e) {
                boolean tabTimingAktiv = PerfLog.isEnabled();
                long startNs = System.nanoTime();
                String traceId = traceId(xDoc, startNs);
                String sheetName = "<unbekannt>";
                String key = null;
                String ergebnis = "start";
                long viewNs = 0;
                long activeSheetNs = 0;
                long matchNs = 0;
                long systemNs = 0;
                long planNs = 0;
                try {
                    long abschnittNs = System.nanoTime();
                    XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, e.Source);
                    viewNs = elapsedNs(abschnittNs);
                    if (view == null) {
                        ergebnis = "keineSpreadsheetView";
                        return;
                    }

                    abschnittNs = System.nanoTime();
                    XSpreadsheet aktuellesSheet = view.getActiveSheet();
                    activeSheetNs = elapsedNs(abschnittNs);
                    if (aktuellesSheet == null) {
                        ergebnis = "keinAktivesSheet";
                        return;
                    }
                    if (tabTimingAktiv) {
                        sheetName = sheetName(aktuellesSheet);
                    }

                    XSpreadsheet vorherigesSheet = letztesSheet;
                    letztesSheet = aktuellesSheet;

                    if (aktuellesSheet == vorherigesSheet) {
                        ergebnis = "gleichesSheet";
                        return;
                    }

                    abschnittNs = System.nanoTime();
                    boolean istAufZielSheet = zielSheetMatch.test(xDoc, aktuellesSheet);
                    boolean warAufZielSheet = vorherigesSheet != null
                            && zielSheetMatch.test(xDoc, vorherigesSheet);
                    matchNs = elapsedNs(abschnittNs);

                    if (!istAufZielSheet) {
                        ergebnis = "keinZielSheet";
                        return;
                    }
                    if (warAufZielSheet) {
                        ergebnis = "zielSheetZuZielSheet";
                        return;
                    }
                    if (SheetRunner.isRunning()) {
                        ergebnis = "sheetRunnerLaeuft";
                        return;
                    }
                    abschnittNs = System.nanoTime();
                    boolean passendesDokument = istPassendesDokument(xDoc);
                    systemNs = elapsedNs(abschnittNs);
                    if (!passendesDokument) {
                        ergebnis = "falschesTurniersystem";
                        return;
                    }
                    if (SheetRunner.consumeSelectionChangeSuppression()) {
                        logger.trace("selectionChanged: Unterdrückt – ausgelöst durch setActiveSheet()");
                        ergebnis = "unterdrueckt";
                        return;
                    }
                    key = schluesselLieferant.apply(xDoc, aktuellesSheet);
                    abschnittNs = System.nanoTime();
                    plane(xDoc, aktuellesSheet, "selectionChanged", traceId, key);
                    planNs = elapsedNs(abschnittNs);
                    ergebnis = "geplant";
                } catch (RuntimeException t) {
                    ergebnis = "fehler";
                    logger.error("Fehler im SelectionChangeListener", t);
                } finally {
                    logTabTiming("SheetSync.selectionChanged", traceId, "selectionChanged",
                            sheetName, key, ergebnis, startNs, viewNs, activeSheetNs, matchNs,
                            systemNs, planNs);
                }
            }

            @Override
            public void disposing(EventObject e) {
                SheetSyncDebouncer.get().cancelAlle(xDoc);
            }
        });

        logger.debug("XSelectionChangeListener für Sheet-Sync registriert");
    }

    // ── OnFocus ─────────────────────────────────────────────────────────────

    @Override
    public void onFocus(Object source) {
        boolean tabTimingAktiv = PerfLog.isEnabled();
        long startNs = System.nanoTime();
        String traceId = traceId(null, startNs);
        String sheetName = "<unbekannt>";
        String key = null;
        String ergebnis = "start";
        long viewNs = 0;
        long activeSheetNs = 0;
        long matchNs = 0;
        long systemNs = 0;
        long planNs = 0;
        try {
            XModel xModel = Lo.qi(XModel.class, source);
            if (xModel == null) {
                ergebnis = "keinModel";
                return;
            }

            XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
            traceId = traceId(xDoc, startNs);
            if (xDoc == null) {
                ergebnis = "keinSpreadsheetDocument";
                return;
            }

            var aktuellerController = xModel.getCurrentController();
            long abschnittNs = System.nanoTime();
            XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, aktuellerController);
            viewNs = elapsedNs(abschnittNs);
            if (view == null) {
                ergebnis = "keineSpreadsheetView";
                return;
            }

            abschnittNs = System.nanoTime();
            XSpreadsheet aktuellesSheet = view.getActiveSheet();
            activeSheetNs = elapsedNs(abschnittNs);
            if (aktuellesSheet == null) {
                ergebnis = "keinAktivesSheet";
                return;
            }
            if (tabTimingAktiv) {
                sheetName = sheetName(aktuellesSheet);
            }

            abschnittNs = System.nanoTime();
            boolean istAufZielSheet = zielSheetMatch.test(xDoc, aktuellesSheet);
            matchNs = elapsedNs(abschnittNs);
            if (!istAufZielSheet) {
                ergebnis = "keinZielSheet";
                return;
            }
            if (SheetRunner.isRunning()) {
                ergebnis = "sheetRunnerLaeuft";
                return;
            }
            abschnittNs = System.nanoTime();
            boolean passendesDokument = istPassendesDokument(xDoc);
            systemNs = elapsedNs(abschnittNs);
            if (!passendesDokument) {
                ergebnis = "falschesTurniersystem";
                return;
            }
            if (SheetRunner.consumeSelectionChangeSuppression()) {
                logger.debug("onFocus: Unterdrückt – ausgelöst durch setActiveSheet()");
                ergebnis = "unterdrueckt";
                return;
            }

            key = schluesselLieferant.apply(xDoc, aktuellesSheet);
            if (key != null && SheetSyncSignaturStore.verifyVeraltet(xDoc, key, VERIFY_INTERVAL)) {
                markiereForce(xDoc, key);
            }
            abschnittNs = System.nanoTime();
            plane(xDoc, aktuellesSheet, "onFocus", traceId, key);
            planNs = elapsedNs(abschnittNs);
            ergebnis = "geplant";

        } catch (RuntimeException t) {
            ergebnis = "fehler";
            logger.error("Fehler beim OnFocus-Sheet-Sync", t);
        } finally {
            logTabTiming("SheetSync.onFocus", traceId, "onFocus", sheetName, key, ergebnis,
                    startNs, viewNs, activeSheetNs, matchNs, systemNs, planNs);
        }
    }

    // ── PropertiesChanged ───────────────────────────────────────────────────

    /**
     * Reagiert auf einen Konfig-Property-Wechsel: Ist das Ziel-Sheet im auslösenden
     * Dokument gerade aktiv, wird ein Hash-Check geplant. Nur bei tatsächlich geänderter
     * Signatur (z.B. neuer Sortier-Zusatzkontext) startet danach der Re-Sync – andere
     * Property-Änderungen lassen den Hash unverändert und werden so übersprungen.
     */
    @Override
    public void onPropertiesChanged(ITurnierEvent eventObj) {
        try {
            if (eventObj == null) return;
            XSpreadsheetDocument xDoc = eventObj.getWorkingSpreadsheetDocument();
            if (xDoc == null) return;
            if (SheetRunner.isRunning()) return;
            if (!istPassendesDokument(xDoc)) return;

            XSpreadsheet aktuellesSheet = aktivesSheet(xDoc);
            if (aktuellesSheet == null) return;
            if (!zielSheetMatch.test(xDoc, aktuellesSheet)) return;

            plane(xDoc, aktuellesSheet, "propertiesChanged", null, null);
        } catch (RuntimeException t) {
            logger.error("Fehler beim PropertiesChanged-Sheet-Sync", t);
        }
    }

    /**
     * Prüft per Probe-Call, ob das Dokument bereits disposed ist. Bei einem geschlossenen
     * Dokument wirft jede UNO-Methode eine {@link DisposedException}.
     */
    private static boolean istDisposed(XSpreadsheetDocument xDoc) {
        XComponent comp = UnoRuntime.queryInterface(XComponent.class, xDoc);
        if (comp == null) {
            return true;
        }
        try {
            xDoc.getSheets();
            return false;
        } catch (DisposedException ignored) {
            return true;
        } catch (RuntimeException e) {
            logger.debug("Dokument-Lebendigkeit nicht prüfbar, behandle als disposed", e);
            return true;
        }
    }

    private static XSpreadsheet aktivesSheet(XSpreadsheetDocument xDoc) {
        XModel xModel = Lo.qi(XModel.class, xDoc);
        if (xModel == null) return null;
        XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());
        if (view == null) return null;
        return view.getActiveSheet();
    }

    // ── Plan + Check ────────────────────────────────────────────────────────

    private void plane(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String grund,
            String traceId, String vorberechneterKey) {
        String key = vorberechneterKey != null ? vorberechneterKey : schluesselLieferant.apply(xDoc, sheet);
        if (key == null) {
            logger.warn("Kein Persistenz-Schlüssel ermittelbar – Sync übersprungen");
            return;
        }
        EingabeSignatur sig = signaturLieferant.apply(xDoc, sheet);
        if (sig == null) {
            logger.warn("Keine Signatur-Engine ermittelbar (key={}) – Sync übersprungen", key);
            return;
        }
        if (PerfLog.isEnabled()) {
            PerfLog.log(logger, "[TAB-TIMING] SheetSync.schedule trace={} trigger={} key={} system={} sheet={} thread={}",
                    traceId, grund, key, erwartesTurnierSystem, sheetName(sheet), Thread.currentThread().getName());
        }
        SheetSyncDebouncer.get().schedule(xDoc, key,
                () -> pruefeUndStarte(xDoc, sheet, sig, key, grund, 1));
    }

    private void pruefeUndStarte(XSpreadsheetDocument xDoc, XSpreadsheet sheet,
            EingabeSignatur signatur, String key, String grund, int versuch) {
        // Race-Schutz: Das Dokument kann zwischen dem Einplanen und dem Feuern dieses
        // debounced Tasks geschlossen worden sein – cancelAlle() im disposing-Handler greift
        // für einen bereits laufenden Task nicht mehr. Jeder UNO-Call würde dann eine
        // DisposedException werfen (Store-Zugriff, WorkingSpreadsheet-Aufbau). Lautlos abbrechen.
        if (istDisposed(xDoc)) {
            logger.debug("Sheet-Sync übersprungen – Dokument bereits geschlossen (key={})", key);
            return;
        }
        long startNs = System.nanoTime();
        long hashStartNs = startNs;
        SignaturErgebnis ergebnis = signatur.berechne(xDoc, versuch);
        long hashDauerMs = (System.nanoTime() - hashStartNs) / 1_000_000L;
        switch (ergebnis) {
            case SignaturErgebnis.Ok ok -> handleOk(xDoc, sheet, key, grund, ok.hash());
            case SignaturErgebnis.SheetFehlt fehlt -> handleSheetFehlt(xDoc, sheet, key, fehlt);
            case SignaturErgebnis.TransientFehler te ->
                handleTransient(xDoc, sheet, signatur, key, grund, te);
            case SignaturErgebnis.PermanenterFehler pe -> logger.warn(
                    "Sheet-Sync-Signatur fehlgeschlagen (permanent, key={}): {}", key, pe.grund(),
                    pe.cause());
        }
        long gesamtMs = (System.nanoTime() - startNs) / 1_000_000L;
        PerfLog.log(logger, "[WORKER-TIMING] SheetSyncListener.pruefeUndStarte key={} system={} hash={} ms gesamt={} ms ergebnis={} thread={}",
                key, erwartesTurnierSystem, hashDauerMs, gesamtMs,
                ergebnis.getClass().getSimpleName(), Thread.currentThread().getName());
    }

    private void handleOk(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String key,
            String grund, String hash) {
        boolean force = verbrauchForce(xDoc, key);
        Optional<String> gespeichert = SheetSyncSignaturStore.ladeHash(xDoc, key);
        if (!force && gespeichert.isPresent() && gespeichert.get().equals(hash)) {
            SheetSyncSignaturStore.aktualisiereVerifyZeit(xDoc, key);
            logger.debug("Sheet-Sync übersprungen (Hash unverändert, key={})", key);
            return;
        }
        if (SheetRunner.isRunning()) {
            logger.debug("Sheet-Sync ausgelassen – SheetRunner läuft (key={})", key);
            return;
        }
        String effektiverGrund = force ? "forcedRevalidation"
                : (gespeichert.isEmpty() ? "erstaufbau" : "hashMismatch");
        logger.debug("Sheet-Sync START – key={}, trigger={}, grund={}",
                key, grund, effektiverGrund);
        // Hash vor dem asynchronen Runner speichern: stimmt mit den jetzt gelesenen Eingaben
        // überein. Sollte der Rebuild scheitern, wird beim nächsten Trigger der dann frische
        // Hash neu verglichen.
        SheetSyncSignaturStore.speichereNachRebuild(xDoc, key, hash, effektiverGrund);
        runnerFactory.apply(new WorkingSpreadsheet(xContext, xDoc), sheet).startSilent();
    }

    private void handleSheetFehlt(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String key,
            SignaturErgebnis.SheetFehlt fehlt) {
        if (!fehlt.erwartet()) {
            logger.debug("Sheet-Sync-Quelle '{}' fehlt (optional) – skip", fehlt.stabileId());
            return;
        }
        if (SheetSyncSignaturStore.recoveryBereitsVersucht(xDoc, key)) {
            logger.debug("Sheet-Sync-Quelle '{}' fehlt erwartet, Recovery bereits versucht – skip",
                    fehlt.stabileId());
            return;
        }
        if (istDisposed(xDoc)) {
            logger.debug("Sheet-Sync-Recovery übersprungen – Dokument bereits geschlossen (key={})", key);
            return;
        }
        if (SheetRunner.isRunning()) {
            return;
        }
        logger.warn("Sheet-Sync-Quelle '{}' fehlt erwartet (key={}) – Recovery-Rebuild",
                fehlt.stabileId(), key);
        SheetSyncSignaturStore.markiereRecoveryVersucht(xDoc, key);
        if (istDisposed(xDoc)) {
            logger.debug("Sheet-Sync-Recovery-Runner übersprungen – Dokument bereits geschlossen (key={})", key);
            return;
        }
        runnerFactory.apply(new WorkingSpreadsheet(xContext, xDoc), sheet).startSilent();
    }

    private void handleTransient(XSpreadsheetDocument xDoc, XSpreadsheet sheet,
            EingabeSignatur signatur, String key, String grund,
            SignaturErgebnis.TransientFehler te) {
        if (te.versuch() < SheetSyncDebouncer.MAX_RETRY) {
            int naechster = te.versuch() + 1;
            logger.debug("Sheet-Sync-Signatur transient fehlgeschlagen (key={}, versuch={}): {} – Retry",
                    key, te.versuch(), te.grund());
            SheetSyncDebouncer.get().scheduleRetry(xDoc, key, naechster,
                    () -> pruefeUndStarte(xDoc, sheet, signatur, key, grund, naechster));
        } else {
            logger.warn("Sheet-Sync-Signatur transient fehlgeschlagen (key={}, versuch={}): {} – "
                    + "forceNextCheck gesetzt", key, te.versuch(), te.grund(), te.cause());
            markiereForce(xDoc, key);
        }
    }

    private void markiereForce(XSpreadsheetDocument xDoc, String key) {
        forceNextCheck.put(forceKey(xDoc, key), Boolean.TRUE);
    }

    private boolean verbrauchForce(XSpreadsheetDocument xDoc, String key) {
        return forceNextCheck.remove(forceKey(xDoc, key)) != null;
    }

    private static String forceKey(XSpreadsheetDocument xDoc, String key) {
        return System.identityHashCode(xDoc) + ":" + key;
    }

    private static long elapsedNs(long startNs) {
        return System.nanoTime() - startNs;
    }

    private static long nsToMs(long ns) {
        return ns / 1_000_000L;
    }

    private static String traceId(XSpreadsheetDocument xDoc, long startNs) {
        return (xDoc == null ? "keinDoc" : Integer.toString(System.identityHashCode(xDoc)))
                + ":" + Long.toUnsignedString(startNs);
    }

    private static String sheetName(XSpreadsheet sheet) {
        if (sheet == null) return "<keinSheet>";
        try {
            XNamed xNamed = Lo.qi(XNamed.class, sheet);
            if (xNamed != null && xNamed.getName() != null) {
                return xNamed.getName();
            }
        } catch (RuntimeException e) {
            logger.trace("Sheet-Name für Tab-Timing nicht ermittelbar", e);
        }
        return "<unbekannt>";
    }

    private void logTabTiming(String abschnitt, String traceId, String trigger,
            String sheetName, String key, String ergebnis, long startNs, long viewNs,
            long activeSheetNs, long matchNs, long systemNs, long planNs) {
        if (!PerfLog.isEnabled()) return;
        long gesamtMs = nsToMs(elapsedNs(startNs));
        PerfLog.log(logger, "[TAB-TIMING] {} trace={} trigger={} system={} sheet={} key={} ergebnis={} "
                + "gesamt={} ms view={} ms activeSheet={} ms match={} ms system={} ms plan={} ms thread={}",
                abschnitt, traceId, trigger, erwartesTurnierSystem, sheetName, key, ergebnis,
                gesamtMs, nsToMs(viewNs), nsToMs(activeSheetNs), nsToMs(matchNs),
                nsToMs(systemNs), nsToMs(planNs), Thread.currentThread().getName());
    }
}
