package de.petanqueturniermanager.spielerdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteErrorCode;

import de.petanqueturniermanager.helper.i18n.I18n;

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
            throw new SpielerDbException(I18n.get("spielerdb.db.fehler.verzeichnis", dbDir), e);
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
            throw new SpielerDbException(I18n.get("spielerdb.db.fehler.treiber"), e);
        } catch (SQLException e) {
            throw sperrOderFehler(e, I18n.get("spielerdb.db.fehler.initialisierung"));
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
        // FLAG_DETERMINISTIC ist Pflicht, damit JAVA_LOWER in CREATE-INDEX-Ausdrücken
        // (UQ_SPIELER_NAME_VEREIN) genutzt werden darf — SQLite verweigert sonst.
        org.sqlite.Function.create(conn, "JAVA_LOWER", new org.sqlite.Function() {
            @Override
            protected void xFunc() throws SQLException {
                String arg = value_text(0);
                result(arg == null ? null : arg.toLowerCase(Locale.ROOT));
            }
        }, 1, org.sqlite.Function.FLAG_DETERMINISTIC);
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
                    CREATE TABLE IF NOT EXISTS LABEL (
                      NR INTEGER PRIMARY KEY AUTOINCREMENT,
                      NAME VARCHAR(150) NOT NULL,
                      CONSTRAINT UQ_LABEL_NAME UNIQUE (NAME)
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
            // Junction-Tabelle für n:m Spieler ↔ Label.
            // CASCADE auf Spieler-Seite: Spieler löschen entfernt seine Zuweisungen automatisch.
            // RESTRICT auf Label-Seite: Label kann nicht gelöscht werden, solange zugewiesen.
            st.execute("""
                    CREATE TABLE IF NOT EXISTS SPIELER_LABEL (
                      SPIELER_NR INTEGER NOT NULL,
                      LABEL_NR INTEGER NOT NULL,
                      PRIMARY KEY (SPIELER_NR, LABEL_NR),
                      CONSTRAINT FK_SL_SPIELER FOREIGN KEY (SPIELER_NR)
                        REFERENCES SPIELER(NR) ON DELETE CASCADE,
                      CONSTRAINT FK_SL_LABEL FOREIGN KEY (LABEL_NR)
                        REFERENCES LABEL(NR) ON DELETE RESTRICT
                    )
                    """);
            st.execute("CREATE INDEX IF NOT EXISTS IDX_SPIELER_NACHNAME ON SPIELER(NACHNAME)");
            st.execute("CREATE INDEX IF NOT EXISTS IDX_SPIELER_VEREIN ON SPIELER(VEREIN_NR)");
            st.execute("CREATE INDEX IF NOT EXISTS IDX_SL_LABEL ON SPIELER_LABEL(LABEL_NR)");
            // SQLite-UNIQUE auf nullable Spalte: mehrere NULL erlaubt (Standard-SQL),
            // damit ist die Bedingung „Lizenznr eindeutig wenn gesetzt" erfüllt.
            st.execute("CREATE UNIQUE INDEX IF NOT EXISTS UQ_SPIELER_LIZENZ ON SPIELER(LIZENZNR)");
            // Eindeutigkeit über (Vorname, Nachname, Verein) – getrimmt und Unicode-
            // case-insensitiv via JAVA_LOWER. COALESCE(VEREIN_NR, -1) zwingt Null-
            // Vereine in dieselbe Vergleichsgruppe (sonst würde SQLite NULL-Werte als
            // distinct behandeln und mehrere namensgleiche Spieler ohne Verein zulassen).
            // -1 ist als Sentinel sicher, weil VEREIN.NR über AUTOINCREMENT bei 1 startet.
            try {
                st.execute("CREATE UNIQUE INDEX IF NOT EXISTS UQ_SPIELER_NAME_VEREIN "
                        + "ON SPIELER(JAVA_LOWER(TRIM(VORNAME)), JAVA_LOWER(TRIM(NACHNAME)), "
                        + "COALESCE(VEREIN_NR, -1))");
            } catch (SQLException e) {
                // Legacy-DB enthält bereits Duplikate – Index kann nicht angelegt werden.
                // Lauter Hinweis im Log; die Anwendung läuft weiter, der Pre-Check im
                // Erfassen-Dialog verhindert das Anlegen neuer Duplikate.
                logger.warn("UQ_SPIELER_NAME_VEREIN konnte nicht angelegt werden "
                        + "(vermutlich vorhandene Duplikate): {}", e.getMessage());
            }
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

    /**
     * Liefert den absoluten Pfad zur DB-Datei, falls die JDBC-URL ein File-Backend
     * ist (z.B. {@code jdbc:sqlite:/.../spieler.sqlite3}). Für In-Memory- oder
     * andere Spezial-URLs wird {@code null} geliefert.
     */
    @Nullable
    public Path dbDatei() {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:sqlite:")) {
            return null;
        }
        String pfad = jdbcUrl.substring("jdbc:sqlite:".length());
        if (pfad.isEmpty() || pfad.startsWith(":")) {
            return null;
        }
        return Path.of(pfad);
    }

    /**
     * Ersetzt den Inhalt der laufenden Spieler-DB durch {@code quelle}. Der bisherige Stand wird vorher nach
     * {@code <db>.bak.<yyyyMMdd-HHmmss>} gesichert.
     *
     * <p>Sicherung und Wiederherstellung laufen über die SQLite-Backup-API der offenen Verbindung, nicht über
     * Dateioperationen: Weitere Verbindungen (z.&nbsp;B. ein zweiter LibreOffice-Prozess) bleiben gültig. Ein Tausch der
     * Datei samt Löschen von WAL und SHM verlöre deren bestätigte, noch nicht zurückgeschriebene Änderungen, und eine
     * noch offene Verbindung schriebe danach in die verschobene Datei. Die Sicherung enthält auch diese WAL-Inhalte;
     * die Wiederherstellung ist eine einzige Schreibtransaktion und lässt den Stand bei einem Fehler unverändert.
     *
     * <p>Pre-Conditions: aktuelle DB ist file-basiert, {@code quelle} ist eine andere SQLite-Datei (Magic-Bytes
     * geprüft).
     */
    public static synchronized void restoreVon(Path quelle) throws SpielerDbException {
        SpielerDbConnection inst = INSTANCE;
        if (inst == null || !inst.istOffen()) {
            throw new SpielerDbException(I18n.get("spielerdb.db.fehler.nicht_initialisiert"));
        }
        Path zielDatei = inst.dbDatei();
        if (zielDatei == null) {
            throw new SpielerDbException(I18n.get("spielerdb.db.fehler.restore_nur_datei"));
        }
        if (inst.istDbDateiOderBegleitdatei(quelle)) {
            throw new SpielerDbException(I18n.get("spielerdb.db.fehler.restore_eigene_datei", quelle));
        }
        pruefeSqliteMagic(quelle);

        String zeitstempel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path sicherung = zielDatei.resolveSibling(zielDatei.getFileName() + ".bak." + zeitstempel);
        SQLiteConnection sqlite = inst.sqliteVerbindung();
        try {
            pruefeErgebnis(sqlite.getDatabase().backup("main", sicherung.toAbsolutePath().toString(), null));
        } catch (SQLException e) {
            throw sperrOderFehler(e, I18n.get("spielerdb.db.fehler.sicherung", sicherung));
        }
        try {
            pruefeErgebnis(sqlite.getDatabase().restore("main", quelle.toAbsolutePath().toString(), null));
            // Ein älteres Backup kennt womöglich noch nicht alle Tabellen und Indizes.
            schemaAnlegen(inst.connection);
        } catch (SQLException e) {
            throw sperrOderFehler(e, I18n.get("spielerdb.db.fehler.restore", sicherung));
        }
        logger.info("Spieler-DB aus {} wiederhergestellt, bisheriger Stand in {}", quelle, sicherung);
    }

    /**
     * Ob {@code datei} die DB-Datei dieser Verbindung oder eine ihrer WAL-Begleitdateien ist. Ein Backup dorthin
     * ersetzte die laufende DB, eine offene Verbindung schriebe danach in die verwaiste Datei.
     */
    public boolean istDbDateiOderBegleitdatei(Path datei) {
        Path db = dbDatei();
        if (db == null) {
            return false;
        }
        for (Path kandidat : List.of(db, db.resolveSibling(db.getFileName() + "-wal"),
                db.resolveSibling(db.getFileName() + "-shm"))) {
            if (istGleicheDatei(kandidat, datei)) {
                return true;
            }
        }
        return false;
    }

    private static boolean istGleicheDatei(Path a, Path b) {
        if (Files.exists(a) && Files.exists(b)) {
            try {
                return Files.isSameFile(a, b);
            } catch (IOException e) {
                logger.debug("Dateivergleich {} / {} nicht möglich, vergleiche Pfade", a, b, e);
            }
        }
        return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
    }

    /** Native SQLite-Verbindung für die Backup-API. */
    public SQLiteConnection sqliteVerbindung() throws SpielerDbException {
        try {
            return connection.unwrap(SQLiteConnection.class);
        } catch (SQLException e) {
            throw new SpielerDbException(I18n.get("spielerdb.db.fehler.keine_sqlite_verbindung"), e);
        }
    }

    /** Die xerial-Backup-API meldet Fehler teils nur über den Rückgabecode. */
    private static void pruefeErgebnis(int rueckgabe) throws SQLException {
        if (rueckgabe != SQLiteErrorCode.SQLITE_OK.code) {
            throw new SQLException("SQLite-Rückgabecode " + rueckgabe + " (" + SQLiteErrorCode.getErrorCode(rueckgabe)
                    + ")", null, rueckgabe);
        }
    }

    private static SpielerDbException sperrOderFehler(SQLException e, String meldung) {
        if (istLockFehler(e)) {
            return new SpielerDbException(I18n.get("spielerdb.db.fehler.gesperrt"), e, true);
        }
        return new SpielerDbException(meldung, e);
    }

    /**
     * SQLite-Header beginnt mit dem ASCII-String {@code "SQLite format 3\0"}
     * (16 Bytes). Verhindert, dass beliebige Dateien als „Backup" akzeptiert
     * werden.
     */
    private static void pruefeSqliteMagic(Path datei) throws SpielerDbException {
        byte[] erwartet = "SQLite format 3\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        try {
            byte[] kopf = new byte[erwartet.length];
            try (var in = Files.newInputStream(datei)) {
                int gelesen = in.readNBytes(kopf, 0, kopf.length);
                if (gelesen != erwartet.length) {
                    throw new SpielerDbException(I18n.get("spielerdb.db.fehler.backup_zu_klein", datei));
                }
            }
            for (int i = 0; i < erwartet.length; i++) {
                if (kopf[i] != erwartet[i]) {
                    throw new SpielerDbException(I18n.get("spielerdb.db.fehler.kein_backup", datei));
                }
            }
        } catch (IOException e) {
            throw new SpielerDbException(I18n.get("spielerdb.db.fehler.backup_nicht_lesbar", datei), e);
        }
    }

    @Override
    public void close() throws SpielerDbException {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new SpielerDbException(I18n.get("spielerdb.db.fehler.schliessen"), e);
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
