/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;

/** Verbindet wie der Menüpunkt „Verbinden“ – mit dem echten Runner, nur ohne Turnierauswahl-Dialog. */
public final class PtmOnlineVerbindenTestZugang {

    private PtmOnlineVerbindenTestZugang() {
    }

    /** @return {@code true}, wenn der Runner ohne Fehler durchgelaufen ist */
    public static boolean verbinden(WorkingSpreadsheet ws, TurnierSystem turnierSystem, Integer spieltagNrOderNull,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten zugang, OnlineTournamentDto turnier) throws InterruptedException {
        PtmOnlineVerbindenRunner runner = new PtmOnlineVerbindenRunner(ws, turnierSystem, spieltagNrOderNull, zugang,
                turnier);
        runner.start();
        runner.join();
        return !runner.isLetzterLaufFehlgeschlagen();
    }
}
