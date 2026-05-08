package de.petanqueturniermanager.spielerdb.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import org.sqlite.SQLiteConnection;

import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbException;

/**
 * Erstellt ein konsistentes SQLite-Backup über die native xerial-Backup-API
 * ({@code SQLiteConnection.getDatabase().backup(...)}). Das ruft intern die
 * SQLite-C-Backup-API auf und produziert einen atomar konsistenten Snapshot —
 * auch bei laufender Connection und parallelen Schreibern.
 *
 * <p>Ein {@code Files.copy()}-Fallback wäre gefährlich (möglicherweise
 * partieller WAL-/Hauptdatei-Stand bei aktiver Verbindung) und ist daher
 * bewusst nicht implementiert.
 */
public final class SpielerDbSqliteBackupExporter implements SpielerDbExporter {

    private final SpielerDbConnection dbConnection;

    public SpielerDbSqliteBackupExporter(SpielerDbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public void export(SpielerDbExportData data, ExportRequest request) throws SpielerDbException {
        Path datei = request.target();
        Path elternverzeichnis = datei.toAbsolutePath().getParent();
        if (elternverzeichnis != null) {
            try {
                Files.createDirectories(elternverzeichnis);
            } catch (IOException e) {
                throw new SpielerDbException(
                        "SQLite-Backup-Verzeichnis nicht anlegbar: " + elternverzeichnis, e);
            }
        }
        try {
            Files.deleteIfExists(datei);
        } catch (IOException e) {
            throw new SpielerDbException(
                    "Vorherige Backup-Datei konnte nicht gelöscht werden: " + datei, e);
        }

        Connection conn = dbConnection.getConnection();
        SQLiteConnection sqlite;
        try {
            sqlite = conn.unwrap(SQLiteConnection.class);
        } catch (SQLException e) {
            throw new SpielerDbException(
                    "SQLite-Backup nicht möglich: Connection ist keine SQLiteConnection", e);
        }
        try {
            sqlite.getDatabase().backup("main", datei.toAbsolutePath().toString(), null);
        } catch (SQLException e) {
            throw new SpielerDbException("SQLite-Backup fehlgeschlagen: " + datei, e);
        }
    }
}
