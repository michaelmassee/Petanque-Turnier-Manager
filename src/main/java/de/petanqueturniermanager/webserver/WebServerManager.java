package de.petanqueturniermanager.webserver;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.star.lang.DisposedException;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFramesSupplier;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;
import com.sun.star.util.XModifyBroadcaster;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.CompositeViewEintragRoh;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.NativeDialogSperre;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.timer.TimerListener;
import de.petanqueturniermanager.timer.TimerState;
import de.petanqueturniermanager.timer.TimerWebServerInstanz;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;

/**
 * Singleton-Verwaltung aller Webserver-Instanzen.
 * <p>
 * Startet und stoppt HTTP-Server-Instanzen gemäß den globalen Plugin-Einstellungen.
 * <p>
 * Das Mapping (UNO → Model) und der SSE-Push erfolgen <strong>immer</strong> im SheetRunner-Thread
 * (via {@link #sseRefreshSenden}), um UNO-Thread-Safety zu gewährleisten.
 * HTTP-Handler-Threads greifen nur auf gecachte JSON-Strings zu.
 * <p>
 * SSE-Protokoll:
 * <ul>
 *   <li>{@code typ="init"} – vollständiger Zustand, beim ersten Render oder für neue Verbindungen</li>
 *   <li>{@code typ="diff"} – nur geänderte Zellen, nach jedem weiteren Render</li>
 *   <li>{@code typ="hinweis"} – Hinweismeldung, wenn das Sheet noch nicht verfügbar ist</li>
 * </ul>
 */
public final class WebServerManager implements TimerListener, IGlobalEventListener {

