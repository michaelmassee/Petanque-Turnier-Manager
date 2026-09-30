/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuelle;
import de.petanqueturniermanager.ptmonline.live.LiveTurnierStand;

/**
 * Erfasst Spielrunden (Paarungen, Bahn, Ergebnisse) und Rangliste eines im Turnierdokument durchgeführten Turniers als
 * Schreibaufträge an PTM-Online – Grundlage der persönlichen Live-Ansicht der Spieler („Aktuelle Partie“, „Meine
 * Partien“, Rangliste).
 * <p>
 * Grundlage ist immer der komplette Stand des Dokuments: PTM-Online ersetzt jede Runde atomar, Korrekturen
 * (neu ausgeloste Runde, geänderte Ergebnisse) kommen 1:1 an, lokal gelöschte Runden werden online gelöscht.
 * Erfasst wird nur, was sich seit dem zuletzt erfassten Stand geändert hat ({@link LiveUebertragungsGedaechtnis});
 * ein noch nicht gesendeter älterer Stand derselben Runde wird dabei ersetzt.
 * <p>
 * Läuft im Dokument-Kontext der Auftragserfassung ({@link PtmOnlineLiveBeobachter}); gesendet wird im Hintergrund.
 */
public final class PtmOnlineLiveSync {

    private static final LiveUebertragungsGedaechtnis GEDAECHTNIS = new LiveUebertragungsGedaechtnis();

    private PtmOnlineLiveSync() {}

    /** Aus dem Dokument gelesener Live-Stand samt Zuordnung der lokalen Nummern zu Online-IDs. */
    record Momentaufnahme(LiveTurnierStand stand, Map<Integer, List<String>> onlineIds) {}

    /** Liest alles für die Aufträge Nötige aus dem Dokument. */
    static Momentaufnahme lese(PtmOnlineVerbindung verbindung, LiveStandQuelle quelle) throws GenerateException {
        return new Momentaufnahme(quelle.lese(), LiveNummernAufloesung.ermitteln(verbindung));
    }

    /**
     * @param rundenOnline Anzahl der online vorhandenen Runden, sofern bekannt – nur gebraucht, wenn noch kein Stand
     *                     erfasst ist (nach dem Öffnen oder Verbinden), um online überzählige Runden zu löschen
     * @return Anzahl der erzeugten Aufträge
     */
    static int erfasse(AuftragsBestand bestand, String tournamentId, Momentaufnahme momentaufnahme,
            OptionalInt rundenOnline) {
        return erfasse(bestand, tournamentId, momentaufnahme.stand(), momentaufnahme.onlineIds(), rundenOnline,
                GEDAECHTNIS);
    }

    static int erfasse(AuftragsBestand bestand, String tournamentId, LiveTurnierStand stand,
            Map<Integer, List<String>> onlineIds, OptionalInt rundenOnline, LiveUebertragungsGedaechtnis gedaechtnis) {
        Optional<LiveUebertragungsGedaechtnis.Stand> bisher = gedaechtnis.letzter(tournamentId);
        LiveUebertragungsGedaechtnis.Stand neu = neuerStand(stand, onlineIds);
        int anzahl = 0;
        Map<Integer, List<LiveMatchDto>> bisherigeRunden = bisher.map(LiveUebertragungsGedaechtnis.Stand::runden)
                .orElse(Map.of());
        for (Map.Entry<Integer, List<LiveMatchDto>> runde : new TreeMap<>(neu.runden()).entrySet()) {
            if (!runde.getValue().equals(bisherigeRunden.get(runde.getKey()))) {
                PtmOnlineAuftraege.runde(bestand, tournamentId, runde.getKey(), runde.getValue());
                anzahl++;
            }
        }
        int naechsteRunde = neu.runden().keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        int letzteZuLoeschende = bisher.isPresent()
                ? bisherigeRunden.keySet().stream().mapToInt(Integer::intValue).max().orElse(0)
                : rundenOnline.orElse(0);
        for (int runde = naechsteRunde; runde <= letzteZuLoeschende; runde++) {
            PtmOnlineAuftraege.rundeLoeschen(bestand, tournamentId, runde);
            anzahl++;
        }
        boolean ranglisteGeaendert = !neu.rangliste()
                .equals(bisher.map(LiveUebertragungsGedaechtnis.Stand::rangliste).orElse(null));
        if (!neu.rangliste().isEmpty() && ranglisteGeaendert) {
            PtmOnlineAuftraege.rangliste(bestand, tournamentId, neu.rangliste());
            anzahl++;
        }
        gedaechtnis.merke(tournamentId, neu);
        return anzahl;
    }

    /** Ob für das Turnier schon ein Stand erfasst ist; sonst wird beim nächsten Mal alles erfasst. */
    static boolean hatStand(String tournamentId) {
        return GEDAECHTNIS.letzter(tournamentId).isPresent();
    }

    /** Gedächtnis verwerfen: die nächste Erfassung überträgt den kompletten Stand (manueller Abgleich, Fortsetzen). */
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
}
