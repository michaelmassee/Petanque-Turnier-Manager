/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * @author Michael Massee
 *
 */
public interface ILigaPropertiesSpalte {

	Integer getSpielPlanHeaderFarbe() throws GenerateException;

	Integer getSpielPlanHintergrundFarbeUnGerade() throws GenerateException;

	Integer getSpielPlanHintergrundFarbeGerade() throws GenerateException;

	String getKopfZeileLinks() throws GenerateException;

	String getKopfZeileMitte() throws GenerateException;

	String getKopfZeileRechts() throws GenerateException;

	String getGruppennamen() throws GenerateException;

	String getBaseDownloadUrl() throws GenerateException;

	String getLigaLogoUr() throws GenerateException;

}
