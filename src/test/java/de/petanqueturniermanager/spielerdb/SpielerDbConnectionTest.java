package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Deckt die JDBC-/Dateisystem-Logik von {@link SpielerDbConnection} ab.
 * {@code getInstance()} (Plugin-Default im User-Homeverzeichnis) wird
 * bewusst NICHT getestet, um das echte Home-Verzeichnis nicht anzufassen —
 * {@link #restoreVon} wird stattdessen über Reflection auf das private
 * {@code INSTANCE}-Feld mit einer file-basierten Test-Connection erprobt.
 */
class SpielerDbConnectionTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void resetInstance() throws Exception {
        setInstance(null);
    }

    @Test
    void fuerJdbcUrl_liefertOffeneVerbindung() throws Exception {
        try (SpielerDbConnection conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:")) {
            assertThat(conn.istOffen()).isTrue();
            assertThat(conn.getConnection()).isNotNull();
            assertThat(conn.getJdbcUrl()).isEqualTo("jdbc:sqlite::memory:");
        }
    }

    @Test
    void istOffen_liefertFalseNachClose() throws Exception {
        SpielerDbConnection conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:");
        conn.close();
        assertThat(conn.istOffen()).isFalse();
    }

    @Test
    void close_istIdempotent() throws Exception {
        SpielerDbConnection conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:");
        conn.close();
        // zweiter close()-Aufruf auf bereits geschlossener Connection darf nicht werfen
        conn.close();
        assertThat(conn.istOffen()).isFalse();
    }

    @Test
    void dbDatei_memoryUrl_liefertNull() throws Exception {
        try (SpielerDbConnection conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:")) {
            assertThat(conn.dbDatei()).isNull();
        }
    }

    @Test
    void dbDatei_fileUrl_liefertPfad() throws Exception {
        Path dbDatei = tempDir.resolve("test.sqlite3");
        try (SpielerDbConnection conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + dbDatei)) {
            assertThat(conn.dbDatei()).isEqualTo(dbDatei);
        }
    }

    @Test
    void restoreVon_ohneInitialisierteInstance_wirft() {
        assertThatThrownBy(() -> SpielerDbConnection.restoreVon(tempDir.resolve("quelle.sqlite3")))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("nicht initialisiert");
    }

    @Test
    void restoreVon_memoryBasierteInstance_wirft() throws Exception {
        SpielerDbConnection memoryConn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:");
        setInstance(memoryConn);

        assertThatThrownBy(() -> SpielerDbConnection.restoreVon(tempDir.resolve("quelle.sqlite3")))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("file-basierte");
    }

    @Test
    void restoreVon_quelleZuKlein_wirft() throws Exception {
        Path zielDatei = tempDir.resolve("aktuell.sqlite3");
        setInstance(SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + zielDatei));

        Path quelle = tempDir.resolve("quelle.sqlite3");
        Files.write(quelle, new byte[] { 1, 2, 3 });

        assertThatThrownBy(() -> SpielerDbConnection.restoreVon(quelle))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("zu klein");
    }

    @Test
    void restoreVon_quelleOhneSqliteMagic_wirft() throws Exception {
        Path zielDatei = tempDir.resolve("aktuell.sqlite3");
        setInstance(SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + zielDatei));

        Path quelle = tempDir.resolve("quelle.sqlite3");
        Files.write(quelle, "Das ist definitiv keine SQLite-Datei !!".getBytes());

        assertThatThrownBy(() -> SpielerDbConnection.restoreVon(quelle))
                .isInstanceOf(SpielerDbException.class)
                .hasMessageContaining("kein SQLite-Backup");
    }

    @Test
    void restoreVon_gueltigeQuelle_ersetztDatei() throws Exception {
        // "Quelle": eine echte, gültige SQLite-Datei (per file-basierter Connection erzeugt und geschlossen)
        Path quelle = tempDir.resolve("quelle.sqlite3");
        SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + quelle).close();
        assertThat(Files.size(quelle)).isPositive();

        Path zielDatei = tempDir.resolve("aktuell.sqlite3");
        setInstance(SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + zielDatei));
        assertThat(Files.exists(zielDatei)).isTrue();

        SpielerDbConnection.restoreVon(quelle);

        // Backup der alten Datei wurde angelegt, Zieldatei enthaelt jetzt die Quelle
        try (Stream<Path> dateien = Files.list(tempDir)) {
            assertThat(dateien.map(Path::getFileName).map(Path::toString))
                    .anyMatch(name -> name.startsWith("aktuell.sqlite3.bak."));
        }
        assertThat(Files.exists(zielDatei)).isTrue();
    }

    private static void setInstance(SpielerDbConnection wert) throws Exception {
        Field feld = SpielerDbConnection.class.getDeclaredField("INSTANCE");
        feld.setAccessible(true);
        feld.set(null, wert);
    }
}
