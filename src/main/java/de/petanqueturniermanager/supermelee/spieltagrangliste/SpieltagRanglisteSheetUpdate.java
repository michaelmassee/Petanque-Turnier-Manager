package de.petanqueturniermanager.supermelee.spieltagrangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

/**
 * Aktualisiert eine Spieltag-Rangliste ohne das Sheet neu zu erstellen.
 * <p>
 * Im Gegensatz zu {@link SpieltagRanglisteSheet} (vollständiger Neuaufbau mit
 * {@code NewSheet.forceCreate()}) schreibt diese Klasse nur den Datenbereich neu.
 * Header, Spaltenbreiten und ConditionalFormats bleiben unverändert.
 * <p>
 * Wird vom {@link de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener}
 * verwendet, um bei einem Tab-Wechsel zur Rangliste nur die Daten zu aktualisieren –
 * ohne das Sheet zu löschen und neu anzulegen.
 * <p>
 * Fallback: Wenn das Sheet noch nicht existiert oder die Spielrunden-Anzahl geändert
 * hat (Strukturänderung), wird automatisch {@link SpieltagRanglisteSheet#generate}
 * ausgelöst (vollständiger Erstaufbau).
 */
public class SpieltagRanglisteSheetUpdate extends SpieltagRanglisteSheet {

    private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheetUpdate.class);

    public SpieltagRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet, SpielTagNr spieltagNr) {
        super(workingSpreadsheet, spieltagNr);
    }

    @Override
    protected void doRun() throws GenerateException {
        SpielTagNr nr = spieltagNrFuerRefresh != null
                ? spieltagNrFuerRefresh
                : getKonfigurationSheet().getAktiveSpieltag();
        setSpieltagNr(nr);

        XSpreadsheet sheet = getXSpreadSheet();
        if (sheet == null) {
            logger.debug("RanglisteUpdate: Spieltag-Rangliste {} nicht vorhanden – vollständiger Erstaufbau",
                    nr.getNr());
            generate(nr);
            return;
        }

        int aktuelleAnz = aktuelleSpielrundeSheet.countNumberOfSpielRundenSheets(nr);
        if (aktuelleAnz != countNumberOfSpielrundenInSheet()) {
            logger.debug("RanglisteUpdate: Spielrunden-Anzahl geändert – Neuaufbau für Spieltag {}", nr.getNr());
            generate(nr);
            return;
        }

        logger.debug("RanglisteUpdate START – Spieltag={}, Thread='{}'", nr.getNr(),
                Thread.currentThread().getName());
        processBoxinfo("processbox.rangliste.aktualisieren");

        blattschutzEntsprerren();
        try {
            MeldeListeSheet_Update meldeliste = new MeldeListeSheet_Update(getWorkingSpreadsheet());
            meldeliste.setSpielTag(nr);
            getSpielerSpalte().alleAktiveUndAusgesetzteMeldungenAusmeldelisteEinfuegen(meldeliste);
            ergebnisseAlsWerteEinfuegen(aktuelleAnz);
            getRangListeSpalte().upDateRanglisteSpalte();
            getRangListeSorter().doSort();
        } finally {
            blattschutzSchuetzen();
        }

        // Bewusst KEIN setActiveSheet(): Im Listener-Pfad ist der User schon auf der
        // Rangliste; ein zusätzliches setActiveSheet würde den Tab-Wechsel revertieren.
        logger.debug("RanglisteUpdate ENDE – Spieltag={}, Thread='{}'", nr.getNr(),
                Thread.currentThread().getName());
    }
}
