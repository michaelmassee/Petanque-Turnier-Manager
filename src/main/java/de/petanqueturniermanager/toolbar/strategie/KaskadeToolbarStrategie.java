/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeTeilnehmerSheet;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeAktuelleRundeSheet;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeKoFeldSheet;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das Kaskaden-KO-Turniersystem.
 * <ul>
 *   <li>{@link #weiter} → nächste Kaskadenrunde oder KO-Felder erstellen</li>
 *   <li>{@link #vorrundenRangliste} → nicht verfügbar (Vorrunden-Rangliste macht im Kaskaden-System keinen Sinn)</li>
 *   <li>{@link #teilnehmer} → Kaskaden-Teilnehmerliste</li>
 * </ul>
 */
public class KaskadeToolbarStrategie implements ITurnierSystemToolbarStrategie {

    private final NichtVerfuegbarToolbarStrategie fallback = new NichtVerfuegbarToolbarStrategie();

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new KaskadeSpielrundeSheet(ws).testTurnierVorhanden().backUpDocument().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        fallback.vorrundenRangliste(ws);
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        new KaskadeTeilnehmerSheet(ws).testTurnierVorhanden().start();
    }

    @Override
    public void neuAuslosen(WorkingSpreadsheet ws) throws Exception {
        new KaskadeAktuelleRundeSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }

    @Override
    public void abschluss(WorkingSpreadsheet ws) throws Exception {
        var koFelder = new KaskadeKoFeldSheet(ws);
        koFelder.setForceOk(true);
        koFelder.testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
    }
}
