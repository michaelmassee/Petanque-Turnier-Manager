/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import de.petanqueturniermanager.ptmonline.dto.LiveKoWinnerDto;
import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;

/**
 * Merkt sich je Online-Turnier den zuletzt als Aufträge erfassten Live-Stand, damit nur geänderte Runden und eine
 * geänderte Rangliste erneut erfasst werden. Bei jeder Ergebnis-Eingabe wird der Stand neu gelesen – ohne Gedächtnis
 * ginge jedes Mal das ganze Turnier über das Netz. Die erfassten Aufträge liegen im Puffer und werden gesendet, bis
 * PTM-Online sie angenommen hat; nach einem Neustart fehlt das Gedächtnis, dann wird wieder alles erfasst.
 */
final class LiveUebertragungsGedaechtnis {

    /**
     * Übertragener Stand: Partien je Rundennummer, Rangliste und KO-Sieger (zweiter Teil der Endrangliste).
     *
     * @param onlineRundenAbgeglichen die online vorhandenen Runden waren bei einer Erfassung bekannt, überzählige sind
     *                                gelöscht; sonst wird das nachgeholt, sobald ihre Anzahl bekannt ist
     */
    record Stand(Map<Integer, List<LiveMatchDto>> runden, List<LiveRankingEntryDto> rangliste,
            List<LiveKoWinnerDto> koSieger, boolean onlineRundenAbgeglichen) {

        Stand {
            runden = Map.copyOf(runden);
            rangliste = List.copyOf(rangliste);
            koSieger = List.copyOf(koSieger);
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
