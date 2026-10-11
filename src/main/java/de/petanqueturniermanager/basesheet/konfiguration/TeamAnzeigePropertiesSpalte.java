/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.helper.ISheet;

/**
 * Basis für Konfigurationen von Turniersystemen mit optionaler Teamname-Spalte in der Meldeliste.
 * <p>
 * Kapselt das gemeinsame Verhalten aller Team-Anzeige-Properties (Spielplan, Rangliste,
 * K.-o.-Baum): Ohne Teamname-Spalte wird {@link TeamAnzeige#NAME} beim Lesen auf
 * {@link TeamAnzeige#SPIELERNAMEN} zurückgeführt.
 * <p>
 * Das Speichern ändert die Teamname-Spalte bewusst nicht: Sie bestimmt das Layout der bereits
 * aufgebauten Meldeliste und darf nur zusammen mit deren Neuaufbau umgeschaltet werden.
 */
public abstract class TeamAnzeigePropertiesSpalte extends BasePropertiesSpalte {

	public static final String KONFIG_PROP_RANGLISTE_TEAM_ANZEIGE = "Rangliste Team Anzeige";

	protected TeamAnzeigePropertiesSpalte(ISheet sheet) {
		super(sheet);
	}

	public abstract boolean isMeldeListeTeamnameAnzeigen();

	/** Liest eine Team-Anzeige und liefert die mit der aktuellen Meldeliste darstellbare Variante. */
	protected TeamAnzeige leseTeamAnzeige(String key, TeamAnzeige standard) {
		return readEnumProperty(key, TeamAnzeige.class, standard).effektiv(isMeldeListeTeamnameAnzeigen());
	}

	protected void schreibeTeamAnzeige(String key, TeamAnzeige anzeige) {
		setStringProperty(key, anzeige.name());
	}

	public TeamAnzeige getRanglisteTeamAnzeige() {
		return leseTeamAnzeige(KONFIG_PROP_RANGLISTE_TEAM_ANZEIGE, TeamAnzeige.NAME);
	}

	public void setRanglisteTeamAnzeige(TeamAnzeige anzeige) {
		schreibeTeamAnzeige(KONFIG_PROP_RANGLISTE_TEAM_ANZEIGE, anzeige);
	}
}
