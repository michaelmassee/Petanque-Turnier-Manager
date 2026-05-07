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

class SpielerDbCsvImportReaderTest {

    @Test
    void liestSpielerVereineLabelsUndJunction(@TempDir Path tmp) throws Exception {
        schreibeUtf8(tmp.resolve("spieler.csv"),
                "﻿nr;vorname;nachname;vereinNr;vereinName;lizenznr\r\n"
                        + "1;Hans;Müller;10;BC Linden;LIZ-1\r\n"
                        + "2;Anna;\"Schmidt; jr.\";;;\r\n");
        schreibeUtf8(tmp.resolve("vereine.csv"),
                "﻿nr;name\r\n10;BC Linden\r\n11;BC Eiche\r\n");
        schreibeUtf8(tmp.resolve("labels.csv"),
                "﻿nr;name\r\n5;Senior 60+\r\n");
        schreibeUtf8(tmp.resolve("spieler_labels.csv"),
                "﻿spielerNr;labelNr\r\n1;5\r\n2;5\r\n");

        ImportRequest req = new ImportRequest(SpielerDbDateiFormat.CSV,
                EnumSet.of(ExportEntity.SPIELER, ExportEntity.VEREINE, ExportEntity.LABELS),
                tmp, ImportModus.AKTUALISIEREN, false);
        ImportRohdaten rd = new SpielerDbCsvImportReader().read(req);

        assertThat(rd.spieler()).hasSize(2);
        assertThat(rd.spieler().get(0).vorname()).isEqualTo("Hans");
        assertThat(rd.spieler().get(0).nachname()).isEqualTo("Müller");
        assertThat(rd.spieler().get(0).vereinNr()).isEqualTo(10);
        assertThat(rd.spieler().get(0).vereinName()).isEqualTo("BC Linden");
        assertThat(rd.spieler().get(0).lizenznr()).isEqualTo("LIZ-1");
        assertThat(rd.spieler().get(1).nachname()).isEqualTo("Schmidt; jr.");
        assertThat(rd.spieler().get(1).vereinNr()).isNull();
        assertThat(rd.spieler().get(1).lizenznr()).isNull();

        assertThat(rd.vereine()).hasSize(2);
        assertThat(rd.labels()).hasSize(1);
        assertThat(rd.spielerLabels()).hasSize(2);
    }

    @Test
    void scopeBegrenztGelesene(@TempDir Path tmp) throws Exception {
        schreibeUtf8(tmp.resolve("vereine.csv"), "﻿nr;name\r\n10;BC Linden\r\n");

        ImportRequest req = new ImportRequest(SpielerDbDateiFormat.CSV,
                EnumSet.of(ExportEntity.VEREINE), tmp, ImportModus.AKTUALISIEREN, false);
        ImportRohdaten rd = new SpielerDbCsvImportReader().read(req);

        assertThat(rd.vereine()).hasSize(1);
        assertThat(rd.spieler()).isEmpty();
        assertThat(rd.labels()).isEmpty();
        assertThat(rd.spielerLabels()).isEmpty();
    }

    @Test
    void fehlendeDateiImScope_wirft(@TempDir Path tmp) {
        ImportRequest req = new ImportRequest(SpielerDbDateiFormat.CSV,
                EnumSet.of(ExportEntity.VEREINE), tmp, ImportModus.AKTUALISIEREN, false);
        assertThatThrownBy(() -> new SpielerDbCsvImportReader().read(req))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("vereine.csv");
    }

    @Test
    void falscherHeader_wirft(@TempDir Path tmp) throws Exception {
        schreibeUtf8(tmp.resolve("vereine.csv"), "id;bezeichnung\r\n10;BC Linden\r\n");
        ImportRequest req = new ImportRequest(SpielerDbDateiFormat.CSV,
                EnumSet.of(ExportEntity.VEREINE), tmp, ImportModus.AKTUALISIEREN, false);
        assertThatThrownBy(() -> new SpielerDbCsvImportReader().read(req))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("Header-Spalte 1");
    }

    private static void schreibeUtf8(Path datei, String inhalt) throws java.io.IOException {
        Files.writeString(datei, inhalt, StandardCharsets.UTF_8);
    }
}
