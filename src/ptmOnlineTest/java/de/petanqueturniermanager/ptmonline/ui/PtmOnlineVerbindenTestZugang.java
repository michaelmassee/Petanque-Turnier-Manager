/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;

/** Verbindet wie der Menüpunkt „Verbinden“ – mit dem echten Runner, nur ohne Turnierauswahl-Dialog. */
public final class PtmOnlineVerbindenTestZugang {

    private PtmOnlineVerbindenTestZugang() {
    }

    /** Startet den Runner und wartet auf sein Ende (siehe Basisklasse der Zusammenspiel-Tests). */
    @FunctionalInterface
    public interface Starter {
        void accept(SheetRunner runner) throws Exception;
    }

    /** @return {@code true}, wenn der Runner ohne Fehler durchgelaufen ist */
    public static boolean verbinden(WorkingSpreadsheet ws, TurnierSystem turnierSystem, Integer spieltagNrOderNull,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten zugang, OnlineTournamentDto turnier, Starter starter)
            throws Exception {
        PtmOnlineVerbindenRunner runner = new PtmOnlineVerbindenRunner(ws, turnierSystem, spieltagNrOderNull, zugang,
                turnier);
        starter.accept(runner);
        return !runner.isLetzterLaufFehlgeschlagen();
    }
}
