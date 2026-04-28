/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import java.util.List;

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
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
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

    public FormuleXMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
        super(workingSpreadsheet, TurnierSystem.FORMULEX, "Formule X Meldeliste Testdaten");
        this.anzTeams = anzTeams;
        delegate = new FormuleXListeDelegate(this);
        meldeListeNew = new FormuleXMeldeListeSheetNew(workingSpreadsheet);
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
        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        getSheetHelper().setActiveSheet(xSheet);

        Formation formation = delegate.getKonfigurationSheet().getMeldeListeFormation();
        int anzSpieler = formation.getAnzSpieler();
        int ersteDatenZeile = meldeListeNew.getErsteDatenZeile();

        List<String> namen = new TestnamenLoader().listeMitTestNamen(anzTeams * anzSpieler);

        boolean teamnameAktiv = delegate.getKonfigurationSheet().isMeldeListeTeamnameAnzeigen();
        int teamnameSpalte = delegate.getTeamnameSpalte();

        for (int i = 0; i < anzTeams; i++) {
            SheetRunner.testDoCancelTask();
            int zeile = ersteDatenZeile + i;

            if (teamnameAktiv && teamnameSpalte >= 0) {
                getSheetHelper().setStringValueInCell(
                        StringCellValue.from(xSheet, Position.from(teamnameSpalte, zeile), "Team " + (i + 1)));
            }

            for (int s = 0; s < anzSpieler; s++) {
                int nameIndex = i * anzSpieler + s;
                if (nameIndex >= namen.size()) {
                    break;
                }
                String[] parts = namen.get(nameIndex).split(", ", 2);
                String vorname = parts.length > 1 ? parts[1] : parts[0];
                String nachname = parts.length > 1 ? parts[0] : "";

                getSheetHelper().setStringValueInCell(
                        StringCellValue.from(xSheet, Position.from(delegate.getVornameSpalte(s), zeile), vorname));
                getSheetHelper().setStringValueInCell(
                        StringCellValue.from(xSheet, Position.from(delegate.getNachnameSpalte(s), zeile), nachname));
            }

            getSheetHelper().setNumberValueInCell(
                    NumberCellValue.from(xSheet, Position.from(delegate.getAktivSpalte(), zeile),
                            FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL));
        }
    }

}
