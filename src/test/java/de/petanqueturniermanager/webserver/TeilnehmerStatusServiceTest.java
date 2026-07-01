package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.i18n.I18n;

class TeilnehmerStatusServiceTest {

    @Test
    void zaehlschemaErkenntDreizeiligeMeldelisteUndStartetNachDrittemHeader() {
        var zellen = new TestZellen()
                .set(2, 2, I18n.get("column.header.vorname"))
                .set(3, 2, I18n.get("column.header.nachname"));

        var schema = TeilnehmerStatusService.zaehlschema(zellen::text);

        assertThat(schema.ersteDatenZeile()).isEqualTo(3);
        assertThat(schema.namensSpalten()).containsExactly(2, 3);
    }

    @Test
    void zaehlschemaErkenntZweizeiligeMeldelisteUndStartetNachZweitemHeader() {
        var zellen = new TestZellen()
                .set(1, 1, I18n.get("column.header.vorname"))
                .set(2, 1, I18n.get("column.header.nachname"));

        var schema = TeilnehmerStatusService.zaehlschema(zellen::text);

        assertThat(schema.ersteDatenZeile()).isEqualTo(2);
        assertThat(schema.namensSpalten()).containsExactly(1, 2);
    }

    @Test
    void zaehlschemaIgnoriertTeamnameAlsNamensspalte() {
        var zellen = new TestZellen()
                .set(1, 2, I18n.get("column.header.teamname"))
                .set(2, 2, I18n.get("column.header.vorname"))
                .set(3, 2, I18n.get("column.header.nachname"));

        var schema = TeilnehmerStatusService.zaehlschema(zellen::text);

        assertThat(schema.namensSpalten()).containsExactly(2, 3);
    }

    private static final class TestZellen {
        private final Map<String, String> werte = new HashMap<>();

        TestZellen set(int spalte, int zeile, String wert) {
            werte.put(key(spalte, zeile), wert);
            return this;
        }

        String text(int spalte, int zeile) {
            return werte.getOrDefault(key(spalte, zeile), "");
        }

        private String key(int spalte, int zeile) {
            return spalte + ":" + zeile;
        }
    }
}
