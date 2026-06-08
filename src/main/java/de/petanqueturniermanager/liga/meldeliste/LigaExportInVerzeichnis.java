package de.petanqueturniermanager.liga.meldeliste;

import java.nio.file.Path;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractExportInVerzeichnis;
import de.petanqueturniermanager.helper.upload.ExportErgebnis;

public class LigaExportInVerzeichnis extends AbstractExportInVerzeichnis {

    public LigaExportInVerzeichnis(WorkingSpreadsheet ws, Path zielVerzeichnis) {
        super(ws, TurnierSystem.LIGA, "Liga Export Verzeichnis", zielVerzeichnis);
    }

    @Override
    protected ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException {
        return new LigaMeldeListeSheetExport(getWorkingSpreadsheet()).exportiere(zielVerzeichnis);
    }
}
