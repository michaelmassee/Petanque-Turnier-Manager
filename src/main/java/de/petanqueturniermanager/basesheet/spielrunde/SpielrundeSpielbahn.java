package de.petanqueturniermanager.basesheet.spielrunde;

/**
 * Erstellung 01.04.2024 / Michael Massee
 */

public enum SpielrundeSpielbahn {
	//	KONFIG_PROPERTIES.add(((AuswahlConfigProperty) AuswahlConfigProperty.from(KONFIG_PROP_SPIELRUNDE_SPIELBAHN)
	//			.setDefaultVal("X").setDescription(
	//					"Spalte Spielbahn in Spielrunde.\r\nX=Keine Spalte\r\nL=Leere Spalte (händisch ausfüllen)\r\nN=Durchnummerieren\r\nR=Random"))
	//			.addAuswahl("X", "Keine Spalte").addAuswahl("L", "Leere Spalte")
	//			.addAuswahl("N", "Durchnummerieren (1-n)").addAuswahl("R", "Zufällig vergeben"));

	X("Nur laufende Nr"), L("Leere Spalte"), N("Durchnummerieren (1-n)"), R("Zufällig vergeben");

	String beschreibung;

	SpielrundeSpielbahn(String beschreibung) {
		this.beschreibung = beschreibung;
	}

}
