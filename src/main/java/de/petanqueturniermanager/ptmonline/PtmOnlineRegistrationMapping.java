/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineKonfliktSheet;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncSheet;
import de.petanqueturniermanager.onlinesync.speicher.NeueZuordnung;
import de.petanqueturniermanager.onlinesync.speicher.PtmOnlineSyncSpeicher;
import de.petanqueturniermanager.onlinesync.speicher.ZuordnungsAnzeige;
import de.petanqueturniermanager.onlinesync.speicher.ZuordnungsZusatz;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationAnswerDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationFeeDto;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/**
 * Bindeglied zum gespeicherten Zustand ({@link PtmOnlineSyncSpeicher}) und zum Info-Blatt „PTMOnline Sync“
 * ({@link PtmOnlineSyncSheet}) einer Online-Turnier-Verbindung. Bei
 * Supermelee (mehrere Spieltage) gilt {@code spieltagNrOderNull} — jeder Spieltag hat seine eigene
 * Verbindung/sein eigenes Blatt; für alle anderen Turniersysteme ist er {@code null} (eine Verbindung pro
 * Dokument).
 */
public class PtmOnlineRegistrationMapping {

    private static final Logger LOGGER = LogManager.getLogger(PtmOnlineRegistrationMapping.class);

    private final WorkingSpreadsheet ws;
    private final @Nullable Integer spieltagNr;
    private final PtmOnlineSyncSpeicher speicher;
    private final PtmOnlineSyncSheet syncSheet;
    private final PtmOnlineKonfliktSheet konfliktSheet;
    /**
     * Zuordnungen dieses Laufs, die nicht in einer Nr-Zelle stehen können, weil es die lokale Zeile nicht (mehr)
     * gibt – vom Server ({@link #ergaenzeAusServer}) oder gerade geschrieben.
     */
    private final Map<String, String> ohneZeile = new LinkedHashMap<>();

    public PtmOnlineRegistrationMapping(WorkingSpreadsheet ws, TurnierSystem turnierSystem, Integer spieltagNrOderNull) {
        this.ws = ws;
        this.spieltagNr = spieltagNrOderNull;
        this.speicher = new PtmOnlineSyncSpeicher(ws, spieltagNrOderNull);
        this.syncSheet = new PtmOnlineSyncSheet(ws, turnierSystem, spieltagNrOderNull);
        this.konfliktSheet = new PtmOnlineKonfliktSheet(ws, turnierSystem);
    }

    /** Gesammelte Konfliktliste des Abgleichs mit den Entscheidungen der Turnierleitung (A-29). */
    public PtmOnlineKonfliktSheet konfliktListe() {
        return konfliktSheet;
    }

    /**
     * Speichert die Verbindungsdaten und baut das optionale Info-Blatt neu auf (nur mit eingeschalteter Option) –
     * einmalig beim Verbinden, synchron innerhalb eines laufenden SheetRunners. Wechselt das Online-Turnier, wird die
     * bisherige Zuordnung geleert.
     */
    public void verbinden(OnlineTournamentDto turnier, SyncBindingDto binding, String leaseToken)
            throws GenerateException {
        speicher.verbinden(turnier, binding.syncDocumentId(), leaseToken);
        syncSheet.nachEinstellungAufbauen();
    }

    /**
     * Gleicht das optionale Info-Blatt mit der Turnier-Option ab: Eingeschaltet wird ein fehlendes Blatt angelegt,
     * ausgeschaltet ein vorhandenes entfernt. Läuft synchron und ist für Aufrufer innerhalb eines laufenden
     * SheetRunners gedacht (Meldungsabgleich).
     */
    public void sicherstellen() throws GenerateException {
        syncSheet.nachEinstellungAnlegen();
    }

    /**
     * Leert den gespeicherten Zustand samt geladenem Auftragsbestand und entfernt das Info-Blatt (Gegenstück zu
     * {@link #verbinden}).
     */
    public void trennen() throws GenerateException {
        PtmOnlineAuftraege.getrennt(ws.getWorkingSpreadsheetDocument(), spieltagNr);
        speicher.trennen();
        syncSheet.entfernen();
    }

