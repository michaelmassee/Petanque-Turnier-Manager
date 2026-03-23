/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * Prüft die Referenz-Sprachdatei (messages.properties) auf Konsistenz mit dem Java-Quellcode:
 * <ul>
 *   <li>Fehlende Schlüssel: im Code verwendet, aber nicht in messages.properties</li>
 *   <li>Ungenutzte Schlüssel: in messages.properties vorhanden, aber im Code nirgends referenziert</li>
 * </ul>
 * <p>
 * Erkennungsstrategie: Alle String-Literale im Quellcode, die dem i18n-Schlüsselmuster entsprechen
 * (mindestens zwei punkt-getrennte Segmente aus Kleinbuchstaben/Ziffern), werden als potenzielle
 * Schlüssel behandelt. Damit werden sowohl direkte {@code I18n.get("key")}-Aufrufe als auch
 * Konstanten, Enum-Werte und andere indirekte Verwendungen erfasst.
 */
class I18nReferenzdateiTest {

    private static final String I18N_ORDNER = "de/petanqueturniermanager/i18n/";
    private static final String REFERENZ_DATEI = "messages.properties";
    private static final Path QUELL_ORDNER = Path.of("src/main/java");

    /**
     * Erkennt String-Literale der Form "segment.segment[.segment...]"
     * mit ausschließlich Kleinbuchstaben, Ziffern und Unterstrichen pro Segment.
     */
    private static final Pattern I18N_SCHLUESSEL_MUSTER = Pattern.compile(
            "\"([a-z][a-z0-9_]*(?:\\.[a-z0-9][a-z0-9_]*)+)\"");

    @Test
    void alleVerwendetenSchluesselSindInReferenzdatei() throws IOException {
        Set<String> referenzKeys = referenzSchluessel();
        Set<String> verwendeteKeys = imCodeVerwendeteSchluessel();

        List<String> fehlend = verwendeteKeys.stream()
                .filter(k -> !referenzKeys.contains(k))
                .sorted()
                .toList();

        assertThat(fehlend)
                .as("Im Code verwendete Schlüssel fehlen in %s", REFERENZ_DATEI)
                .isEmpty();
    }

    @Test
    void keineUngenutztenSchluesselInReferenzdatei() throws IOException {
        Set<String> referenzKeys = referenzSchluessel();
        Set<String> verwendeteKeys = imCodeVerwendeteSchluessel();

        List<String> ungenutzt = referenzKeys.stream()
                .filter(k -> !verwendeteKeys.contains(k))
                .sorted()
                .toList();

        assertThat(ungenutzt)
                .as("Schlüssel in %s vorhanden, aber im Code nicht referenziert", REFERENZ_DATEI)
                .isEmpty();
    }

    @Test
    void quellordnerVorhanden() {
        assertThat(QUELL_ORDNER).as("Java-Quellordner nicht gefunden: %s", QUELL_ORDNER).isDirectory();
    }

    private Set<String> referenzSchluessel() throws IOException {
        String pfad = I18N_ORDNER + REFERENZ_DATEI;
        Properties props = new Properties();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(pfad)) {
            assertThat(stream).as("Referenzdatei nicht im Classpath: %s", pfad).isNotNull();
            props.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return props.stringPropertyNames();
    }

    private Set<String> imCodeVerwendeteSchluessel() throws IOException {
        assertThat(QUELL_ORDNER).as("Java-Quellordner nicht gefunden: %s", QUELL_ORDNER).isDirectory();

        Set<String> referenzKeys = referenzSchluessel();
        // Gültige Präfixe aus der Referenzdatei ableiten (erster Segment jedes Schlüssels).
        // Verhindert false positives durch Package-Namen o.Ä. und wächst automatisch mit neuen Präfixen.
        Set<String> gueltigePraefixe = new TreeSet<>();
        for (String key : referenzKeys) {
            int punkt = key.indexOf('.');
            if (punkt > 0) {
                gueltigePraefixe.add(key.substring(0, punkt + 1));
            }
        }

        Set<String> gefunden = new TreeSet<>();
        SoftAssertions soft = new SoftAssertions();
        try (var stream = Files.walk(QUELL_ORDNER)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .forEach(datei -> {
                        try {
                            var matcher = I18N_SCHLUESSEL_MUSTER.matcher(
                                    Files.readString(datei, StandardCharsets.UTF_8));
                            while (matcher.find()) {
                                String schluessel = matcher.group(1);
                                boolean hatGueltigePraefix = gueltigePraefixe.stream()
                                        .anyMatch(schluessel::startsWith);
                                if (hatGueltigePraefix) {
                                    gefunden.add(schluessel);
                                }
                            }
                        } catch (IOException e) {
                            soft.fail("Datei konnte nicht gelesen werden: %s — %s", datei, e.getMessage());
                        }
                    });
        }
        soft.assertAll();
        return gefunden;
    }

}
