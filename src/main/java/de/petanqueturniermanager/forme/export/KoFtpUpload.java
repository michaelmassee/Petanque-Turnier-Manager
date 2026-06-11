/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.forme.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;

public class KoFtpUpload extends AbstractFtpUpload {

    public KoFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.KO, "Ko FTP Upload",
                zielVerzeichnis -> new KoExportInVerzeichnis(ws, zielVerzeichnis));
    }

    @Override
    protected IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException {
        return new KoKonfigurationSheet(getWorkingSpreadsheet());
    }
}
