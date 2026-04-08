/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.TeilnehmerSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das Supermêlée-Turniersystem.
 */
public class SupermeleeToolbarStrategie implements ITurnierSystemToolbarStrategie {

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new SpielrundeSheet_Naechste(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        new SpieltagRanglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        new TeilnehmerSheet(ws).testTurnierVorhanden().start();
    }

    @Override
    public void naechsterSpieltag(WorkingSpreadsheet ws) throws Exception {
        new MeldeListeSheet_NeuerSpieltag(ws).testTurnierVorhanden().backUpDocument().start();
    }

    @Override
    public void gesamtrangliste(WorkingSpreadsheet ws) throws Exception {
        new EndranglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
    }
}
