/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheetsync;

/**
 * Legt fest, ob der {@link SheetSyncNachzieher} nur vorhandene Sync-Blätter aktualisiert oder fehlende
 * Ausgabe-Blätter auch anlegt.
 */
public enum NachziehModus {

	/**
	 * Hintergrund (Webserver, PTM-Online-Live): nur vorhandene Blätter, nur Datenbereich. Fehlende Blätter
	 * bleiben fehlend – ihr Erstaufbau legt Blätter an und kann den Fokus wechseln.
	 */
	NUR_VORHANDENE,

	/**
	 * Ausdrücklicher Export (Verzeichnis-Export, WhatsApp): zusätzlich die {@link SheetSyncAusgabe}n der Einträge
	 * nachziehen, die fehlende Blätter anlegen und abhängige Blätter (z.&nbsp;B. Direktvergleich) neu aufbauen.
	 */
	FEHLENDE_ANLEGEN
}
