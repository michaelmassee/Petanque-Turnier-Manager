/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;

public class SupermeleeFtpUpload extends AbstractFtpUpload {

    public SupermeleeFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.SUPERMELEE, "Supermelee FTP Upload");
    }

    @Override
    protected IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException {
        return new SuperMeleeKonfigurationSheet(getWorkingSpreadsheet());
    }
}
