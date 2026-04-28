/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetUpdate;
import de.petanqueturniermanager.schweizer.meldeliste.TeilnehmerSheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das Schweizer Turniersystem.
 */
public class SchweizerToolbarStrategie implements ITurnierSystemToolbarStrategie {

    private final NichtVerfuegbarToolbarStrategie fallback = new NichtVerfuegbarToolbarStrategie();

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

    @Override
    public void neuAuslosen(WorkingSpreadsheet ws) throws Exception {
        new SchweizerSpielrundeSheetUpdate(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }

    @Override
    public void abschluss(WorkingSpreadsheet ws) throws Exception {
        fallback.abschluss(ws);
    }
}
