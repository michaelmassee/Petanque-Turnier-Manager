package de.petanqueturniermanager.poule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellContentType;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet;

/**
 * Liest den aktuellen Turnierstatus aus dem Poule-A/B-Dokument.
 */
public class PouleStatusLeser {

    private static final Logger logger = LogManager.getLogger(PouleStatusLeser.class);
    private static final int MAX_ZEILEN = 2000;
    private static final String SCORE_PRAEFIX = "PTM_EDIT:";
    private static final String POSITIONS_TRENNZEICHEN = "\\|";
    private static final String KOORDINATEN_TRENNZEICHEN = ",";

    private final WorkingSpreadsheet workingSpreadsheet;

    private PouleStatusLeser(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    public static PouleStatusLeser von(WorkingSpreadsheet workingSpreadsheet) {
        return new PouleStatusLeser(workingSpreadsheet);
    }

    public PouleTurnierSchritt liesStatus() {
        var xDoc = workingSpreadsheet.getWorkingSpreadsheetDocument();

        var vorrundeSheet = SheetMetadataHelper.findeSheetUndHeile(
                xDoc, SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE, SheetNamen.pouleVorrunde());

        if (vorrundeSheet == null) {
            return new PouleTurnierSchritt(false, 0, 0, false, false);
        }

        int[] zaehler = zaehleVorrundeSpiele(vorrundeSheet);

        var koSheet = SheetMetadataHelper.findeSheet(xDoc, SheetMetadataHelper.schluesselPouleKo("A"));
        if (koSheet.isEmpty()) {
            return new PouleTurnierSchritt(true, zaehler[0], zaehler[1], false, false);
        }

        boolean beendet = pruefeKoBeendet(xDoc, koSheet.get());
        return new PouleTurnierSchritt(true, zaehler[0], zaehler[1], true, beendet);
    }

    private int[] zaehleVorrundeSpiele(XSpreadsheet sheet) {
        int gesamt = 0;
        int gespielt = 0;
        try {
            for (int zeile = AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
                var beschrZelle = sheet.getCellByPosition(AbstractPouleVorrundeSheet.SPALTE_BESCHR, zeile);
                if (beschrZelle.getType() == CellContentType.EMPTY) {
                    break;
                }
                gesamt++;
                var ergZelle = sheet.getCellByPosition(AbstractPouleVorrundeSheet.SPALTE_ERG_A, zeile);
                if (ergZelle.getType() == CellContentType.VALUE) {
                    gespielt++;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            logger.error("Fehler beim Lesen der Poule-Vorrunde-Spiele", e);
        }
        return new int[] { gespielt, gesamt };
    }

    /**
     * Prüft ob alle KO-Scores des A-Finales eingetragen sind.
     * Liest dazu die gespeicherten Score-Zellpositionen aus dem Named Range.
     */
    private boolean pruefeKoBeendet(
            com.sun.star.sheet.XSpreadsheetDocument xDoc, XSpreadsheet koSheet) {
        var scoreKey = SheetMetadataHelper.scoreSchluessel(SheetMetadataHelper.schluesselPouleKo("A"));
        var scoreText = SheetMetadataHelper.leseScoreText(xDoc, scoreKey);
        if (scoreText == null || !scoreText.startsWith(SCORE_PRAEFIX)) {
            return false;
        }
        var positionenStr = scoreText.substring(SCORE_PRAEFIX.length());
        var positionen = positionenStr.split(POSITIONS_TRENNZEICHEN);
        try {
            for (var pos : positionen) {
                if (pos.isBlank()) {
                    continue;
                }
                var teile = pos.split(KOORDINATEN_TRENNZEICHEN);
                if (teile.length < 2) {
                    continue;
                }
                int spalte = Integer.parseInt(teile[0].trim());
                int zeile = Integer.parseInt(teile[1].trim());
                var zelle = koSheet.getCellByPosition(spalte, zeile);
                if (zelle.getType() != CellContentType.VALUE) {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Prüfen der KO-Scores für Beendet-Erkennung", e);
            return false;
        }
        return positionen.length > 0;
    }
}
