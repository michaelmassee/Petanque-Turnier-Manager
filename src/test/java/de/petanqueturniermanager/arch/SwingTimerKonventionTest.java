/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.arch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Verhindert, dass Produktivcode wieder {@code javax.swing.Timer} einführt und
 * dadurch auf macOS im LibreOffice-Prozess AWT/AppKit initialisiert.
 */
public class SwingTimerKonventionTest {

    @Test
    void produktivcodeVerwendetKeinenSwingTimer() throws IOException {
        List<String> verstoesse = new ArrayList<>();
        Path quellWurzel = Paths.get("src/main/java");

        try (Stream<Path> dateien = Files.walk(quellWurzel)) {
            dateien.filter(p -> p.toString().endsWith(".java")).forEach(datei -> {
                String inhalt = liesDatei(datei);
                if (inhalt.contains("import javax.swing.Timer")
                        || inhalt.contains("javax.swing.Timer")) {
                    verstoesse.add(quellWurzel.relativize(datei).toString());
                }
            });
        }

        assertThat(verstoesse)
                .as("Produktivcode darf keinen javax.swing.Timer verwenden; für Debounce/Delay "
                        + "ScheduledExecutorService nutzen und UNO-Arbeit per LoMainThread.post(...) marshallen.")
                .isEmpty();
    }

    private static String liesDatei(Path datei) {
        try {
            return Files.readString(datei);
        } catch (IOException e) {
            throw new IllegalStateException("Fehler beim Lesen von " + datei, e);
        }
    }
}
