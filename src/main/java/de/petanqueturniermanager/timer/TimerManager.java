package de.petanqueturniermanager.timer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

/**
 * Verwaltet den Rundenzeit-Timer.
 * <p>
 * Lifecycle: Wird von {@code PetanqueTurnierMngrSingleton.init()} initialisiert und
 * von {@code PetanqueTurnierMngrSingleton.dispose()} heruntergefahren.
 * <p>
 * Countdown-Berechnung basiert auf {@link System#nanoTime()} um Drift zu vermeiden.
 * <p>
 * Alle Zustandsänderungen werden über den {@link TimerListener}-Mechanismus
 * an registrierte Listener emittiert (Observer-Pattern).
 */
public class TimerManager {

    private static final Logger logger = LogManager.getLogger(TimerManager.class);

    private static volatile TimerManager instanz;

    private final XComponentContext xContext;
    private final ScheduledExecutorService executor;
    private final List<TimerListener> listeners = new CopyOnWriteArrayList<>();

    private volatile TimerZustand zustand = TimerZustand.INAKTIV;
    private volatile long endNanos;
    private volatile long restNanos;
    private volatile String bezeichnung = "";
    private volatile ScheduledFuture<?> tickTask;
    private volatile TimerWebServerInstanz webServerInstanz;
    private volatile int letzterPort = TimerEinstellungen.DEFAULT_PORT;

