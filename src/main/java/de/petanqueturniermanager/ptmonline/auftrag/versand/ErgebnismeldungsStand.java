/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag.versand;

/**
 * Huckepack jeder Sync-Antwort (Header {@code X-PTM-Result-Reporting}, {@code X-PTM-Open-Result-Reports}): ob Spieler
 * Ergebnisse melden dürfen und wie viele Meldungen auf die Turnierleitung warten. So erfährt PTM von Meldungen, ohne
 * eigens nachzufragen, sobald es ohnehin schreibt.
 *
 * @param aktiv Ergebnismeldung durch Spieler eingeschaltet
 * @param offen Anzahl offener Meldungen
 */
public record ErgebnismeldungsStand(boolean aktiv, int offen) {
}
