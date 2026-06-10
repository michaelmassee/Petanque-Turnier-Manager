package de.petanqueturniermanager.helper.upload;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;

public abstract class AbstractFtpUpload extends SheetRunner {

    private static final Logger logger = LogManager.getLogger(AbstractFtpUpload.class);

    protected AbstractFtpUpload(WorkingSpreadsheet ws, TurnierSystem ts, String name) {
        super(ws, ts, name);
    }

    @Override
    protected final void doRun() throws GenerateException {
        var ergebnisOpt = ExportErgebnis.laden(getWorkingSpreadsheet());
        if (ergebnisOpt.isEmpty()) {
            throw new GenerateException(I18n.get("ftp.upload.fehler.kein.export"));
        }

        var konfigurierbar = getUploadKonfigurierbar();
        var konfig = new UploadKonfiguration(
                konfigurierbar.getUploadProtokoll(),
                konfigurierbar.getUploadHost(),
                konfigurierbar.getUploadPort(),
                konfigurierbar.getUploadBenutzer(),
                konfigurierbar.getUploadVerzeichnis());

        if (!konfig.istVollstaendig()) {
            throw new GenerateException(I18n.get("ftp.upload.fehler.konfiguration"));
        }

        String passwort = GlobalProperties.get().getUploadPasswort(konfig.host());
        if (passwort.isEmpty()) {
            var eingabe = PasswortEingabeDialog.zeigen(getWorkingSpreadsheet(), konfig.host());
            if (eingabe.isEmpty()) {
                throw new GenerateException(I18n.get("upload.passwort.abgebrochen"));
            }
            passwort = eingabe.get();
        }

        try {
            int anzahl = UploadServiceFactory.erstelle(konfig)
                    .hochladen(ergebnisOpt.get().exportierteDateien(), passwort);
            processBox().info(I18n.get("ftp.upload.erfolg", anzahl));
        } catch (IOException e) {
            logger.error("FTP/SFTP-Upload fehlgeschlagen", e);
            throw new GenerateException(e.getMessage());
        }
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    protected abstract IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException;
}
