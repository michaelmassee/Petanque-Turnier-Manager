/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import de.petanqueturniermanager.helper.position.Position;

/**
 * Wo das Ergebnis einer Partie im Turnierdokument steht: die Zelle mit den Punkten von Seite A und die mit denen von
 * Seite B. In zeilenbasierten Spielrunden liegen sie nebeneinander („Punkte A | Punkte B“), im Turnierbaum
 * untereinander. Das Blatt wird wie beim Lesen über seinen Metadaten-Schlüssel gesucht, bei alten Dokumenten über den
 * Namen. Alle Spalten und Zeilen 0-basiert.
 *
 * @param blattSchluessel Metadaten-Schlüssel des Blatts
 * @param blattName       Blattname für Dokumente ohne Metadaten
 */
public record ErgebnisOrt(String blattSchluessel, String blattName, int spalteA, int zeileA, int spalteB,
        int zeileB) {

    /** Zeilenbasierte Spielrunde: die Punkte von Seite B stehen rechts neben denen von Seite A. */
    public static ErgebnisOrt inZeile(String blattSchluessel, String blattName, int zeile, int spaltePunkteA) {
        return new ErgebnisOrt(blattSchluessel, blattName, spaltePunkteA, zeile, spaltePunkteA + 1, zeile);
    }

    /** Turnierbaum: je Seite eine eigene Ergebnis-Zelle. */
    public static ErgebnisOrt inZellen(String blattSchluessel, String blattName, Position punkteA,
            Position punkteB) {
        return new ErgebnisOrt(blattSchluessel, blattName, punkteA.getSpalte(), punkteA.getZeile(),
                punkteB.getSpalte(), punkteB.getZeile());
    }

    public Position punkteA() {
        return Position.from(spalteA, zeileA);
    }

    public Position punkteB() {
        return Position.from(spalteB, zeileB);
    }
}
