package de.petanqueturniermanager.ko;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * K.-O.-Beispielturnier mit 10 Teams und Cadrage-Auslosung.
 * <p>
 * Entspricht dem Menüeintrag {@code ptm:ko_testdaten_cadrage}.
 */
public class KoCadrageTurnierTestDaten extends KoTurnierTestDaten {

    public KoCadrageTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, 10);
    }
}
