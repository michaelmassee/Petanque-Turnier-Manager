/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;

public class KaskadeFtpUpload extends AbstractFtpUpload {

    public KaskadeFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.KASKADE, "Kaskade FTP Upload",
                zielVerzeichnis -> new KaskadeExportInVerzeichnis(ws, zielVerzeichnis));
    }
}
