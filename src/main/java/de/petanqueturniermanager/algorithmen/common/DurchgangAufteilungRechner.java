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
 * <p>Einfache Chunk-Aufteilung: Paarungen werden in Blöcke der Größe {@code anzahlBahnen}
 * eingeteilt. Der letzte Block enthält den Rest, auch wenn er kleiner als {@code anzahlBahnen}
 * ist — anders als bei {@link GruppenAufteilungRechner} ist ein 1-Paarung-Rest hier ein gültiger
 * eigener Durchgang, keine Faltung in den vorherigen Block nötig.
 */
public class DurchgangAufteilungRechner {

    private DurchgangAufteilungRechner() {
        // Hilfsklasse – kein Instanz-Konstruktor
    }

    /**
     * Berechnet die Größen der Durchgang-Blöcke.
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
}
