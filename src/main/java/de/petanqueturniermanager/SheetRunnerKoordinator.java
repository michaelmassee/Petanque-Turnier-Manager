/*
 * Erstellung : 14.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    private static final long WARTE_INTERVALL_MS = 50;

    private final AtomicBoolean laeuft = new AtomicBoolean();
    /** Ob der aktive Lauf ein stiller Hintergrundlauf ist ({@link SheetRunner#startSilent()}). */
    private volatile boolean stillBelegt;
    /** Anzahl vom Anwender gestarteter Läufe, die auf das Ende eines stillen Hintergrundlaufs warten. */
    private final AtomicInteger wartendeVordergrundLaeufe = new AtomicInteger();
    /** Zählt jeden begonnenen Lauf; erkennt Läufe, die während eines Lesevorgangs begonnen und geendet haben. */
    private final AtomicLong laufZaehler = new AtomicLong();
    private volatile SheetRunner aktuellerRunner = null;
    private volatile Thread ausfuehrenderThread = null;
    private final List<Runnable> zustandsListener =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * Wird gesetzt wenn der Runner {@code setActiveSheet()} aufruft, damit der
     * {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener}
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
     * Momentaufnahme für konsistentes Lesen außerhalb eines Laufs (siehe {@link #unveraendertSeit}).
     *
     * @return leer, solange ein Lauf aktiv ist
     */
    OptionalLong ruhenderLaufStand() {
        long stand = laufZaehler.get();
        return laeuft.get() ? OptionalLong.empty() : OptionalLong.of(stand);
    }

    /**
     * Ob seit {@code stand} (aus {@link #ruhenderLaufStand()}) kein Lauf begonnen hat und gerade keiner aktiv ist
     * – dann hat während des dazwischen liegenden Lesens kein Runner geschrieben.
     */
    boolean unveraendertSeit(long stand) {
        return !laeuft.get() && laufZaehler.get() == stand;
    }

    /** Klassenname des aktiven Runners für Meldungen, leer wenn keiner läuft. */
    String getRunnerName() {
        SheetRunner snapshot = aktuellerRunner;
        return snapshot == null ? "" : snapshot.getClass().getSimpleName();
    }

    /**
     * Ob {@code thread} gerade einen SheetRunner-Lauf ausführt – der Runner-Thread selbst oder ein Thread,
     * der {@link SheetRunner#run()} direkt aufgerufen hat.
     */
    boolean fuehrtLaufAus(Thread thread) {
        return thread != null && thread == ausfuehrenderThread;
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
        boolean vorher = laeuft.getAndSet(neuerWert);
        zaehleLaufbeginn(vorher, neuerWert);
        return vorher;
    }

    /**
     * Setzt den aktuell aktiven Runner (oder {@code null} nach Abschluss). Wird vom ausführenden Thread
     * aufgerufen und merkt sich diesen für {@link #fuehrtLaufAus(Thread)}.
     */
    void setRunner(SheetRunner runner) {
        this.aktuellerRunner = runner;
        this.ausfuehrenderThread = runner == null ? null : Thread.currentThread();
    }

    /** Setzt das Lauf-Flag direkt. */
    void setLaeuft(boolean wert) {
        if (!wert) {
            stillBelegt = false;
        }
        zaehleLaufbeginn(laeuft.getAndSet(wert), wert);
    }

    /**
     * Belegt den Koordinator für einen Lauf, falls keiner aktiv ist.
     *
     * @param still {@code true} für einen stillen Hintergrundlauf
     * @return {@code true}, wenn der Aufrufer jetzt laufen darf
     */
    boolean belege(boolean still) {
        if (still && wartendeVordergrundLaeufe.get() > 0) {
            return false; // ein wartender Anwender-Lauf hat Vorrang
        }
        if (getAndSetLaeuft(true)) {
            return false;
        }
        stillBelegt = still;
        return true;
    }

    /** Ob gerade ein stiller Hintergrundlauf aktiv ist, auf dessen Ende ein Anwender-Lauf warten darf. */
    boolean istStillBelegt() {
        return laeuft.get() && stillBelegt;
    }

    /**
     * Wartet, bis der aktive stille Hintergrundlauf endet, und belegt dann den Koordinator für einen Anwender-Lauf.
     * Neue stille Läufe starten währenddessen nicht. Darf nicht auf dem LO-Main-Thread laufen: Der Hintergrundlauf
     * braucht die SolarMutex, die der Main-Thread hält.
     *
     * @return {@code false}, wenn stattdessen ein anderer Anwender-Lauf aktiv ist oder die Wartezeit abläuft
     */
    boolean warteUndBelege(long maxWartezeitMs) throws InterruptedException {
        wartendeVordergrundLaeufe.incrementAndGet();
        try {
            long ende = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(maxWartezeitMs);
            while (System.nanoTime() < ende) {
                if (belege(false)) {
                    return true;
                }
                if (!istStillBelegt()) {
                    return false;
                }
                TimeUnit.MILLISECONDS.sleep(WARTE_INTERVALL_MS);
            }
            return false;
        } finally {
            wartendeVordergrundLaeufe.decrementAndGet();
        }
    }

    /** Erst nach dem Setzen des Flags zählen: ein Leser sieht so entweder das Flag oder den neuen Zählerstand. */
    private void zaehleLaufbeginn(boolean vorher, boolean neuerWert) {
        if (!vorher && neuerWert) {
            laufZaehler.incrementAndGet();
        }
    }

    /**
     * Signalisiert, dass das nächste {@code selectionChanged}-Ereignis, das auf die
     * Rangliste zeigt, vom {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener}
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
        stillBelegt = false;
        wartendeVordergrundLaeufe.set(0);
        aktuellerRunner = null;
        zustandsListener.clear();
        selectionChangeUnterdrücken.set(false);
    }
}
