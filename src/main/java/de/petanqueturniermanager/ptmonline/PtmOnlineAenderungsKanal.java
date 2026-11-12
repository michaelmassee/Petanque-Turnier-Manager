/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Sofort-Signal von PTM-Online an ein verbundenes Turnierdokument: ein WebSocket auf den Hub des Turniers
 * ({@code GET /api/sync/tournaments/:id/socket}). Über die Verbindung kommen keine Turnierdaten, nur
 * {@code {"type":"changed","source":"document"|"online"}}. Änderungen aus PTM-Online ({@code online}) und jeder
 * Zustandswechsel der Verbindung lösen {@code abrufAnstossen} aus; das Echo eigener Schreibaufträge
 * ({@code document}) wird übersprungen.
 * <p>
 * Ausgelöst wird der Abruf:
 * <ul>
 * <li>bei einer Änderung aus PTM-Online,</li>
 * <li>nach dem (Wieder-)Verbinden – zwischendurch kann eine Änderung verpasst worden sein,</li>
 * <li>nach dem Verbindungsverlust – der Aufrufer fällt damit auf den kurzen regelmäßigen Abruf zurück.</li>
 * </ul>
 * Nach einem Abbruch wird mit wachsendem Abstand neu verbunden. Dauerhaft aufgegeben wird bei Ablehnung des Zugangs
 * oder der Dokumentbindung (401, 403, 409, 410) und bei zu vielen Verbindungen (1013); ein alter Server ohne den
 * Endpunkt (404) wird nur selten erneut versucht.
 * <p>
 * Threading: Die Listener-Methoden laufen auf Threads des {@link java.net.http.HttpClient}, Ping und Neuverbinden auf
 * der übergebenen Ausführung. Der Kanal fasst weder Dokument noch Oberfläche an; {@code abrufAnstossen} darf das
 * ebenfalls nicht und wird nie unter der Sperre des Kanals aufgerufen.
 */
final class PtmOnlineAenderungsKanal {

    private static final Logger logger = LogManager.getLogger(PtmOnlineAenderungsKanal.class);

    /**
     * Keepalive gegen das Kappen ruhender Verbindungen (NAT, Proxy, Cloudflare); die Plattform beantwortet es
     * selbst, ohne den Hub aufzuwecken. Gleicher Abstand wie bei den Webseiten ({@code LIVE_SOCKET_PING_MS}).
     */
    static final long PING_ABSTAND_MS = 50_000;
    static final long ERSTE_WIEDERHOLUNG_MS = 2_000;
    static final long MAX_WIEDERHOLUNG_MS = 60_000;
    /** Server ohne Sofort-Signal (ältere PTM-Online-Version): nur selten erneut versuchen. */
    static final long ALTER_SERVER_WIEDERHOLUNG_MS = 600_000;

    static final String PING = "ping";
    static final int ZU_VIELE_VERBINDUNGEN = 1013;
    private static final int NICHT_GEFUNDEN = 404;
    private static final Set<Integer> ZUGANG_ABGELEHNT = Set.of(401, 403, 409, 410);
    private static final String TYP_GEAENDERT = "changed";
    private static final String QUELLE_DOKUMENT = "document";

    /** Baut die Verbindung auf (wartet nicht). */
    @FunctionalInterface
    interface Verbinder {
        CompletableFuture<WebSocket> verbinde(WebSocket.Listener listener);
    }

    private final String turnierId;
    private final ScheduledExecutorService ausfuehrung;
    private final Verbinder verbinder;
    private final Runnable abrufAnstossen;

    // Alle folgenden Felder nur unter der Sperre dieses Objekts.
    private boolean gestartet;
    private boolean gestoppt;
    private boolean aufgegeben;
    /** Zählt die Verbindungsversuche; Rückmeldungen älterer Versuche werden verworfen. */
    private int versuch;
    private @Nullable WebSocket socket;
    private @Nullable ScheduledFuture<?> ping;
    private @Nullable ScheduledFuture<?> wiederholung;
    private long naechsteWiederholungMs = ERSTE_WIEDERHOLUNG_MS;

    PtmOnlineAenderungsKanal(String turnierId, ScheduledExecutorService ausfuehrung, Verbinder verbinder,
            Runnable abrufAnstossen) {
        this.turnierId = turnierId;
        this.ausfuehrung = ausfuehrung;
        this.verbinder = verbinder;
        this.abrufAnstossen = abrufAnstossen;
    }

