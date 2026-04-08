/**
 * Erstellung : 22.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.meldeliste;

import de.petanqueturniermanager.helper.i18n.I18n;

public enum TurnierSystem {

	KEIN(0, "enum.turniersystem.kein"), // wenn nur der runner gebraucht wird
	SUPERMELEE(1, "enum.turniersystem.supermelee"), LIGA(2, "enum.turniersystem.liga"),
	MAASTRICHTER(3, "enum.turniersystem.maastrichter"), SCHWEIZER(4, "enum.turniersystem.schweizer"),
	JGJ(5, "enum.turniersystem.jgj"), KO(6, "enum.turniersystem.ko"),
	POULE(7, "enum.turniersystem.poule");

	private final String i18nKey;
	private final int id;

	private TurnierSystem(int id, String i18nKey) {
		this.id = id;
		this.i18nKey = i18nKey;
	}

	public String getBezeichnung() {
		return I18n.get(i18nKey);
	}

	@Override
	public String toString() {
		return getBezeichnung();
	}

	public int getId() {
		return id;
	}

	public boolean hatMehrereSpielTage() {
		return this == SUPERMELEE;
	}

	public static TurnierSystem findById(int id) {
		for (TurnierSystem spielsystem : values()) {
			if (spielsystem.getId() == id) {
				return spielsystem;
			}
		}
		return null;
	}

}
