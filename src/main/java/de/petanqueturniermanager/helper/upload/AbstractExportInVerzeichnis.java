package de.petanqueturniermanager.helper.upload;

import java.nio.file.Path;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

public abstract class AbstractExportInVerzeichnis extends SheetRunner {

    private final Path zielVerzeichnis;

    protected AbstractExportInVerzeichnis(WorkingSpreadsheet ws, TurnierSystem ts, String name,
            Path zielVerzeichnis) {
        super(ws, ts, name);
        this.zielVerzeichnis = zielVerzeichnis;
    }

    @Override
    protected final void doRun() throws GenerateException {
        var ergebnis = exportiereInVerzeichnis(zielVerzeichnis);
        ergebnis.speichern(getWorkingSpreadsheet());
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    protected abstract ExportErgebnis exportiereInVerzeichnis(Path zielVerzeichnis) throws GenerateException;
}
