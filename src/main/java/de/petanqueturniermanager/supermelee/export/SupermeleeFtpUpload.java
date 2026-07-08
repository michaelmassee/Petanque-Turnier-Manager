/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;

public class SupermeleeFtpUpload extends AbstractFtpUpload {

    public SupermeleeFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.SUPERMELEE, "Supermelee FTP Upload",
                zielVerzeichnis -> new SupermeleeExportInVerzeichnis(ws, zielVerzeichnis));
    }
}
