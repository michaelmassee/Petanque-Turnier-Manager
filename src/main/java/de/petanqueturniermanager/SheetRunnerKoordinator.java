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
import de.petanqueturniermanager.helper.i18n.I18n;

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

    /**
     * Wird gesetzt wenn der Runner {@code setActiveSheet()} aufruft, damit der
     * {@link de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener}
     * das dadurch ausgelöste {@code selectionChanged}-Ereignis ignoriert.
     * Asynchrone Ereignisse aus LibreOffice können sonst nach {@code setLaeuft(false)}
     * ankommen und einen unerwünschten zweiten Neuaufbau starten.
     */
    private final AtomicBoolean selectionChangeUnterdrücken = new AtomicBoolean(false);

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
            throw new GenerateException(SheetRunner.VERARBEITUNG_ABGEBROCHEN);
        }
    }

    /** Registriert einen Listener, der bei Zustandsänderungen benachrichtigt wird. */
    void addZustandsListener(Runnable listener) {
        zustandsListener.add(listener);
    }

    /** Entfernt einen zuvor registrierten Zustandslistener. */
    void removeZustandsListener(Runnable listener) {
        zustandsListener.remove(listener);
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

    /** Gibt den aktuell aktiven Runner zurück, oder {@code null} wenn keiner läuft. */
    SheetRunner getRunner() {
        return aktuellerRunner;
    }

    /** Setzt das Lauf-Flag direkt. */
    void setLaeuft(boolean wert) {
        laeuft.set(wert);
    }

    /**
     * Signalisiert, dass das nächste {@code selectionChanged}-Ereignis, das auf die
     * Rangliste zeigt, vom {@link de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener}
     * ignoriert werden soll. Wird gesetzt bevor der Runner {@code setActiveSheet()} aufruft.
     */
    void unterdrückeNaechstesSelectionChange() {
        selectionChangeUnterdrücken.set(true);
    }

    /**
     * Liest und löscht das Unterdrückungs-Flag atomar.
     *
     * @return {@code true} wenn das nächste selectionChanged ignoriert werden soll
     */
    boolean consumeSelectionChangeSuppression() {
        return selectionChangeUnterdrücken.getAndSet(false);
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
        selectionChangeUnterdrücken.set(false);
    }
}
