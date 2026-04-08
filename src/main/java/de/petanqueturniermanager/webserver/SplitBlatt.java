package de.petanqueturniermanager.webserver;

/**
 * Blatt-Knoten im Split-Baum: verweist auf ein Panel per Index.
 * <p>
 * JSON-Darstellung: {@code {"panel": 0}}
 *
 * @param panel 0-basierter Index in der Panel-Liste des Composite Views
 */
public record SplitBlatt(int panel) implements SplitKnoten {
}
