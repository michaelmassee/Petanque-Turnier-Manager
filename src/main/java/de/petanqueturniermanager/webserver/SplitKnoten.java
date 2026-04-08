package de.petanqueturniermanager.webserver;

/**
 * Knoten im binären Split-Baum eines Composite Views.
 * <p>
 * Entweder ein Blatt ({@link SplitBlatt}), das auf ein Panel verweist,
 * oder eine Teilung ({@link SplitTeilung}), die zwei Kindknoten trennt.
 * <p>
 * Gson-Serialisierung erfolgt über {@link SplitKnotenAdapter}.
 */
public sealed interface SplitKnoten permits SplitBlatt, SplitTeilung {
}
