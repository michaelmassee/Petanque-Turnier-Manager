package de.petanqueturniermanager.spielerdb.export;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteErrorCode;

import de.petanqueturniermanager.helper.i18n.I18n;
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
 *
 * <p>Das Backup entsteht zuerst in einer temporären Datei daneben und ersetzt die Zieldatei erst bei Erfolg: Eine
 * vorhandene ältere Sicherung bleibt bei einem Fehler erhalten. Die laufende Spieler-DB selbst (oder ihre
 * WAL-Begleitdateien) wird als Ziel abgelehnt – sie würde sonst ersetzt, und die offene Verbindung schriebe danach in
 * die verwaiste Datei.
 */
public final class SpielerDbSqliteBackupExporter implements SpielerDbExporter {

    private static final Logger logger = LogManager.getLogger(SpielerDbSqliteBackupExporter.class);

    private final SpielerDbConnection dbConnection;

    public SpielerDbSqliteBackupExporter(SpielerDbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public void export(SpielerDbExportData data, ExportRequest request) throws SpielerDbException {
        Path datei = request.target().toAbsolutePath();
        if (dbConnection.istDbDateiOderBegleitdatei(datei)) {
            throw new SpielerDbException(I18n.get("spielerdb.export.fehler.ziel_ist_db", datei));
        }
        Path elternverzeichnis = datei.getParent();
        if (elternverzeichnis == null) {
            throw new SpielerDbException(I18n.get("spielerdb.export.fehler.verzeichnis", datei));
        }
        try {
            Files.createDirectories(elternverzeichnis);
        } catch (IOException e) {
            throw new SpielerDbException(I18n.get("spielerdb.export.fehler.verzeichnis", elternverzeichnis), e);
        }
        Path temporaer = temporaereDatei(elternverzeichnis, datei);
        try {
            int rueckgabe = dbConnection.sqliteVerbindung().getDatabase().backup("main", temporaer.toString(), null);
            if (rueckgabe != SQLiteErrorCode.SQLITE_OK.code) {
                throw new SpielerDbException(I18n.get("spielerdb.export.fehler.backup", datei,
                        SQLiteErrorCode.getErrorCode(rueckgabe).name()));
            }
            ersetze(temporaer, datei);
        } catch (SQLException e) {
            throw new SpielerDbException(I18n.get("spielerdb.export.fehler.backup", datei, e.getMessage()), e);
        } finally {
            loescheLeise(temporaer);
        }
    }

    private static Path temporaereDatei(Path verzeichnis, Path datei) throws SpielerDbException {
        try {
            return Files.createTempFile(verzeichnis, datei.getFileName() + ".", ".tmp");
        } catch (IOException e) {
            throw new SpielerDbException(I18n.get("spielerdb.export.fehler.verzeichnis", verzeichnis), e);
        }
    }

    private static void ersetze(Path temporaer, Path datei) throws SpielerDbException {
        try {
            try {
                Files.move(temporaer, datei, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                logger.debug("Atomares Ersetzen von {} nicht unterstützt, ersetze normal", datei, e);
                Files.move(temporaer, datei, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new SpielerDbException(I18n.get("spielerdb.export.fehler.ersetzen", datei), e);
        }
    }

    private static void loescheLeise(Path temporaer) {
        try {
            Files.deleteIfExists(temporaer);
        } catch (IOException e) {
            logger.warn("Temporäre Backup-Datei {} konnte nicht gelöscht werden", temporaer, e);
        }
    }
}