    /**
     * Ob eine Änderung aus PTM-Online gemeldet wird (nicht das Echo eigener Schreibaufträge). Unbekannte Nachrichten,
     * auch die Antwort {@code pong} auf das Keepalive, zählen nicht.
     */
    static boolean istAenderungAusPtmOnline(String nachricht) {
        try {
            JsonElement element = JsonParser.parseString(nachricht);
            if (!element.isJsonObject()) {
                return false;
            }
            JsonObject objekt = element.getAsJsonObject();
            return TYP_GEAENDERT.equals(text(objekt, "type")) && !QUELLE_DOKUMENT.equals(text(objekt, "source"));
        } catch (JsonParseException e) {
            logger.trace("PTM-Online-Sofort-Signal: Nachricht ignoriert: {}", nachricht, e);
            return false;
        }
    }

    private static @Nullable String text(JsonObject objekt, String feld) {
        JsonElement wert = objekt.get(feld);
        return wert != null && wert.isJsonPrimitive() ? wert.getAsString() : null;
    }

    boolean gehoertZu(String andereTurnierId) {
        return turnierId.equals(andereTurnierId);
    }

    synchronized boolean istVerbunden() {
        return socket != null;
    }

    synchronized boolean istAufgegeben() {
        return aufgegeben;
    }

    /** Verbindet einmalig; weitere Aufrufe ändern nichts. */
    void starten() {
        int dieserVersuch;
        synchronized (this) {
            if (gestartet || gestoppt) {
                return;
            }
            gestartet = true;
            dieserVersuch = ++versuch;
        }
        verbinden(dieserVersuch);
    }

    /** Trennt endgültig; danach wird nicht mehr neu verbunden. */
    void stoppen() {
        WebSocket offen;
        synchronized (this) {
            gestoppt = true;
            abbrechen(ping);
            abbrechen(wiederholung);
            offen = socket;
            socket = null;
        }
        if (offen != null) {
            schliessen(offen);
        }
    }

    /**
     * Startet einen Verbindungsversuch. Nie unter der Sperre aufrufen: Ist der Aufbau schon abgeschlossen, meldet
     * die Future sofort im aufrufenden Thread zurück und kann den Abruf anstoßen.
     */
    private void verbinden(int dieserVersuch) {
        CompletableFuture<WebSocket> aufbau;
        try {
            aufbau = verbinder.verbinde(new Empfaenger(dieserVersuch));
        } catch (RuntimeException e) {
            logger.warn("PTM-Online-Sofort-Signal für {} nicht möglich, es bleibt beim regelmäßigen Abruf", turnierId, e);
            synchronized (this) {
                aufgegeben = true;
            }
            return;
        }
        aufbau.whenComplete((neuerSocket, fehler) -> aufgebaut(dieserVersuch, neuerSocket, fehler));
    }

    private void aufgebaut(int dieserVersuch, @Nullable WebSocket neuerSocket, @Nullable Throwable fehler) {
        synchronized (this) {
            if (dieserVersuch != versuch || gestoppt) {
                if (neuerSocket != null) {
                    neuerSocket.abort();
                }
                return;
            }
            if (fehler != null || neuerSocket == null) {
                behandleFehler(fehler);
                return;
            }
            if (!pingEinplanen()) {
                gestoppt = true;
                neuerSocket.abort();
                return;
            }
            socket = neuerSocket;
            naechsteWiederholungMs = ERSTE_WIEDERHOLUNG_MS;
            logger.debug("PTM-Online-Sofort-Signal für {} verbunden", turnierId);
        }
        abrufAnstossen.run();
    }

    private boolean pingEinplanen() {
        try {
            ping = ausfuehrung.scheduleAtFixedRate(this::pinge, PING_ABSTAND_MS, PING_ABSTAND_MS, TimeUnit.MILLISECONDS);
            return true;
        } catch (RejectedExecutionException e) {
            logger.debug("PTM-Online-Sofort-Signal: Ausführung beendet, Verbindung wird verworfen", e);
            return false;
        }
    }

