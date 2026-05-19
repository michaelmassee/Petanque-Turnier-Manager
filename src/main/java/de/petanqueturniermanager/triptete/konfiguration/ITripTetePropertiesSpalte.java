package de.petanqueturniermanager.triptete.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;

/**
 * Konfigurations-Properties für das Trip-Tête-Turniersystem.
 */
public interface ITripTetePropertiesSpalte extends IPropertiesSpalte {

	Integer getSpielPlanHeaderFarbe();

	Integer getSpielPlanHintergrundFarbeUnGerade();

	Integer getSpielPlanHintergrundFarbeGerade();

	String getKopfZeileLinks();

	String getKopfZeileMitte();

	String getKopfZeileRechts();

	Integer getSpielZiel();

}
