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
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpieler;

class SpielerDbCsvImportReaderTest {

    @Test
    void liestFlacheCsvMitMarker(@TempDir Path tmp) throws Exception {
        Path datei = schreibeUtf8Mitbom(tmp.resolve("spieler.csv"),
                "# PTM-SpielerDB-CSV;version=1\r\n"
                        + "vorname;nachname;verein;lizenznr\r\n"
                        + "Hans;Müller;BC Linden;LIZ-1\r\n"
                        + "Anna;\"Schmidt; jr.\";;\r\n");
        ImportRohdaten rd = leseFlach(datei);

        assertThat(rd.spieler()).hasSize(2);
        RohSpieler hans = rd.spieler().get(0);
        assertThat(hans.vorname()).isEqualTo("Hans");
        assertThat(hans.nachname()).isEqualTo("Müller");
        assertThat(hans.vereinName()).isEqualTo("BC Linden");
        assertThat(hans.lizenznr()).isEqualTo("LIZ-1");
        assertThat(hans.nr()).isNull();
        assertThat(hans.vereinNr()).isNull();
        assertThat(hans.quellZeile()).isEqualTo(3); // Marker=1, Header=2, Daten=3

        RohSpieler anna = rd.spieler().get(1);
        assertThat(anna.nachname()).isEqualTo("Schmidt; jr.");
        assertThat(anna.vereinName()).isNull();
        assertThat(anna.lizenznr()).isNull();
        assertThat(anna.quellZeile()).isEqualTo(4);

        assertThat(rd.vereine()).isEmpty();
        assertThat(rd.labels()).isEmpty();
        assertThat(rd.spielerLabels()).isEmpty();
    }

    @Test
    void liestOhneMarker_tolerant(@TempDir Path tmp) throws Exception {
        Path datei = schreibeUtf8Mitbom(tmp.resolve("spieler.csv"),
                "vorname;nachname;verein;lizenznr\r\n"
                        + "Hans;Müller;BC Linden;\r\n");
        ImportRohdaten rd = leseFlach(datei);
        assertThat(rd.spieler()).hasSize(1);
        assertThat(rd.spieler().get(0).vereinName()).isEqualTo("BC Linden");
    }

    @Test
    void zukuenftigeVersion_wirft(@TempDir Path tmp) throws Exception {
        Path datei = schreibeUtf8Mitbom(tmp.resolve("spieler.csv"),
                "# PTM-SpielerDB-CSV;version=99\r\n"
                        + "vorname;nachname;verein;lizenznr\r\n"
                        + "Hans;Müller;BC Linden;\r\n");
        assertThatThrownBy(() -> leseFlach(datei))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("Version 99");
    }

    @Test
    void falscherHeader_wirft(@TempDir Path tmp) throws Exception {
        Path datei = schreibeUtf8Mitbom(tmp.resolve("spieler.csv"),
                "vorname;name;verein;lizenz\r\nHans;Müller;BC Linden;\r\n");
        assertThatThrownBy(() -> leseFlach(datei))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("Header-Spalte 2");
    }

    @Test
    void leereDatei_wirft(@TempDir Path tmp) throws Exception {
        Path datei = tmp.resolve("spieler.csv");
        Files.writeString(datei, "", StandardCharsets.UTF_8);
        assertThatThrownBy(() -> leseFlach(datei))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("leer");
    }

    @Test
    void normalisiertWhitespaceUndNfc(@TempDir Path tmp) throws Exception {
        Path datei = schreibeUtf8Mitbom(tmp.resolve("spieler.csv"),
                "# PTM-SpielerDB-CSV;version=1\r\n"
                        + "vorname;nachname;verein;lizenznr\r\n"
                        + "  Hans  ;Müller;BC  Linden ;\r\n");
        ImportRohdaten rd = leseFlach(datei);
        RohSpieler s = rd.spieler().get(0);
        assertThat(s.vorname()).isEqualTo("Hans");
        assertThat(s.vereinName()).isEqualTo("BC Linden");
        assertThat(s.lizenznr()).isNull();
    }

    @Test
    void quotingFunktioniert(@TempDir Path tmp) throws Exception {
        Path datei = schreibeUtf8Mitbom(tmp.resolve("spieler.csv"),
                "# PTM-SpielerDB-CSV;version=1\r\n"
                        + "vorname;nachname;verein;lizenznr\r\n"
                        + "\"Hans \"\"Hansi\"\"\";Müller;\"Linden;Mitte\";\r\n");
        ImportRohdaten rd = leseFlach(datei);
        RohSpieler s = rd.spieler().get(0);
        assertThat(s.vorname()).isEqualTo("Hans \"Hansi\"");
        assertThat(s.vereinName()).isEqualTo("Linden;Mitte");
    }

    private static ImportRohdaten leseFlach(Path datei) throws SpielerDbException {
        ImportRequest req = new ImportRequest(SpielerDbDateiFormat.CSV,
                EnumSet.of(ExportEntity.SPIELER), datei, ImportModus.AKTUALISIEREN, false);
        return new SpielerDbCsvImportReader().read(req);
    }

    private static Path schreibeUtf8Mitbom(Path datei, String inhalt) throws java.io.IOException {
        // Test-Inputs schreiben wir bewusst ohne BOM — der Reader muss beides
        // verkraften. Roundtrip-Inputs aus dem Exporter haben BOM, das wird in
        // den Roundtrip-Tests von SpielerDbCsvExporterTest geprüft.
        Files.writeString(datei, inhalt, StandardCharsets.UTF_8);
        return datei;
    }
}
