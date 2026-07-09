package de.petanqueturniermanager.spielerdb.importer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbException;

class SpielerDbSqliteRestoreImporterTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void resetInstance() throws Exception {
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
                .hasMessageContaining("nicht initialisiert");
    }
}
