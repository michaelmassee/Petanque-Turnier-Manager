/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * Prüft ob alle i18n-Sprachdateien vollständig sind:
 * Jeder Schlüssel aus der deutschen Referenzdatei muss in allen Sprachdateien vorhanden sein.
 * <p>
 * Sprachdateien werden automatisch anhand des Musters {@code messages_*.properties} im
 * Classpath-Ordner entdeckt — neue Dateien werden ohne Codeänderung berücksichtigt.
 * <p>
 * Fehlende Schlüssel werden pro Sprache gesammelt und am Ende gemeinsam ausgegeben,
 * sodass alle Lücken auf einmal sichtbar sind.
 */
class I18nVollstaendigkeitTest {

    private static final String I18N_ORDNER = "de/petanqueturniermanager/i18n/";
    private static final String REFERENZ_DATEI = "messages.properties";

    @Test
    void alleSprachdateienEnthaltenAlleSchluessel() throws IOException, URISyntaxException {
        Set<String> referenzKeys = new TreeSet<>(laden(REFERENZ_DATEI).stringPropertyNames());

        SoftAssertions soft = new SoftAssertions();

        for (String dateiname : alleSprachdateienEntdecken()) {
            Set<String> spracheKeys = laden(dateiname).stringPropertyNames();

            List<String> fehlend = referenzKeys.stream()
                    .filter(k -> !spracheKeys.contains(k))
                    .sorted()
                    .toList();

            soft.assertThat(fehlend)
                    .as("Fehlende Schlüssel in %s (%d von %d)", dateiname, fehlend.size(), referenzKeys.size())
                    .isEmpty();
        }

        soft.assertAll();
    }

    @Test
    void keineUnbekanntenSchluesselInSprachdateien() throws IOException, URISyntaxException {
        Set<String> referenzKeys = laden(REFERENZ_DATEI).stringPropertyNames();

        SoftAssertions soft = new SoftAssertions();

        for (String dateiname : alleSprachdateienEntdecken()) {
            Set<String> spracheKeys = new TreeSet<>(laden(dateiname).stringPropertyNames());

            List<String> unbekannt = spracheKeys.stream()
                    .filter(k -> !referenzKeys.contains(k))
                    .sorted()
                    .toList();

            soft.assertThat(unbekannt)
                    .as("Unbekannte Schlüssel in %s (nicht in Referenzdatei vorhanden)", dateiname)
                    .isEmpty();
        }

        soft.assertAll();
    }

    @Test
    void referenzdateiIstNichtLeer() throws IOException {
        Set<String> referenzKeys = laden(REFERENZ_DATEI).stringPropertyNames();
        assertThat(referenzKeys).isNotEmpty();
    }

    @Test
    void mindestensEineSprachdateiVorhanden() throws IOException, URISyntaxException {
        assertThat(alleSprachdateienEntdecken())
                .as("Keine Sprachdateien (messages_*.properties) im i18n-Ordner gefunden")
                .isNotEmpty();
    }

    /**
     * Entdeckt automatisch alle Sprachdateien (messages_*.properties) im i18n-Ordner.
     * Neue Sprachdateien werden ohne Codeänderung berücksichtigt.
     */
    private List<String> alleSprachdateienEntdecken() throws IOException, URISyntaxException {
        URL ordnerUrl = getClass().getClassLoader().getResource(I18N_ORDNER);
        assertThat(ordnerUrl)
                .as("i18n-Ordner nicht im Classpath gefunden: %s", I18N_ORDNER)
                .isNotNull();
        Path ordner = Paths.get(ordnerUrl.toURI());
        try (var dateien = Files.list(ordner)) {
            return dateien
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith("messages_") && name.endsWith(".properties"))
                    .sorted()
                    .toList();
        }
    }

    private Properties laden(String dateiname) throws IOException {
        String pfad = I18N_ORDNER + dateiname;
        Properties props = new Properties();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(pfad)) {
            assertThat(stream)
                    .as("i18n-Datei nicht im Classpath gefunden: %s", pfad)
                    .isNotNull();
            props.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return props;
    }

}
