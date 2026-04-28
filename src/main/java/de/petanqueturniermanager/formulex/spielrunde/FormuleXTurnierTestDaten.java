/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import java.util.concurrent.ThreadLocalRandom;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetTestDaten;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXTeilnehmerSheet;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Generiert ein vollständiges Formule X Beispielturnier:
 * Meldeliste (39 Teams) + 5 Spielrunden mit Zufallsergebnissen + Rangliste.
 */
public class FormuleXTurnierTestDaten extends FormuleXAbstractSpielrundeSheet {

    public static final int ANZ_TEAMS   = 39;
    public static final int ANZ_RUNDEN  = 5;

    private final FormuleXMeldeListeSheetTestDaten meldelisteTestDaten;
    private final FormuleXSpielrundeSheetNaechste  naechsteSpielrunde;
    private final FormuleXRanglisteSheet           ranglisteSheet;
    private final FormuleXTeilnehmerSheet          teilnehmerSheet;

    public FormuleXTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
        meldelisteTestDaten = new FormuleXMeldeListeSheetTestDaten(workingSpreadsheet, ANZ_TEAMS);
        naechsteSpielrunde  = new FormuleXSpielrundeSheetNaechste(workingSpreadsheet);
        ranglisteSheet      = new FormuleXRanglisteSheet(workingSpreadsheet);
        teilnehmerSheet     = new FormuleXTeilnehmerSheet(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        if (!NewTestDatenValidator
                .from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.FORMULEX)
                .prefix(getLogPrefix()).validate()) {
            return;
        }
        getSheetHelper().removeAllSheetsExclude();
        generate();
        getSheetHelper().setActiveSheet(ranglisteSheet.getXSpreadSheet());
    }

    public void generate() throws GenerateException {
        // 1. Meldeliste erstellen
        meldelisteTestDaten.erstelleMeldelisteWithTestdaten();
        naechsteSpielrunde.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R);

        naechsteSpielrunde.getKonfigurationSheet().setAnzahlRunden(ANZ_RUNDEN);

        // 2. Spielrunden erstellen und mit Zufallsergebnissen füllen
        for (int runde = 1; runde <= ANZ_RUNDEN; runde++) {
            SheetRunner.testDoCancelTask();
            processBoxinfo("processbox.erstelle.spielrunde", runde, ANZ_RUNDEN);
            naechsteSpielrunde.doRun();

            XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(
                    getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                    getSpielrundeSchluessel(runde),
                    getSheetName(SpielRundeNr.from(runde)));
            if (sheet != null) {
                ergebnisseEinfuegen(sheet);
            }
        }

        // 3. Rangliste erstellen
        SheetRunner.testDoCancelTask();
        ranglisteSheet.doRun();

        // 4. Teilnehmer erstellen
        SheetRunner.testDoCancelTask();
        teilnehmerSheet.generate();

        // 5. Seitenstile aktualisieren
        naechsteSpielrunde.getKonfigurationSheet().seitenstileAktualisieren();
    }

    private void ergebnisseEinfuegen(XSpreadsheet sheet) throws GenerateException {
        var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
        RangePosition readRange = RangePosition.from(
                TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE, ERSTE_DATEN_ZEILE + 100);
        RangeData data = RangeHelper.from(sheet, xDoc, readRange).getDataFromRange();

        RangeData ergebnisData = new RangeData();
        for (RowData row : data) {
            if (row.size() < 2) {
                break;
            }
            int nrA = row.get(0).getIntVal(0);
            if (nrA <= 0) {
                break;
            }
            int nrB = row.get(1).getIntVal(0);
            RowData ergebnisZeile = ergebnisData.addNewRow();
            if (nrB > 0) {
                int winner = ThreadLocalRandom.current().nextInt(2);
                int loserPts = ThreadLocalRandom.current().nextInt(0, 13);
                ergebnisZeile.add(new CellData(winner == 0 ? 13 : loserPts));
                ergebnisZeile.add(new CellData(winner == 0 ? loserPts : 13));
            } else {
                ergebnisZeile.add(new CellData(0));
                ergebnisZeile.add(new CellData(0));
            }
        }

        if (!ergebnisData.isEmpty()) {
            Position startPos = Position.from(ERG_TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
            RangeHelper.from(sheet, xDoc, ergebnisData.getRangePosition(startPos)).setDataInRange(ergebnisData);
        }
    }
}
