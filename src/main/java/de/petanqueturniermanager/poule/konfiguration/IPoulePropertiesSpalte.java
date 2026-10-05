/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.poule.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;

/**
 * Konfigurationseigenschaften für das Poule-A/B-Turniersystem.
 */
public interface IPoulePropertiesSpalte extends IPropertiesSpalte {

    Formation getMeldeListeFormation();

    boolean isMeldeListeTeamnameAnzeigen();

    boolean isMeldeListeVereinsnameAnzeigen();

    void setMeldeListeFormation(Formation formation);

    void setMeldeListeTeamnameAnzeigen(boolean anzeigen);

    void setMeldeListeVereinsnameAnzeigen(boolean anzeigen);

    TeamAnzeige getSpielplanTeamAnzeige();

    void setSpielplanTeamAnzeige(TeamAnzeige anzeige);

    TeamAnzeige getRanglisteTeamAnzeige();

    void setRanglisteTeamAnzeige(TeamAnzeige anzeige);

    boolean isSpielplanMitBahnspalte();

    void setSpielplanMitBahnspalte(boolean mitBahnspalte);

    int getPouleVorrundeTabFarbe();

    int getPouleVorrundenRanglisteTabFarbe();

}
