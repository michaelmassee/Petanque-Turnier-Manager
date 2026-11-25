/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.onlinesync.speicher.ZuordnungsZusatz;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.PersonDto;
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;

/**
 * Automatischer Meldelisten-Abgleich: Änderungen der Meldeliste gehen zeitnah nach PTM-Online, das Dokument ist
 * Master (E-15, E-16). Erkannt werden gegenüber dem zuletzt abgeglichenen, gespeicherten Stand
 * <ul>
 * <li>geänderte Namen und Teamnamen zugeordneter Meldungen → Änderungsauftrag; auch online zwischenzeitlich geänderte
 * Stände werden überschrieben,</li>
 * <li>neue Meldungen mit lokaler UUID → Online-Anlage (bis zum Turnierstart; eine gleichnamige, noch nicht verknüpfte
 * Online-Anmeldung lehnt PTM-Online ab, die verknüpft der manuelle Abgleich),</li>
 * <li>gelöschte Meldungen → Online-Stornierung, erst wenn die Zeile in zwei aufeinanderfolgenden Läufen fehlt. Kehrt
 * die Zeile vor dem Versand zurück, wird die Stornierung zurückgenommen.</li>
 * </ul>
 * Die Setzposition meldet die Check-in-Erkennung ({@link PtmOnlineCheckin}) mit der Teilnahme. Die Gegenrichtung
 * (Online-Anmeldungen und -Änderungen ins Dokument) bleibt dem manuellen Abgleich vorbehalten; hier wird das Dokument
 * nur gelesen. Zeilen ohne lokale UUID übergeht der Abgleich – die vergibt „Meldeliste aktualisieren“.
 * <p>
 * Nur im Dokument-Kontext der Auftragserfassung verwendet ({@link PtmOnlineLiveBeobachter}), nie gleichzeitig.
 */
final class PtmOnlineMeldelistenSync {

    private static final Logger logger = LogManager.getLogger(PtmOnlineMeldelistenSync.class);

    /** Gesehene Zuordnungen samt Online-ID, bis ihre Zeile zurückkehrt oder die Stornierung bestätigt ist. */
    private final Map<String, Map<String, String>> gesehen = new HashMap<>();
    /** Zugeordnete Meldungen, deren Zeile im letzten Lauf fehlte, je Online-Turnier. */
    private final Map<String, Set<String>> fehlend = new HashMap<>();

    /**
     * Übernimmt die vorhandenen Zuordnungen eines manuellen Abgleichs oder Rundenstarts im Dokument-Kontext.
     * So sind sofort danach gelöschte Zeilen auch vor dem ersten Hintergrundlauf bekannt. Bereits vorgemerkte
     * Löschungen bleiben erhalten; wieder vorhandene Zeilen müssen bei erneutem Fehlen neu bestätigt werden.
     */
    void uebernehmen(PtmOnlineStatusAuftrag status, Map<String, String> onlineIds) {
        Map<String, String> bekannt = gesehen.computeIfAbsent(status.tournamentId(), id -> new LinkedHashMap<>());
        Set<String> bisherFehlend = fehlend.get(status.tournamentId());
        for (PtmOnlineStatusAuftrag.Eintrag eintrag : status.eintraege()) {
            String onlineId = onlineIds.get(eintrag.lokaleUuid());
            if (onlineId != null) {
                bekannt.put(eintrag.lokaleUuid(), onlineId);
                if (bisherFehlend != null) {
                    bisherFehlend.remove(eintrag.lokaleUuid());
                }
            }
        }
    }

    /**
     * Eine Meldelistenzeile mit lokaler UUID.
     *
     * @param teamname          Inhalt der Teamname-Spalte, {@code null} ohne Teamname-Spalte
     * @param amAktivenSpieltag gehört zum Online-Turnier der Verbindung (bei Supermêlée: am aktiven Spieltag gemeldet)
     */
    record LokaleMeldung(int zeile1Basiert, String uuid, List<MeldelisteSpielerDaten> spieler,
            @Nullable String teamname, boolean amAktivenSpieltag) {

        LokaleMeldung {
            spieler = List.copyOf(spieler);
        }
    }

