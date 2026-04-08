package de.petanqueturniermanager.webserver;

/**
 * Innerer Knoten im Split-Baum: teilt den Anzeigebereich in zwei Hälften.
 * <p>
 * JSON-Darstellung:
 * {@code {"richtung":"H","groesse":50,"links":{...},"rechts":{...}}}
 *
 * @param richtung  Teilungsrichtung: {@code "H"} = horizontal (links|rechts),
 *                  {@code "V"} = vertikal (oben|unten)
 * @param groesse   Größe des linken/oberen Knotens in Prozent (1–99)
 * @param links     linker oder oberer Kindknoten
 * @param rechts    rechter oder unterer Kindknoten
 */
public record SplitTeilung(
        String richtung,
        int groesse,
        SplitKnoten links,
        SplitKnoten rechts) implements SplitKnoten {

    /** Horizontale Teilung mit gleicher Größe (50%). */
    public static SplitTeilung horizontal(SplitKnoten links, SplitKnoten rechts) {
        return new SplitTeilung("H", 50, links, rechts);
    }

    /** Vertikale Teilung mit gleicher Größe (50%). */
    public static SplitTeilung vertikal(SplitKnoten links, SplitKnoten rechts) {
        return new SplitTeilung("V", 50, links, rechts);
    }

    public boolean istHorizontal() {
        return "H".equalsIgnoreCase(richtung);
    }
}
