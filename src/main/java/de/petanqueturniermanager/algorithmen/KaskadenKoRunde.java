/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import java.util.List;

/**
 * Eine einzelne Kaskadenrunde im Kaskaden-KO-System.<br>
 * <br>
 * Runde {@code r} enthält genau {@code 2^(r-1)} Gruppen:
 * <ul>
 *   <li>Runde 1: 1 Gruppe (alle Teams)</li>
 *   <li>Runde 2: 2 Gruppen (Sieger- und Verlierer-Gruppe aus Runde 1)</li>
 *   <li>Runde 3: 4 Gruppen usw.</li>
 * </ul>
 *
 * @param rundenNr     Rundennummer (1-basiert)
 * @param gruppenRunden Gruppen dieser Runde in Pfad-Reihenfolge (lexikografisch S &lt; V)
 *
 * @author Michael Massee
 * @see KaskadenKoGruppenRunde
 * @see KaskadenKoRundenPlan
 */
public record KaskadenKoRunde(int rundenNr, List<KaskadenKoGruppenRunde> gruppenRunden) {
}
