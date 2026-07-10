/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

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
        RangeData rangeData = new RangeData();
        for (List<String> row : sheetDaten.rows()) {
            RowData rowData = new RowData();
            for (String value : row) {
                rowData.newString(value);
            }
            rangeData.add(rowData);
        }
        if (rangeData.isEmpty()) {
            return;
        }
        Position startPos = Position.from(0, 0);
        RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
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
