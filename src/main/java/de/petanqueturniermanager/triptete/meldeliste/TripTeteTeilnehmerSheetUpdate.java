package de.petanqueturniermanager.triptete.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;

/**
 * Aktualisiert die Trip-Tête-Teilnehmerliste ohne das Sheet neu zu erstellen.
 * <p>
 * Löscht nur den Datenbereich und schreibt ihn neu. Wird vom
 * {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener} verwendet, um beim
 * Tab-Wechsel die Teilnehmerliste mit der Meldeliste zu synchronisieren.
 * <p>
 * Wenn das Sheet noch nicht existiert, wird der Lauf still abgebrochen – das initiale
 * Anlegen erfolgt ausschließlich über den Menüpunkt.
 */
public class TripTeteTeilnehmerSheetUpdate extends TripTeteTeilnehmerSheet {

    private static final Logger logger = LogManager.getLogger(TripTeteTeilnehmerSheetUpdate.class);

    private static final int CLEAR_LETZTE_ZEILE = 200;
    private static final int CLEAR_LETZTE_SPALTE = 40;

    public TripTeteTeilnehmerSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            logger.debug("TripTete-TeilnehmerUpdate: Sheet nicht vorhanden – skip");
            return;
        }
        processBoxinfo("processbox.teilnehmer.aktualisieren.einfach");
        loescheBisherigenInhalt(sheet);
        befuelleTeilnehmerDaten();
    }

    private void loescheBisherigenInhalt(XSpreadsheet sheet) throws GenerateException {
        RangeHelper.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                RangePosition.from(0, 0, CLEAR_LETZTE_SPALTE, CLEAR_LETZTE_ZEILE))
                .clearRange();
    }
}
