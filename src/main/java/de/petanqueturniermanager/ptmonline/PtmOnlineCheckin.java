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

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;

/**
 * Check-in am Turniertag: meldet Teilnahme-Änderungen der Meldeliste sofort an PTM-Online statt erst beim
 * Rundenstart. Damit erhalten die Spieler die Postfach-Nachricht „Du bist eingecheckt“ schon beim Einchecken. PTM-Online verschickt sie je Meldung und Online-Turnier nur einmal – auch wenn die Teilnahme am
 * Turniertag mehrfach wechselt. Bei Supermelee ist jeder Spieltag ein eigenes Online-Turnier.
 * <p>
 * Gepusht werden nur Änderungen gegenüber dem zuletzt bekannten Stand je Online-Turnier – Teilnahme oder Teamnummer,
 * denn die persönliche Live-Ansicht zeigt die Nr. der Meldeliste. Ist für eine Meldung noch
 * keiner bekannt (Dokument gerade geöffnet, verbunden oder Meldung neu zugeordnet), geht sie im Hintergrund nur als
 * aktiv hinaus, weil der Hintergrund die Online-Teilnahme nicht kennt. Den Rest erledigt der manuelle Abgleich: Er
 * vergleicht mit der Online-Teilnahme ({@link #abweichendVonOnline}) und setzt sie auf den Stand des Dokuments, das ab
 * dem Verbinden Master ist (E-28) – auch zurück auf nicht eingecheckt. Online-Anlage, fehlende lokale IDs und
 * Turnierstart bleiben Abgleich und Rundenstart vorbehalten – hier wird das Dokument nur gelesen.
 * <p>
 * Nur im Dokument-Kontext der Auftragserfassung verwendet ({@link PtmOnlineLiveBeobachter}).
 */
final class PtmOnlineCheckin {

    /** Zuletzt an PTM-Online gemeldeter Stand je Online-Turnier und lokaler UUID. */
    private final Map<String, Map<String, Gemeldet>> bekannterStand = new HashMap<>();

    /** An PTM-Online gemeldete Teilnahme und Teamnummer einer Meldung. */
    record Gemeldet(OnlineTeilnahme teilnahme, @Nullable Integer teamNummer) {

        static Gemeldet aus(PtmOnlineStatusAuftrag.Eintrag eintrag) {
            return new Gemeldet(eintrag.teilnahme(), eintrag.teamNummer());
        }
    }

    /**
     * Zu sendende Änderungen und der Stand, der nach erfolgreichem Senden als bekannt gilt.
     *
     * @param stand Teilnahme und Teamnummer aller zugeordneten Meldungen je lokaler UUID
     */
    record Aenderung(PtmOnlineStatusAuftrag auftrag, Map<String, Gemeldet> stand) {

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
        Map<String, Gemeldet> bekannt = bekannterStand.getOrDefault(tournamentId, Map.of());
        Map<String, Gemeldet> stand = new LinkedHashMap<>();
        List<PtmOnlineStatusAuftrag.Eintrag> geaendert = new ArrayList<>();
        for (PtmOnlineStatusAuftrag.Eintrag eintrag : aktuell) {
            stand.put(eintrag.lokaleUuid(), Gemeldet.aus(eintrag));
            if (istZuMelden(bekannt.get(eintrag.lokaleUuid()), eintrag)) {
                geaendert.add(eintrag);
            }
        }
        if (geaendert.isEmpty()) {
            bekannterStand.put(tournamentId, stand);
            return Optional.empty();
        }
        return Optional.of(new Aenderung(new PtmOnlineStatusAuftrag(tournamentId, geaendert), stand));
    }

    private static boolean istZuMelden(@Nullable Gemeldet bekannt, PtmOnlineStatusAuftrag.Eintrag aktuell) {
        if (bekannt == null) {
            return aktuell.teilnahme() == OnlineTeilnahme.AKTIV;
        }
        return bekannt.teilnahme() != aktuell.teilnahme()
                || OnlineMeldungsStand.teamNummerAbweichend(aktuell.teamNummer(), bekannt.teamNummer());
    }

