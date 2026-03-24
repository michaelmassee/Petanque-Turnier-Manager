/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Prüft die Vollständigkeit von {@code description.xml} und den zugehörigen {@code desc_xx.txt}-Dateien
 * in Bezug auf die vorhandenen i18n-Sprachdateien.
 * <p>
 * Quelle der Wahrheit für Sprachcodes sind die {@code messages_*.properties}-Dateien
 * sowie die Basisdatei {@code messages.properties} (→ "de").
 * Kommt eine neue Sprache hinzu, werden die Tests automatisch rot, bis description.xml
 * und die desc_xx.txt-Datei ergänzt sind.
 * <p>
 * Geprüfte Invarianten:
 * <ul>
 *   <li>Für jeden Sprachcode existiert ein {@code <publisher>/<name lang="xx">} in description.xml</li>
 *   <li>Für jeden Sprachcode existiert ein {@code <display-name>/<name lang="xx">} in description.xml</li>
 *   <li>Für jeden Sprachcode existiert ein {@code <extension-description>/<src lang="xx">} in description.xml</li>
 *   <li>Für jeden Sprachcode existiert eine {@code description/desc_xx.txt}-Datei</li>
 *   <li>Jede {@code desc_xx.txt}-Datei hat einen korrespondierenden Sprachcode (keine Waisen)</li>
 * </ul>
 */
class DescriptionXmlVollstaendigkeitTest {

    private static final Path RESSOURCEN_BASIS = Paths.get("src/main/resources");
    private static final String I18N_ORDNER = "de/petanqueturniermanager/i18n/";
    private static final Path DESCRIPTION_XML = Paths.get("description.xml");
    private static final Path DESCRIPTION_ORDNER = Paths.get("description");

    @Test
    void publisherEintraegeVollstaendig() throws Exception {
        pruefeXmlEintraege("publisher/name", "publisher/name[@lang='%s'] fehlt in description.xml");
    }

    @Test
    void displayNameEintraegeVollstaendig() throws Exception {
        pruefeXmlEintraege("display-name/name", "display-name/name[@lang='%s'] fehlt in description.xml");
    }

    @Test
    void extensionDescriptionEintraegeVollstaendig() throws Exception {
        Set<String> sprachcodes = alleSprachcodes();
        Document doc = parseDescriptionXml();
        XPath xpath = XPathFactory.newInstance().newXPath();

        SoftAssertions soft = new SoftAssertions();
        for (String lang : sprachcodes) {
            String ausdruck = "//extension-description/src[@lang='" + lang + "']";
            var treffer = (NodeList) xpath.evaluate(ausdruck, doc, XPathConstants.NODESET);
            soft.assertThat(treffer.getLength())
                    .as("extension-description/src[@lang='%s'] fehlt in description.xml", lang)
                    .isGreaterThan(0);
        }
        soft.assertAll();
    }

    @Test
    void alleDescDateienVorhanden() throws IOException {
        Set<String> sprachcodes = alleSprachcodes();

        SoftAssertions soft = new SoftAssertions();
        for (String lang : sprachcodes) {
            Path descDatei = DESCRIPTION_ORDNER.resolve("desc_" + lang + ".txt");
            soft.assertThat(descDatei)
                    .as("description/desc_%s.txt fehlt", lang)
                    .isRegularFile();
        }
        soft.assertAll();
    }

    @Test
    void keineVerwaistenDescDateien() throws IOException {
        Set<String> sprachcodes = alleSprachcodes();

        SoftAssertions soft = new SoftAssertions();
        try (var dateistrom = Files.list(DESCRIPTION_ORDNER)) {
            dateistrom
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.matches("desc_[a-z]+\\.txt"))
                    .forEach(name -> {
                        String lang = name.substring("desc_".length(), name.length() - ".txt".length());
                        soft.assertThat(sprachcodes)
                                .as("desc_%s.txt vorhanden, aber keine messages_%s.properties – verwaiste Datei?",
                                        lang, lang)
                                .contains(lang);
                    });
        }
        soft.assertAll();
    }

    @Test
    void descriptionXmlVorhanden() {
        assertThat(DESCRIPTION_XML)
                .as("description.xml nicht gefunden: %s", DESCRIPTION_XML.toAbsolutePath())
                .isRegularFile();
    }

    @Test
    void descriptionOrdnerVorhanden() {
        assertThat(DESCRIPTION_ORDNER)
                .as("description-Ordner nicht gefunden: %s", DESCRIPTION_ORDNER.toAbsolutePath())
                .isDirectory();
    }

    // -------------------------------------------------------------------------

    private void pruefeXmlEintraege(String elementPfad, String fehlermeldung) throws Exception {
        Set<String> sprachcodes = alleSprachcodes();
        Document doc = parseDescriptionXml();
        XPath xpath = XPathFactory.newInstance().newXPath();

        SoftAssertions soft = new SoftAssertions();
        for (String lang : sprachcodes) {
            String ausdruck = "//" + elementPfad + "[@lang='" + lang + "']";
            var treffer = (NodeList) xpath.evaluate(ausdruck, doc, XPathConstants.NODESET);
            soft.assertThat(treffer.getLength())
                    .as(fehlermeldung, lang)
                    .isGreaterThan(0);
        }
        soft.assertAll();
    }

    /**
     * Ermittelt alle Sprachcodes aus den vorhandenen {@code messages_*.properties}-Dateien
     * sowie "de" für die Basisdatei {@code messages.properties}.
     */
    private Set<String> alleSprachcodes() throws IOException {
        assertThat(RESSOURCEN_BASIS.resolve(I18N_ORDNER))
                .as("i18n-Ordner nicht gefunden")
                .isDirectory();

        Set<String> sprachcodes = new TreeSet<>();
        sprachcodes.add("de");

        try (var dateistrom = Files.list(RESSOURCEN_BASIS.resolve(I18N_ORDNER))) {
            dateistrom
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith("messages_") && name.endsWith(".properties"))
                    .map(name -> name.substring("messages_".length(), name.length() - ".properties".length()))
                    .forEach(sprachcodes::add);
        }
        return sprachcodes;
    }

    private Document parseDescriptionXml() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Namespace-Awareness deaktivieren: XPath-Ausdrücke ohne Namespace-Präfix
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(DESCRIPTION_XML.toFile());
    }
}
