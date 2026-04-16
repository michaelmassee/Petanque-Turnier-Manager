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
 * Editierbare Zellen erhalten ein Lachsorange-Zebra-Muster als bedingte Formatierung
 * (ISEVEN/ISODD), das sie klar von nicht-editierbaren Zellen (blaues Zebra) unterscheidet.
 * <p>
 * Die Farben sind fest verdrahtet und nicht konfigurierbar.
 * Bestehende bedingte Formatierungen (z. B. Fehlerprüfungen) werden mit {@code append()}
 * erhalten und haben wegen ihres niedrigeren Index höhere Priorität.
 */
public class EditierbaresZelleFormatHelper {

	/** Hintergrundfarbe für gerade Zeilen: helles Orange/Creme. */
	public static final int EDITIERBAR_GERADE_FARBE = 0xFFF3E0;

	/** Hintergrundfarbe für ungerade Zeilen: helles Amber. */
	public static final int EDITIERBAR_UNGERADE_FARBE = 0xFFE0B2;

	private EditierbaresZelleFormatHelper() {
		// Hilfsklasse – kein Instantiieren
	}

	/**
	 * Wendet das Lachsorange-Zebra-Muster als bedingte Formatierung auf den angegebenen
	 * Bereich an. Vorhandene bedingte Formatierungen (z. B. Fehlerprüfungen) bleiben erhalten
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
				.formulaIsEvenRow().style(geradeStyle).applyAndDoReset()
				.formulaIsOddRow().style(ungeradeStyle).applyAndDoReset();
	}
}
