/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import de.petanqueturniermanager.addins.GlobalImpl;

/**
 * Wie eine Formel ein Team in der Meldeliste findet. Ziel ist überall {@link Uuid}: die UUID bleibt, wenn sich die
 * Teamnummer ändert. {@link Nr} gilt nur noch für Systeme, deren Spielrunden noch nicht umgestellt sind.
 */
public sealed interface TeamSchluessel {

	/**
	 * @param nrBereich Nr-Spalte der Meldeliste in Formel-Notation (z.&nbsp;B. {@code $'Meldeliste'.$A$1:$A$999})
	 * @return Ausdruck für die Zeile des Teams im Bereich, {@code #N/A} wenn es fehlt (wie {@code MATCH})
	 */
	String zeileImBereich(String nrBereich);

	/** @return Ausdruck für die aktuell angezeigte Teamnummer */
	String nrAnzeige(String nrBereich);

	/** Team über die UUID, als Literal oder Formelausdruck (z.&nbsp;B. eines Siegers). */
	static TeamSchluessel uuidLiteral(String uuid) {
		return new Uuid("\"" + uuid + "\"");
	}

	/** Team über eine Teamnummer (Zelladresse oder Zahl) – Übergang für noch nicht umgestellte Systeme. */
	static TeamSchluessel nr(String nrAusdruck) {
		return new Nr(nrAusdruck);
	}

	/** @param ausdruck Formelausdruck, der die UUID als Text liefert */
	record Uuid(String ausdruck) implements TeamSchluessel {

		@Override
		public String zeileImBereich(String nrBereich) {
			return GlobalImpl.FORMAT_PTM_UUID_ZEILE(ausdruck, nrBereich);
		}

		@Override
		public String nrAnzeige(String nrBereich) {
			return GlobalImpl.FORMAT_PTM_UUID_TEAMNR(ausdruck, nrBereich);
		}
	}

	/** @param ausdruck Formelausdruck oder Zelladresse mit der Teamnummer */
	record Nr(String ausdruck) implements TeamSchluessel {

		@Override
		public String zeileImBereich(String nrBereich) {
			return "MATCH(" + ausdruck + ";" + nrBereich + ";0)";
		}

		@Override
		public String nrAnzeige(String nrBereich) {
			return ausdruck;
		}
	}
}
