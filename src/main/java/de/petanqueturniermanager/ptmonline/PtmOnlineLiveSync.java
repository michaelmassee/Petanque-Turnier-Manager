/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;

import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;
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

    private PtmOnlineLiveSync() {}

    /** Aus dem Dokument gelesener Live-Stand samt Zuordnung der lokalen Nummern zu Online-IDs. */
    record Momentaufnahme(LiveTurnierStand stand, Map<Integer, List<String>> onlineIds) {}

    /** Liest alles für die Aufträge Nötige aus dem Dokument. */
    static Momentaufnahme lese(PtmOnlineVerbindung verbindung, LiveStandQuelle quelle) throws GenerateException {
        return new Momentaufnahme(quelle.lese(), LiveNummernAufloesung.ermitteln(verbindung));
    }

    /**
     * @param rundenOnline höchste online vorhandene Rundennummer, sofern bekannt – gebraucht, solange sie seit dem
     *                     Öffnen oder Verbinden noch nicht abgeglichen sind, um online überzählige Runden zu löschen
     *                     (auch nachträglich, wenn der erste Abruf gescheitert ist)
     * @return Anzahl der erzeugten Aufträge
     */
    static int erfasse(AuftragsBestand bestand, String tournamentId, Momentaufnahme momentaufnahme,
            OptionalInt rundenOnline) {
        return erfasse(bestand, tournamentId, momentaufnahme.stand(), momentaufnahme.onlineIds(), rundenOnline,
                gedaechtnis(bestand));
    }

    static int erfasse(AuftragsBestand bestand, String tournamentId, LiveTurnierStand stand,
            Map<Integer, List<String>> onlineIds, OptionalInt rundenOnline, LiveUebertragungsGedaechtnis gedaechtnis) {
        Optional<LiveUebertragungsGedaechtnis.Stand> bisher = gedaechtnis.letzter(tournamentId);
        boolean rundenAbgleichOffen = bisher.map(b -> !b.onlineRundenAbgeglichen()).orElse(true);
        OptionalInt abzugleichendeOnlineRunden = rundenAbgleichOffen ? rundenOnline : OptionalInt.empty();
        LiveUebertragungsGedaechtnis.Stand neu = neuerStand(stand, onlineIds,
                !rundenAbgleichOffen || rundenOnline.isPresent());
        Map<Integer, List<LiveMatchDto>> bisherigeRunden = bisher.map(LiveUebertragungsGedaechtnis.Stand::runden)
                .orElse(Map.of());
        int anzahl = erfasseRunden(bestand, tournamentId, neu.runden(), bisherigeRunden);
        anzahl += erfasseRundenLoeschungen(bestand, tournamentId, neu.runden(), bisherigeRunden,
                abzugleichendeOnlineRunden);
        if (erfasseRangliste(bestand, tournamentId, neu, bisher, abzugleichendeOnlineRunden)) {
            anzahl++;
        }
        gedaechtnis.merke(tournamentId, neu);
        return anzahl;
    }

    private static int erfasseRunden(AuftragsBestand bestand, String tournamentId,
            Map<Integer, List<LiveMatchDto>> runden, Map<Integer, List<LiveMatchDto>> bisherigeRunden) {
        int anzahl = 0;
        for (Map.Entry<Integer, List<LiveMatchDto>> runde : new TreeMap<>(runden).entrySet()) {
            if (!runde.getValue().equals(bisherigeRunden.get(runde.getKey()))) {
                PtmOnlineAuftraege.runde(bestand, tournamentId, runde.getKey(), runde.getValue());
                anzahl++;
            }
        }
        return anzahl;
    }

    /**
     * Löscht online alle Runden hinter der letzten lokalen: die zuletzt erfassten und – solange noch nicht abgeglichen
     * – die online vorhandenen.
     */
    private static int erfasseRundenLoeschungen(AuftragsBestand bestand, String tournamentId,
            Map<Integer, List<LiveMatchDto>> runden, Map<Integer, List<LiveMatchDto>> bisherigeRunden,
            OptionalInt abzugleichendeOnlineRunden) {
        int naechsteRunde = hoechsteRunde(runden) + 1;
        int letzteZuLoeschende = Math.max(hoechsteRunde(bisherigeRunden), abzugleichendeOnlineRunden.orElse(0));
        int anzahl = 0;
        for (int runde = naechsteRunde; runde <= letzteZuLoeschende; runde++) {
            PtmOnlineAuftraege.rundeLoeschen(bestand, tournamentId, runde);
            anzahl++;
        }
        return anzahl;
    }

    /**
     * Eine geänderte Rangliste (samt KO-Siegern) ersetzt den Online-Stand samt eines noch wartenden älteren Auftrags –
     * auch eine geleerte. Werden gerade online vorhandene Runden abgeglichen, kann dort eine ältere Rangliste stehen: dann wird
     * sie auch unverändert gesendet. Eine leere Rangliste nur, wenn online eine ältere stehen kann (zuletzt erfasst oder
     * online Runden vorhanden); vor dem Turnierstart lehnt PTM-Online sie sonst ab.
     *
     * @return ob ein Auftrag entstanden ist
     */
    private static boolean erfasseRangliste(AuftragsBestand bestand, String tournamentId,
            LiveUebertragungsGedaechtnis.Stand neu, Optional<LiveUebertragungsGedaechtnis.Stand> bisher,
            OptionalInt abzugleichendeOnlineRunden) {
        List<LiveRankingEntryDto> rangliste = neu.rangliste();
        Optional<List<LiveRankingEntryDto>> bisherigeRangliste = bisher.map(LiveUebertragungsGedaechtnis.Stand::rangliste);
        boolean onlineRundenVorhanden = abzugleichendeOnlineRunden.orElse(0) > 0;
        boolean unveraendert = bisherigeRangliste.filter(rangliste::equals).isPresent()
                && bisher.map(LiveUebertragungsGedaechtnis.Stand::koSieger).filter(neu.koSieger()::equals).isPresent();
        if (unveraendert && !onlineRundenVorhanden) {
            return false;
        }
        boolean onlineKannRanglisteHaben = onlineRundenVorhanden
                || bisherigeRangliste.filter(liste -> !liste.isEmpty()).isPresent();
        if (rangliste.isEmpty() && !onlineKannRanglisteHaben) {
            return false;
        }
        PtmOnlineAuftraege.rangliste(bestand, tournamentId, rangliste, neu.koSieger());
        return true;
    }

    private static int hoechsteRunde(Map<Integer, List<LiveMatchDto>> runden) {
        return runden.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /**
     * Zuletzt erfasster Stand dieses Dokuments: Er hängt am Auftragspuffer, nicht am Online-Turnier – sonst unterdrückte
     * eine abgewiesene Dokumentkopie desselben Turniers die Übertragung des Originals.
     */
    private static LiveUebertragungsGedaechtnis gedaechtnis(AuftragsBestand bestand) {
        return bestand.zwischenstand(LiveUebertragungsGedaechtnis.class, LiveUebertragungsGedaechtnis::new);
    }

    /** Ob für das Turnier schon ein Stand erfasst ist; sonst wird beim nächsten Mal alles erfasst. */
    static boolean hatStand(AuftragsBestand bestand, String tournamentId) {
        return gedaechtnis(bestand).letzter(tournamentId).isPresent();
    }

    /**
     * Ob die online vorhandenen Runden noch abzugleichen sind, weil ihre Anzahl bei keiner Erfassung bekannt war
     * (z.&nbsp;B. erster Abruf ohne Netz); dann ist der Online-Zustand erneut abzurufen.
     */
    static boolean brauchtOnlineRunden(AuftragsBestand bestand, String tournamentId) {
        return gedaechtnis(bestand).letzter(tournamentId).filter(stand -> !stand.onlineRundenAbgeglichen())
                .isPresent();
    }

    /** Gedächtnis verwerfen: die nächste Erfassung überträgt den kompletten Stand (manueller Abgleich, Fortsetzen). */
    static void vergessen(AuftragsBestand bestand, String tournamentId) {
        gedaechtnis(bestand).vergessen(tournamentId);
    }

    /** Wie {@link #vergessen(AuftragsBestand, String)}, für alle geladenen Bindungen dieses Dokuments. */
    static void vergessen(XSpreadsheetDocument xDoc, String tournamentId) {
        PtmOnlineAuftraege.geladene(xDoc).forEach(bestand -> vergessen(bestand, tournamentId));
    }

    private static LiveUebertragungsGedaechtnis.Stand neuerStand(LiveTurnierStand stand,
            Map<Integer, List<String>> onlineIds, boolean onlineRundenAbgeglichen) {
        Map<Integer, List<LiveMatchDto>> runden = new TreeMap<>();
        stand.runden().forEach(runde -> runden.put(runde.nr(), LiveUebertragungsDaten.matches(runde, onlineIds)));
        return new LiveUebertragungsGedaechtnis.Stand(runden,
                LiveUebertragungsDaten.rangliste(stand.rangliste(), onlineIds),
                LiveUebertragungsDaten.koSieger(stand.koSieger(), onlineIds), onlineRundenAbgeglichen);
    }
}
