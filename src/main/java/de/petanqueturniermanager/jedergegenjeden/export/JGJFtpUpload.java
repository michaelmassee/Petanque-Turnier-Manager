/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.jedergegenjeden.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;

public class JGJFtpUpload extends AbstractFtpUpload {

    public JGJFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.JGJ, "JGJ FTP Upload",
                zielVerzeichnis -> new JGJExportInVerzeichnis(ws, zielVerzeichnis));
    }

    @Override
    protected IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException {
        return new JGJKonfigurationSheet(getWorkingSpreadsheet());
    }
}
