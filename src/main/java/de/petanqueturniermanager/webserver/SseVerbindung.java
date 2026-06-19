package de.petanqueturniermanager.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Verwaltet eine einzelne offene Server-Sent-Events-Verbindung zu einem Browser-Client.
 * <p>
 * Sendet bei Verbindungsaufbau sofort den gecachten Init-State aus der Eltern-Instanz,
 * damit Browser nach einem Reconnect sofort den vollständigen Tabellenzustand erhalten.
 * <p>
 * Entfernt sich selbst aus der Eltern-Instanz, wenn die Verbindung unterbrochen wird.
 */
public class SseVerbindung {

    private static final Logger logger = LogManager.getLogger(SseVerbindung.class);

    private final OutputStream outputStream;
    private SseElternInstanz elternInstanz;
    private final String rolle;
    private final String clientId;

    public SseVerbindung(OutputStream outputStream, SseElternInstanz elternInstanz) {
        this(outputStream, elternInstanz, "", "");
    }

    public SseVerbindung(OutputStream outputStream, SseElternInstanz elternInstanz, String rolle, String clientId) {
        this.outputStream = outputStream;
        this.elternInstanz = elternInstanz;
        this.rolle = rolle == null ? "" : rolle;
        this.clientId = clientId == null ? "" : clientId;
    }

    public String getRolle() {
        return rolle;
    }

    public String getClientId() {
        return clientId;
    }

    /**
     * Bindet diese Verbindung an eine neue Eltern-Instanz (Quelle) um, ohne den
     * zugrunde liegenden {@link OutputStream} zu schließen.
     * <p>
     * Wird beim Wechsel der zugeordneten Regie-Quelle (Umschalten der View in der
     * Sidebar-Kombobox) benötigt: Der offene Browser-Stream bleibt bestehen, künftige
     * Updates und Entfern-Callbacks gehen an die neue Quelle. Direkt im Anschluss sollte
     * {@link #sendeInitNachricht()} aufgerufen werden, damit der Browser den vollständigen
     * Zustand der neuen Quelle erhält und die View ohne Reconnect austauscht.
     *
     * @param neueEltern die neue Eltern-Instanz, an die diese Verbindung künftig gebunden ist
     */
    void neuBinden(SseElternInstanz neueEltern) {
        this.elternInstanz = neueEltern;
    }

    /**
     * Sendet den aktuellen Init-State an den neu verbundenen Client.
     * Muss direkt nach dem Verbindungsaufbau aufgerufen werden, damit der Browser
     * sofort den vollständigen Tabellenzustand erhält – auch nach einem Reconnect.
     */
    public void sendeInitNachricht() {
        String initJson = elternInstanz.getCachedInitJson();
        if (initJson != null) {
            senden(initJson);
        }
        for (String zusatzJson : elternInstanz.getInitZusatzJsons()) {
            if (zusatzJson != null) {
                senden(zusatzJson);
            }
        }
    }

    /**
     * Sendet ein SSE-Datenereignis an den Browser.
     * Bei Fehler wird die Verbindung aus der Eltern-Instanz entfernt.
     *
     * @param eventDaten JSON-Inhalt des {@code data:}-Feldes
     */
    public void senden(String eventDaten) {
        try {
            outputStream.write(("data: " + eventDaten + "\n\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException e) {
            logger.debug("SSE-Verbindung unterbrochen, entferne Client: {}", e.getMessage());
            elternInstanz.verbindungEntfernen(this);
        }
    }

    /**
     * Sendet einen SSE-Kommentar als Keep-Alive-Signal.
     * Verhindert, dass Browser/TV die stille Verbindung nach 30–60 s kappen.
     * Bei Fehler wird die Verbindung aus der Eltern-Instanz entfernt.
     */
    public void keepAlive() {
        try {
            outputStream.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException e) {
            logger.debug("SSE-Keep-Alive fehlgeschlagen, entferne Client: {}", e.getMessage());
            elternInstanz.verbindungEntfernen(this);
        }
    }

    /**
     * Schließt den zugrunde liegenden Output-Stream.
     */
    public void schliessen() {
        try {
            outputStream.close();
        } catch (IOException e) {
            logger.debug("Fehler beim Schließen des SSE-Streams: {}", e.getMessage());
        }
    }
}