    // ── Zuordnung lokale Meldung ↔ Online-Anmeldung ─────────────────────────
    // Die Online-ID steht in der Nr-Zelle der Meldung (NrZellFormel); der Speicher führt je lokaler UUID nur
    // Anzeige, Revision und Vermerke. Gezählt werden nur Meldungen mit einer Zuordnung dieser Verbindung –
    // bei Supermelee trägt die Nr-Zelle sonst womöglich die ID eines anderen Spieltags.

    public void addMapping(String lokaleUuid, String onlineRegistrationId, int executionRevision,
            String lokaleBezeichnung, String onlineStatus) throws GenerateException {
        addMappings(List.of(new NeueZuordnung(lokaleUuid, onlineRegistrationId, executionRevision,
                lokaleBezeichnung, onlineStatus, "", "", "", "", "", "")));
    }

    public Optional<String> getOnlineId(String lokaleUuid) throws GenerateException {
        return Optional.ofNullable(getOnlineIdsProUuid().get(lokaleUuid));
    }

    /** Ob {@code onlineRegistrationId} bereits einer lokalen Meldung zugeordnet ist (bereits importiert). */
    public boolean istBereitsImportiert(String onlineRegistrationId) throws GenerateException {
        return getImportierteOnlineIds().contains(onlineRegistrationId);
    }

    /** Ob dem Online-Turnier bereits Meldungen zugeordnet sind – liest nur den gespeicherten Zustand. */
    public boolean hatZuordnungen() throws GenerateException {
        return !speicher.getLokaleUuids().isEmpty();
    }

    /** Alle bereits zugeordneten Online-IDs – einmal lesen statt {@link #istBereitsImportiert} je Anmeldung. */
    public Set<String> getImportierteOnlineIds() throws GenerateException {
        return new LinkedHashSet<>(getOnlineIdsProUuid().values());
    }

    /** Entfernt die Zuordnungen dieser lokalen UUIDs; die Online-Anmeldungen gelten danach als nicht importiert. */
    public void entferneZuordnungen(Set<String> lokaleUuids) throws GenerateException {
        speicher.entferneZuordnungen(lokaleUuids);
        Map<String, Optional<String>> ohneId = new LinkedHashMap<>();
        lokaleUuids.forEach(uuid -> ohneId.put(uuid, Optional.empty()));
        setzeInNrZellen(ohneId);
        lokaleUuids.forEach(ohneZeile::remove);
    }

    public Optional<String> getLokaleUuid(String onlineRegistrationId) throws GenerateException {
        return getOnlineIdsProUuid().entrySet().stream().filter(e -> e.getValue().equals(onlineRegistrationId))
                .map(Map.Entry::getKey).findFirst();
    }

    /**
     * Ergänzt die Zuordnung für Meldungen, deren lokale Zeile fehlt (gelöscht), aus den Online-Anmeldungen: Der
     * Server kennt zu jeder abgeglichenen Anmeldung die lokale UUID. Gilt für die Lebensdauer dieses Objekts (ein
     * Lauf); Nr-Zellen haben Vorrang.
     */
    public void ergaenzeAusServer(Collection<RegistrationDto> registrations) throws GenerateException {
        Map<String, String> ausServer = new LinkedHashMap<>();
        for (RegistrationDto registration : registrations) {
            if (registration.localRegistrationUuid() != null) {
                ausServer.putIfAbsent(registration.localRegistrationUuid(), registration.id());
            }
        }
        ergaenzeOhneZeile(ausServer);
    }

