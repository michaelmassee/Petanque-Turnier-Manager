/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterTeilnehmerSheet;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
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
}
