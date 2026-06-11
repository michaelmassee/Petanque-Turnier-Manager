package de.petanqueturniermanager.helper.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
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

    private final Function<Path, AbstractExportInVerzeichnis> exportFactory;

    protected AbstractFtpUpload(WorkingSpreadsheet ws, TurnierSystem ts, String name,
            Function<Path, AbstractExportInVerzeichnis> exportFactory) {
        super(ws, ts, name);
        this.exportFactory = exportFactory;
    }

    @Override
    protected final void doRun() throws GenerateException {
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

        Path tempVerzeichnis = erstelleTempVerzeichnis();
        try {
            var ergebnis = exportFactory.apply(tempVerzeichnis).exportiere();
            String passwort = GlobalProperties.get().getUploadPasswort(konfig.host());
            if (passwort.isEmpty()) {
                passwort = fragePasswort(konfig);
            }

            try {
                int anzahl = ladeHoch(konfig, passwort, ergebnis);
                processBox().info(I18n.get("ftp.upload.erfolg", anzahl));
            } catch (IOException e) {
                if (istAnmeldefehler(e)) {
                    try {
                        GlobalProperties.get().setUploadPasswort(konfig.host(), "");
                        String neuesPasswort = fragePasswort(konfig);
                        int anzahl = ladeHoch(konfig, neuesPasswort, ergebnis);
                        processBox().info(I18n.get("ftp.upload.erfolg", anzahl));
                        return;
                    } catch (IOException retryFehler) {
                        logger.error("FTP/SFTP-Upload nach erneuter Passwort-Eingabe fehlgeschlagen", retryFehler);
                        throw new GenerateException(retryFehler.getMessage());
                    }
                }
                logger.error("FTP/SFTP-Upload fehlgeschlagen", e);
                throw new GenerateException(e.getMessage());
            }
        } finally {
            try {
                FileUtils.deleteDirectory(tempVerzeichnis.toFile());
            } catch (IOException e) {
                logger.warn("Temporäres Exportverzeichnis konnte nicht gelöscht werden: {}", tempVerzeichnis, e);
            }
        }
    }

    private Path erstelleTempVerzeichnis() throws GenerateException {
        try {
            return Files.createTempDirectory("ptm-upload-export-");
        } catch (IOException e) {
            logger.error("Temporäres Exportverzeichnis für FTP/SFTP-Upload konnte nicht erstellt werden", e);
            throw new GenerateException(e.getMessage());
        }
    }

    private String fragePasswort(UploadKonfiguration konfig) throws GenerateException {
        var eingabe = PasswortEingabeDialog.zeigen(getWorkingSpreadsheet(), konfig.host());
        if (eingabe.isEmpty()) {
            throw new GenerateException(I18n.get("upload.passwort.abgebrochen"));
        }
        return eingabe.get();
    }

    private int ladeHoch(UploadKonfiguration konfig, String passwort, ExportErgebnis ergebnis) throws IOException {
        return UploadServiceFactory.erstelle(konfig, getWorkingSpreadsheet())
                .hochladen(ergebnis.exportierteDateien(), passwort);
    }

    private boolean istAnmeldefehler(IOException e) {
        String meldung = e.getMessage();
        return meldung != null && (meldung.contains("Auth fail") || meldung.contains("Auth cancel"));
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }

    protected abstract IUploadKonfigurierbar getUploadKonfigurierbar() throws GenerateException;
}
