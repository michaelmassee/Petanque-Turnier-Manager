/**
* Erstellung : 22.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.meldeliste;

public enum Formation {

	TETE(1, "Tête"), //
	DOUBLETTE(2, "Doublette"), //
	TRIPLETTE(3, "Triplette"), //
	SUPERMELEE(4, "Supermêlée"); // das ist keine formation, mischung aus Doublette und Triplette

	private final String bezeichnung;
	private final int id;

	private Formation(int id, String bezeichnung) {
		this.id = id;
		this.bezeichnung = bezeichnung;
	}

	public String getBezeichnung() {
		return bezeichnung;
	}

	@Override
	public String toString() {
		return this.bezeichnung;
	}

	public int getId() {
		return id;
	}

	static Formation findById(int id) {
		for (Formation formation : values()) {
			if (formation.getId() == id) {
				return formation;
			}
		}
		return null;
	}

}
