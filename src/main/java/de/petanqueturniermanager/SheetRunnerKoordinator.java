/**
 * Erstellung : 14.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Verwaltet den Laufzustand von {@link SheetRunner}-Threads.
 * <p>
 * Kapselt die bisher statischen Felder {@code isRunning}, {@code runner} und
 * {@code STATE_LISTENERS} aus {@link SheetRunner}. Dadurch bleibt
 * {@link SheetRunner} für seine eigentliche Aufgabe (Thread-Ausführung)
 * zuständig, während diese Klasse die Koordination übernimmt.
 */
class SheetRunnerKoordinator {

    private static final Logger logger = LogManager.getLogger(SheetRunnerKoordinator.class);

    private final AtomicBoolean laeuft = new AtomicBoolean();
    private volatile SheetRunner aktuellerRunner = null;
    private final List<Runnable> zustandsListener =
            Collections.synchronizedList(new ArrayList<>());

    /** Gibt zurück, ob aktuell ein {@link SheetRunner} aktiv ist. */
    boolean isRunning() {
        return laeuft.get();
    }

    /**
     * Unterbricht den aktuell laufenden {@link SheetRunner}, falls vorhanden.
     */
    void abbrechen() {
        SheetRunner snapshot = aktuellerRunner;
        if (snapshot != null) {
            snapshot.interrupt();
        }
    }

    /**
     * Wirft eine {@link GenerateException}, wenn der aktuelle Runner unterbrochen wurde.
     *
     * @throws GenerateException wenn der Thread als unterbrochen markiert ist
     */
    void abbrechenPruefen() throws GenerateException {
        SheetRunner snapshot = aktuellerRunner;
        if (snapshot != null && snapshot.isInterrupted()) {
            throw new GenerateException("Verarbeitung abgebrochen");
        }
    }

    /** Registriert einen Listener, der bei Zustandsänderungen benachrichtigt wird. */
    void addZustandsListener(Runnable listener) {
        zustandsListener.add(listener);
    }

    /**
     * Atomares getAndSet auf dem Lauf-Flag.
     *
     * @param neuerWert der zu setzende Wert
     * @return der vorherige Wert
     */
    boolean getAndSetLaeuft(boolean neuerWert) {
        return laeuft.getAndSet(neuerWert);
    }

    /** Setzt den aktuell aktiven Runner (oder {@code null} nach Abschluss). */
    void setRunner(SheetRunner runner) {
        this.aktuellerRunner = runner;
    }

    /** Setzt das Lauf-Flag direkt. */
    void setLaeuft(boolean wert) {
        laeuft.set(wert);
    }

    /** Benachrichtigt alle registrierten Zustandslistener. */
    void benachrichtigeListener() {
        for (Runnable r : new ArrayList<>(zustandsListener)) {
            try {
                r.run();
            } catch (Exception e) {
                logger.warn("ZustandsListener-Fehler: {}", e.getMessage());
            }
        }
    }

    /** Setzt den gesamten Zustand zurück – ausschließlich für Tests. */
    void zuruecksetzen() {
        laeuft.set(false);
        aktuellerRunner = null;
        zustandsListener.clear();
    }
}
