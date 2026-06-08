/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;

public class KaskadeFtpUpload extends AbstractFtpUpload {

    public KaskadeFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.KASKADE, "Kaskade FTP Upload");
    }

    @Override
    protected IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException {
        return new KaskadeKonfigurationSheet(getWorkingSpreadsheet());
    }
}
