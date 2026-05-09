/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.konfiguration;

import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;

/**
 * Konfiguration für den KO-Turnierbaum.
 * Wird von KoKonfigurationSheet und MaastrichterKonfigurationSheet implementiert,
 * damit KoTurnierbaumSheet keine direkte Abhängigkeit auf einen konkreten Sheet-Typ hat.
 */
public interface IKoBracketKonfiguration {
	int getGruppenGroesse();
	KoSpielbaumTeamAnzeige getSpielbaumTeamAnzeige();
	SpielrundeSpielbahn getSpielbaumSpielbahn();
	boolean isSpielbaumSpielUmPlatz3();

	// Meldeliste-Struktur (für VLOOKUP-Formeln in den Bracket-Zellen)
	boolean isMeldeListeTeamnameAnzeigen();
	boolean isMeldeListeVereinsnameAnzeigen();
	Formation getMeldeListeFormation();

	// Tab-Farbe KO-Turnierbaum (konfigurierbar, Default aus SheetTabFarben)
	default int getKoTurnierbaumTabFarbe() {
		return SheetTabFarben.KO_TURNIERBAUM;
	}

	// Zellfarben: Default-Werte entsprechen KoPropertiesSpalte-Defaults
	default int getTurnierbaumHeaderFarbe()      { return 0x2544DD; }
	default int getTurnierbaumTeamAFarbe()       { return 0xDCEEFA; }
	default int getTurnierbaumTeamBFarbe()       { return 0xF0F7FF; }
	default int getTurnierbaumSiegerFarbe()      { return 0xFFD700; }
	default int getTurnierbaumBahnFarbe()        { return 0xEEEEEE; }
	default int getTurnierbaumDrittePlatzFarbe() { return 0xCD7F32; }
}
