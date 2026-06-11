package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.msgbox.MessageBox;

class SftpUploadServiceTest {

    @AfterEach
    void dialogeWiederAktivieren() {
        MessageBox.setDialogeUeberspringen(false);
    }

    @Test
    void hostKeyFehlerEnthaeltKnownHostsHinweisFuerStandardPort() {
        var konfiguration = new UploadKonfiguration(
                UploadProtokoll.SFTP,
                "57489369.ssh.w1.strato.hosting",
                22,
                "benutzer",
                "/ziel");

        String meldung = SftpUploadService.formatiereVerbindungsfehler(
                "reject HostKey: 57489369.ssh.w1.strato.hosting",
                konfiguration);

        assertThat(meldung)
                .contains("SFTP-Verbindungsfehler: reject HostKey: 57489369.ssh.w1.strato.hosting")
                .contains("Fingerprint beim Anbieter prüfen")
                .contains("ssh-keygen -R 57489369.ssh.w1.strato.hosting")
                .contains("ssh-keyscan -H 57489369.ssh.w1.strato.hosting >> ~/.ssh/known_hosts");
    }

    @Test
    void hostKeyFehlerEnthaeltKnownHostsHinweisFuerAbweichendenPort() {
        var konfiguration = new UploadKonfiguration(
                UploadProtokoll.SFTP,
                "example.org",
                2222,
                "benutzer",
                "/ziel");

        String meldung = SftpUploadService.formatiereVerbindungsfehler(
                "UnknownHostKey: example.org",
                konfiguration);

        assertThat(meldung)
                .contains("ssh-keygen -R \"[example.org]:2222\"")
                .contains("ssh-keyscan -H -p 2222 example.org >> ~/.ssh/known_hosts");
    }

    @Test
    void andereJschFehlerBleibenKurz() {
        var konfiguration = new UploadKonfiguration(UploadProtokoll.SFTP, "example.org", 22, "benutzer", "/ziel");

        String meldung = SftpUploadService.formatiereVerbindungsfehler("Auth fail", konfiguration);

        assertThat(meldung)
                .contains("SFTP-Verbindungsfehler: Auth fail")
                .contains("Benutzername, Passwort und SFTP-Zugang prüfen");
    }

    @Test
    void sonstigeJschFehlerBleibenKurz() {
        var konfiguration = new UploadKonfiguration(UploadProtokoll.SFTP, "example.org", 22, "benutzer", "/ziel");

        String meldung = SftpUploadService.formatiereVerbindungsfehler("Session.connect: timeout", konfiguration);

        assertThat(meldung).isEqualTo("SFTP-Verbindungsfehler: Session.connect: timeout");
    }

    @Test
    void hostKeyRueckfrageLiefertJaWennDialogBestaetigtWird() {
        MessageBox.setDialogeUeberspringen(true);
        var ws = mock(WorkingSpreadsheet.class);
        when(ws.getxContext()).thenReturn(mock(XComponentContext.class));

        var userInfo = new SftpHostKeyUserInfo(ws);

        assertThat(userInfo.promptYesNo("fingerprint")).isTrue();
    }

    @Test
    void userInfoLiefertGesetztesPasswortFuerJschCallback() {
        var ws = mock(WorkingSpreadsheet.class);
        var userInfo = new SftpHostKeyUserInfo(ws);

        assertThat(userInfo.promptPassword("Password")).isFalse();

        userInfo.setPasswort("geheim");

        assertThat(userInfo.promptPassword("Password")).isTrue();
        assertThat(userInfo.getPassword()).isEqualTo("geheim");
    }
}
