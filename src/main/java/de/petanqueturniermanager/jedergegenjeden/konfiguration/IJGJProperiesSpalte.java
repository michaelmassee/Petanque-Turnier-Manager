package de.petanqueturniermanager.jedergegenjeden.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public interface IJGJProperiesSpalte {
	Integer getSpielPlanHeaderFarbe() throws GenerateException;

	Integer getSpielPlanHintergrundFarbeUnGerade() throws GenerateException;

	Integer getSpielPlanHintergrundFarbeGerade() throws GenerateException;

	String getKopfZeileLinks() throws GenerateException;

	String getKopfZeileMitte() throws GenerateException;

	String getKopfZeileRechts() throws GenerateException;

	String getGruppennamen() throws GenerateException;

	void setGruppennamen(String name) throws GenerateException;
}
