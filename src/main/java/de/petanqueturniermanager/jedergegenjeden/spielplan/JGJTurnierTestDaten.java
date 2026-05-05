package de.petanqueturniermanager.jedergegenjeden.spielplan;


import de.petanqueturniermanager.helper.random.RandomSource;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheetTestDaten;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Generiert ein vollständiges JGJ-Beispielturnier:
 * Meldeliste + Spielplan mit Zufallsergebnissen + Rangliste.
 * Subklassen können Formation und Teamanzahl über Template-Methoden überschreiben.
 */
public class JGJTurnierTestDaten extends JGJSpielPlanSheet {

    private static final int ANZ_TEAMS_DEFAULT = 10;

    public JGJTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
    }

    protected Formation getFormation() {
        return Formation.TETE;
    }

    protected int getAnzTeams() {
        return ANZ_TEAMS_DEFAULT;
    }

    protected int getGruppengroesse() {
        return 0;
    }

    /**
     * Öffentlicher Einstiegspunkt für Tests: generiert das vollständige JGJ-Turnier
     * ohne Dialoge direkt auf dem aktuellen Dokument.
     */
    public void generate() throws GenerateException {
        doRun();
    }

    @Override
    protected void doRun() throws GenerateException {
        if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.JGJ)
                .prefix(getLogPrefix()).validate()) {
            return;
        }
        getSheetHelper().removeAllSheetsExclude(new String[] {});

        // 1. Meldeliste erstellen und mit Testnamen befüllen
        var meldeListeTestDaten = new JGJMeldeListeSheetTestDaten(
                getWorkingSpreadsheet(), getFormation(), getAnzTeams(), getGruppengroesse());
        meldeListeTestDaten.erstellenUndBefuellen();

        // 2. Spielplan generieren
        TeamMeldungen aktiveMeldungen = meldeListeTestDaten.getAktiveMeldungen();
        generate(aktiveMeldungen);

        // 3. Zufallsergebnisse einfügen
        processBoxinfo("processbox.jgj.testdaten.ergebnisse");
        ergebnisseEinfuegen();

        // 4. Rangliste erstellen
        processBoxinfo("processbox.erstelle.rangliste");
        new JGJRanglisteSheet(getWorkingSpreadsheet()).upDateSheet();

        // 5. Kopfzeile und Werbefußzeile setzen
        getKonfigurationSheet().setKopfZeileMitte(getTurnierSystem().getBezeichnung());
        getKonfigurationSheet().seitenstileAktualisieren();

        // 6. Rangliste als aktives Sheet setzen
        var ranglisteSheet = new JGJRanglisteSheet(getWorkingSpreadsheet());
        TurnierSheet.from(ranglisteSheet.getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
    }

    private void ergebnisseEinfuegen() throws GenerateException {
        XSpreadsheet sheet = getXSpreadSheet();
        RangePosition leseBereich = RangePosition.from(
                TEAM_A_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE,
                TEAM_B_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE + 500);
        RangeData data = RangeHelper
                .from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), leseBereich)
                .getDataFromRange();

        for (int i = 0; i < data.size(); i++) {
            RowData zeile = data.get(i);
            if (zeile.size() < 2) break;

            int nrA = zeile.get(0).getIntVal(0);
            if (nrA <= 0) continue; // Gruppenheader-Zeile oder leere Zeile überspringen

            int nrB = zeile.get(1).getIntVal(0);
            if (nrB <= 0) continue; // Freispiel – kein Ergebnis nötig

            int spielZeile = ERSTE_SPIELTAG_DATEN_ZEILE + i;
            int gewinner = RandomSource.nextInt(2);
            int verliererPunkte = RandomSource.nextInt(0, 13);
            int ergA = (gewinner == 0) ? 13 : verliererPunkte;
            int ergB = (gewinner == 0) ? verliererPunkte : 13;

            getSheetHelper().setNumberValueInCell(
                    NumberCellValue.from(sheet, Position.from(SPIELPNKT_A_SPALTE, spielZeile)).setValue(ergA));
            getSheetHelper().setNumberValueInCell(
                    NumberCellValue.from(sheet, Position.from(SPIELPNKT_B_SPALTE, spielZeile)).setValue(ergB));
        }
    }

}
