/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TeilnehmerSheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das K.-O.-Turniersystem.
 * Weiter und Vorrunden-Rangliste sind für K.-O. nicht über die Toolbar verfügbar,
 * da dieses System einen Turnierbaum statt Vorrunden verwendet.
 */
public class KoToolbarStrategie implements ITurnierSystemToolbarStrategie {

    private final NichtVerfuegbarToolbarStrategie fallback = new NichtVerfuegbarToolbarStrategie();

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        fallback.weiter(ws);
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        fallback.vorrundenRangliste(ws);
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
    public boolean hatAbschlussphase() {
        return true;
    }

    @Override
    public void abschluss(WorkingSpreadsheet ws) throws Exception {
        new KoTurnierbaumSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }
}
