/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

public enum SpielSystem {

	SUPERMELEE(1, "Supermêlée");

	private final String bezeichnung;
	private final int id;

	private SpielSystem(int id, String bezeichnung) {
		this.id = id;
		this.bezeichnung = bezeichnung;
	}

	public String getBezeichnung() {
		return this.bezeichnung;
	}

	@Override
	public String toString() {
		return this.bezeichnung;
	}

	public int getId() {
		return this.id;
	}

	public static SpielSystem findById(int id) {
		for (SpielSystem spielsystem : values()) {
			if (spielsystem.getId() == id) {
				return spielsystem;
			}
		}
		return null;
	}

}
