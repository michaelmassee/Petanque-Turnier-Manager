/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.ExportFormat;

public class SchweizerFtpUpload extends AbstractFtpUpload {

    public SchweizerFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.SCHWEIZER, "Schweizer FTP Upload",
                zielVerzeichnis -> new SchweizerExportInVerzeichnis(ws, zielVerzeichnis, ExportFormat.HTML_UND_PDFS));
    }
}
