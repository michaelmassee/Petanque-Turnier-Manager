package de.petanqueturniermanager.supermelee.spieltagrangliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Generiert ein vollständiges Supermelee-Beispielturnier:
 * Meldeliste + 5 komplette Spieltage mit Spielrunden und Spieltag-Ranglisten.
 * <p>
 * Wrapper für {@link SpieltagRanglisteSheet_TestDaten} mit öffentlicher
 * {@code generate()}-Methode für den direkten Aufruf in Tests.
 */
public class SupermeleeTurnierTestDaten extends SpieltagRanglisteSheet_TestDaten {

    public SupermeleeTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    /**
     * Öffentlicher Einstiegspunkt für Tests: generiert das vollständige Supermelee-Turnier
     * ohne Dialoge direkt auf dem aktuellen Dokument.
     */
    public void generate() throws GenerateException {
        doRun();
    }
}
