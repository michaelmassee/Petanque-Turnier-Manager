package de.petanqueturniermanager.supermelee.endrangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Aktualisiert die Supermelee-Endrangliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link EndranglisteSheet} (vollständiger Neuaufbau mit
 * {@code NewSheet.forceCreate()}) schreibt diese Klasse nur den Datenbereich neu.
 * Header, Spaltenbreiten und ConditionalFormats bleiben unverändert.
 * <p>
 * Wird vom {@link de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener}
 * verwendet, um bei einem Tab-Wechsel zur Endrangliste nur die Daten zu aktualisieren –
 * ohne das Sheet zu löschen und neu anzulegen.
 * <p>
 * Fallback: Wenn das Endrangliste-Sheet noch nicht existiert, wird automatisch
 * ein vollständiger Erstaufbau via {@link EndranglisteSheet#doRun()} ausgelöst.
 */
public class EndranglisteSheetUpdate extends EndranglisteSheet {

    private static final Logger logger = LogManager.getLogger(EndranglisteSheetUpdate.class);

    public EndranglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            logger.debug("RanglisteUpdate: Endrangliste nicht vorhanden – vollständiger Erstaufbau");
            new EndranglisteSheet(getWorkingSpreadsheet()).doRun();
            return;
        }

        logger.debug("EndranglisteUpdate START – Thread='{}'", Thread.currentThread().getName());
        processBoxinfo("processbox.rangliste.aktualisieren");

        blattschutzEntsprerren();
        try {
            spielerEinfuegen();
            spielTageUndSummenAlsWerteEinfuegen();
            getRangListeSpalte().upDateRanglisteSpalte();
            formatDatenGeradeUngeradeMitStreichSpieltag();
            formatSchlechtesteSpieltagSpalte();
            getxCalculatable().calculate(); // Sort-Spalten-Formeln auswerten
            getRangListeSorter().doSort();
        } finally {
            blattschutzSchuetzen();
        }

        // Bewusst KEIN setActiveSheet(): Im Listener-Pfad ist der User schon auf der
        // Endrangliste; ein zusätzliches setActiveSheet würde den Tab-Wechsel revertieren.
        logger.debug("EndranglisteUpdate ENDE – Thread='{}'", Thread.currentThread().getName());
    }
}
