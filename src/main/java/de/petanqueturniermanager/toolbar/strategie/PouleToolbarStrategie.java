/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.poule.meldeliste.PouleTeilnehmerSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;
import de.petanqueturniermanager.poule.vorrunde.PouleVorrundeSheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das Poule A/B-Turniersystem.
 */
public class PouleToolbarStrategie implements ITurnierSystemToolbarStrategie {

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new PouleVorrundeSheet(ws).testTurnierVorhanden().backUpDocument().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        new PouleVorrundenRanglisteSheet(ws).testTurnierVorhanden().start();
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        new PouleTeilnehmerSheet(ws).testTurnierVorhanden().start();
    }
}
