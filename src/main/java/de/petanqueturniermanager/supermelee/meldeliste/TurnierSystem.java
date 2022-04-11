/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

public enum TurnierSystem {

	KEIN(0, "Kein"), // wenn nur der runner gebraucht wird
	SUPERMELEE(1, "Supermêlée"), LIGA(2, "Liga"), MAASTRICHTER(3, "Maastricht"), SCHWEIZER(4, "Schweizer");

	private final String bezeichnung;
	private final int id;

	private TurnierSystem(int id, String bezeichnung) {
		this.id = id;
		this.bezeichnung = bezeichnung;
	}

	public String getBezeichnung() {
		return bezeichnung;
	}

	@Override
	public String toString() {
		return bezeichnung;
	}

	public int getId() {
		return id;
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
