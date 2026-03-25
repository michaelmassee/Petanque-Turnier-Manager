package de.petanqueturniermanager.liga.spielplan;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Öffentlicher Einstiegspunkt für {@link de.petanqueturniermanager.beispielturnier.BeispielturnierRegistrierung}
 * und Tests: erzeugt ein vollständiges Liga-Beispielturnier
 * (Meldeliste, gerade Teamanzahl, kein Freilos) + Spielplan + Rangliste.
 *
 * <h3>Warum nicht {@code generate()}?</h3>
 * Die Elternklasse {@link LigaSpielPlanSheetTestDaten} besitzt bereits eine Methode
 * {@code generate()}, die von {@code doRun()} aufgerufen wird:
 * <pre>
 *   doRun() → this.generate() → …
 * </pre>
 * Würde diese Klasse {@code generate()} überschreiben, entstünde eine Endlosrekursion:
 * <pre>
 *   doRun() → this.generate() [überschrieben] → doRun() → this.generate() → …
 * </pre>
 * Der Einstiegspunkt heißt deshalb {@code erzeugeBeispielturnier()} und ruft
 * {@code doRun()} direkt auf – womit {@link LigaSpielPlanSheetTestDaten#generate()}
 * unangetastet bleibt.
 */
public class LigaTurnierTestDaten extends LigaSpielPlanSheetTestDaten {

    public LigaTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, false); // gerade Teamanzahl, kein Freilos
    }

    /**
     * Erzeugt das vollständige Liga-Beispielturnier ohne Dialoge auf dem aktuellen Dokument.
     * <p>
     * Ruft {@code doRun()} direkt auf. <strong>Darf nicht {@code generate()} heißen</strong>,
     * da {@link LigaSpielPlanSheetTestDaten#generate()} bereits von {@code doRun()} aufgerufen
     * wird – eine Umbenennung würde eine Endlosrekursion auslösen.
     */
    public void erzeugeBeispielturnier() throws GenerateException {
        doRun();
    }
}
