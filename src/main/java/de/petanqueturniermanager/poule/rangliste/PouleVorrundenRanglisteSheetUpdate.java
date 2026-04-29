/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Aktualisiert die Poule Vorrunden-Rangliste ohne das Sheet neu zu erstellen.
 * <p>
 * Wenn das Sheet noch nicht vorhanden ist, wird ein Vollaufbau via
 * {@link PouleVorrundenRanglisteSheet#doRun()} durchgeführt.
 * Andernfalls werden nur die Daten neu berechnet und geschrieben,
 * ohne das Sheet via {@code forceCreate()} zu löschen und neu anzulegen.
 * Dadurch werden keine Sheet-Events ausgelöst, die eine Race Condition
 * mit dem {@code RanglisteRefreshListener} verursachen könnten.
 */
public class PouleVorrundenRanglisteSheetUpdate extends PouleVorrundenRanglisteSheet {

    private static final Logger logger = LogManager.getLogger(PouleVorrundenRanglisteSheetUpdate.class);

    public PouleVorrundenRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    public void doRun() throws GenerateException {
        if (getXSpreadSheet() == null) {
            // Kein Sheet vorhanden → Vollaufbau
            new PouleVorrundenRanglisteSheet(getWorkingSpreadsheet()).doRun();
            return;
        }

        var vorrundeSheet = SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE,
                SheetNamen.pouleVorrunde());

        if (vorrundeSheet == null) {
            logger.warn("Poule Vorrunde-Sheet nicht gefunden, Update wird übersprungen.");
            return;
        }

        XSpreadsheet xSheet = getXSpreadSheet();
        berechnungUndSchreiben(vorrundeSheet, xSheet);

        // Bewusst KEIN setActiveSheet(xSheet): Im Listener-Pfad ist der User schon auf der
        // Rangliste; ein zusätzliches setActiveSheet aus dem selectionChanged-Handler heraus
        // kollidiert mit LO-internem Tab-Klick-Handling und revertiert den Tab-Wechsel.
        // Bei programmatischen Aufrufen übernimmt der aufrufende Parent-Runner die Aktivierung.
    }
}
