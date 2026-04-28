/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TeilnehmerSheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das Jeder-gegen-Jeden-Turniersystem.
 * Weiter ist für dieses System nicht über die Toolbar verfügbar,
 * da alle Begegnungen in einem Durchgang geplant werden.
 */
public class JGJToolbarStrategie implements ITurnierSystemToolbarStrategie {

    private final NichtVerfuegbarToolbarStrategie fallback = new NichtVerfuegbarToolbarStrategie();

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new JGJSpielPlanSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        new JGJRanglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        new TeilnehmerSheet(ws).testTurnierVorhanden().start();
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
