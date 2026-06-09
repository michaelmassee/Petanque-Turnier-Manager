/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportErgebnisTest {

    @TempDir
    private Path tmp;

    @Test
    void serialisierungErhaeltPfadeMitSemikolon() throws Exception {
        Path datei = tmp.resolve("rang;liste.pdf");
        Files.writeString(datei, "pdf");

        String wert = ExportErgebnis.serialisiere(List.of(datei));

        assertThat(ExportErgebnis.dateienAusProperty(wert)).containsExactly(datei);
    }

    @Test
    void legacySerialisierungOhneSemikolonBleibtLesbar() throws Exception {
        Path datei = tmp.resolve("rangliste.pdf");
        Files.writeString(datei, "pdf");

        assertThat(ExportErgebnis.dateienAusProperty(datei.toString())).containsExactly(datei);
    }
}
