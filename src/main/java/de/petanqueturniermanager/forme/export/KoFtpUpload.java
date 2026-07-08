/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.forme.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;

public class KoFtpUpload extends AbstractFtpUpload {

    public KoFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.KO, "Ko FTP Upload",
                zielVerzeichnis -> new KoExportInVerzeichnis(ws, zielVerzeichnis));
    }
}
