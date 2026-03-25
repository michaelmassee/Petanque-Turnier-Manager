package de.petanqueturniermanager.schweizer.spielrunde;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * Schweizer-Beispielturnier mit 19 Teams (ungerade Anzahl → 1 Freilos pro Runde,
 * Teamname in Spielrunde, Bahn zufällig).
 * <p>
 * Entspricht dem Menüeintrag {@code ptm:schweizer_testdaten_turnier_19}.
 */
public class Schweizer19TeamsTurnierTestDaten extends SchweizerTurnierTestDaten {

    public Schweizer19TeamsTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, 19, SpielplanTeamAnzeige.NAME);
    }
}
