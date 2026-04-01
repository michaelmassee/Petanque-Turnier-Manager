/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.vorrunde;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.algorithmen.PouleGruppenRechner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Erstellt für jede Poule-Gruppe ein eigenes Spielplan-Sheet.
 * <p>
 * Jedes Sheet enthält nur die Spiele der jeweiligen Gruppe und kann
 * als Aushang an die Gruppenverantwortlichen weitergegeben werden.
 */
public class PouleSpielplaeneSheet extends AbstractPouleVorrundeSheet {

    public PouleSpielplaeneSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, "Poule-Spielplaene");
    }

    private XSpreadsheet aktuellesPouleSheet;

    /**
     * Liefert das aktuell in Bearbeitung befindliche Poule-Sheet.
     * Wird in {@link #erstellePouleSpielplanSheet} gesetzt, bevor Formatierungsmethoden
     * aufgerufen werden, die intern {@code ISheet.getXSpreadSheet()} benötigen.
     */
    @Override
    public XSpreadsheet getXSpreadSheet() {
        return aktuellesPouleSheet;
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    public void doRun() throws GenerateException {
        processBoxinfo("processbox.poule.spielplaene.erstellen");
        meldeliste.upDateSheet();

        var meldungen = meldeliste.getAktiveMeldungen();
        int anzTeams = meldungen.size();

        if (anzTeams < 3) {
            MessageBox.from(getWorkingSpreadsheet(), MessageBoxTypeEnum.ERROR_OK)
                    .caption(I18n.get("msg.caption.fehler"))
                    .message(I18n.get("poule.vorrunde.fehler.zu.wenige.meldungen", anzTeams)).show();
            return;
        }

        var gruppenGroessen = PouleGruppenRechner.berechneGruppenGroessen(anzTeams);
        var poules = PouleSeedingService.verteileTeams(meldungen, gruppenGroessen);

        for (var poule : poules) {
            SheetRunner.testDoCancelTask();
            erstellePouleSpielplanSheet(poule);
        }
    }

    private void erstellePouleSpielplanSheet(PouleSeedingService.Poule poule) throws GenerateException {
        var sheetName = SheetNamen.pouleSpielplan(poule.pouleNr());
        var metaKey = SheetMetadataHelper.schluesselPouleSpielplan(poule.pouleNr());

        NewSheet.from(this, sheetName, metaKey)
                .tabColor(SHEET_COLOR)
                .pos(DefaultSheetPos.POULE_WORK)
                .forceCreate().hideGrid().create();

        aktuellesPouleSheet = SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                metaKey, sheetName);

        try {
            headerSchreiben(aktuellesPouleSheet);
            spaltenBreitenSetzen(aktuellesPouleSheet);

            int letzteDatenZeile;
            if (poule.teams().size() == 4) {
                schreibeViererPoule(aktuellesPouleSheet, poule, ERSTE_DATEN_ZEILE, 1);
                letzteDatenZeile = ERSTE_DATEN_ZEILE + VIERER_POULE_DATEN_ZEILEN - 1;
            } else {
                schreibeDreierPoule(aktuellesPouleSheet, poule, ERSTE_DATEN_ZEILE, 1);
                letzteDatenZeile = ERSTE_DATEN_ZEILE + DREIER_POULE_DATEN_ZEILEN - 1;
            }

            formatierungDurchfuehren(aktuellesPouleSheet, letzteDatenZeile);
            printBereichSetzen(aktuellesPouleSheet, letzteDatenZeile);

            if (SheetRunner.isRunning()) {
                SheetFreeze.from(aktuellesPouleSheet, getWorkingSpreadsheet()).anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();
            }
        } finally {
            aktuellesPouleSheet = null;
        }
    }
}
