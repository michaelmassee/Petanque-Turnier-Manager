package de.petanqueturniermanager.jedergegenjeden.konfiguration;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.upload.IUploadKonfigurierbar;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public interface IJGJProperiesSpalte extends de.petanqueturniermanager.basesheet.konfiguration.IFreispielPropertiesSpalte, IUploadKonfigurierbar {
	Integer getSpielPlanHeaderFarbe() throws GenerateException;

	Integer getSpielPlanHintergrundFarbeUnGerade() throws GenerateException;

	Integer getSpielPlanHintergrundFarbeGerade() throws GenerateException;

	String getKopfZeileLinks() throws GenerateException;

	String getKopfZeileMitte() throws GenerateException;

	String getKopfZeileRechts() throws GenerateException;

	Formation getMeldeListeFormation();

	void setMeldeListeFormation(Formation formation);

	boolean isMeldeListeTeamnameAnzeigen();

	void setMeldeListeTeamnameAnzeigen(boolean anzeigen);

	boolean isMeldeListeVereinsnameAnzeigen();

	void setMeldeListeVereinsnameAnzeigen(boolean anzeigen);

	SpielplanTeamAnzeige getSpielplanTeamAnzeige();

	void setSpielplanTeamAnzeige(SpielplanTeamAnzeige anzeige);

	int getGruppengroesse();

	void setGruppengroesse(int groesse);

	boolean isRueckrunde();

	void setRueckrunde(boolean mitRueckrunde);

	JGJGesamtranglisteSortModus getGesamtranglisteSortModus();
}
