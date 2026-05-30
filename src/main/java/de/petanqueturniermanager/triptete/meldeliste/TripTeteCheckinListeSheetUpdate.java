package de.petanqueturniermanager.triptete.meldeliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Aktualisiert die Trip-Tête-Checkin-Liste ohne das Sheet neu zu erstellen.
 * <p>
 * Schreibt nur den Datenbereich neu. Wird vom
 * {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener} verwendet, um beim
 * Tab-Wechsel die Checkin-Liste mit der Meldeliste zu synchronisieren.
 * <p>
 * Wenn die Checkin-Liste noch nicht existiert, wird der Lauf still abgebrochen – das
 * initiale Anlegen erfolgt ausschließlich über den Menüpunkt.
 */
public class TripTeteCheckinListeSheetUpdate extends TripTeteCheckinListeSheet {

    public TripTeteCheckinListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        if (getXSpreadSheet() == null) {
            return;
        }
        processBoxinfo("processbox.checkin.aktualisieren");
        aktualisiereInhalt();
    }
}
