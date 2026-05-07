package de.petanqueturniermanager.spielerdb.importer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.importer.ValidierteDaten.ValLabel;
import de.petanqueturniermanager.spielerdb.importer.ValidierteDaten.ValSpieler;
import de.petanqueturniermanager.spielerdb.importer.ValidierteDaten.ValSpielerLabel;
import de.petanqueturniermanager.spielerdb.importer.ValidierteDaten.ValVerein;
import de.petanqueturniermanager.spielerdb.matching.SpielerMatchKeyNormalizer;

/**
 * Apply-Schicht des Imports. Wendet die {@link ValidierteDaten} in einer
 * einzigen Transaktion an. Reihenfolge: Vereine → Labels → Spieler → Junction
 * (FK-Constraints).
 *
 * <p>Bei {@code dryRun=true} wird die Transaktion am Ende zurückgerollt; die
 * Counts im {@link ImportErgebnis} sind trotzdem korrekt — Vorbereitung für
 * eine spätere Preview-UI.
 *
 * <p>Konflikt-Logik je Entity (per {@link ImportModus}):
 * <ul>
 *   <li>Vereine/Labels: SKIP oder UPDATE (kein INSERT_NEU — Identität läuft
 *       über den Namen).</li>
 *   <li>Spieler: SKIP, UPDATE_MERGE (feldweise; Lizenznr nur wenn Ziel leer;
 *       Labels werden gemerged statt ersetzt) oder INSERT_NEU.</li>
 * </ul>
 */
public final class SpielerDbImporter {

    private static final Logger logger = LogManager.getLogger(SpielerDbImporter.class);

    private final SpielerDbConnection dbConn;

    public SpielerDbImporter(SpielerDbConnection dbConn) {
        this.dbConn = dbConn;
    }

