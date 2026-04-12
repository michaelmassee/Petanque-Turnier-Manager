/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import java.util.List;

/**
 * Strategie zur Erzeugung der Spielpaarungen für eine Gruppe im Kaskaden-KO-System.<br>
 * <br>
 * Die Implementierung erhält vollständigen Kontext über die aktuelle Gruppe
 * (Pfad, Rundennummer, Teamanzahl) und gibt genau {@code floor(anzTeams / 2)}
 * {@link KaskadenKoSpielPaar}-Objekte zurück.<br>
 * <br>
 * <b>Kontrakt der Implementierung:</b>
 * <ul>
 *   <li>Rückgabeliste hat genau {@code floor(anzTeams / 2)} Einträge</li>
 *   <li>Alle Positionen liegen im Bereich {@code [1, anzTeams - (anzTeams % 2)]};
 *       Position {@code anzTeams} ist das Freilos-Team (ungerade Anzahl) und
 *       darf nicht in Paarungen erscheinen</li>
 *   <li>Keine doppelten Positionen</li>
 * </ul>
 * Der {@link KaskadenKoRundenPlaner} prüft nach der Erzeugung defensiv die
 * Anzahl der zurückgegebenen Paarungen.
 *
 * @author Michael Massee
 * @see KaskadenKoRundenPlaner#SEQUENZIELL
 * @see KaskadenKoRundenPlaner
 */
@FunctionalInterface
public interface KaskadenKoPaarungsStrategie {

    /**
     * Erzeugt die Spielpaarungen für eine Kaskadengruppe.
     *
     * @param pfad      S/V-Pfad der aktuellen Gruppe (z. B. {@code ""}, {@code "S"}, {@code "VS"})
     * @param rundenNr  aktuelle Kaskadenrundennummer (1-basiert)
     * @param anzTeams  Teamanzahl in dieser Gruppe (&ge; 1)
     * @return unveränderliche Liste mit {@code floor(anzTeams / 2)} Spielpaaren
     */
    List<KaskadenKoSpielPaar> generiere(String pfad, int rundenNr, int anzTeams);
}
