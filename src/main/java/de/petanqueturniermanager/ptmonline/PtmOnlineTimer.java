/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.DokumentKontext;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.timer.TimerState;
import de.petanqueturniermanager.timer.TimerSteuerung;
import de.petanqueturniermanager.timer.TimerSteuerungListener;

/**
 * Gibt die Bedienung des Rundenzeit-Timers an das PTM-Online-Turnier des Dokuments weiter, aus dem der Timer gestartet
 * wurde: Start, Pause, Fortsetzen, Zeitkorrektur und Stopp. Turnier- und Live-Ansicht zeigen den Timer dann wie einen
 * online gestarteten. Gesendet wird der absolute Stand; PTM-Online rechnet die Endzeit ab Eingang.
 * <p>
 * Der Versand läuft auf einem eigenen Hintergrund-Thread, nie auf dem LO-Main-Thread der Bedienung und unabhängig von
 * einem gerade laufenden {@code SheetRunner}. Ohne Netz bleibt der Auftrag offen und geht mit dem nächsten Versand
 * hinaus; ein neuerer Stand ersetzt ihn. Fehler werden nur protokolliert – der Timer selbst läuft immer lokal.
 */
public final class PtmOnlineTimer implements TimerSteuerungListener {

    private static final Logger logger = LogManager.getLogger(PtmOnlineTimer.class);

    private final ExecutorService versand = Executors.newSingleThreadExecutor(r -> {
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
        String body = TournamentSyncClient.timerBody(state, steuerung.dauerSekunden());
        versand.execute(() -> sendeImKontext(ursprung.get(), body));
    }

    private static void sendeImKontext(WorkingSpreadsheet ws, String body) {
        try {
            DokumentKontext.mitKontextWerfend(ws.getWorkingSpreadsheetDocument(), () -> sende(ws, body));
        } catch (GenerateException | RuntimeException e) {
            logger.warn("PTM-Online: Timer nicht übertragen", e);
        }
    }

    private static void sende(WorkingSpreadsheet ws, String body) throws GenerateException {
        TurnierSystem ts = DocumentPropertiesHelper
                .nurLesend(() -> new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument());
        if (!PtmOnlineVerbindung.hatVerbindung(ws, ts)) {
            return;
        }
        Optional<PtmOnlineVerbindung> verbindung = PtmOnlineVerbindung.ermitteln(ws, ts);
        if (verbindung.isEmpty()) {
            return;
        }
        PtmOnlineRegistrationMapping mapping = verbindung.get().mapping();
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(ws, mapping, verbindung.get().spieltagNr());
        boolean pausiert = mapping.istPausiert();
        PtmOnlineAuftraege.timer(bestand, verbindung.get().tournamentId(), body, pausiert);
        try {
            PtmOnlineAuftraege.SynchronerVersand ergebnis = PtmOnlineAuftraege.sendeSynchron(bestand, mapping,
                    verbindung.get().gebundenerClient(), pausiert);
            logger.info("PTM-Online: Timer übertragen ({})", ergebnis.stopp());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("PTM-Online: Timer-Versand unterbrochen", e);
        }
    }
}
