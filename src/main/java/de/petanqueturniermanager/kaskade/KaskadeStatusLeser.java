package de.petanqueturniermanager.kaskade;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellContentType;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet;

/**
 * Liest den aktuellen Turnierstatus aus dem Kaskaden-KO-Dokument.
 */
public class KaskadeStatusLeser {

    private static final Logger logger = LogManager.getLogger(KaskadeStatusLeser.class);
    private static final int MAX_ZEILEN = 2000;
    private static final List<String> KO_FELD_BUCHSTABEN = List.of("A", "B", "C", "D", "E", "F", "G", "H");
    private static final String SCORE_PRAEFIX = "PTM_EDIT:";
    private static final String POSITIONS_TRENNZEICHEN = "\\|";
    private static final String KOORDINATEN_TRENNZEICHEN = ",";

    private final WorkingSpreadsheet workingSpreadsheet;

    private KaskadeStatusLeser(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    public static KaskadeStatusLeser von(WorkingSpreadsheet workingSpreadsheet) {
        return new KaskadeStatusLeser(workingSpreadsheet);
    }

    public KaskadeTurnierSchritt liesStatus() {
        var xDoc = workingSpreadsheet.getWorkingSpreadsheetDocument();

        int hoechsteRunde = 0;
        XSpreadsheet hoechsteRundenSheet = null;
        for (int n = 1; n < 1000; n++) {
            var schluessel = SheetMetadataHelper.schluesselKaskadenRunde(n);
            var sheetOpt = SheetMetadataHelper.findeSheet(xDoc, schluessel);
            if (sheetOpt.isEmpty()) {
                break;
            }
            hoechsteRunde = n;
            hoechsteRundenSheet = sheetOpt.get();
        }

        if (hoechsteRundenSheet == null) {
            return new KaskadeTurnierSchritt(false, 0, 0, 0, false, false);
        }

        int[] zaehler = zaehleRundeSpiele(hoechsteRundenSheet);

        var vorhandeneFeldKeys = new ArrayList<String>();
        for (var b : KO_FELD_BUCHSTABEN) {
            var schluessel = SheetMetadataHelper.schluesselKaskadenFeld(b);
            if (SheetMetadataHelper.findeSheet(xDoc, schluessel).isPresent()) {
                vorhandeneFeldKeys.add(schluessel);
            }
        }

        if (vorhandeneFeldKeys.isEmpty()) {
            return new KaskadeTurnierSchritt(true, hoechsteRunde, zaehler[0], zaehler[1], false, false);
        }

        boolean beendet = pruefeAlleFelderBeendet(xDoc, vorhandeneFeldKeys);
        return new KaskadeTurnierSchritt(true, hoechsteRunde, zaehler[0], zaehler[1], true, beendet);
    }

    private int[] zaehleRundeSpiele(XSpreadsheet sheet) {
        int gesamt = 0;
        int gespielt = 0;
        try {
            for (int zeile = KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
                var teamZelle = sheet.getCellByPosition(KaskadeSpielrundeSheet.TEAM_A_SPALTE, zeile);
                if (teamZelle.getType() == CellContentType.EMPTY) {
                    break;
                }
                gesamt++;
                var ergZelle = sheet.getCellByPosition(KaskadeSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile);
                if (ergZelle.getType() == CellContentType.VALUE) {
                    gespielt++;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            logger.error("Fehler beim Lesen der Kaskaden-Runden-Spiele", e);
        }
        return new int[] { gespielt, gesamt };
    }

    private boolean pruefeAlleFelderBeendet(XSpreadsheetDocument xDoc, List<String> feldKeys) {
        for (var feldKey : feldKeys) {
            var feldSheet = SheetMetadataHelper.findeSheet(xDoc, feldKey);
            if (feldSheet.isEmpty() || !pruefeFeldBeendet(xDoc, feldSheet.get(), feldKey)) {
                return false;
            }
        }
        return true;
    }

    private boolean pruefeFeldBeendet(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String feldKey) {
        var scoreKey = SheetMetadataHelper.scoreSchluessel(feldKey);
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
            logger.error("Fehler beim Prüfen der Kaskaden-KO-Feld-Scores", e);
            return false;
        }
        return positionen.length > 0;
    }
}
