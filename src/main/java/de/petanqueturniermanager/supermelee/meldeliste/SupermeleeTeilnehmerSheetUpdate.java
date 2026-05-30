package de.petanqueturniermanager.supermelee.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;

/**
 * Aktualisiert die Supermelee-Spieltag-Teilnehmerliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link SupermeleeTeilnehmerSheet#generate()} (vollständiger
 * Neuaufbau mit {@code NewSheet.forceCreate()}) löscht diese Klasse nur den
 * Datenbereich und schreibt ihn neu. Header, Spaltenbreiten und Metadaten werden
 * vom {@link de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerSheetBuilder}
 * regulär mit-erzeugt.
 * <p>
 * Wird vom {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener}
 * verwendet, um bei einem Tab-Wechsel zum Teilnehmer-Sheet die Liste mit der
 * Meldeliste zu synchronisieren – ohne das Sheet zu löschen und neu anzulegen.
 * <p>
 * Wenn das Teilnehmer-Sheet noch nicht existiert, wird der Update-Lauf still
 * abgebrochen – das initiale Anlegen erfolgt ausschließlich über den Menüpunkt
 * "Spieltag anlegen", nicht über einen Hintergrund-Listener.
 */
public class SupermeleeTeilnehmerSheetUpdate extends SupermeleeTeilnehmerSheet {

    private static final Logger logger = LogManager.getLogger(SupermeleeTeilnehmerSheetUpdate.class);

    /** Großzügige Clear-Range: deckt jede plausibele Block-Konfiguration ab. */
    private static final int CLEAR_LETZTE_ZEILE = 200;
    private static final int CLEAR_LETZTE_SPALTE = 40;

    public SupermeleeTeilnehmerSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        setSpielTagNr(getKonfigurationSheet().getAktiveSpieltag());
        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            logger.debug("TeilnehmerUpdate: Sheet für Spieltag {} nicht vorhanden – skip "
                    + "(Erstaufbau erfolgt ausschließlich über Menü 'Spieltag anlegen')",
                    getSpielTagNr().getNr());
            return;
        }

        logger.debug("TeilnehmerUpdate START – Spieltag {}, Thread='{}'",
                getSpielTagNr().getNr(), Thread.currentThread().getName());
        processBoxinfo("processbox.teilnehmer.aktualisieren", getSpielTagNr().getNr());

        loescheBisherigenInhalt(sheet);
        befuelleTeilnehmerDaten();

        // Bewusst KEIN setActiveSheet(sheet): Im Listener-Pfad ist der User schon auf dem
        // Teilnehmer-Sheet; ein zusätzliches setActiveSheet aus dem selectionChanged-Handler
        // heraus kollidiert mit LO-internem Tab-Klick-Handling und revertiert den Tab-Wechsel.
        logger.debug("TeilnehmerUpdate ENDE – Spieltag {}, Thread='{}'",
                getSpielTagNr().getNr(), Thread.currentThread().getName());
    }

    private void loescheBisherigenInhalt(XSpreadsheet sheet) throws GenerateException {
        RangeHelper.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                RangePosition.from(0, 0, CLEAR_LETZTE_SPALTE, CLEAR_LETZTE_ZEILE))
                .clearRange();
    }
}
