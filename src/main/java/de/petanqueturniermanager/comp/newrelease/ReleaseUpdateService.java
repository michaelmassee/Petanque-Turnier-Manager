/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.helper.perflog.PerfLog;

/**
 * Zentraler Service für die Plugin-Versionserkennung und den Update-Check.
 *
 * <p>
 * <b>Lifecycle:</b>
 * <ul>
 *   <li>{@link #init(XComponentContext)} wird beim Plugin-Start aufgerufen.
 *       Der Aufruf kehrt sofort zurück – jeglicher Netz-IO läuft im
 *       Daemon-Executor.</li>
 *   <li>{@link #get()} liefert den initialisierten Singleton.</li>
 *   <li>{@link #dispose()} fährt den Executor herunter (idempotent).
 *       Wird vom {@code TerminateListener} bei LO-Shutdown gerufen.</li>
 * </ul>
 *
 * <p>
 * <b>Update-Strategie:</b> bei jedem LO-Start ein Live-Fetch von GitHub mit bis zu
 * drei Versuchen (Backoff {@code 1s}, {@code 5s}, {@code 30s}). Kein Disk-Cache –
 * der Check läuft ohnehin nur einmal pro (typischerweise stunden- bis tagelanger)
 * LO-Session, ein Cache würde daher kaum echte Netz-Calls einsparen, aber eine
 * zusätzliche Fehlerklasse (Korruption, Serialisierung) einführen. Schlagen alle
 * Retries fehl, gibt es für diese Session einfach keine Update-Info –
 * Status {@link UpdateStatus#NICHT_VERFUEGBAR}.
 *
 * <p>
 * Listener-Notifications laufen im Executor – nie im aufrufenden Thread,
 * insbesondere nicht im UI-Thread.
 */
public final class ReleaseUpdateService {

    private static final Logger logger = LogManager.getLogger(ReleaseUpdateService.class);

    public static final String GITHUB_REPOSITORY = "michaelmassee/Petanque-Turnier-Manager";
    static final List<Duration> DEFAULT_RETRY_BACKOFFS = List.of(
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            Duration.ofSeconds(30));

    private static final Object SINGLETON_LOCK = new Object();
    // Zugriff stets unter SINGLETON_LOCK.
    private static @Nullable ReleaseUpdateService instanz;

    private final XComponentContext context;
    private final GithubReleaseClient client;
    private final List<Duration> retryBackoffs;
    private final ExecutorService executor;
    private final CopyOnWriteArrayList<Runnable> statusListener = new CopyOnWriteArrayList<>();

    private volatile UpdateStatus status = UpdateStatus.UNBEKANNT;
    private volatile Optional<ReleaseInfo> aktuellesRelease = Optional.empty();
    private volatile Optional<String> installierteVersion = Optional.empty();
    private volatile boolean disposed;

    ReleaseUpdateService(XComponentContext context, GithubReleaseClient client) {
        this(context, client, DEFAULT_RETRY_BACKOFFS);
    }

    ReleaseUpdateService(XComponentContext context, GithubReleaseClient client, List<Duration> retryBackoffs) {
        this.context = Objects.requireNonNull(context, "context");
        this.client = Objects.requireNonNull(client, "client");
        this.retryBackoffs = List.copyOf(Objects.requireNonNull(retryBackoffs, "retryBackoffs"));
        this.executor = Executors.newSingleThreadExecutor(daemonThreadFactory());
    }

    /**
     * Startet den Service. Erster Refresh läuft im Hintergrund – diese Methode blockiert nicht.
     * Idempotent: ein zweiter Aufruf wirft eine {@link IllegalStateException} nicht, sondern
     * loggt und ignoriert (LO kann den Init-Pfad mehrfach durchlaufen).
     */
    public static void init(XComponentContext context) {
        Objects.requireNonNull(context, "context");
        synchronized (SINGLETON_LOCK) {
            if (instanz != null) {
                logger.debug("ReleaseUpdateService bereits initialisiert – init() wird ignoriert");
                return;
            }
            instanz = new ReleaseUpdateService(context, new GithubReleaseClient(GITHUB_REPOSITORY));
            instanz.starteInitialenRefresh();
        }
    }

