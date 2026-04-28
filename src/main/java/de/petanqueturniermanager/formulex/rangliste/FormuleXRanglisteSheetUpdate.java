/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetUpdate;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.toolbar.TurnierModus;

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
    protected void doRun() throws GenerateException {
        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
        }

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
            if (TurnierModus.get().istAktiv()) {
                BlattschutzRegistry.fuer(getTurnierSystem())
                        .ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
            }
            return;
        }

        loescheDatenzeilen(sheet, aktiveMeldungen.size());
        berechnungUndSchreiben(sheet, meldeliste, aktiveMeldungen);

        if (TurnierModus.get().istAktiv()) {
            BlattschutzRegistry.fuer(getTurnierSystem())
                    .ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
        }

        if (SheetRunner.isRunning()) {
            getSheetHelper().setActiveSheet(sheet);
            SheetRunner.unterdrückeNaechstesSelectionChange();
        }
        LOGGER.debug("RanglisteUpdate ENDE – Thread='{}'", Thread.currentThread().getName());
    }

    private void loescheDatenzeilen(XSpreadsheet sheet, int neueTeamAnzahl) throws GenerateException {
        int bisherigeLetzte = sucheLetzteZeileMitSpielerNummer();
        int neueLetzte = ERSTE_DATEN_ZEILE + neueTeamAnzahl - 1;
        if (bisherigeLetzte > neueLetzte) {
            RangeHelper.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                    RangePosition.from(TEAM_NR_SPALTE, neueLetzte + 1, VALIDATE_SPALTE, bisherigeLetzte))
                    .clearRange();
            LOGGER.debug("loescheDatenzeilen: Zeilen {}-{} gelöscht", neueLetzte + 1, bisherigeLetzte);
        }
    }
}
