package de.petanqueturniermanager.maastrichter;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Maastrichter-Beispielturnier mit 35 Teams (3 Vorrunden + 2 Finalgruppen:
 * Gruppe A ohne Cadrage, Gruppe B mit Cadrage).
 * <p>
 * Entspricht dem Menüeintrag {@code ptm:maastrichter_testdaten_turnier_35}.
 */
public class Maastrichter35TeamsTurnierTestDaten extends MaastrichterTurnierTestDaten {

    public Maastrichter35TeamsTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, 35, 3, 16, 8);
    }
}
