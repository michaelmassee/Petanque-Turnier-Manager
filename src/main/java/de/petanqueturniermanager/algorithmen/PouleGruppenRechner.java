/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * Berechnet die Gruppengrößen für ein Poule-A/B-Turnier.
 *
 * <p>Standard sind 4er-Gruppen. Wenn die Gesamtanzahl kein Vielfaches von 4 ergibt,
 * werden 3er-Gruppen als Ausnahme gebildet.
 *
 * <p>Aufteilungsregeln (q = anzTeams / 4, r = anzTeams % 4):
 * <ul>
 *   <li>Rest 0: q Vierergruppen</li>
 *   <li>Rest 1: (q−2) Vierergruppen + 3 Dreiergruppen &nbsp;(mindestens 9 Teams erforderlich)</li>
 *   <li>Rest 2: (q−1) Vierergruppen + 2 Dreiergruppen &nbsp;(mindestens 6 Teams erforderlich)</li>
 *   <li>Rest 3:  q    Vierergruppen + 1 Dreiergruppe</li>
 * </ul>
 *
 * <p>Wahrheitstabelle ausgewählter Werte:
 * <pre>
 * anzTeams | r | Vierer | Dreier | Ergebnis
 * ---------|---|--------|--------|------------------
 *  8       | 0 |  2     |  0     | [4, 4]
 *  9       | 1 |  0     |  3     | [3, 3, 3]
 * 13       | 1 |  1     |  3     | [4, 3, 3, 3]
 *  6       | 2 |  0     |  2     | [3, 3]
 * 10       | 2 |  1     |  2     | [4, 3, 3]
 * 14       | 2 |  2     |  2     | [4, 4, 3, 3]
 *  7       | 3 |  1     |  1     | [4, 3]
 * 11       | 3 |  2     |  1     | [4, 4, 3]
 * </pre>
 */
public class PouleGruppenRechner {

    private record GruppenAnzahl(int vierer, int dreier) {}

    private PouleGruppenRechner() {
        // Hilfsklasse – kein Instanz-Konstruktor
    }

    /**
     * Berechnet die Liste der Gruppengrößen.
     * Vierergruppen werden vor Dreiergruppen aufgelistet.
     *
     * @param anzTeams Gesamtanzahl Teams (mindestens 3; bei Rest 1 mindestens 9)
     * @return unveränderliche Liste der Gruppengrößen (4 oder 3)
     * @throws IllegalArgumentException wenn {@code anzTeams} die Mindestbedingungen verletzt
     */
    public static List<Integer> berechneGruppenGroessen(int anzTeams) {
        var gruppen = berechneGruppenAnzahl(anzTeams);
        List<Integer> ergebnis = new ArrayList<>();
        for (int i = 0; i < gruppen.vierer(); i++) {
            ergebnis.add(4);
        }
        for (int i = 0; i < gruppen.dreier(); i++) {
            ergebnis.add(3);
        }
        return List.copyOf(ergebnis);
    }

    /**
     * Gibt die Anzahl der 4er-Gruppen zurück.
     *
     * @param anzTeams Gesamtanzahl Teams
     * @return Anzahl der Vierergruppen
     * @throws IllegalArgumentException wenn {@code anzTeams} die Mindestbedingungen verletzt
     */
    public static int anzVierTeamGruppen(int anzTeams) {
        return berechneGruppenAnzahl(anzTeams).vierer();
    }

    /**
     * Gibt die Anzahl der 3er-Gruppen zurück.
     *
     * @param anzTeams Gesamtanzahl Teams
     * @return Anzahl der Dreiergruppen
     * @throws IllegalArgumentException wenn {@code anzTeams} die Mindestbedingungen verletzt
     */
    public static int anzDreiTeamGruppen(int anzTeams) {
        return berechneGruppenAnzahl(anzTeams).dreier();
    }

    /**
     * Gibt die Gesamtanzahl der Gruppen zurück.
     *
     * @param anzTeams Gesamtanzahl Teams
     * @return Summe aus Vierer- und Dreiergruppen
     * @throws IllegalArgumentException wenn {@code anzTeams} die Mindestbedingungen verletzt
     */
    public static int anzGruppen(int anzTeams) {
        var gruppen = berechneGruppenAnzahl(anzTeams);
        return gruppen.vierer() + gruppen.dreier();
    }

    private static GruppenAnzahl berechneGruppenAnzahl(int anzTeams) {
        validiereAnzTeams(anzTeams);
        int q = anzTeams / 4;
        int r = anzTeams % 4;
        int vierer;
        int dreier;
        switch (r) {
            case 0 -> { vierer = q;     dreier = 0; }
            case 1 -> { vierer = q - 2; dreier = 3; }
            case 2 -> { vierer = q - 1; dreier = 2; }
            case 3 -> { vierer = q;     dreier = 1; }
            default -> throw new IllegalStateException("Unerreichbar: r=" + r);
        }
        return new GruppenAnzahl(vierer, dreier);
    }

    private static void validiereAnzTeams(int anzTeams) {
        checkArgument(anzTeams >= 3,
                "anzTeams muss mindestens 3 sein, war: %s", anzTeams);
        int r = anzTeams % 4;
        if (r == 1) {
            checkArgument(anzTeams >= 9,
                    "Bei Rest 1 (anzTeams %% 4 == 1) werden 3 Dreiergruppen benoetigt "
                    + "(q-2 Vierergruppen), daher mindestens 9 Teams erforderlich; war: %s",
                    anzTeams);
        }
    }
}
