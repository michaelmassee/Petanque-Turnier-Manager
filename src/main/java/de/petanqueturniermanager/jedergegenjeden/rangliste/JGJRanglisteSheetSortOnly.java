package de.petanqueturniermanager.jedergegenjeden.rangliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Neu-Berechnung der JGJ-Rangliste ohne vollständigen Neuaufbau des Sheets.
 * <p>
 * Delegiert an {@link JGJRanglisteSheetUpdate}: liest den Spielplan neu ein,
 * berechnet Statistiken und sortiert – ohne das Sheet zu löschen und neu anzulegen.
 */
public class JGJRanglisteSheetSortOnly extends JGJRanglisteSheetUpdate {

    public JGJRanglisteSheetSortOnly(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }
}
