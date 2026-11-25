/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.timer.TimerState;
import de.petanqueturniermanager.timer.TimerSteuerung;
import de.petanqueturniermanager.timer.TimerSteuerungListener;

/**
 * Gibt die Bedienung des Rundenzeit-Timers an das PTM-Online-Turnier des Dokuments weiter, aus dem der Timer gestartet
 * wurde: Start, Pause, Fortsetzen, Zeitkorrektur und Stopp. Turnier- und Live-Ansicht zeigen den Timer dann wie einen
 * online gestarteten. Gesendet wird der absolute Stand; PTM-Online rechnet die Endzeit ab Eingang, eine
 * Versandverzögerung gleicht {@link PtmOnlineAuftraege#timerNachstellen} aus.
 * <p>
 * Die Bedienung merkt den Stand je Dokument nur vor und stößt den {@link PtmOnlineLiveBeobachter} an. Dessen
 * {@link PtmOnlineErfassungsRunner} erfasst ihn als Auftrag – gespeichert in den Dokument-Properties
 * ({@code PtmOnlineSyncSpeicher}) – und der Hintergrund sendet ihn. Ohne Netz bleibt der Auftrag offen und
 * geht mit dem nächsten Versand hinaus; ein neuerer Stand ersetzt ihn. Der Timer selbst läuft immer lokal.
 */
public final class PtmOnlineTimer implements TimerSteuerungListener {

    /** Bedienter Timer-Stand eines Dokuments, der noch als Auftrag zu erfassen ist. */
    record Vormerkung(String body, Instant stand) {}

    private static final Map<String, Vormerkung> VORMERKUNGEN = new ConcurrentHashMap<>();

    /** Das Anstoßen liest das Dokument; das gehört nicht auf den Thread der Bedienung. */
    private final ExecutorService anstoss = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "PTM-Online-Timer");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void onSteuerung(TimerState state, TimerSteuerung steuerung) {
        Optional<WorkingSpreadsheet> ursprung = steuerung.ursprung();
        if (ursprung.isEmpty()) {
            return;
        }
        XSpreadsheetDocument xDoc = ursprung.get().getWorkingSpreadsheetDocument();
        VORMERKUNGEN.put(schluessel(xDoc),
                new Vormerkung(TournamentSyncClient.timerBody(state, steuerung.dauerSekunden()), Instant.now()));
        anstoss.execute(() -> PtmOnlineLiveBeobachter.anstossen(xDoc));
    }

    /** Der zuletzt bediente, noch nicht erfasste Stand des Dokuments. */
    static Optional<Vormerkung> vorgemerkt(XSpreadsheetDocument xDoc) {
        return Optional.ofNullable(VORMERKUNGEN.get(schluessel(xDoc)));
    }

    /** Der Stand ist als Auftrag gespeichert; ein inzwischen neuer bedienter Stand bleibt vorgemerkt. */
    static void erfasst(XSpreadsheetDocument xDoc, Vormerkung vormerkung) {
        VORMERKUNGEN.remove(schluessel(xDoc), vormerkung);
    }

    /** Vergisst den Stand eines geschlossenen Dokuments. */
    static void vergessen(XSpreadsheetDocument xDoc) {
        VORMERKUNGEN.remove(schluessel(xDoc));
    }

    private static String schluessel(XSpreadsheetDocument xDoc) {
        return UnoRuntime.generateOid(xDoc);
    }
}
