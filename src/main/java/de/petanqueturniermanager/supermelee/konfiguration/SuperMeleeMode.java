/**
 * Erstellung 04.07.2019 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.konfiguration;

/**
 * @author Michael Massee
 *
 */
public enum SuperMeleeMode {
	//@formatter:off
	Triplette("T"), // aufüllen mit Doublettes
	Doublette("D"); // aufüllen mit Triplettes
	//@formatter:on

	private final String key;

	SuperMeleeMode(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

}
