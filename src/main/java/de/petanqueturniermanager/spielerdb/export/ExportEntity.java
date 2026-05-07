package de.petanqueturniermanager.spielerdb.export;

/**
 * Logische Einheit, die exportiert werden kann. {@link #SPIELER_LABELS} ist
 * die Junction-Tabelle Spieler↔Label und wird vom Loader automatisch ergänzt,
 * sobald sowohl {@link #SPIELER} als auch {@link #LABELS} im Scope liegen —
 * der Dialog blendet diesen Eintrag daher nicht separat ein. Trotzdem ist die
 * Relation hier first-class, damit Loader, Exporter und ein späterer Importer
 * sie explizit behandeln können.
 */
public enum ExportEntity {
    SPIELER,
    VEREINE,
    LABELS,
    SPIELER_LABELS
}
