/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;

public class FormuleXFtpUpload extends AbstractFtpUpload {

    public FormuleXFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.FORMULEX, "FormuleX FTP Upload");
    }

    @Override
    protected IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException {
        return new FormuleXKonfigurationSheet(getWorkingSpreadsheet());
    }
}
