/**
 * Erstellung : 16.04.2026 / Michael Massee
 **/

package de.petanqueturniermanager.helper.sheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.EditierbareZelleHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Zentrale Hilfsklasse für die visuelle Hervorhebung editierbarer Felder.
 * <p>
 * Editierbare Zellen erhalten ein Lachsorange-Zebra-Muster als bedingte Formatierung.
 * Die CF-Formel referenziert {@code PTM.ALG.BOOLEANPROPERTY("editierbareFelderHervorheben")}
 * direkt, sodass Änderungen am Property sofort sichtbar werden (via {@code calculateAll()})
 * ohne das Sheet komplett neu aufzubauen.
 * <p>
 * Bestehende bedingte Formatierungen (z. B. Fehlerprüfungen) werden mit {@code append()}
 * erhalten und haben wegen ihres niedrigeren Index höhere Priorität.
 */
public class EditierbaresZelleFormatHelper {

	/** Hintergrundfarbe für gerade Zeilen: helles Orange/Creme. */
	public static final int EDITIERBAR_GERADE_FARBE = 0xFFF3E0;

	/** Hintergrundfarbe für ungerade Zeilen: helles Amber. */
	public static final int EDITIERBAR_UNGERADE_FARBE = 0xFFE0B2;

	/** Property-Key für "Editierbare Felder hervorheben" – kein Leerzeichen, kein Tippfehlerrisiko. */
	public static final String PROPERTY_KEY = "editierbareFelderHervorheben";

	private EditierbaresZelleFormatHelper() {
		// Hilfsklasse – kein Instantiieren
	}

	/**
	 * Wendet das Lachsorange-Zebra-Muster als bedingte Formatierung auf den angegebenen
	 * Bereich an. Die CF-Formel enthält {@code PTM.ALG.BOOLEANPROPERTY} als Bedingung,
	 * sodass die Darstellung per Konfiguration ohne Sheet-Rebuild abschaltbar ist.
	 * Vorhandene bedingte Formatierungen (z. B. Fehlerprüfungen) bleiben erhalten
	 * und haben Vorrang, da sie mit niedrigerem Index zuerst geprüft werden.
	 *
	 * @param sheet Sheet, auf dem formatiert wird
	 * @param range Zellbereich der editierbaren Felder
	 * @throws GenerateException bei UNO-API-Fehlern
	 */
	public static void anwenden(ISheet sheet, RangePosition range) throws GenerateException {
		var geradeStyle = new EditierbareZelleHintergrundFarbeGeradeStyle(EDITIERBAR_GERADE_FARBE);
		var ungeradeStyle = new EditierbareZelleHintergrundFarbeUnGeradeStyle(EDITIERBAR_UNGERADE_FARBE);
		ConditionalFormatHelper.from(sheet, range).append()
				.formulaIsEvenRowAndBoolProp(PROPERTY_KEY).style(geradeStyle).applyAndDoReset()
				.formulaIsOddRowAndBoolProp(PROPERTY_KEY).style(ungeradeStyle).applyAndDoReset();
	}
}
