/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.ko.meldeliste.KoTeilnehmerSheet;
import de.petanqueturniermanager.ko.meldeliste.KoCheckinListeSheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das K.-O.-Turniersystem.
 * Weiter erstellt beim K.-O.-System den Turnierbaum; Vorrunden-Rangliste ist nicht verfügbar.
 */
public class KoToolbarStrategie implements ITurnierSystemToolbarStrategie {

    private final NichtVerfuegbarToolbarStrategie fallback = new NichtVerfuegbarToolbarStrategie();

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new KoTurnierbaumSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        fallback.vorrundenRangliste(ws);
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        new KoTeilnehmerSheet(ws).testTurnierVorhanden().start();
    }

    @Override
    public void checkin(WorkingSpreadsheet ws) throws Exception {
        new KoCheckinListeSheet(ws).testTurnierVorhanden().backUpDocument().start();
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