    public ImportErgebnis importiere(ValidierteDaten daten, ImportRequest request)
            throws SpielerDbException {
        Connection con = dbConn.getConnection();
        ImportIdMapping mapping = new ImportIdMapping();
        Counter counter = new Counter();
        List<ImportWarnung> warnungen = new ArrayList<>(daten.warnungen());

        boolean vorigerAutoCommit = true;
        try {
            vorigerAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            verarbeiteVereine(con, daten.vereine(), mapping, counter, request.modus());
            verarbeiteLabels(con, daten.labels(), mapping, counter, request.modus());
            verarbeiteSpieler(con, daten.spieler(), mapping, counter, warnungen, request.modus());
            verarbeiteJunction(con, daten.spielerLabels(), mapping, counter, request.modus());

            if (request.dryRun()) {
                con.rollback();
            } else {
                con.commit();
            }
            return counter.zuErgebnis(request.dryRun(), warnungen);
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException rb) {
                logger.warn("Rollback fehlgeschlagen", rb);
            }
            throw new SpielerDbException("Import fehlgeschlagen", e);
        } finally {
            try {
                con.setAutoCommit(vorigerAutoCommit);
            } catch (SQLException e) {
                logger.warn("AutoCommit-Wiederherstellung fehlgeschlagen", e);
            }
        }
    }

    // ---- Vereine -----------------------------------------------------------

    private static void verarbeiteVereine(Connection con, List<ValVerein> vereine,
            ImportIdMapping mapping, Counter counter, ImportModus modus) throws SQLException {
        for (ValVerein v : vereine) {
            Optional<Integer> bestehend = findeVereinByName(con, v.name());
            if (bestehend.isPresent()) {
                int nr = bestehend.get();
                if (modus == ImportModus.AKTUALISIEREN) {
                    aktualisiereVereinName(con, nr, v.name());
                    counter.vereineAktualisiert++;
                } else {
                    counter.vereineUebersprungen++;
                }
                mapping.merkeVerein(v.altNr(), nr);
            } else {
                int neueNr = insertVerein(con, v.name());
                counter.vereineEingefuegt++;
                mapping.merkeVerein(v.altNr(), neueNr);
            }
        }
    }

    private static Optional<Integer> findeVereinByName(Connection con, String name) throws SQLException {
        String sql = "SELECT NR FROM VEREIN WHERE LOWER(NAME) = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty();
            }
        }
    }

    private static int insertVerein(Connection con, String name) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO VEREIN (NAME) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Verein-Insert lieferte keinen Schlüssel");
                }
                return keys.getInt(1);
            }
        }
    }

    private static void aktualisiereVereinName(Connection con, int nr, String name)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE VEREIN SET NAME = ? WHERE NR = ?")) {
            ps.setString(1, name);
            ps.setInt(2, nr);
            ps.executeUpdate();
        }
    }

    // ---- Labels ------------------------------------------------------------

    private static void verarbeiteLabels(Connection con, List<ValLabel> labels,
            ImportIdMapping mapping, Counter counter, ImportModus modus) throws SQLException {
        for (ValLabel l : labels) {
            Optional<Integer> bestehend = findeLabelByName(con, l.name());
            if (bestehend.isPresent()) {
                int nr = bestehend.get();
                if (modus == ImportModus.AKTUALISIEREN) {
                    aktualisiereLabelName(con, nr, l.name());
                    counter.labelsAktualisiert++;
                } else {
                    counter.labelsUebersprungen++;
                }
                mapping.merkeLabel(l.altNr(), nr);
            } else {
                int neueNr = insertLabel(con, l.name());
                counter.labelsEingefuegt++;
                mapping.merkeLabel(l.altNr(), neueNr);
            }
        }
    }

    private static Optional<Integer> findeLabelByName(Connection con, String name) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT NR FROM LABEL WHERE LOWER(NAME) = ?")) {
            ps.setString(1, name.toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty();
            }
        }
    }

    private static int insertLabel(Connection con, String name) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO LABEL (NAME) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Label-Insert lieferte keinen Schlüssel");
                }
                return keys.getInt(1);
            }
        }
    }

    private static void aktualisiereLabelName(Connection con, int nr, String name)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE LABEL SET NAME = ? WHERE NR = ?")) {
            ps.setString(1, name);
            ps.setInt(2, nr);
            ps.executeUpdate();
        }
    }

    // ---- Spieler -----------------------------------------------------------

    private record DbSpieler(int nr, String vorname, String nachname,
            @Nullable Integer vereinNr, @Nullable String lizenznr) { }

    private static void verarbeiteSpieler(Connection con, List<ValSpieler> spieler,
            ImportIdMapping mapping, Counter counter, List<ImportWarnung> warnungen,
            ImportModus modus) throws SQLException {
        for (ValSpieler s : spieler) {
            Integer neueVereinNr = mapping.neueVereinNr(s.altVereinNr());
            Optional<DbSpieler> bestehend = findeSpieler(con, s, neueVereinNr);

            if (bestehend.isEmpty()) {
                int neueNr = insertSpieler(con, s.vorname(), s.nachname(),
                        neueVereinNr, s.lizenznr());
                counter.spielerEingefuegt++;
                mapping.merkeSpieler(s.altNr(), neueNr);
                continue;
            }

            DbSpieler db = bestehend.get();
            switch (modus) {
                case NUR_NEUE -> {
                    counter.spielerUebersprungen++;
                    mapping.merkeSpieler(s.altNr(), db.nr());
                }
                case AKTUALISIEREN -> {
                    aktualisiereSpielerMerge(con, db, s, neueVereinNr, warnungen);
                    counter.spielerAktualisiert++;
                    mapping.merkeSpieler(s.altNr(), db.nr());
                }
                case DUPLIKATE_SEPARAT -> {
                    Optional<Integer> neu = versucheInsertSpieler(con, s.vorname(), s.nachname(),
                            neueVereinNr, s.lizenznr(), warnungen);
                    if (neu.isPresent()) {
                        counter.spielerEingefuegt++;
                        mapping.merkeSpieler(s.altNr(), neu.get());
                    } else {
                        counter.spielerUebersprungen++;
                    }
                }
            }
        }
    }

    private static Optional<DbSpieler> findeSpieler(Connection con, ValSpieler s,
            @Nullable Integer neueVereinNr) throws SQLException {
        if (s.lizenznr() != null && !s.lizenznr().isEmpty()) {
            Optional<DbSpieler> perLizenz = findeSpielerByLizenz(con, s.lizenznr());
            if (perLizenz.isPresent()) {
                return perLizenz;
            }
        }
        return findeSpielerByNameUndVerein(con, s.vorname(), s.nachname(), neueVereinNr);
    }

    private static Optional<DbSpieler> findeSpielerByLizenz(Connection con, String lizenznr)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT NR, VORNAME, NACHNAME, VEREIN_NR, LIZENZNR FROM SPIELER WHERE LIZENZNR = ?")) {
            ps.setString(1, lizenznr);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapDb(rs)) : Optional.empty();
            }
        }
    }

    private static Optional<DbSpieler> findeSpielerByNameUndVerein(Connection con,
            String vorname, String nachname, @Nullable Integer vereinNr) throws SQLException {
        // Suche per normalisiertem Schlüssel; SQL-LOWER + Java-Whitespace-Collapse
        // auf Datenbankseite ist nicht trivial — daher load-and-compare.
        String sql = vereinNr == null
                ? "SELECT NR, VORNAME, NACHNAME, VEREIN_NR, LIZENZNR FROM SPIELER "
                        + "WHERE VEREIN_NR IS NULL"
                : "SELECT NR, VORNAME, NACHNAME, VEREIN_NR, LIZENZNR FROM SPIELER "
                        + "WHERE VEREIN_NR = ?";
        String suchKey = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinNr(
                vorname, nachname, vereinNr);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (vereinNr != null) {
                ps.setInt(1, vereinNr);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DbSpieler db = mapDb(rs);
                    String dbKey = SpielerMatchKeyNormalizer.spielerSchluesselMitVereinNr(
                            db.vorname(), db.nachname(), db.vereinNr());
                    if (dbKey.equals(suchKey)) {
                        return Optional.of(db);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static DbSpieler mapDb(ResultSet rs) throws SQLException {
        int nr = rs.getInt(1);
        String vorname = rs.getString(2);
        String nachname = rs.getString(3);
        int vereinNrRaw = rs.getInt(4);
        Integer vereinNr = rs.wasNull() ? null : vereinNrRaw;
        String lizenz = rs.getString(5);
        return new DbSpieler(nr, vorname, nachname, vereinNr, lizenz);
    }

    private static int insertSpieler(Connection con, String vorname, String nachname,
            @Nullable Integer vereinNr, @Nullable String lizenznr) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO SPIELER (VORNAME, NACHNAME, VEREIN_NR, LIZENZNR) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, vorname);
            ps.setString(2, nachname);
            if (vereinNr == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, vereinNr);
            }
            if (lizenznr == null || lizenznr.isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, lizenznr);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Spieler-Insert lieferte keinen Schlüssel");
                }
                return keys.getInt(1);
            }
        }
    }

    /**
     * Variante mit defensiver Behandlung von UNIQUE-Verletzungen — Lizenznr
     * kann von einem anderen, nicht gematchten Spieler bereits belegt sein.
     */
    private static Optional<Integer> versucheInsertSpieler(Connection con, String vorname,
            String nachname, @Nullable Integer vereinNr, @Nullable String lizenznr,
            List<ImportWarnung> warnungen) throws SQLException {
        try {
            return Optional.of(insertSpieler(con, vorname, nachname, vereinNr, lizenznr));
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (msg.contains("unique") || msg.contains("constraint")) {
                warnungen.add(new ImportWarnung("Spieler '" + vorname + " " + nachname
                        + "' konnte nicht eingefügt werden (UNIQUE-Verletzung) — übersprungen"));
                return Optional.empty();
            }
            throw e;
        }
    }

    private static void aktualisiereSpielerMerge(Connection con, DbSpieler db, ValSpieler s,
            @Nullable Integer neueVereinNr, List<ImportWarnung> warnungen) throws SQLException {
        // Feldweise:
        // - Vorname/Nachname/Verein: überschreiben
        // - Lizenznr: nur setzen, wenn Ziel leer
        @Nullable String neueLizenz = db.lizenznr();
        if ((neueLizenz == null || neueLizenz.isEmpty())
                && s.lizenznr() != null && !s.lizenznr().isEmpty()) {
            neueLizenz = s.lizenznr();
        } else if (s.lizenznr() != null && !s.lizenznr().isEmpty()
                && !s.lizenznr().equals(db.lizenznr())) {
            warnungen.add(new ImportWarnung(
                    "Lizenznummer für '" + s.vorname() + " " + s.nachname()
                            + "' bleibt erhalten (DB: '" + db.lizenznr()
                            + "', Import: '" + s.lizenznr() + "')"));
        }

        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE SPIELER SET VORNAME = ?, NACHNAME = ?, VEREIN_NR = ?, LIZENZNR = ? "
                        + "WHERE NR = ?")) {
            ps.setString(1, s.vorname());
            ps.setString(2, s.nachname());
            if (neueVereinNr == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, neueVereinNr);
            }
            if (neueLizenz == null || neueLizenz.isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, neueLizenz);
            }
            ps.setInt(5, db.nr());
            ps.executeUpdate();
        }
    }

    // ---- Junction ----------------------------------------------------------

    private static void verarbeiteJunction(Connection con, List<ValSpielerLabel> junction,
            ImportIdMapping mapping, Counter counter, ImportModus modus) throws SQLException {
        // Bei NUR_NEUE und DUPLIKATE_SEPARAT könnte man Junction-Inserts überspringen
        // für übersprungene Spieler — aber das mapping liefert dann eh null.
        // INSERT OR IGNORE macht doppelte Einträge zum No-Op (Merge-Semantik).
        Map<Integer, Integer> inserted = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT OR IGNORE INTO SPIELER_LABEL (SPIELER_NR, LABEL_NR) VALUES (?, ?)")) {
            for (ValSpielerLabel j : junction) {
                Integer sNr = mapping.neueSpielerNr(j.altSpielerNr());
                Integer lNr = mapping.neueLabelNr(j.altLabelNr());
                if (sNr == null || lNr == null) {
                    continue;
                }
                ps.setInt(1, sNr);
                ps.setInt(2, lNr);
                int n = ps.executeUpdate();
                if (n > 0) {
                    counter.junctionEingefuegt++;
                    inserted.merge(sNr, 1, Integer::sum);
                }
            }
        }
        // Stille Verwendung von modus — vermeidet "unused parameter"-Warnungen
        // und dokumentiert, dass das aktuelle Verhalten modus-unabhängig ist.
        logger.debug("Junction-Phase abgeschlossen, Modus={}, neue Einträge={}",
                modus, inserted.size());
    }

    // ---- Counter -----------------------------------------------------------

    private static final class Counter {
        int spielerEingefuegt;
        int spielerAktualisiert;
        int spielerUebersprungen;
        int vereineEingefuegt;
        int vereineAktualisiert;
        int vereineUebersprungen;
        int labelsEingefuegt;
        int labelsAktualisiert;
        int labelsUebersprungen;
        int junctionEingefuegt;

        ImportErgebnis zuErgebnis(boolean dryRun, List<ImportWarnung> warnungen) {
            return new ImportErgebnis(spielerEingefuegt, spielerAktualisiert, spielerUebersprungen,
                    vereineEingefuegt, vereineAktualisiert, vereineUebersprungen,
                    labelsEingefuegt, labelsAktualisiert, labelsUebersprungen,
                    junctionEingefuegt, dryRun, warnungen);
        }
    }
}
