package de.petanqueturniermanager.spielerdb.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;
import de.petanqueturniermanager.spielerdb.SpielerDbException;

class SpielerDbSqliteBackupExporterTest {

    private static final SpielerDbExportData KEINE_DATEN = new SpielerDbExportData(
            new ExportMeta(1, Instant.parse("2026-11-26T00:00:00Z"), null), List.of(), List.of(), List.of(), List.of());

    @TempDir
    Path tempDir;

    private Path dbDatei;
    private SpielerDbConnection db;

    @BeforeEach
    void dbOeffnen() throws Exception {
        I18n.initFuerTest(Locale.GERMAN);
        dbDatei = tempDir.resolve("spieler.sqlite3");
        db = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + dbDatei);
        speichereSpieler(db, "Erster");
    }

    @AfterEach
    void dbSchliessen() throws Exception {
        db.close();
    }

    /** Backup auf die laufende DB ersetzte sie; spätere Änderungen landeten in der verwaisten Datei. */
    @Test
    void backupAufDieEigeneDbDateiWirdAbgelehnt() throws Exception {
        for (Path ziel : List.of(dbDatei, tempDir.resolve("spieler.sqlite3-wal"))) {
            assertThatThrownBy(() -> exportiere(ziel)).as(ziel.toString())
                    .isInstanceOf(SpielerDbException.class)
                    .hasMessage(I18n.get("spielerdb.export.fehler.ziel_ist_db", ziel.toAbsolutePath()));
        }

        speichereSpieler(db, "Danach");
        db.close();
        db = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + dbDatei);
        assertThat(vornamen(db)).containsExactlyInAnyOrder("Erster", "Danach");
    }

    @Test
    void backupErsetztEineAeltereSicherungOhneTemporaereReste() throws Exception {
        Path ziel = tempDir.resolve("sicherung").resolve("backup.sqlite3");
        exportiere(ziel);
        speichereSpieler(db, "Zweiter");

        exportiere(ziel);

        try (SpielerDbConnection backup = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + ziel)) {
            assertThat(vornamen(backup)).containsExactlyInAnyOrder("Erster", "Zweiter");
        }
        try (Stream<Path> dateien = Files.list(ziel.getParent())) {
            assertThat(dateien.map(datei -> datei.getFileName().toString())).noneMatch(name -> name.endsWith(".tmp"));
        }
    }

    private void exportiere(Path ziel) throws SpielerDbException {
        new SpielerDbSqliteBackupExporter(db).export(KEINE_DATEN, new ExportRequest(SpielerDbDateiFormat.SQLITE_BACKUP,
                EnumSet.of(ExportEntity.SPIELER), new AllExportFilter(), ziel));
    }

    private static void speichereSpieler(SpielerDbConnection verbindung, String vorname) throws SQLException {
        try (Statement st = verbindung.getConnection().createStatement()) {
            st.executeUpdate("INSERT INTO SPIELER (VORNAME, NACHNAME) VALUES ('" + vorname + "', 'Test')");
        }
    }

    private static List<String> vornamen(SpielerDbConnection verbindung) throws SQLException {
        List<String> namen = new ArrayList<>();
        try (Statement st = verbindung.getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT VORNAME FROM SPIELER")) {
            while (rs.next()) {
                namen.add(rs.getString(1));
            }
        }
        return namen;
    }
}