    /**
     * Wie {@link #ergaenzeAusServer}, aber aus noch nicht angenommenen Verknüpfungs- und Änderungsaufträgen dieser
     * Bindung: Wurde die Zeile gelöscht, bevor PTM-Online die lokale UUID erfahren hat, kennt sie nur der Auftrag.
     */
    public void ergaenzeAusOffenenAuftraegen(AuftragsBestand bestand) throws GenerateException {
        ergaenzeOhneZeile(PtmOnlineAuftraege.offeneVerknuepfungen(bestand));
    }

    private void ergaenzeOhneZeile(Map<String, String> onlineIdProUuid) throws GenerateException {
        Set<String> zugeordnet = speicher.getLokaleUuids();
        Map<String, String> inNrZellen = onlineIdsAusNrZellen(zugeordnet);
        onlineIdProUuid.forEach((uuid, onlineId) -> {
            if (zugeordnet.contains(uuid) && !inNrZellen.containsKey(uuid)) {
                ohneZeile.putIfAbsent(uuid, onlineId);
            }
        });
    }

    /** Ob die zuletzt übernommene Online-Anmeldung dieser lokalen Meldung online storniert ist. */
    public boolean istOnlineStorniert(String lokaleUuid) throws GenerateException {
        return speicher.getRohStatus(lokaleUuid).map(OnlineAnmeldeStatus::istStorniert).orElse(false);
    }

    /** Hängt eine bereits zugeordnete lokale Meldung auf eine andere Online-Anmeldung um. */
    public void ersetzeOnlineId(String lokaleUuid, String onlineRegistrationId, int executionRevision)
            throws GenerateException {
        if (!speicher.getLokaleUuids().contains(lokaleUuid)) {
            throw new GenerateException(I18n.get("ptmonline.fehler.keine_zuordnung", lokaleUuid));
        }
        setzeInNrZellen(Map.of(lokaleUuid, Optional.of(onlineRegistrationId)));
        speicher.setExecutionRevisionen(Map.of(lokaleUuid, executionRevision));
    }

    /** Online-ID je lokaler UUID aller Zuordnungen – einmal lesen statt {@link #getOnlineId} je Meldung. */
    public Map<String, String> getOnlineIdsProUuid() throws GenerateException {
        Set<String> zugeordnet = speicher.getLokaleUuids();
        Map<String, String> ergebnis = onlineIdsAusNrZellen(zugeordnet);
        ohneZeile.forEach((uuid, id) -> {
            if (zugeordnet.contains(uuid)) {
                ergebnis.putIfAbsent(uuid, id);
            }
        });
        return ergebnis;
    }

    private Map<String, String> onlineIdsAusNrZellen(Set<String> zugeordnet) {
        Map<String, String> ergebnis = new LinkedHashMap<>();
        ziel().ifPresent(ziel -> ziel.leseOnlineIdsProUuid().forEach((uuid, id) -> {
            if (zugeordnet.contains(uuid)) {
                ergebnis.put(uuid, id);
            }
        }));
        return ergebnis;
    }

    /** Schreibt Online-IDs in die Nr-Zellen; was dort keine Zeile hat, merkt sich der Lauf. */
    private void setzeInNrZellen(Map<String, Optional<String>> onlineIdProUuid) throws GenerateException {
        Set<String> geschrieben = Set.of();
        Optional<MeldelisteZiel> ziel = ziel();
        if (ziel.isPresent()) {
            try {
                geschrieben = ziel.get().setzeOnlineIds(onlineIdProUuid);
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                LOGGER.error("PTM-Online-IDs konnten nicht in die Nr-Zellen geschrieben werden", e);
                throw new GenerateException(e.getMessage());
            }
        }
        for (Map.Entry<String, Optional<String>> eintrag : onlineIdProUuid.entrySet()) {
            if (geschrieben.contains(eintrag.getKey()) || eintrag.getValue().isEmpty()) {
                ohneZeile.remove(eintrag.getKey());
            } else {
                ohneZeile.put(eintrag.getKey(), eintrag.getValue().get());
            }
        }
    }

    /** Liste, deren Nr-Zellen die Meldungen dieser Verbindung tragen (Meldeliste oder Mêlée-Anmeldung). */
    private Optional<MeldelisteZiel> ziel() {
        return MeldelisteZielFactory.fuerPtmOnline(ws);
    }

