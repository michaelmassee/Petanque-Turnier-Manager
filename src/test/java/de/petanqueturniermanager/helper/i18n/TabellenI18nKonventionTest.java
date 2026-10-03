/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Verhindert sichtbare, nicht übersetzbare Textliterale in Calc-Zellen.
 *
 * <p>Die Prüfung erfasst die zwei üblichen Schreibwege für {@code StringCellValue}.
 * Technische Kennzeichen wie A/B, mathematische Symbole und Calc-Formeln sind keine
 * Texte für Anwender und bleiben bewusst ausgenommen.</p>
 */
class TabellenI18nKonventionTest {

    private static final Path QUELL_ORDNER = Path.of("src/main/java");
    private static final Pattern ZELL_TEXT_LITERAL = Pattern.compile(
            "(?:StringCellValue\\.from|\\.setValue)\\([^\\r\\n]*\\\"([^\\\"]*)\\\"");

    @Test
    void sichtbareTabellenTexteSindI18n() throws IOException {
        List<String> fundstellen = new ArrayList<>();
        try (var dateien = Files.walk(QUELL_ORDNER)) {
            dateien.filter(datei -> datei.toString().endsWith(".java"))
                    .forEach(datei -> pruefeDatei(datei, fundstellen));
        }

        assertThat(fundstellen)
                .as("Sichtbare Tabellen-Texte müssen I18n.get(...) verwenden; technische Kennzeichen und Formeln sind ausgenommen")
                .isEmpty();
    }

    private static void pruefeDatei(Path datei, List<String> fundstellen) {
        try {
            List<String> zeilen = Files.readAllLines(datei, StandardCharsets.UTF_8);
            for (int index = 0; index < zeilen.size(); index++) {
                String zeile = zeilen.get(index);
                int kommentar = zeile.indexOf("//");
                if (kommentar >= 0) {
                    zeile = zeile.substring(0, kommentar);
                }
                if (zeile.contains("I18n.get(")) {
                    continue;
                }
                Matcher matcher = ZELL_TEXT_LITERAL.matcher(zeile);
                while (matcher.find()) {
                    String literal = matcher.group(1);
                    if (istSichtbarerText(literal)) {
                        fundstellen.add(QUELL_ORDNER.relativize(datei) + ":" + (index + 1) + " = \"" + literal + "\"");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Quelldatei nicht lesbar: " + datei, e);
        }
    }

    private static boolean istSichtbarerText(String literal) {
        if (literal.isBlank() || !literal.matches("[\\p{L}\\p{N} .-]+")
                || literal.matches("[ABx]") || literal.matches("[+\\-#Δ∑0-9x]+")) {
            return false;
        }
        return !(literal.startsWith("=") || literal.startsWith("IF(") || literal.startsWith("INDIRECT("));
    }
}
