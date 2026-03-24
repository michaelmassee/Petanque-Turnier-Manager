/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
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

    // Direkt auf Quelldateien zeigen – nicht auf den kompilierten build-Ordner.
    // So liest der Test immer die aktuellen Dateien, auch ohne vorherigen Rebuild.
    private static final Path RESSOURCEN_BASIS = Paths.get("src/main/resources");
    private static final String I18N_ORDNER = "de/petanqueturniermanager/i18n/";
    private static final String REFERENZ_DATEI = "messages.properties";

    @Test
    void alleSprachdateienEnthaltenAlleSchluessel() throws IOException {
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
    void keineUnbekanntenSchluesselInSprachdateien() throws IOException {
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
    void mindestensEineSprachdateiVorhanden() throws IOException {
        assertThat(alleSprachdateienEntdecken())
                .as("Keine Sprachdateien (messages_*.properties) im i18n-Ordner gefunden")
                .isNotEmpty();
    }

    @Test
    void alleSheetNameSchluesselSindInReferenzdateiVorhanden() throws IOException {
        Properties referenz = laden(REFERENZ_DATEI);
        Set<String> referenzKeys = referenz.stringPropertyNames();

        List<String> alleSheetKeys = List.of(
                SheetNamen.KEY_MELDELISTE,
                SheetNamen.KEY_RANGLISTE,
                SheetNamen.KEY_DIREKTVERGLEICH,
                SheetNamen.KEY_SPIELPLAN,
                SheetNamen.KEY_ENDRANGLISTE,
                SheetNamen.KEY_SUPERMELEE_TEAMS,
                SheetNamen.KEY_KO_KONFIGURATION,
                SheetNamen.KEY_KO_RUNDE,
                SheetNamen.KEY_CADRAGE,
                SheetNamen.KEY_VORRUNDEN_ERGEBNISSE,
                SheetNamen.KEY_MAASTRICHTER_VR_RANGLISTE,
                SheetNamen.KEY_ANMELDUNGEN,
                SheetNamen.KEY_TEILNEHMER,
                SheetNamen.KEY_SPIELRUNDE_MUSTER,
                SheetNamen.KEY_SUPERMELEE_SPIELRUNDE_MUSTER,
                SheetNamen.KEY_SPIELRUNDE_PLAN_MUSTER,
                SheetNamen.KEY_SPIELTAG_RANGLISTE_MUSTER,
                SheetNamen.KEY_KO_TURNIERBAUM_EINZEL,
                SheetNamen.KEY_KO_TURNIERBAUM_GRUPPE_MUSTER,
                SheetNamen.KEY_MAASTRICHTER_VORRUNDE_MUSTER,
                SheetNamen.KEY_MAASTRICHTER_FINALRUNDE_MUSTER,
                SheetNamen.KEY_ANMELDUNGEN_MUSTER,
                SheetNamen.KEY_TEILNEHMER_MUSTER
        );

        SoftAssertions soft = new SoftAssertions();
        for (String schluessel : alleSheetKeys) {
            soft.assertThat(referenzKeys)
                    .as("sheet.name-Schlüssel '%s' fehlt in %s", schluessel, REFERENZ_DATEI)
                    .contains(schluessel);
        }
        soft.assertAll();
    }

    @Test
    void alleSheetNameLegacyWerteStimmenMitDeutscherUebersetzungUeberein() throws IOException {
        Properties referenz = laden(REFERENZ_DATEI);

        record KeyLegacy(String schluessel, String legacy) {}

        List<KeyLegacy> paare = List.of(
                new KeyLegacy(SheetNamen.KEY_MELDELISTE,             SheetNamen.LEGACY_MELDELISTE),
                new KeyLegacy(SheetNamen.KEY_RANGLISTE,              SheetNamen.LEGACY_RANGLISTE),
                new KeyLegacy(SheetNamen.KEY_DIREKTVERGLEICH,        SheetNamen.LEGACY_DIREKTVERGLEICH),
                new KeyLegacy(SheetNamen.KEY_SPIELPLAN,              SheetNamen.LEGACY_SPIELPLAN),
                new KeyLegacy(SheetNamen.KEY_ENDRANGLISTE,           SheetNamen.LEGACY_ENDRANGLISTE),
                new KeyLegacy(SheetNamen.KEY_SUPERMELEE_TEAMS,       SheetNamen.LEGACY_SUPERMELEE_TEAMS),
                new KeyLegacy(SheetNamen.KEY_KO_KONFIGURATION,       SheetNamen.LEGACY_KO_KONFIGURATION),
                new KeyLegacy(SheetNamen.KEY_KO_RUNDE,               SheetNamen.LEGACY_KO_RUNDE),
                new KeyLegacy(SheetNamen.KEY_CADRAGE,                SheetNamen.LEGACY_CADRAGE),
                new KeyLegacy(SheetNamen.KEY_VORRUNDEN_ERGEBNISSE,   SheetNamen.LEGACY_VORRUNDEN_ERGEBNISSE),
                new KeyLegacy(SheetNamen.KEY_MAASTRICHTER_VR_RANGLISTE, SheetNamen.LEGACY_MAASTRICHTER_VR_RANGLISTE),
                new KeyLegacy(SheetNamen.KEY_ANMELDUNGEN,            SheetNamen.LEGACY_ANMELDUNGEN),
                new KeyLegacy(SheetNamen.KEY_TEILNEHMER,             SheetNamen.LEGACY_TEILNEHMER),
                new KeyLegacy(SheetNamen.KEY_KO_TURNIERBAUM_EINZEL,  SheetNamen.LEGACY_KO_TURNIERBAUM_EINZEL)
        );

        SoftAssertions soft = new SoftAssertions();
        for (var paar : paare) {
            soft.assertThat(referenz.getProperty(paar.schluessel()))
                    .as("LEGACY-Wert für '%s' stimmt nicht mit deutschem Referenzwert überein", paar.schluessel())
                    .isEqualTo(paar.legacy());
        }
        soft.assertAll();
    }

    /**
     * Entdeckt automatisch alle Sprachdateien (messages_*.properties) im i18n-Ordner.
     * Liest direkt aus src/main/resources – unabhängig vom build-Ordner.
     */
    private List<String> alleSprachdateienEntdecken() throws IOException {
        Path ordner = RESSOURCEN_BASIS.resolve(I18N_ORDNER);
        assertThat(ordner)
                .as("i18n-Ordner nicht gefunden: %s", ordner.toAbsolutePath())
                .isDirectory();
        try (var dateien = Files.list(ordner)) {
            return dateien
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith("messages_") && name.endsWith(".properties"))
                    .sorted()
                    .toList();
        }
    }

    private Properties laden(String dateiname) throws IOException {
        Path pfad = RESSOURCEN_BASIS.resolve(I18N_ORDNER).resolve(dateiname);
        assertThat(pfad)
                .as("i18n-Datei nicht gefunden: %s", pfad.toAbsolutePath())
                .isRegularFile();
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(pfad, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }

}
