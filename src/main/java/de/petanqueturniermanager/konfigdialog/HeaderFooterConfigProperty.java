/*
 * Erstellung 31.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog;

import de.petanqueturniermanager.basesheet.konfiguration.SeitenstileDebouncer;

/**
 * Konfig-Property für eine Kopf-/Fußzeile.
 *
 * <p>Jede Instanz hängt automatisch eine {@code mitNachSpeichernAktion} an, die
 * den {@link SeitenstileDebouncer} triggert: wenn der Anwender den Wert in
 * Sidebar oder Dialog ändert, werden die PageStyles des Dokuments (debounced)
 * aktualisiert, sodass die Änderung sofort in allen Sheets wirkt.
 */
public class HeaderFooterConfigProperty extends ConfigProperty<String> {

	protected HeaderFooterConfigProperty(String key) {
		super(ConfigPropertyType.STRING, key);
	}

	public static HeaderFooterConfigProperty from(String key) {
		return (HeaderFooterConfigProperty) new HeaderFooterConfigProperty(key)
				.setDefaultVal("")
				.mitNachSpeichernAktion(SeitenstileDebouncer::aktualisiereSeitenstileDebounced);
	}

}
