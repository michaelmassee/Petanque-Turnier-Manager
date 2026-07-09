/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.ExportFormat;

public class FormuleXFtpUpload extends AbstractFtpUpload {

    public FormuleXFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.FORMULEX, "FormuleX FTP Upload",
                zielVerzeichnis -> new FormuleXExportInVerzeichnis(ws, zielVerzeichnis, ExportFormat.HTML_UND_PDFS));
    }
}