    /**
     * Stand der Zuordnungen im Dokument.
     *
     * @param onlineIdProUuid  Online-ID je zugeordneter Meldung mit Zeile
     * @param zusatzProUuid    zuletzt abgeglichene Besetzung, Teamname und Vermerke
     * @param rohStatusProUuid Online-Status aller gespeicherten Zuordnungen, auch ohne Zeile
     * @param gesperrt         Meldungen mit offenem bzw. abgelehntem Auftrag
     *                         ({@link PtmOnlineAuftraege#gesperrteMeldungen})
     */
    record Zuordnungen(Map<String, String> onlineIdProUuid, Map<String, ZuordnungsZusatz> zusatzProUuid,
            Map<String, String> rohStatusProUuid, Set<String> gesperrt) {

        Zuordnungen {
            onlineIdProUuid = Map.copyOf(onlineIdProUuid);
            zusatzProUuid = Map.copyOf(zusatzProUuid);
            rohStatusProUuid = Collections.unmodifiableMap(new HashMap<>(rohStatusProUuid));
            gesperrt = Set.copyOf(gesperrt);
        }

        private boolean istAbgeschlossen(String uuid) {
            return OnlineAnmeldeStatus.istBeendet(rohStatusProUuid.get(uuid));
        }

        private boolean istLokalEntfernt(String uuid) {
            return Optional.ofNullable(zusatzProUuid.get(uuid))
                    .map(zusatz -> ZuordnungsVermerke.lese(zusatz.vermerk()).hat(ZuordnungsVermerke.LOKAL_ENTFERNT))
                    .orElse(false);
        }
    }

    /** Zu erzeugender Auftrag. */
    sealed interface Aktion permits Aenderung, Anlage, Storno {
    }

    /** Besetzung und/oder Teamname einer zugeordneten Meldung haben sich lokal geändert. */
    record Aenderung(String uuid, String onlineId, NeueOnlineAnmeldung anmeldung, List<PersonDto> personen,
            AbgeglicheneBesetzung besetzung, String bezeichnung) implements Aktion {

        Aenderung {
            personen = List.copyOf(personen);
        }
    }

    /** Neue lokale Meldung ohne Online-Zuordnung. */
    record Anlage(LokaleMeldung meldung, NeueOnlineAnmeldung anmeldung, String bezeichnung) implements Aktion {
    }

    /** Lokal gelöschte, zugeordnete Meldung. */
    record Storno(String uuid, String onlineId) implements Aktion {
    }

    /**
     * Ergebnis eines Laufs.
     *
     * @param nachlauf gelöschte Meldungen warten auf ihre Bestätigung durch einen weiteren Lauf
     */
    record Ergebnis(List<Aktion> aktionen, boolean nachlauf) {

        Ergebnis {
            aktionen = List.copyOf(aktionen);
        }
    }

    /**
     * Liest Meldeliste und Zuordnungen und erzeugt die nötigen Aufträge im Bestand.
     *
     * @return ob ein weiterer Lauf nötig ist, um gelöschte Meldungen zu bestätigen oder eine Stornierung
     *         zurückzunehmen
     */
    boolean erfasse(AuftragsBestand bestand, PtmOnlineVerbindung verbindung) throws GenerateException {
        PtmOnlineRegistrationMapping mapping = verbindung.mapping();
        MeldelisteZiel ziel = verbindung.ziel();
        String tournamentId = verbindung.tournamentId();
        List<LokaleMeldung> meldungen = leseMeldungen(ziel);
        boolean ruecknahmeErledigt = PtmOnlineAuftraege.stornosZuruecknehmen(bestand,
                meldungen.stream().map(LokaleMeldung::uuid).collect(Collectors.toSet()));
        Zuordnungen zuordnungen = new Zuordnungen(mapping.getOnlineIdsProUuid(), mapping.getZusaetzeProUuid(),
                mapping.getRohStatusProUuid(), PtmOnlineAuftraege.gesperrteMeldungen(bestand));
        Ergebnis ergebnis = ermittle(tournamentId, ziel.getFormation(), ziel.hatVereinsspalte(),
                !PtmOnlineAuftraege.istGestartet(mapping.getOnlineStatus()), meldungen, zuordnungen);
        Map<String, Integer> revisionen = mapping.getExecutionRevisionenProUuid();
        for (Aktion aktion : ergebnis.aktionen()) {
            switch (aktion) {
                case Aenderung a -> PtmOnlineAuftraege.aenderung(bestand, tournamentId, a.uuid(), a.onlineId(),
                        a.anmeldung(), a.personen(), a.besetzung(),
                        PtmOnlineAuftraege.erwarteteRevision(bestand, a.uuid(), revisionen), a.bezeichnung());
                case Anlage a -> legeAn(bestand, tournamentId, ziel, a);
                case Storno s -> PtmOnlineAuftraege.storno(bestand, tournamentId, s.uuid(), s.onlineId(),
                        PtmOnlineAuftraege.erwarteteRevision(bestand, s.uuid(), revisionen));
            }
        }
        if (!ergebnis.aktionen().isEmpty()) {
            logger.info("PTM-Online: {} Meldelisten-Änderungen als Auftrag erfasst", ergebnis.aktionen().size());
        }
        return ergebnis.nachlauf() || !ruecknahmeErledigt;
    }

