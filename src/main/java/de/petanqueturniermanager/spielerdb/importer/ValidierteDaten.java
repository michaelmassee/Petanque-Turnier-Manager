package de.petanqueturniermanager.spielerdb.importer;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Ergebnis der Validierungs-Stufe: Pflichtfelder garantiert, Junction-Refs
 * konsistent (oder bereits ausgefiltert mit Warnung). Wird vom Importer
 * konsumiert.
 */
public record ValidierteDaten(
        List<ValSpieler> spieler,
        List<ValVerein> vereine,
        List<ValLabel> labels,
        List<ValSpielerLabel> spielerLabels,
        List<ImportWarnung> warnungen) {

    public ValidierteDaten {
        spieler = List.copyOf(spieler);
        vereine = List.copyOf(vereine);
        labels = List.copyOf(labels);
        spielerLabels = List.copyOf(spielerLabels);
        warnungen = List.copyOf(warnungen);
    }

    /** {@code altNr} ist die NR aus der Import-Datei, nur für Junction-Auflösung. */
    public record ValSpieler(
            @Nullable Integer altNr,
            String vorname,
            String nachname,
            @Nullable Integer altVereinNr,
            @Nullable String vereinName,
            @Nullable String lizenznr) { }

    public record ValVerein(@Nullable Integer altNr, String name) { }

    public record ValLabel(@Nullable Integer altNr, String name) { }

    public record ValSpielerLabel(int altSpielerNr, int altLabelNr) { }
}
