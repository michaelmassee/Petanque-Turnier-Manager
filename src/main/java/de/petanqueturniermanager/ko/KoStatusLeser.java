package de.petanqueturniermanager.ko;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Liest den aktuellen Turnierstatus aus dem KO-Dokument.
 */
public class KoStatusLeser {

    private static final Logger logger = LogManager.getLogger(KoStatusLeser.class);
    private static final String SCORE_PRAEFIX = "PTM_EDIT:";
    private static final List<String> TURNIERBAUM_BUCHSTABEN = List.of("A", "B", "C", "D", "E", "F", "G", "H");

    private final WorkingSpreadsheet workingSpreadsheet;

    private KoStatusLeser(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    public static KoStatusLeser von(WorkingSpreadsheet workingSpreadsheet) {
        return new KoStatusLeser(workingSpreadsheet);
    }

    public KoTurnierSchritt liesStatus() {
        var xDoc = workingSpreadsheet.getWorkingSpreadsheetDocument();

        var vorhandeneSchluessel = new ArrayList<String>();

        // Einzel-Turnierbaum (kein Buchstaben-Suffix)
        var einzelSchluessel = SheetMetadataHelper.schluesselKoTurnierbaum("");
        if (SheetMetadataHelper.findeSheet(xDoc, einzelSchluessel).isPresent()) {
            vorhandeneSchluessel.add(einzelSchluessel);
        } else {
            // Mehrere Turnierbäume A, B, C …
            for (var buchstabe : TURNIERBAUM_BUCHSTABEN) {
                var schluessel = SheetMetadataHelper.schluesselKoTurnierbaum(buchstabe);
                if (SheetMetadataHelper.findeSheet(xDoc, schluessel).isEmpty()) {
                    break;
                }
                vorhandeneSchluessel.add(schluessel);
            }
        }

        if (vorhandeneSchluessel.isEmpty()) {
            return new KoTurnierSchritt(false, false);
        }

        boolean beendet = pruefeAlleBeendet(xDoc, vorhandeneSchluessel);
        return new KoTurnierSchritt(true, beendet);
    }

    private boolean pruefeAlleBeendet(com.sun.star.sheet.XSpreadsheetDocument xDoc, List<String> schluessel) {
        for (var s : schluessel) {
            if (!pruefeBaumBeendet(xDoc, s)) {
                return false;
            }
        }
        return true;
    }

    private boolean pruefeBaumBeendet(com.sun.star.sheet.XSpreadsheetDocument xDoc, String baumSchluessel) {
        var scoreKey = SheetMetadataHelper.scoreSchluessel(baumSchluessel);
        var scoreText = SheetMetadataHelper.leseScoreText(xDoc, scoreKey);
        if (scoreText == null || !scoreText.startsWith(SCORE_PRAEFIX)) {
            return false;
        }
        var positionenStr = scoreText.substring(SCORE_PRAEFIX.length());
        if (positionenStr.isBlank()) {
            return false;
        }
        var positionen = positionenStr.split("\\|");
        var sheet = SheetMetadataHelper.findeSheet(xDoc, baumSchluessel);
        if (sheet.isEmpty()) {
            return false;
        }
        try {
            for (var pos : positionen) {
                if (pos.isBlank()) {
                    continue;
                }
                var teile = pos.split(",");
                if (teile.length < 2) {
                    continue;
                }
                int spalte = Integer.parseInt(teile[0].trim());
                int zeile = Integer.parseInt(teile[1].trim());
                var zelle = sheet.get().getCellByPosition(spalte, zeile);
                if (zelle.getType() == com.sun.star.table.CellContentType.EMPTY) {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Prüfen der KO-Turnierbaum-Scores für '{}'", baumSchluessel, e);
            return false;
        }
        return positionen.length > 0;
    }
}
