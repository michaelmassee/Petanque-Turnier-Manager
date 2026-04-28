/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das Liga-Turniersystem.
 * Weiter und Teilnehmer sind für die Liga nicht über die Toolbar verfügbar.
 */
public class LigaToolbarStrategie implements ITurnierSystemToolbarStrategie {

    private final NichtVerfuegbarToolbarStrategie fallback = new NichtVerfuegbarToolbarStrategie();

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new LigaSpielPlanSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        new LigaRanglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        fallback.teilnehmer(ws);
    }

    @Override
    public void neuAuslosen(WorkingSpreadsheet ws) throws Exception {
        fallback.neuAuslosen(ws);
    }

    @Override
    public void abschluss(WorkingSpreadsheet ws) throws Exception {
        fallback.abschluss(ws);
    }
}
