package de.petanqueturniermanager.spielerdb.webview;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.net.httpserver.HttpServer;

import de.petanqueturniermanager.spielerdb.LabelRepository;
import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.spielerdb.VereinRepository;

/**
 * Lifecycle-Singleton für den read-only Spieler-DB-Web-Viewer. Bindet stets an
 * {@code 127.0.0.1} (kein {@code "localhost"} wegen IPv6-Doppelresolution,
 * kein {@code 0.0.0.0} aus Sicherheitsgründen) und lässt das OS einen freien
 * Port wählen ({@code Port 0}). Ein wiederholter Aufruf von
 * {@link #starteOderHole(SpielerDbConnection)} liefert idempotent dieselbe URL.
 */
public final class SpielerDbWebViewServer {

    private static final Logger logger = LogManager.getLogger(SpielerDbWebViewServer.class);
    private static final String BIND_HOST = "127.0.0.1";
    private static final int BACKLOG = 10;

    private static final AtomicReference<RunningServer> CURRENT = new AtomicReference<>();
    private static final Object STARTUP_LOCK = new Object();

    private SpielerDbWebViewServer() {}

    /**
     * Startet den Server falls noch nicht gestartet, sonst gibt die bestehende
     * URL zurück. Der Aufruf ist threadsafe und idempotent.
     */
    public static URI starteOderHole(SpielerDbConnection conn) throws SpielerDbException {
        RunningServer aktuell = CURRENT.get();
        if (aktuell != null) {
            return aktuell.viewUri();
        }
        synchronized (STARTUP_LOCK) {
            aktuell = CURRENT.get();
            if (aktuell != null) {
                return aktuell.viewUri();
            }
            try {
                RunningServer neu = baueUndStarte(conn);
                CURRENT.set(neu);
                Runtime.getRuntime().addShutdownHook(
                        new Thread(SpielerDbWebViewServer::stoppe, "spielerdb-webview-shutdown"));
                logger.info("Spieler-DB-Web-Viewer läuft auf {}", neu.viewUri());
                return neu.viewUri();
            } catch (IOException e) {
                throw new SpielerDbException("Web-Viewer konnte nicht gestartet werden", e);
            }
        }
    }

    /** Stoppt den Server, falls einer läuft. Mehrfachaufrufe sind erlaubt. */
    public static void stoppe() {
        RunningServer alt = CURRENT.getAndSet(null);
        if (alt == null) {
            return;
        }
        try {
            alt.server().stop(0);
        } catch (RuntimeException e) {
            logger.warn("Fehler beim Stoppen des Web-Viewers", e);
        }
        alt.pool().shutdownNow();
        logger.info("Spieler-DB-Web-Viewer gestoppt (Port {})", alt.port());
    }

    private static RunningServer baueUndStarte(SpielerDbConnection conn) throws IOException {
        InetAddress loopback;
        try {
            loopback = InetAddress.getByName(BIND_HOST);
        } catch (UnknownHostException e) {
            throw new IOException("Loopback-Adresse nicht auflösbar", e);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), BACKLOG);
        ExecutorService pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "spielerdb-webview");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(pool);

        SpielerRepository spielerRepo = new SpielerRepository(conn);
        VereinRepository vereinRepo = new VereinRepository(conn);
        LabelRepository labelRepo = new LabelRepository(conn);

        SpielerDbApiHandler apiHandler = new SpielerDbApiHandler(
                spielerRepo, vereinRepo, labelRepo, conn.getJdbcUrl());
        StaticAssetsHandler assetsHandler = new StaticAssetsHandler();

        server.createContext("/api", apiHandler);
        server.createContext("/", assetsHandler);
        server.start();
        int port = server.getAddress().getPort();
        URI uri = URI.create("http://" + BIND_HOST + ":" + port + "/");
        return new RunningServer(server, pool, port, uri);
    }

    private record RunningServer(HttpServer server, ExecutorService pool, int port, URI viewUri) {}

    @Nullable
    static RunningServer aktuell() {
        return CURRENT.get();
    }
}
