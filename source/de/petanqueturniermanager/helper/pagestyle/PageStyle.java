package de.petanqueturniermanager.helper.pagestyle;

public enum PageStyle {

	STANDARD("Standard"), SPIELTAG("Spieltag"), PETTURNMNGR("PetTurnMngr");

	private final String name;

	PageStyle(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
