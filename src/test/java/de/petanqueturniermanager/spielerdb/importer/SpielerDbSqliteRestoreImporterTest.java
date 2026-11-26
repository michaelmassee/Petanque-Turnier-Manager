package de.petanqueturniermanager.spielerdb.importer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbException;

class SpielerDbSqliteRestoreImporterTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void resetInstance() throws Exception {
        I18n.initFuerTest(Locale.GERMAN);
        Field feld = SpielerDbConnection.class.getDeclaredField("INSTANCE");
        feld.setAccessible(true);
        feld.set(null, null);
    }

    @Test
    void restore_delegiertAnSpielerDbConnection() {
        // Ohne initialisierte SpielerDbConnection.INSTANCE liefert restoreVon()
        // eine SpielerDbException -> belegt, dass restore() tatsaechlich delegiert.
        SpielerDbSqliteRestoreImporter importer = new SpielerDbSqliteRestoreImporter();
        assertThatThrownBy(() -> importer.restore(tempDir.resolve("quelle.sqlite3")))
                .isInstanceOf(SpielerDbException.class)
                .hasMessage(I18n.get("spielerdb.db.fehler.nicht_initialisiert"));
    }
}
