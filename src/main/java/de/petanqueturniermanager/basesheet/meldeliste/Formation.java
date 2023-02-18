/**
 * Erstellung : 22.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.basesheet.meldeliste;

public enum Formation {

	TETE(1, "Tête", 1), // Formé
	DOUBLETTE(2, "Doublette", 2), // Formé
	TRIPLETTE(3, "Triplette", 3), // Formé
	SUPERMELEE(4, "SuperMêlée", 1);

	private final String bezeichnung;
	private final int id;
	private final int anzSpielerImTeam;

	private Formation(int id, String bezeichnung, int anzSpielerImTeam) {
		this.id = id;
		this.bezeichnung = bezeichnung;
		this.anzSpielerImTeam = anzSpielerImTeam;
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

	public static Formation findById(int id) {
		for (Formation formation : values()) {
			if (formation.getId() == id) {
				return formation;
			}
		}
		return null;
	}

	public int getAnzSpielerImTeam() {
		return anzSpielerImTeam;
	}

}
