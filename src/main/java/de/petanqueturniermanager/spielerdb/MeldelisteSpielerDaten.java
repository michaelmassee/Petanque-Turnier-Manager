package de.petanqueturniermanager.spielerdb;

import org.jspecify.annotations.Nullable;

/**
 * Roh-Datensatz eines einzelnen Spieler-Slots aus der Meldeliste — getrennte
 * Vor-/Nachnamen-Felder und (falls Vereinsname-Spalte aktiv) der Vereinsname.
 * Wird vom Abgleich-Dialog genutzt, um Spieler gegen die Spieler-DB zu matchen
 * und fehlende Datensätze zu importieren.
 */
public record MeldelisteSpielerDaten(
        String vorname,
        String nachname,
        @Nullable String vereinName,
        int zeile1Basiert) {

    public MeldelisteSpielerDaten {
        vorname = vorname == null ? "" : vorname.strip();
        nachname = nachname == null ? "" : nachname.strip();
        if (vereinName != null) {
            String trimmed = vereinName.strip();
            vereinName = trimmed.isEmpty() ? null : trimmed;
        }
    }
}
