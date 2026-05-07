package de.petanqueturniermanager.spielerdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * CRUD für {@link SpielerDatensatz} mit JOIN auf VEREIN für Anzeige
 * ({@link SpielerMitVerein}). Suche als indexfreundlicher Prefix-Match.
 */
public final class SpielerRepository {

    /**
     * Trennzeichen für die in {@code group_concat} gepackten Label-IDs/-Namen.
     * Bewusst exotisches Zeichen, kann in Labelnamen praktisch nicht vorkommen.
     */
    private static final String LABEL_SEP = "";

    private static final String SELECT_JOIN =
            "SELECT s.NR, s.VORNAME, s.NACHNAME, s.VEREIN_NR, s.LIZENZNR, "
                    + "v.NAME AS VEREIN_NAME, "
                    + "(SELECT group_concat(l.NR, '" + LABEL_SEP + "') "
                    + "   FROM SPIELER_LABEL sl JOIN LABEL l ON l.NR = sl.LABEL_NR "
                    + "   WHERE sl.SPIELER_NR = s.NR ORDER BY JAVA_LOWER(l.NAME)) AS LABEL_NRS, "
                    + "(SELECT group_concat(l.NAME, '" + LABEL_SEP + "') "
                    + "   FROM SPIELER_LABEL sl JOIN LABEL l ON l.NR = sl.LABEL_NR "
                    + "   WHERE sl.SPIELER_NR = s.NR ORDER BY JAVA_LOWER(l.NAME)) AS LABEL_NAMES "
                    + "FROM SPIELER s "
                    + "LEFT JOIN VEREIN v ON s.VEREIN_NR = v.NR ";

    private final Connection connection;

    public SpielerRepository(SpielerDbConnection conn) {
        this.connection = conn.getConnection();
    }

