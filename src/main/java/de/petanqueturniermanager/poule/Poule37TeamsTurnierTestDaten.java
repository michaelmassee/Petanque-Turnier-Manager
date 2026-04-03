/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.poule.ko.PouleKoSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;

/**
 * Generiert ein vollständiges Poule-A/B-Beispielturnier mit 37 Teams ohne Dialoge:
 * <ol>
 *   <li>Meldeliste (37 Teams, Triplette)</li>
 *   <li>Teilnehmerliste</li>
 *   <li>Vorrunde (9 Vierer-Poules + 1 Dreier-Poule = 10 Gruppen)</li>
 *   <li>Spielpläne (je ein Sheet pro Gruppe)</li>
 *   <li>Zufallsergebnisse für alle Matches</li>
 *   <li>Vorrunden-Rangliste</li>
 *   <li>KO-Sheets (A-Turnier: 20 Teams, B-Turnier: 17 Teams)</li>
 * </ol>
 */
public class Poule37TeamsTurnierTestDaten extends PouleTurnierTestDaten {

    public Poule37TeamsTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, 37);
    }

    @Override
    protected void doRun() throws GenerateException {
        super.doRun();

        // Vorrunden-Rangliste erstellen (muss vor KO-Sheets erstellt sein)
        new PouleVorrundenRanglisteSheet(getWorkingSpreadsheet()).doRun();

        // KO-Sheets erstellen (A + B).
        // pruefeUndAktualisiereVorrundenRangliste() zeigt keine MessageBox,
        // weil das Rangliste-Sheet bereits existiert.
        new PouleKoSheet(getWorkingSpreadsheet()).doRun();
    }
}