    public static ReleaseUpdateService get() {
        synchronized (SINGLETON_LOCK) {
            if (instanz == null) {
                throw new IllegalStateException("ReleaseUpdateService nicht initialisiert – init(context) zuerst aufrufen");
            }
            return instanz;
        }
    }

    /**
     * Test-Hook: ersetzt den Singleton durch eine vorgegebene Instanz und gibt
     * die bisherige (falls vorhanden) zur Shutdown-Behandlung zurück.
     */
    static Optional<ReleaseUpdateService> ersetzeSingletonFuerTest(ReleaseUpdateService neu) {
        synchronized (SINGLETON_LOCK) {
            var bisher = Optional.ofNullable(instanz);
            instanz = neu;
            return bisher;
        }
    }

    /** Test-Hook: räumt den Singleton-Slot. */
    static void resetSingletonFuerTest() {
        synchronized (SINGLETON_LOCK) {
            if (instanz != null) {
                instanz.dispose();
            }
            instanz = null;
        }
    }

    private void starteInitialenRefresh() {
        // Komplett asynchron auf dem Background-Executor: InstallierteVersion.ermitteln
        // ist UNO-IO, das nicht auf dem LO-Main-Thread im Plugin-Init laufen darf
        // (sonst weißes Calc-Fenster beim Start).
        executor.execute(this::initialerRefreshInternalMitTiming);
    }

    private void initialerRefreshInternalMitTiming() {
        long startNs = System.nanoTime();
        try {
            initialerRefreshInternal();
        } finally {
            long dauerMs = (System.nanoTime() - startNs) / 1_000_000L;
            PerfLog.log(logger, "[STARTUP-TIMING] ReleaseUpdateService initialerRefresh (background): {} ms", dauerMs);
        }
    }

    private void initialerRefreshInternal() {
        aktualisiereInstallierteVersionFallsUnbekannt();
        fuehreRefreshAus();
    }

    /**
     * Versucht die installierte Version zu ermitteln, falls noch unbekannt.
     * Wird sowohl beim Start als auch vor jedem Retry-Versuch aufgerufen: liefert
     * {@link com.sun.star.deployment.XPackageInformationProvider} die eigene Extension
     * in einem frühen Init-Moment noch nicht (LO-Registrierung u.U. noch nicht
     * abgeschlossen), bekommt der Lookup so innerhalb des bestehenden
     * Retry-Fensters mehrere Versuche, statt für die ganze Session leer zu bleiben.
     */
    private void aktualisiereInstallierteVersionFallsUnbekannt() {
        if (installierteVersion.isEmpty()) {
            installierteVersion = InstallierteVersion.ermitteln(context).map(InstallierteVersion::raw);
        }
    }

    /**
     * Liefert den aktuellen Status. Nicht-blockierend.
     */
    public UpdateStatus getStatus() {
        return status;
    }

    /**
     * Installierte Plugin-Version (roh), falls ermittelbar.
     */
    public Optional<String> getInstallierteVersion() {
        return installierteVersion;
    }

    /**
     * Zuletzt live von GitHub abgerufene Release-Info.
     */
    public Optional<ReleaseInfo> getAktuellesRelease() {
        return aktuellesRelease;
    }

    /**
     * Komfort: liefert den Tag-Namen des letzten bekannten Releases (z.B. {@code v1.2.3}).
     */
    public Optional<String> getNeuesteVersionTag() {
        return aktuellesRelease.map(ReleaseInfo::tagName);
    }

    /**
     * Komfort: {@code true} wenn der Status {@link UpdateStatus#UPDATE_VERFUEGBAR} ist.
     */
    public boolean isUpdateVerfuegbar() {
        return status == UpdateStatus.UPDATE_VERFUEGBAR;
    }

