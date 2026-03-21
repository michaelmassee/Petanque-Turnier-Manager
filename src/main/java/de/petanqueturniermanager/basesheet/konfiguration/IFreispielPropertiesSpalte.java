package de.petanqueturniermanager.basesheet.konfiguration;

/**
 * Interface für Turniersysteme mit Freispiel-Unterstützung.
 * Definiert konfigurierbare Spielpunkte für Freispiel-Runden (Team B = 0).
 */
public interface IFreispielPropertiesSpalte {

	Integer getFreispielPunktePlus();

	Integer getFreispielPunkteMinus();

}
