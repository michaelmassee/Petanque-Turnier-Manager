/*
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.basesheet.meldeliste.TeilnehmerListeSortModus;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;

/**
 * @author Michael Massee
 *
 */
public interface IPropertiesSpalte {

	// ---------------------------------------------------------------
	// Tab-Farben (konfigurierbar, Defaults aus SheetTabFarben)
	// ---------------------------------------------------------------

	default int getMeldelisteTabFarbe() {
		return SheetTabFarben.MELDELISTE;
	}

	default int getTeilnehmerTabFarbe() {
		return SheetTabFarben.TEILNEHMER;
	}

	default int getSpielrundeTabFarbe() {
		return SheetTabFarben.SPIELRUNDE;
	}

	default int getRanglisteTabFarbe() {
		return SheetTabFarben.RANGLISTE;
	}

	default int getDirektvergleichTabFarbe() {
		return SheetTabFarben.DIREKTVERGLEICH;
	}

	// ---------------------------------------------------------------
	// Zellhintergrundfarben
	// ---------------------------------------------------------------

	Integer getMeldeListeHintergrundFarbeGerade();

	MeldungenHintergrundFarbeGeradeStyle getMeldeListeHintergrundFarbeGeradeStyle();

	Integer getMeldeListeHintergrundFarbeUnGerade();

	MeldungenHintergrundFarbeUnGeradeStyle getMeldeListeHintergrundFarbeUnGeradeStyle();

	Integer getMeldeListeHeaderFarbe();

	Integer getRanglisteHintergrundFarbeGerade();

	Integer getRanglisteHintergrundFarbeUnGerade();

	Integer getRanglisteHeaderFarbe();

	String getFusszeileLinks();

	String getFusszeileMitte();

	Integer getMaxAnzTeilnehmerInSpalte();

	String getTurnierlogoUrl();

	default boolean isEditierbareFelder() {
		return true;
	}

	default boolean isMeldelisteExportieren() {
		return false;
	}

	default boolean isSpielrundenExportieren() {
		return false;
	}

	default boolean isTeilnehmerlisteExportieren() {
		return false;
	}

	default boolean isAbschlussSheetExportieren() {
		return false;
	}

	default String getAbschlussSheetName() {
		return "";
	}

	/**
	 * Sortierreihenfolge der Checkin-Liste. Default {@link TeilnehmerListeSortModus#NAME}.
	 * Verwendet dieselbe Modi-Enum wie die Teilnehmerliste.
	 */
	default TeilnehmerListeSortModus getCheckinListeSortModus() {
		return TeilnehmerListeSortModus.NAME;
	}

	/**
	 * Sortierreihenfolge der Teilnehmerliste. Default {@link TeilnehmerListeSortModus#NAME}.
	 */
	default TeilnehmerListeSortModus getTeilnehmerListeSortModus() {
		return TeilnehmerListeSortModus.NAME;
	}

	/**
	 * Sortierreihenfolge der Meldeliste. Default {@link TeilnehmerListeSortModus#NUMMER} – vor
	 * Einführung dieser Option wurde die Meldeliste beim Aktualisieren stets nach Name sortiert,
	 * der Default NUMMER ist somit eine bewusste Verhaltensänderung.
	 */
	default TeilnehmerListeSortModus getMeldelisteSortModus() {
		return TeilnehmerListeSortModus.NUMMER;
	}

	/**
	 * Ob die Melee-Anmeldung (Vorab-Liste loser Einzelspieler) eingeschaltet ist. Default
	 * {@code false}. Nur Turniersysteme mit konfigurierbarer Meldeliste-Formation registrieren die
	 * zugehörige Property, siehe
	 * {@code BasePropertiesSpalte#addMeleeAnmeldungProp(java.util.List)}.
	 */
	default boolean isMeleeAnmeldungAktiv() {
		return false;
	}

	/**
	 * Team-Mix-Modus für die Übernahme der Melee-Anmeldungen in die Meldeliste.
	 * Default {@link SuperMeleeMode#Triplette} (Triplette bevorzugt, Rest mit Doubletten aufgefüllt).
	 */
	default SuperMeleeMode getMeleeTeamModus() {
		return SuperMeleeMode.Triplette;
	}

}
