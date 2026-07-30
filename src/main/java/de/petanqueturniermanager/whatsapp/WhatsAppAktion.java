/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.whatsapp;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.I18n;

public enum WhatsAppAktion {
	TEILNEHMER("TEILNEHMER", "whatsapp.aktion.teilnehmer"),
	SPIELRUNDE("AKTUELLE_SPIELRUNDE", "whatsapp.aktion.spielrunde"),
	RANGLISTE("RANGLISTE", "whatsapp.aktion.rangliste");

	private final String resolverKey;
	private final String i18nKey;

	WhatsAppAktion(String resolverKey, String i18nKey) {
		this.resolverKey = resolverKey;
		this.i18nKey = i18nKey;
	}

	public String resolverKey() {
		return resolverKey;
	}

	public String titel(TurnierSystem ts) {
		if (this == SPIELRUNDE) {
			return switch (ts) {
			case JGJ, LIGA, TRIPTETE -> I18n.get("whatsapp.aktion.spielplan");
			case KO -> I18n.get("whatsapp.aktion.turnierbaum");
			default -> I18n.get(i18nKey);
			};
		}
		return I18n.get(i18nKey);
	}
}
