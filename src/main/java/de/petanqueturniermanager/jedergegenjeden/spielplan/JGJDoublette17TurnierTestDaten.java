package de.petanqueturniermanager.jedergegenjeden.spielplan;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * JGJ-Beispielturnier mit 17 Doublette-Teams, aufgeteilt in Gruppen à 6 Teams.
 * Entspricht dem Menüeintrag {@code ptm:jgj_testdaten_turnier_doublette_17}.
 */
public class JGJDoublette17TurnierTestDaten extends JGJTurnierTestDaten {

    private static final int ANZ_TEAMS = 17;
    private static final int GRUPPENGROESSE = 6;

    public JGJDoublette17TurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected Formation getFormation() {
        return Formation.DOUBLETTE;
    }

    @Override
    protected int getAnzTeams() {
        return ANZ_TEAMS;
    }

    @Override
    protected int getGruppengroesse() {
        return GRUPPENGROESSE;
    }

}