    /**
     * Stößt einen Refresh-Versuch im Hintergrund an.
     * Nicht-blockierend; ist der Executor bereits geshutdownet, passiert nichts.
     */
    public void triggerRefresh() {
        plane();
    }

    /**
     * Registriert einen Listener, der bei jedem Statuswechsel aufgerufen wird.
     * Wer addiert, MUSS in seinem dispose-Pfad {@link #removeStatusListener(Runnable)} rufen –
     * sonst entstehen Memory-Leaks (LO hält UI-Komponenten u.U. lange).
     */
    public void addStatusListener(Runnable listener) {
        statusListener.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeStatusListener(Runnable listener) {
        statusListener.remove(listener);
    }

    /**
     * Stösst alle registrierten Listener synchron im aufrufenden Thread an.
     * Nutzbar wenn sich externe Bedingungen ändern, die die UI-Darstellung
     * beeinflussen (z.B. "newversioncheck immer true"-Override), ohne dass
     * sich der Status selbst ändert.
     */
    public void loeseListenerAus() {
        benachrichtigeListener();
    }

    int listenerCountFuerTest() {
        return statusListener.size();
    }

    /**
     * Idempotenter Shutdown des Hintergrund-Executors.
     */
    public synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                logger.debug("Executor wurde nicht innerhalb von 2s beendet");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void plane() {
        if (disposed || executor.isShutdown()) {
            return;
        }
        try {
            executor.execute(this::fuehreRefreshAus);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            logger.debug("Refresh-Task abgewiesen (Executor herunterfahren?)");
        }
    }

    private void fuehreRefreshAus() {
        setzeStatus(UpdateStatus.LAEUFT);
        benachrichtigeListener();
        for (int versuch = 0; versuch < retryBackoffs.size(); versuch++) {
            if (Thread.currentThread().isInterrupted() || disposed) {
                return;
            }
            aktualisiereInstallierteVersionFallsUnbekannt();
            var release = client.ladeLetztesRelease(GlobalProperties.get().isIncludeBetaVersions());
            if (release.isPresent()) {
                aktuellesRelease = release;
                aktualisiereStatusAusRelease(release.get());
                benachrichtigeListener();
                return;
            }
            if (versuch + 1 < retryBackoffs.size()) {
                if (!warte(retryBackoffs.get(versuch + 1))) {
                    return;
                }
            }
        }
        // alle Retries fehlgeschlagen – kein Release-Stand für diese Session.
        setzeStatus(UpdateStatus.NICHT_VERFUEGBAR);
        benachrichtigeListener();
    }

    private boolean warte(Duration dauer) {
        try {
            Thread.sleep(dauer.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void aktualisiereStatusAusRelease(ReleaseInfo release) {
        var inst = installierteVersion.orElse(null);
        var verfuegbar = release.tagName();
        var betaErlaubt = GlobalProperties.get().isIncludeBetaVersions();
        if (release.prerelease() && !betaErlaubt) {
            setzeStatus(UpdateStatus.KEIN_UPDATE);
            return;
        }
        if (VersionVergleicher.istNeuer(inst, verfuegbar, betaErlaubt)) {
            setzeStatus(UpdateStatus.UPDATE_VERFUEGBAR);
        } else {
            setzeStatus(UpdateStatus.KEIN_UPDATE);
        }
    }

    private void setzeStatus(UpdateStatus neu) {
        if (status != neu) {
            logger.debug("UpdateStatus: {} → {}", status, neu);
            status = neu;
        }
    }

    private void benachrichtigeListener() {
        for (var listener : statusListener) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                logger.warn("Statuslistener warf Exception – andere Listener werden weiter benachrichtigt", e);
            }
        }
    }

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            var t = new Thread(runnable, "ptm-release-check");
            t.setDaemon(true);
            return t;
        };
    }
}