    /** Lokale Bezeichnung je lokaler UUID – auch für Meldungen, deren Zeile es nicht mehr gibt. */
    public Map<String, String> getBezeichnungenProUuid() throws GenerateException {
        return speicher.getBezeichnungenProUuid();
    }

    /** Roher Online-Status je lokaler UUID, wie ihn der letzte Abgleich gesehen hat. */
    public Map<String, String> getRohStatusProUuid() throws GenerateException {
        return speicher.getRohStatusProUuid();
    }

    public Map<String, Integer> getExecutionRevisionenProUuid() throws GenerateException {
        return speicher.getExecutionRevisionenProUuid();
    }

    /** Setzt die Ausführungsrevisionen mehrerer Zuordnungen in einem Schreibzugriff. */
    public void setExecutionRevisionen(Map<String, Integer> revisionProUuid) throws GenerateException {
        speicher.setExecutionRevisionen(revisionProUuid);
    }

    public void setBezeichnung(String lokaleUuid, String lokaleBezeichnung, String onlineStatus)
            throws GenerateException {
        speicher.setBezeichnung(lokaleUuid, lokaleBezeichnung, onlineStatus);
    }

    /** Schreibt Tarife und Anmeldefragen lesbar sowie den unübersetzten Anmeldestatus (Storno-Erkennung). */
    public void setOnlineDetails(String lokaleUuid, RegistrationDto registration) throws GenerateException {
        speicher.setOnlineDetails(lokaleUuid, onlineDetails(registration));
    }

    /**
     * Neue Zuordnung zu einer Online-Anmeldung samt lesbarer Details, zum gesammelten Schreiben per
     * {@link #addMappings}.
     */
    public static NeueZuordnung neueZuordnung(String lokaleUuid, String lokaleBezeichnung,
            RegistrationDto registration) {
        return neueZuordnung(lokaleUuid, lokaleBezeichnung, registration,
                AbgeglicheneBesetzung.ausOnline(registration.personen(), registration.club()));
    }

    /** @param besetzung Online-Besetzung, wie sie in der Meldeliste steht (bei „Nur Teamname“ der Teamname) */
    public static NeueZuordnung neueZuordnung(String lokaleUuid, String lokaleBezeichnung,
            RegistrationDto registration, AbgeglicheneBesetzung besetzung) {
        ZuordnungsAnzeige.OnlineDetails details = onlineDetails(registration);
        return new NeueZuordnung(lokaleUuid, registration.id(),
                registration.executionRevision() == null ? 1 : registration.executionRevision(), lokaleBezeichnung,
                OnlineAnmeldeStatus.anzeige(registration.status()), details.tarife(), details.fragen(),
                details.rohStatus(), besetzung.alsText(), "", AbgeglichenerTeamname.alsText(registration.teamName()));
    }

    /** Zuletzt abgeglichene Besetzung, Teamname und Vermerke je lokaler UUID (T-15, T-17). */
    public Map<String, ZuordnungsZusatz> getZusaetzeProUuid() throws GenerateException {
        return speicher.getZusaetzeProUuid();
    }

    /** Setzt Besetzung und/oder Vermerke mehrerer Zuordnungen in einem Schreibzugriff. */
    public void setZusaetze(Map<String, ZuordnungsZusatz> zusatzProUuid) throws GenerateException {
        speicher.setZusaetze(zusatzProUuid);
    }

