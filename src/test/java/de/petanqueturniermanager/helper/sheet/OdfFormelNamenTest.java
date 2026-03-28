package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Stellt sicher, dass in Formel-Strings ausschließlich ODF-Standardfunktionsnamen (englisch)
 * verwendet werden.
 *
 * <p>Hintergrund: {@code SheetHelper.setFormulaInCell()} ruft intern {@code XCell.setFormula()}
 * auf – das ist die <em>ODF-Formelsprache</em>, die sprachunabhängig und immer englisch ist.
 * Lokalisierte Funktionsnamen (z.B. {@code SVERWEIS}, {@code WENNS}, {@code ZÄHLENWENNS})
 * führen in <em>jeder</em> Spracheinstellung zu {@code #NAME?} – der Bug betrifft also
 * alle Nutzer, nicht nur deutschsprachige.
 *
 * <p>Dieser Test durchsucht alle {@code .java}-Quelldateien nach String-Literalen, die bekannte
 * lokalisierte Funktionsnamen enthalten, und schlägt mit einer genauen Fundstellen-Liste fehl,
 * sobald ein Treffer gefunden wird.
 */
public class OdfFormelNamenTest {

    /**
     * Bekannte lokalisierte (nicht-ODF) Funktionsnamen, die in Formel-Strings verboten sind.
     * Für jeden Eintrag wird nach {@code FUNKTIONSNAME(} gesucht (Groß-/Kleinschreibung ignoriert),
     * damit normale String-Werte wie "SVERWEIS" ohne Klammer nicht fälschlich gemeldet werden.
     *
     * <p>ODF-Entsprechungen (zur Information):
     * <ul>
     *   <li>SVERWEIS      → VLOOKUP</li>
     *   <li>WVERWEIS      → HLOOKUP</li>
     *   <li>SUMMEWENN     → SUMIF</li>
     *   <li>SUMMEWENNS    → SUMIFS</li>
     *   <li>ZÄHLENWENNS   → COUNTIFS</li>
     *   <li>MITTELWERTWENN→ AVERAGEIF</li>
     *   <li>MITTELWERTWENNS→AVERAGEIFS</li>
     *   <li>WENNS         → IFS</li>
     *   <li>MINWENNS      → MINIFS</li>
     *   <li>MAXWENNS      → MAXIFS</li>
     *   <li>WECHSELN      → SUBSTITUTE</li>
     *   <li>LINKS         → LEFT</li>
     *   <li>RECHTS        → RIGHT</li>
     *   <li>LÄNGE         → LEN</li>
     *   <li>GROSS         → UPPER</li>
     *   <li>KLEIN         → LOWER</li>
     *   <li>VERKETTEN     → CONCATENATE</li>
     *   <li>VERGLEICH     → MATCH</li>
     *   <li>INDEX         → INDEX (gleich – kein Problem)</li>
     *   <li>INDIREKT      → INDIRECT</li>
     *   <li>ADRESSE       → ADDRESS</li>
     *   <li>BEREICH.VERSCHIEBEN → OFFSET</li>
     *   <li>ISTLEER       → ISBLANK</li>
     *   <li>ISTTEXT       → ISTEXT</li>
     *   <li>ISTFEHLER     → ISERROR</li>
     *   <li>ISTGERADE     → ISEVEN</li>
     *   <li>ISTUNGERADE   → ISODD</li>
     *   <li>GANZZAHL      → INT</li>
     *   <li>RUNDEN        → ROUND</li>
     *   <li>AUFRUNDEN     → ROUNDUP</li>
     *   <li>ABRUNDEN      → ROUNDDOWN</li>
     *   <li>ZUFALLSZAHL   → RAND</li>
     *   <li>ZUFALLSBEREICH → RANDBETWEEN</li>
     *   <li>HEUTE         → TODAY</li>
     *   <li>JETZT         → NOW</li>
     *   <li>WOCHENTAG     → WEEKDAY</li>
     *   <li>MONAT         → MONTH</li>
     *   <li>JAHR          → YEAR</li>
     *   <li>TAG           → DAY</li>
     *   <li>STUNDE        → HOUR</li>
     *   <li>MINUTE        → MINUTE (gleich)</li>
     *   <li>SEKUNDE       → SECOND</li>
     *   <li>DATUM         → DATE</li>
     *   <li>ZEIT          → TIME</li>
     *   <li>DATEDIF       → DATEDIF (gleich)</li>
     * </ul>
     *
     * <p>Bereits in {@code SheetHelper.FORMULA_GERMAN_SEARCH_LIST} übersetzte Funktionen
     * sind hier trotzdem aufgeführt: Sie wären zwar durch die Übersetzung gerettet, aber
     * die bevorzugte Lösung ist, sie gar nicht erst in lokalisierter Form zu schreiben.
     */
    private static final Set<String> VERBOTENE_FUNKTIONSNAMEN = Set.of(
            // Lookup & Reference
            "SVERWEIS",
            "WVERWEIS",
            "VERGLEICH",
            "INDIREKT",
            "ADRESSE",
            // Logik
            "WENNS",
            // Aggregation
            "SUMMEWENN",
            "SUMMEWENNS",
            "ZÄHLENWENNS",
            "MITTELWERTWENN",
            "MITTELWERTWENNS",
            "MINWENNS",
            "MAXWENNS",
            // Text
            "WECHSELN",
            "LINKS",
            "RECHTS",
            "LÄNGE",
            "GROSS",
            "KLEIN",
            "VERKETTEN",
            // Information
            "ISTLEER",
            "ISTTEXT",
            "ISTFEHLER",
            "ISTGERADE",
            "ISTUNGERADE",
            // Mathe
            "GANZZAHL",
            "RUNDEN",
            "AUFRUNDEN",
            "ABRUNDEN",
            "ZUFALLSZAHL",
            "ZUFALLSBEREICH",
            // Datum & Zeit
            "HEUTE",
            "JETZT",
            "WOCHENTAG",
            "MONAT",
            "JAHR",
            "STUNDE",
            "SEKUNDE",
            "DATUM",
            "ZEIT"
    );

    /** Findet String-Literale in Java-Quellcode: Inhalt zwischen {@code "…"}, ohne Escape-Handling. */
    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("\"([^\"\\\\]|\\\\.)*+\"");

    @Test
    void keineLokalisiertenFunktionsNamenInFormelStrings() throws IOException {
        var quellVerzeichnis = Paths.get("src/main/java");
        assertThat(quellVerzeichnis).as("Quellverzeichnis existiert").isDirectory();

        var verstoesze = new ArrayList<String>();

        try (var stream = Files.walk(quellVerzeichnis)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .forEach(datei -> pruefeJavaDatei(datei, verstoesze));
        }

        assertThat(verstoesze)
                .as("Lokalisierte ODF-Funktionsnamen in Formel-Strings gefunden.\n"
                        + "Verwende stattdessen den englischen ODF-Standardnamen (z.B. VLOOKUP statt SVERWEIS).\n"
                        + "Hintergrund: XCell.setFormula() ist die ODF-Formelsprache – immer englisch, "
                        + "unabhängig von der LibreOffice-Spracheinstellung des Nutzers.\n"
                        + "Fundstellen")
                .isEmpty();
    }

    private void pruefeJavaDatei(Path datei, List<String> verstoesze) {
        List<String> zeilen;
        try {
            zeilen = Files.readAllLines(datei);
        } catch (IOException e) {
            throw new RuntimeException("Datei konnte nicht gelesen werden: " + datei, e);
        }

        for (int i = 0; i < zeilen.size(); i++) {
            var zeile = zeilen.get(i);
            var zeilenNr = i + 1;
            pruefeZeile(datei, zeilenNr, zeile, verstoesze);
        }
    }

    private void pruefeZeile(Path datei, int zeilenNr, String zeile, List<String> verstoesze) {
        Matcher literalMatcher = STRING_LITERAL_PATTERN.matcher(zeile);
        while (literalMatcher.find()) {
            var literal = literalMatcher.group();
            for (var funktionsName : VERBOTENE_FUNKTIONSNAMEN) {
                // Suche nach FUNKTIONSNAME( im String-Literal (Großschreibung erzwungen da Calc-Funktionen immer Großbuchstaben)
                if (literal.toUpperCase().contains(funktionsName + "(")) {
                    verstoesze.add(String.format("  %s:%d – \"%s\" enthält \"%s(\"",
                            datei, zeilenNr, literal.replace("\"", ""), funktionsName));
                }
            }
        }
    }
}
