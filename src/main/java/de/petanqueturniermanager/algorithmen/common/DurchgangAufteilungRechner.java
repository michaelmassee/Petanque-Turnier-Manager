/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.common;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * Berechnet die Größen der Durchgang-Blöcke (Heats) für die optionale Durchgang-Aufteilung
 * einer Spielrunde (Schweizer/Maastrichter), wenn mehr Paarungen als Bahnen vorhanden sind.
 *
 * <p>Zwei Modi: {@link #berechne(int, int)} macht eine einfache Chunk-Aufteilung — Paarungen
 * werden in Blöcke der Größe {@code anzahlBahnen} eingeteilt, der letzte Block enthält den
 * Rest, auch wenn er kleiner als {@code anzahlBahnen} ist (anders als bei
 * {@link GruppenAufteilungRechner} keine Faltung in den vorherigen Block). {@link
 * #berechne(int, int, boolean)} mit {@code gleichmaessigAufteilen == true} verteilt die
 * Paarungen stattdessen möglichst gleichmäßig auf dieselbe Anzahl Durchgänge, damit kein
 * Durchgang unnötig unterbesetzt bleibt.
 */
public class DurchgangAufteilungRechner {

    private DurchgangAufteilungRechner() {
        // Hilfsklasse – kein Instanz-Konstruktor
    }

    /**
     * Berechnet die Größen der Durchgang-Blöcke per Chunk-Aufteilung (siehe Klassen-Javadoc).
     *
     * @param anzahlPaarungen Gesamtanzahl Paarungen der Runde (muss &gt; 0 sein)
     * @param anzahlBahnen    maximale Paarungen pro Durchgang (muss &gt; 0 sein)
     * @return unveränderliche Liste der Durchgang-Größen (Index 0 = Durchgang 1 usw.)
     * @throws IllegalArgumentException wenn Vorbedingungen verletzt sind
     */
    public static List<Integer> berechne(int anzahlPaarungen, int anzahlBahnen) {
        checkArgument(anzahlPaarungen > 0, "anzahlPaarungen muss groesser als 0 sein");
        checkArgument(anzahlBahnen > 0, "anzahlBahnen muss groesser als 0 sein");

        List<Integer> ergebnis = new ArrayList<>();
        for (int start = 0; start < anzahlPaarungen; start += anzahlBahnen) {
            ergebnis.add(Math.min(anzahlBahnen, anzahlPaarungen - start));
        }
        return ergebnis;
    }

    /**
     * Berechnet die Größen der Durchgang-Blöcke.
     *
     * <p>Bei {@code gleichmaessigAufteilen == true} bleibt die Anzahl Durchgänge identisch zur
     * Chunk-Aufteilung ({@code numDurchgaenge = ceil(anzahlPaarungen / anzahlBahnen)}), die
     * Paarungen werden aber gleichmäßig darauf verteilt: {@code basis = anzahlPaarungen /
     * numDurchgaenge}, {@code rest = anzahlPaarungen % numDurchgaenge} Durchgänge bekommen
     * {@code basis + 1} Paarungen, die übrigen {@code basis}. Da {@code numDurchgaenge} als
     * Ceiling-Division definiert ist, gilt stets {@code basis + 1 <= anzahlBahnen}. Beispiel:
     * 13 Paarungen, 4 Bahnen → {@code [4, 3, 3, 3]} statt {@code [4, 4, 4, 1]}.
     *
     * @param anzahlPaarungen      Gesamtanzahl Paarungen der Runde (muss &gt; 0 sein)
     * @param anzahlBahnen         maximale Paarungen pro Durchgang (muss &gt; 0 sein)
     * @param gleichmaessigAufteilen {@code true} für gleichmäßige Verteilung, {@code false} für
     *                             die Chunk-Aufteilung wie {@link #berechne(int, int)}
     * @return unveränderliche Liste der Durchgang-Größen (Index 0 = Durchgang 1 usw.)
     * @throws IllegalArgumentException wenn Vorbedingungen verletzt sind
     */
    public static List<Integer> berechne(int anzahlPaarungen, int anzahlBahnen, boolean gleichmaessigAufteilen) {
        if (!gleichmaessigAufteilen) {
            return berechne(anzahlPaarungen, anzahlBahnen);
        }
        checkArgument(anzahlPaarungen > 0, "anzahlPaarungen muss groesser als 0 sein");
        checkArgument(anzahlBahnen > 0, "anzahlBahnen muss groesser als 0 sein");

        int numDurchgaenge = (anzahlPaarungen + anzahlBahnen - 1) / anzahlBahnen;
        int basis = anzahlPaarungen / numDurchgaenge;
        int rest = anzahlPaarungen % numDurchgaenge;

        List<Integer> ergebnis = new ArrayList<>();
        for (int i = 0; i < numDurchgaenge; i++) {
            ergebnis.add(i < rest ? basis + 1 : basis);
        }
        return ergebnis;
    }
}
