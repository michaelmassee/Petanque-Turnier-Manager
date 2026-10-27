/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.common;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

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

    /**
     * Berechnet die Durchgang-Blöcke in Sheet-Zeilen für eine Spielrunde, die Freilos-Zeilen enthält.
     *
     * <p>Ein Freilos belegt keine Bahn und zählt deshalb nicht zur Auslastung eines Durchgangs: Die
     * Aufteilung erfolgt über {@link #berechne(int, int, boolean)} mit der Anzahl der tatsächlich
     * gespielten Paarungen. Freilos-Zeilen werden dem Durchgang zugeschlagen, in dem sie stehen
     * (Freilose am Ende der Runde also dem letzten Durchgang), damit die Blöcke weiterhin lückenlos
     * alle Zeilen abdecken. Beispiel: 8 Spiele + 1 Freilos bei 8 Bahnen → {@code [9]} statt
     * {@code [8, 1]}.
     *
     * @param anzahlZeilen           Gesamtanzahl Paarungszeilen inkl. Freilos (muss &gt; 0 sein)
     * @param freilosIndizes         0-basierte Zeilenindizes der Freilos-Paarungen
     * @param anzahlBahnen           maximale Spiele pro Durchgang (muss &gt; 0 sein)
     * @param gleichmaessigAufteilen siehe {@link #berechne(int, int, boolean)}
     * @return unveränderliche Liste der Durchgang-Größen in Zeilen (Index 0 = Durchgang 1 usw.)
     * @throws IllegalArgumentException wenn Vorbedingungen verletzt sind
     */
    public static List<Integer> berechneZeilen(int anzahlZeilen, Set<Integer> freilosIndizes, int anzahlBahnen,
            boolean gleichmaessigAufteilen) {
        checkArgument(anzahlZeilen > 0, "anzahlZeilen muss groesser als 0 sein");
        int anzahlSpiele = (int) IntStream.range(0, anzahlZeilen).filter(i -> !freilosIndizes.contains(i)).count();
        if (anzahlSpiele == 0) {
            return List.of(anzahlZeilen);
        }
        List<Integer> spielBloecke = berechne(anzahlSpiele, anzahlBahnen, gleichmaessigAufteilen);

        int[] zeilenProBlock = new int[spielBloecke.size()];
        int block = 0;
        int spieleImBlock = 0;
        for (int zeile = 0; zeile < anzahlZeilen; zeile++) {
            boolean istSpiel = !freilosIndizes.contains(zeile);
            if (istSpiel && spieleImBlock == spielBloecke.get(block)) {
                block++;
                spieleImBlock = 0;
            }
            zeilenProBlock[block]++;
            if (istSpiel) {
                spieleImBlock++;
            }
        }
        return IntStream.of(zeilenProBlock).boxed().toList();
    }
}
