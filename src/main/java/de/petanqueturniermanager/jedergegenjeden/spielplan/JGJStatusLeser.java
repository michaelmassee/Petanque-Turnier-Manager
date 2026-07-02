package de.petanqueturniermanager.jedergegenjeden.spielplan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
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
    private static final String SCORE_PRAEFIX = "PTM_EDIT:";
    private static final String POSITIONS_TRENNZEICHEN = "\\|";
    private static final String KOORDINATEN_TRENNZEICHEN = ",";

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
        JGJTurnierSchritt spielplanStatus = zaehleSpiele(sheet);
        var finalrundenKeys = SheetMetadataHelper.getSchluesselMitPrefix(
                xDoc, SheetMetadataHelper.SCHLUESSEL_JGJ_FINALRUNDE_PREFIX);
        if (finalrundenKeys.length == 0) {
            return spielplanStatus;
        }
        boolean beendet = pruefeAlleFinalrundenBeendet(xDoc, finalrundenKeys);
        return new JGJTurnierSchritt(true,
                spielplanStatus.hrGespielt(), spielplanStatus.hrGesamt(),
                spielplanStatus.rrGespielt(), spielplanStatus.rrGesamt(),
                true, beendet);
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

                // "gespielt" an den Ergebnis-Eingabespalten erkennen, NICHT an der Arbeitsspalte
                // SPIELE_A_SPALTE: die enthält die Formel WENN(PkA>PkB;1;0) und ist daher auch bei
                // leerem Ergebnis nie leer (immer 0) → würde jedes Spiel als gespielt zählen.
                // Ein Ergebnis liegt vor, sobald eine der Punkte-Eingaben gefüllt ist (analog
                // JGJRanglisteRechner: pktA>0 || pktB>0; Freispiele sind vorbelegt).
                boolean gespielt = sheet.getCellByPosition(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE, zeile)
                        .getType() != CellContentType.EMPTY
                        || sheet.getCellByPosition(JGJSpielPlanSheet.SPIELPNKT_B_SPALTE, zeile)
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

    private boolean pruefeAlleFinalrundenBeendet(XSpreadsheetDocument xDoc, String[] finalrundenKeys) {
        for (var finalrundeKey : finalrundenKeys) {
            var finalrundeSheet = SheetMetadataHelper.findeSheet(xDoc, finalrundeKey);
            if (finalrundeSheet.isEmpty() || !pruefeFinalrundeBeendet(xDoc, finalrundeSheet.get(), finalrundeKey)) {
                return false;
            }
        }
        return true;
    }

    private boolean pruefeFinalrundeBeendet(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String finalrundeKey) {
        var scoreKey = SheetMetadataHelper.scoreSchluessel(finalrundeKey);
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
                var zelle = sheet.getCellByPosition(spalte, zeile);
                if (zelle.getType() != CellContentType.VALUE) {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Prüfen der JGJ-Finalrunden-Scores", e);
            return false;
        }
        return positionen.length > 0;
    }
}
