/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt eine Formule X Meldeliste mit Testdaten (ohne Dialog).<br>
 * Kann alleinstehend verwendet werden oder von einer übergeordneten
 * TestDaten-Klasse aufgerufen werden.
 */
public class FormuleXMeldeListeSheetTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

    private final int anzTeams;
    private final FormuleXListeDelegate delegate;
    private final FormuleXMeldeListeSheetNew meldeListeNew;
    private final TestnamenLoader testnamenLoader;

    public FormuleXMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
        super(workingSpreadsheet, TurnierSystem.FORMULEX, "Formule X Meldeliste Testdaten");
        this.anzTeams = anzTeams;
        delegate = new FormuleXListeDelegate(this);
        meldeListeNew = new FormuleXMeldeListeSheetNew(workingSpreadsheet);
        testnamenLoader = new TestnamenLoader();
    }

    @Override
    public XSpreadsheet getXSpreadSheet() throws GenerateException {
        return getSheetHelper().findByName(SheetNamen.meldeliste());
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }

    @Override
    protected FormuleXKonfigurationSheet getKonfigurationSheet() {
        return delegate.getKonfigurationSheet();
    }

    /**
     * Füllt die Meldeliste mit Testnamen und führt danach ein Update durch.
     * Wird auch von übergeordneten TestDaten-Klassen aufgerufen.
     */
    public void erstelleMeldelisteWithTestdaten() throws GenerateException {
        getSheetHelper().removeAllSheetsExclude();

        getKonfigurationSheet().update();
        meldeListeNew.createMeldelisteWithParams(Formation.TRIPLETTE, true, false, 4);

        teamNamenEinfuegen();

        new FormuleXMeldeListeSheetUpdate(getWorkingSpreadsheet()).doRun();
    }

    @Override
    protected void doRun() throws GenerateException {
        if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.FORMULEX)
                .prefix(getLogPrefix()).validate()) {
            return;
        }
        erstelleMeldelisteWithTestdaten();
    }

    // ---------------------------------------------------------------

    private void teamNamenEinfuegen() throws GenerateException {
        var konfiguration = delegate.getKonfigurationSheet();
        int anzSpieler = konfiguration.getMeldeListeFormation().getAnzSpieler();
        boolean teamnameAktiv = konfiguration.isMeldeListeTeamnameAnzeigen();
        boolean vereinsnameAktiv = konfiguration.isMeldeListeVereinsnameAnzeigen();
        var spieler = testnamenLoader.listeMitSpielerTestNamen(anzTeams * anzSpieler);

        var data = new RangeData();
        for (int i = 0; i < anzTeams; i++) {
            testDoCancelTask();
            var zeile = data.addNewRow();
            if (teamnameAktiv) {
                zeile.newString("Team " + (i + 1));
            }
            for (int s = 0; s < anzSpieler; s++) {
                var stn = spieler.get(i * anzSpieler + s);
                zeile.newString(stn.vorname());
                zeile.newString(stn.nachname());
                if (vereinsnameAktiv) {
                    zeile.newString("Verein " + ((i % 5) + 1));
                }
            }
            zeile.newEmpty(); // Setzposition
            zeile.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);
        }

        var xSheet = meldeListeNew.getXSpreadSheet();
        var startPos = Position.from(1, FormuleXListeDelegate.ERSTE_DATEN_ZEILE);
        RangeHelper.from(xSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
                data.getRangePosition(startPos)).setDataInRange(data);
    }

}
