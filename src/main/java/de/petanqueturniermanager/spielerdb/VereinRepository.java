package de.petanqueturniermanager.spielerdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD für {@link VereinDatensatz}. Eingabe-Namen werden konsistent getrimmt;
 * Lookup ist case-insensitiv.
 */
public final class VereinRepository {

    private final Connection connection;

    public VereinRepository(SpielerDbConnection conn) {
        this.connection = conn.getConnection();
    }

    public List<VereinDatensatz> findAll() throws SpielerDbException {
        String sql = "SELECT NR, NAME FROM VEREIN ORDER BY JAVA_LOWER(NAME)";
        List<VereinDatensatz> ergebnis = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ergebnis.add(new VereinDatensatz(rs.getInt("NR"), rs.getString("NAME")));
            }
            return ergebnis;
        } catch (SQLException e) {
            throw new SpielerDbException("Vereine laden fehlgeschlagen", e);
        }
    }

    /** Case-insensitiver, getrimmter Lookup. */
    public Optional<VereinDatensatz> findByName(String name) throws SpielerDbException {
        String norm = name.strip();
        String sql = "SELECT NR, NAME FROM VEREIN WHERE JAVA_LOWER(NAME) = JAVA_LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, norm);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new VereinDatensatz(rs.getInt("NR"), rs.getString("NAME")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Verein-Lookup fehlgeschlagen: " + name, e);
        }
    }

    public Optional<VereinDatensatz> findById(int nr) throws SpielerDbException {
        String sql = "SELECT NR, NAME FROM VEREIN WHERE NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new VereinDatensatz(rs.getInt("NR"), rs.getString("NAME")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Verein-Lookup fehlgeschlagen: nr=" + nr, e);
        }
    }

    /**
     * Insert mit getrimmtem Namen. Wirft {@link DuplikatException}, wenn der
     * Name (case-insensitiv) bereits existiert.
     */
    public VereinDatensatz insert(String name) throws SpielerDbException {
        String norm = name.strip();
        if (norm.isEmpty()) {
            throw new SpielerDbException("Vereinsname darf nicht leer sein");
        }
        String sql = "INSERT INTO VEREIN (NAME) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, norm);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new VereinDatensatz(keys.getInt(1), norm);
                }
                throw new SpielerDbException("Insert lieferte keinen Schlüssel");
            }
        } catch (SQLException e) {
            if (istUniqueVerletzung(e)) {
                throw new DuplikatException("Verein existiert bereits: " + norm, e);
            }
            throw new SpielerDbException("Verein-Insert fehlgeschlagen: " + norm, e);
        }
    }

    public void update(int nr, String neuerName) throws SpielerDbException {
        String norm = neuerName.strip();
        if (norm.isEmpty()) {
            throw new SpielerDbException("Vereinsname darf nicht leer sein");
        }
        String sql = "UPDATE VEREIN SET NAME = ? WHERE NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, norm);
            ps.setInt(2, nr);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SpielerDbException("Verein nicht gefunden: nr=" + nr);
            }
        } catch (SQLException e) {
            if (istUniqueVerletzung(e)) {
                throw new DuplikatException("Verein existiert bereits: " + norm, e);
            }
            throw new SpielerDbException("Verein-Update fehlgeschlagen: nr=" + nr, e);
        }
    }

    /**
     * Löscht einen Verein. Schlägt mit {@link InBenutzungException} fehl,
     * wenn noch Spieler zugeordnet sind (FK ON DELETE RESTRICT).
     */
    public void delete(int nr) throws SpielerDbException {
        String sql = "DELETE FROM VEREIN WHERE NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nr);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SpielerDbException("Verein nicht gefunden: nr=" + nr);
            }
        } catch (SQLException e) {
            if (istFkVerletzung(e)) {
                throw new InBenutzungException("Verein wird noch von Spielern verwendet: nr=" + nr, e);
            }
            throw new SpielerDbException("Verein-Delete fehlgeschlagen: nr=" + nr, e);
        }
    }

    /** Anzahl Spieler, die diesem Verein zugeordnet sind. */
    public int countSpieler(int vereinNr) throws SpielerDbException {
        String sql = "SELECT COUNT(*) FROM SPIELER WHERE VEREIN_NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, vereinNr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Spieler-Count fehlgeschlagen: vereinNr=" + vereinNr, e);
        }
    }

    private static boolean istUniqueVerletzung(SQLException e) {
        String raw = e.getMessage();
        if (raw == null) {
            return false;
        }
        String msg = raw.toLowerCase(java.util.Locale.ROOT);
        // SQLite: "[SQLITE_CONSTRAINT_UNIQUE] ... UNIQUE constraint failed: ..."
        if (msg.contains("unique constraint failed") || msg.contains("sqlite_constraint_unique")) {
            return true;
        }
        // Generischer 23xxx-Constraint-State (HSQLDB / sonstige) als Fallback.
        String state = e.getSQLState();
        return state != null && state.startsWith("23");
    }

    private static boolean istFkVerletzung(SQLException e) {
        String raw = e.getMessage();
        if (raw == null) {
            return false;
        }
        String msg = raw.toLowerCase(java.util.Locale.ROOT);
        if (msg.contains("foreign key constraint failed") || msg.contains("sqlite_constraint_foreignkey")) {
            return true;
        }
        String state = e.getSQLState();
        return state != null && state.startsWith("23")
                && (msg.contains("foreign") || msg.contains("integrity"));
    }

    /** Name (case-insensitiv) ist bereits vergeben. */
    public static final class DuplikatException extends SpielerDbException {
        private static final long serialVersionUID = 1L;
        public DuplikatException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Verein hat noch zugeordnete Spieler (RESTRICT). */
    public static final class InBenutzungException extends SpielerDbException {
        private static final long serialVersionUID = 1L;
        public InBenutzungException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
