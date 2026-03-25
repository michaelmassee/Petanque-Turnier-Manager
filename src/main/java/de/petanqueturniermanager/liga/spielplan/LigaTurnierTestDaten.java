package de.petanqueturniermanager.liga.spielplan;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Generiert ein vollständiges Liga-Beispielturnier:
 * Meldeliste (gerade Teamanzahl, kein Freilos) + Spielplan + Rangliste.
 * <p>
 * Wrapper für {@link LigaSpielPlanSheetTestDaten} mit öffentlichem
 * Einstiegspunkt {@code generiere()} für den direkten Aufruf in Tests.
 * <p>
 * <strong>Wichtig:</strong> Der Einstiegspunkt heißt absichtlich {@code generiere()},
 * nicht {@code generate()} – damit wird {@link LigaSpielPlanSheetTestDaten#generate()}
 * nicht überschrieben, was zu einer Endlosrekursion führen würde
 * (doRun → this.generate() → doRun → …).
 */
public class LigaTurnierTestDaten extends LigaSpielPlanSheetTestDaten {

    public LigaTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, false); // gerade Teamanzahl, kein Freilos
    }

    /**
     * Öffentlicher Einstiegspunkt für Tests: generiert das vollständige Liga-Turnier
     * ohne Dialoge direkt auf dem aktuellen Dokument.
     */
    public void generiere() throws GenerateException {
        doRun();
    }
}
