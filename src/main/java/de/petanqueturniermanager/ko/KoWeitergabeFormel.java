/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.helper.position.Position;

/**
 * Löst die Team-UUID einer weitergereichten Zelle im K.-o.-Baum auf (Sieger, Verlierer fürs Spiel um Platz 3,
 * Cadrage-Sieger, Turniersieger). Diese Zellen tragen keine UUID, sondern reichen die Anzeige ihres Quellspiels weiter:
 * {@code IF(<beide Ergebnisse da>;IF(a>b;teamA;IF(a<b;teamB;"?"));"")} (beim Verlierer mit vertauschten
 * Vergleichen). Die Auflösung wertet den Vergleich mit den eingetragenen Punkten aus und folgt der gewählten Zelle,
 * bis eine Formel mit UUID-Literal erreicht ist (Runde 1 bzw. Cadrage).
 * <p>
 * Anders als die Zuordnung über die angezeigte Nummer oder den Namen ist das auch bei gleichnamigen Teams eindeutig.
 * UNO-frei; der Zellzugriff kommt über {@link Zellen}.
 */
public final class KoWeitergabeFormel {

	/** Mehr Ebenen als Runden plus Cadrage hat kein Baum; schützt vor Zirkelbezügen in von Hand geänderten Zellen. */
	private static final int MAX_TIEFE = 16;

	private static final String REF = "\\$?([A-Z]{1,3})\\$?(\\d+)";
	private static final String VERGLEICH = "\\s*" + REF + "\\s*([<>])\\s*" + REF + "\\s*";
	private static final Pattern WEITERGABE = Pattern.compile("IF\\(" + VERGLEICH + ";\\s*" + REF + "\\s*;\\s*IF\\("
			+ VERGLEICH + ";\\s*" + REF + "\\s*;", Pattern.CASE_INSENSITIVE);

	private KoWeitergabeFormel() {
	}

	/** Zellzugriff mit 0-basierten Blattkoordinaten. */
	public interface Zellen {

		/** @return Formeltext der Zelle, {@code null} oder leer ohne Formel */
		String formel(Position position);

		/** @return eingetragene Punkte, leer ohne Zahl */
		Optional<Integer> punkte(Position position);
	}

	/**
	 * @param position Team-Zelle im Turnierbaum
	 * @return UUID des Teams, das die Zelle gerade anzeigt; leer ohne Ergebnis, bei Gleichstand oder wenn die Formel
	 *         keiner bekannten Form folgt
	 */
	public static Optional<String> uuid(Zellen zellen, Position position) {
		Position aktuell = position;
		for (int tiefe = 0; tiefe < MAX_TIEFE; tiefe++) {
			String formel = zellen.formel(aktuell);
			Optional<String> uuid = TeamUuidFormel.findeUuid(formel);
			if (uuid.isPresent()) {
				return uuid;
			}
			Optional<Position> quelle = quelle(zellen, formel);
			if (quelle.isEmpty()) {
				return Optional.empty();
			}
			aktuell = quelle.get();
		}
		return Optional.empty();
	}

	/** @return die Zelle, deren Anzeige die Weitergabe-Formel nach den eingetragenen Punkten zeigt */
	static Optional<Position> quelle(Zellen zellen, String formel) {
		if (formel == null || formel.isEmpty()) {
			return Optional.empty();
		}
		Matcher treffer = WEITERGABE.matcher(formel);
		if (!treffer.find()) {
			return Optional.empty();
		}
		Optional<Boolean> ersterVergleich = vergleiche(zellen, treffer, 1);
		if (ersterVergleich.isEmpty()) {
			return Optional.empty();
		}
		if (ersterVergleich.get()) {
			return Optional.of(position(treffer, 6));
		}
		return vergleiche(zellen, treffer, 8).filter(Boolean::booleanValue).map(wahr -> position(treffer, 13));
	}

	/** Vergleich ab Gruppe {@code erste}: Spalte, Zeile, Operator, Spalte, Zeile. */
	private static Optional<Boolean> vergleiche(Zellen zellen, Matcher treffer, int erste) {
		Optional<Integer> links = zellen.punkte(position(treffer, erste));
		Optional<Integer> rechts = zellen.punkte(position(treffer, erste + 3));
		if (links.isEmpty() || rechts.isEmpty()) {
			return Optional.empty();
		}
		boolean groesser = ">".equals(treffer.group(erste + 2));
		return Optional.of(groesser ? links.get() > rechts.get() : links.get() < rechts.get());
	}

	private static Position position(Matcher treffer, int spaltenGruppe) {
		return Position.from(treffer.group(spaltenGruppe).toUpperCase(Locale.ROOT),
				Integer.parseInt(treffer.group(spaltenGruppe + 1)) - 1);
	}
}
