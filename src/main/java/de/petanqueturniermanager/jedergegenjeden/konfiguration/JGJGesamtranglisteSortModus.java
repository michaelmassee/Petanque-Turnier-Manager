package de.petanqueturniermanager.jedergegenjeden.konfiguration;

/**
 * Sortiermodus der gruppenübergreifenden JGJ-Gesamtrangliste.
 *
 * <ul>
 *   <li>{@link #GRUPPENPLATZ} – „Snake": erst alle Gruppensieger, dann alle
 *       Zweitplatzierten usw. (1A,1B,…,2A,2B,…). Nutzt die je-Gruppe-Sortierung.</li>
 *   <li>{@link #ABSOLUT} – alle Teams gemeinsam nach denselben Kriterien wie die
 *       Einzel-Rangliste (Siege↓ → Spielpunkte-Diff↓ → Spielpunkte+↓).</li>
 * </ul>
 */
public enum JGJGesamtranglisteSortModus {
	GRUPPENPLATZ,
	ABSOLUT;
}
