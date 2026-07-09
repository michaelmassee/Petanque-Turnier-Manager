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
import com.jcraft.jsch.UserInfo;

class SftpUploadService implements IUploadService {

    private static final Logger logger = LogManager.getLogger(SftpUploadService.class);
    private static final int VERBINDUNGS_TIMEOUT_MS = 10_000;

    private final UploadKonfiguration konfiguration;
    private final UserInfo userInfo;

    SftpUploadService(UploadKonfiguration konfiguration) {
        this(konfiguration, null);
    }

    SftpUploadService(UploadKonfiguration konfiguration, UserInfo userInfo) {
        this.konfiguration = konfiguration;
        this.userInfo = userInfo;
    }

    @Override
    public int hochladen(List<Path> dateien, String passwort) throws IOException {
        Session session = null;
        ChannelSftp kanal = null;
        try {
            session = verbinde(passwort);

            kanal = (ChannelSftp) session.openChannel("sftp");
            kanal.connect();

            String remotePfad = konfiguration.remotePfad();
            wechsleInVerzeichnis(kanal, remotePfad);

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
            throw wandleJschFehlerUm(e);
        } finally {
            trenneVerbindung(session, kanal);
        }
    }

    @Override
    public void testeVerbindung(String passwort) throws IOException {
        Session session = null;
        ChannelSftp kanal = null;
        try {
            session = verbinde(passwort);
            kanal = (ChannelSftp) session.openChannel("sftp");
            kanal.connect();
            wechsleInVerzeichnis(kanal, konfiguration.remotePfad());
        } catch (JSchException e) {
            throw wandleJschFehlerUm(e);
        } finally {
            trenneVerbindung(session, kanal);
        }
    }

    private IOException wandleJschFehlerUm(JSchException e) {
        String meldung = formatiereVerbindungsfehler(e.getMessage(), konfiguration);
        return istAuthFehler(e.getMessage())
                ? new AnmeldeFehlgeschlagenException(meldung, e)
                : new IOException(meldung, e);
    }

    static boolean istAuthFehler(String meldung) {
        return meldung != null && (meldung.contains("Auth fail") || meldung.contains("Auth cancel"));
    }

    private Session verbinde(String passwort) throws IOException, JSchException {
        var jsch = new JSch();
        ladeKnownHosts(jsch);
        Session session = jsch.getSession(konfiguration.benutzer(), konfiguration.host(), konfiguration.port());
        session.setPassword(passwort);
        if (userInfo != null) {
            if (userInfo instanceof SftpHostKeyUserInfo hostKeyUserInfo) {
                hostKeyUserInfo.setPasswort(passwort);
            }
            session.setUserInfo(userInfo);
        }
        var config = new Properties();
        config.put("StrictHostKeyChecking", userInfo != null ? "ask" : "yes");
        config.put("PreferredAuthentications", "password");
        session.setConfig(config);
        session.connect(VERBINDUNGS_TIMEOUT_MS);
        return session;
    }

