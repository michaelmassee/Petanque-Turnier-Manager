package de.petanqueturniermanager.helper.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

class SftpUploadService implements IUploadService {

    private static final Logger logger = LogManager.getLogger(SftpUploadService.class);

    private final UploadKonfiguration konfiguration;

    SftpUploadService(UploadKonfiguration konfiguration) {
        this.konfiguration = konfiguration;
    }

    @Override
    public int hochladen(List<Path> dateien, String passwort) throws IOException {
        Session session = null;
        ChannelSftp kanal = null;
        try {
            var jsch = new JSch();
            ladeKnownHosts(jsch);
            session = jsch.getSession(konfiguration.benutzer(), konfiguration.host(), konfiguration.port());
            session.setPassword(passwort);
            var config = new Properties();
            config.put("StrictHostKeyChecking", "yes");
            session.setConfig(config);
            session.connect();

            kanal = (ChannelSftp) session.openChannel("sftp");
            kanal.connect();

            String remotePfad = konfiguration.remotePfad();
            erstelleVerzeichnisWennFehlt(kanal, remotePfad);
            kanal.cd(remotePfad);

            int anzahl = 0;
            for (Path datei : dateien) {
                Path dateiName = datei.getFileName();
                if (dateiName == null) {
                    logger.warn("SFTP: Datei ohne Namen übersprungen: {}", datei);
                    continue;
                }
                try (InputStream is = Files.newInputStream(datei)) {
                    kanal.put(is, dateiName.toString());
                    anzahl++;
                    logger.info("SFTP hochgeladen: {}/{}", remotePfad, dateiName);
                } catch (SftpException e) {
                    throw new IOException("SFTP Upload fehlgeschlagen: " + dateiName, e);
                }
            }
            return anzahl;
        } catch (JSchException e) {
            throw new IOException("SFTP-Verbindungsfehler: " + e.getMessage(), e);
        } catch (SftpException e) {
            throw new IOException("SFTP-Verzeichniswechsel fehlgeschlagen: " + e.getMessage(), e);
        } finally {
            if (kanal != null && kanal.isConnected()) {
                kanal.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void erstelleVerzeichnisWennFehlt(ChannelSftp kanal, String remotePfad) {
        try {
            kanal.mkdir(remotePfad);
        } catch (SftpException e) {
            logger.debug("SFTP mkdir ignoriert (Verzeichnis existiert möglicherweise): {}", remotePfad);
        }
    }

    private void ladeKnownHosts(JSch jsch) throws IOException {
        Path knownHosts = Paths.get(System.getProperty("user.home"), ".ssh", "known_hosts");
        if (!Files.isRegularFile(knownHosts)) {
            throw new IOException("SFTP known_hosts nicht gefunden: " + knownHosts);
        }
        try {
            jsch.setKnownHosts(knownHosts.toString());
        } catch (JSchException e) {
            throw new IOException("SFTP known_hosts kann nicht geladen werden: " + knownHosts, e);
        }
    }
}