    private static void legeAn(AuftragsBestand bestand, String tournamentId, MeldelisteZiel ziel, Anlage anlage)
            throws GenerateException {
        LokaleMeldung meldung = anlage.meldung();
        OptionalInt setzposition = ziel.getSetzpositionAusZeile(meldung.zeile1Basiert());
        // Teilnahme inaktiv wie beim manuellen Abgleich: den Check-in meldet die Check-in-Erkennung.
        PtmOnlineAuftraege.anlage(bestand, tournamentId, meldung.uuid(), anlage.anmeldung(), OnlineTeilnahme.INAKTIV,
                setzposition.isPresent() ? setzposition.getAsInt() : null, anlage.bezeichnung(), true);
    }

    /** Alle Meldelistenzeilen mit lokaler UUID, in wenigen Blockzugriffen gelesen. */
    private static List<LokaleMeldung> leseMeldungen(MeldelisteZiel ziel) throws GenerateException {
        Map<Integer, List<MeldelisteSpielerDaten>> proZeile = PtmOnlineMeldungsInhalt.proZeile(
                ziel.leseAlleSpielerRoh());
        Map<Integer, String> uuidProZeile;
        try {
            uuidProZeile = ziel.leseLokaleUuids(proZeile.keySet());
        } catch (MeldelisteZiel.MeldelisteSchreibException e) {
            throw new GenerateException(e.getMessage());
        }
        Optional<Map<Integer, String>> teamnamen = ziel.leseTeamnamen();
        Optional<Set<Integer>> spieltagZeilen = ziel.zeilenDesAktivenSpieltags();
        List<LokaleMeldung> meldungen = new ArrayList<>();
        proZeile.forEach((zeile, spieler) -> {
            String uuid = uuidProZeile.get(zeile);
            if (uuid != null && !uuid.isBlank()) {
                meldungen.add(new LokaleMeldung(zeile, uuid, spieler,
                        teamnamen.map(namen -> namen.getOrDefault(zeile, "")).orElse(null),
                        spieltagZeilen.map(zeilen -> zeilen.contains(zeile)).orElse(true)));
            }
        });
        return meldungen;
    }

    /**
     * Vergleicht die Meldeliste mit den Zuordnungen (ohne Dokumentzugriff).
     *
     * @param vereinsspalte die Meldeliste führt den Verein; sonst geht der zuletzt abgeglichene Online-Verein zurück
     * @param anlageErlaubt das Online-Turnier läuft noch nicht – danach lehnt PTM-Online Anlagen ab
     */
    Ergebnis ermittle(String tournamentId, Formation formation, boolean vereinsspalte, boolean anlageErlaubt,
            List<LokaleMeldung> meldungen, Zuordnungen zuordnungen) {
        List<Aktion> aktionen = new ArrayList<>();
        Map<String, LokaleMeldung> proUuid = new LinkedHashMap<>();
        meldungen.forEach(meldung -> proUuid.putIfAbsent(meldung.uuid(), meldung));
        for (LokaleMeldung meldung : proUuid.values()) {
            if (zuordnungen.gesperrt().contains(meldung.uuid())) {
                continue;
            }
            String onlineId = zuordnungen.onlineIdProUuid().get(meldung.uuid());
            if (onlineId != null) {
                if (!zuordnungen.istAbgeschlossen(meldung.uuid())) {
                    aenderung(meldung, onlineId, formation, vereinsspalte,
                            zuordnungen.zusatzProUuid().get(meldung.uuid()))
                            .ifPresent(aktionen::add);
                }
            } else if (anlageErlaubt && meldung.amAktivenSpieltag()
                    && !zuordnungen.rohStatusProUuid().containsKey(meldung.uuid())) {
                aktionen.add(new Anlage(meldung, PtmOnlineMeldungsInhalt.anmeldung(formation, meldung.spieler(),
                        meldung.teamname()), PtmOnlineMeldungsInhalt.bezeichnung(meldung.spieler())));
            }
        }
        Ergebnis stornierungen = stornos(tournamentId, proUuid, zuordnungen);
        aktionen.addAll(stornierungen.aktionen());
        return new Ergebnis(aktionen, stornierungen.nachlauf());
    }