    private TimerManager(XComponentContext xContext) {
        this.xContext = xContext;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "PTM-Timer");
            t.setDaemon(true);
            return t;
        });
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    /**
     * Initialisiert den TimerManager. Darf nur einmal aufgerufen werden.
     * Wird von {@code PetanqueTurnierMngrSingleton.init()} aufgerufen.
     */
    public static void init(XComponentContext xContext) {
        if (instanz == null) {
            synchronized (TimerManager.class) {
                if (instanz == null) {
                    instanz = new TimerManager(xContext);
                    logger.debug("TimerManager initialisiert");
                }
            }
        }
    }

    /**
     * Gibt die aktive Instanz zurück.
     *
     * @throws IllegalStateException wenn {@link #init} noch nicht aufgerufen wurde
     */
    public static TimerManager get() {
        var tm = instanz;
        if (tm == null) {
            throw new IllegalStateException("TimerManager nicht initialisiert");
        }
        return tm;
    }

    /**
     * Fährt den TimerManager sauber herunter.
     * Wird von {@code PetanqueTurnierMngrSingleton.dispose()} aufgerufen.
     */
    public static void dispose() {
        synchronized (TimerManager.class) {
            var tm = instanz;
            if (tm != null) {
                tm.stoppen();
                tm.executor.shutdownNow();
                if (tm.webServerInstanz != null) {
                    tm.webServerInstanz.stoppen();
                }
                instanz = null;
                logger.debug("TimerManager disposed");
            }
        }
    }

    // ── Listener-Verwaltung ────────────────────────────────────────────────────

    /** Registriert einen Listener, der bei jeder Zustandsänderung benachrichtigt wird. */
    public void addListener(TimerListener listener) {
        listeners.add(listener);
    }

    /** Entfernt einen Listener. */
    public void removeListener(TimerListener listener) {
        listeners.remove(listener);
    }

    /** Gibt den aktuellen Zustand zurück (für Polling, z.B. durch Statusleiste bei Update). */
    public TimerState getAktuellerZustand() {
        return aktuellerState();
    }

    // ── Timer-Steuerung ────────────────────────────────────────────────────────

    /**
     * Startet den Timer. Ein eventuell laufender Timer wird zuerst gestoppt.
     *
     * @param dauerSekunden Gesamtdauer in Sekunden (muss &gt; 0 sein)
     * @param bezeichnung   optionaler Rundenname (darf null sein)
     * @param port          Webserver-Port für die Timer-Anzeige
     */
    public synchronized void starten(long dauerSekunden, String bezeichnung, int port) {
        if (dauerSekunden <= 0) {
            logger.warn("starten() mit ungültiger Dauer aufgerufen: {}", dauerSekunden);
            return;
        }
        stoppeTickTask();
        this.bezeichnung = bezeichnung != null ? bezeichnung : "";
        this.letzterPort = port;
        startWebServerWennNoetig(port);

        endNanos = System.nanoTime() + dauerSekunden * 1_000_000_000L;
        zustand = TimerZustand.LAEUFT;
        tickTask = executor.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);

        logger.info("Timer gestartet: {} Sekunden, Port {}", dauerSekunden, port);
    }

    /** Wechselt zwischen Pause und Fortsetzen. Ohne Wirkung wenn Timer inaktiv oder beendet. */
    public synchronized void pauseOderFortsetzen() {
        switch (zustand) {
            case LAEUFT -> {
                restNanos = endNanos - System.nanoTime();
                if (restNanos < 0) restNanos = 0;
                stoppeTickTask();
                zustand = TimerZustand.PAUSIERT;
                emittiere(aktuellerState());
                logger.debug("Timer pausiert, restNanos={}", restNanos);
            }
            case PAUSIERT -> {
                endNanos = System.nanoTime() + restNanos;
                zustand = TimerZustand.LAEUFT;
                tickTask = executor.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
                logger.debug("Timer fortgesetzt");
            }
            default -> logger.debug("pauseOderFortsetzen() im Zustand {} ignoriert", zustand);
        }
    }

    /**
     * Passt die verbleibende Zeit an. Funktioniert während LAEUFT und PAUSIERT.
     *
     * @param deltaSekunden positive oder negative Anzahl Sekunden (z.B. +60 oder -60)
     */
    public synchronized void zeitAnpassen(long deltaSekunden) {
        if (zustand == TimerZustand.INAKTIV || zustand == TimerZustand.BEENDET) {
            return;
        }
        if (zustand == TimerZustand.LAEUFT) {
            endNanos = Math.max(System.nanoTime(), endNanos + deltaSekunden * 1_000_000_000L);
        } else {
            restNanos = Math.max(0, restNanos + deltaSekunden * 1_000_000_000L);
        }
        emittiere(aktuellerState());
        logger.debug("Zeit angepasst um {} s, neuer Zustand: {}", deltaSekunden, aktuellerState().anzeige());
    }

    /** Stoppt den Timer und setzt den Zustand auf INAKTIV zurück. */
    public synchronized void stoppen() {
        stoppeTickTask();
        zustand = TimerZustand.INAKTIV;
        bezeichnung = "";
        emittiere(TimerState.inaktiv());
        logger.debug("Timer gestoppt");
    }

    /** Liefert den aktuellen {@link TimerZustand}. */
    public TimerZustand getZustand() {
        return zustand;
    }

    // ── Interner Tick ──────────────────────────────────────────────────────────

    private void tick() {
        long verbleibend;
        TimerZustand neuerZustand;

        synchronized (this) {
            verbleibend = (endNanos - System.nanoTime()) / 1_000_000_000L;
            if (verbleibend <= 0) {
                verbleibend = 0;
                neuerZustand = TimerZustand.BEENDET;
                stoppeTickTask();
                zustand = TimerZustand.BEENDET;
            } else {
                neuerZustand = TimerZustand.LAEUFT;
            }
        }

        emittiere(new TimerState(formatiere(verbleibend), verbleibend, neuerZustand, bezeichnung));
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private void startWebServerWennNoetig(int port) {
        if (webServerInstanz != null && webServerInstanz.getPort() == port && webServerInstanz.laeuft()) {
            return;
        }
        if (webServerInstanz != null) {
            webServerInstanz.stoppen();
        }
        try {
            webServerInstanz = new TimerWebServerInstanz(port);
            webServerInstanz.starten();
            addListener(webServerInstanz);
            logger.info("Timer-Webserver gestartet auf Port {}", port);
        } catch (IOException e) {
            logger.error("Timer-Webserver konnte nicht gestartet werden auf Port {}: {}", port, e.getMessage(), e);
            webServerInstanz = null;
            MessageBox.from(xContext, MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("timer.dialog.titel"))
                    .message(I18n.get("timer.fehler.webserver.port.belegt", port))
                    .show();
        }
    }

    private void stoppeTickTask() {
        var task = tickTask;
        if (task != null) {
            task.cancel(false);
            tickTask = null;
        }
    }

    private void emittiere(TimerState state) {
        listeners.forEach(l -> {
            try {
                l.onChange(state);
            } catch (Exception e) {
                logger.error("Fehler in TimerListener", e);
            }
        });
    }

    private TimerState aktuellerState() {
        long verbleibend = zustand == TimerZustand.PAUSIERT
                ? restNanos / 1_000_000_000L
                : Math.max(0, (endNanos - System.nanoTime()) / 1_000_000_000L);
        return new TimerState(formatiere(verbleibend), verbleibend, zustand, bezeichnung);
    }

    /**
     * Formatiert Sekunden als "MM:SS".
     *
     * @param sekunden nicht-negativer Sekundenwert
     * @return z.B. "04:32"
     */
    public static String formatiere(long sekunden) {
        long s = Math.max(0, sekunden);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    /**
     * Parst einen "MM:SS"-String in Sekunden.
     * Gültiges Format: {@code ^\d{1,2}:[0-5]\d$}, Gesamtwert &gt; 0.
     *
     * @param mmss Eingabe-String
     * @return Sekunden als long
     * @throws IllegalArgumentException wenn das Format ungültig ist
     */
    public static long parseDauer(String mmss) {
        if (mmss == null || !mmss.matches("^\\d{1,2}:[0-5]\\d$")) {
            throw new IllegalArgumentException("Ungültiges Format: " + mmss);
        }
        var teile = mmss.split(":");
        long minuten = Long.parseLong(teile[0]);
        long sekunden = Long.parseLong(teile[1]);
        long gesamt = minuten * 60 + sekunden;
        if (gesamt <= 0) {
            throw new IllegalArgumentException("Dauer muss größer als 0 sein");
        }
        return gesamt;
    }
}
