/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.maastrichter.finalrunde.MaastrichterFinalrundeSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterTeilnehmerSheet;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetUpdate;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das Maastrichter Turniersystem.
 */
public class MaastrichterToolbarStrategie implements ITurnierSystemToolbarStrategie {

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new MaastrichterSpielrundeSheetNaechste(ws).testTurnierVorhanden().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        new MaastrichterVorrundenRanglisteSheet(ws).testTurnierVorhanden().start();
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        new MaastrichterTeilnehmerSheet(ws).testTurnierVorhanden().start();
    }

    @Override
    public boolean hatNeuAuslosen() {
        return true;
    }

    @Override
    public void neuAuslosen(WorkingSpreadsheet ws) throws Exception {
        new MaastrichterSpielrundeSheetUpdate(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }

    @Override
    public boolean hatAbschlussphase() {
        return true;
    }

    @Override
    public void abschluss(WorkingSpreadsheet ws) throws Exception {
        new MaastrichterFinalrundeSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }
}
