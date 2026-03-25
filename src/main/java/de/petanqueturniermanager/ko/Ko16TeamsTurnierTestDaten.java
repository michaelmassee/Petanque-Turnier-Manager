package de.petanqueturniermanager.ko;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * K.-O.-Beispielturnier mit 16 Teams (2 Gruppen A und B).
 * <p>
 * Entspricht dem Menüeintrag {@code ptm:ko_testdaten_16_teams}.
 */
public class Ko16TeamsTurnierTestDaten extends KoTurnierTestDaten {

    public Ko16TeamsTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, 16);
    }
}
