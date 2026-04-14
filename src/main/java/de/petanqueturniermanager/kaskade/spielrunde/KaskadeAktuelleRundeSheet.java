/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.spielrunde;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Erstellt die aktuelle Kaskadenrunde neu (Menüaktion „Aktuelle Runde neu erstellen").<br>
 * <br>
 * Strategie:
 * <ul>
 *   <li>Wurde noch keine Runde erstellt ({@code aktiveRunde == 0}): verhält sich wie
 *       {@link KaskadeSpielrundeSheet} und legt Runde 1 an.</li>
 *   <li>Sonst: dekrement {@code aktiveRunde} um 1 und ruft den normalen
 *       {@link KaskadeSpielrundeSheet#doRun()} auf, der dann dieselbe Runde als „nächste"
 *       mit {@code forceCreate = true} neu anlegt.</li>
 * </ul>
 *
 * @author Michael Massee
 */
public class KaskadeAktuelleRundeSheet extends KaskadeSpielrundeSheet {

    public KaskadeAktuelleRundeSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        var konfig      = getKonfigurationSheet();
        int aktiveRunde = konfig.getAktiveKaskadenRunde();

        if (aktiveRunde <= 0) {
            // Noch keine Runde erstellt: wie normale nächste Runde behandeln
            super.doRun();
            return;
        }

        // Runde dekrementieren, damit super.doRun() dieselbe Runde als „nächste" neu erstellt
        konfig.setAktiveKaskadenRunde(aktiveRunde - 1);
        setForceOk(true);
        super.doRun();
    }
}
