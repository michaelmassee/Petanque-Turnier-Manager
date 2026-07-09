package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat.ZielTyp;

class SpielerDbDateiFormatTest {

    @BeforeEach
    void setUp() {
        I18n.init(null);
    }

    @Test
    void csv_zielTypDatei() {
        assertThat(SpielerDbDateiFormat.CSV.zielTyp()).isEqualTo(ZielTyp.DATEI);
        assertThat(SpielerDbDateiFormat.CSV.defaultEndung()).isEqualTo("csv");
    }

    @Test
    void json_zielTypDatei() {
        assertThat(SpielerDbDateiFormat.JSON.zielTyp()).isEqualTo(ZielTyp.DATEI);
        assertThat(SpielerDbDateiFormat.JSON.defaultEndung()).isEqualTo("json");
    }

    @Test
    void calc_zielTypDatei() {
        assertThat(SpielerDbDateiFormat.CALC.zielTyp()).isEqualTo(ZielTyp.DATEI);
        assertThat(SpielerDbDateiFormat.CALC.defaultEndung()).isEqualTo("ods");
    }

    @Test
    void sqliteBackup_zielTypDatei() {
        assertThat(SpielerDbDateiFormat.SQLITE_BACKUP.zielTyp()).isEqualTo(ZielTyp.DATEI);
        assertThat(SpielerDbDateiFormat.SQLITE_BACKUP.defaultEndung()).isEqualTo("sqlite3");
    }

    @Test
    void anzeigeName_liefertNichtLeerenText() {
        for (SpielerDbDateiFormat format : SpielerDbDateiFormat.values()) {
            assertThat(format.anzeigeName()).isNotBlank();
        }
    }
}
