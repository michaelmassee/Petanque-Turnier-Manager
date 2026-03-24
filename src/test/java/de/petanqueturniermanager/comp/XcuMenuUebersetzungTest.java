package de.petanqueturniermanager.comp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
 * Jeder Menüeintrag muss alle fünf Sprachen enthalten und darf keine leeren Werte haben.
 * <p>
 * Neue XCU-Dateien nach dem Muster {@code Addons_*.xcu} werden automatisch berücksichtigt.
 */
class XcuMenuUebersetzungTest {

    private static final Set<String> ERWARTETE_SPRACHEN = Set.of("de", "en", "es", "fr", "nl");
    private static final String OOR_NS = "http://openoffice.org/2001/registry";
    private static final String XML_NS = "http://www.w3.org/XML/1998/namespace";

    @Test
    void alleMenuTitelHabenAlleSprachversionen() throws Exception {
        SoftAssertions soft = new SoftAssertions();

        alleAddonsXcuDateien().forEach(xcu -> {
            try {
                pruefeXcuDatei(xcu, soft);
            } catch (Exception e) {
                throw new RuntimeException("Fehler beim Parsen von " + xcu, e);
            }
        });

        soft.assertAll();
    }

    @Test
    void mindestensEineXcuDateiGefunden() throws Exception {
        assertThat(alleAddonsXcuDateien())
                .as("Keine Addons_*.xcu-Dateien im registry-Verzeichnis gefunden")
                .isNotEmpty();
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────

    private List<Path> alleAddonsXcuDateien() throws Exception {
        List<Path> dateien = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Paths.get("registry"))) {
            files.filter(p -> p.getFileName().toString().startsWith("Addons_")
                           && p.toString().endsWith(".xcu"))
                 .sorted()
                 .forEach(dateien::add);
        }
        return dateien;
    }

    private void pruefeXcuDatei(Path xcu, SoftAssertions soft) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var doc = factory.newDocumentBuilder().parse(xcu.toFile());

        NodeList props = doc.getElementsByTagName("prop");
        for (int i = 0; i < props.getLength(); i++) {
            Element prop = (Element) props.item(i);
            if (!"Title".equals(prop.getAttributeNS(OOR_NS, "name"))) continue;

            // Alle value-Kinder mit xml:lang einlesen: lang → text
            Map<String, String> vorhandeneSprachen = new LinkedHashMap<>();
            NodeList values = prop.getElementsByTagName("value");
            for (int j = 0; j < values.getLength(); j++) {
                Element value = (Element) values.item(j);
                String lang = value.getAttributeNS(XML_NS, "lang");
                if (!lang.isEmpty()) {
                    vorhandeneSprachen.put(lang, value.getTextContent().strip());
                }
            }

            // Kein xml:lang → kein Menüeintrag (z.B. Separator-URL), überspringen
            if (vorhandeneSprachen.isEmpty()) continue;

            String kontext = "%s [%s]".formatted(xcu.getFileName(), knotenKontext(prop));

            // 1. Fehlende Sprachen
            Set<String> fehlend = new TreeSet<>(ERWARTETE_SPRACHEN);
            fehlend.removeAll(vorhandeneSprachen.keySet());
            soft.assertThat(fehlend)
                    .as("Fehlende Sprachversionen in %s", kontext)
                    .isEmpty();

            // 2. Leere Übersetzungen
            List<String> leer = vorhandeneSprachen.entrySet().stream()
                    .filter(e -> e.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
            soft.assertThat(leer)
                    .as("Leere Übersetzungen in %s", kontext)
                    .isEmpty();
        }
    }

    /** Liefert den oor:name des Elternknotens als Kontext für Fehlermeldungen. */
    private String knotenKontext(Element prop) {
        Node parent = prop.getParentNode();
        if (parent instanceof Element parentEl) {
            String name = parentEl.getAttributeNS(OOR_NS, "name");
            if (!name.isEmpty()) return name;
        }
        return "?";
    }
}
