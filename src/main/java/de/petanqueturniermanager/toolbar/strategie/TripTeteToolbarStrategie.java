/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteCheckinListeSheet;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteTeilnehmerSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;

/**
 * Toolbar-Strategie für das Trip-Tête-Turniersystem.
 * <ul>
 *   <li>{@link #weiter} → Spielplan erstellen / aktualisieren</li>
 *   <li>{@link #teilnehmer} → Teilnehmerliste</li>
 *   <li>{@link #checkin} → Checkin-Liste</li>
 * </ul>
 */
public class TripTeteToolbarStrategie implements ITurnierSystemToolbarStrategie {

    private final NichtVerfuegbarToolbarStrategie fallback = new NichtVerfuegbarToolbarStrategie();

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new TripTeteSpielPlanSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        fallback.vorrundenRangliste(ws);
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        new TripTeteTeilnehmerSheet(ws).testTurnierVorhanden().start();
    }

    @Override
    public void checkin(WorkingSpreadsheet ws) throws Exception {
        new TripTeteCheckinListeSheet(ws).testTurnierVorhanden().backUpDocument().start();
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
