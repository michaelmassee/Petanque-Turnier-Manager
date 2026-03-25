package de.petanqueturniermanager.liga.spielplan;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Liga-Beispielturnier mit ungerade Teamanzahl und Freispiel
 * (Meldeliste + Spielplan + Rangliste).
 * <p>
 * Entspricht dem Menüeintrag {@code ptm:liga_spielplan_testdaten_mit_freispiel}.
 *
 * <h3>Warum nicht {@code generate()}?</h3>
 * Wie bei {@link LigaTurnierTestDaten} würde eine Überschreibung von {@code generate()}
 * eine Endlosrekursion erzeugen ({@code doRun() → generate() → doRun() → …}).
 * Der Einstiegspunkt heißt deshalb {@code erzeugeBeispielturnier()}.
 */
public class LigaMitFreispielTurnierTestDaten extends LigaSpielPlanSheetTestDaten {

    public LigaMitFreispielTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, true); // ungerade Teamanzahl → Freispiel
    }

    /**
     * Erzeugt das vollständige Liga-Beispielturnier mit Freispiel ohne Dialoge.
     */
    public void erzeugeBeispielturnier() throws GenerateException {
        doRun();
    }
}
