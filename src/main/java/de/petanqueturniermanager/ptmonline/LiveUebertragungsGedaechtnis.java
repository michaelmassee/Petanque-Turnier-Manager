/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;

/**
 * Merkt sich je Online-Turnier den zuletzt vollständig übertragenen Live-Stand, damit nur geänderte Runden und
 * eine geänderte Rangliste erneut gesendet werden. Bei jeder Ergebnis-Eingabe wird der Stand neu gelesen – ohne
 * Gedächtnis ginge jedes Mal das ganze Turnier über das Netz.
 * <p>
 * Nach einem Fehler wird der Eintrag verworfen: welcher Teil online angekommen ist, ist dann unbekannt, der
 * nächste Lauf überträgt wieder alles.
 */
final class LiveUebertragungsGedaechtnis {

    /** Übertragener Stand: Partien je Rundennummer und Rangliste. */
    record Stand(Map<Integer, List<LiveMatchDto>> runden, List<LiveRankingEntryDto> rangliste) {

        Stand {
            runden = Map.copyOf(runden);
            rangliste = List.copyOf(rangliste);
        }
    }

    private final Map<String, Stand> proTurnier = new ConcurrentHashMap<>();

    Optional<Stand> letzter(String tournamentId) {
        return Optional.ofNullable(proTurnier.get(tournamentId));
    }

    void merke(String tournamentId, Stand stand) {
        proTurnier.put(tournamentId, stand);
    }

    void vergessen(String tournamentId) {
        proTurnier.remove(tournamentId);
    }
}
