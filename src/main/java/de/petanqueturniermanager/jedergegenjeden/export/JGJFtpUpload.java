/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;

public class JGJFtpUpload extends AbstractFtpUpload {

    public JGJFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.JGJ, "JGJ FTP Upload",
                zielVerzeichnis -> new JGJExportInVerzeichnis(ws, zielVerzeichnis));
    }
}
