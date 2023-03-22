package de.petanqueturniermanager.supermelee.konfiguration;

/**
 * Erstellung 20.02.2023 / Michael Massee
 */

public enum SuprMleEndranglisteSortMode {

	//@formatter:off
	Siege("S"),
	SiegeTage("T");
	//@formatter:on

	private final String key;

	SuprMleEndranglisteSortMode(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

}