    private void behandleFehler(@Nullable Throwable fehler) {
        Throwable ursache = ursache(fehler);
        if (ursache instanceof WebSocketHandshakeException handshake) {
            int status = handshake.getResponse().statusCode();
            if (status == NICHT_GEFUNDEN) {
                logger.info("PTM-Online kennt das Sofort-Signal noch nicht, es bleibt beim regelmäßigen Abruf");
                planeWiederholung(ALTER_SERVER_WIEDERHOLUNG_MS);
                return;
            }
            if (ZUGANG_ABGELEHNT.contains(status)) {
                logger.info("PTM-Online lehnt das Sofort-Signal für {} ab (HTTP {}), es bleibt beim regelmäßigen Abruf",
                        turnierId, status);
                aufgegeben = true;
                return;
            }
        }
        logger.debug("PTM-Online-Sofort-Signal für {} nicht verbunden, neuer Versuch in {} ms", turnierId,
                naechsteWiederholungMs, ursache);
        planeWiederholungMitAbstand();
    }

    private static @Nullable Throwable ursache(@Nullable Throwable fehler) {
        Throwable ursache = fehler;
        while ((ursache instanceof CompletionException || ursache instanceof ExecutionException)
                && ursache.getCause() != null) {
            ursache = ursache.getCause();
        }
        return ursache;
    }

    private void getrennt(int dieserVersuch, int code, @Nullable Throwable fehler) {
        synchronized (this) {
            if (dieserVersuch != versuch || gestoppt || socket == null) {
                return;
            }
            socket = null;
            abbrechen(ping);
            if (code == ZU_VIELE_VERBINDUNGEN) {
                logger.info("PTM-Online: zu viele Verbindungen zu {}, es bleibt beim regelmäßigen Abruf", turnierId);
                aufgegeben = true;
            } else {
                logger.debug("PTM-Online-Sofort-Signal für {} getrennt (Code {}), neuer Versuch in {} ms", turnierId,
                        code, naechsteWiederholungMs, fehler);
                planeWiederholungMitAbstand();
            }
        }
        abrufAnstossen.run();
    }

    private void planeWiederholungMitAbstand() {
        long abstand = naechsteWiederholungMs;
        naechsteWiederholungMs = Math.min(naechsteWiederholungMs * 2, MAX_WIEDERHOLUNG_MS);
        planeWiederholung(abstand);
    }

    private void planeWiederholung(long abstandMs) {
        if (gestoppt || aufgegeben) {
            return;
        }
        try {
            wiederholung = ausfuehrung.schedule(this::wiederverbinden, abstandMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            logger.debug("PTM-Online-Sofort-Signal: Ausführung beendet, kein neuer Versuch", e);
            gestoppt = true;
        }
    }

    private void wiederverbinden() {
        int dieserVersuch;
        synchronized (this) {
            if (gestoppt || aufgegeben || socket != null) {
                return;
            }
            dieserVersuch = ++versuch;
        }
        verbinden(dieserVersuch);
    }

    private void pinge() {
        WebSocket offen;
        synchronized (this) {
            offen = socket;
        }
        if (offen != null) {
            offen.sendText(PING, true).exceptionally(fehler -> {
                logger.debug("PTM-Online-Sofort-Signal: Keepalive für {} nicht gesendet", turnierId, fehler);
                return null;
            });
        }
    }

    private synchronized boolean istAktuell(int dieserVersuch) {
        return dieserVersuch == versuch && !gestoppt;
    }

    private void schliessen(WebSocket offen) {
        offen.sendClose(WebSocket.NORMAL_CLOSURE, "").whenComplete((ws, fehler) -> {
            if (fehler != null) {
                logger.trace("PTM-Online-Sofort-Signal: Schließen fehlgeschlagen, Verbindung wird abgebrochen", fehler);
                offen.abort();
            }
        });
    }

    private static void abbrechen(@Nullable ScheduledFuture<?> geplant) {
        if (geplant != null) {
            geplant.cancel(false);
        }
    }

    /** Empfänger eines Verbindungsversuchs; nach einem neueren Versuch werden seine Meldungen ignoriert. */
    private final class Empfaenger implements WebSocket.Listener {

        private final int dieserVersuch;
        private final StringBuilder teile = new StringBuilder();

        Empfaenger(int dieserVersuch) {
            this.dieserVersuch = dieserVersuch;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public @Nullable CompletionStage<?> onText(WebSocket webSocket, CharSequence daten, boolean letzterTeil) {
            teile.append(daten);
            if (letzterTeil) {
                String nachricht = teile.toString();
                teile.setLength(0);
                if (istAktuell(dieserVersuch) && istAenderungAusPtmOnline(nachricht)) {
                    abrufAnstossen.run();
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public @Nullable CompletionStage<?> onClose(WebSocket webSocket, int code, String grund) {
            getrennt(dieserVersuch, code, null);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable fehler) {
            getrennt(dieserVersuch, -1, fehler);
        }
    }
}
