/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.beispielturnier;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * Unit-Test (kein UI) zur Vollständigkeitsprüfung der {@link BeispielturnierRegistrierung}.
 * <p>
 * Prüft automatisch, ob jede Klasse in {@code src/main/java}, deren Dateiname auf
 * {@code TurnierTestDaten.java} endet, einen Eintrag in der Registrierung besitzt.
 * <p>
 * Wird eine neue {@code *TurnierTestDaten}-Klasse hinzugefügt, ohne dass ein Eintrag in
 * {@link BeispielturnierRegistrierung#alleEintraege()} ergänzt wird, schlägt dieser Test fehl.
 */
class BeispielturnierVollstaendigkeitTest {

    private static final Path QUELL_ORDNER = Path.of("src/main/java");
    private static final String DATEINAME_SUFFIX = "TurnierTestDaten.java";

    @Test
    void alleQuellklassenSindRegistriert() throws IOException {
        Set<String> registrierteQuelldateien = BeispielturnierRegistrierung.alleEintraege().stream()
                .map(BeispielturnierRegistrierung.Eintrag::quelldateiName)
                .collect(Collectors.toSet());

        List<String> gefundeneQuelldateien = alleTestdatenDateienEntdecken();

        SoftAssertions soft = new SoftAssertions();
        for (String dateiname : gefundeneQuelldateien) {
            soft.assertThat(registrierteQuelldateien)
                    .as("'%s' existiert in src/main/java, hat aber keinen Eintrag in BeispielturnierRegistrierung",
                            dateiname)
                    .contains(dateiname);
        }
        soft.assertAll();
    }

    @Test
    void alleRegistriertenQuelldateienExistieren() throws IOException {
        List<String> gefundeneQuelldateien = alleTestdatenDateienEntdecken();

        SoftAssertions soft = new SoftAssertions();
        for (var eintrag : BeispielturnierRegistrierung.alleEintraege()) {
            soft.assertThat(gefundeneQuelldateien)
                    .as("Registrierter quelldateiName '%s' wurde in src/main/java nicht gefunden",
                            eintrag.quelldateiName())
                    .contains(eintrag.quelldateiName());
        }
        soft.assertAll();
    }

    @Test
    void registrierungIsNichtLeer() {
        assertThat(BeispielturnierRegistrierung.alleEintraege())
                .as("BeispielturnierRegistrierung darf nicht leer sein")
                .isNotEmpty();
    }

    @Test
    void keineAnzahlDoppelteQuelldateiNamen() {
        List<String> quelldateiNamen = BeispielturnierRegistrierung.alleEintraege().stream()
                .map(BeispielturnierRegistrierung.Eintrag::quelldateiName)
                .toList();
        assertThat(quelldateiNamen)
                .as("Jede quelldateiName darf nur einmal in der Registrierung vorkommen")
                .doesNotHaveDuplicates();
    }

    @Test
    void keineLeereSchluesselListen() {
        SoftAssertions soft = new SoftAssertions();
        for (var eintrag : BeispielturnierRegistrierung.alleEintraege()) {
            soft.assertThat(eintrag.erwarteteSchluessel())
                    .as("Eintrag '%s' hat keine erwarteten Schlüssel", eintrag.bezeichnung())
                    .isNotEmpty();
        }
        soft.assertAll();
    }

    /**
     * Entdeckt alle Dateien in {@code src/main/java}, deren Name auf
     * {@value DATEINAME_SUFFIX} endet (z.B. {@code SchweizerTurnierTestDaten.java}).
     */
    private List<String> alleTestdatenDateienEntdecken() throws IOException {
        assertThat(QUELL_ORDNER)
                .as("Java-Quellordner nicht gefunden: %s", QUELL_ORDNER.toAbsolutePath())
                .isDirectory();
        try (var dateistrom = Files.walk(QUELL_ORDNER)) {
            return dateistrom
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(DATEINAME_SUFFIX))
                    .sorted()
                    .toList();
        }
    }
}
