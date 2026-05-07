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
 * CRUD für {@link LabelDatensatz}. Eingabe-Namen werden konsistent getrimmt;
 * Lookup ist case-insensitiv. Architektur und Fehlerbehandlung sind bewusst
 * spiegelbildlich zu {@link VereinRepository} aufgebaut.
 */
public final class LabelRepository {

    private final Connection connection;

    public LabelRepository(SpielerDbConnection conn) {
        this.connection = conn.getConnection();
    }

    public List<LabelDatensatz> findAll() throws SpielerDbException {
        String sql = "SELECT NR, NAME FROM LABEL ORDER BY JAVA_LOWER(NAME)";
        List<LabelDatensatz> ergebnis = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ergebnis.add(new LabelDatensatz(rs.getInt("NR"), rs.getString("NAME")));
            }
            return ergebnis;
        } catch (SQLException e) {
            throw new SpielerDbException("Labels laden fehlgeschlagen", e);
        }
    }

    /** Case-insensitiver, getrimmter Lookup. */
    public Optional<LabelDatensatz> findByName(String name) throws SpielerDbException {
        String norm = name.strip();
        String sql = "SELECT NR, NAME FROM LABEL WHERE JAVA_LOWER(NAME) = JAVA_LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, norm);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new LabelDatensatz(rs.getInt("NR"), rs.getString("NAME")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Label-Lookup fehlgeschlagen: " + name, e);
        }
    }

    public Optional<LabelDatensatz> findById(int nr) throws SpielerDbException {
        String sql = "SELECT NR, NAME FROM LABEL WHERE NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new LabelDatensatz(rs.getInt("NR"), rs.getString("NAME")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Label-Lookup fehlgeschlagen: nr=" + nr, e);
        }
    }

    /**
     * Insert mit getrimmtem Namen. Wirft {@link DuplikatException}, wenn der
     * Name (case-insensitiv) bereits existiert.
     */
    public LabelDatensatz insert(String name) throws SpielerDbException {
        String norm = name.strip();
        if (norm.isEmpty()) {
            throw new SpielerDbException("Labelname darf nicht leer sein");
        }
        String sql = "INSERT INTO LABEL (NAME) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, norm);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new LabelDatensatz(keys.getInt(1), norm);
                }
                throw new SpielerDbException("Insert lieferte keinen Schlüssel");
            }
        } catch (SQLException e) {
            if (istUniqueVerletzung(e)) {
                throw new DuplikatException("Label existiert bereits: " + norm, e);
            }
            throw new SpielerDbException("Label-Insert fehlgeschlagen: " + norm, e);
        }
    }

    public void update(int nr, String neuerName) throws SpielerDbException {
        String norm = neuerName.strip();
        if (norm.isEmpty()) {
            throw new SpielerDbException("Labelname darf nicht leer sein");
        }
        String sql = "UPDATE LABEL SET NAME = ? WHERE NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, norm);
            ps.setInt(2, nr);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SpielerDbException("Label nicht gefunden: nr=" + nr);
            }
        } catch (SQLException e) {
            if (istUniqueVerletzung(e)) {
                throw new DuplikatException("Label existiert bereits: " + norm, e);
            }
            throw new SpielerDbException("Label-Update fehlgeschlagen: nr=" + nr, e);
        }
    }

    /**
     * Löscht ein Label. Schlägt mit {@link InBenutzungException} fehl, wenn
     * noch Spieler zugeordnet sind (FK ON DELETE RESTRICT).
     */
    public void delete(int nr) throws SpielerDbException {
        String sql = "DELETE FROM LABEL WHERE NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nr);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SpielerDbException("Label nicht gefunden: nr=" + nr);
            }
        } catch (SQLException e) {
            if (istFkVerletzung(e)) {
                throw new InBenutzungException("Label wird noch von Spielern verwendet: nr=" + nr, e);
            }
            throw new SpielerDbException("Label-Delete fehlgeschlagen: nr=" + nr, e);
        }
    }

    /** Anzahl Spieler, denen dieses Label zugeordnet ist. */
    public int countSpieler(int labelNr) throws SpielerDbException {
        String sql = "SELECT COUNT(*) FROM SPIELER WHERE LABEL_NR = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, labelNr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new SpielerDbException("Spieler-Count fehlgeschlagen: labelNr=" + labelNr, e);
        }
    }

    private static boolean istUniqueVerletzung(SQLException e) {
        String raw = e.getMessage();
        if (raw == null) {
            return false;
        }
        String msg = raw.toLowerCase(java.util.Locale.ROOT);
        if (msg.contains("unique constraint failed") || msg.contains("sqlite_constraint_unique")) {
            return true;
        }
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

    /** Label hat noch zugeordnete Spieler (RESTRICT). */
    public static final class InBenutzungException extends SpielerDbException {
        private static final long serialVersionUID = 1L;
        public InBenutzungException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
