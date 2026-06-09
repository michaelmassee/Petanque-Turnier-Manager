/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AbstractExportInVerzeichnisTest {

    @Test
    void htmlZieldatei_ungespeichertesDokument_nutztFallback() throws Exception {
        assertThat(AbstractExportInVerzeichnis.htmlZieldatei(Path.of("/tmp/export"), "Liga.html", ""))
                .isEqualTo(Path.of("/tmp/export/Liga.html"));
    }

    @Test
    void htmlZieldatei_gespeichertesDokument_nutztDokumentBasisname() throws Exception {
        assertThat(AbstractExportInVerzeichnis.htmlZieldatei(
                Path.of("/tmp/export"), "Liga.html", Path.of("/tmp/mein-turnier.ods").toUri().toString()))
                .isEqualTo(Path.of("/tmp/export/mein-turnier.html"));
    }

    @Test
    void htmlZieldatei_kaputteUri_wirftGenerateException() {
        assertThatThrownBy(() -> AbstractExportInVerzeichnis.htmlZieldatei(
                Path.of("/tmp/export"), "Liga.html", "http:// example.org/datei.ods"))
                .isInstanceOf(de.petanqueturniermanager.exception.GenerateException.class);
    }
}
