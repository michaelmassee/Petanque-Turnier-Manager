/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog;

/**
 * Konfig-Property für den mehrzeiligen Spielrunde-Footer.
 *
 * <p>Im Gegensatz zu {@link HeaderFooterConfigProperty} gibt es hier keine
 * {@code nachSpeichernAktion}: der Footer-Text wird erst beim nächsten Sheet-Aufbau
 * (z.B. "Nächste Spielrunde") in die jeweilige Turniertabelle geschrieben, siehe
 * {@code de.petanqueturniermanager.basesheet.spielrunde.SpielrundeFooterHelper}.
 */
public class SpielrundeFooterConfigProperty extends ConfigProperty<String> {

	protected SpielrundeFooterConfigProperty(String key) {
		super(ConfigPropertyType.STRING, key);
	}

	public static SpielrundeFooterConfigProperty from(String key) {
		return (SpielrundeFooterConfigProperty) new SpielrundeFooterConfigProperty(key).setDefaultVal("");
	}

}
