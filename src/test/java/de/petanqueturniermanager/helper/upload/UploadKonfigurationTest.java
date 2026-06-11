package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UploadKonfigurationTest {

    @Test
    void leererPortNutztFtpStandardPort() {
        assertThat(UploadKonfiguration.portOderStandard("", UploadProtokoll.FTP)).isEqualTo(21);
    }

    @Test
    void leererPortNutztSftpStandardPort() {
        assertThat(UploadKonfiguration.portOderStandard("  ", UploadProtokoll.SFTP)).isEqualTo(22);
    }

    @Test
    void gesetzterPortBleibtErhalten() {
        assertThat(UploadKonfiguration.portOderStandard("2222", UploadProtokoll.SFTP)).isEqualTo(2222);
    }
}
