/*
 * Erstellung 20.03.2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit-Tests für {@link I18n}. Kein LibreOffice-Kontext erforderlich.
 */
class I18nTest {

    @BeforeEach
    void zuruecksetzen() throws Exception {
        // Singleton-Zustand zurücksetzen damit jeder Test frisch startet
        Field initialisiert = I18n.class.getDeclaredField("initialisiert");
        initialisiert.setAccessible(true);
        ((AtomicBoolean) initialisiert.get(null)).set(false);

        Field bundle = I18n.class.getDeclaredField("bundle");
        bundle.setAccessible(true);
        bundle.set(null, null);

        Field fallback = I18n.class.getDeclaredField("fallback");
        fallback.setAccessible(true);
        fallback.set(null, null);
    }

    @Test
    void init_ohneKontext_laedt_deutschesFallback() {
        I18n.init(null);
        // Bekannter Schlüssel muss deutschen Text liefern
        assertThat(I18n.get("msg.caption.abbruch")).isEqualTo("Abbruch");
    }

    @Test
    void get_vorInit_gibtSchluesselZurueck() {
        // Kein init() aufgerufen → bundle ist null → Schlüssel wird zurückgegeben
        String ergebnis = I18n.get("msg.caption.abbruch");
        assertThat(ergebnis).isEqualTo("msg.caption.abbruch");
    }

    @Test
    void get_unbekannterSchluessel_gibtSchluesselZurueck() {
        I18n.init(null);
        String ergebnis = I18n.get("schluessel.gibt.es.nicht");
        assertThat(ergebnis).isEqualTo("schluessel.gibt.es.nicht");
    }

    @Test
    void get_mitParameter_formatiertKorrekt() {
        I18n.init(null);
        String ergebnis = I18n.get("msg.text.ungueltige.anzahl.spieler", 3);
        assertThat(ergebnis).contains("3");
    }

    @Test
    void get_enumTurnierSystem_liefertBezeichnung() {
        I18n.init(null);
        assertThat(I18n.get("enum.turniersystem.supermelee")).isNotEmpty();
        assertThat(I18n.get("enum.turniersystem.ko")).isNotEmpty();
    }

    @Test
    void get_mitEinzelnemApostroph_ersetztPlatzhalterTrotzdem() {
        // Regressionstest: ein einzelnes, nicht verdoppeltes ' darf keinen Quote-Block auslösen
        // (frühere MessageFormat-basierte Implementierung hat hier {0} verschluckt)
        I18n.initFuerTest(Locale.FRENCH);
        String ergebnis = I18n.get("error.tabelle.nicht.vorhanden", "Meldeliste");
        assertThat(ergebnis).isEqualTo("Le tableau 'Meldeliste' n'existe pas.");
    }

    @Test
    void get_mitVerdoppeltemApostroph_liefertLiteralenApostroph() {
        // ''{0}'' ist die bestehende Konvention für "Parameter in Anführungszeichen zeigen"
        I18n.init(null);
        String ergebnis = I18n.get("error.tabelle.nicht.vorhanden", "Meldeliste");
        assertThat(ergebnis).isEqualTo("Die Tabelle 'Meldeliste' ist nicht vorhanden.");
    }

    @Test
    void init_zweimalAufgerufen_keineAenderung() {
        I18n.init(null);
        String ersterWert = I18n.get("msg.caption.abbruch");
        I18n.init(null); // zweiter Aufruf muss ignoriert werden
        assertThat(I18n.get("msg.caption.abbruch")).isEqualTo(ersterWert);
    }
}
