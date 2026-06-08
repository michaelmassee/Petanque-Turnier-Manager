/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.poule.konfiguration.PouleKonfigurationSheet;

public class PouleFtpUpload extends AbstractFtpUpload {

    public PouleFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.POULE, "Poule FTP Upload");
    }

    @Override
    protected IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException {
        return new PouleKonfigurationSheet(getWorkingSpreadsheet());
    }
}
