package de.petanqueturniermanager.spielerdb.importer;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Rohe (noch nicht validierte) Daten aus einer Import-Quelle. Alle Felder
 * sind als String/Integer-Boxed eingelesen, damit der Validator klare
 * Fehlermeldungen zu Null/Empty/Leerstring liefern kann.
 *
 * <p>Listen für Entities außerhalb des Scopes bleiben leer (statt null).
 *
 * <p>{@code nr}-Felder sind die <b>alten</b> NRs aus der Import-Datei und
 * werden ausschließlich für die Junction-Auflösung verwendet — niemals als
 * Ziel-IDs in der DB.
 */
public record ImportRohdaten(
        List<RohSpieler> spieler,
        List<RohVerein> vereine,
        List<RohLabel> labels,
        List<RohSpielerLabel> spielerLabels) {

    public ImportRohdaten {
        spieler = List.copyOf(spieler);
        vereine = List.copyOf(vereine);
        labels = List.copyOf(labels);
        spielerLabels = List.copyOf(spielerLabels);
    }

    public record RohSpieler(
            @Nullable Integer nr,
            String vorname,
            String nachname,
            @Nullable Integer vereinNr,
            @Nullable String vereinName,
            @Nullable String lizenznr) { }

    public record RohVerein(
            @Nullable Integer nr,
            String name) { }

    public record RohLabel(
            @Nullable Integer nr,
            String name) { }

    public record RohSpielerLabel(int spielerNr, int labelNr) { }
}
