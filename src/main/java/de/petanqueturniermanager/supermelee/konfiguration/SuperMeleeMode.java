/**
 * Erstellung 04.07.2019 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.konfiguration;

/**
 * @author Michael Massee
 *
 */
public enum SuperMeleeMode {
	Triplette("T"), // aufüllen mit Doublettes
	Doublette("D"); // aufüllen mit Triplettes

	private final String key;

	SuperMeleeMode(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

}
