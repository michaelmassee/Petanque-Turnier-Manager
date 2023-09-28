/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

/**
 * @author Michael Massee
 *
 */
public interface ILigaPropertiesSpalte {

	Integer getSpielPlanHeaderFarbe();

	Integer getSpielPlanHintergrundFarbeUnGerade();

	Integer getSpielPlanHintergrundFarbeGerade();

	String getKopfZeileLinks();

	String getKopfZeileMitte();

	String getKopfZeileRechts();

	String getGruppennamen();

	String getBaseDownloadUrl();

	String getLigaLogoUr();

	void setGruppennamen(String name);

	String getPdfImageUr();

}
