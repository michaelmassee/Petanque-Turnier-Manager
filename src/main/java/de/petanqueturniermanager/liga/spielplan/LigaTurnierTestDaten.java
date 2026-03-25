package de.petanqueturniermanager.liga.spielplan;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Generiert ein vollständiges Liga-Beispielturnier:
 * Meldeliste (gerade Teamanzahl, kein Freilos) + Spielplan + Rangliste.
 * <p>
 * Wrapper für {@link LigaSpielPlanSheetTestDaten} mit öffentlicher
 * {@code generate()}-Methode für den direkten Aufruf in Tests.
 */
public class LigaTurnierTestDaten extends LigaSpielPlanSheetTestDaten {

    public LigaTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, false); // gerade Teamanzahl, kein Freilos
    }

    /**
     * Öffentlicher Einstiegspunkt für Tests: generiert das vollständige Liga-Turnier
     * ohne Dialoge direkt auf dem aktuellen Dokument.
     */
    public void generate() throws GenerateException {
        doRun();
    }
}