    private static final Logger logger = LogManager.getLogger(WebServerManager.class);
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SplitKnoten.class, new SplitKnotenAdapter())
            .create();
    private static final WebServerManager INSTANCE = new WebServerManager();

    /** Maximale Anzahl URL-Slots im Menü. */
    private static final int MAX_URL_SLOTS = 10;
    public static final String STARTSEITE_VIEW_ID = "startseite";

    /**
     * Alle laufenden Composite-View-Instanzen.
     * <p>
     * {@code CopyOnWriteArrayList}, weil {@link #sseRefreshSendenIntern} sie aus dem SheetRunner-Thread
     * unsynchronisiert iteriert (Zeile ~961), waehrend {@link #konfigurationGeaendertIntern} sie aus dem
     * {@code konfigExecutor}-Thread strukturell veraendert (add/remove/clear) – ohne threadsichere
     * Collection wuerde das zu einer {@link java.util.ConcurrentModificationException} fuehren, sobald
     * eine Composite-View-Konfigurationsaenderung zeitlich mit einer laufenden Live-Refresh-Welle
     * zusammenfaellt.
     */
    private final List<CompositeViewInstanz> compositeInstanzen = new CopyOnWriteArrayList<>();
    /** Die ersten MAX_URL_SLOTS Composite-Instanzen (nach Port sortiert) für Menü-URL-Anzeige. */
    private final List<WebServerSlot> slots = new ArrayList<>(MAX_URL_SLOTS);

    private final TabellenMapper mapper = new TabellenMapper();
    private final DiffEngine diffEngine = new DiffEngine();
    private final WebserverModifyListener modifyListener = new WebserverModifyListener();
    private final Map<String, WebserverModifyListener> modifyListenerNachDokument = new ConcurrentHashMap<>();
    private final Object konfigExecutorLock = new Object();
    private ExecutorService konfigExecutor = neuerKonfigExecutor();

    /** Letzte Panel-Modelle pro Quelle und Panel-ID (viewId → panelId → TabelleModel). */
    private final Map<String, Map<Integer, TabelleModel>> letzteCompositeModelle = new ConcurrentHashMap<>();
    /** Letzte Panel-Titel pro Quelle und Panel-ID (viewId → panelId → Titel). */
    private final Map<String, Map<Integer, String>> letzteCompositeTitel = new ConcurrentHashMap<>();
    /** Monoton steigende Version pro Quelle (viewId → AtomicInteger). */
    private final Map<String, AtomicInteger> compositeVersionen = new ConcurrentHashMap<>();
    /** Letzte iframe-Quelle pro Quelle und Panel-ID (viewId → panelId → URL oder lokaler Dateipfad). */
    private final Map<String, Map<Integer, String>> letzteCompositeUrls = new ConcurrentHashMap<>();
    /** Initialisierte TIMER-Panels pro Quelle (viewId → Set<panelId>). ConcurrentHashMap erlaubt keine null-Werte,
     * daher können TIMER-Panels nicht in letzteCompositeModelle als null-Marker gespeichert werden. */
    private final Map<String, Set<Integer>> initialisiertePanels = new ConcurrentHashMap<>();
    /** BLATT-Panels pro Quelle, die zuletzt als "fehlend" gerendert wurden (viewId → Set<panelId>).
     * Wird für Diff-Erkennung verwendet, damit der fehlend-Hinweis nicht in jedem Render erneut gesendet wird. */
    private final Map<String, Set<Integer>> fehlendePanels = new ConcurrentHashMap<>();
    private final Map<String, DokumentQuelle> dokumente = new ConcurrentHashMap<>();
    private final Map<String, DokumentRegieQuelle> dokumentRegieQuellen = new ConcurrentHashMap<>();

    /** Dokument, das den WebServer gestartet hat – nur dieses darf ihn wieder stoppen. */
    private XSpreadsheetDocument ownerDocument = null;
    private XSpreadsheetDocument registriertesDocument = null;
    /** Kontext, der beim {@link #starten(XComponentContext) Start} übergeben wurde –
     *  benötigt, um in {@link #konfigurationGeaendert()} ohne externes Sheet ein
     *  {@link WorkingSpreadsheet} für den Live-Push bauen zu können. */
    private volatile XComponentContext gespeicherterCtx = null;
    private volatile boolean laeuft = false;
    private TimerWebServerInstanz timerInstanz;

    /** Dedizierter Webserver für die Turnier-Startseite (separater Port, kein Composite).
     *  {@code volatile}, weil aus dem nicht-synchronisierten Push-Pfad
     *  {@link #pushStartseiteFallsAktiv(WorkingSpreadsheet)} gelesen wird. */
    private volatile TurnierStartseiteWebServerInstanz startseiteInstanz;
    private volatile WebserverRegieServerInstanz regieInstanz;
    /** Monoton steigende Version für Startseite-SSE-Nachrichten. */
    private final AtomicInteger startseiteVersion = new AtomicInteger(0);
    /** Zuletzt gepushter Teilnehmer-Status (Diff-Cache, vermeidet unnötige Pushes). */
    private volatile TeilnehmerStatusService.TeilnehmerStatus letzterStartseiteStatus;
    /** Zuletzt gepushte Turniersystem-Bezeichnung (Diff-Cache). */
    private volatile String letztesStartseiteTurniersystem = "";
    /** Zuletzt gepushter Turnier-Status-Text (Diff-Cache). */
    private volatile String letzterStartseiteTurnierStatus = "";
    private volatile String letztesStartseiteLogo = "";
    private volatile String letzteStartseiteBeschreibung = "";
    private volatile String letzteStartseiteAnimation = "";
    private volatile String letzteStartseiteTextfarbe = "";
    /** Zuletzt gepushte Checkin-Namenslisten (Diff-Cache + Basis für Neu-Erkennung der Animation). */
    private volatile List<String> letzteStartseiteNichtEingecheckt = List.of();
    private volatile List<String> letzteStartseiteEingecheckt = List.of();
    private volatile TimerState letzterTimerZustand = TimerState.inaktiv();

    private record StartseitePanelDaten(StartseiteSseNachricht nachricht, String logoQuelle) {
    }

    private record DokumentQuelle(
            String dokumentId,
            String anzeigename,
            XSpreadsheetDocument document,
            WorkingSpreadsheet ws,
            TurnierSystem turnierSystem,
            boolean master) {
    }

    private final List<Runnable> statusListener = new CopyOnWriteArrayList<>();

    /** Serialisiert alle Render-/Push-Wellen. Niemals zwei parallele {@code renderUndPushenComposite}-Läufe. */
    private final ReentrantLock refreshLock = new ReentrantLock();
    /** Zeitstempel des letzten erfolgreich abgeschlossenen Refresh-Laufs (für Watchdog-Heuristik). */
    private volatile long lastSuccessfulRefreshAt;
    /** Schwelle, ab der der Watchdog auch ohne dirty-Flag einen Recovery-Refresh anstößt (Self-Healing). */
    private static final long STILLE_SCHWELLE_MS = 10_000L;
    /** Schwelle, ab der eine hängende Dirty-Welle als Race-Hänger interpretiert wird. */
    private static final long DIRTY_HAENGT_SCHWELLE_MS = 2_000L;
    /** Watchdog-Tick-Intervall. */
    private static final long WATCHDOG_INTERVAL_MS = 5_000L;

    private ScheduledExecutorService watchdog;

    private WebServerManager() {
    }

    private static ExecutorService neuerKonfigExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            var t = new Thread(r, "PTM-WebServer-Konfiguration");
            t.setDaemon(true);
            return t;
        });
    }

    private void fuehreKonfigTaskAus(Runnable task) {
        synchronized (konfigExecutorLock) {
            if (konfigExecutor.isShutdown() || konfigExecutor.isTerminated()) {
                konfigExecutor = neuerKonfigExecutor();
            }
            konfigExecutor.execute(task);
        }
    }

    /** Zugriff auf den Listener für externe Mark-Trigger (z.B. SheetRunner-Ende). */
    public WebserverModifyListener getModifyListener() {
        return modifyListener;
    }

    public void addStatusListener(Runnable listener) {
        statusListener.add(listener);
    }

    public void removeStatusListener(Runnable listener) {
        statusListener.remove(listener);
    }

    private void statusListenerBenachrichtigen() {
        statusListener.forEach(Runnable::run);
    }

    public static WebServerManager get() {
        return INSTANCE;
    }

    private Optional<DokumentQuelle> registriereDokumentFallsTurnier(WorkingSpreadsheet ws) {
        if (ws == null || ws.getWorkingSpreadsheetDocument() == null) {
            return Optional.empty();
        }
        XSpreadsheetDocument doc = ws.getWorkingSpreadsheetDocument();
        try {
            var docProps = new DocumentPropertiesHelper(ws);
            TurnierSystem system = docProps.getTurnierSystemAusDocument();
            if (system == null || system == TurnierSystem.KEIN) {
                return Optional.empty();
            }
            String dokumentId = dokumentId(doc);
            boolean master = istOwnerDocument(doc);
            String anzeigename = dokumentAnzeigename(doc, system, master);
            var quelle = new DokumentQuelle(dokumentId, anzeigename, doc, ws, system, master);
            dokumente.put(dokumentId, quelle);
            registriereModifyListenerFallsNoetig(quelle);
            if (!master) {
                aktualisiereDokumentRegieQuellen(quelle);
            }
            return Optional.of(quelle);
        } catch (RuntimeException e) {
            if (istDokumentGeschlossen(e)) {
                return Optional.empty();
            }
            logger.debug("Turnierdokument konnte nicht registriert werden: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void registriereAlleOffenenTurnierdokumente(XComponentContext ctx) {
        try {
            var desktop = DocumentHelper.getCurrentDesktop(ctx);
            var framesSupplier = Lo.qi(XFramesSupplier.class, desktop);
            if (framesSupplier == null || framesSupplier.getFrames() == null) {
                return;
            }
            var frames = framesSupplier.getFrames();
            for (int i = 0; i < frames.getCount(); i++) {
                Object frameObj = frames.getByIndex(i);
                XFrame frame = Lo.qi(XFrame.class, frameObj);
                if (frame == null) {
                    continue;
                }
                XController controller = frame.getController();
                if (controller == null) {
                    continue;
                }
                var doc = Lo.qi(XSpreadsheetDocument.class, controller.getModel());
                if (doc != null) {
                    registriereDokumentFallsTurnier(new WorkingSpreadsheet(ctx, doc));
                }
            }
        } catch (Exception e) {
            logger.debug("Offene Turnierdokumente konnten nicht vollständig registriert werden: {}", e.getMessage());
        }
    }

    private void aktualisiereDokumentRegieQuellen(DokumentQuelle dokument) {
        for (var konfig : GlobalProperties.get().getCompositeViewKonfigurationen()) {
            aktualisiereDokumentRegieQuellen(dokument, konfig);
        }
        if (GlobalProperties.get().isStartseiteAktiv()) {
            aktualisiereDokumentStartseiteQuelle(dokument);
        }
    }

    private void aktualisiereDokumentRegieQuellen(DokumentQuelle dokument, CompositeViewKonfiguration konfig) {
        String viewId = dokumentViewId(dokument.dokumentId(), compositeViewId(konfig.port()));
        dokumentRegieQuellen.computeIfAbsent(viewId, id -> {
            var quelle = new DokumentRegieQuelle(
                    id,
                    anzeigeMitDokument(dokument.anzeigename(), anzeigeName(konfig)),
                    konfig.port(),
                    konfig);
            quelle.setCachedInitJson(GSON.toJson(CompositeSseNachricht.hinweis(
                    I18n.get("webserver.hinweis.kein.dokument.titel"),
                    I18n.get("webserver.hinweis.kein.dokument.text"))));
            return quelle;
        });
        cachesFuerQuelleInitialisieren(viewId);
    }

    private void aktualisiereDokumentStartseiteQuelle(DokumentQuelle dokument) {
        String viewId = dokumentViewId(dokument.dokumentId(), STARTSEITE_VIEW_ID);
        dokumentRegieQuellen.computeIfAbsent(viewId, id -> {
            var quelle = new DokumentRegieQuelle(
                    id,
                    anzeigeMitDokument(dokument.anzeigename(), I18n.get("startseite.anzeigename")),
                    GlobalProperties.get().getStartseitePort(),
                    null);
            quelle.setCachedInitJson(GSON.toJson(leereStartseiteNachricht(0)));
            return quelle;
        });
    }

    private void cachesFuerQuelleInitialisieren(String viewId) {
        compositeVersionen.computeIfAbsent(viewId, k -> new AtomicInteger(0));
        letzteCompositeModelle.computeIfAbsent(viewId, k -> new ConcurrentHashMap<>());
        letzteCompositeTitel.computeIfAbsent(viewId, k -> new ConcurrentHashMap<>());
        letzteCompositeUrls.computeIfAbsent(viewId, k -> new ConcurrentHashMap<>());
        initialisiertePanels.computeIfAbsent(viewId, k -> ConcurrentHashMap.newKeySet());
        fehlendePanels.computeIfAbsent(viewId, k -> ConcurrentHashMap.newKeySet());
    }

    private void entferneCachesFuerQuelle(String viewId) {
        letzteCompositeModelle.remove(viewId);
        letzteCompositeTitel.remove(viewId);
        compositeVersionen.remove(viewId);
        letzteCompositeUrls.remove(viewId);
        initialisiertePanels.remove(viewId);
        fehlendePanels.remove(viewId);
    }

    private static String anzeigeName(CompositeViewKonfiguration konfig) {
        return konfig.name() == null || konfig.name().isBlank()
                ? I18n.get("webserver.compositeview.anzeigename") + " " + konfig.port()
                : konfig.name().trim();
    }

    private static String anzeigeMitDokument(String dokument, String quelle) {
        return dokument + " / " + quelle;
    }

    private static String dokumentId(XSpreadsheetDocument doc) {
        var storable = Lo.qi(XStorable.class, doc);
        if (storable != null) {
            try {
                String location = storable.getLocation();
                if (location != null && !location.isBlank()) {
                    return "url:" + location;
                }
            } catch (RuntimeException e) {
                // Fallback auf UNO-OID.
            }
        }
        return "oid:" + UnoRuntime.generateOid(Lo.qi(XInterface.class, doc));
    }

    private static String dokumentAnzeigename(XSpreadsheetDocument doc, TurnierSystem system, boolean master) {
        String prefix = master ? I18n.get("webserver.regie.dokument.master") : system.getBezeichnung();
        var model = Lo.qi(XModel.class, doc);
        if (model == null || model.getURL() == null || model.getURL().isBlank()) {
            return prefix;
        }
        String url = model.getURL();
        int slash = Math.max(url.lastIndexOf('/'), url.lastIndexOf('\\'));
        String dateiname = slash >= 0 ? url.substring(slash + 1) : url;
        if (dateiname.isBlank()) {
            return prefix;
        }
        return prefix + " " + dateiname;
    }

    public static String compositeViewId(int port) {
        return "composite:" + port;
    }

    static String dokumentViewId(String dokumentId, String basisViewId) {
        return "doc:" + encodeViewIdTeil(dokumentId) + ":" + basisViewId;
    }

    private static String encodeViewIdTeil(String wert) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(wert.getBytes(StandardCharsets.UTF_8));
    }

    static Optional<String> dokumentIdAusViewId(String viewId) {
        if (viewId == null || !viewId.startsWith("doc:")) {
            return Optional.empty();
        }
        int trenner = viewId.indexOf(':', "doc:".length());
        if (trenner < 0) {
            return Optional.empty();
        }
        try {
            return Optional.of(new String(
                    Base64.getUrlDecoder().decode(viewId.substring("doc:".length(), trenner)),
                    StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Startet alle konfigurierten Webserver-Instanzen.
     * Führt nach dem Start direkt ein initiales Rendering durch.
     *
     * @param ctx LibreOffice-Kontext für das initiale Rendering
     */
    public synchronized void starten(XComponentContext ctx) {
        if (laeuft) {
            logger.debug("WebServerManager läuft bereits");
            return;
        }
        var compositeKonfigs = new ArrayList<>(GlobalProperties.get().getCompositeViewKonfigurationen());
        boolean startseiteAktiv = GlobalProperties.get().isStartseiteAktiv();
        boolean regieAktiv = GlobalProperties.get().isWebserverRegieAktiv();
        if (compositeKonfigs.isEmpty() && !startseiteAktiv && !regieAktiv) {
            logger.info("Keine Webserver-Ports konfiguriert, kein Start");
            return;
        }
        slots.clear();
        compositeInstanzen.clear();
        gespeicherterCtx = ctx;
        ownerDocument = new WorkingSpreadsheet(ctx).getWorkingSpreadsheetDocument();
        logger.info("Webserver Master-Dokument gesetzt: {}", ownerDocument != null ? "ja" : "null");
        safeProcessBoxInfo(I18n.get("webserver.prozessbox.starten"));

        for (var konfig : compositeKonfigs) {
            try {
                var instanz = new CompositeViewInstanz(konfig);
                instanz.starten();
                instanz.setCachedInitJson(GSON.toJson(CompositeSseNachricht.hinweis(
                        I18n.get("webserver.hinweis.kein.dokument.titel"),
                        I18n.get("webserver.hinweis.kein.dokument.text"))));
                compositeInstanzen.add(instanz);
                cachesFuerQuelleInitialisieren(instanz.getViewId());
                logger.info("Composite-View-Server gestartet auf Port {}", konfig.port());
                safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestartet.url", buildUrl(konfig.port())));
            } catch (IOException e) {
                logger.error("Fehler beim Starten des Composite-View-Servers auf Port {}: {}",
                        konfig.port(), e.getMessage(), e);
                safeProcessBoxFehler(I18n.get("webserver.prozessbox.fehler.port", konfig.port(), e.getMessage()));
            }
        }

        startseiteAusKonfigurationStarten();
        regieAusKonfigurationStarten();

        var alleSlots = new ArrayList<WebServerSlot>(compositeInstanzen.size() + 1);
        alleSlots.addAll(compositeInstanzen);
        if (startseiteInstanz != null) {
            alleSlots.add(startseiteInstanz);
        }
        alleSlots.stream()
                .filter(WebServerSlot::laeuft)
                .sorted(Comparator.comparingInt(WebServerSlot::getPort))
                .limit(MAX_URL_SLOTS)
                .forEach(slots::add);

        if (!compositeInstanzen.isEmpty() || startseiteInstanz != null || regieInstanz != null) {
            laeuft = true;
            statusListenerBenachrichtigen();
            try {
                var masterWs = new WorkingSpreadsheet(ctx);
                registriereDokumentFallsTurnier(masterWs);
                registriereAlleOffenenTurnierdokumente(ctx);
                sseRefreshSendenIntern(masterWs);
                for (var dokument : new ArrayList<>(dokumente.values())) {
                    if (!dokument.master()) {
                        sseRefreshSendenIntern(dokument.ws());
                    }
                }
            } catch (Exception e) {
                logger.debug("Initiales Rendering fehlgeschlagen: {}", e.getMessage());
                sendeHinweisAnAlle(
                        I18n.get("webserver.hinweis.kein.dokument.titel"),
                        I18n.get("webserver.hinweis.kein.dokument.text"));
            }
            starteWatchdog();
        }
    }

    /**
     * Startet einen leichten Periodic-Watchdog, der nur dann eine Refresh-Welle anstößt,
     * wenn entweder eine Dirty-Welle hängt (Race-Hänger) oder zu lange keine Aktualisierung
     * stattfand (Self-Healing bei komplett verschluckten Modify-Events). Macht selbst nie
     * ein Mapping – der Aufruf läuft über den normalen Listener-Pfad und respektiert damit
     * Debounce und Refresh-Lock.
     */
    private void starteWatchdog() {
        if (watchdog != null && !watchdog.isShutdown()) {
            return;
        }
        watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "WebserverWatchdog");
            t.setDaemon(true);
            return t;
        });
        watchdog.scheduleWithFixedDelay(this::watchdogTick,
                WATCHDOG_INTERVAL_MS, WATCHDOG_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void watchdogTick() {
        try {
            if (!laeuft || gespeicherterCtx == null) {
                return;
            }
            if (de.petanqueturniermanager.SheetRunner.isRunning()) {
                return;
            }
            if (compositeInstanzen.stream().noneMatch(CompositeViewInstanz::hatAktiveVerbindungen)) {
                // Kein Browser offen – nichts zu tun.
                return;
            }
            boolean dirtyHaengt = modifyListener.isDirty()
                    && modifyListener.dirtySinceMs() > DIRTY_HAENGT_SCHWELLE_MS;
            boolean stilleZuLang = lastSuccessfulRefreshAt > 0L
                    && System.currentTimeMillis() - lastSuccessfulRefreshAt > STILLE_SCHWELLE_MS;
            if (dirtyHaengt || stilleZuLang) {
                logger.debug("Watchdog-Trigger: dirtyHaengt={}, stilleZuLang={}", dirtyHaengt, stilleZuLang);
                modifyListener.markDirtyAndSchedule();
            }
        } catch (RuntimeException e) {
            logger.debug("Watchdog-Tick fehlgeschlagen: {}", e.getMessage());
        }
    }

    private void stoppeWatchdog() {
        if (watchdog != null) {
            watchdog.shutdownNow();
            watchdog = null;
        }
    }

    /**
     * Startet die dedizierte Turnier-Startseite, falls in {@link GlobalProperties} aktiviert.
     * Bei Port-Konflikt/Fehler wird das Feld {@code null} gelassen — die übrigen Webserver
     * laufen weiter.
     */
    private void startseiteAusKonfigurationStarten() {
        if (!GlobalProperties.get().isStartseiteAktiv()) {
            return;
        }
        int port = GlobalProperties.get().getStartseitePort();
        try {
            startseiteInstanz = new TurnierStartseiteWebServerInstanz(port);
            startseiteInstanz.starten();
            startseiteVersion.set(0);
            startseiteDiffCacheZuruecksetzen();
            // Init-Cache vorläufig leer befüllen; sseRefreshSendenIntern liefert sofort konkrete Werte nach.
            startseiteInstanz.setCachedInitJson(GSON.toJson(StartseiteSseNachricht.init(
                    startseiteVersion.incrementAndGet(), "", "", "keine", "", 0, 0,
                    I18n.get("startseite.label.angemeldet"),
                    I18n.get("startseite.label.aktiv"),
                    I18n.get("startseite.tagline"),
                    "", "", StartseiteSprueche.alle(),
                    GlobalProperties.get().getStartseiteZoom(),
                    false, List.of(), List.of())));
            logger.info("Turnier-Startseite-Server gestartet auf Port {}", port);
            safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestartet.url", buildUrl(port)));
        } catch (IOException e) {
            logger.error("Fehler beim Starten der Turnier-Startseite auf Port {}: {}", port, e.getMessage(), e);
            safeProcessBoxFehler(I18n.get("webserver.prozessbox.fehler.port", port, e.getMessage()));
            startseiteInstanz = null;
        }
    }

    /**
     * Startet den Timer-Webserver auf dem angegebenen Port (oder gibt die laufende Instanz zurück).
     * Wird von {@link de.petanqueturniermanager.timer.TimerManager} beim Timer-Start aufgerufen.
     *
     * @param port TCP-Port für den Timer-Server
     * @throws IOException wenn der Port nicht gebunden werden kann
     */
    public synchronized void timerServerBesorgen(int port) throws IOException {
        if (timerInstanz != null && timerInstanz.getPort() == port && timerInstanz.laeuft()) {
            return;
        }
        if (timerInstanz != null) {
            timerInstanz.stoppen();
            timerInstanz = null;
        }
        timerInstanz = new TimerWebServerInstanz(port);
        timerInstanz.starten();
        logger.info("Timer-Webserver gestartet auf Port {}", port);
    }

    /**
     * Empfängt Timer-Zustandsänderungen und leitet sie an den laufenden Timer-Webserver
     * sowie an alle Composite-Instanzen mit TIMER-Panels weiter.
     * Registriert einmalig in {@code PetanqueTurnierMngrSingleton.init()}.
     */
    @Override
    public void onChange(TimerState state) {
        TimerWebServerInstanz timerSnapshot;
        List<CompositeViewInstanz> compositeSnapshots;
        List<DokumentRegieQuelle> dokumentQuellenSnapshots;
        synchronized (this) {
            letzterTimerZustand = state;
            timerSnapshot = timerInstanz != null && timerInstanz.laeuft() ? timerInstanz : null;
            compositeSnapshots = new ArrayList<>(compositeInstanzen);
            dokumentQuellenSnapshots = new ArrayList<>(dokumentRegieQuellen.values());
        }
        if (timerSnapshot != null) {
            timerSnapshot.onChange(state);
        }
        for (var instanz : compositeSnapshots) {
            if (!instanz.hatTimerPanels()) {
                continue;
            }
            sendeTimerUpdateAnComposite(instanz, state);
        }
        for (var quelle : dokumentQuellenSnapshots) {
            if (!quelle.hatTimerPanels()) {
                continue;
            }
            sendeTimerUpdateAnRegieQuelle(quelle, state);
        }
    }

    /**
     * Pusht den aktuellen Timer-Zustand als diff-Nachricht an alle TIMER-Panels
     * der gegebenen Composite-Instanz.
     */
    private void sendeTimerUpdateAnComposite(CompositeViewInstanz instanz, TimerState state) {
        sendeTimerUpdate(instanz.getKonfiguration(), instanz.getViewId(), instanz::sseNachrichtPushen, state);
    }

    private void sendeTimerUpdateAnRegieQuelle(DokumentRegieQuelle quelle, TimerState state) {
        sendeTimerUpdate(quelle.getKonfiguration(), quelle.getViewId(), quelle::sseNachrichtPushen, state);
    }

    private void sendeTimerUpdate(CompositeViewKonfiguration konfig, String viewId,
            Consumer<String> ssePushen, TimerState state) {
        var panelNachrichten = new ArrayList<CompositePanelNachricht>();
        for (int i = 0; i < konfig.panels().size(); i++) {
            if (konfig.panels().get(i).typ() == PanelTyp.TIMER) {
                var panelKonfig = konfig.panels().get(i);
                panelNachrichten.add(CompositePanelNachricht.timer(i, panelKonfig.zoom(), panelKonfig.horizontalAusrichtung(), panelKonfig.vertikalAusrichtung(), state));
            }
        }
        if (panelNachrichten.isEmpty()) {
            return;
        }
        int version = compositeVersionen.computeIfAbsent(viewId, k -> new AtomicInteger(0)).incrementAndGet();
        ssePushen.accept(GSON.toJson(CompositeSseNachricht.diff(version, panelNachrichten, konfig.wurzel(), konfig.zoom(), konfig.mitHeaderFooter(), konfig.rand().toDaten())));
    }

    /**
     * Stoppt alle laufenden Webserver-Instanzen und schließt alle SSE-Verbindungen.
     */
    public void stoppen() {
        List<Runnable> stopAktionen = new ArrayList<>();
        synchronized (this) {
            if (timerInstanz != null) {
                var aktuelleTimerInstanz = timerInstanz;
                stopAktionen.add(aktuelleTimerInstanz::stoppen);
                timerInstanz = null;
            }
            synchronized (konfigExecutorLock) {
                konfigExecutor.shutdownNow();
            }
            // Nur der Timer-Webserver kann unabhängig vom Haupt-Webserver laufen.
            // Gesammelte Stop-Aktionen müssen nach Freigabe des Manager-Locks trotzdem laufen.
            if (laeuft) {
                stoppeWatchdog();
                deregistriereModifyListener();
                for (var instanz : compositeInstanzen) {
                    stopAktionen.add(instanz::stoppen);
                }
                if (startseiteInstanz != null) {
                    var aktuelleStartseiteInstanz = startseiteInstanz;
                    stopAktionen.add(aktuelleStartseiteInstanz::stoppen);
                    startseiteInstanz = null;
                }
                if (regieInstanz != null) {
                    var aktuelleRegieInstanz = regieInstanz;
                    stopAktionen.add(aktuelleRegieInstanz::stoppen);
                    regieInstanz = null;
                }
                for (var quelle : dokumentRegieQuellen.values()) {
                    stopAktionen.add(quelle::stoppen);
                }
                startseiteDiffCacheZuruecksetzen();
                slots.clear();
                compositeInstanzen.clear();
                dokumentRegieQuellen.clear();
                letzteCompositeModelle.clear();
                letzteCompositeTitel.clear();
                compositeVersionen.clear();
                letzteCompositeUrls.clear();
                initialisiertePanels.clear();
                fehlendePanels.clear();
                laeuft = false;
                ownerDocument = null;
                entferneAlleModifyListener();
                dokumente.clear();
                statusListenerBenachrichtigen();
                logger.info("Webserver gestoppt");
                safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestoppt"));
            }
        }
        stopAktionen.forEach(Runnable::run);
    }

    /**
     * Wird vom SheetRunner-Thread nach erfolgreichem {@code doRun()} aufgerufen.
     * Mappt das Sheet, berechnet Diffs und pusht SSE-Nachrichten.
     * <p>
     * Aufrufe für fremde Dokumente (nicht das Owner-Dokument) werden ignoriert –
     * der WebServer zeigt ausschließlich Sheets des Dokuments, das ihn gestartet hat.
     * <p>
     * <strong>Muss im SheetRunner-Thread aufgerufen werden</strong> – greift auf UNO zu.
     *
     * @param ws aktuelles Arbeits-Spreadsheet
     */
    public void sseRefreshSenden(WorkingSpreadsheet ws) {
        if (!laeuft) {
            return;
        }
        var dokument = registriereDokumentFallsTurnier(ws);
        if (dokument.isEmpty()) {
            logger.debug("sseRefreshSenden ignoriert – ws ist kein registrierbares Turnierdokument");
            return;
        }
        sseRefreshSendenIntern(ws);
    }

    /**
     * Aktualisiert Zoom und Zentrieren aller laufenden Instanzen aus den gespeicherten GlobalProperties
     * und pusht den neuen Zustand sofort an alle verbundenen Browser-Clients.
     * <p>
     * Muss nach {@link GlobalProperties#speichernCompositeViews(boolean, java.util.List)} aufgerufen werden.
     * Der Server-Abgleich läuft auf einem Webserver-Worker; ein ggf. nötiger Startseiten-Live-Push
     * mit UNO-Zugriff wird anschließend auf den LO-Main-Thread gepostet.
     */
    public void konfigurationGeaendert() {
        fuehreKonfigTaskAus(() -> {
            try {
                var ergebnis = konfigurationGeaendertIntern();
                ergebnis.sseIoAktionen().forEach(Runnable::run);
                if (ergebnis.startseitePushNoetig()) {
                    pushStartseiteNachKonfigurationsaenderung();
                }
            } catch (RuntimeException e) {
                logger.warn("Webserver-Konfigurationsabgleich fehlgeschlagen: {}", e.getMessage(), e);
            }
        });
    }

    private synchronized KonfigAbgleichErgebnis konfigurationGeaendertIntern() {
        var sseIoAktionen = new ArrayList<Runnable>();
        if (!laeuft) {
            return new KonfigAbgleichErgebnis(List.of(), false);
        }
        var compositeEintraege = GlobalProperties.get().getCompositeViewEintraege();
        for (var instanz : compositeInstanzen) {
            int port = instanz.getKonfiguration().port();
            String viewId = instanz.getViewId();
            compositeEintraege.stream()
                    .filter(e -> e.port() == port && e.aktiv())
                    .findFirst()
                    .ifPresent(e -> {
                        var panelModelle = letzteCompositeModelle.get(viewId);
                        var panelTitel = letzteCompositeTitel.get(viewId);
                        if (panelModelle == null) {
                            return;
                        }
                        var konfig = instanz.getKonfiguration();
                        int neueAnzahl = e.panels().size();
                        int alteAnzahl = konfig.panels().size();

                        // Neue Wurzel aus layoutJson parsen
                        SplitKnoten neueWurzel = konfig.wurzel();
                        try {
                            var parsedWurzel = GSON.fromJson(e.layoutJson(), SplitKnoten.class);
                            if (parsedWurzel != null) {
                                neueWurzel = parsedWurzel;
                            }
                        } catch (Exception ex) {
                            logger.warn("Ungültiger Layout-JSON bei konfigurationGeaendert für Port {}", port, ex);
                        }

                        // Alte sheetConfig → alten Index mappen, um Resolver und Modelle
                        // korrekt auf neue Positionen zu übertragen (auch nach Löschung/Umnummerierung).
                        var altSheetConfigZuIndex = new HashMap<String, Integer>();
                        for (int i = 0; i < alteAnzahl; i++) {
                            altSheetConfigZuIndex.put(konfig.panels().get(i).sheetConfig(), i);
                        }

                        // Neue Panel-Einstellungen mit sheetConfig-basiertem Matching aufbauen.
                        // Für bekannte Panels (gleiche sheetConfig): alten Resolver + Modell behalten.
                        // Für neue Panels: Resolver aus sheetConfig erstellen, kein Modell-Cache.
                        var alleInitPanels = new ArrayList<CompositePanelNachricht>();
                        var neuePanelKonfigs = new ArrayList<PanelKonfiguration>();
                        var neuePanelModelle = new ConcurrentHashMap<Integer, TabelleModel>();
                        var neuePanelTitel = new ConcurrentHashMap<Integer, String>();
                        var neueUrlCache = letzteCompositeUrls.computeIfAbsent(viewId, p -> new ConcurrentHashMap<>());
                        for (int i = 0; i < neueAnzahl; i++) {
                            var neuerPanelEintrag = e.panels().get(i);
                            if (neuerPanelEintrag.typ() == PanelTyp.TIMER) {
                                neuePanelKonfigs.add(new PanelKonfiguration(
                                        PanelTyp.TIMER, "", null,
                                        neuerPanelEintrag.zoom(), neuerPanelEintrag.sichtbarerTabellenAnteil(),
                                        neuerPanelEintrag.horizontalAusrichtung(), neuerPanelEintrag.vertikalAusrichtung(), false, ""));
                                alleInitPanels.add(CompositePanelNachricht.timer(i, neuerPanelEintrag.zoom(), neuerPanelEintrag.horizontalAusrichtung(), neuerPanelEintrag.vertikalAusrichtung(), letzterTimerZustand));
                                continue;
                            }
                            if (neuerPanelEintrag.typ() == PanelTyp.URL) {
                                neuePanelKonfigs.add(new PanelKonfiguration(
                                        PanelTyp.URL, "", null,
                                        neuerPanelEintrag.zoom(), neuerPanelEintrag.sichtbarerTabellenAnteil(),
                                        neuerPanelEintrag.horizontalAusrichtung(), neuerPanelEintrag.vertikalAusrichtung(), neuerPanelEintrag.blattnameAnzeigen(),
                                        neuerPanelEintrag.externeUrl()));
                                neueUrlCache.put(i, neuerPanelEintrag.externeUrl());
                                alleInitPanels.add(CompositePanelNachricht.url(i, neuerPanelEintrag.zoom(), neuerPanelEintrag.externeUrl()));
                                continue;
                            }
                            if (neuerPanelEintrag.typ() == PanelTyp.STATISCHE_DATEI) {
                                neuePanelKonfigs.add(new PanelKonfiguration(
                                        PanelTyp.STATISCHE_DATEI, "", null,
                                        neuerPanelEintrag.zoom(), neuerPanelEintrag.sichtbarerTabellenAnteil(),
                                        neuerPanelEintrag.horizontalAusrichtung(), neuerPanelEintrag.vertikalAusrichtung(), false,
                                        neuerPanelEintrag.externeUrl()));
                                neueUrlCache.put(i, neuerPanelEintrag.externeUrl());
                                alleInitPanels.add(CompositePanelNachricht.statischeDatei(i, neuerPanelEintrag.zoom(), neuerPanelEintrag.externeUrl()));
                                continue;
                            }
                            if (neuerPanelEintrag.typ() == PanelTyp.TURNIERSTARTSEITE) {
                                neuePanelKonfigs.add(new PanelKonfiguration(
                                        PanelTyp.TURNIERSTARTSEITE, "", null,
                                        neuerPanelEintrag.zoom(), neuerPanelEintrag.sichtbarerTabellenAnteil(),
                                        neuerPanelEintrag.horizontalAusrichtung(), neuerPanelEintrag.vertikalAusrichtung(), false, ""));
                                alleInitPanels.add(CompositePanelNachricht.startseite(i, leereStartseiteNachricht(0)));
                                continue;
                            }
                            var altIndex = altSheetConfigZuIndex.get(neuerPanelEintrag.sheetConfig());
                            SheetResolver resolver = altIndex != null
                                    ? konfig.panels().get(altIndex).resolver()
                                    : SheetResolverFactory.erstellen(neuerPanelEintrag.sheetConfig());
                            neuePanelKonfigs.add(new PanelKonfiguration(
                                    PanelTyp.BLATT, neuerPanelEintrag.sheetConfig(), resolver,
                                    neuerPanelEintrag.zoom(), neuerPanelEintrag.sichtbarerTabellenAnteil(),
                                    neuerPanelEintrag.horizontalAusrichtung(), neuerPanelEintrag.vertikalAusrichtung(), neuerPanelEintrag.blattnameAnzeigen(), ""));
                            var vollModell = altIndex != null ? panelModelle.get(altIndex) : null;
                            if (vollModell != null) {
                                neuePanelModelle.put(i, vollModell);
                                // altIndex != null ist hier garantiert (vollModell != null impliziert altIndex != null)
                                String titel = panelTitel != null ? panelTitel.getOrDefault(altIndex, "") : "";
                                neuePanelTitel.put(i, titel);
                                alleInitPanels.add(CompositePanelNachricht.init(
                                        i, vollModell, titel,
                                        neuerPanelEintrag.zoom(), neuerPanelEintrag.sichtbarerTabellenAnteil(),
                                        neuerPanelEintrag.horizontalAusrichtung(), neuerPanelEintrag.vertikalAusrichtung(), neuerPanelEintrag.blattnameAnzeigen()));
                            }
                        }

                        // Cache durch korrekt re-indizierte Maps ersetzen
                        panelModelle.clear();
                        panelModelle.putAll(neuePanelModelle);
                        initialisiertePanels.computeIfPresent(viewId, (p, s) -> { s.clear(); return s; });
                        if (panelTitel != null) {
                            panelTitel.clear();
                            panelTitel.putAll(neuePanelTitel);
                        }

                        String cachedJson = null;
                        String pushJson = null;
                        if (!alleInitPanels.isEmpty()) {
                            int version = compositeVersionen
                                    .computeIfAbsent(viewId, p -> new AtomicInteger(0))
                                    .incrementAndGet();
                            cachedJson = GSON.toJson(CompositeSseNachricht.init(
                                    version, alleInitPanels, neueWurzel, e.zoom(), e.mitHeaderFooter(), e.rand().toDaten()));
                            // Sind nicht alle Panels im Cache (geänderte sheetConfig), würde ein
                            // composite_init die fehlenden Panels im Browser löschen → diff pushen,
                            // damit bestehende Panel-Zustände erhalten bleiben.
                            pushJson = alleInitPanels.size() < neueAnzahl
                                    ? GSON.toJson(CompositeSseNachricht.diff(version, alleInitPanels, neueWurzel, e.zoom(), e.mitHeaderFooter(), e.rand().toDaten()))
                                    : cachedJson;
                        }
                        String ausstehenderPush = instanz.aktualisiereKonfiguration(
                                new CompositeViewKonfiguration(port, e.name(), e.zoom(), neueWurzel, neuePanelKonfigs, e.mitHeaderFooter(), e.rand()),
                                cachedJson, pushJson);
                        if (ausstehenderPush != null) {
                            sseIoAktionen.add(() -> instanz.sseNachrichtPushen(ausstehenderPush));
                        }
                    });
        }

        reconciliereCompositeInstanzen(compositeEintraege, sseIoAktionen);
        reconciliereStartseiteInstanz(sseIoAktionen);
        reconciliereDokumentRegieQuellen(sseIoAktionen);
        reconciliereRegieInstanz(sseIoAktionen);
        if (regieInstanz != null && regieInstanz.laeuft()) {
            var aktuelleRegieInstanz = regieInstanz;
            sseIoAktionen.add(aktuelleRegieInstanz::konfigurationGeaendert);
        }
        // Logo/Beschreibung sind nicht Teil des Diff-Cache (letzterStartseiteStatus enthält nur
        // angemeldet/aktiv). Deshalb Diff-Cache zurücksetzen und sofort pushen, damit Änderungen
        // ohne Spielerzahl-Wechsel die Live-Clients erreichen.
        startseiteDiffCacheZuruecksetzen();
        boolean startseitePushNoetig = startseiteInstanz != null && startseiteInstanz.laeuft() && gespeicherterCtx != null;
        return new KonfigAbgleichErgebnis(List.copyOf(sseIoAktionen), startseitePushNoetig);
    }

    private void reconciliereDokumentRegieQuellen(List<Runnable> sseIoAktionen) {
        var sollIds = new HashSet<String>();
        var refreshDokumente = new HashSet<String>();
        for (var dokument : dokumente.values()) {
            if (dokument.master()) {
                continue;
            }
            refreshDokumente.add(dokument.dokumentId());
            for (var konfig : GlobalProperties.get().getCompositeViewKonfigurationen()) {
                String viewId = dokumentViewId(dokument.dokumentId(), compositeViewId(konfig.port()));
                sollIds.add(viewId);
                var vorhanden = dokumentRegieQuellen.get(viewId);
                if (vorhanden == null || !konfig.equals(vorhanden.getKonfiguration())) {
                    if (vorhanden != null) {
                        dokumentRegieQuellen.remove(viewId);
                        entferneCachesFuerQuelle(viewId);
                        sseIoAktionen.add(vorhanden::stoppen);
                    }
                    aktualisiereDokumentRegieQuellen(dokument, konfig);
                    refreshDokumente.add(dokument.dokumentId());
                }
            }
            if (GlobalProperties.get().isStartseiteAktiv()) {
                String viewId = dokumentViewId(dokument.dokumentId(), STARTSEITE_VIEW_ID);
                sollIds.add(viewId);
                if (!dokumentRegieQuellen.containsKey(viewId)) {
                    aktualisiereDokumentStartseiteQuelle(dokument);
                    refreshDokumente.add(dokument.dokumentId());
                }
            }
        }
        for (var eintrag : new ArrayList<>(dokumentRegieQuellen.entrySet())) {
            if (!sollIds.contains(eintrag.getKey())) {
                dokumentRegieQuellen.remove(eintrag.getKey());
                entferneCachesFuerQuelle(eintrag.getKey());
                sseIoAktionen.add(eintrag.getValue()::stoppen);
            }
        }
        refreshDokumente.stream()
                .map(dokumente::get)
                .filter(java.util.Objects::nonNull)
                .forEach(this::initialenDokumentRefreshPlanen);
    }

    private record KonfigAbgleichErgebnis(List<Runnable> sseIoAktionen, boolean startseitePushNoetig) {
    }

    private void pushStartseiteNachKonfigurationsaenderung() {
        XComponentContext ctx = gespeicherterCtx;
        if (ctx == null) {
            return;
        }
        LoMainThread.post(ctx, () -> {
            if (startseiteInstanz == null || !startseiteInstanz.laeuft()) {
                return;
            }
            try {
                pushStartseiteFallsAktiv(new WorkingSpreadsheet(ctx));
            } catch (RuntimeException e) {
                logger.warn("Live-Push der Startseite nach Konfig-Änderung fehlgeschlagen: {}", e.getMessage(), e);
            }
        });
    }

    private static StartseiteSseNachricht leereStartseiteNachricht(int version) {
        return StartseiteSseNachricht.init(
                version, "", "", "keine", "", 0, 0,
                I18n.get("startseite.label.angemeldet"),
                I18n.get("startseite.label.aktiv"),
                I18n.get("startseite.tagline"),
                "", "", StartseiteSprueche.alle(),
                GlobalProperties.get().getStartseiteZoom(),
                false, List.of(), List.of());
    }

    private StartseitePanelDaten startseitePanelDaten(WorkingSpreadsheet ws, int version) {
        var status = TeilnehmerStatusService.ermitteln(ws);
        boolean checkinListenAnzeigen = GlobalProperties.get().isStartseiteCheckinListenAnzeigen();
        var namenListen = checkinListenAnzeigen
                ? TeilnehmerStatusService.ermittelnNamen(ws)
                : new TeilnehmerStatusService.TeilnehmerNamenListen(List.of(), List.of());
        String turniersystem = TurnierStatusErmittler.turniersystemBezeichnung(ws);
        String turnierStatus = TurnierStatusErmittler.ermitteln(ws);
        var docProps = new DocumentPropertiesHelper(ws);
        String logoQuelle = turnierlogoQuelle(ws, docProps);
        String beschreibung = docProps.getStringProperty("Turnierbeschreibung", "");
        String beschreibungAnimation = docProps.getStringProperty(
                de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog
                        .DOC_PROP_BESCHREIBUNG_ANIMATION,
                de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog.ANIMATION_DEFAULT);
        int textfarbeInt = docProps.getIntProperty(
                de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog
                        .DOC_PROP_BESCHREIBUNG_TEXTFARBE,
                de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog
                        .DEFAULT_BESCHREIBUNG_TEXTFARBE);
        String textfarbe = String.format("#%06x", textfarbeInt & 0xFFFFFF);
        String logoUrl = startseiteLogoUrl(logoQuelle, version);
        var nachricht = StartseiteSseNachricht.init(
                version, logoUrl, beschreibung, beschreibungAnimation, textfarbe,
                status.angemeldet(), status.aktiv(),
                I18n.get("startseite.label.angemeldet"),
                I18n.get("startseite.label.aktiv"),
                I18n.get("startseite.tagline"),
                turniersystem, turnierStatus, StartseiteSprueche.alle(),
                GlobalProperties.get().getStartseiteZoom(),
                checkinListenAnzeigen, namenListen.angemeldetNichtEingecheckt(), namenListen.eingecheckt());
        return new StartseitePanelDaten(nachricht, logoQuelle);
    }

    /**
     * Gleicht die laufende Startseite-Instanz mit den aktuellen GlobalProperties ab:
     * - aktiviert: nicht laufend → neu starten
     * - aktiviert: laufend auf anderem Port → stoppen + neu starten
     * - deaktiviert: laufend → stoppen
     */
    private void reconciliereStartseiteInstanz(List<Runnable> sseIoAktionen) {
        boolean sollAktiv = GlobalProperties.get().isStartseiteAktiv();
        int sollPort = GlobalProperties.get().getStartseitePort();
        boolean laeuftSchon = startseiteInstanz != null && startseiteInstanz.laeuft();
        boolean portStimmt = laeuftSchon && startseiteInstanz.getPort() == sollPort;

        if (!sollAktiv) {
            if (laeuftSchon) {
                var aktuelleStartseiteInstanz = startseiteInstanz;
                sseIoAktionen.add(aktuelleStartseiteInstanz::stoppen);
                startseiteInstanz = null;
                startseiteDiffCacheZuruecksetzen();
                logger.info("Turnier-Startseite gestoppt (deaktiviert)");
            }
        } else if (!portStimmt) {
            if (laeuftSchon) {
                var aktuelleStartseiteInstanz = startseiteInstanz;
                sseIoAktionen.add(aktuelleStartseiteInstanz::stoppen);
                startseiteInstanz = null;
                startseiteDiffCacheZuruecksetzen();
            }
            startseiteAusKonfigurationStarten();
        }

        // Slots ggf. neu aufbauen, falls sich die Startseite-Existenz geändert hat.
        slots.clear();
        var alle = new ArrayList<WebServerSlot>(compositeInstanzen.size() + 1);
        alle.addAll(compositeInstanzen);
        if (startseiteInstanz != null) {
            alle.add(startseiteInstanz);
        }
        alle.stream()
                .filter(WebServerSlot::laeuft)
                .sorted(Comparator.comparingInt(WebServerSlot::getPort))
                .limit(MAX_URL_SLOTS)
                .forEach(slots::add);
    }

    private void regieAusKonfigurationStarten() {
        if (!GlobalProperties.get().isWebserverRegieAktiv()) {
            return;
        }
        int port = GlobalProperties.get().getWebserverRegiePort();
        try {
            regieInstanz = new WebserverRegieServerInstanz(port, this);
            regieInstanz.starten();
            logger.info("Webserver-Regie gestartet auf Port {}", port);
            safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestartet.url", buildUrl(port)));
        } catch (IOException e) {
            logger.error("Fehler beim Starten der Webserver-Regie auf Port {}: {}", port, e.getMessage(), e);
            safeProcessBoxFehler(I18n.get("webserver.prozessbox.fehler.port", port, e.getMessage()));
            regieInstanz = null;
        }
    }

    private void reconciliereRegieInstanz(List<Runnable> sseIoAktionen) {
        boolean sollAktiv = GlobalProperties.get().isWebserverRegieAktiv();
        int sollPort = GlobalProperties.get().getWebserverRegiePort();
        boolean laeuftSchon = regieInstanz != null && regieInstanz.laeuft();
        boolean portStimmt = laeuftSchon && regieInstanz.getPort() == sollPort;

        if (!sollAktiv) {
            if (laeuftSchon) {
                var aktuelleRegieInstanz = regieInstanz;
                sseIoAktionen.add(aktuelleRegieInstanz::stoppen);
                regieInstanz = null;
                logger.info("Webserver-Regie gestoppt (deaktiviert)");
            }
        } else if (!portStimmt) {
            if (laeuftSchon) {
                var aktuelleRegieInstanz = regieInstanz;
                sseIoAktionen.add(aktuelleRegieInstanz::stoppen);
                regieInstanz = null;
            }
            regieAusKonfigurationStarten();
        }
    }

    /**
     * Gleicht die laufenden Composite-View-Instanzen mit den aktuellen GlobalProperties ab:
     * stoppt Instanzen für entfernte/deaktivierte Einträge und startet neue für hinzugekommene/
     * reaktivierte Einträge. Aktualisiert danach die Slots-Liste.
     */
    private void reconciliereCompositeInstanzen(List<CompositeViewEintragRoh> compositeEintraege,
            List<Runnable> sseIoAktionen) {
        Set<Integer> aktivePorte = new HashSet<>();
        for (var e : compositeEintraege) {
            if (e.aktiv()) {
                aktivePorte.add(e.port());
            }
        }
        Set<Integer> laufendePorte = new HashSet<>();
        for (var instanz : compositeInstanzen) {
            laufendePorte.add(instanz.getKonfiguration().port());
        }

        // Instanzen stoppen, deren Port nicht mehr aktiv konfiguriert ist
        var zuStoppen = new ArrayList<CompositeViewInstanz>();
        for (var instanz : compositeInstanzen) {
            if (!aktivePorte.contains(instanz.getKonfiguration().port())) {
                zuStoppen.add(instanz);
            }
        }
        for (var instanz : zuStoppen) {
            int port = instanz.getKonfiguration().port();
            String viewId = instanz.getViewId();
            sseIoAktionen.add(instanz::stoppen);
            compositeInstanzen.remove(instanz);
            entferneCachesFuerQuelle(viewId);
            logger.info("Composite-View-Server auf Port {} gestoppt (konfigurationGeaendert)", port);
            safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestoppt.port", port));
        }

        // Neue Instanzen starten für aktive Einträge ohne laufende Instanz
        for (var konfig : GlobalProperties.get().getCompositeViewKonfigurationen()) {
            if (!laufendePorte.contains(konfig.port())) {
                try {
                    var instanz = new CompositeViewInstanz(konfig);
                    instanz.starten();
                    var versionZaehler = new AtomicInteger(0);
                    compositeVersionen.put(instanz.getViewId(), versionZaehler);
                    letzteCompositeModelle.put(instanz.getViewId(), new ConcurrentHashMap<>());
                    letzteCompositeTitel.put(instanz.getViewId(), new ConcurrentHashMap<>());
                    letzteCompositeUrls.put(instanz.getViewId(), new ConcurrentHashMap<>());
                    initialisiertePanels.put(instanz.getViewId(), ConcurrentHashMap.newKeySet());
                    fehlendePanels.put(instanz.getViewId(), ConcurrentHashMap.newKeySet());
                    initialisiereNeueCompositeInstanz(instanz, konfig, versionZaehler);
                    compositeInstanzen.add(instanz);
                    logger.info("Composite-View-Server auf Port {} gestartet (konfigurationGeaendert)", konfig.port());
                    safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestartet.url", buildUrl(konfig.port())));
                } catch (IOException e) {
                    logger.error("Fehler beim Starten des Composite-View-Servers auf Port {}: {}", konfig.port(), e.getMessage(), e);
                    safeProcessBoxFehler(I18n.get("webserver.prozessbox.fehler.port", konfig.port(), e.getMessage()));
                }
            }
        }

        // Slots nach Änderungen neu aufbauen
        if (!zuStoppen.isEmpty() || aktivePorte.stream().anyMatch(p -> !laufendePorte.contains(p))) {
            slots.clear();
            compositeInstanzen.stream()
                    .filter(WebServerSlot::laeuft)
                    .sorted(Comparator.comparingInt(WebServerSlot::getPort))
                    .limit(MAX_URL_SLOTS)
                    .forEach(slots::add);
        }
    }

    /**
     * Setzt den initialen SSE-Cache für eine neu gestartete Composite-Instanz.
     * <p>
     * Panels ohne Sheet-Zugriff (TIMER, URL, STATISCHE_DATEI) können sofort initialisiert werden.
     * BLATT-Panels ohne gecachtes Modell werden erst beim nächsten Rendering befüllt;
     * bis dahin bleibt der Hinweis-Cache für diese Instanz erhalten.
     */
    private void initialisiereNeueCompositeInstanz(
            CompositeViewInstanz instanz,
            CompositeViewKonfiguration konfig,
            AtomicInteger versionZaehler) {
        var alleInitPanels = new ArrayList<CompositePanelNachricht>();
        var viewId = instanz.getViewId();
        var fehlendCache = fehlendePanels.computeIfAbsent(viewId, p -> ConcurrentHashMap.newKeySet());
        for (int i = 0; i < konfig.panels().size(); i++) {
            var panelKonfig = konfig.panels().get(i);
            if (panelKonfig.typ() == PanelTyp.TIMER) {
                alleInitPanels.add(CompositePanelNachricht.timer(
                        i, panelKonfig.zoom(), panelKonfig.horizontalAusrichtung(), panelKonfig.vertikalAusrichtung(), letzterTimerZustand));
            } else if (panelKonfig.typ() == PanelTyp.URL) {
                alleInitPanels.add(CompositePanelNachricht.url(i, panelKonfig.zoom(), panelKonfig.externeUrl()));
                letzteCompositeUrls.get(viewId).put(i, panelKonfig.externeUrl());
            } else if (panelKonfig.typ() == PanelTyp.STATISCHE_DATEI) {
                alleInitPanels.add(CompositePanelNachricht.statischeDatei(i, panelKonfig.zoom(), panelKonfig.externeUrl()));
                letzteCompositeUrls.get(viewId).put(i, panelKonfig.externeUrl());
            } else if (panelKonfig.typ() == PanelTyp.TURNIERSTARTSEITE) {
                alleInitPanels.add(CompositePanelNachricht.startseite(i, leereStartseiteNachricht(0)));
            } else {
                // BLATT-Panel ohne ws-Zugriff: vorläufig als "kein Dokument" rendern.
                // Beim ersten echten Refresh wird der Status auf "echtes Sheet" oder
                // "Sheet nicht gefunden" korrigiert.
                fehlendCache.add(i);
                alleInitPanels.add(CompositePanelNachricht.fehlend(i,
                        I18n.get("webserver.hinweis.kein.dokument.titel"),
                        I18n.get("webserver.hinweis.kein.dokument.text")));
            }
        }
        int version = versionZaehler.incrementAndGet();
        instanz.setCachedInitJson(GSON.toJson(
                CompositeSseNachricht.init(version, alleInitPanels, konfig.wurzel(), konfig.zoom(), konfig.mitHeaderFooter(), konfig.rand().toDaten())));
    }

    private void sseRefreshSendenIntern(WorkingSpreadsheet ws) {
        // Ein natives modales Dialogfenster mit eigener Event-Loop (z.B. FarbwahlDialog) ist offen –
        // gleichzeitiger UNO-Zugriff aus diesem Hintergrund-Thread kollidiert damit (SIGABRT durch
        // Stack-Corruption in ColorDialog::Execute(), per CoreDump-Analyse verifiziert). Welle
        // überspringen, sie wird über markDirty() nachgeholt, sobald der Dialog geschlossen ist.
        if (NativeDialogSperre.istOffen()) {
            modifyListener.markDirty();
            logger.debug("Refresh übersprungen – natives Dialogfenster offen");
            return;
        }
        // Serialisiert: niemals zwei parallele Render-/Push-Wellen. Konkurrierende Aufrufer
        // signalisieren stattdessen "nochmal" über den Listener-Dirty-Flag.
        if (!refreshLock.tryLock()) {
            modifyListener.markDirty();
            logger.debug("Refresh läuft bereits – markDirty()");
            return;
        }
        try {
            var dokument = registriereDokumentFallsTurnier(ws).orElse(null);
            if (dokument == null) {
                return;
            }
            // Genau ein calculate() pro Refresh-Welle, vor der Composite-Schleife –
            // spart bei N Compositen N-1 Recalc-Durchläufe und stellt sicher, dass
            // abhängige Formelzellen (z.B. Rangliste) den aktuellen Stand spiegeln.
            forceRecalc(ws);
            if (dokument.master()) {
                for (var instanz : compositeInstanzen) {
                    renderUndPushenComposite(instanz.getKonfiguration(), instanz.getViewId(), instanz::setCachedInitJson,
                            instanz::sseNachrichtPushen, instanz::setLogoQuelle, ws);
                }
                // Entkoppelter Mini-Push-Pfad für die Startseite — kein Tabellen-Mapping,
                // keine Diff-Engine, nur zwei Integer + Diff-Cache.
                pushStartseiteFallsAktiv(ws);
            } else {
                aktualisiereDokumentRegieQuellen(dokument);
                for (var konfig : GlobalProperties.get().getCompositeViewKonfigurationen()) {
                    String viewId = dokumentViewId(dokument.dokumentId(), compositeViewId(konfig.port()));
                    var quelle = dokumentRegieQuellen.get(viewId);
                    if (quelle != null && quelle.laeuft()) {
                        renderUndPushenComposite(konfig, viewId, quelle::setCachedInitJson,
                                quelle::sseNachrichtPushen, quelle::setLogoQuelle, ws);
                    }
                }
                pushStartseiteFuerDokument(dokument, ws);
            }
            lastSuccessfulRefreshAt = System.currentTimeMillis();
        } finally {
            refreshLock.unlock();
            // Livelock-Schutz: konkurrierende Aufrufer haben evtl. nur markDirty() gemacht
            // und sind an tryLock() abgeprallt. Falls jetzt dirty noch true ist, garantiert
            // der erfolgreiche Owner eine Folge-Welle.
            if (modifyListener.isDirty()) {
                modifyListener.scheduleIfNeededExternal();
            }
        }
    }

    private void forceRecalc(WorkingSpreadsheet ws) {
        var doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return;
        }
        var calc = Lo.qi(XCalculatable.class, doc);
        if (calc == null) {
            return;
        }
        try {
            calc.calculate();
        } catch (RuntimeException ex) {
            logger.debug("Recalc übersprungen: {}", ex.getMessage());
        }
    }

    /**
     * Entkoppelter Push-Pfad für die Turnier-Startseite. Ermittelt die aktuelle
     * Teilnehmer-/Aktiv-Zahl und pusht eine {@code startseite_update}-Nachricht,
     * aber nur wenn sich die Zahlen seit dem letzten Push tatsächlich geändert haben.
     * Aktualisiert zusätzlich den Init-Cache (vollständige Nachricht inkl. Logo + Name),
     * damit reconnectende Clients sofort den korrekten Zustand sehen.
     */
    private void pushStartseiteFallsAktiv(WorkingSpreadsheet ws) {
        if (startseiteInstanz == null || !startseiteInstanz.laeuft()) {
            return;
        }
        if (ws == null || ws.getWorkingSpreadsheetDocument() == null) {
            // Kein aktives Dokument (z.B. initialer Refresh direkt beim Add-in-Start,
            // bevor LO ein "current document" gesetzt hat) – nichts zu pushen.
            return;
        }
        try {
            var status = TeilnehmerStatusService.ermitteln(ws);
            boolean checkinListenAnzeigen = GlobalProperties.get().isStartseiteCheckinListenAnzeigen();
            var namenListen = checkinListenAnzeigen
                    ? TeilnehmerStatusService.ermittelnNamen(ws)
                    : new TeilnehmerStatusService.TeilnehmerNamenListen(List.of(), List.of());
            String turniersystem = TurnierStatusErmittler.turniersystemBezeichnung(ws);
            String turnierStatus = TurnierStatusErmittler.ermitteln(ws);
            var docProps = new DocumentPropertiesHelper(ws);
            String logoQuelle = turnierlogoQuelle(ws, docProps);
            String beschreibung = docProps.getStringProperty("Turnierbeschreibung", "");
            String beschreibungAnimation = docProps.getStringProperty(
                    de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog
                            .DOC_PROP_BESCHREIBUNG_ANIMATION,
                    de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog.ANIMATION_DEFAULT);
            int textfarbeInt = docProps.getIntProperty(
                    de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog
                            .DOC_PROP_BESCHREIBUNG_TEXTFARBE,
                    de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog
                            .DEFAULT_BESCHREIBUNG_TEXTFARBE);
            String textfarbe = String.format("#%06x", textfarbeInt & 0xFFFFFF);
            boolean unverändert = status.equals(letzterStartseiteStatus)
                    && turniersystem.equals(letztesStartseiteTurniersystem)
                    && turnierStatus.equals(letzterStartseiteTurnierStatus)
                    && logoQuelle.equals(letztesStartseiteLogo)
                    && beschreibung.equals(letzteStartseiteBeschreibung)
                    && beschreibungAnimation.equals(letzteStartseiteAnimation)
                    && textfarbe.equals(letzteStartseiteTextfarbe)
                    && namenListen.angemeldetNichtEingecheckt().equals(letzteStartseiteNichtEingecheckt)
                    && namenListen.eingecheckt().equals(letzteStartseiteEingecheckt);
            int zoom = GlobalProperties.get().getStartseiteZoom();
            startseiteInstanz.setLogoQuelle(logoQuelle);

            int version = startseiteVersion.incrementAndGet();
            // Frontend referenziert immer den lokalen Endpunkt turnierlogo (Browser darf
            // file:// nicht direkt laden). Relativ halten, damit Regie-URLs wie /ziel/
            // korrekt bei /ziel/turnierlogo bleiben.
            String logoUrl = startseiteLogoUrl(logoQuelle, version);
            var sprueche = StartseiteSprueche.alle();
            // Init-Cache immer mit voller Nachricht (für neue Verbindungen).
            startseiteInstanz.setCachedInitJson(GSON.toJson(StartseiteSseNachricht.init(
                    version, logoUrl, beschreibung, beschreibungAnimation, textfarbe,
                    status.angemeldet(), status.aktiv(),
                    I18n.get("startseite.label.angemeldet"),
                    I18n.get("startseite.label.aktiv"),
                    I18n.get("startseite.tagline"),
                    turniersystem, turnierStatus, sprueche, zoom,
                    checkinListenAnzeigen, namenListen.angemeldetNichtEingecheckt(), namenListen.eingecheckt())));

            if (!unverändert) {
                List<String> neueEintraege = neueEintraege(letzteStartseiteNichtEingecheckt,
                        namenListen.angemeldetNichtEingecheckt());
                var neueImEingecheckt = neueEintraege(letzteStartseiteEingecheckt, namenListen.eingecheckt());
                if (!neueImEingecheckt.isEmpty()) {
                    var kombiniert = new ArrayList<>(neueEintraege);
                    kombiniert.addAll(neueImEingecheckt);
                    neueEintraege = List.copyOf(kombiniert);
                }
                startseiteInstanz.sseNachrichtPushen(GSON.toJson(StartseiteSseNachricht.update(
                        version, logoUrl, beschreibung, beschreibungAnimation, textfarbe,
                        status.angemeldet(), status.aktiv(),
                        I18n.get("startseite.label.angemeldet"),
                        I18n.get("startseite.label.aktiv"),
                        I18n.get("startseite.tagline"),
                        turniersystem, turnierStatus, sprueche, zoom,
                        checkinListenAnzeigen, namenListen.angemeldetNichtEingecheckt(), namenListen.eingecheckt(),
                        neueEintraege)));
                letzterStartseiteStatus = status;
                letztesStartseiteTurniersystem = turniersystem;
                letzterStartseiteTurnierStatus = turnierStatus;
                letztesStartseiteLogo = logoQuelle;
                letzteStartseiteBeschreibung = beschreibung;
                letzteStartseiteAnimation = beschreibungAnimation;
                letzteStartseiteTextfarbe = textfarbe;
                letzteStartseiteNichtEingecheckt = namenListen.angemeldetNichtEingecheckt();
                letzteStartseiteEingecheckt = namenListen.eingecheckt();
            }
        } catch (RuntimeException e) {
            if (istDokumentGeschlossen(e)) {
                logger.debug("Push der Turnier-Startseite übersprungen: Dokument ist bereits geschlossen");
                return;
            }
            logger.warn("Push der Turnier-Startseite fehlgeschlagen: {}", e.getMessage(), e);
        }
    }

    private void pushStartseiteFuerDokument(DokumentQuelle dokument, WorkingSpreadsheet ws) {
        if (!GlobalProperties.get().isStartseiteAktiv()) {
            return;
        }
        String viewId = dokumentViewId(dokument.dokumentId(), STARTSEITE_VIEW_ID);
        var quelle = dokumentRegieQuellen.get(viewId);
        if (quelle == null || !quelle.laeuft()) {
            return;
        }
        try {
            int version = compositeVersionen.computeIfAbsent(viewId, k -> new AtomicInteger(0)).incrementAndGet();
            var daten = startseitePanelDaten(ws, version);
            quelle.setLogoQuelle(daten.logoQuelle());
            String json = GSON.toJson(daten.nachricht());
            quelle.setCachedInitJson(json);
            quelle.sseNachrichtPushen(json);
        } catch (RuntimeException e) {
            if (istDokumentGeschlossen(e)) {
                logger.debug("Startseiten-Refresh für Quelle {} übersprungen: Dokument ist bereits geschlossen", viewId);
                return;
            }
            logger.warn("Push der dokumentbezogenen Turnier-Startseite fehlgeschlagen: {}", e.getMessage(), e);
        }
    }

    static String startseiteLogoUrl(String logoQuelle, int version) {
        return logoQuelle == null || logoQuelle.isBlank() ? "" : "turnierlogo?v=" + version;
    }

    static String turnierlogoQuelle(WorkingSpreadsheet ws, DocumentPropertiesHelper docProps) {
        String startseiteLogo = docProps.getStringProperty(
                de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog
                        .DOC_PROP_TURNIERLOGO_URL,
                "");
        if (!startseiteLogo.isBlank()) {
            return startseiteLogo.trim();
        }
        try {
            var system = docProps.getTurnierSystemAusDocument();
            if (system == null) {
                return "";
            }
            String logo = switch (system) {
                case SUPERMELEE -> new SuperMeleeKonfigurationSheet(ws).getTurnierlogoUrl();
                case SCHWEIZER -> new SchweizerKonfigurationSheet(ws).getTurnierlogoUrl();
                case TRIPTETE -> new TripTeteKonfigurationSheet(ws).getTurnierlogoUrl();
                case JGJ -> new JGJKonfigurationSheet(ws).getTurnierlogoUrl();
                case FORMULEX -> new FormuleXKonfigurationSheet(ws).getTurnierlogoUrl();
                case MAASTRICHTER -> new MaastrichterKonfigurationSheet(ws).getTurnierlogoUrl();
                case KO -> new KoKonfigurationSheet(ws).getTurnierlogoUrl();
                case KASKADE -> new KaskadeKonfigurationSheet(ws).getTurnierlogoUrl();
                case POULE -> new PouleKonfigurationSheet(ws).getTurnierlogoUrl();
                case LIGA -> new LigaKonfigurationSheet(ws).getTurnierlogoUrl();
                case KEIN -> "";
            };
            return logo == null ? "" : logo.trim();
        } catch (RuntimeException e) {
            logger.debug("Turnierlogo aus Konfigurationssheet konnte nicht gelesen werden: {}", e.getMessage());
            return "";
        }
    }

    /** Setzt alle Diff-Cache-Felder der Startseite zurück, sodass der nächste Push erzwungen wird. */
    private void startseiteDiffCacheZuruecksetzen() {
        letzterStartseiteStatus = null;
        letztesStartseiteTurniersystem = "";
        letzterStartseiteTurnierStatus = "";
        letztesStartseiteLogo = "";
        letzteStartseiteBeschreibung = "";
        letzteStartseiteAnimation = "";
        letzteStartseiteTextfarbe = "";
        letzteStartseiteNichtEingecheckt = List.of();
        letzteStartseiteEingecheckt = List.of();
    }

    /**
     * Namen, die seit dem letzten Push neu in {@code neu} aufgetaucht sind (in {@code alt} noch nicht
     * enthalten) – Basis für die dezente „neuer Eintrag"-Animation im Frontend. Beim allerersten Push
     * (leerer Diff-Cache, {@code alt} leer) wird bewusst nichts als „neu" markiert, da sonst beim
     * Verbindungsaufbau die komplette Liste aufblitzen würde.
     */
    private static List<String> neueEintraege(List<String> alt, List<String> neu) {
        if (alt.isEmpty()) {
            return List.of();
        }
        var bekannt = new HashSet<>(alt);
        var neue = new ArrayList<String>();
        for (String name : neu) {
            if (!bekannt.contains(name)) {
                neue.add(name);
            }
        }
        return List.copyOf(neue);
    }

    private void registriereModifyListenerFallsNoetig(DokumentQuelle dokument) {
        var xDoc = dokument.document();
        if (xDoc == null) {
            return;
        }
        if (dokument.master()) {
            registriereMasterModifyListenerFallsNoetig(dokument.ws());
            return;
        }
        if (modifyListenerNachDokument.containsKey(dokument.dokumentId())) {
            return;
        }
        try {
            var broadcaster = Lo.qi(XModifyBroadcaster.class, xDoc);
            if (broadcaster == null) {
                return;
            }
            var listener = new WebserverModifyListener();
            listener.setWs(dokument.ws());
            broadcaster.addModifyListener(listener);
            modifyListenerNachDokument.put(dokument.dokumentId(), listener);
            logger.debug("WebserverModifyListener am Dokument {} registriert", dokument.dokumentId());
        } catch (Exception e) {
            logger.debug("XModifyBroadcaster nicht verfügbar – Live-Updates deaktiviert: {}",
                    e.getMessage());
        }
    }

    private void registriereMasterModifyListenerFallsNoetig(WorkingSpreadsheet ws) {
        var xDoc = ws.getWorkingSpreadsheetDocument();
        if (xDoc == null || xDoc.equals(registriertesDocument)) {
            return;
        }
        deregistriereModifyListener();
        try {
            var broadcaster = Lo.qi(XModifyBroadcaster.class, xDoc);
            modifyListener.setWs(ws);
            broadcaster.addModifyListener(modifyListener);
            registriertesDocument = xDoc;
            logger.debug("WebserverModifyListener am Master-Dokument registriert");
        } catch (Exception e) {
            logger.debug("XModifyBroadcaster nicht verfügbar – Live-Updates deaktiviert: {}",
                    e.getMessage());
        }
    }

    private void deregistriereModifyListener() {
        if (registriertesDocument == null) {
            return;
        }
        try {
            var broadcaster = Lo.qi(XModifyBroadcaster.class, registriertesDocument);
            if (broadcaster != null) {
                broadcaster.removeModifyListener(modifyListener);
            }
        } catch (Exception e) {
            logger.debug("Fehler beim Entfernen des ModifyListeners: {}", e.getMessage());
        }
        modifyListener.setWs(null);
        registriertesDocument = null;
        logger.debug("WebserverModifyListener deregistriert");
    }

    private void deregistriereModifyListener(DokumentQuelle dokument) {
        var listener = modifyListenerNachDokument.remove(dokument.dokumentId());
        if (listener == null) {
            return;
        }
        try {
            var broadcaster = Lo.qi(XModifyBroadcaster.class, dokument.document());
            if (broadcaster != null) {
                broadcaster.removeModifyListener(listener);
            }
        } catch (Exception e) {
            logger.debug("Fehler beim Entfernen des ModifyListeners für {}: {}",
                    dokument.dokumentId(), e.getMessage());
        }
        listener.setWs(null);
    }

    private void entferneAlleModifyListener() {
        for (var dokument : new ArrayList<>(dokumente.values())) {
            if (!dokument.master()) {
                deregistriereModifyListener(dokument);
            }
        }
        modifyListenerNachDokument.clear();
    }

    private void renderUndPushenComposite(
            CompositeViewKonfiguration konfig,
            String viewId,
            Consumer<String> cachedInitSetzen,
            Consumer<String> ssePushen,
            Consumer<String> logoQuelleSetzen,
            WorkingSpreadsheet ws) {
        int port = konfig.port();
        var panelModelle = letzteCompositeModelle.computeIfAbsent(viewId, p -> new ConcurrentHashMap<>());
        var panelTitel = letzteCompositeTitel.computeIfAbsent(viewId, p -> new ConcurrentHashMap<>());
        var fehlendCache = fehlendePanels.computeIfAbsent(viewId, p -> ConcurrentHashMap.newKeySet());
        try {
            var panelNachrichten = new ArrayList<CompositePanelNachricht>();
            boolean irgendeineAenderung = false;
            boolean erstesRendering = false;

            var urlCache = letzteCompositeUrls.computeIfAbsent(viewId, p -> new ConcurrentHashMap<>());
            for (int i = 0; i < konfig.panels().size(); i++) {
                var panelKonfig = konfig.panels().get(i);

                // TIMER-Panel: kein Sheet-Lookup, Timer-Zustand aus letzterTimerZustand
                if (panelKonfig.typ() == PanelTyp.TIMER) {
                    panelNachrichten.add(CompositePanelNachricht.timer(i, panelKonfig.zoom(), panelKonfig.horizontalAusrichtung(), panelKonfig.vertikalAusrichtung(), letzterTimerZustand));
                    // irgendeineAenderung nur beim ersten Rendering setzen – laufende Timer-Updates
                    // kommen über onChange() direkt als diff-Push ohne Sheet-Refresh
                    var timerPanels = initialisiertePanels.computeIfAbsent(viewId, p -> ConcurrentHashMap.newKeySet());
                    if (timerPanels.add(i)) {
                        irgendeineAenderung = true;
                        erstesRendering = true;
                    }
                    continue;
                }

                // URL-/Datei-Panel: kein Sheet-Lookup, nur Diff auf Quellen-Änderung
                if (panelKonfig.typ() == PanelTyp.URL || panelKonfig.typ() == PanelTyp.STATISCHE_DATEI) {
                    String letzteQuelle = urlCache.get(i);
                    if (!java.util.Objects.equals(letzteQuelle, panelKonfig.externeUrl())) {
                        panelNachrichten.add(panelKonfig.typ() == PanelTyp.URL
                                ? CompositePanelNachricht.url(i, panelKonfig.zoom(), panelKonfig.externeUrl())
                                : CompositePanelNachricht.statischeDatei(i, panelKonfig.zoom(), panelKonfig.externeUrl()));
                        urlCache.put(i, panelKonfig.externeUrl());
                        irgendeineAenderung = true;
                        erstesRendering = erstesRendering || letzteQuelle == null;
                    }
                    continue;
                }

                if (panelKonfig.typ() == PanelTyp.TURNIERSTARTSEITE) {
                    int naechsteVersion = compositeVersionen
                            .computeIfAbsent(viewId, p -> new AtomicInteger(0))
                            .get() + 1;
                    var startseite = startseitePanelDaten(ws, naechsteVersion);
                    logoQuelleSetzen.accept(startseite.logoQuelle());
                    panelNachrichten.add(CompositePanelNachricht.startseite(i, startseite.nachricht()));
                    irgendeineAenderung = true;
                    continue;
                }

                var sheetOpt = panelKonfig.resolver().resolve(ws);
                if (sheetOpt.isEmpty()) {
                    // Nicht aufgelöstes Sheet: nur dieses Panel zeigt einen Hinweis,
                    // alle anderen Panels rendern weiterhin normal. Init-Cache enthält
                    // weiter unten die fehlend-Nachricht für reconnectende Clients.
                    panelModelle.remove(i);
                    panelTitel.remove(i);
                    if (fehlendCache.add(i)) {
                        irgendeineAenderung = true;
                        panelNachrichten.add(CompositePanelNachricht.fehlend(i,
                                I18n.get("webserver.hinweis.sheet.nicht.gefunden.titel"),
                                I18n.get("webserver.hinweis.sheet.nicht.gefunden.text",
                                        panelKonfig.resolver().getAnzeigeName())));
                    }
                    continue;
                }
                var sheet = sheetOpt.get();
                String neuerTitel = baueSeitenTitel(panelKonfig.resolver(), sheet);
                TabelleModel neuesModell = mapper.map(sheet, ws.getWorkingSpreadsheetDocument());
                TabelleModel altesModell = panelModelle.get(i);
                String alterTitel = panelTitel.get(i);
                boolean warVorherFehlend = fehlendCache.remove(i);

                if (altesModell != null && layoutGeaendert(altesModell, neuesModell)) {
                    altesModell = null;
                }

                TabelleModel diffModell = diffEngine.diff(altesModell, neuesModell);
                boolean panelGeaendert = altesModell == null
                        || !diffModell.getZellen().isEmpty()
                        || kopfFusszeileGeaendert(altesModell, neuesModell)
                        || !java.util.Objects.equals(alterTitel, neuerTitel)
                        || warVorherFehlend;

                if (altesModell == null) {
                    erstesRendering = true;
                }

                panelModelle.put(i, neuesModell);
                panelTitel.put(i, neuerTitel);

                if (panelGeaendert) {
                    irgendeineAenderung = true;
                }
                // Beim ersten Rendering (altesModell == null) oder beim Wechsel fehlend→OK
                // vollständiges Modell senden, sonst nur Diff wenn nötig
                TabelleModel zuSendendesModell = (altesModell == null) ? neuesModell : diffModell;
                panelNachrichten.add(CompositePanelNachricht.init(i, zuSendendesModell, neuerTitel,
                        panelKonfig.zoom(), panelKonfig.sichtbarerTabellenAnteil(),
                        panelKonfig.horizontalAusrichtung(), panelKonfig.vertikalAusrichtung(), panelKonfig.blattnameAnzeigen()));
            }

            if (!irgendeineAenderung) {
                return;
            }

            int version = compositeVersionen
                    .computeIfAbsent(viewId, p -> new AtomicInteger(0))
                    .incrementAndGet();

            // Init-Cache mit vollem State aller Panels befüllen
            var alleInitPanels = new ArrayList<CompositePanelNachricht>();
            for (int i = 0; i < konfig.panels().size(); i++) {
                var panelKonfig = konfig.panels().get(i);
                if (panelKonfig.typ() == PanelTyp.TIMER) {
                    alleInitPanels.add(CompositePanelNachricht.timer(i, panelKonfig.zoom(), panelKonfig.horizontalAusrichtung(), panelKonfig.vertikalAusrichtung(), letzterTimerZustand));
                    continue;
                }
                if (panelKonfig.typ() == PanelTyp.URL || panelKonfig.typ() == PanelTyp.STATISCHE_DATEI) {
                    String cachedQuelle = urlCache.get(i);
                    if (cachedQuelle != null) {
                        alleInitPanels.add(panelKonfig.typ() == PanelTyp.URL
                                ? CompositePanelNachricht.url(i, panelKonfig.zoom(), cachedQuelle)
                                : CompositePanelNachricht.statischeDatei(i, panelKonfig.zoom(), cachedQuelle));
                    }
                    continue;
                }
                if (panelKonfig.typ() == PanelTyp.TURNIERSTARTSEITE) {
                    var startseite = startseitePanelDaten(ws, version);
                    logoQuelleSetzen.accept(startseite.logoQuelle());
                    alleInitPanels.add(CompositePanelNachricht.startseite(i, startseite.nachricht()));
                    continue;
                }
                var vollModell = panelModelle.get(i);
                if (vollModell != null) {
                    alleInitPanels.add(CompositePanelNachricht.init(i, vollModell, panelTitel.getOrDefault(i, ""),
                            panelKonfig.zoom(), panelKonfig.sichtbarerTabellenAnteil(),
                            panelKonfig.horizontalAusrichtung(), panelKonfig.vertikalAusrichtung(), panelKonfig.blattnameAnzeigen()));
                } else if (fehlendCache.contains(i)) {
                    alleInitPanels.add(CompositePanelNachricht.fehlend(i,
                            I18n.get("webserver.hinweis.sheet.nicht.gefunden.titel"),
                            I18n.get("webserver.hinweis.sheet.nicht.gefunden.text",
                                    panelKonfig.resolver().getAnzeigeName())));
                }
            }
            CompositeSseNachricht initNachricht = CompositeSseNachricht.init(version, alleInitPanels, konfig.wurzel(), konfig.zoom(), konfig.mitHeaderFooter(), konfig.rand().toDaten());
            cachedInitSetzen.accept(GSON.toJson(initNachricht));

            // Push: beim ersten Rendering init, sonst diff mit geänderten Panels
            CompositeSseNachricht push = erstesRendering
                    ? initNachricht
                    : CompositeSseNachricht.diff(version, panelNachrichten, konfig.wurzel(), konfig.zoom(), konfig.mitHeaderFooter(), konfig.rand().toDaten());
            ssePushen.accept(GSON.toJson(push));

        } catch (Exception e) {
            if (istDokumentGeschlossen(e)) {
                logger.debug("Composite-Refresh für Quelle {} übersprungen: Dokument ist bereits geschlossen", viewId);
                return;
            }
            logger.error("Fehler beim Rendern des Composite Views für Quelle {}: {}", viewId, e.getMessage(), e);
            safeProcessBoxFehler(I18n.get("webserver.prozessbox.render.fehler", port, e.getMessage()));
        }
    }

    static boolean istDokumentGeschlossen(Throwable e) {
        for (Throwable aktuelle = e; aktuelle != null; aktuelle = aktuelle.getCause()) {
            if (aktuelle instanceof DisposedException) {
                return true;
            }
        }
        return false;
    }

    private void sendeCompositeHinweis(CompositeViewInstanz instanz, String titel, String text) {
        String json = GSON.toJson(CompositeSseNachricht.hinweis(titel, text));
        instanz.setCachedInitJson(json);
        instanz.sseNachrichtPushen(json);
    }

    /**
     * Baut den Seitentitel aus dem Anzeigenamen des Resolvers und – wenn vorhanden – der laufenden Nummer.
     * Beispiel: "Spielrunde" + Nummer 3 → "Spielrunde 3"
     */
    private static String baueSeitenTitel(SheetResolver resolver, XSpreadsheet sheet) {
        var name = resolver.getAnzeigeName();
        return resolver.getNummer(sheet)
                .map(nr -> name + " " + nr)
                .orElse(name);
    }

    private void sendeHinweisAnAlle(String titel, String text) {
        for (var instanz : compositeInstanzen) {
            sendeCompositeHinweis(instanz, titel, text);
        }
    }

    @Override
    public void onLoad(Object source) {
        registriereDokumentAusEvent(source);
    }

    @Override
    public void onLoadFinished(Object source) {
        registriereDokumentAusEvent(source);
    }

    @Override
    public void onViewCreated(Object source) {
        registriereDokumentAusEvent(source);
    }

    @Override
    public void onFocus(Object source) {
        registriereDokumentAusEvent(source);
    }

    @Override
    public void onUnload(Object source) {
        if (!laeuft) {
            return;
        }
        var doc = DocumentHelper.getCurrentSpreadsheetDocumentFrom(source);
        if (doc == null) {
            return;
        }
        if (istOwnerDocument(doc)) {
            logger.info("Master-Dokument geschlossen – WebServer wird gestoppt");
            stoppen();
            return;
        }
        entferneDokument(doc);
    }

    private void registriereDokumentAusEvent(Object source) {
        if (!laeuft || gespeicherterCtx == null) {
            return;
        }
        var doc = DocumentHelper.getCurrentSpreadsheetDocumentFrom(source);
        if (doc == null) {
            return;
        }
        registriereDokumentFallsTurnier(new WorkingSpreadsheet(gespeicherterCtx, doc))
                .ifPresent(this::initialenDokumentRefreshPlanen);
        if (regieInstanz != null && regieInstanz.laeuft()) {
            regieInstanz.konfigurationGeaendert();
        }
    }

    private void initialenDokumentRefreshPlanen(DokumentQuelle dokument) {
        if (dokument.master()) {
            modifyListener.markDirtyAndSchedule();
            return;
        }
        var listener = modifyListenerNachDokument.get(dokument.dokumentId());
        if (listener != null) {
            listener.markDirtyAndSchedule();
        }
    }

    private void entferneDokument(XSpreadsheetDocument doc) {
        String dokumentId = dokumentId(doc);
        var dokument = dokumente.remove(dokumentId);
        if (dokument == null || dokument.master()) {
            return;
        }
        deregistriereModifyListener(dokument);
        var prefix = "doc:" + encodeViewIdTeil(dokumentId) + ":";
        for (var eintrag : new ArrayList<>(dokumentRegieQuellen.entrySet())) {
            if (eintrag.getKey().startsWith(prefix)) {
                var quelle = eintrag.getValue();
                dokumentRegieQuellen.remove(eintrag.getKey());
                entferneCachesFuerQuelle(eintrag.getKey());
                quelle.stoppen();
            }
        }
        if (regieInstanz != null && regieInstanz.laeuft()) {
            regieInstanz.konfigurationGeaendert();
        }
        statusListenerBenachrichtigen();
    }

    public boolean isLaeuft() {
        return laeuft;
    }

    /**
     * Gibt true zurück wenn Slot (0-basiert) eine aktive, laufende Instanz hat.
     * Prüft explizit {@code instanz.laeuft()} – verhindert optimistisch-falsches Menü.
     */
    public synchronized boolean hatInstanzFuerSlot(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            return false;
        }
        var instanz = slots.get(slot);
        return instanz != null && instanz.laeuft();
    }

    /** true wenn irgendein Slot eine aktive Instanz hat (für Separator-Sichtbarkeit). */
    public synchronized boolean hatIrgendeinenAktivenSlot() {
        return slots.stream().anyMatch(i -> i != null && i.laeuft());
    }

    /**
     * Gibt die URL (Hostname:PORT) für den angegebenen Slot zurück, oder {@code null} wenn kein aktiver Port.
     * Gibt {@code null} zurück wenn slot außerhalb des gültigen Bereichs – kein Crash.
     */
    public synchronized String getUrlFuerSlot(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            return null;
        }
        var instanz = slots.get(slot);
        return (instanz != null && instanz.laeuft())
                ? buildUrl(instanz.getPort())
                : null;
    }

    /**
     * Gibt den Menü-Label für einen Slot zurück: "[AnzeigeName] – [URL]".
     * <p>
     * Der {@code AnzeigeName} stammt aus dem {@link SheetResolver} – er ist bereits auf spätere
     * Erweiterungen (mehrere Sheets pro Port) vorbereitet: der Resolver kann dann einen
     * kombinierten Namen zurückgeben, ohne dass diese Methode angepasst werden muss.
     *
     * @param slot 0-basierter Slot-Index
     * @return Label, oder {@code null} wenn kein aktiver Port für den Slot
     */
    public synchronized String getMenuLabelFuerSlot(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            return null;
        }
        var instanz = slots.get(slot);
        if (instanz == null || !instanz.laeuft()) {
            return null;
        }
        var url = buildUrl(instanz.getPort());
        var name = instanz.getAnzeigeName();
        return name + " – " + url;
    }

    public synchronized List<RegieQuelleInfo> verfuegbareRegieQuellen() {
        var result = new ArrayList<RegieQuelleInfo>();
        for (var eintrag : GlobalProperties.get().getCompositeViewEintraege()) {
            if (!eintrag.aktiv()) {
                continue;
            }
            String id = compositeViewId(eintrag.port());
            boolean laeuft = compositeInstanzen.stream()
                    .anyMatch(i -> i.getKonfiguration().port() == eintrag.port() && i.laeuft());
            String name = eintrag.name() == null || eintrag.name().isBlank()
                    ? I18n.get("webserver.compositeview.anzeigename") + " " + eintrag.port()
                    : eintrag.name().trim();
            result.add(new RegieQuelleInfo(id, name, eintrag.port(), laeuft,
                    I18n.get("webserver.regie.dokument.master"), true));
        }
        if (GlobalProperties.get().isStartseiteAktiv()) {
            int startseitePort = GlobalProperties.get().getStartseitePort();
            boolean startseiteLaeuft = startseiteInstanz != null && startseiteInstanz.laeuft();
            result.add(new RegieQuelleInfo(STARTSEITE_VIEW_ID, I18n.get("startseite.anzeigename"),
                    startseitePort, startseiteLaeuft, I18n.get("webserver.regie.dokument.master"), true));
        }
        dokumentRegieQuellen.values().stream()
                .filter(DokumentRegieQuelle::laeuft)
                .sorted(Comparator.comparing(DokumentRegieQuelle::getAnzeigeName))
                .map(q -> new RegieQuelleInfo(q.getViewId(), q.getAnzeigeName(), q.getPort(), q.laeuft(),
                        dokumentIdAusViewId(q.getViewId())
                                .flatMap(id -> Optional.ofNullable(dokumente.get(id)))
                                .map(DokumentQuelle::anzeigename)
                                .orElse(""),
                        false))
                .forEach(result::add);
        return result;
    }

    public synchronized Optional<RegieQuelle> laufendeRegieQuelleFuerId(String viewId) {
        if (viewId == null || viewId.isBlank()) {
            return Optional.empty();
        }
        if (STARTSEITE_VIEW_ID.equals(viewId)) {
            return startseiteInstanz != null && startseiteInstanz.laeuft()
                    ? Optional.of(startseiteInstanz)
                    : Optional.empty();
        }
        var dokumentQuelle = dokumentRegieQuellen.get(viewId);
        if (dokumentQuelle != null && dokumentQuelle.laeuft()) {
            return Optional.of(dokumentQuelle);
        }
        for (var instanz : compositeInstanzen) {
            if (instanz.laeuft() && viewId.equals(instanz.getViewId())) {
                return Optional.of(instanz);
            }
        }
        return Optional.empty();
    }

    /**
     * Prüft ob das übergebene Dokument das Owner-Dokument des laufenden WebServers ist.
     * Nur das Owner-Dokument darf den WebServer stoppen und sieht die URL-Slots als aktiv.
     *
     * @param doc das zu prüfende Dokument (darf null sein)
     * @return true wenn WS läuft und {@code doc} das Owner-Dokument ist
     */
    public synchronized boolean istOwnerDocument(XSpreadsheetDocument doc) {
        return laeuft && ownerDocument != null && doc != null && ownerDocument.equals(doc);
    }

    /**
     * Prüft ob sich das Frontend-Layout geändert hat: Zeilen-/Spaltenanzahl
     * oder die absolute Startposition des gerenderten Bereichs.
     * <p>
     * Reine Inhaltsänderungen ohne Strukturänderung werden hier nicht erkannt –
     * die {@link DiffEngine} übernimmt diesen Teil.
     */
    private static boolean layoutGeaendert(TabelleModel altes, TabelleModel neues) {
        return altes.getZeilen()      != neues.getZeilen()
            || altes.getSpalten()     != neues.getSpalten()
            || altes.getStartZeile()  != neues.getStartZeile()
            || altes.getStartSpalte() != neues.getStartSpalte();
    }

    /**
     * Prüft ob sich Kopf- oder Fußzeile geändert hat.
     * Nötig, da die {@link DiffEngine} nur Zellinhalte vergleicht.
     */
    private static boolean kopfFusszeileGeaendert(TabelleModel altes, TabelleModel neues) {
        return !java.util.Objects.equals(altes.getKopfzeileLinks(),  neues.getKopfzeileLinks())
            || !java.util.Objects.equals(altes.getKopfzeileMitte(),  neues.getKopfzeileMitte())
            || !java.util.Objects.equals(altes.getKopfzeileRechts(), neues.getKopfzeileRechts())
            || !java.util.Objects.equals(altes.getFusszeileLinks(),  neues.getFusszeileLinks())
            || !java.util.Objects.equals(altes.getFusszeileMitte(),  neues.getFusszeileMitte())
            || !java.util.Objects.equals(altes.getFusszeileRechts(), neues.getFusszeileRechts());
    }

    /** Baut eine URL aus einem Port – zentrale Methode, kein String-Duplikat. */
    private static String buildUrl(int port) {
        return "http://" + ermittleHostname() + ":" + port + "/";
    }

    /**
     * Ermittelt den Hostnamen oder die IP-Adresse dieses Rechners für die URL-Anzeige.
     * <p>
     * Reihenfolge: Systemhostname → erste nicht-loopback IPv4-Adresse → "localhost".
     */
    private static String ermittleHostname() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            if (hostname != null && !hostname.equals("localhost") && !hostname.startsWith("127.")) {
                return hostname;
            }
        } catch (Exception e) {
            logger.debug("Hostname nicht ermittelbar: {}", e.getMessage());
        }
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                var iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                var adressen = iface.getInetAddresses();
                while (adressen.hasMoreElements()) {
                    var adresse = adressen.nextElement();
                    if (adresse instanceof Inet4Address && !adresse.isLoopbackAddress()) {
                        return adresse.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Netzwerk-Interfaces nicht ermittelbar: {}", e.getMessage());
        }
        return "localhost";
    }

    /** ProcessBox-Info-Meldung, sicher auch wenn ProcessBox nicht initialisiert ist. */
    private static void safeProcessBoxInfo(String msg) {
        try {
            ProcessBox.from().info(msg);
        } catch (Exception e) {
            logger.debug("ProcessBox nicht verfügbar für Meldung: {}", msg);
        }
    }

    /** ProcessBox-Fehler-Meldung, sicher auch wenn ProcessBox nicht initialisiert ist. */
    private static void safeProcessBoxFehler(String msg) {
        try {
            ProcessBox.from().fehler(msg);
        } catch (Exception e) {
            logger.debug("ProcessBox nicht verfügbar für Fehlermeldung: {}", msg);
        }
    }
}
