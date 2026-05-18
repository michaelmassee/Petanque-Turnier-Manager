package de.petanqueturniermanager.helper.rangliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.XComponentContext;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Lauscht auf Sheet-Tab-Wechsel und OS-Fokusereignisse und löst einen Rangliste-Refresh
 * <b>nur dann</b> aus, wenn sich die semantisch relevanten Eingangsdaten seit dem letzten
 * Rebuild geändert haben.
 * <p>
 * Erkennung über kanonische SHA-256-Signatur ({@link RanglisteEingabeSignatur}), Vergleich
 * mit persistiertem Hash im Dokument ({@link RanglisteSignaturStore}). Trigger werden vor
 * dem Hash-Check über den {@link RanglisteRefreshDebouncer} entkoppelt, damit Event-Stürme
 * zu einem Check zusammengezogen werden und der Hash-Check nicht im UI-Thread läuft.
 * <p>
 * Architekturregeln siehe {@code turniersysteme/RANGLISTE_LISTENER.md}.
 */
public final class RanglisteRefreshListener implements IGlobalEventListener {

    private static final Logger logger = LogManager.getLogger(RanglisteRefreshListener.class);

    /** Safety-Revalidation: nach diesem Intervall wird der gespeicherte Hash neu bestätigt. */
    private static final Duration VERIFY_INTERVAL = Duration.ofMinutes(10);

