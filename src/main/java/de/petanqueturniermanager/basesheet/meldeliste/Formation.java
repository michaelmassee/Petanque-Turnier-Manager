/**
 * Erstellung : 22.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.basesheet.meldeliste;

import de.petanqueturniermanager.helper.i18n.I18n;

public enum Formation {

	TETE(1, "enum.formation.tete", 1), // Formé
	DOUBLETTE(2, "enum.formation.doublette", 2), // Formé
	TRIPLETTE(3, "enum.formation.triplette", 3), // Formé
	MELEE(4, "enum.formation.melee", 1); // wenn die Teamgroese nicht festeht, wird z.b. bei Supermelee gebraucht

	private final String i18nKey;
	private final int id;
	private final int anzSpieler;

	private Formation(int id, String i18nKey, int anzSpieler) {
		this.id = id;
		this.i18nKey = i18nKey;
		this.anzSpieler = anzSpieler;
	}

	public String getBezeichnung() {
		return I18n.get(i18nKey);
	}

	@Override
	public String toString() {
		return getBezeichnung();
	}

	public int getId() {
		return this.id;
	}

	public static Formation findById(int id) {
		for (Formation formation : values()) {
			if (formation.getId() == id) {
				return formation;
			}
		}
		return null;
	}

	public int getAnzSpieler() {
		return anzSpieler;
	}

}
