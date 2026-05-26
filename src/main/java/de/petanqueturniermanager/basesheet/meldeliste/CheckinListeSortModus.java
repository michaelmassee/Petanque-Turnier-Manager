/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

/**
 * Sortierreihenfolge der Checkin-Liste, konfigurierbar in der Turnier-Konfiguration.
 * <p>
 * Der {@code key} wird als Wert der Auswahl-Property (Combobox) in den Document-Properties
 * gespeichert und über {@code readEnumProperty(...)} wieder eingelesen.
 */
public enum CheckinListeSortModus {

    /** Standard: Sortierung nach Nachname (letzte Namens-Spalte). */
    NACHNAME,
    /** Sortierung nach Melde-/Teamnummer. */
    NUMMER;

    /**
     * Schlüssel für die Speicherung als Document-Property.
     *
     * @return Enum-Name als Schlüssel
     */
    public String getKey() {
        return name();
    }
}
