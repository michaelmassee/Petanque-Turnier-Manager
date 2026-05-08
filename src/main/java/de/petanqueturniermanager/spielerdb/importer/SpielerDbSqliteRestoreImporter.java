package de.petanqueturniermanager.spielerdb.importer;

import java.nio.file.Path;

import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbException;

/**
 * Sonderpfad für Format {@code SQLITE_BACKUP}: ersetzt die laufende
 * Spieler-DB komplett durch ein Backup. Keine Konflikt-Logik — die
 * Backup-Datei wird verbatim übernommen, der alte Stand landet als
 * {@code *.bak.*} neben der DB.
 *
 * <p>Verzichtet bewusst auf Pipeline-Stufen (kein Reader, kein Validator,
 * kein Importer-Apply): SQLite kennt sein eigenes Format am besten,
 * Magic-Byte-Check ist die einzige Validierung.
 */
public final class SpielerDbSqliteRestoreImporter {

    public void restore(Path quelle) throws SpielerDbException {
        SpielerDbConnection.restoreVon(quelle);
    }
}
