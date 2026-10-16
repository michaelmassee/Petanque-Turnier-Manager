/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Eine Partie einer Spielrunde, wie sie das Turnierdokument führt.
 *
 * @param teamA       lokale Nummern der Seite A: Team-Nr (Formée/Mêlée-Team) bzw. Spieler-Nummern (Supermelee)
 * @param teamB       wie {@code teamA}; leer = Freilos
 * @param punkteA     eingetragene Punkte der Seite A, {@code null} = noch kein Ergebnis
 * @param punkteB     eingetragene Punkte der Seite B, {@code null} = noch kein Ergebnis
 * @param bahn        Spielbahn, {@code null} wenn das Turnier keine Bahnen führt
 * @param stufe       Anzeige über der Partie (z.&nbsp;B. „Halbfinale“, „Poule 3“), {@code null} wenn nicht nötig
 * @param ergebnisOrt Zellen des Ergebnisses, {@code null} wo eine gemeldete Partie nicht direkt eingetragen werden
 *                    kann (Turnierbaum, Poule, Kaskade, Trip-Tête)
 */
public record LivePartie(List<Integer> teamA, List<Integer> teamB, @Nullable Integer punkteA,
        @Nullable Integer punkteB, @Nullable String bahn, @Nullable String stufe, @Nullable ErgebnisOrt ergebnisOrt) {

    public LivePartie {
        teamA = List.copyOf(teamA);
        teamB = List.copyOf(teamB);
    }

    /** Partie ohne bekannten Ergebnisort. */
    public LivePartie(List<Integer> teamA, List<Integer> teamB, @Nullable Integer punkteA, @Nullable Integer punkteB,
            @Nullable String bahn, @Nullable String stufe) {
        this(teamA, teamB, punkteA, punkteB, bahn, stufe, null);
    }

    /** Partie zweier Teams mit je einer Team-Nr. */
    public static LivePartie teams(int teamA, int teamB, @Nullable Integer punkteA, @Nullable Integer punkteB,
            @Nullable String bahn, @Nullable String stufe) {
        return new LivePartie(List.of(teamA), List.of(teamB), punkteA, punkteB, bahn, stufe);
    }

    /** Freilos für ein Team. */
    public static LivePartie freilos(int team, @Nullable String bahn, @Nullable String stufe) {
        return new LivePartie(List.of(team), List.of(), null, null, bahn, stufe);
    }

    public boolean istFreilos() {
        return teamB.isEmpty();
    }

    /** Dieselbe Partie mit den Zellen ihres Ergebnisses. */
    public LivePartie mitErgebnisOrt(ErgebnisOrt ort) {
        return new LivePartie(teamA, teamB, punkteA, punkteB, bahn, stufe, ort);
    }

    /** Dieselbe Partie mit einer anderen Anzeige über der Partie. */
    public LivePartie mitStufe(@Nullable String andereStufe) {
        return new LivePartie(teamA, teamB, punkteA, punkteB, bahn, andereStufe, ergebnisOrt);
    }
}
