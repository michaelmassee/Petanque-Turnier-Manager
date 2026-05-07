package de.petanqueturniermanager.spielerdb.importer;

/**
 * UI-Preset für die Konfliktbehandlung. Wird vom Importer auf die internen
 * Strategien je Entity gemappt — der Anwender sieht nur diese drei Optionen,
 * keine drei Dropdowns.
 *
 * <p>Mapping:
 * <pre>
 *   NUR_NEUE         → Spieler/Vereine/Labels: SKIP (gematcht), Insert (neu)
 *   AKTUALISIEREN    → Spieler: UPDATE_MERGE; Vereine/Labels: UPDATE
 *   DUPLIKATE_SEPARAT→ Spieler: INSERT_NEU;   Vereine/Labels: SKIP
 * </pre>
 *
 * <p>„Vereine/Labels: INSERT_NEU" gibt es bewusst nicht — Identität läuft
 * über den Namen, ein „BC Linden (Import) (Import)"-Suffix-Hack erzeugt
 * Datenmüll.
 */
public enum ImportModus {
    NUR_NEUE,
    AKTUALISIEREN,
    DUPLIKATE_SEPARAT
}
