/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuelle;
import de.petanqueturniermanager.ptmonline.live.LiveTurnierStand;

/**
 * Überträgt Spielrunden (Paarungen, Bahn, Ergebnisse) und Rangliste eines im Turnierdokument durchgeführten
 * Turniers an PTM-Online – Grundlage der persönlichen Live-Ansicht der Spieler („Aktuelle Partie“, „Meine
 * Partien“, Rangliste).
 * <p>
 * Grundlage ist immer der komplette Stand des Dokuments: PTM-Online ersetzt jede Runde atomar, Korrekturen
 * (neu ausgeloste Runde, geänderte Ergebnisse) kommen 1:1 an, lokal gelöschte Runden werden online gelöscht.
 * Gesendet wird nur, was sich seit der letzten erfolgreichen Übertragung geändert hat
 * ({@link LiveUebertragungsGedaechtnis}).
 * <p>
 * Läuft ausschließlich im Hintergrund-Thread des {@link PtmOnlineLiveBeobachter}, der auch Wiederholungen bei
 * Netzfehlern übernimmt – der Turnierbetrieb wartet nie auf das Netz.
 */
public final class PtmOnlineLiveSync {

    private static final int MAX_RUNDEN_NR = 999;
    private static final LiveUebertragungsGedaechtnis GEDAECHTNIS = new LiveUebertragungsGedaechtnis();

    private PtmOnlineLiveSync() {}

    /** Aus dem Dokument gelesener Live-Stand samt Zuordnung der lokalen Nummern zu Online-IDs. */
    record Momentaufnahme(LiveTurnierStand stand, Map<Integer, List<String>> onlineIds) {}

    /** Bei pausiertem Sync wird nichts gelesen und nichts übertragen. */
    static void uebertragen(PtmOnlineVerbindung verbindung, LiveStandQuelle quelle)
            throws GenerateException, IOException, InterruptedException {
        if (!PtmOnlineSpielrundeSync.istSyncAktiv(verbindung.mapping())) {
            return;
        }
        uebertragen(verbindung, lese(verbindung, quelle));
    }

    /** Liest alles für die Übertragung Nötige aus dem Dokument – danach braucht das Senden kein Dokument mehr. */
    static Momentaufnahme lese(PtmOnlineVerbindung verbindung, LiveStandQuelle quelle) throws GenerateException {
        return new Momentaufnahme(quelle.lese(), LiveNummernAufloesung.ermitteln(verbindung));
    }

    static void uebertragen(PtmOnlineVerbindung verbindung, Momentaufnahme momentaufnahme)
            throws GenerateException, IOException, InterruptedException {
        uebertragen(verbindung.gebundenerClient(), verbindung.tournamentId(), momentaufnahme.stand(),
                momentaufnahme.onlineIds());
    }

    static void uebertragen(TournamentSyncClient client, String tournamentId, LiveTurnierStand stand,
            Map<Integer, List<String>> onlineIds) throws IOException, InterruptedException {
        uebertragen(client, tournamentId, stand, onlineIds, GEDAECHTNIS);
    }

    /** Sendet nur, was sich seit der letzten erfolgreichen Übertragung geändert hat. */
    static void uebertragen(TournamentSyncClient client, String tournamentId, LiveTurnierStand stand,
            Map<Integer, List<String>> onlineIds, LiveUebertragungsGedaechtnis gedaechtnis)
            throws IOException, InterruptedException {
        Optional<LiveUebertragungsGedaechtnis.Stand> bisher = gedaechtnis.letzter(tournamentId);
        LiveUebertragungsGedaechtnis.Stand neu = neuerStand(stand, onlineIds);
        try {
            sendeAenderungen(client, tournamentId, bisher, neu);
        } catch (IOException | InterruptedException | RuntimeException e) {
            gedaechtnis.vergessen(tournamentId);
            throw e;
        }
        gedaechtnis.merke(tournamentId, neu);
    }

    /** Übertragungsgedächtnis verwerfen: der nächste Lauf überträgt den kompletten Stand (manueller Abgleich). */
    static void vergessen(String tournamentId) {
        GEDAECHTNIS.vergessen(tournamentId);
    }

    private static LiveUebertragungsGedaechtnis.Stand neuerStand(LiveTurnierStand stand,
            Map<Integer, List<String>> onlineIds) {
        Map<Integer, List<LiveMatchDto>> runden = new TreeMap<>();
        stand.runden().forEach(runde -> runden.put(runde.nr(), LiveUebertragungsDaten.matches(runde, onlineIds)));
        return new LiveUebertragungsGedaechtnis.Stand(runden,
                LiveUebertragungsDaten.rangliste(stand.rangliste(), onlineIds));
    }

    private static void sendeAenderungen(TournamentSyncClient client, String tournamentId,
            Optional<LiveUebertragungsGedaechtnis.Stand> bisher, LiveUebertragungsGedaechtnis.Stand neu)
            throws IOException, InterruptedException {
        Map<Integer, List<LiveMatchDto>> bisherigeRunden = bisher.map(LiveUebertragungsGedaechtnis.Stand::runden)
                .orElse(Map.of());
        for (Map.Entry<Integer, List<LiveMatchDto>> runde : new TreeMap<>(neu.runden()).entrySet()) {
            if (!runde.getValue().equals(bisherigeRunden.get(runde.getKey()))) {
                client.putRound(tournamentId, runde.getKey(), runde.getValue());
            }
        }
        int naechsteRunde = neu.runden().keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        if (bisher.isEmpty() || bisherigeRunden.keySet().stream().anyMatch(nr -> nr >= naechsteRunde)) {
            loescheUeberzaehligeRunden(client, tournamentId, naechsteRunde);
        }
        boolean ranglisteGeaendert = !neu.rangliste()
                .equals(bisher.map(LiveUebertragungsGedaechtnis.Stand::rangliste).orElse(null));
        if (!neu.rangliste().isEmpty() && ranglisteGeaendert) {
            client.putRanking(tournamentId, neu.rangliste());
        }
    }

    /** Runden, die lokal nicht mehr existieren (z.&nbsp;B. gelöschte letzte Spielrunde), auch online löschen. */
    private static void loescheUeberzaehligeRunden(TournamentSyncClient client, String tournamentId, int abRunde)
            throws IOException, InterruptedException {
        int runde = abRunde;
        while (runde <= MAX_RUNDEN_NR && client.deleteRound(tournamentId, runde)) {
            runde++;
        }
    }
}
