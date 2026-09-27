/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;
import de.petanqueturniermanager.ptmonline.live.LivePartie;
import de.petanqueturniermanager.ptmonline.live.LiveRanglistenEintrag;
import de.petanqueturniermanager.ptmonline.live.LiveRunde;

/**
 * Wandelt den lokalen Live-Stand in die Payloads der PTM-Online-Sync-API um und hält dabei deren Regeln ein:
 * <ul>
 * <li>Eine Partie wird nur übertragen, wenn beide Seiten vollständig online zugeordnet sind. Eine Seite ohne
 * Zuordnung würde online sonst als Freilos erscheinen.</li>
 * <li>Jede Meldung darf pro Runde nur einmal vorkommen; spätere Doppelungen werden ausgelassen.</li>
 * <li>Ergebnisse außerhalb 0–13 kennt PTM-Online nicht – die Partie gilt dann als laufend.</li>
 * <li>Bahnen sind auf 40 Zeichen begrenzt.</li>
 * </ul>
 */
final class LiveUebertragungsDaten {

    private static final Logger logger = LogManager.getLogger(LiveUebertragungsDaten.class);

    static final int MAX_PUNKTE = 13;
    static final int MAX_BAHN_LAENGE = 40;

    private LiveUebertragungsDaten() {}

    static List<LiveMatchDto> matches(LiveRunde runde, Map<Integer, List<String>> onlineIds) {
        Set<String> eingeteilt = new HashSet<>();
        List<LiveMatchDto> matches = new ArrayList<>();
        for (LivePartie partie : runde.partien()) {
            Optional<List<String>> teamA = onlineIdsVon(partie.teamA(), onlineIds);
            Optional<List<String>> teamB = partie.istFreilos() ? Optional.of(List.of())
                    : onlineIdsVon(partie.teamB(), onlineIds);
            if (teamA.isEmpty() || teamA.get().isEmpty() || teamB.isEmpty()) {
                logger.debug("PTM-Online Live: Partie {} in Runde {} ohne Online-Zuordnung übersprungen", partie,
                        runde.nr());
                continue;
            }
            List<String> beide = new ArrayList<>(teamA.get());
            beide.addAll(teamB.get());
            if (beide.stream().anyMatch(eingeteilt::contains) || new HashSet<>(beide).size() != beide.size()) {
                logger.warn("PTM-Online Live: Meldung in Runde {} mehrfach eingeteilt, Partie {} übersprungen",
                        runde.nr(), partie);
                continue;
            }
            eingeteilt.addAll(beide);
            boolean ergebnisGueltig = istGueltig(partie.punkteA()) && istGueltig(partie.punkteB());
            matches.add(new LiveMatchDto(teamA.get(), teamB.get(), ergebnisGueltig ? partie.punkteA() : null,
                    ergebnisGueltig ? partie.punkteB() : null, bahn(partie.bahn()), leerAlsNull(partie.stufe())));
        }
        return matches;
    }

    static List<LiveRankingEntryDto> rangliste(List<LiveRanglistenEintrag> eintraege,
            Map<Integer, List<String>> onlineIds) {
        List<LiveRankingEntryDto> ergebnis = new ArrayList<>();
        for (LiveRanglistenEintrag eintrag : eintraege) {
            onlineIdsVon(eintrag.nummern(), onlineIds).filter(ids -> !ids.isEmpty())
                    .ifPresent(ids -> ergebnis.add(new LiveRankingEntryDto(eintrag.platz(), ids, eintrag.siege(),
                            nichtNegativ(eintrag.punktePlus()), nichtNegativ(eintrag.punkteMinus()))));
        }
        return ergebnis;
    }

    /** @return leer, wenn mindestens eine Nummer nicht online zugeordnet ist */
    private static Optional<List<String>> onlineIdsVon(List<Integer> nummern, Map<Integer, List<String>> onlineIds) {
        List<String> ids = new ArrayList<>();
        for (Integer nummer : nummern) {
            List<String> zugeordnet = onlineIds.get(nummer);
            if (zugeordnet == null) {
                return Optional.empty();
            }
            ids.addAll(zugeordnet);
        }
        return Optional.of(List.copyOf(ids));
    }

    private static boolean istGueltig(@Nullable Integer punkte) {
        return punkte != null && punkte >= 0 && punkte <= MAX_PUNKTE;
    }

    private static @Nullable Integer nichtNegativ(@Nullable Integer wert) {
        return wert == null || wert < 0 ? null : wert;
    }

    private static @Nullable String bahn(@Nullable String bahn) {
        String text = leerAlsNull(bahn);
        return text == null || text.length() <= MAX_BAHN_LAENGE ? text : text.substring(0, MAX_BAHN_LAENGE);
    }

    private static @Nullable String leerAlsNull(@Nullable String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
