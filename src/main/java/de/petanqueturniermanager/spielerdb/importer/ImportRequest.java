package de.petanqueturniermanager.spielerdb.importer;

import java.nio.file.Path;
import java.util.EnumSet;

import de.petanqueturniermanager.spielerdb.SpielerDbDateiFormat;
import de.petanqueturniermanager.spielerdb.export.ExportEntity;

/**
 * Gebündelte Aufrufparameter eines Imports.
 *
 * @param format   Quellformat (CSV, JSON, Calc; SQLite-Restore läuft über
 *                 separaten Pfad).
 * @param entities Zu importierende Entities. Junction wird vom Importer
 *                 automatisch ergänzt, wenn Spieler und Labels beide
 *                 enthalten sind.
 * @param source   Quellpfad — bei {@link SpielerDbDateiFormat#CSV} ein
 *                 Verzeichnis, sonst eine Datei.
 * @param modus    UI-Preset für die Konfliktbehandlung.
 * @param dryRun   Wenn {@code true}, läuft die Pipeline vollständig durch,
 *                 die Transaktion wird aber zurückgerollt — Counts werden
 *                 trotzdem geliefert (Vorbereitung für Preview-UI).
 */
public record ImportRequest(
        SpielerDbDateiFormat format,
        EnumSet<ExportEntity> entities,
        Path source,
        ImportModus modus,
        boolean dryRun) {

    public ImportRequest {
        entities = EnumSet.copyOf(entities);
    }
}
