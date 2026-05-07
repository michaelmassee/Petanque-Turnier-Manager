package de.petanqueturniermanager.spielerdb.export;

import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Gebündelte Aufrufparameter eines Exports.
 *
 * @param format   Zielformat
 * @param entities Zu exportierende Entities (Loader ergänzt
 *                 {@link ExportEntity#SPIELER_LABELS} automatisch, wenn
 *                 SPIELER und LABELS beide enthalten sind)
 * @param filter   Filter-Strategie (aktuell nur {@link AllExportFilter})
 * @param target   Zielpfad — bei {@link ExportFormat#CSV} ein Verzeichnis,
 *                 sonst eine einzelne Datei
 */
public record ExportRequest(
        ExportFormat format,
        EnumSet<ExportEntity> entities,
        ExportFilter filter,
        Path target) {

    public ExportRequest {
        entities = EnumSet.copyOf(entities);
    }
}