    /**
     * Manueller Abgleich: lokale Einträge, deren Teilnahme oder Teamnummer vom Online-Stand abweicht. Meldungen ohne
     * bekannten Online-Stand (nicht zugeordnet oder online nicht bestätigt) bleiben unberührt.
     *
     * @param lokal                 je online zugeordneter Meldung Teilnahme, Setzposition und Teamnummer
     *                              ({@link #leseStand})
     * @param onlineIdProUuid       Online-Anmeldungs-ID je lokaler UUID
     * @param onlineStandProOnlineId Online-Stand bestätigter Anmeldungen
     */
    static List<PtmOnlineStatusAuftrag.Eintrag> abweichendVonOnline(List<PtmOnlineStatusAuftrag.Eintrag> lokal,
            Map<String, String> onlineIdProUuid, Map<String, OnlineMeldungsStand> onlineStandProOnlineId) {
        return lokal.stream().filter(eintrag -> Optional.ofNullable(onlineIdProUuid.get(eintrag.lokaleUuid()))
                .map(onlineStandProOnlineId::get).filter(online -> weichtAb(eintrag, online)).isPresent()).toList();
    }

    private static boolean weichtAb(PtmOnlineStatusAuftrag.Eintrag lokal, OnlineMeldungsStand online) {
        return !online.teilnahme().equals(lokal.teilnahme().apiWert())
                || OnlineMeldungsStand.teamNummerAbweichend(lokal.teamNummer(), online.teamNummer());
    }

    /** Die Änderung ist als Auftrag gespeichert; ihr Stand gilt als bekannt. */
    void bestaetigen(Aenderung aenderung) {
        // Veränderbare Kopie: ein späterer Rundenstart-Abgleich ergänzt den Stand ({@link #uebernehmen}).
        bekannterStand.put(aenderung.auftrag().tournamentId(), new HashMap<>(aenderung.stand()));
    }

    /** Ein gesendeter Rundenstart-Abgleich hat die Teilnahme seiner Meldungen gemeldet. */
    void uebernehmen(PtmOnlineStatusAuftrag gesendet) {
        Map<String, Gemeldet> bekannt = bekannterStand.computeIfAbsent(gesendet.tournamentId(),
                id -> new HashMap<>());
        gesendet.eintraege().forEach(eintrag -> bekannt.put(eintrag.lokaleUuid(), Gemeldet.aus(eintrag)));
    }

    /**
     * Liest die aktuelle Teilnahme aller online zugeordneten Meldungen, ohne das Dokument zu verändern. Bei
     * Supermelee liefert die Meldeliste die Spalte des aktiven (= verbundenen) Spieltags.
     */
    static List<PtmOnlineStatusAuftrag.Eintrag> leseStand(PtmOnlineVerbindung verbindung) throws GenerateException {
        TeilnahmeNummern nummern = TeilnahmeNummern.ausAktivSpalte(verbindung.meldeliste());
        MeldelisteZiel ziel = verbindung.ziel();
        List<LokaleOnlineMeldung> meldungen = PtmOnlineSpielrundeSync.lokaleMeldungen(ziel, nummern.alle(),
                nummern.aktive(), nummern.ausgesetzt());
        Map<Integer, String> uuidProZeile = leseLokaleUuids(ziel,
                meldungen.stream().map(LokaleOnlineMeldung::zeile1Basiert).toList());
        Map<String, String> onlineIds = verbindung.mapping().getOnlineIdsProUuid();
        List<PtmOnlineStatusAuftrag.Eintrag> eintraege = new ArrayList<>();
        for (LokaleOnlineMeldung meldung : meldungen) {
            String uuid = uuidProZeile.get(meldung.zeile1Basiert());
            if (uuid != null && onlineIds.containsKey(uuid)) {
                eintraege.add(new PtmOnlineStatusAuftrag.Eintrag(uuid, meldung.teilnahme(), meldung.seedingPosition(),
                        meldung.teamNummer()));
            }
        }
        return eintraege;
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
