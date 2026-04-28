package de.petanqueturniermanager.maastrichter;

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
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Liest den aktuellen Turnierstatus aus dem Maastrichter-Dokument.
 */
public class MaastrichterStatusLeser {

    private static final Logger logger = LogManager.getLogger(MaastrichterStatusLeser.class);
    private static final int MAX_ZEILEN = 2000;
    private static final List<String> FINALRUNDE_BUCHSTABEN = List.of("A", "B", "C", "D");
    private static final String SCORE_PRAEFIX = "PTM_EDIT:";
    private static final String POSITIONS_TRENNZEICHEN = "\\|";
    private static final String KOORDINATEN_TRENNZEICHEN = ",";

    private final WorkingSpreadsheet workingSpreadsheet;

    private MaastrichterStatusLeser(WorkingSpreadsheet workingSpreadsheet) {
        this.workingSpreadsheet = workingSpreadsheet;
    }

    public static MaastrichterStatusLeser von(WorkingSpreadsheet workingSpreadsheet) {
        return new MaastrichterStatusLeser(workingSpreadsheet);
    }

    public MaastrichterTurnierSchritt liesStatus() {
        var xDoc = workingSpreadsheet.getWorkingSpreadsheetDocument();

        int hoechsteVorrunde = 0;
        XSpreadsheet hoechsteVorrundeSheet = null;
        for (int n = 1; n < 1000; n++) {
            var schluessel = SheetMetadataHelper.schluesselMaastrichterVorrunde(n);
            var sheetOpt = SheetMetadataHelper.findeSheet(xDoc, schluessel);
            if (sheetOpt.isEmpty()) {
                break;
            }
            hoechsteVorrunde = n;
            hoechsteVorrundeSheet = sheetOpt.get();
        }

        if (hoechsteVorrundeSheet == null) {
            return new MaastrichterTurnierSchritt(false, 0, 0, 0, false, false);
        }

        int[] zaehler = zaehleVorrundeSpiele(hoechsteVorrundeSheet);

        var vorhandeneFinalrundenKeys = new ArrayList<String>();
        for (var b : FINALRUNDE_BUCHSTABEN) {
            var schluessel = SheetMetadataHelper.schluesselMaastrichterFinalrunde(b);
            if (SheetMetadataHelper.findeSheet(xDoc, schluessel).isPresent()) {
                vorhandeneFinalrundenKeys.add(schluessel);
            }
        }

        if (vorhandeneFinalrundenKeys.isEmpty()) {
            return new MaastrichterTurnierSchritt(true, hoechsteVorrunde, zaehler[0], zaehler[1], false, false);
        }

        boolean beendet = pruefeAlleFinalrundenBeendet(xDoc, vorhandeneFinalrundenKeys);
        return new MaastrichterTurnierSchritt(true, hoechsteVorrunde, zaehler[0], zaehler[1], true, beendet);
    }

    private int[] zaehleVorrundeSpiele(XSpreadsheet sheet) {
        int gesamt = 0;
        int gespielt = 0;
        try {
            for (int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE; zeile < MAX_ZEILEN; zeile++) {
                var teamZelle = sheet.getCellByPosition(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, zeile);
                if (teamZelle.getType() == CellContentType.EMPTY) {
                    break;
                }
                gesamt++;
                var ergZelle = sheet.getCellByPosition(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile);
                if (ergZelle.getType() == CellContentType.VALUE) {
                    gespielt++;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            logger.error("Fehler beim Lesen der Maastrichter-Vorrunde-Spiele", e);
        }
        return new int[] { gespielt, gesamt };
    }

    private boolean pruefeAlleFinalrundenBeendet(XSpreadsheetDocument xDoc, List<String> finalrundenKeys) {
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
            logger.error("Fehler beim Prüfen der Maastrichter-Finalrunden-Scores", e);
            return false;
        }
        return positionen.length > 0;
    }
}
