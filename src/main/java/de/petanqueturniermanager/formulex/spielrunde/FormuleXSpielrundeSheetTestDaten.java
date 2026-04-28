/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetTestDaten;

/**
 * Erstellt Testdaten für das Formule X Turniersystem:
 * Meldeliste mit Testnamen + erste Spielrunde.
 */
public class FormuleXSpielrundeSheetTestDaten extends FormuleXAbstractSpielrundeSheet {

    private final FormuleXMeldeListeSheetTestDaten meldeListeTestDaten;
    private final FormuleXSpielrundeSheetNaechste spielrundeNaechste;

    public FormuleXSpielrundeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
        super(workingSpreadsheet);
        meldeListeTestDaten = new FormuleXMeldeListeSheetTestDaten(workingSpreadsheet, 16);
        spielrundeNaechste = new FormuleXSpielrundeSheetNaechste(workingSpreadsheet);
    }

    @Override
    protected void doRun() throws GenerateException {
        getSheetHelper().removeAllSheetsExclude();
        generate();
        getSheetHelper().setActiveSheet(getXSpreadSheet());
    }

    public void generate() throws GenerateException {
        meldeListeTestDaten.erstelleMeldelisteWithTestdaten();
        spielrundeNaechste.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R);
        spielrundeNaechste.doRun();
    }
}
