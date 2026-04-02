/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

/**
 * @author Michael Massee
 *
 */
public interface ILigaPropertiesSpalte extends de.petanqueturniermanager.basesheet.konfiguration.IFreispielPropertiesSpalte {

	Integer getSpielPlanHeaderFarbe();

	Integer getSpielPlanHintergrundFarbeUnGerade();

	Integer getSpielPlanHintergrundFarbeGerade();

	String getKopfZeileLinks();

	String getKopfZeileMitte();

	String getKopfZeileRechts();

	String getGruppenname();

	String getBaseDownloadUrl();

	String getLigaLogoUr();

	void setGruppenname(String name);

}
