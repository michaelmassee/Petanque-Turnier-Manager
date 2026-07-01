package de.petanqueturniermanager.jedergegenjeden.spielplan;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJGesamtranglisteSheet;

/**
 * JGJ-Beispielturnier mit 8 Triplette-Teams, aufgeteilt in zwei Gruppen à 4 Teams.
 * Erzeugt zusätzlich zur (gruppierten) Einzel-Rangliste die gruppenübergreifende
 * {@link JGJGesamtranglisteSheet}. Entspricht dem Menüeintrag
 * {@code ptm:jgj_testdaten_turnier_triplette_2gruppen}.
 */
public class JGJTriplette2Gruppen4TurnierTestDaten extends JGJTurnierTestDaten {

    private static final int ANZ_TEAMS = 8;
    private static final int GRUPPENGROESSE = 4;

    public JGJTriplette2Gruppen4TurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected Formation getFormation() {
        return Formation.TRIPLETTE;
    }

    @Override
    protected int getAnzTeams() {
        return ANZ_TEAMS;
    }

    @Override
    protected int getGruppengroesse() {
        return GRUPPENGROESSE;
    }

    @Override
    protected void doRun() throws GenerateException {
        super.doRun();

        // Zusätzlich die gruppenübergreifende Gesamtrangliste erzeugen und aktiv setzen.
        processBoxinfo("processbox.erstelle.rangliste");
        var gesamtrangliste = new JGJGesamtranglisteSheet(getWorkingSpreadsheet());
        gesamtrangliste.upDateSheet();
        TurnierSheet.from(gesamtrangliste.getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
    }
}
