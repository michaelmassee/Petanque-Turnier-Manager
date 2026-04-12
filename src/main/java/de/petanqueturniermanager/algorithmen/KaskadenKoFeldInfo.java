/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import java.util.Optional;

/**
 * Unveränderliche Beschreibung eines einzelnen Feldes im Kaskaden-KO-System.<br>
 * <br>
 * Jedes Feld entsteht durch eine Folge von Sieger- und Verlierer-Entscheidungen
 * im Kaskadenbaum. Der {@code pfad} kodiert diesen Entstehungsweg (z. B. {@code "SV"}
 * = Sieger in Runde 1, Verlierer in Runde 2). Der {@code bezeichner} ist der
 * alphabetische Name des Feldes (A, B, C, …), der sich aus der lexikografischen
 * Position des Pfades ergibt.
 *
 * @param bezeichner     Alphabetischer Feldname (A, B, C, D, E, F, G, H)
 * @param pfad           Entstehungspfad aus 'S' (Sieger) und 'V' (Verlierer),
 *                       z. B. "SS" für zweimal Sieger → Feld A bei k=2
 * @param gesamtTeams    Anzahl Teams in diesem Feld
 * @param cadrageRechner Cadrage-Berechnung für dieses Feld;
 *                       leer wenn {@code gesamtTeams < 2} (kein Spiel möglich)
 *
 * @author Michael Massee
 */
public record KaskadenKoFeldInfo(
        String bezeichner,
        String pfad,
        int gesamtTeams,
        Optional<CadrageRechner> cadrageRechner) {

    /**
     * Erstellt ein {@link Optional} mit einem {@link CadrageRechner} für die
     * angegebene Teamanzahl, oder {@link Optional#empty()} wenn kein Spiel
     * möglich ist ({@code anzTeams < 2}).
     *
     * @param anzTeams Teamanzahl des Feldes
     * @return befülltes Optional wenn anzTeams &gt;= 2, sonst leer
     */
    public static Optional<CadrageRechner> cadrageRechnerFuer(int anzTeams) {
        return anzTeams >= 2 ? Optional.of(new CadrageRechner(anzTeams)) : Optional.empty();
    }

    /**
     * @return {@code true} wenn für dieses Feld eine Cadrage-Vorrunde gespielt
     *         werden muss (Teamanzahl ist keine Zweierpotenz)
     */
    public boolean isCadrageNoetig() {
        return cadrageRechner.map(r -> r.anzTeams() > 0).orElse(false);
    }

    /**
     * @return Anzahl Cadrage-Spiele (0 wenn keine Cadrage nötig oder Feld leer)
     */
    public int anzCadrageSpiele() {
        return cadrageRechner.map(r -> r.anzTeams() / 2).orElse(0);
    }

    /**
     * @return Anzahl Teams mit Freilos in der Cadrage-Vorrunde (0 wenn Feld leer)
     */
    public int anzFreilose() {
        return cadrageRechner.map(CadrageRechner::anzFreilose).orElse(0);
    }

    /**
     * @return Ziel-Teamanzahl nach der Cadrage (Zweierpotenz);
     *         entspricht {@code gesamtTeams} wenn keine Cadrage nötig
     */
    public int zielTeams() {
        return cadrageRechner.map(CadrageRechner::zielAnzahlTeams).orElse(gesamtTeams);
    }
}
