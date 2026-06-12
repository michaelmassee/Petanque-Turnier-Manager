/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;

class AbstractExportInVerzeichnisTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void initI18n() {
        I18n.initFuerTest(Locale.GERMAN);
    }

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
                .isInstanceOf(GenerateException.class);
    }

    @Test
    void turnierlogo_remoteUrl_bleibtUnveraendert() throws Exception {
        var logo = AbstractExportInVerzeichnis.bereiteTurnierlogoVor(
                tempDir, "https://example.org/logo.png?a=1&b=2");

        assertThat(logo.logoUrl()).isEqualTo("https://example.org/logo.png?a=1&b=2");
        assertThat(logo.kopierteDatei()).isEmpty();
    }

    @Test
    void turnierlogo_dataUri_bleibtUnveraendert() throws Exception {
        var logo = AbstractExportInVerzeichnis.bereiteTurnierlogoVor(
                tempDir, "data:image/png;base64,abc");

        assertThat(logo.logoUrl()).isEqualTo("data:image/png;base64,abc");
        assertThat(logo.kopierteDatei()).isEmpty();
    }

    @Test
    void turnierlogo_lokalerPfad_wirdKopiertUndRelativReferenziert() throws Exception {
        Path quelle = Files.writeString(tempDir.resolve("Mein Logo.png"), "png");
        Path exportDir = Files.createDirectory(tempDir.resolve("export"));

        var logo = AbstractExportInVerzeichnis.bereiteTurnierlogoVor(exportDir, quelle.toString());

        Path ziel = tempDir.resolve("export/turnier-logo.png");
        assertThat(logo.logoUrl()).isEqualTo("turnier-logo.png");
        assertThat(logo.kopierteDatei()).contains(ziel);
        assertThat(ziel).hasContent("png");
    }

    @Test
    void turnierlogo_fileUri_wirdKopiertUndRelativReferenziert() throws Exception {
        Path quelle = Files.writeString(tempDir.resolve("logo.svg"), "<svg/>");
        Path exportDir = Files.createDirectory(tempDir.resolve("export"));

        var logo = AbstractExportInVerzeichnis.bereiteTurnierlogoVor(exportDir, quelle.toUri().toString());

        Path ziel = tempDir.resolve("export/turnier-logo.svg");
        assertThat(logo.logoUrl()).isEqualTo("turnier-logo.svg");
        assertThat(logo.kopierteDatei()).contains(ziel);
        assertThat(ziel).hasContent("<svg/>");
    }

    @Test
    void turnierlogo_lokalerPfadOhneEndung_wirdOhneEndungKopiert() throws Exception {
        Path quelle = Files.writeString(tempDir.resolve("logo"), "logo");
        Path exportDir = Files.createDirectory(tempDir.resolve("export"));

        var logo = AbstractExportInVerzeichnis.bereiteTurnierlogoVor(exportDir, quelle.toString());

        Path ziel = tempDir.resolve("export/turnier-logo");
        assertThat(logo.logoUrl()).isEqualTo("turnier-logo");
        assertThat(logo.kopierteDatei()).contains(ziel);
        assertThat(ziel).hasContent("logo");
    }

    @Test
    void turnierlogo_fehlendeLokaleDatei_wirftGenerateException() {
        assertThatThrownBy(() -> AbstractExportInVerzeichnis.bereiteTurnierlogoVor(
                tempDir, tempDir.resolve("fehlt.png").toString()))
                .isInstanceOf(GenerateException.class)
                .hasMessageContaining("fehlt.png");
    }

    @Test
    void htmlExportErgebnisFuegtHtmlUndLogoZurDateilisteHinzu() {
        var dateien = new ArrayList<Path>();
        Path html = tempDir.resolve("export.html");
        Path logo = tempDir.resolve("turnier-logo.png");

        new AbstractExportInVerzeichnis.HtmlExportErgebnis(html, Optional.of(logo)).addTo(dateien);

        assertThat(dateien).containsExactly(html, logo);
    }
}
