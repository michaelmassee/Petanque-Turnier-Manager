package de.petanqueturniermanager.maastrichter.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;

/**
 * Aktualisiert die Maastrichter-Teilnehmerliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link MaastrichterTeilnehmerSheet#generate()} (vollständiger Neuaufbau mit
 * {@code NewSheet.forceCreate()}) löscht diese Klasse nur den Datenbereich und schreibt ihn neu.
 * Wird vom {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener} verwendet, um bei
 * einem Tab-Wechsel zum Teilnehmer-Sheet die Liste mit der Meldeliste zu synchronisieren.
 * <p>
 * Wenn das Teilnehmer-Sheet noch nicht existiert, wird der Update-Lauf still abgebrochen – das
 * initiale Anlegen erfolgt ausschließlich über den Menüpunkt, nicht über einen Listener.
 */
public class MaastrichterTeilnehmerSheetUpdate extends MaastrichterTeilnehmerSheet {

    private static final Logger logger = LogManager.getLogger(MaastrichterTeilnehmerSheetUpdate.class);

    /** Großzügige Clear-Range: deckt jede plausible Block-Konfiguration ab. */
    private static final int CLEAR_LETZTE_ZEILE = 200;
    private static final int CLEAR_LETZTE_SPALTE = 40;

    public MaastrichterTeilnehmerSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            logger.debug("TeilnehmerUpdate: Sheet nicht vorhanden – skip "
                    + "(Erstaufbau erfolgt ausschließlich über Menü)");
            return;
        }
        processBoxinfo("processbox.teilnehmer.aktualisieren.einfach");
        loescheBisherigenInhalt(sheet);
        befuelleTeilnehmerDaten(false);

        // Bewusst KEIN setActiveSheet(sheet): Im Listener-Pfad ist der User schon auf dem
        // Teilnehmer-Sheet; ein zusätzliches setActiveSheet aus dem selectionChanged-Handler
        // heraus kollidiert mit LO-internem Tab-Klick-Handling und revertiert den Tab-Wechsel.
    }

    private void loescheBisherigenInhalt(XSpreadsheet sheet) throws GenerateException {
        RangeHelper.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                RangePosition.from(0, 0, CLEAR_LETZTE_SPALTE, CLEAR_LETZTE_ZEILE))
                .clearRange();
    }
}
