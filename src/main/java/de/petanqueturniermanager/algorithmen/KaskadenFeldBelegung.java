/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import java.util.List;

/**
 * Ergebnis der Kaskadenrunden für ein einzelnes Endfeld:<br>
 * kombiniert die Feldinformation ({@link KaskadenKoFeldInfo}) mit der geordneten
 * Liste der dort zugewiesenen Teamnummern (in Setzreihenfolge, 1-basierter Index).<br>
 * <br>
 * Dieses Record ersetzt den fehleranfälligen {@code Map<String, List<Integer>>}-Ansatz:
 * Der Bezeichner ("A", "B" …) und alle Cadrage-Kennwerte sind direkt über
 * {@code feld()} abrufbar, ohne String-Schlüssel.
 *
 * @param feld     Feldinformation inkl. Bezeichner, Pfad, Cadrage-Daten
 * @param teamNrs  Teamnummern in Setzreihenfolge (Index 0 = Platz 1 im Feld)
 *
 * @author Michael Massee
 */
public record KaskadenFeldBelegung(KaskadenKoFeldInfo feld, List<Integer> teamNrs) {

    /**
     * Kurzform für {@code feld().bezeichner()}: liefert den Feldbezeichner
     * (z. B. {@code "A"}, {@code "B"} …).
     *
     * @return alphabetischer Feldname
     */
    public String bezeichner() {
        return feld.bezeichner();
    }
}
