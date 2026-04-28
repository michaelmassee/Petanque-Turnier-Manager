/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar.strategie;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXTeilnehmerSheet;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXSpielrundeSheetNaechste;
import de.petanqueturniermanager.toolbar.ITurnierSystemToolbarStrategie;

/**
 * Toolbar-Strategie für das Formule X Turniersystem.
 * <ul>
 *   <li>{@link #weiter} → nächste Spielrunde erstellen</li>
 *   <li>{@link #vorrundenRangliste} → nicht verfügbar</li>
 *   <li>{@link #teilnehmer} → Teilnehmerliste</li>
 * </ul>
 */
public class FormuleXToolbarStrategie implements ITurnierSystemToolbarStrategie {

    private final NichtVerfuegbarToolbarStrategie fallback = new NichtVerfuegbarToolbarStrategie();

    @Override
    public void weiter(WorkingSpreadsheet ws) throws Exception {
        new FormuleXSpielrundeSheetNaechste(ws).testTurnierVorhanden().backUpDocument().start();
    }

    @Override
    public void vorrundenRangliste(WorkingSpreadsheet ws) throws Exception {
        fallback.vorrundenRangliste(ws);
    }

    @Override
    public void teilnehmer(WorkingSpreadsheet ws) throws Exception {
        new FormuleXTeilnehmerSheet(ws).testTurnierVorhanden().start();
    }
}
