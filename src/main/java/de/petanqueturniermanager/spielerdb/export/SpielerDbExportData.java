package de.petanqueturniermanager.spielerdb.export;

import java.util.List;

import de.petanqueturniermanager.spielerdb.LabelDatensatz;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.VereinDatensatz;

/**
 * In-Memory-Repräsentation aller Daten, die ein konkreter Export schreibt.
 * Wird einmal vom {@link SpielerDbExportLoader} aus den Repositories befüllt
 * und anschließend an die einzelnen {@link SpielerDbExporter} weitergereicht.
 *
 * <p>Listen, die nicht im Scope sind, bleiben leer (statt {@code null}). Das
 * vereinfacht die Exporter-Logik und vermeidet doppelte Null-Checks.
 */
public record SpielerDbExportData(
        ExportMeta meta,
        List<SpielerMitVerein> spieler,
        List<VereinDatensatz> vereine,
        List<LabelDatensatz> labels,
        List<SpielerLabelZuordnung> spielerLabels) {

    public SpielerDbExportData {
        spieler = List.copyOf(spieler);
        vereine = List.copyOf(vereine);
        labels = List.copyOf(labels);
        spielerLabels = List.copyOf(spielerLabels);
    }
}
