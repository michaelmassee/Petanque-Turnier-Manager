/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.basesheet.meldeliste;

public enum Formation {

	TETE(1, "Tête"), // Formé
	DOUBLETTE(2, "Doublette"), // Formé
	TRIPLETTE(3, "Triplette"), // Formé
	MELEE(4, "Mêlée"); // wenn die Teamgroese nicht festeht, wird z.b. bei Supermelee gebraucht

	private final String bezeichnung;
	private final int id;

	private Formation(int id, String bezeichnung) {
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

	public static Formation findById(int id) {
		for (Formation formation : values()) {
			if (formation.getId() == id) {
				return formation;
			}
		}
		return null;
	}

}
