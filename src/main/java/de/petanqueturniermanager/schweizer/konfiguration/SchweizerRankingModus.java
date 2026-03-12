package de.petanqueturniermanager.schweizer.konfiguration;

/**
 * Ranking-Modus für das Schweizer Turniersystem.
 * <p>
 * Bestimmt, welche Kriterien für die Rangliste und die Sortierung
 * innerhalb der Sieggruppen beim Pairing verwendet werden.
 */
public enum SchweizerRankingModus {
    /** Standard: Siege → BHZ → FBHZ → Punktediff → Punkte+ */
    MIT_BUCHHOLZ,
    /** Vereinfacht (häufig bei kleinen Turnieren): Siege → Punktediff → Punkte+ */
    OHNE_BUCHHOLZ
}