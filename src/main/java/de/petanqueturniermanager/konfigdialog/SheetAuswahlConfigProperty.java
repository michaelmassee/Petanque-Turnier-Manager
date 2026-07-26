/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog;

/**
 * Markiert eine STRING-Property, deren Wert im Konfigurationsdialog nicht als Freitext,
 * sondern als ComboBox mit den Namen der aktuell im Dokument vorhandenen Tabellenblätter
 * angeboten wird. Die Auswahlliste wird beim Öffnen des Dialogs dynamisch aus dem
 * {@code WorkingSpreadsheet} ermittelt ({@link de.petanqueturniermanager.konfigdialog.properties.element.SheetAuswahlConfigElement}),
 * anders als bei {@link AuswahlConfigProperty} mit ihrer statisch fest verdrahteten Liste.
 */
public class SheetAuswahlConfigProperty extends ConfigProperty<String> {

	protected SheetAuswahlConfigProperty(String key) {
		super(ConfigPropertyType.STRING, key);
	}

	public static SheetAuswahlConfigProperty from(String key) {
		return new SheetAuswahlConfigProperty(key);
	}

}
