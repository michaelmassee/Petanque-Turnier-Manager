/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.ExportFormat;

public class PouleFtpUpload extends AbstractFtpUpload {

    public PouleFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.POULE, "Poule FTP Upload",
                zielVerzeichnis -> new PouleExportInVerzeichnis(ws, zielVerzeichnis, ExportFormat.HTML_UND_PDFS));
    }
}