    private void trenneVerbindung(Session session, ChannelSftp kanal) {
        if (kanal != null && kanal.isConnected()) {
            kanal.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private void wechsleInVerzeichnis(ChannelSftp kanal, String remotePfad) throws IOException {
        String mkdirFehler = erstelleVerzeichnisWennFehlt(kanal, remotePfad);
        try {
            kanal.cd(remotePfad);
        } catch (SftpException e) {
            throw new IOException(formatiereVerzeichnisFehler(kanal, remotePfad, mkdirFehler, e), e);
        }
    }

    /**
     * @return Fehlermeldung des mkdir-Versuchs, oder {@code null} falls erfolgreich (Verzeichnis
     *         existiert dann entweder bereits oder wurde neu angelegt).
     */
    private String erstelleVerzeichnisWennFehlt(ChannelSftp kanal, String remotePfad) {
        try {
            kanal.mkdir(remotePfad);
            return null;
        } catch (SftpException e) {
            logger.debug("SFTP mkdir fehlgeschlagen (Verzeichnis existiert möglicherweise bereits): {}", remotePfad,
                    e);
            return e.getMessage();
        }
    }

    private String formatiereVerzeichnisFehler(ChannelSftp kanal, String remotePfad, String mkdirFehler,
            SftpException cdFehler) {
        String loginVerzeichnis = ermittleLoginVerzeichnis(kanal);
        var meldung = new StringBuilder("SFTP-Verzeichniswechsel fehlgeschlagen: \"")
                .append(remotePfad)
                .append("\" (").append(cdFehler.getMessage()).append(")");
        if (mkdirFehler != null) {
            meldung.append(". Anlegen des Verzeichnisses ist ebenfalls fehlgeschlagen: ").append(mkdirFehler);
        }
        if (loginVerzeichnis == null) {
            return meldung.toString();
        }
        meldung.append(". SFTP-Login-Verzeichnis ist \"").append(loginVerzeichnis).append("\"");
        if (remotePfad.startsWith("/") && !"/".equals(loginVerzeichnis)) {
            meldung.append(". Falls der Server-Zugang auf ein Unterverzeichnis eingeschränkt ist (Chroot), ")
                    .append("bitte im FTP-Server-Dialog einen Pfad relativ zu diesem Login-Verzeichnis eintragen ")
                    .append("(ohne das \"").append(loginVerzeichnis).append("\"-Präfix)");
        }
        return meldung.toString();
    }

    private String ermittleLoginVerzeichnis(ChannelSftp kanal) {
        try {
            return kanal.pwd();
        } catch (SftpException e) {
            logger.debug("SFTP pwd() fehlgeschlagen", e);
            return null;
        }
    }

    private void ladeKnownHosts(JSch jsch) throws IOException {
        Path knownHosts = Paths.get(System.getProperty("user.home"), ".ssh", "known_hosts");
        if (userInfo == null && !Files.isRegularFile(knownHosts)) {
            throw new IOException("SFTP known_hosts nicht gefunden: " + knownHosts);
        }
        if (userInfo != null) {
            bereiteKnownHostsDateiVor(knownHosts);
        }
        try {
            jsch.setKnownHosts(knownHosts.toString());
        } catch (JSchException e) {
            throw new IOException("SFTP known_hosts kann nicht geladen werden: " + knownHosts, e);
        }
    }

    private void bereiteKnownHostsDateiVor(Path knownHosts) throws IOException {
        Path parent = knownHosts.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(knownHosts)) {
            Files.createFile(knownHosts);
        }
    }

    static String formatiereVerbindungsfehler(String meldung, UploadKonfiguration konfiguration) {
        String basis = "SFTP-Verbindungsfehler: " + meldung;
        if (meldung != null && meldung.contains("Auth fail")) {
            return basis + ". Anmeldung fehlgeschlagen. Bitte Benutzername, Passwort und SFTP-Zugang prüfen.";
        }
        if (meldung != null && meldung.contains("Auth cancel")) {
            return basis + ". Anmeldung wurde abgebrochen. Bitte Passwort erneut eingeben.";
        }
        if (meldung == null || !meldung.contains("HostKey")) {
            return basis;
        }

        String host = konfiguration.host();
        int port = konfiguration.port();
        String entfernen = port == 22
                ? "ssh-keygen -R " + host
                : "ssh-keygen -R \"[" + host + "]:" + port + "\"";
        String neuLaden = port == 22
                ? "ssh-keyscan -H " + host + " >> ~/.ssh/known_hosts"
                : "ssh-keyscan -H -p " + port + " " + host + " >> ~/.ssh/known_hosts";

        return basis
                + ". Der gespeicherte SFTP-Host-Key passt nicht zum Server. "
                + "Bitte den Fingerprint beim Anbieter prüfen und den known_hosts-Eintrag erneuern: "
                + entfernen + " && " + neuLaden;
    }
}
