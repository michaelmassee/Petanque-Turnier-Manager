/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.meldeliste.TeilnehmerSheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das Schweizer Turniersystem.
 */
public class SchweizerToolbarStrategie implements ITurnierSystemToolbarStrategie {

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new SchweizerSpielrundeSheetNaechste(ws).testTurnierVorhanden().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        new SchweizerRanglisteSheet(ws).testTurnierVorhanden().start();
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        new TeilnehmerSheet(ws).testTurnierVorhanden().start();
    }
}
