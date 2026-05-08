/**
 * Spieler-DB-Import. Pipeline:
 * <pre>
 *   Reader → ImportRohdaten → Validator → ValidierteDaten → Importer → DB
 * </pre>
 *
 * <p>Bewusst ohne {@code import}-Paketname (Java-Reserved-Word) — die Klassen
 * spiegeln die Exporter-Strukturen aus {@code spielerdb.export}. IDs aus
 * Import-Dateien sind reine Referenz-IDs (zum Auflösen der Junction); Ziel-IDs
 * werden ausschließlich von der DB vergeben und über {@code ImportIdMapping}
 * propagiert.
 */
@org.jspecify.annotations.NullMarked
package de.petanqueturniermanager.spielerdb.importer;
