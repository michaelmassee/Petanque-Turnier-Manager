package de.petanqueturniermanager.helper.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FtpUploadService implements IUploadService {

    private static final Logger logger = LogManager.getLogger(FtpUploadService.class);
    private static final int VERBINDUNGS_TIMEOUT_MS = 10_000;

    private final UploadKonfiguration konfiguration;

    FtpUploadService(UploadKonfiguration konfiguration) {
        this.konfiguration = konfiguration;
    }

    @Override
    public int hochladen(List<Path> dateien, String passwort) throws IOException {
        var client = verbindeUndLogin(passwort);
        try {
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);

            String remotePfad = normalisiereRemotePfad(konfiguration.remotePfad());
            client.makeDirectory(remotePfad);

            int anzahl = 0;
            for (Path datei : dateien) {
                Path dateiName = datei.getFileName();
                if (dateiName == null) {
                    logger.warn("FTP: Datei ohne Namen übersprungen: {}", datei);
                    continue;
                }
                String ziel = remotePfad + "/" + dateiName;
                try (InputStream is = Files.newInputStream(datei)) {
                    if (client.storeFile(ziel, is)) {
                        anzahl++;
                        logger.info("FTP hochgeladen: {}", ziel);
                    } else {
                        throw new IOException("FTP Upload nicht bestätigt: " + ziel
                                + " (Antwortcode: " + client.getReplyCode() + ")");
                    }
                }
            }
            return anzahl;
        } finally {
            trenneVerbindung(client);
        }
    }

    @Override
    public void testeVerbindung(String passwort) throws IOException {
        trenneVerbindung(verbindeUndLogin(passwort));
    }

    private FTPClient verbindeUndLogin(String passwort) throws IOException {
        var client = new FTPClient();
        client.setConnectTimeout(VERBINDUNGS_TIMEOUT_MS);
        client.setDefaultTimeout(VERBINDUNGS_TIMEOUT_MS);
        try {
            client.setControlEncoding("UTF-8");
            client.connect(konfiguration.host(), konfiguration.port());
            if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                throw new IOException("FTP-Verbindung abgelehnt, Antwortcode: " + client.getReplyCode());
            }
            if (!client.login(konfiguration.benutzer(), passwort)) {
                throw new IOException("FTP-Anmeldung fehlgeschlagen für Benutzer: " + konfiguration.benutzer());
            }
            return client;
        } catch (IOException e) {
            trenneVerbindung(client);
            throw e;
        }
    }

    private void trenneVerbindung(FTPClient client) {
        if (client.isConnected()) {
            try {
                client.logout();
            } catch (IOException e) {
                logger.debug("FTP-Logout-Fehler ignoriert", e);
            }
            try {
                client.disconnect();
            } catch (IOException e) {
                logger.debug("FTP-Disconnect-Fehler ignoriert", e);
            }
        }
    }

    private String normalisiereRemotePfad(String pfad) {
        return pfad.replace('\\', '/').replaceAll("/+$", "");
    }
}
