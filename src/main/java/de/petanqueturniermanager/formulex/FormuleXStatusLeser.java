package de.petanqueturniermanager.formulex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellContentType;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXPropertiesSpalte;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXAbstractSpielrundeSheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Liest den aktuellen Turnierstatus aus einem Formule-X-Dokument.
 */
public class FormuleXStatusLeser {

    private static final Logger logger = LogManager.getLogger(FormuleXStatusLeser.class);
    private static final int MAX_ZEILEN = 2000;

    private final WorkingSpreadsheet workingSpreadsheet;

    private FormuleXStatusLeser(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    public static FormuleXStatusLeser von(WorkingSpreadsheet workingSpreadsheet) {
        return new FormuleXStatusLeser(workingSpreadsheet);
    }

    public FormuleXTurnierSchritt liesStatus() {
        var xDoc = workingSpreadsheet.getWorkingSpreadsheetDocument();
        var docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
        int anzahlRunden = docPropHelper.getIntProperty(FormuleXPropertiesSpalte.KONFIG_PROP_ANZAHL_RUNDEN, 4);

        int hoechsteRunde = 0;
        XSpreadsheet hoechsteRundeSheet = null;
        for (int n = 1; n < 1000; n++) {
            var schluessel = SheetMetadataHelper.SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX + n;
            var sheetOpt = SheetMetadataHelper.findeSheet(xDoc, schluessel);
            if (sheetOpt.isEmpty()) {
                break;
            }
            hoechsteRunde = n;
            hoechsteRundeSheet = sheetOpt.get();
        }

        if (hoechsteRundeSheet == null) {
            return new FormuleXTurnierSchritt(false, 0, 0, 0, anzahlRunden, false);
        }

        int[] zaehler = zaehleSpiele(hoechsteRundeSheet);
        int gespielt = zaehler[0];
        int gesamt = zaehler[1];

        boolean beendet = hoechsteRunde >= anzahlRunden && gesamt > 0 && gespielt == gesamt;
        return new FormuleXTurnierSchritt(true, hoechsteRunde, gespielt, gesamt, anzahlRunden, beendet);
    }

    private int[] zaehleSpiele(XSpreadsheet sheet) {
        int gesamt = 0;
        int gespielt = 0;
        try {
            for (int zeile = FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
                var teamZelle = sheet.getCellByPosition(FormuleXAbstractSpielrundeSheet.TEAM_A_SPALTE, zeile);
                if (teamZelle.getType() == CellContentType.EMPTY) {
                    break;
                }
                var teamBZelle = sheet.getCellByPosition(FormuleXAbstractSpielrundeSheet.TEAM_B_SPALTE, zeile);
                if (teamBZelle.getType() == CellContentType.EMPTY) {
                    continue; // Freilos: kein Gegner, kein Ergebnis nötig
                }
                gesamt++;
                var ergZelle = sheet.getCellByPosition(FormuleXAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile);
                if (ergZelle.getType() == CellContentType.VALUE) {
                    gespielt++;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            logger.error("Fehler beim Lesen der Formule-X-Spielrunden-Spiele", e);
        }
        return new int[] { gespielt, gesamt };
    }
}
