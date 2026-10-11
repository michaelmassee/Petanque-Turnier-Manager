/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Die UUID eines Teams steht als Literal in der Formel seiner Zelle, z.&nbsp;B. in der Nr-Spalte der Meldeliste
 * {@code =PTM.ALG.TNRUUID(7;"<uuid>")}. Diese Klasse findet sie im Formeltext wieder.
 * <p>
 * Gesucht wird nach dem UUID-Muster, nicht nach dem Funktionsnamen: {@code getFormula} liefert Add-in-Funktionen
 * je nach Kontext unter ihrem programmatischen statt dem angezeigten Namen.
 */
public final class TeamUuidFormel {

	private static final Pattern UUID_MUSTER = Pattern.compile(
			"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

	private TeamUuidFormel() {
	}

	/**
	 * @param formel Formeltext einer Zelle, {@code null} oder leer bei leerer Zelle
	 * @return die erste UUID im Formeltext, kleingeschrieben
	 */
	public static Optional<String> findeUuid(String formel) {
		if (formel == null || formel.isEmpty()) {
			return Optional.empty();
		}
		Matcher treffer = UUID_MUSTER.matcher(formel);
		return treffer.find() ? Optional.of(treffer.group().toLowerCase(Locale.ROOT)) : Optional.empty();
	}

	/**
	 * Sucht die Zeile, deren Formel (erste Spalte des Bereichs) die UUID trägt – Gegenstück zu
	 * {@code MATCH(nr;bereich;0)}, aber über die UUID statt über die änderbare Teamnummer.
	 *
	 * @param uuid    gesuchte UUID
	 * @param formeln Formel-Array des Bereichs, wie {@code XCellRangeFormula.getFormulaArray()} es liefert
	 * @return 1-basierter Zeilenindex im Bereich
	 */
	public static OptionalInt zeileImBereich(String uuid, String[][] formeln) {
		if (!istUuid(uuid) || formeln == null) {
			return OptionalInt.empty();
		}
		String gesucht = uuid.toLowerCase(Locale.ROOT);
		for (int zeile = 0; zeile < formeln.length; zeile++) {
			String[] zellen = formeln[zeile];
			if (zellen != null && zellen.length > 0
					&& findeUuid(zellen[0]).filter(gesucht::equals).isPresent()) {
				return OptionalInt.of(zeile + 1);
			}
		}
		return OptionalInt.empty();
	}

	/** @return {@code true}, wenn {@code wert} genau eine UUID ist (Groß-/Kleinschreibung egal) */
	public static boolean istUuid(String wert) {
		return wert != null && UUID_MUSTER.matcher(wert).matches();
	}
}
