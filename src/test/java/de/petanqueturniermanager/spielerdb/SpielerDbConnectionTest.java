package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.petanqueturniermanager.helper.i18n.I18n;

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

    private final List<SpielerDbConnection> weitere = new ArrayList<>();

    @BeforeEach
    void deutscheMeldungen() {
        I18n.initFuerTest(Locale.GERMAN);
    }

    @AfterEach
    void resetInstance() throws Exception {
        for (SpielerDbConnection verbindung : weitere) {
            verbindung.close();
        }
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
                .hasMessage(I18n.get("spielerdb.db.fehler.nicht_initialisiert"));
    }

    @Test
    void restoreVon_memoryBasierteInstance_wirft() throws Exception {
        SpielerDbConnection memoryConn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite::memory:");
        setInstance(memoryConn);

        assertThatThrownBy(() -> SpielerDbConnection.restoreVon(tempDir.resolve("quelle.sqlite3")))
                .isInstanceOf(SpielerDbException.class)
                .hasMessage(I18n.get("spielerdb.db.fehler.restore_nur_datei"));
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

    /**
     * Zweite Verbindung auf dieselbe Datei (z.&nbsp;B. ein zweiter LibreOffice-Prozess): Ihre bestätigten Änderungen
     * stehen womöglich nur im WAL. Sicherung und Wiederherstellung dürfen sie weder verlieren noch die Verbindung von
     * der wiederhergestellten DB abhängen.
     */
    @Test
    void restoreVon_zweiteVerbindung_verliertKeineBestaetigtenAenderungen() throws Exception {
        Path quelle = tempDir.resolve("quelle.sqlite3");
        try (SpielerDbConnection q = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + quelle)) {
            speichereSpieler(q.getConnection(), "Quelle");
        }
        Path zielDatei = tempDir.resolve("aktuell.sqlite3");
        SpielerDbConnection eigene = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + zielDatei);
        setInstance(eigene);
        SpielerDbConnection zweite = zweiteVerbindung(zielDatei);
        speichereSpieler(eigene.getConnection(), "Erster");
        speichereSpieler(zweite.getConnection(), "Zweiter");

        SpielerDbConnection.restoreVon(quelle);

        Path sicherung;
        try (Stream<Path> dateien = Files.list(tempDir)) {
            sicherung = dateien.filter(d -> d.getFileName().toString().startsWith("aktuell.sqlite3.bak."))
                    .findFirst().orElseThrow();
        }
        try (SpielerDbConnection gesichert = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + sicherung)) {
            assertThat(vornamen(gesichert.getConnection())).containsExactlyInAnyOrder("Erster", "Zweiter");
        }
        assertThat(vornamen(eigene.getConnection())).containsExactly("Quelle");
        assertThat(vornamen(zweite.getConnection())).as("zweite Verbindung sieht den wiederhergestellten Stand")
                .containsExactly("Quelle");

        speichereSpieler(zweite.getConnection(), "Danach");
        assertThat(vornamen(eigene.getConnection())).containsExactlyInAnyOrder("Quelle", "Danach");
    }

    @Test
    void restoreVon_eigeneDbDatei_wirftUndLaesstDbUnveraendert() throws Exception {
        Path zielDatei = tempDir.resolve("aktuell.sqlite3");
        SpielerDbConnection eigene = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + zielDatei);
        setInstance(eigene);
        speichereSpieler(eigene.getConnection(), "Erster");

        assertThatThrownBy(() -> SpielerDbConnection.restoreVon(zielDatei))
                .isInstanceOf(SpielerDbException.class)
                .hasMessage(I18n.get("spielerdb.db.fehler.restore_eigene_datei", zielDatei));
        assertThat(vornamen(eigene.getConnection())).containsExactly("Erster");
    }

    @Test
    void istDbDateiOderBegleitdatei_erkenntDbUndWalDateien() throws Exception {
        Path zielDatei = tempDir.resolve("aktuell.sqlite3");
        try (SpielerDbConnection conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + zielDatei)) {
            assertThat(conn.istDbDateiOderBegleitdatei(zielDatei)).isTrue();
            assertThat(conn.istDbDateiOderBegleitdatei(tempDir.resolve("unter/../aktuell.sqlite3"))).isTrue();
            assertThat(conn.istDbDateiOderBegleitdatei(tempDir.resolve("aktuell.sqlite3-wal"))).isTrue();
            assertThat(conn.istDbDateiOderBegleitdatei(tempDir.resolve("backup.sqlite3"))).isFalse();
        }
    }

    @Test
    void pruefeExportZiel_lehntDbUndBegleitdateienAbUndLaesstDbUnveraendert() throws Exception {
        Path dbDatei = tempDir.resolve("aktuell.sqlite3");
        try (SpielerDbConnection conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + dbDatei)) {
            speichereSpieler(conn.getConnection(), "Erster");

            for (Path ziel : List.of(dbDatei, tempDir.resolve("unter/../aktuell.sqlite3"),
                    tempDir.resolve("aktuell.sqlite3-wal"), tempDir.resolve("aktuell.sqlite3-shm"))) {
                assertThatThrownBy(() -> conn.pruefeExportZiel(ziel))
                        .isInstanceOf(SpielerDbException.class)
                        .hasMessage(I18n.get("spielerdb.export.fehler.ziel_ist_db", ziel.toAbsolutePath()));
            }
            assertThat(vornamen(conn.getConnection())).containsExactly("Erster");
        }
    }

    @Test
    void pruefeExportZiel_erlaubtAndereDatei() throws Exception {
        Path dbDatei = tempDir.resolve("aktuell.sqlite3");
        try (SpielerDbConnection conn = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + dbDatei)) {
            assertThatCode(() -> conn.pruefeExportZiel(tempDir.resolve("spieler.csv"))).doesNotThrowAnyException();
        }
    }

    private SpielerDbConnection zweiteVerbindung(Path datei) throws SpielerDbException {
        SpielerDbConnection verbindung = SpielerDbConnection.fuerJdbcUrl("jdbc:sqlite:" + datei);
        weitere.add(verbindung);
        return verbindung;
    }

    private static void speichereSpieler(Connection conn, String vorname) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO SPIELER (VORNAME, NACHNAME) VALUES ('" + vorname + "', 'Test')");
        }
    }

    private static List<String> vornamen(Connection conn) throws SQLException {
        List<String> namen = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT VORNAME FROM SPIELER")) {
            while (rs.next()) {
                namen.add(rs.getString(1));
            }
        }
        return namen;
    }

    private static void setInstance(SpielerDbConnection wert) throws Exception {
        Field feld = SpielerDbConnection.class.getDeclaredField("INSTANCE");
        feld.setAccessible(true);
        feld.set(null, wert);
    }
}
