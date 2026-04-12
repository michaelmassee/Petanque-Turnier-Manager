/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import java.util.List;

/**
 * Beschreibt die Spielpaarungen einer Teilgruppe innerhalb einer Kaskadenrunde.<br>
 * <br>
 * Jede Gruppe entsteht aus einer Folge von Sieger-/Verlierer-Entscheidungen
 * vorangegangener Kaskadenrunden (kodiert im {@code pfad}). In der ersten
 * Kaskadenrunde gibt es genau eine Gruppe mit leerem Pfad (alle Teams).<br>
 * <br>
 * <b>Freilos-Konvention:</b> Bei ungerader {@code anzTeams} erhält das Team
 * an letzter Position (Position {@code anzTeams}) ein Freilos. Es erscheint
 * <em>nicht</em> in {@code spielPaare}, sondern wird durch die Größenberechnung
 * in die Verlierer-Gruppe übernommen.<br>
 * <br>
 * <b>Invariante:</b> {@code anzTeams == 2 * spielPaare.size() + anzFreilose}
 *
 * @param pfad        S/V-Pfad der Gruppe (leer für Startrunde, z. B. {@code "S"}, {@code "VS"})
 * @param anzTeams    Anzahl Teams in dieser Gruppe (&ge; 1)
 * @param spielPaare  {@code floor(anzTeams / 2)} Spielpaarungen, positionen 1-basiert
 * @param anzFreilose Anzahl Teams mit Freilos (0 oder 1); Freilos-Team → Verlierer-Seite
 *
 * @author Michael Massee
 * @see KaskadenKoSpielPaar
 * @see KaskadenKoRunde
 */
public record KaskadenKoGruppenRunde(
        String pfad,
        int anzTeams,
        List<KaskadenKoSpielPaar> spielPaare,
        int anzFreilose) {
}
