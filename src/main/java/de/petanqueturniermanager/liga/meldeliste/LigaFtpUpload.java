package de.petanqueturniermanager.liga.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;

public class LigaFtpUpload extends AbstractFtpUpload {

    public LigaFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.LIGA, "Liga FTP Upload");
    }

    @Override
    protected IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException {
        return new LigaKonfigurationSheet(getWorkingSpreadsheet());
    }
}
