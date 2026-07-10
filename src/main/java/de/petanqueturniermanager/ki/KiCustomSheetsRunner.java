/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCell;
import com.sun.star.text.XText;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

final class KiCustomSheetsRunner extends SheetRunner implements ISheet {

    private final List<CustomSheetDaten> sheets;
    private XSpreadsheet aktuellesSheet;

    KiCustomSheetsRunner(WorkingSpreadsheet workingSpreadsheet, List<CustomSheetDaten> sheets) {
        super(workingSpreadsheet, TurnierSystem.KEIN, "KI Zusatz-Sheets");
        this.sheets = List.copyOf(sheets);
    }

    @Override
    protected void doRun() throws GenerateException {
        for (CustomSheetDaten sheetDaten : sheets) {
            NewSheet.from(this, sheetDaten.name(), "KI_" + sheetDaten.name())
                    .newIfExist()
                    .showGrid()
                    .setActiv()
                    .create();
            aktuellesSheet = getSheetHelper().findByName(sheetDaten.name());
            schreibe(sheetDaten);
        }
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    private void schreibe(CustomSheetDaten sheetDaten) throws GenerateException {
        int rowIndex = 0;
        for (List<String> row : sheetDaten.rows()) {
            int colIndex = 0;
            for (String value : row) {
                XCell cell = getSheetHelper().getCell(aktuellesSheet, Position.from(colIndex, rowIndex));
                XText text = de.petanqueturniermanager.helper.Lo.qi(XText.class, cell);
                if (text != null) {
                    text.setString(value);
                }
                colIndex++;
            }
            rowIndex++;
        }
    }

    @Override
    public XSpreadsheet getXSpreadSheet() {
        return aktuellesSheet;
    }

    @Override
    public Logger getLogger() {
        return super.getLogger();
    }

    @Override
    public TurnierSheet getTurnierSheet() throws GenerateException {
        return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
    }
}
