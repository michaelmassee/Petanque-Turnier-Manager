/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetUpdate;
import de.petanqueturniermanager.helper.rangliste.RanglisteUpdateHelper;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Aktualisiert die Formule X Rangliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link FormuleXRanglisteSheet} (vollständiger Neuaufbau) schreibt
 * diese Klasse nur den Datenbereich neu. Header und Metadaten bleiben unverändert.
 * <p>
 * Fallback: Wenn das Rangliste-Sheet noch nicht existiert, wird automatisch
 * {@link FormuleXRanglisteSheet#doRun()} ausgelöst.
 */
public class FormuleXRanglisteSheetUpdate extends FormuleXRanglisteSheet {

    private static final Logger LOGGER = LogManager.getLogger(FormuleXRanglisteSheetUpdate.class);

    public FormuleXRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    public void doRun() throws GenerateException {

        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            LOGGER.debug("RanglisteUpdate: Sheet nicht vorhanden – vollständiger Erstaufbau");
            new FormuleXRanglisteSheet(getWorkingSpreadsheet()).doRun();
            return;
        }

        LOGGER.debug("RanglisteUpdate START – Thread='{}'", Thread.currentThread().getName());
        processBoxinfo("processbox.rangliste.aktualisieren");

        FormuleXMeldeListeSheetUpdate meldeliste = new FormuleXMeldeListeSheetUpdate(getWorkingSpreadsheet());
        TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
        if (aktiveMeldungen == null || aktiveMeldungen.size() == 0) {
            processBoxinfo("processbox.abbruch");
            return;
        }

        RanglisteUpdateHelper.loescheDatenzeilen(this, sheet, aktiveMeldungen.size());
        berechnungUndSchreiben(sheet, meldeliste, aktiveMeldungen);


        // Bewusst KEIN setActiveSheet(sheet): Im Listener-Pfad ist der User schon auf der
        // Rangliste; ein zusätzliches setActiveSheet aus dem selectionChanged-Handler heraus
        // kollidiert mit LO-internem Tab-Klick-Handling und revertiert den Tab-Wechsel.
        // Bei programmatischen Aufrufen übernimmt der aufrufende Parent-Runner die Aktivierung.
        LOGGER.debug("RanglisteUpdate ENDE – Thread='{}'", Thread.currentThread().getName());
    }

}
