package de.petanqueturniermanager.maastrichter;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Maastrichter-Beispielturnier mit 57 Teams (4 Vorrunden + 4 Finalgruppen,
 * letzte Gruppe mit Cadrage).
 * <p>
 * Entspricht dem Menüeintrag {@code ptm:maastrichter_testdaten_turnier_57}.
 */
public class Maastrichter57TeamsTurnierTestDaten extends MaastrichterTurnierTestDaten {

    public Maastrichter57TeamsTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, 57, 4, 16, 8);
    }
}
