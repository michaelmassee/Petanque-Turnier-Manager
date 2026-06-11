/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;

public class SchweizerFtpUpload extends AbstractFtpUpload {

    public SchweizerFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.SCHWEIZER, "Schweizer FTP Upload",
                zielVerzeichnis -> new SchweizerExportInVerzeichnis(ws, zielVerzeichnis));
    }

    @Override
    protected IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException {
        return new SchweizerKonfigurationSheet(getWorkingSpreadsheet());
    }
}
