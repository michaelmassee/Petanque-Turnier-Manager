package de.petanqueturniermanager.spielerdb.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.export.ExportEntity;

class SpielerDbJsonImportReaderTest {

    private static final String JSON = """
            {
              "meta": {"version": 1, "exportedAt": "2026-05-07T10:00:00Z"},
              "spieler": [
                {"nr": 1, "vorname": "Hans", "nachname": "Müller", "vereinNr": 10, "lizenznr": "LIZ-1"},
                {"nr": 2, "vorname": "Anna", "nachname": "Schmidt", "vereinNr": null, "lizenznr": null}
              ],
              "vereine": [
                {"nr": 10, "name": "BC Linden"}
              ],
              "labels": [
                {"nr": 5, "name": "Senior 60+"}
              ],
              "spielerLabels": [
                {"spielerNr": 1, "labelNr": 5}
              ]
            }
            """;

    @Test
    void liestAlleSektionen(@TempDir Path tmp) throws Exception {
        Path datei = tmp.resolve("export.json");
        Files.writeString(datei, JSON, StandardCharsets.UTF_8);

        ImportRequest req = new ImportRequest(SpielerDbDateiFormat.JSON,
                EnumSet.of(ExportEntity.SPIELER, ExportEntity.VEREINE, ExportEntity.LABELS),
                datei, ImportModus.AKTUALISIEREN, false);
        ImportRohdaten rd = new SpielerDbJsonImportReader().read(req);

        assertThat(rd.spieler()).hasSize(2);
        assertThat(rd.spieler().get(0).vorname()).isEqualTo("Hans");
        assertThat(rd.spieler().get(0).vereinNr()).isEqualTo(10);
        assertThat(rd.spieler().get(0).lizenznr()).isEqualTo("LIZ-1");
        assertThat(rd.spieler().get(1).vereinNr()).isNull();
        assertThat(rd.spieler().get(1).lizenznr()).isNull();
        assertThat(rd.vereine()).hasSize(1);
        assertThat(rd.labels()).hasSize(1);
        assertThat(rd.spielerLabels()).hasSize(1);
    }

    @Test
    void fehlendeSektionImScope_wirft(@TempDir Path tmp) throws Exception {
        Path datei = tmp.resolve("export.json");
        Files.writeString(datei, "{\"meta\":{\"version\":1}}", StandardCharsets.UTF_8);

        ImportRequest req = new ImportRequest(SpielerDbDateiFormat.JSON,
                EnumSet.of(ExportEntity.SPIELER), datei, ImportModus.AKTUALISIEREN, false);
        assertThatThrownBy(() -> new SpielerDbJsonImportReader().read(req))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("spieler");
    }

    @Test
    void kaputteJson_wirft(@TempDir Path tmp) throws Exception {
        Path datei = tmp.resolve("export.json");
        Files.writeString(datei, "{ kaputt", StandardCharsets.UTF_8);

        ImportRequest req = new ImportRequest(SpielerDbDateiFormat.JSON,
                EnumSet.of(ExportEntity.SPIELER), datei, ImportModus.AKTUALISIEREN, false);
        assertThatThrownBy(() -> new SpielerDbJsonImportReader().read(req))
                .isInstanceOf(SpielerDbException.class);
    }
}
