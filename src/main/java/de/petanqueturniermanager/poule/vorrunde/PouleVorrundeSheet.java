/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.vorrunde;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.PouleGruppenRechner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

/**
 * Erstellt das Poule-Vorrunde-Sheet für das Poule-A/B-Turniersystem.
 * <p>
 * Zeigt alle Poule-Gruppen mit ihren Spielen und Ergebnisfeldern in einem einzigen Sheet.
 * Formeln berechnen für Runden 2 und 3 automatisch die Sieger/Verlierer.
 */
public class PouleVorrundeSheet extends AbstractPouleVorrundeSheet implements ISheet {

    public PouleVorrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet, "Poule-Vorrunde");
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return SheetMetadataHelper.findeSheetUndHeile(
                getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE,
                SheetNamen.pouleVorrunde());
    }

    @Override
    public final TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    public void doRun() throws GenerateException {
        processBoxinfo("processbox.poule.vorrunde.erstellen");
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

        NewSheet.from(this, SheetNamen.pouleVorrunde(), SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDE)
                .tabColor(getKonfigurationSheet().getPouleVorrundeTabFarbe()).pos(DefaultSheetPos.POULE_WORK)
                .forceCreate().hideGrid().create();

        var xSheet = getXSpreadSheet();

        headerSchreiben(xSheet);
        spaltenBreitenSetzen(xSheet);

        int aktuelleZeile = ERSTE_DATEN_ZEILE;

        for (var poule : poules) {
            SheetRunner.testDoCancelTask();
            if (poule.teams().size() == 4) {
                schreibeViererPoule(xSheet, poule, aktuelleZeile, aktuelleZeile);
                aktuelleZeile += VIERER_POULE_DATEN_ZEILEN + SPACER_ZEILEN;
            } else {
                schreibeDreierPoule(xSheet, poule, aktuelleZeile, aktuelleZeile);
                aktuelleZeile += DREIER_POULE_DATEN_ZEILEN + SPACER_ZEILEN;
            }
        }

        int letzteDatenZeile = aktuelleZeile - SPACER_ZEILEN - 1;
        formatierungDurchfuehren(xSheet, letzteDatenZeile);
        printBereichSetzen(xSheet, letzteDatenZeile);

        if (SheetRunner.isRunning()) {
            SheetFreeze.from(xSheet, getWorkingSpreadsheet()).anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();
        }
    }
}
