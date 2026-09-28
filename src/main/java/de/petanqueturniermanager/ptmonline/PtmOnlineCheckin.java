/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;

/**
 * Check-in am Turniertag: meldet Teilnahme-Änderungen der Meldeliste sofort an PTM-Online statt erst beim
 * Rundenstart. Damit erhalten die Spieler die Postfach-Nachricht „Du bist eingecheckt“ (und den Live-Link) schon
 * beim Einchecken. PTM-Online verschickt sie je Meldung und Online-Turnier nur einmal – auch wenn die Teilnahme am
 * Turniertag mehrfach wechselt. Bei Supermelee ist jeder Spieltag ein eigenes Online-Turnier.
 * <p>
 * Gepusht werden nur Änderungen gegenüber dem zuletzt bekannten Stand je Online-Turnier. Ist für eine Meldung noch
 * keiner bekannt (Dokument gerade geöffnet, verbunden oder Meldung neu zugeordnet), geht sie nur als aktiv hinaus: ein
 * lokal leerer Check-in nimmt einen online erfolgten nicht zurück. Online-Anlage, fehlende lokale IDs und Turnierstart
 * bleiben dem Rundenstart vorbehalten – hier wird das Dokument nur gelesen.
 * <p>
 * Nur vom Thread „PTM-Online-Live“ verwendet ({@link PtmOnlineLiveBeobachter}).
 */
final class PtmOnlineCheckin {

    /** Zuletzt an PTM-Online gemeldete Teilnahme je Online-Turnier und lokaler UUID. */
    private final Map<String, Map<String, OnlineTeilnahme>> bekannterStand = new HashMap<>();

    /**
     * Zu sendende Änderungen und der Stand, der nach erfolgreichem Senden als bekannt gilt.
     *
     * @param stand Teilnahme aller zugeordneten Meldungen je lokaler UUID
     */
    record Aenderung(PtmOnlineStatusAuftrag auftrag, Map<String, OnlineTeilnahme> stand) {

        Aenderung {
            stand = Map.copyOf(stand);
        }
    }

    /**
     * Vergleicht den aktuellen lokalen Stand mit dem bekannten. Ohne Änderung gilt der aktuelle Stand sofort als
     * bekannt.
     *
     * @param aktuell je online zugeordneter Meldung Teilnahme und Setzposition (ohne Online-Anlage)
     * @return leer, wenn nichts zu senden ist
     */
    Optional<Aenderung> ermittle(String tournamentId, List<PtmOnlineStatusAuftrag.Eintrag> aktuell) {
        Map<String, OnlineTeilnahme> bekannt = bekannterStand.getOrDefault(tournamentId, Map.of());
        Map<String, OnlineTeilnahme> stand = new LinkedHashMap<>();
        List<PtmOnlineStatusAuftrag.Eintrag> geaendert = new ArrayList<>();
        for (PtmOnlineStatusAuftrag.Eintrag eintrag : aktuell) {
            stand.put(eintrag.lokaleUuid(), eintrag.teilnahme());
            if (istZuMelden(bekannt.get(eintrag.lokaleUuid()), eintrag.teilnahme())) {
                geaendert.add(eintrag);
            }
        }
        if (geaendert.isEmpty()) {
            bekannterStand.put(tournamentId, stand);
            return Optional.empty();
        }
        return Optional.of(new Aenderung(new PtmOnlineStatusAuftrag(tournamentId, false, geaendert), stand));
    }

    private static boolean istZuMelden(@Nullable OnlineTeilnahme bekannt, OnlineTeilnahme aktuell) {
        if (bekannt == null) {
            return aktuell == OnlineTeilnahme.AKTIV;
        }
        return bekannt != aktuell;
    }

    /** Die Änderung ist erledigt (gesendet oder online verworfen); ihr Stand gilt als bekannt. */
    void bestaetigen(Aenderung aenderung) {
        bekannterStand.put(aenderung.auftrag().tournamentId(), aenderung.stand());
    }

    /** Ein gesendeter Rundenstart-Abgleich hat die Teilnahme seiner Meldungen gemeldet. */
    void uebernehmen(PtmOnlineStatusAuftrag gesendet) {
        Map<String, OnlineTeilnahme> bekannt = bekannterStand.computeIfAbsent(gesendet.tournamentId(),
                id -> new HashMap<>());
        gesendet.eintraege().forEach(eintrag -> bekannt.put(eintrag.lokaleUuid(), eintrag.teilnahme()));
    }

    /**
     * Liest die aktuelle Teilnahme aller online zugeordneten Meldungen, ohne das Dokument zu verändern. Bei
     * Supermelee zählt die Spalte des verbundenen Spieltags.
     */
    static List<PtmOnlineStatusAuftrag.Eintrag> leseStand(WorkingSpreadsheet ws, TurnierSystem ts,
            PtmOnlineVerbindung verbindung) throws GenerateException {
        TeilnahmeNummern nummern = teilnahmeNummern(ws, ts, verbindung);
        MeldelisteZiel ziel = verbindung.ziel();
        List<LokaleOnlineMeldung> meldungen = PtmOnlineSpielrundeSync.lokaleMeldungen(ziel, verbindung.meldeliste(),
                nummern.alle(), nummern.aktive(), nummern.ausgesetzt());
        Map<Integer, String> uuidProZeile = leseLokaleUuids(ziel,
                meldungen.stream().map(LokaleOnlineMeldung::zeile1Basiert).toList());
        Map<String, String> onlineIds = verbindung.mapping().getOnlineIdsProUuid();
        List<PtmOnlineStatusAuftrag.Eintrag> eintraege = new ArrayList<>();
        for (LokaleOnlineMeldung meldung : meldungen) {
            String uuid = uuidProZeile.get(meldung.zeile1Basiert());
            if (uuid != null && onlineIds.containsKey(uuid)) {
                eintraege.add(new PtmOnlineStatusAuftrag.Eintrag(uuid, meldung.teilnahme(), meldung.seedingPosition(),
                        null));
            }
        }
        return eintraege;
    }

    private static TeilnahmeNummern teilnahmeNummern(WorkingSpreadsheet ws, TurnierSystem ts,
            PtmOnlineVerbindung verbindung) throws GenerateException {
        Integer spieltagNr = verbindung.spieltagNr();
        if (ts != TurnierSystem.SUPERMELEE || spieltagNr == null) {
            return TeilnahmeNummern.ausAktivSpalte(verbindung.meldeliste());
        }
        MeldeListeSheet_Update meldeliste = new MeldeListeSheet_Update(ws);
        meldeliste.setSpielTag(SpielTagNr.from(spieltagNr));
        return new TeilnahmeNummern(PtmOnlineSpielrundeSync.nummern(meldeliste.getAlleMeldungen()),
                PtmOnlineSpielrundeSync.nummern(meldeliste.getAktiveMeldungen()),
                PtmOnlineSpielrundeSync.nummern(meldeliste.getAusgesetztMeldungen()));
    }

    private static Map<Integer, String> leseLokaleUuids(MeldelisteZiel ziel, List<Integer> zeilen1Basiert)
            throws GenerateException {
        try {
            return ziel.leseLokaleUuids(zeilen1Basiert.stream().filter(zeile -> zeile > 0).toList());
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
    }
}
