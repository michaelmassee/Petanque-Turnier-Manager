/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.triptete.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.ExportFormat;

public class TripTeteFtpUpload extends AbstractFtpUpload {

    public TripTeteFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.TRIPTETE, "TripTete FTP Upload",
                zielVerzeichnis -> new TripTeteExportInVerzeichnis(ws, zielVerzeichnis, ExportFormat.HTML_UND_PDFS));
    }
}
