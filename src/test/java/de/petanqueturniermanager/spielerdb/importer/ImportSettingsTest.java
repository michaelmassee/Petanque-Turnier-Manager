package de.petanqueturniermanager.spielerdb.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;

class ImportSettingsTest {

    private Preferences testKnoten;
    private ImportSettings settings;

    @BeforeEach
    void setUp() {
        testKnoten = Preferences.userRoot().node("de/petanqueturniermanager/test/importsettings");
        settings = new ImportSettings(testKnoten);
    }

    @AfterEach
    void tearDown() throws Exception {
        testKnoten.removeNode();
    }

    @Test
    void letzterPfad_ohneVorherigenAufruf_liefertNull() {
        assertThat(settings.letzterPfad(SpielerDbDateiFormat.CSV)).isNull();
    }

    @Test
    void merkeLetztenPfad_liefertGespeichertenWert() {
        settings.merkeLetztenPfad(SpielerDbDateiFormat.CSV, "/tmp/spieler.csv");
        assertThat(settings.letzterPfad(SpielerDbDateiFormat.CSV)).isEqualTo("/tmp/spieler.csv");
    }

    @Test
    void formate_sindUnabhaengigVoneinander() {
        settings.merkeLetztenPfad(SpielerDbDateiFormat.CSV, "/tmp/spieler.csv");
        settings.merkeLetztenPfad(SpielerDbDateiFormat.JSON, "/tmp/spieler.json");

        assertThat(settings.letzterPfad(SpielerDbDateiFormat.CSV)).isEqualTo("/tmp/spieler.csv");
        assertThat(settings.letzterPfad(SpielerDbDateiFormat.JSON)).isEqualTo("/tmp/spieler.json");
    }

    @Test
    void defaultKonstruktor_verwendetUserRootKnoten() {
        assertThat(new ImportSettings().letzterPfad(SpielerDbDateiFormat.CALC)).isNull();
    }
}
