package de.petanqueturniermanager.spielerdb.export;

import java.nio.file.Path;
import java.util.EnumSet;

import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;

/**
 * Gebündelte Aufrufparameter eines Exports.
 *
 * @param format   Zielformat
 * @param entities Zu exportierende Entities (Loader ergänzt
 *                 {@link ExportEntity#SPIELER_LABELS} automatisch, wenn
 *                 SPIELER und LABELS beide enthalten sind)
 * @param filter   Filter-Strategie (aktuell nur {@link AllExportFilter})
 * @param target   Zielpfad — eine einzelne Datei (für CSV: {@code .csv}-Datei)
 */
public record ExportRequest(
        SpielerDbDateiFormat format,
        EnumSet<ExportEntity> entities,
        ExportFilter filter,
        Path target) {

    public ExportRequest {
        entities = EnumSet.copyOf(entities);
    }
}
