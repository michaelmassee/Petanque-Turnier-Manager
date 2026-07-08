/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;

public class MaastrichterFtpUpload extends AbstractFtpUpload {

    public MaastrichterFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.MAASTRICHTER, "Maastrichter FTP Upload",
                zielVerzeichnis -> new MaastrichterExportInVerzeichnis(ws, zielVerzeichnis));
    }
}
