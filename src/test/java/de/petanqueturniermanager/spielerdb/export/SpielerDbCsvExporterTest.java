package de.petanqueturniermanager.spielerdb.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.petanqueturniermanager.spielerdb.SpielerDbCsvFormat;
import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.importer.ImportModus;
import de.petanqueturniermanager.spielerdb.importer.ImportRequest;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpieler;
import de.petanqueturniermanager.spielerdb.importer.SpielerDbCsvImportReader;

class SpielerDbCsvExporterTest {

    @Test
    void schreibtMarkerHeaderUndBom(@TempDir Path tmp) throws Exception {
        Path datei = tmp.resolve("export.csv");
        SpielerDbExportData data = baueDaten(List.of(
                spieler(1, "Hans", "Müller", "BC Linden", "LIZ-1")));

        new SpielerDbCsvExporter().export(data, requestFuer(datei));

        String inhalt = Files.readString(datei, StandardCharsets.UTF_8);
        assertThat(inhalt).startsWith("﻿# PTM-SpielerDB-CSV;version=1");
        assertThat(inhalt).contains("vorname;nachname;verein;lizenznr");
        assertThat(inhalt).contains("Hans;Müller;BC Linden;LIZ-1");
    }

    @Test
    void sortiertNachNachnameVornameVerein(@TempDir Path tmp) throws Exception {
        Path datei = tmp.resolve("export.csv");
        SpielerDbExportData data = baueDaten(List.of(
                spieler(1, "Zoe", "Müller", "BC Eiche", null),
                spieler(2, "Anna", "Müller", "BC Linden", null),
                spieler(3, "Anna", "Müller", "BC Eiche", null),
                spieler(4, "Bert", "Adler", null, null)));

        new SpielerDbCsvExporter().export(data, requestFuer(datei));

        List<String> zeilen = Files.readAllLines(datei, StandardCharsets.UTF_8);
        // Marker, Header, dann sortierte Daten
        assertThat(zeilen).hasSize(6);
        assertThat(zeilen.get(2)).startsWith("Bert;Adler;");
        assertThat(zeilen.get(3)).startsWith("Anna;Müller;BC Eiche");
        assertThat(zeilen.get(4)).startsWith("Anna;Müller;BC Linden");
        assertThat(zeilen.get(5)).startsWith("Zoe;Müller;BC Eiche");
    }

    @Test
    void roundtripExportImport_erhaeltSpielerInhalte(@TempDir Path tmp) throws Exception {
        Path datei = tmp.resolve("export.csv");
        SpielerDbExportData data = baueDaten(List.of(
                spieler(1, "Hans \"Hansi\"", "Müller", "Linden;Mitte", "LIZ-1"),
                spieler(2, "Anna", "Schmidt", null, null),
                spieler(3, "Pierre", "Lefèvre", "PC Köln", "FR-12345")));

        new SpielerDbCsvExporter().export(data, requestFuer(datei));

        ImportRohdaten rd = new SpielerDbCsvImportReader().read(
                new ImportRequest(SpielerDbDateiFormat.CSV, EnumSet.of(ExportEntity.SPIELER),
                        datei, ImportModus.AKTUALISIEREN, false));
        assertThat(rd.spieler()).hasSize(3);

        // Suche per Nachname (Reihenfolge ist sortiert).
        RohSpieler hans = rd.spieler().stream()
                .filter(s -> "Müller".equals(s.nachname())).findFirst().orElseThrow();
        assertThat(hans.vorname()).isEqualTo("Hans \"Hansi\"");
        assertThat(hans.vereinName()).isEqualTo("Linden;Mitte");
        assertThat(hans.lizenznr()).isEqualTo("LIZ-1");

        RohSpieler anna = rd.spieler().stream()
                .filter(s -> "Schmidt".equals(s.nachname())).findFirst().orElseThrow();
        assertThat(anna.vereinName()).isNull();
        assertThat(anna.lizenznr()).isNull();

        RohSpieler pierre = rd.spieler().stream()
                .filter(s -> "Lefèvre".equals(s.nachname())).findFirst().orElseThrow();
        assertThat(pierre.vereinName()).isEqualTo("PC Köln");
    }

    @Test
    void formatMarker_versionsBumpsErkennbar() {
        assertThat(SpielerDbCsvFormat.formatMarkerZeile()).contains("version=" + SpielerDbCsvFormat.VERSION);
        assertThat(SpielerDbCsvFormat.istMarkerZeile("# PTM-SpielerDB-CSV;version=42")).isTrue();
        assertThat(SpielerDbCsvFormat.leseVersion("# PTM-SpielerDB-CSV;version=7")).isEqualTo(7);
        assertThat(SpielerDbCsvFormat.leseVersion("# beliebiger kommentar")).isNull();
    }

    private static SpielerDbExportData baueDaten(List<SpielerMitVerein> spieler) {
        return new SpielerDbExportData(
                new ExportMeta(1, Instant.parse("2026-05-08T00:00:00Z"), null),
                spieler, List.of(), List.of(), List.of());
    }

    private static SpielerMitVerein spieler(int nr, String vorname, String nachname,
            String vereinName, String lizenznr) {
        return new SpielerMitVerein(nr, vorname, nachname,
                vereinName == null ? null : 1, vereinName,
                List.of(), List.of(), lizenznr);
    }

    private static ExportRequest requestFuer(Path datei) {
        return new ExportRequest(SpielerDbDateiFormat.CSV,
                EnumSet.of(ExportEntity.SPIELER), new AllExportFilter(), datei);
    }
}
