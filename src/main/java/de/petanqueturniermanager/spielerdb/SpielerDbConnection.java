package de.petanqueturniermanager.spielerdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Lifecycle für die lokale Spieler-Stammdaten-DB. Implementierung: SQLite via
 * xerial sqlite-jdbc.
 *
 * <p>Pfad: {@code ~/.petanqueturniermanager/spielerdb/spieler.sqlite3}.
 * SQLite-Filelocking erlaubt mehrere Prozesse parallel auf derselben Datei
 * — mit aktiviertem WAL-Mode sogar concurrent readers während eines Writers.
 * Damit ist der frühere HSQLDB-Server-Mode-Workaround obsolet.
 *
 * <p>Schema-Erstellung läuft idempotent über {@code CREATE TABLE IF NOT EXISTS}.
 * Im Plugin-Betrieb wird eine Singleton-Connection gehalten; Tests greifen
 * über {@link #fuerJdbcUrl(String)} mit {@code jdbc:sqlite::memory:} auf
 * isolierte In-Memory-DBs zu.
 */
public final class SpielerDbConnection implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(SpielerDbConnection.class);

    private static final String DEFAULT_DIR = ".petanqueturniermanager/spielerdb";
    private static final String DB_FILENAME = "spieler.sqlite3";
    private static final String DRIVER_CLASS = "org.sqlite.JDBC";

    /**
     * Erkennungs-Substrings in JDBC-Fehlermeldungen, die auf einen
     * Multi-Prozess-Lock hindeuten. Bleiben als Schutzschicht dabei, falls SQLite
     * mal {@code SQLITE_BUSY} über {@code busy_timeout} hinaus durchreichen sollte.
     */
    private static final String[] LOCK_HINTS = {
            "database is locked", "locked", "busy"
    };

    @Nullable private static volatile SpielerDbConnection INSTANCE;

    private final String jdbcUrl;
    private final Connection connection;

    private SpielerDbConnection(String jdbcUrl, Connection connection) {
        this.jdbcUrl = jdbcUrl;
        this.connection = connection;
    }

    /** Plugin-Default: DB im User-Homeverzeichnis. */
    public static synchronized SpielerDbConnection getInstance() throws SpielerDbException {
        SpielerDbConnection inst = INSTANCE;
        if (inst != null && inst.istOffen()) {
            return inst;
        }
        Path dbDir = Path.of(System.getProperty("user.home"), DEFAULT_DIR);
        try {
            Files.createDirectories(dbDir);
        } catch (IOException e) {
            throw new SpielerDbException("DB-Verzeichnis nicht anlegbar: " + dbDir, e);
        }
        String url = "jdbc:sqlite:" + dbDir.resolve(DB_FILENAME);
        SpielerDbConnection neu = baueConnection(url);
        Runtime.getRuntime().addShutdownHook(
                new Thread(neu::schliessenLeise, "spielerdb-shutdown"));
        INSTANCE = neu;
        return neu;
    }

    /** Test-Konstruktor: erlaubt In-Memory- oder temp-File-DBs. */
    public static SpielerDbConnection fuerJdbcUrl(String jdbcUrl) throws SpielerDbException {
        return baueConnection(jdbcUrl);
    }

    private static SpielerDbConnection baueConnection(String jdbcUrl) throws SpielerDbException {
        try {
            Class.forName(DRIVER_CLASS);
            Connection conn = DriverManager.getConnection(jdbcUrl);
            conn.setAutoCommit(true);
            initialisiereVerbindung(conn);
            schemaAnlegen(conn);
            logger.info("Spieler-DB verbunden: {}", jdbcUrl);
            return new SpielerDbConnection(jdbcUrl, conn);
        } catch (ClassNotFoundException e) {
            throw new SpielerDbException("SQLite-Treiber nicht im Classpath", e);
        } catch (SQLException e) {
            if (istLockFehler(e)) {
                throw new SpielerDbException(
                        "Spieler-Datenbank ist gerade gesperrt (Schreibvorgang läuft). "
                                + "Bitte kurz warten und erneut versuchen.", e, true);
            }
            throw new SpielerDbException("DB-Initialisierung fehlgeschlagen", e);
        }
    }

    /**
     * Pragmas für jede neu aufgebaute Connection. Reihenfolge wichtig:
     * <ul>
     *   <li>{@code foreign_keys = ON} muss <i>pro Connection</i> gesetzt werden,
     *       SQLite-Default ist OFF.</li>
     *   <li>{@code journal_mode = WAL} aktiviert Write-Ahead-Log → concurrent
     *       readers während eines Writers, ideal für mehrere LO-Prozesse.
     *       Persistent in der Datei, einmalige Aktivierung reicht eigentlich,
     *       schadet aber nicht beim erneuten Setzen.</li>
     *   <li>{@code synchronous = NORMAL} ist die für WAL empfohlene Einstellung
     *       (gut genug für Stammdaten, deutlich schneller als FULL).</li>
     *   <li>{@code busy_timeout = 5000} lässt einen blockierten Writer bis zu
     *       5 s auf den Lock warten, statt sofort {@code SQLITE_BUSY} zu werfen.</li>
     * </ul>
     * Für In-Memory-Tests (URL {@code jdbc:sqlite::memory:}) ist WAL ein No-Op,
     * SQLite ignoriert den Mode dort gracefully.
     */
    private static void initialisiereVerbindung(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
            st.execute("PRAGMA synchronous = NORMAL");
            st.execute("PRAGMA busy_timeout = 5000");
        }
        registriereJavaLower(conn);
    }

    /**
     * Registriert die SQL-Funktion {@code JAVA_LOWER(text)} pro Connection.
     *
     * <p>SQLites eingebautes {@code LOWER()} ist ASCII-only — {@code LOWER('MÜLLER')}
     * bleibt {@code 'MüLLER'}, sodass case-insensitive-Suchen über deutsche Umlaute
     * fehlschlagen. Mit dieser UDF läuft das Lowercase über
     * {@link String#toLowerCase(Locale)} mit {@link Locale#ROOT} und liefert echtes
     * Unicode-Lowercase, identisch zur Java-seitigen Normalisierung in den
     * Repository-Aufrufern.
     */
    private static void registriereJavaLower(Connection conn) throws SQLException {
        org.sqlite.Function.create(conn, "JAVA_LOWER", new org.sqlite.Function() {
            @Override
            protected void xFunc() throws SQLException {
                String arg = value_text(0);
                result(arg == null ? null : arg.toLowerCase(Locale.ROOT));
            }
        });
    }

    private static void schemaAnlegen(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS VEREIN (
                      NR INTEGER PRIMARY KEY AUTOINCREMENT,
                      NAME VARCHAR(150) NOT NULL,
                      CONSTRAINT UQ_VEREIN_NAME UNIQUE (NAME)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS SPIELER (
                      NR INTEGER PRIMARY KEY AUTOINCREMENT,
                      VORNAME VARCHAR(100) NOT NULL,
                      NACHNAME VARCHAR(100) NOT NULL,
                      VEREIN_NR INTEGER,
                      LIZENZNR VARCHAR(50),
                      CONSTRAINT FK_SPIELER_VEREIN FOREIGN KEY (VEREIN_NR)
                        REFERENCES VEREIN(NR) ON DELETE RESTRICT
                    )
                    """);
            st.execute("CREATE INDEX IF NOT EXISTS IDX_SPIELER_NACHNAME ON SPIELER(NACHNAME)");
            st.execute("CREATE INDEX IF NOT EXISTS IDX_SPIELER_VEREIN ON SPIELER(VEREIN_NR)");
            // SQLite-UNIQUE auf nullable Spalte: mehrere NULL erlaubt (Standard-SQL),
            // damit ist die Bedingung „Lizenznr eindeutig wenn gesetzt" erfüllt.
            st.execute("CREATE UNIQUE INDEX IF NOT EXISTS UQ_SPIELER_LIZENZ ON SPIELER(LIZENZNR)");
        }
    }

    private static boolean istLockFehler(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        for (String hint : LOCK_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public boolean istOffen() {
        try {
            return !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void close() throws SpielerDbException {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new SpielerDbException("DB-Close fehlgeschlagen", e);
        }
        if (INSTANCE == this) {
            INSTANCE = null;
        }
    }

    private void schliessenLeise() {
        try {
            close();
        } catch (SpielerDbException e) {
            logger.warn("Spieler-DB konnte beim Shutdown nicht sauber geschlossen werden", e);
        }
    }
}
