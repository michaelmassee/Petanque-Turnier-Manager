package de.petanqueturniermanager.meldeliste;

/*
* SpielrundeGespielt.java
*
* Erstellung     : 31.08.2017 / massee
*
*/

public enum SpielrundeGespielt {
	NEIN(-1), JA(1), AUSGESETZT(2), UNDEFINIERT(99);

	private final int id;

	private SpielrundeGespielt(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	static SpielrundeGespielt findById(int id) {
		for (SpielrundeGespielt spielrundegespielt : values()) {
			if (spielrundegespielt.getId() == id) {
				return spielrundegespielt;
			}
		}
		return UNDEFINIERT;
	}
}
