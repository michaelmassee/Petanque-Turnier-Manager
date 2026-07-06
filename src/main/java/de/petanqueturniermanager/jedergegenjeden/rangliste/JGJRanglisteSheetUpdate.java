package de.petanqueturniermanager.jedergegenjeden.rangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.rangliste.RanglisteUpdateHelper;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Aktualisiert die JGJ-Rangliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link JGJRanglisteSheet} (vollständiger Neuaufbau mit
 * {@code NewSheet.forceCreate()}) schreibt diese Klasse nur den Datenbereich neu.
 * Header, Spaltenbreiten und Metadaten bleiben unverändert.
 * <p>
 * Fallback: Wenn das Rangliste-Sheet noch nicht existiert, wird automatisch
 * {@link JGJRanglisteSheet#upDateSheet()} ausgelöst (vollständiger Erstaufbau).
 */
public class JGJRanglisteSheetUpdate extends JGJRanglisteSheet {

    private static final Logger logger = LogManager.getLogger(JGJRanglisteSheetUpdate.class);

    public JGJRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    @Override
    public void doRun() throws GenerateException {
        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            logger.debug("RanglisteUpdate: Sheet nicht vorhanden – vollständiger Erstaufbau");
            upDateSheet();
            return;
        }

        logger.debug("RanglisteUpdate START – Thread='{}'", Thread.currentThread().getName());
        processBoxinfo("processbox.rangliste.aktualisieren");

        var meldeListe = new JGJMeldeListeSheet_Update(getWorkingSpreadsheet());
        TeamMeldungen aktiveMeldungen = meldeListe.getAktiveMeldungen();
        if (aktiveMeldungen == null || aktiveMeldungen.size() == 0) {
            processBoxinfo("processbox.abbruch");
            return;
        }

        int clearAnzZeilen = berechneClearAnzZeilen(aktiveMeldungen.size(),
                getKonfigurationSheet().getGruppengroesse());
        RanglisteUpdateHelper.loescheDatenzeilen(this, sheet, clearAnzZeilen);
        insertHeader(sheet);
        berechnungUndSchreiben(sheet, meldeListe, aktiveMeldungen);

        logger.debug("RanglisteUpdate ENDE – Thread='{}'", Thread.currentThread().getName());
    }

    /**
     * Berechnet die Mindestanzahl zu löschender Zeilen.
     * Im Gruppen-Modus kommen pro Gruppe eine Header-Zeile hinzu.
     */
    private int berechneClearAnzZeilen(int teamAnzahl, int gruppengroesse) {
        if (gruppengroesse <= 0 || teamAnzahl <= gruppengroesse) {
            return teamAnzahl;
        }
        int anzGruppen = (int) Math.ceil((double) teamAnzahl / gruppengroesse);
        return teamAnzahl + anzGruppen;
    }
}
