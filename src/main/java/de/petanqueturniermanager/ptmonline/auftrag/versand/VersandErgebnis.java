/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag.versand;

import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;

/**
 * Endgültiger Ausgang eines gesendeten Auftrags. Er wird im Dokument-Kontext angewendet (Zuordnungen, Revisionen) und
 * der Auftrag danach aus dem Puffer genommen.
 *
 * @param angenommen {@code true}, wenn PTM-Online den Auftrag ausgeführt hat; {@code false} bei einer fachlichen
 *                   Ablehnung, die eine Wiederholung nicht ändert (z.&nbsp;B. {@code tournament_running})
 */
public record VersandErgebnis(SyncAuftrag auftrag, SyncAntwort antwort, boolean angenommen) {
}
