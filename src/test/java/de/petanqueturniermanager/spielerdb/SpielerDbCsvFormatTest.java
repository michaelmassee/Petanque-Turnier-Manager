package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpielerDbCsvFormatTest {

    @Test
    void formatMarkerZeile_enthaeltVersion() {
        assertThat(SpielerDbCsvFormat.formatMarkerZeile()).isEqualTo("# PTM-SpielerDB-CSV;version=1");
    }

    @Test
    void istMarkerZeile_erkenntPraefixUnabhaengigVonVersion() {
        assertThat(SpielerDbCsvFormat.istMarkerZeile("# PTM-SpielerDB-CSV;version=1")).isTrue();
        assertThat(SpielerDbCsvFormat.istMarkerZeile("# PTM-SpielerDB-CSV;version=99")).isTrue();
        assertThat(SpielerDbCsvFormat.istMarkerZeile("vorname;nachname")).isFalse();
    }

    @Test
    void leseVersion_liestGueltigeVersion() {
        assertThat(SpielerDbCsvFormat.leseVersion("# PTM-SpielerDB-CSV;version=1")).isEqualTo(1);
        assertThat(SpielerDbCsvFormat.leseVersion("# PTM-SpielerDB-CSV;version=42")).isEqualTo(42);
    }

    @Test
    void leseVersion_ohneVersionAngabe_liefertNull() {
        assertThat(SpielerDbCsvFormat.leseVersion("# PTM-SpielerDB-CSV")).isNull();
    }

    @Test
    void leseVersion_versionOhneZiffern_liefertNull() {
        assertThat(SpielerDbCsvFormat.leseVersion("# PTM-SpielerDB-CSV;version=abc")).isNull();
    }

    @Test
    void leseVersion_ueberlaufWertLiefertNull() {
        // Kein gültiger int -> NumberFormatException intern abgefangen
        assertThat(SpielerDbCsvFormat.leseVersion("# PTM-SpielerDB-CSV;version=99999999999999")).isNull();
    }
}
