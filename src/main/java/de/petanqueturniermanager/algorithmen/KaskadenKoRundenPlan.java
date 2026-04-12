/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import java.util.List;

/**
 * Der vollständige Rundenplan für ein Kaskaden-KO-Turnier.<br>
 * <br>
 * Enthält:
 * <ol>
 *   <li>Die {@code kaskadenStufen} Kaskadenrunden (wer spielt gegen wen, und
 *       welche Gruppen entstehen dabei)</li>
 *   <li>Die resultierenden Endfelder (A, B, C, D …) mit Cadrage-Informationen
 *       für die anschließende KO-Phase</li>
 * </ol>
 *
 * @param gesamtTeams    Gesamtanzahl Teams
 * @param kaskadenStufen Anzahl Kaskadenrunden (1–3 bei Nutzung mit {@link KaskadenKoFeldRechner})
 * @param kaskadeRunden  die {@code kaskadenStufen} Kaskadenrunden in aufsteigender Reihenfolge
 * @param felder         Endfelder nach abgeschlossener Kaskadierung (identisch mit
 *                       {@link KaskadenKoFeldRechner#berechne(int, int)})
 *
 * @author Michael Massee
 * @see KaskadenKoRundenPlaner
 * @see KaskadenKoRunde
 * @see KaskadenKoFeldInfo
 */
public record KaskadenKoRundenPlan(
        int gesamtTeams,
        int kaskadenStufen,
        List<KaskadenKoRunde> kaskadeRunden,
        List<KaskadenKoFeldInfo> felder) {
}
