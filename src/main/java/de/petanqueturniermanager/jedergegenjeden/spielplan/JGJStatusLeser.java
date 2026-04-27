package de.petanqueturniermanager.jedergegenjeden.spielplan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellContentType;
import com.sun.star.table.XCell;
import com.sun.star.text.XText;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Liest den aktuellen Turnierstatus aus dem JGJ-Spielplan-Sheet.
 */
public class JGJStatusLeser {

    private static final Logger logger = LogManager.getLogger(JGJStatusLeser.class);
    private static final int MAX_ZEILEN = 1000;

    private final WorkingSpreadsheet workingSpreadsheet;

    private JGJStatusLeser(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    public static JGJStatusLeser von(WorkingSpreadsheet workingSpreadsheet) {
        return new JGJStatusLeser(workingSpreadsheet);
    }

    public JGJTurnierSchritt liesStatus() {
        var xDoc = workingSpreadsheet.getWorkingSpreadsheetDocument();
        var sheet = SheetMetadataHelper.findeSheetUndHeile(
                xDoc,
                SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN,
                SheetNamen.LEGACY_SPIELPLAN);
        if (sheet == null) {
            return new JGJTurnierSchritt(false, 0, 0, 0, 0);
        }
        return zaehleSpiele(sheet);
    }

    private JGJTurnierSchritt zaehleSpiele(XSpreadsheet sheet) {
        int hrGesamt = 0;
        int hrGespielt = 0;
        int rrGesamt = 0;
        int rrGespielt = 0;

        try {
            for (int zeile = JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
                XCell nrZelle = sheet.getCellByPosition(JGJSpielPlanSheet.SPIEL_NR_SPALTE, zeile);
                if (nrZelle.getType() == CellContentType.EMPTY) {
                    break;
                }
                var xText = Lo.qi(XText.class, nrZelle);
                String nr = xText != null ? xText.getString() : "";
                boolean istHinrunde = nr.startsWith(JGJSpielPlanSheet.NR_HINRUNDE_PREFIX);
                boolean istRueckrunde = nr.startsWith(JGJSpielPlanSheet.NR_RUECKRUNDE_PREFIX);
                if (!istHinrunde && !istRueckrunde) {
                    continue;
                }

                boolean gespielt = sheet.getCellByPosition(JGJSpielPlanSheet.SPIELE_A_SPALTE, zeile)
                        .getType() != CellContentType.EMPTY;

                if (istHinrunde) {
                    hrGesamt++;
                    if (gespielt) hrGespielt++;
                } else {
                    rrGesamt++;
                    if (gespielt) rrGespielt++;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            logger.error("Fehler beim Lesen des JGJ-Spielplans", e);
        }
        return new JGJTurnierSchritt(true, hrGespielt, hrGesamt, rrGespielt, rrGesamt);
    }
}