    private final XComponentContext xContext;
    private final BiPredicate<XSpreadsheetDocument, XSpreadsheet> ranglisteMatch;
    private final TurnierSystem erwartesTurnierSystem;
    private final BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory;
    /** Signatur-Engine pro (Doc, Rangliste-Sheet). Spieltag-Variante: dynamisch pro Spieltag-Nr. */
    private final BiFunction<XSpreadsheetDocument, XSpreadsheet, RanglisteEingabeSignatur> signaturLieferant;
    /** Persistenz-Schlüssel pro (Doc, Rangliste-Sheet). Spieltag-Variante: inkl. Nr. */
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
    public static RanglisteRefreshListener fuerSchluessel(
            XComponentContext xContext,
            String namedRangeKey,
            TurnierSystem erwartesTurnierSystem,
            RanglisteEingabeSignatur signatur,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
        return new RanglisteRefreshListener(xContext,
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
     * @param signaturProSpieltagNr  Funktion {@code spieltagNr → RanglisteEingabeSignatur}.
     *                               Wird intern pro Nr gecached.
     */
    public static RanglisteRefreshListener fuerSpieltagRangliste(
            XComponentContext xContext,
            TurnierSystem erwartesTurnierSystem,
            java.util.function.IntFunction<RanglisteEingabeSignatur> signaturProSpieltagNr,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
        ConcurrentHashMap<Integer, RanglisteEingabeSignatur> cache = new ConcurrentHashMap<>();
        BiFunction<XSpreadsheetDocument, XSpreadsheet, Optional<Integer>> nrLookup =
                (xDoc, sheet) -> SheetMetadataHelper.findeSpieltagNr(xDoc, sheet)
                        .map(SpielTagNr::getNr);
        return new RanglisteRefreshListener(xContext,
                (xDoc, sheet) -> nrLookup.apply(xDoc, sheet).isPresent(),
                erwartesTurnierSystem,
                (xDoc, sheet) -> nrLookup.apply(xDoc, sheet)
                        .map(n -> cache.computeIfAbsent(n, signaturProSpieltagNr::apply))
                        .orElse(null),
                (xDoc, sheet) -> nrLookup.apply(xDoc, sheet)
                        .map(n -> "SUPERMELEE_SPIELTAG_" + n).orElse(null),
                runnerFactory);
    }

    RanglisteRefreshListener(XComponentContext xContext,
            BiPredicate<XSpreadsheetDocument, XSpreadsheet> ranglisteMatch,
            TurnierSystem erwartesTurnierSystem,
            BiFunction<XSpreadsheetDocument, XSpreadsheet, RanglisteEingabeSignatur> signaturLieferant,
            BiFunction<XSpreadsheetDocument, XSpreadsheet, String> schluesselLieferant,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> runnerFactory) {
        this.xContext = checkNotNull(xContext);
        this.ranglisteMatch = checkNotNull(ranglisteMatch);
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
                try {
                    XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, e.Source);
                    if (view == null) return;

                    XSpreadsheet aktuellesSheet = view.getActiveSheet();
                    if (aktuellesSheet == null) return;

                    XSpreadsheet vorherigesSheet = letztesSheet;
                    letztesSheet = aktuellesSheet;

                    if (aktuellesSheet == vorherigesSheet) return;

                    boolean istAufRangliste = ranglisteMatch.test(xDoc, aktuellesSheet);
                    boolean warAufRangliste = vorherigesSheet != null
                            && ranglisteMatch.test(xDoc, vorherigesSheet);

                    if (istAufRangliste && !warAufRangliste && !SheetRunner.isRunning()
                            && istPassendesDokument(xDoc)) {
                        if (SheetRunner.consumeSelectionChangeSuppression()) {
                            logger.trace("selectionChanged: Unterdrückt – ausgelöst durch setActiveSheet()");
                            return;
                        }
                        plane(xDoc, aktuellesSheet, "selectionChanged");
                    }
                } catch (RuntimeException t) {
                    logger.error("Fehler im SelectionChangeListener", t);
                }
            }

            @Override
            public void disposing(EventObject e) {
                RanglisteRefreshDebouncer.get().cancelAlle(xDoc);
            }
        });

        logger.debug("XSelectionChangeListener für Rangliste registriert");
    }

    // ── OnFocus ─────────────────────────────────────────────────────────────

    @Override
    public void onFocus(Object source) {
        try {
            XModel xModel = Lo.qi(XModel.class, source);
            if (xModel == null) return;

            XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
            if (xDoc == null) return;

            var aktuellerController = xModel.getCurrentController();
            XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, aktuellerController);
            if (view == null) return;

            XSpreadsheet aktuellesSheet = view.getActiveSheet();
            if (aktuellesSheet == null) return;

            if (!ranglisteMatch.test(xDoc, aktuellesSheet)) return;
            if (SheetRunner.isRunning()) return;
            if (!istPassendesDokument(xDoc)) return;
            if (SheetRunner.consumeSelectionChangeSuppression()) {
                logger.debug("onFocus: Unterdrückt – ausgelöst durch setActiveSheet()");
                return;
            }

            String key = schluesselLieferant.apply(xDoc, aktuellesSheet);
            if (key != null && RanglisteSignaturStore.verifyVeraltet(xDoc, key, VERIFY_INTERVAL)) {
                markiereForce(xDoc, key);
            }
            plane(xDoc, aktuellesSheet, "onFocus");

        } catch (RuntimeException t) {
            logger.error("Fehler beim OnFocus-Ranglisten-Refresh", t);
        }
    }

    // ── Plan + Check ────────────────────────────────────────────────────────

    private void plane(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String grund) {
        String key = schluesselLieferant.apply(xDoc, sheet);
        if (key == null) {
            logger.warn("Kein Persistenz-Schlüssel ermittelbar – Refresh übersprungen");
            return;
        }
        RanglisteEingabeSignatur sig = signaturLieferant.apply(xDoc, sheet);
        if (sig == null) {
            logger.warn("Keine Signatur-Engine ermittelbar (key={}) – Refresh übersprungen", key);
            return;
        }
        RanglisteRefreshDebouncer.get().schedule(xDoc, key,
                () -> pruefeUndStarte(xDoc, sheet, sig, key, grund, 1));
    }

    private void pruefeUndStarte(XSpreadsheetDocument xDoc, XSpreadsheet sheet,
            RanglisteEingabeSignatur signatur, String key, String grund, int versuch) {
        SignaturErgebnis ergebnis = signatur.berechne(xDoc, versuch);
        switch (ergebnis) {
            case SignaturErgebnis.Ok ok -> handleOk(xDoc, sheet, key, grund, ok.hash());
            case SignaturErgebnis.SheetFehlt fehlt -> handleSheetFehlt(xDoc, sheet, key, fehlt);
            case SignaturErgebnis.TransientFehler te ->
                handleTransient(xDoc, sheet, signatur, key, grund, te);
            case SignaturErgebnis.PermanenterFehler pe -> logger.warn(
                    "Rangliste-Signatur fehlgeschlagen (permanent, key={}): {}", key, pe.grund(),
                    pe.cause());
        }
    }

    private void handleOk(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String key,
            String grund, String hash) {
        boolean force = verbrauchForce(xDoc, key);
        Optional<String> gespeichert = RanglisteSignaturStore.ladeHash(xDoc, key);
        if (!force && gespeichert.isPresent() && gespeichert.get().equals(hash)) {
            RanglisteSignaturStore.aktualisiereVerifyZeit(xDoc, key);
            logger.debug("Rangliste-Refresh übersprungen (Hash unverändert, key={})", key);
            return;
        }
        if (SheetRunner.isRunning()) {
            logger.debug("Rangliste-Refresh ausgelassen – SheetRunner läuft (key={})", key);
            return;
        }
        String effektiverGrund = force ? "forcedRevalidation"
                : (gespeichert.isEmpty() ? "erstaufbau" : "hashMismatch");
        logger.debug("Rangliste-Refresh START – key={}, trigger={}, grund={}",
                key, grund, effektiverGrund);
        // Hash vor dem asynchronen Runner speichern: stimmt mit den jetzt gelesenen Eingaben
        // überein. Sollte der Rebuild scheitern, wird beim nächsten Trigger der dann frische
        // Hash neu verglichen.
        RanglisteSignaturStore.speichereNachRebuild(xDoc, key, hash, effektiverGrund);
        runnerFactory.apply(new WorkingSpreadsheet(xContext, xDoc), sheet).startSilent();
    }

    private void handleSheetFehlt(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String key,
            SignaturErgebnis.SheetFehlt fehlt) {
        if (!fehlt.erwartet()) {
            logger.debug("Rangliste-Quelle '{}' fehlt (optional) – skip", fehlt.stabileId());
            return;
        }
        if (RanglisteSignaturStore.recoveryBereitsVersucht(xDoc, key)) {
            logger.debug("Rangliste-Quelle '{}' fehlt erwartet, Recovery bereits versucht – skip",
                    fehlt.stabileId());
            return;
        }
        if (SheetRunner.isRunning()) {
            return;
        }
        logger.warn("Rangliste-Quelle '{}' fehlt erwartet (key={}) – Recovery-Rebuild",
                fehlt.stabileId(), key);
        RanglisteSignaturStore.markiereRecoveryVersucht(xDoc, key);
        runnerFactory.apply(new WorkingSpreadsheet(xContext, xDoc), sheet).startSilent();
    }

    private void handleTransient(XSpreadsheetDocument xDoc, XSpreadsheet sheet,
            RanglisteEingabeSignatur signatur, String key, String grund,
            SignaturErgebnis.TransientFehler te) {
        if (te.versuch() < RanglisteRefreshDebouncer.MAX_RETRY) {
            int naechster = te.versuch() + 1;
            logger.debug("Rangliste-Signatur transient fehlgeschlagen (key={}, versuch={}): {} – Retry",
                    key, te.versuch(), te.grund());
            RanglisteRefreshDebouncer.get().scheduleRetry(xDoc, key, naechster,
                    () -> pruefeUndStarte(xDoc, sheet, signatur, key, grund, naechster));
        } else {
            logger.warn("Rangliste-Signatur transient fehlgeschlagen (key={}, versuch={}): {} – "
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
}