    /**
     * Speichert mehrere neue Zuordnungen in einem Zugriff (siehe {@link PtmOnlineSyncSpeicher#addMappings}). Eine
     * lokale Meldung, die schon eine Online-Anmeldung hat, behält sie. Ist die Meldung bereits zugeordnet, fehlt aber
     * ihre Online-ID (z.&nbsp;B. Nr-Zelle überschrieben), wird nur die ID samt Ausführungsrevision nachgetragen.
     */
    public void addMappings(List<NeueZuordnung> zuordnungen) throws GenerateException {
        Set<String> bereitsZugeordnet = speicher.getLokaleUuids();
        Map<String, String> bestehendeIds = getOnlineIdsProUuid();
        speicher.addMappings(zuordnungen);
        Map<String, Optional<String>> neueIds = new LinkedHashMap<>();
        Map<String, Integer> nachgetrageneRevisionen = new LinkedHashMap<>();
        for (NeueZuordnung zuordnung : zuordnungen) {
            String uuid = zuordnung.lokaleUuid();
            if (bestehendeIds.containsKey(uuid) || neueIds.containsKey(uuid)) {
                continue;
            }
            neueIds.put(uuid, Optional.of(zuordnung.onlineId()));
            if (bereitsZugeordnet.contains(uuid)) {
                nachgetrageneRevisionen.put(uuid, Math.max(1, zuordnung.executionRevision()));
            }
        }
        setzeInNrZellen(neueIds);
        if (!nachgetrageneRevisionen.isEmpty()) {
            speicher.setExecutionRevisionen(nachgetrageneRevisionen);
        }
    }

    /**
     * Aktualisiert Bezeichnung, Status und – sofern die Online-Anmeldung bekannt ist – die Details mehrerer
     * Zuordnungen in einem Zugriff.
     *
     * @param bezeichnungProUuid   lokale Bezeichnung je lokaler UUID
     * @param registrationProUuid  aktuelle Online-Anmeldung je lokaler UUID; fehlt sie, wird der Status geleert
     */
    public void aktualisiereAnzeigen(Map<String, String> bezeichnungProUuid,
            Map<String, RegistrationDto> registrationProUuid) throws GenerateException {
        Map<String, ZuordnungsAnzeige> anzeigen = new LinkedHashMap<>();
        bezeichnungProUuid.forEach((uuid, bezeichnung) -> {
            RegistrationDto registration = registrationProUuid.get(uuid);
            anzeigen.put(uuid, registration == null
                    ? new ZuordnungsAnzeige(bezeichnung, "", Optional.empty())
                    : new ZuordnungsAnzeige(bezeichnung, OnlineAnmeldeStatus.anzeige(registration.status()),
                            Optional.of(onlineDetails(registration))));
        });
        speicher.setAnzeigen(anzeigen);
    }

    private static ZuordnungsAnzeige.OnlineDetails onlineDetails(RegistrationDto registration) {
        String tarife = registration.feeSelections() == null ? "" : registration.feeSelections().stream()
                .map(PtmOnlineRegistrationMapping::tarifText).filter(text -> !text.isBlank())
                .collect(Collectors.joining(" | "));
        String fragen = registration.registrationAnswers() == null ? "" : registration.registrationAnswers().stream()
                .map(PtmOnlineRegistrationMapping::frageText).filter(text -> !text.isBlank())
                .collect(Collectors.joining(" | "));
        return new ZuordnungsAnzeige.OnlineDetails(tarife, fragen, registration.status());
    }

    private static String tarifText(RegistrationFeeDto tarif) {
        if (tarif == null) return "";
        String betrag = tarif.amountCents() == null ? "" : String.format(Locale.ROOT, "%.2f EUR", tarif.amountCents() / 100.0);
        return (String.valueOf(tarif.name() == null ? "" : tarif.name()).strip() + (betrag.isBlank() ? "" : ": " + betrag)).strip();
    }

    private static String frageText(RegistrationAnswerDto antwort) {
        if (antwort == null) return "";
        String frage = antwort.questionLabel() == null ? antwort.questionId() : antwort.questionLabel();
        String teilnehmer = switch (antwort.participant() == null ? "" : antwort.participant()) {
            case "primary" -> I18n.get("ptmonline.teilnehmer.spieler1");
            case "partner" -> I18n.get("ptmonline.teilnehmer.partner");
            case "partner2" -> I18n.get("ptmonline.teilnehmer.partner2");
            default -> "";
        };
        return (String.valueOf(frage == null ? "" : frage).strip()
                + (teilnehmer.isBlank() ? "" : " (" + teilnehmer + ")")).strip();
    }

