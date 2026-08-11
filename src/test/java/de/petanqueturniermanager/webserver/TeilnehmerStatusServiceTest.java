package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerNamenLeser;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

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

    @Test
    void zaehlschemaFaelltBeiFormationNurTeamnameAufTeamnameSpalteZurueck() {
        var zellen = new TestZellen()
                .set(1, 1, I18n.get("column.header.teamname"));

        var schema = TeilnehmerStatusService.zaehlschema(zellen::text);

        assertThat(schema.ersteDatenZeile()).isEqualTo(2);
        assertThat(schema.namensSpalten()).containsExactly(1);
    }

    @Test
    void namenListenNutzenTeamnameSpalteAuchOhneTeamNrLookup() {
        SheetHelper sheetHelper = mock(SheetHelper.class);
        XSpreadsheet sheet = mock(XSpreadsheet.class);

        when(sheetHelper.getTextFromCell(eq(sheet), any(Position.class))).thenAnswer(invocation -> {
            Position pos = invocation.getArgument(1);
            if (pos.getSpalte() == 1 && pos.getZeile() == 2) {
                return "Team Alpha";
            }
            return "";
        });
        when(sheetHelper.getIntFromCell(eq(sheet), any(Position.class))).thenAnswer(invocation -> {
            Position pos = invocation.getArgument(1);
            if (pos.getSpalte() == 2 && pos.getZeile() == 2) {
                return 1;
            }
            return 0;
        });

        var schema = new TeilnehmerStatusService.Zaehlschema(2, List.of(1));
        var keineGemapptenNamen = new TeilnehmerNamenLeser.TeilnehmerNamen(Map.of(), Map.of(), Map.of());

        var listen = TeilnehmerStatusService.namenListen(sheetHelper, sheet, 2, schema, keineGemapptenNamen);

        assertThat(listen.angemeldetNichtEingecheckt()).isEmpty();
        assertThat(listen.eingecheckt()).containsExactly("Team Alpha");
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