    /** Gesamtzahl Spielerdatensätze in der DB. Für Calc-AddIn-Statistik. */
    public int anzahl() throws SpielerDbException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM SPIELER");
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new SpielerDbException("Anzahl-Abfrage fehlgeschlagen", e);
        }
    }

    /**
     * Wildcard-Suche auf Nachname und Vorname (case-insensitiv).
     * <ul>
     *   <li>{@code wildcard=false} → indexfreundlicher Prefix-Match {@code term%}.</li>
     *   <li>{@code wildcard=true}  → Substring-Match {@code %term%}.</li>
     * </ul>
     * Normalisierung des Suchbegriffs (trim, lowercase) passiert genau einmal hier;
     * die SQL-Spalte wird passend mit {@code LOWER(...)} normalisiert.
     * Parameter werden ausschließlich per PreparedStatement gebunden — keine
     * String-Konkatenation, kein SQL-Injection-Risiko.
     */
    public List<SpielerMitVerein> findeMitWildcard(String suche, boolean wildcard, int limit)
            throws SpielerDbException {
        String normalized = suche == null ? "" : suche.trim().toLowerCase(Locale.ROOT);
        String pattern = wildcard ? "%" + normalized + "%" : normalized + "%";
        String sql = SELECT_JOIN
                + "WHERE JAVA_LOWER(s.NACHNAME) LIKE ? OR JAVA_LOWER(s.VORNAME) LIKE ? "
                + "ORDER BY JAVA_LOWER(s.NACHNAME), JAVA_LOWER(s.VORNAME) "
                + "LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Wildcard-Suche fehlgeschlagen: " + suche, e);
        }
    }

    public List<SpielerMitVerein> findAll() throws SpielerDbException {
        String sql = SELECT_JOIN + "ORDER BY JAVA_LOWER(s.NACHNAME), JAVA_LOWER(s.VORNAME)";
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return mapList(rs);
        } catch (SQLException e) {
            throw new SpielerDbException("Spieler laden fehlgeschlagen", e);
        }
    }

    /**
     * Prefix-Suche auf Nachname und Vorname (case-insensitiv). Leerer
     * Suchstring liefert die ersten {@code limit} Treffer alphabetisch.
     */
    public List<SpielerMitVerein> findByNamePart(String praefix, int limit) throws SpielerDbException {
        String norm = praefix.strip();
        String sql = SELECT_JOIN
                + "WHERE JAVA_LOWER(s.NACHNAME) LIKE JAVA_LOWER(?) OR JAVA_LOWER(s.VORNAME) LIKE JAVA_LOWER(?) "
                + "ORDER BY JAVA_LOWER(s.NACHNAME), JAVA_LOWER(s.VORNAME) "
                + "LIMIT ?";
        String pattern = norm + "%";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Spieler-Suche fehlgeschlagen: " + praefix, e);
        }
    }

    public Optional<SpielerMitVerein> findById(int nr) throws SpielerDbException {
        String sql = SELECT_JOIN + "WHERE s.NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Spieler-Lookup fehlgeschlagen: nr=" + nr, e);
        }
    }

    /**
     * Sucht einen vorhandenen Spieler mit identischem Vor-, Nachnamen und
     * Verein (case-insensitiv). Ein {@code null}-Verein matcht nur einen
     * Datensatz, der ebenfalls keinen Verein hat. {@code ausserNr} kann beim
     * Bearbeiten gesetzt werden, um den eigenen Datensatz auszuschließen.
     */
    public Optional<SpielerMitVerein> findeDuplikat(String vorname, String nachname,
            @Nullable Integer vereinNr, @Nullable Integer ausserNr) throws SpielerDbException {
        String vor = vorname.strip();
        String nach = nachname.strip();
        if (vor.isEmpty() || nach.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder sql = new StringBuilder(SELECT_JOIN);
        sql.append("WHERE JAVA_LOWER(s.VORNAME) = JAVA_LOWER(?) ")
                .append("AND JAVA_LOWER(s.NACHNAME) = JAVA_LOWER(?) ");
        if (vereinNr == null) {
            sql.append("AND s.VEREIN_NR IS NULL ");
        } else {
            sql.append("AND s.VEREIN_NR = ? ");
        }
        if (ausserNr != null) {
            sql.append("AND s.NR <> ? ");
        }
        sql.append("LIMIT 1");
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, vor);
            ps.setString(idx++, nach);
            if (vereinNr != null) {
                ps.setInt(idx++, vereinNr);
            }
            if (ausserNr != null) {
                ps.setInt(idx++, ausserNr);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Duplikat-Lookup fehlgeschlagen: "
                    + vor + " " + nach, e);
        }
    }

    public Optional<SpielerMitVerein> findByLizenz(String lizenznr) throws SpielerDbException {
        String norm = lizenznr.strip();
        if (norm.isEmpty()) {
            return Optional.empty();
        }
        String sql = SELECT_JOIN + "WHERE s.LIZENZNR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, norm);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Spieler-Lookup per Lizenz fehlgeschlagen: " + lizenznr, e);
        }
    }

    public SpielerDatensatz insert(SpielerDatensatz neu) throws SpielerDbException {
        String vorname = neu.vorname().strip();
        String nachname = neu.nachname().strip();
        if (vorname.isEmpty() || nachname.isEmpty()) {
            throw new SpielerDbException("Vorname und Nachname sind Pflichtfelder");
        }
        String lizenz = normLizenz(neu.lizenznr());
        String sql = "INSERT INTO SPIELER (VORNAME, NACHNAME, VEREIN_NR, LIZENZNR) "
                + "VALUES (?, ?, ?, ?)";
        boolean vorigerAutoCommit = true;
        try {
            vorigerAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            int neueNr;
            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, vorname);
                ps.setString(2, nachname);
                setNullableInt(ps, 3, neu.vereinNr());
                setNullableString(ps, 4, lizenz);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SpielerDbException("Insert lieferte keinen Schlüssel");
                    }
                    neueNr = keys.getInt(1);
                }
            }
            schreibeLabelZuweisungen(neueNr, neu.labelNrs());
            connection.commit();
            return new SpielerDatensatz(neueNr, vorname, nachname,
                    neu.vereinNr(), neu.labelNrs(), lizenz);
        } catch (SQLException e) {
            rollbackLeise();
            if (istNameDuplikat(e)) {
                throw new NameDuplikatException("Spieler mit gleichem Vor-/Nachnamen "
                        + "und Verein existiert bereits: " + vorname + " " + nachname, e);
            }
            if (istUniqueVerletzung(e)) {
                throw new LizenzDuplikatException("Lizenznummer existiert bereits: " + lizenz, e);
            }
            if (istFkVerletzung(e)) {
                throw new SpielerDbException(
                        "Verein-/Label-Referenz ungültig: vereinNr=" + neu.vereinNr()
                                + ", labelNrs=" + neu.labelNrs(), e);
            }
            throw new SpielerDbException("Spieler-Insert fehlgeschlagen", e);
        } finally {
            wiederherstellenAutoCommit(vorigerAutoCommit);
        }
    }

    public void update(SpielerDatensatz datensatz) throws SpielerDbException {
        Integer nr = datensatz.nr();
        if (nr == null) {
            throw new SpielerDbException("Update braucht persistierten Datensatz (nr != null)");
        }
        String vorname = datensatz.vorname().strip();
        String nachname = datensatz.nachname().strip();
        if (vorname.isEmpty() || nachname.isEmpty()) {
            throw new SpielerDbException("Vorname und Nachname sind Pflichtfelder");
        }
        String lizenz = normLizenz(datensatz.lizenznr());
        String sql = "UPDATE SPIELER SET VORNAME = ?, NACHNAME = ?, VEREIN_NR = ?, "
                + "LIZENZNR = ? WHERE NR = ?";
        boolean vorigerAutoCommit = true;
        try {
            vorigerAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, vorname);
                ps.setString(2, nachname);
                setNullableInt(ps, 3, datensatz.vereinNr());
                setNullableString(ps, 4, lizenz);
                ps.setInt(5, nr);
                int n = ps.executeUpdate();
                if (n == 0) {
                    throw new SpielerDbException("Spieler nicht gefunden: nr=" + nr);
                }
            }
            ersetzeLabelZuweisungen(nr, datensatz.labelNrs());
            connection.commit();
        } catch (SQLException e) {
            rollbackLeise();
            if (istNameDuplikat(e)) {
                throw new NameDuplikatException("Spieler mit gleichem Vor-/Nachnamen "
                        + "und Verein existiert bereits: " + vorname + " " + nachname, e);
            }
            if (istUniqueVerletzung(e)) {
                throw new LizenzDuplikatException("Lizenznummer existiert bereits: " + lizenz, e);
            }
            if (istFkVerletzung(e)) {
                throw new SpielerDbException(
                        "Verein-/Label-Referenz ungültig: vereinNr=" + datensatz.vereinNr()
                                + ", labelNrs=" + datensatz.labelNrs(), e);
            }
            throw new SpielerDbException("Spieler-Update fehlgeschlagen: nr=" + nr, e);
        } finally {
            wiederherstellenAutoCommit(vorigerAutoCommit);
        }
    }

    public void delete(int nr) throws SpielerDbException {
        String sql = "DELETE FROM SPIELER WHERE NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nr);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SpielerDbException("Spieler nicht gefunden: nr=" + nr);
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Spieler-Delete fehlgeschlagen: nr=" + nr, e);
        }
    }

    private static List<SpielerMitVerein> mapList(ResultSet rs) throws SQLException {
        List<SpielerMitVerein> ergebnis = new ArrayList<>();
        while (rs.next()) {
            ergebnis.add(map(rs));
        }
        return ergebnis;
    }

    private static SpielerMitVerein map(ResultSet rs) throws SQLException {
        int nr = rs.getInt("NR");
        String vorname = rs.getString("VORNAME");
        String nachname = rs.getString("NACHNAME");
        int vereinNrRaw = rs.getInt("VEREIN_NR");
        Integer vereinNr = rs.wasNull() ? null : Integer.valueOf(vereinNrRaw);
        String vereinName = rs.getString("VEREIN_NAME");
        List<Integer> labelNrs = parseIntList(rs.getString("LABEL_NRS"));
        List<String> labelNamen = parseStringList(rs.getString("LABEL_NAMES"));
        String lizenznr = rs.getString("LIZENZNR");
        return new SpielerMitVerein(nr, vorname, nachname,
                vereinNr, vereinName, labelNrs, labelNamen, lizenznr);
    }

    private static List<Integer> parseIntList(@Nullable String konkateniert) {
        if (konkateniert == null || konkateniert.isEmpty()) {
            return List.of();
        }
        String[] teile = konkateniert.split(LABEL_SEP, -1);
        List<Integer> ergebnis = new ArrayList<>(teile.length);
        for (String t : teile) {
            ergebnis.add(Integer.valueOf(t));
        }
        return List.copyOf(ergebnis);
    }

    private static List<String> parseStringList(@Nullable String konkateniert) {
        if (konkateniert == null || konkateniert.isEmpty()) {
            return List.of();
        }
        return List.of(konkateniert.split(LABEL_SEP, -1));
    }

    /** Schreibt die Junction-Einträge — Aufruf innerhalb einer Transaktion. */
    private void schreibeLabelZuweisungen(int spielerNr, List<Integer> labelNrs) throws SQLException {
        if (labelNrs.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO SPIELER_LABEL (SPIELER_NR, LABEL_NR) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Integer labelNr : labelNrs) {
                ps.setInt(1, spielerNr);
                ps.setInt(2, labelNr);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Ersetzt alle bisherigen Junction-Einträge — Aufruf innerhalb einer Transaktion. */
    private void ersetzeLabelZuweisungen(int spielerNr, List<Integer> labelNrs) throws SQLException {
        try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM SPIELER_LABEL WHERE SPIELER_NR = ?")) {
            del.setInt(1, spielerNr);
            del.executeUpdate();
        }
        schreibeLabelZuweisungen(spielerNr, labelNrs);
    }

    private void rollbackLeise() {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            // Sekundärfehler beim Rollback — primäre Exception nicht überdecken.
        }
    }

    private void wiederherstellenAutoCommit(boolean vorigerWert) {
        try {
            connection.setAutoCommit(vorigerWert);
        } catch (SQLException ex) {
            // Verbindung könnte bereits geschlossen sein — keine kritische Folge,
            // nächste Operation legt den Wert ohnehin neu fest.
        }
    }

    @Nullable
    private static String normLizenz(@Nullable String roh) {
        if (roh == null) {
            return null;
        }
        String s = roh.strip();
        return s.isEmpty() ? null : s;
    }

    private static void setNullableInt(PreparedStatement ps, int idx, @Nullable Integer wert) throws SQLException {
        if (wert == null) {
            ps.setNull(idx, Types.INTEGER);
        } else {
            ps.setInt(idx, wert);
        }
    }

    private static void setNullableString(PreparedStatement ps, int idx, @Nullable String wert) throws SQLException {
        if (wert == null) {
            ps.setNull(idx, Types.VARCHAR);
        } else {
            ps.setString(idx, wert);
        }
    }

    /**
     * UNIQUE-Verletzung des kombinierten Indexes
     * {@code UQ_SPIELER_NAME_VEREIN} — also gleicher Vor-/Nachname samt Verein.
     * Wird per Index-Name in der SQLite-Fehlermeldung erkannt.
     */
    private static boolean istNameDuplikat(SQLException e) {
        if (!istUniqueVerletzung(e)) {
            return false;
        }
        String raw = e.getMessage();
        if (raw == null) {
            return false;
        }
        return raw.toLowerCase(Locale.ROOT).contains("uq_spieler_name_verein");
    }

    private static boolean istUniqueVerletzung(SQLException e) {
        String raw = e.getMessage();
        if (raw == null) {
            return false;
        }
        String msg = raw.toLowerCase(Locale.ROOT);
        // SQLite-Format: "[SQLITE_CONSTRAINT_UNIQUE] ... UNIQUE constraint failed: ..."
        if (msg.contains("unique constraint failed") || msg.contains("sqlite_constraint_unique")) {
            return true;
        }
        // HSQLDB-Style-Fallback (für alte/andere Backends)
        String state = e.getSQLState();
        return state != null && state.startsWith("23") && msg.contains("unique");
    }

    private static boolean istFkVerletzung(SQLException e) {
        String raw = e.getMessage();
        if (raw == null) {
            return false;
        }
        String msg = raw.toLowerCase(Locale.ROOT);
        if (msg.contains("foreign key constraint failed") || msg.contains("sqlite_constraint_foreignkey")) {
            return true;
        }
        String state = e.getSQLState();
        return state != null && state.startsWith("23")
                && (msg.contains("foreign") || msg.contains("integrity"));
    }

    /** Vor-/Nachname + Verein (case-insensitiv, getrimmt) bereits vergeben. */
    public static final class NameDuplikatException extends SpielerDbException {
        private static final long serialVersionUID = 1L;
        public NameDuplikatException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Lizenznummer (case-sensitiv) ist bereits vergeben. */
    public static final class LizenzDuplikatException extends SpielerDbException {
        private static final long serialVersionUID = 1L;
        public LizenzDuplikatException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