    /**
     * Zugeordnete Meldungen, deren Zeile in diesem und im vorigen Lauf fehlt. Liest die Meldeliste keine einzige
     * Meldung (Blatt neu aufgebaut, nicht lesbar), wird nichts storniert und die Bestätigung beginnt von vorn.
     * Offene Aufträge blockieren nur das Erzeugen, nicht das Merken der Löschung. Erst ein bestätigter Storno oder
     * eine entfernte Zuordnung beendet die Vormerkung, damit ein Revisionskonflikt erneut versucht werden kann.
     */
    private Ergebnis stornos(String tournamentId, Map<String, LokaleMeldung> proUuid, Zuordnungen zuordnungen) {
        if (proUuid.isEmpty()) {
            fehlend.remove(tournamentId);
            return new Ergebnis(List.of(), false);
        }
        Map<String, String> bisherGesehen = gesehen.getOrDefault(tournamentId, Map.of());
        Set<String> bisherFehlend = fehlend.getOrDefault(tournamentId, Set.of());
        Map<String, String> jetztGesehen = new LinkedHashMap<>();
        proUuid.keySet().forEach(uuid -> Optional.ofNullable(zuordnungen.onlineIdProUuid().get(uuid))
                .ifPresent(onlineId -> jetztGesehen.put(uuid, onlineId)));
        Set<String> jetztFehlend = new LinkedHashSet<>();
        List<Aktion> stornos = new ArrayList<>();
        boolean nachlauf = false;
        for (Map.Entry<String, String> eintrag : bisherGesehen.entrySet()) {
            String uuid = eintrag.getKey();
            if (proUuid.containsKey(uuid) || !zuordnungen.rohStatusProUuid().containsKey(uuid)
                    || zuordnungen.istLokalEntfernt(uuid) || zuordnungen.istAbgeschlossen(uuid)) {
                continue;
            }
            jetztGesehen.put(uuid, eintrag.getValue());
            jetztFehlend.add(uuid);
            if (zuordnungen.gesperrt().contains(uuid)) {
                continue;
            }
            if (bisherFehlend.contains(uuid)) {
                stornos.add(new Storno(uuid, eintrag.getValue()));
            } else {
                nachlauf = true;
            }
        }
        gesehen.put(tournamentId, jetztGesehen);
        fehlend.put(tournamentId, jetztFehlend);
        return new Ergebnis(stornos, nachlauf);
    }

    /** Änderungsauftrag, wenn Besetzung oder Teamname vom zuletzt abgeglichenen Stand abweichen. */
    private static Optional<Aktion> aenderung(LokaleMeldung meldung, String onlineId, Formation formation,
            boolean vereinsspalte, @Nullable ZuordnungsZusatz zusatz) {
        if (zusatz == null) {
            return Optional.empty();
        }
        String bezeichnung = PtmOnlineMeldungsInhalt.bezeichnung(meldung.spieler());
        Optional<String> zuletztTeamname = AbgeglichenerTeamname.lese(zusatz.teamname());
        if (formation == Formation.NUR_TEAMNAME) {
            // Der Teamname ist die Besetzung; die Online-Kontaktperson bleibt (PTM-Online übernimmt keine Personen).
            String teamname = AbgeglichenerTeamname.normalisiert(meldung.spieler().getFirst().vorname());
            if (zuletztTeamname.isEmpty() || zuletztTeamname.get().equals(teamname)) {
                return Optional.empty();
            }
            return Optional.of(new Aenderung(meldung.uuid(), onlineId,
                    PtmOnlineMeldungsInhalt.anmeldung(formation, meldung.spieler(), null), List.of(),
                    AbgeglicheneBesetzung.von(List.of(new AbgeglicheneBesetzung.Person(teamname, "", null))),
                    bezeichnung));
        }
        // Ohne abgeglichene Besetzung fehlen die Benutzer-IDs: Eine Änderung löste sonst alle Kontoverknüpfungen.
        AbgeglicheneBesetzung zuletzt = AbgeglicheneBesetzung.lese(zusatz.besetzung());
        if (zuletzt.istLeer()) {
            return Optional.empty();
        }
        List<AbgeglicheneBesetzung.Person> lokal = PtmOnlineMeldungsInhalt.personen(meldung.spieler());
        boolean besetzungGeaendert = !AbgeglicheneBesetzung.von(lokal).namensSchluessel()
                .equals(zuletzt.namensSchluessel());
        boolean teamnameGeaendert = meldung.teamname() != null && zuletztTeamname
                .filter(name -> !name.equals(AbgeglichenerTeamname.normalisiert(meldung.teamname()))).isPresent();
        if (!besetzungGeaendert && !teamnameGeaendert) {
            return Optional.empty();
        }
        // Ohne Teamname-Spalte geht der zuletzt abgeglichene zurück, sonst löschte PTM-Online den Online-Teamnamen.
        String teamname = meldung.teamname() != null ? meldung.teamname() : zuletztTeamname.orElse(null);
        return Optional.of(new Aenderung(meldung.uuid(), onlineId,
                PtmOnlineMeldungsInhalt.anmeldung(formation, meldung.spieler(), teamname,
                        PtmOnlineMeldungsInhalt.verein(meldung.spieler(), vereinsspalte, zuletzt)),
                zuletzt.fuerUebertragung(lokal), zuletzt.mitBenutzerIdsFuer(lokal), bezeichnung));
    }
}
