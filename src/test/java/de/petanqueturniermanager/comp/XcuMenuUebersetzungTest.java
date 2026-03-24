package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Prüft, dass alle Menü-Titel in den Addons-XCU-Dateien vollständige
 * Übersetzungen in allen unterstützten Sprachen enthalten.
 * <p>
 * Texte in XCU-Dateien sind direkt mehrsprachig hardcodiert (kein i18n-Framework),
 * mit {@code <value xml:lang="de">}, {@code <value xml:lang="en">} usw.
 * <p>
 * Die erwarteten Sprachen werden dynamisch aus den vorhandenen
 * {@code messages_*.properties}-Dateien abgeleitet. Neue Sprachdateien werden
 * automatisch berücksichtigt — ohne Codeänderung.
 */
class XcuMenuUebersetzungTest {

    private static final Path I18N_ORDNER = Paths.get("src/main/resources/de/petanqueturniermanager/i18n/");
    private static final String OOR_NS = "http://openoffice.org/2001/registry";
    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    @Test
    void alleMenuTitelHabenAlleSprachversionen() throws Exception {
        Set<String> erwarteteSprachen = erwarteteSprachen();
        SoftAssertions soft = new SoftAssertions();

        for (Path xcu : alleAddonsXcuDateien()) {
            pruefeXcuDatei(xcu, erwarteteSprachen, soft);
        }

        soft.assertAll();
    }

    @Test
    void mindestensEineXcuDateiGefunden() throws Exception {
        assertThat(alleAddonsXcuDateien())
                .as("Keine Addons_*.xcu-Dateien im registry-Verzeichnis gefunden")
                .isNotEmpty();
    }

    @Test
    void mindestensEineSpracheDateiGefunden() throws Exception {
        assertThat(erwarteteSprachen())
                .as("Keine messages_*.properties-Dateien gefunden — Sprachen nicht ermittelbar")
                .isNotEmpty();
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────

    /**
     * Leitet die erwarteten Sprachcodes aus den vorhandenen Sprachdateien ab:
     * {@code messages.properties} → {@code de}, {@code messages_en.properties} → {@code en} usw.
     */
    private Set<String> erwarteteSprachen() throws IOException {
        Set<String> sprachen = new TreeSet<>();
        try (Stream<Path> dateien = Files.list(I18N_ORDNER)) {
            dateien.map(p -> p.getFileName().toString())
                   .filter(name -> name.startsWith("messages") && name.endsWith(".properties"))
                   .forEach(name -> {
                       if ("messages.properties".equals(name)) {
                           sprachen.add("de");
                       } else {
                           // messages_en.properties → en
                           sprachen.add(name.substring("messages_".length(), name.length() - ".properties".length()));
                       }
                   });
        }
        return sprachen;
    }

    private List<Path> alleAddonsXcuDateien() throws IOException {
        try (Stream<Path> files = Files.walk(Paths.get("registry"))) {
            return files.filter(p -> p.getFileName().toString().startsWith("Addons_")
                                  && p.toString().endsWith(".xcu"))
                        .sorted()
                        .toList();
        }
    }

    private void pruefeXcuDatei(Path xcu, Set<String> erwarteteSprachen, SoftAssertions soft) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var doc = factory.newDocumentBuilder().parse(xcu.toFile());

        NodeList props = doc.getElementsByTagName("prop");
        for (int i = 0; i < props.getLength(); i++) {
            Element prop = (Element) props.item(i);
            if (!"Title".equals(prop.getAttributeNS(OOR_NS, "name"))) continue;

            Map<String, String> vorhandeneSprachen = sprachwerteAus(prop);

            // Kein xml:lang → kein Menüeintrag (z.B. Separator), überspringen
            if (vorhandeneSprachen.isEmpty()) continue;

            String kontext = "%s [%s]".formatted(xcu.getFileName(), knotenKontext(prop));
            pruefeVollstaendigkeit(vorhandeneSprachen, erwarteteSprachen, kontext, soft);
        }
    }

    /** Liest alle {@code <value xml:lang="...">}-Kinder eines prop-Elements. */
    private Map<String, String> sprachwerteAus(Element prop) {
        Map<String, String> sprachen = new LinkedHashMap<>();
        NodeList values = prop.getElementsByTagName("value");
        for (int j = 0; j < values.getLength(); j++) {
            Element value = (Element) values.item(j);
            String lang = value.getAttributeNS(XML_NS, "lang");
            if (!lang.isEmpty()) {
                sprachen.put(lang, value.getTextContent().strip());
            }
        }
        return sprachen;
    }

    private void pruefeVollstaendigkeit(Map<String, String> vorhanden, Set<String> erwartet,
            String kontext, SoftAssertions soft) {
        Set<String> fehlend = new TreeSet<>(erwartet);
        fehlend.removeAll(vorhanden.keySet());
        soft.assertThat(fehlend)
                .as("Fehlende Sprachversionen in %s", kontext)
                .isEmpty();

        List<String> leer = vorhanden.entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        soft.assertThat(leer)
                .as("Leere Übersetzungen in %s", kontext)
                .isEmpty();
    }

    /** Liefert den {@code oor:name} des Elternknotens als Kontext für Fehlermeldungen. */
    private String knotenKontext(Element prop) {
        Node parent = prop.getParentNode();
        if (parent instanceof Element parentEl) {
            String name = parentEl.getAttributeNS(OOR_NS, "name");
            if (!name.isEmpty()) return name;
        }
        return "?";
    }
}
