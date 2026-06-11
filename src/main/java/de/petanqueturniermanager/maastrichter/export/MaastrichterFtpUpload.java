/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.export;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.AbstractFtpUpload;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;

public class MaastrichterFtpUpload extends AbstractFtpUpload {

    public MaastrichterFtpUpload(WorkingSpreadsheet ws) {
        super(ws, TurnierSystem.MAASTRICHTER, "Maastrichter FTP Upload",
                zielVerzeichnis -> new MaastrichterExportInVerzeichnis(ws, zielVerzeichnis));
    }

    @Override
    protected IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException {
        return new MaastrichterKonfigurationSheet(getWorkingSpreadsheet());
    }
}
