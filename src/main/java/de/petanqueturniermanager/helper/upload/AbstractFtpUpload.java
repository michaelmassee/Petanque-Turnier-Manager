package de.petanqueturniermanager.helper.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.FtpServerEintrag;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;

public abstract class AbstractFtpUpload extends SheetRunner {

    private static final Logger logger = LogManager.getLogger(AbstractFtpUpload.class);

    /** Document Property: id des zuletzt für dieses Dokument verwendeten FTP-Servers. */
    private static final String PROP_LETZTER_FTP_SERVER = "FTP Letzter Server";

    private final Function<Path, AbstractExportInVerzeichnis> exportFactory;

    protected AbstractFtpUpload(WorkingSpreadsheet ws, TurnierSystem ts, String name,
            Function<Path, AbstractExportInVerzeichnis> exportFactory) {
        super(ws, ts, name);
        this.exportFactory = exportFactory;
    }

    @Override
    protected final void doRun() throws GenerateException {
        List<FtpServerEintrag> serverListe = GlobalProperties.get().getFtpServerEintraege();
        if (serverListe.isEmpty()) {
            throw new GenerateException(I18n.get("ftp.upload.fehler.keine.server"));
        }

        var docPropHelper = new DocumentPropertiesHelper(getWorkingSpreadsheet());
        String letzteServerId = docPropHelper.getStringProperty(PROP_LETZTER_FTP_SERVER, "");
        FtpServerEintrag ausgewaehlt = FtpServerAuswahlDialog
                .zeigen(getWorkingSpreadsheet(), serverListe, letzteServerId)
                .orElse(null);
        if (ausgewaehlt == null) {
            throw new GenerateException(I18n.get("upload.server.auswahl.abgebrochen"));
        }
        docPropHelper.setStringProperty(PROP_LETZTER_FTP_SERVER, ausgewaehlt.id());

        var konfig = new UploadKonfiguration(
                ausgewaehlt.protokoll(), ausgewaehlt.host(), ausgewaehlt.port(),
                ausgewaehlt.benutzer(), ausgewaehlt.remotePfad());

        Path tempVerzeichnis = erstelleTempVerzeichnis();
        try {
            var ergebnis = exportFactory.apply(tempVerzeichnis).exportiere();
            String passwort = ausgewaehlt.passwort();
            if (passwort.isEmpty()) {
                passwort = fragePasswort(konfig);
            }

            try {
                int anzahl = ladeHoch(konfig, passwort, ergebnis);
                processBox().info(I18n.get("ftp.upload.erfolg", anzahl));
            } catch (IOException e) {
                if (istAnmeldefehler(e)) {
                    try {
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
        return e instanceof AnmeldeFehlgeschlagenException;
    }

    @Override
    protected IKonfigurationSheet getKonfigurationSheet() {
        return null;
    }
}
