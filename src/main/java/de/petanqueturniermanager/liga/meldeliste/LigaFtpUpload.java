package de.petanqueturniermanager.liga.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;

public class LigaFtpUpload extends AbstractFtpUpload {

    public LigaFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.LIGA, "Liga FTP Upload",
                zielVerzeichnis -> new LigaExportInVerzeichnis(ws, zielVerzeichnis));
    }
}
