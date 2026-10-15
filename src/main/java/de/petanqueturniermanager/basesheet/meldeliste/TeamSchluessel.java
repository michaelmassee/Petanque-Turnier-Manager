/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Wie eine Formel ein Team in der Meldeliste findet. Gespeicherte Teamreferenzen (Setzlisten, Spielrunden, Spielpläne)
 * verwenden immer {@link Uuid}: die UUID bleibt, wenn sich die Teamnummer ändert. {@link AngezeigteNr} verweist auf
 * eine Zelle, die selbst die aktuelle Teamnummer anzeigt (Sieger-Weitergabe im KO-Baum, Teilnehmerlisten,
 * Ranglisten) und deshalb nach einer Nummernänderung von selbst stimmt.
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

	/** Team über einen Ausdruck, der die aktuell angezeigte Teamnummer liefert (z.&nbsp;B. eine Sieger-Zelle). */
	static TeamSchluessel angezeigteNr(String nrAusdruck) {
		return new AngezeigteNr(nrAusdruck);
	}

	/** Nur der explizite Freispiel-Platzhalter 0, als Wert oder Calc-Formel. */
	static boolean istFreispielNrFormel(String nrFormel) {
		return "0".equals(nrFormel) || "=0".equals(nrFormel);
	}

	/** Verlangt die UUID eines echten Teams; nrAdresse dient nur der Fehlermeldung. */
	static TeamSchluessel ausNrFormel(String nrFormel, String nrAdresse) throws GenerateException {
		return TeamUuidFormel.findeUuid(nrFormel).map(TeamSchluessel::uuidLiteral)
				.orElseThrow(() -> new GenerateException(I18n.get("spielplan.team.uuid_fehlt", nrAdresse)));
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

	/** @param ausdruck Formelausdruck oder Zelladresse, die die aktuelle Teamnummer anzeigt */
	record AngezeigteNr(String ausdruck) implements TeamSchluessel {

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
