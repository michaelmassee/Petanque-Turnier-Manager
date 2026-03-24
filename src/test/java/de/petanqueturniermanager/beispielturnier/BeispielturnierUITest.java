/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.beispielturnier;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.beispielturnier.BeispielturnierRegistrierung.Eintrag;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Parameterisierter UI-Test: Führt alle in {@link BeispielturnierRegistrierung} registrierten
 * Beispielturniere vollständig durch und prüft das Ergebnis.
 * <p>
 * Pro Turnier wird geprüft:
 * <ol>
 *   <li>Generierung läuft fehlerfrei durch (keine Exception)</li>
 *   <li>Alle erwarteten Sheets sind vorhanden (Suche per Metadaten-Schlüssel –
 *       locale-unabhängig)</li>
 * </ol>
 * <p>
 * <strong>Locale:</strong> Wird über die System-Property {@code beispielturnier.locale}
 * gesteuert (Standard: {@code de}).
 * <p>
 * <strong>Ausführung:</strong>
 * <pre>
 *   ./gradlew beispielturnierTests              # Deutsch
 *   ./gradlew beispielturnierTests -Plocale=en  # Englisch
 *   ./gradlew beispielturnierTests -Plocale=nl  # Niederländisch
 *   ./gradlew beispielturnierTests -Plocale=fr  # Französisch
 *   ./gradlew beispielturnierTests -Plocale=es  # Spanisch
 * </pre>
 */
@Tag("beispielturnier")
class BeispielturnierUITest extends BaseCalcUITest {

    /** System-Property-Name für die Ziel-Locale. */
    private static final String PROP_LOCALE = "beispielturnier.locale";

    @Override
    public void beforeTest() {
        super.beforeTest();
        // Gewünschte Locale aus System-Property lesen und I18n neu initialisieren.
        // I18n.initFuerTest() erzwingt Neuinitialisierung auch wenn bereits initialisiert.
        String localeTag = System.getProperty(PROP_LOCALE, "de");
        I18n.initFuerTest(Locale.forLanguageTag(localeTag));
    }

    static Stream<Eintrag> beispielturnierEintraege() {
        return BeispielturnierRegistrierung.alleEintraege().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("beispielturnierEintraege")
    void turnierwirdFehlerfreiGeneriert(Eintrag eintrag) throws GenerateException {
        // Generierung durchführen – Exception = Testfehler
        eintrag.generator().generiere(wkingSpreadsheet);

        // Alle erwarteten Sheets müssen vorhanden sein (Suche per Metadaten-Schlüssel)
        pruefeErwarteteSheets(eintrag.bezeichnung(), eintrag.erwarteteSchluessel(),
                wkingSpreadsheet.getWorkingSpreadsheetDocument());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("beispielturnierEintraege")
    void mindestensEinSheetWurdeAngelegt(Eintrag eintrag) throws GenerateException {
        eintrag.generator().generiere(wkingSpreadsheet);

        var sheetNamen = wkingSpreadsheet.getWorkingSpreadsheetDocument()
                .getSheets().getElementNames();
        assertThat(sheetNamen)
                .as("Turnier '%s': mindestens ein Sheet erwartet", eintrag.bezeichnung())
                .hasSizeGreaterThan(0);
    }

    // -------------------------------------------------------------------------

    private void pruefeErwarteteSheets(String turnierBezeichnung, List<String> schluessel,
            XSpreadsheetDocument xDoc) {
        SoftAssertions soft = new SoftAssertions();
        for (String schluessel1 : schluessel) {
            XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc, schluessel1, null);
            soft.assertThat(sheet)
                    .as("Turnier '%s': Sheet mit Schlüssel '%s' nicht gefunden",
                            turnierBezeichnung, schluessel1)
                    .isNotNull();
        }
        soft.assertAll();
    }
}