    /** Pausiert: kein Meldungsabgleich und kein Rundenstart-Sync; die Verbindung selbst bleibt bestehen. */
    public boolean istPausiert() throws GenerateException {
        return speicher.istPausiert();
    }

    public void setPausiert(boolean pausiert) throws GenerateException {
        speicher.setPausiert(pausiert);
    }

    /** Archiviert die getrennte Verbindung: Zustand und Blatt bleiben zur Einsicht, gelten aber als nicht verbunden. */
    public void archivieren() throws GenerateException {
        speicher.archivieren();
    }

    /** @see PtmOnlineSyncSpeicher#getOnlineStatus() */
    public Optional<String> getOnlineStatus() throws GenerateException {
        return speicher.getOnlineStatus();
    }

    /** @see PtmOnlineSyncSpeicher#setOnlineStatus(String) */
    public void setOnlineStatus(String onlineStatus) throws GenerateException {
        speicher.setOnlineStatus(onlineStatus);
    }

    public void setLastSync(Instant zeitpunkt) throws GenerateException {
        speicher.setLastSync(zeitpunkt);
    }

    public Optional<Instant> getLastSync() throws GenerateException {
        return speicher.getLastSync();
    }

    /** @see PtmOnlineSyncSpeicher#getAbrufCursor() */
    public Optional<Instant> getAbrufCursor() throws GenerateException {
        return speicher.getAbrufCursor();
    }

    public void setAbrufCursor(Instant cursor) throws GenerateException {
        speicher.setAbrufCursor(cursor);
    }

    public Optional<String> getTournamentId() throws GenerateException {
        return speicher.getTournamentId();
    }

    public Optional<String> getSyncDocumentId() throws GenerateException {
        return speicher.getSyncDocumentId();
    }

    public Optional<String> getLeaseToken() throws GenerateException {
        return speicher.getLeaseToken();
    }

    /** Schreibzähler und Auftragspuffer der Bindung, wie mit dem Dokument gespeichert. */
    public AuftragsBestand leseAuftragsBestand() throws GenerateException {
        return speicher.leseAuftragsBestand();
    }

    /** Speichert Schreibzähler und Auftragspuffer; nur im Dokument-Kontext (SheetRunner). */
    public void schreibeAuftragsBestand(AuftragsBestand bestand) throws GenerateException {
        speicher.schreibeAuftragsBestand(bestand);
    }

    public Optional<String> getConnectRequestId() throws GenerateException {
        return speicher.getConnectRequestId();
    }

    public void setConnectRequestId(String connectRequestId) throws GenerateException {
        speicher.setConnectRequestId(connectRequestId);
    }

    /** Lokaler Turnierstart, dessen Übergang zu {@code running} online noch aussteht (KP-05). */
    public Optional<Instant> getRunningAusstehendSeit() throws GenerateException {
        return speicher.getRunningAusstehendSeit();
    }

    public void setRunningAusstehendSeit(Instant lokalerStart) throws GenerateException {
        speicher.setRunningAusstehendSeit(lokalerStart);
    }

    /** @see PtmOnlineSyncSpeicher#getLetzterRundenstart() */
    public Optional<Instant> getLetzterRundenstart() throws GenerateException {
        return speicher.getLetzterRundenstart();
    }

    /** @see PtmOnlineSyncSpeicher#setLetzterRundenstart(Instant) */
    public void setLetzterRundenstart(Instant rundenstart) throws GenerateException {
        speicher.setLetzterRundenstart(rundenstart);
    }

    public void protokolliereOfflineBestaetigung(Instant zeitpunkt, Instant letzterSync) throws GenerateException {
        speicher.protokolliereOfflineBestaetigung(zeitpunkt, letzterSync);
    }
}
