/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

/**
 * Wo das Ergebnis einer Partie im Turnierdokument steht: zwei nebeneinander liegende Zellen „Punkte A | Punkte B“ in
 * einer Zeile einer zeilenbasierten Spielrunde. Das Blatt wird wie beim Lesen über seinen Metadaten-Schlüssel gesucht,
 * bei alten Dokumenten über den Namen.
 *
 * @param blattSchluessel Metadaten-Schlüssel des Blatts
 * @param blattName       Blattname für Dokumente ohne Metadaten
 * @param zeile           Zeile der Partie (0-basiert)
 * @param spaltePunkteA   Spalte der Punkte von Seite A (0-basiert); die Punkte von Seite B stehen rechts daneben
 */
public record ErgebnisOrt(String blattSchluessel, String blattName, int zeile, int spaltePunkteA) {

    public int spaltePunkteB() {
        return spaltePunkteA + 1;
    }
}
