/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.speicher;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;

/**
 * Zustand einer PTM-Online-Verbindung in den benutzerdefinierten Dokument-Properties: Verbindungsdaten, Status,
 * Schreibzähler und Auftragspuffer sowie die Zuordnung lokale Meldung ↔ Online-Anmeldung.
 * <p>
 * Properties statt Blattzellen, weil der Hintergrund-Sync diesen Zustand sehr oft schreibt: Zellen eines im
 * Turnier-Modus gesperrten Blatts bräuchten dafür jedes Mal einen Blattschutz-Scope, der alle Blätter entsperrt und
 * wieder sperrt. Das Blatt „PTMOnline Sync“ zeigt die Kopfwerte nur noch per {@code PTM.ALG.STRINGPROPERTY} an.
 * <p>
 * Bei Supermelee gibt es eine Verbindung pro Spieltag ({@code spieltagNr}), sonst genau eine pro Dokument. Geschrieben
 * wird ohne {@code TurnierEvent} ({@link DocumentPropertiesHelper#setStringPropertyOhneEvent}): Der Sync läuft in
 * Hintergrund-Threads, ein Event würde dort VCL-Arbeit der Sidebar auslösen. Gelesen wird immer im übergebenen
 * Dokument, nie fokus-basiert.
 */
public final class PtmOnlineSyncSpeicher {

    private static final Logger LOGGER = LogManager.getLogger(PtmOnlineSyncSpeicher.class);

    private static final String PRAEFIX = "PTMOnline.";
    private static final String SPIELTAG_PRAEFIX = PRAEFIX + "Spieltag";
    private static final int MAX_EINTRAEGE = MeldungenSpalte.MAX_ANZ_MELDUNGEN;

    /** Gespeicherte Werte einer Verbindung; der Property-Name ist Präfix + {@link #name}. */
    public enum Feld {
        TURNIER_NAME("TurnierName"),
        TURNIER_ID("TurnierId"),
        LETZTER_SYNC("LetzterSync"),
        SYNC_DOKUMENT("SyncDokument"),
        SCHLUESSEL("Schluessel"),
        /** Sprachneutraler Marker, siehe {@link SyncStatus}. */
        SYNC_STATUS("SyncStatus"),
        /** Übersetzter Sync-Status, nur für die Anzeige im Blatt. */
        SYNC_STATUS_ANZEIGE("SyncStatusAnzeige"),
        /** Status des Online-Turniers, sprachneutral wie PTM-Online ihn meldet. */
        ONLINE_STATUS("OnlineStatus"),
        /** Übersetzter Status des Online-Turniers, nur für die Anzeige im Blatt. */
        ONLINE_STATUS_ANZEIGE("OnlineStatusAnzeige"),
        SCHREIBZAEHLER("Schreibzaehler"),
        CONNECT_ID("ConnectId"),
        RUNNING_AUSSTEHEND("RunningAusstehend"),
        OFFLINE_BESTAETIGT("OfflineBestaetigt"),
        ABRUF_CURSOR("AbrufCursor"),
        LETZTER_RUNDENSTART("LetzterRundenstart"),
        AUFTRAEGE("Auftraege"),
        ZUORDNUNGEN("Zuordnungen");

        private final String name;

        Feld(String name) {
            this.name = name;
        }

        /** Property-Name dieses Werts für die Verbindung des Dokuments bzw. des Spieltags. */
        public String propertyName(@Nullable Integer spieltagNrOderNull) {
            return praefix(spieltagNrOderNull) + name;
        }
    }

    /** Zustand der Verbindung, gespeichert als sprachneutraler Marker. */
    private enum SyncStatus {
        AKTIV("", "ptmonline.sheet.sync.aktiv"),
        PAUSIERT("PAUSIERT", "ptmonline.sheet.sync.pausiert"),
        ARCHIVIERT("GETRENNT", "ptmonline.sheet.sync.getrennt");

        private final String marker;
        private final String anzeigeSchluessel;

        SyncStatus(String marker, String anzeigeSchluessel) {
            this.marker = marker;
            this.anzeigeSchluessel = anzeigeSchluessel;
        }

        static SyncStatus ausMarker(String marker) {
            for (SyncStatus status : values()) {
                if (status.marker.equals(marker)) {
                    return status;
                }
            }
            return AKTIV;
        }
    }

    private final DocumentPropertiesHelper properties;
    private final @Nullable Integer spieltagNr;

    public PtmOnlineSyncSpeicher(WorkingSpreadsheet ws, @Nullable Integer spieltagNrOderNull) {
        this(new DocumentPropertiesHelper(ws), spieltagNrOderNull);
    }

    private PtmOnlineSyncSpeicher(DocumentPropertiesHelper properties, @Nullable Integer spieltagNrOderNull) {
        this.properties = properties;
        this.spieltagNr = spieltagNrOderNull;
    }

    private static String praefix(@Nullable Integer spieltagNrOderNull) {
        return spieltagNrOderNull == null ? PRAEFIX : SPIELTAG_PRAEFIX + spieltagNrOderNull + ".";
    }

    // ── Verbinden, Trennen, Status ────────────────────────────────────────────

    /** Speichert die Verbindungsdaten. Wechselt das Online-Turnier, wird die Zuordnung geleert. */
    public void verbinden(OnlineTournamentDto turnier, String syncDokumentId, String leaseToken) {
        Optional<String> bisherigesTurnier = optional(lese(Feld.TURNIER_ID));
        if (bisherigesTurnier.isPresent() && !bisherigesTurnier.get().equals(turnier.id)) {
            leeren();
        }
        schreibe(Feld.TURNIER_NAME, turnier.name);
        schreibe(Feld.TURNIER_ID, turnier.id);
        schreibe(Feld.LETZTER_SYNC, "");
        schreibe(Feld.SYNC_DOKUMENT, syncDokumentId);
        schreibe(Feld.SCHLUESSEL, leaseToken);
        schreibe(Feld.ABRUF_CURSOR, "");
        schreibeStatus(SyncStatus.AKTIV);
        schreibeOnlineStatus(StringUtils.strip(StringUtils.defaultString(turnier.status)));
    }

    /** Leert alle Werte der Verbindung (Gegenstück zu {@link #verbinden}); die Verbindung gilt danach als nie angelegt. */
    public void trennen() {
        for (Feld feld : Feld.values()) {
            if (!lese(feld).isEmpty()) {
                schreibe(feld, "");
            }
        }
    }

    /** Online-Turnier der aktiven Verbindung; leer ohne Verbindung und bei archivierter Verbindung. */
    public Optional<String> getTournamentId() {
        if (leseSyncStatus() == SyncStatus.ARCHIVIERT) {
            return Optional.empty();
        }
        return optional(lese(Feld.TURNIER_ID));
    }

    /** Ob je eine Verbindung angelegt wurde (auch archiviert). */
    private boolean istAngelegt() {
        return !lese(Feld.TURNIER_ID).isBlank();
    }

    public Optional<String> getSyncDocumentId() {
        return optional(lese(Feld.SYNC_DOKUMENT));
    }

    public Optional<String> getLeaseToken() {
        return optional(lese(Feld.SCHLUESSEL));
    }

    public Optional<Instant> getLastSync() {
        return zeitpunkt(Feld.LETZTER_SYNC);
    }

    public void setLastSync(@Nullable Instant zeitpunkt) {
        schreibe(Feld.LETZTER_SYNC, zeitpunkt == null ? "" : zeitpunkt.toString());
    }

    /**
     * Server-Zeitstempel für den nächsten inkrementellen Anmeldeabruf ({@code since}). Getrennt vom angezeigten
     * letzten Abgleich, weil er von der Server-Uhr stammt; leer heißt: alle Anmeldungen abrufen.
     */
    public Optional<Instant> getAbrufCursor() {
        return zeitpunkt(Feld.ABRUF_CURSOR);
    }

    public void setAbrufCursor(@Nullable Instant cursor) {
        schreibe(Feld.ABRUF_CURSOR, cursor == null ? "" : cursor.toString());
    }

    /** Zuletzt bekannter Status des Online-Turniers ({@code draft}, {@code registration}, …), leer wenn unbekannt. */
    public Optional<String> getOnlineStatus() {
        return optional(lese(Feld.ONLINE_STATUS));
    }

    /**
     * Hält den Status des Online-Turniers fest, sprachneutral und übersetzt für die Anzeige. Ein leerer Status und
     * ein Status ohne angelegte Verbindung werden ignoriert.
     */
    public void setOnlineStatus(@Nullable String onlineStatus) {
        String status = StringUtils.strip(StringUtils.defaultString(onlineStatus));
        if (!status.isEmpty() && istAngelegt()) {
            schreibeOnlineStatus(status);
        }
    }

    private void schreibeOnlineStatus(String status) {
        schreibe(Feld.ONLINE_STATUS, status);
        schreibe(Feld.ONLINE_STATUS_ANZEIGE, status.isEmpty() ? "" : PtmOnlineSyncStatus.statusAnzeige(status));
    }

    /** Pausiert: kein Meldungsabgleich, kein Rundenstart-Sync. Die Verbindung selbst bleibt bestehen. */
    public boolean istPausiert() {
        return leseSyncStatus() == SyncStatus.PAUSIERT;
    }

    public void setPausiert(boolean pausiert) {
        schreibeStatus(pausiert ? SyncStatus.PAUSIERT : SyncStatus.AKTIV);
    }

    /**
     * Archiviert die Verbindung nach dem Trennen: Verbindungsdaten und Zuordnung bleiben zur Einsicht stehen, die
     * Verbindung gilt aber als nicht verbunden. Ein erneutes {@link #verbinden} hebt das Archiv auf.
     */
    public void archivieren() {
        schreibeStatus(SyncStatus.ARCHIVIERT);
    }

    private SyncStatus leseSyncStatus() {
        return SyncStatus.ausMarker(lese(Feld.SYNC_STATUS));
    }

    private void schreibeStatus(SyncStatus status) {
        schreibe(Feld.SYNC_STATUS, status.marker);
        schreibe(Feld.SYNC_STATUS_ANZEIGE, I18n.get(status.anzeigeSchluessel));
    }

    /**
     * Liest den Verbindungszustand rein lesend – auch für Anzeigen auf dem Main-Thread (Sidebar). Maßgeblich ist
     * das Dokument {@code ws}, nicht der UI-Fokus.
     */
    public static PtmOnlineSyncStatus leseStatus(WorkingSpreadsheet ws, @Nullable Integer spieltagNrOderNull) {
        PtmOnlineSyncSpeicher speicher = new PtmOnlineSyncSpeicher(ws, spieltagNrOderNull);
        if (!speicher.istAngelegt()) {
            return PtmOnlineSyncStatus.NICHT_VERBUNDEN;
        }
        SyncStatus status = speicher.leseSyncStatus();
        return new PtmOnlineSyncStatus(status != SyncStatus.ARCHIVIERT, status == SyncStatus.PAUSIERT,
                speicher.getLastSync(), speicher.getOnlineStatus());
    }

    /** Spieltage, für die je eine Verbindung angelegt wurde (auch archivierte), aufsteigend. */
    public static List<Integer> angelegteSpieltage(WorkingSpreadsheet ws) {
        DocumentPropertiesHelper properties = new DocumentPropertiesHelper(ws);
        String endung = "." + Feld.TURNIER_ID.name;
        return properties.getPropertyNamenMitPraefix(SPIELTAG_PRAEFIX).stream()
                .filter(name -> name.endsWith(endung))
                .filter(name -> !properties.getStringProperty(name, "").isBlank())
                .map(name -> name.substring(SPIELTAG_PRAEFIX.length(), name.length() - endung.length()))
                .map(PtmOnlineSyncSpeicher::spieltagNr).flatMap(Optional::stream).sorted().toList();
    }

    private static Optional<Integer> spieltagNr(String text) {
        try {
            return Optional.of(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            LOGGER.debug("PTM-Online: keine Spieltag-Nummer in Property '{}'", text, e);
            return Optional.empty();
        }
    }

    // ── Schreibzähler und Auftragspuffer ──────────────────────────────────────

    /** Liest Schreibzähler und Auftragspuffer; unlesbare Einträge werden übersprungen und protokolliert. */
    public AuftragsBestand leseAuftragsBestand() {
        return AuftragsBestand.aus(zaehler(), PtmOnlineSyncJson.auftraegeAusJson(lese(Feld.AUFTRAEGE)));
    }

    /** Speichert Zähler und Auftragspuffer. */
    public void schreibeAuftragsBestand(AuftragsBestand bestand) throws GenerateException {
        List<AuftragsBestand.Eintrag> eintraege = bestand.eintraege();
        if (eintraege.size() > MAX_EINTRAEGE) {
            throw new GenerateException(I18n.get("ptmonline.fehler.auftragspuffer_voll"));
        }
        schreibe(Feld.SCHREIBZAEHLER, Long.toString(bestand.zaehler()));
        schreibe(Feld.AUFTRAEGE, eintraege.isEmpty() ? "" : PtmOnlineSyncJson.auftraegeAlsJson(eintraege));
        bestand.gespeichert();
    }

    private long zaehler() {
        String wert = lese(Feld.SCHREIBZAEHLER);
        try {
            return wert.isBlank() ? 0 : Long.parseLong(wert);
        } catch (NumberFormatException e) {
            LOGGER.warn("PTM-Online: ungültiger Schreibzähler '{}', es zählen die gespeicherten Aufträge", wert, e);
            return 0;
        }
    }

    /** Stabile ID des laufenden Verbindungsversuchs; vor dem Senden gespeichert, damit eine Wiederholung sie trägt. */
    public Optional<String> getConnectRequestId() {
        return optional(lese(Feld.CONNECT_ID));
    }

    public void setConnectRequestId(@Nullable String connectRequestId) {
        schreibe(Feld.CONNECT_ID, connectRequestId);
    }

    /** Lokaler Startzeitpunkt, solange der Übergang zu {@code running} online noch nicht bestätigt ist (KP-05). */
    public Optional<Instant> getRunningAusstehendSeit() {
        return zeitpunkt(Feld.RUNNING_AUSSTEHEND);
    }

    public void setRunningAusstehendSeit(@Nullable Instant lokalerStart) {
        schreibe(Feld.RUNNING_AUSSTEHEND, lokalerStart == null ? "" : lokalerStart.toString());
    }

    /**
     * Zeitpunkt, zu dem im Dokument zuletzt eine Folgerunde angelegt wurde. Ein online danach erkannter
     * zurückgesetzter Start (E-23) gilt dadurch als ausdrücklich wieder aufgenommen.
     */
    public Optional<Instant> getLetzterRundenstart() {
        return zeitpunkt(Feld.LETZTER_RUNDENSTART);
    }

    public void setLetzterRundenstart(Instant rundenstart) {
        schreibe(Feld.LETZTER_RUNDENSTART, rundenstart.toString());
    }

    /** Protokolliert die ausdrückliche Bestätigung, ohne letzten Abgleich zu starten (P-25). */
    public void protokolliereOfflineBestaetigung(Instant zeitpunkt, @Nullable Instant letzterSync) {
        schreibe(Feld.OFFLINE_BESTAETIGT, zeitpunkt + " / " + (letzterSync == null ? "-" : letzterSync.toString()));
    }

    // ── Zuordnung lokale Meldung ↔ Online-Anmeldung ─────────────────────────

    public PtmOnlineZuordnungsTabelle zuordnungen() {
        return PtmOnlineSyncJson.zuordnungenAusJson(lese(Feld.ZUORDNUNGEN));
    }

    /** Hängt neue Zuordnungen an; lokale Meldungen, die schon zugeordnet sind, bleiben unverändert. */
    public void addMappings(List<NeueZuordnung> neue) throws GenerateException {
        PtmOnlineZuordnungsTabelle neueTabelle = zuordnungen().mitNeuen(neue);
        if (neueTabelle.anzahl() > MAX_EINTRAEGE) {
            throw new GenerateException(I18n.get("ptmonline.fehler.zuordnung_voll"));
        }
        speichere(neueTabelle);
    }

    /** Aktualisiert Bezeichnung, Status und Online-Details mehrerer Zuordnungen; nicht genannte bleiben unverändert. */
    public void setAnzeigen(Map<String, ZuordnungsAnzeige> anzeigeProUuid) {
        speichere(zuordnungen().mitAnzeigen(anzeigeProUuid));
    }

    /** Anmeldestatus der zuletzt übernommenen Online-Anmeldung, unübersetzt (z.B. {@code cancelled}). */
    public Optional<String> getRohStatus(String lokaleUuid) {
        return zuordnungen().zuordnung(lokaleUuid).map(PtmOnlineZuordnung::rohStatus).flatMap(PtmOnlineSyncSpeicher::optional);
    }

    public void setBezeichnung(String lokaleUuid, String lokaleBezeichnung, String onlineStatus) {
        speichere(zuordnungen().mitBezeichnung(lokaleUuid, lokaleBezeichnung, onlineStatus));
    }

    public void setOnlineDetails(String lokaleUuid, ZuordnungsAnzeige.OnlineDetails details) {
        speichere(zuordnungen().mitDetails(lokaleUuid, details));
    }

    public Set<String> getLokaleUuids() {
        return zuordnungen().uuids();
    }

    public Map<String, String> getBezeichnungenProUuid() {
        return zuordnungen().proUuid(PtmOnlineZuordnung::lokaleBezeichnung);
    }

    public Map<String, String> getRohStatusProUuid() {
        return zuordnungen().proUuid(PtmOnlineZuordnung::rohStatus);
    }

    public Map<String, Integer> getExecutionRevisionenProUuid() {
        return zuordnungen().proUuid(PtmOnlineZuordnung::revision);
    }

    public void setExecutionRevisionen(Map<String, Integer> revisionProUuid) {
        speichere(zuordnungen().mitRevisionen(revisionProUuid));
    }

    /** Zuletzt abgeglichene Besetzung, Vermerke und Teamname je lokaler UUID. */
    public Map<String, ZuordnungsZusatz> getZusaetzeProUuid() {
        return zuordnungen().proUuid(PtmOnlineZuordnung::zusatz);
    }

    /** Setzt Besetzung, Vermerk und/oder Teamname mehrerer Zuordnungen; {@code null}-Werte bleiben unverändert. */
    public void setZusaetze(Map<String, ZuordnungsZusatz> zusatzProUuid) {
        speichere(zuordnungen().mitZusaetzen(zusatzProUuid));
    }

    /** Entfernt die Zuordnungen der genannten lokalen UUIDs; sie gelten danach als nicht importiert. */
    public void entferneZuordnungen(Set<String> lokaleUuids) {
        speichere(zuordnungen().ohne(lokaleUuids));
    }

    public void leeren() {
        speichere(PtmOnlineZuordnungsTabelle.LEER);
    }

    private void speichere(PtmOnlineZuordnungsTabelle tabelle) {
        schreibe(Feld.ZUORDNUNGEN, tabelle.anzahl() == 0 ? "" : PtmOnlineSyncJson.zuordnungenAlsJson(tabelle));
    }

    // ── Properties ───────────────────────────────────────────────────────────

    private String lese(Feld feld) {
        return properties.getStringProperty(feld.propertyName(spieltagNr), "");
    }

    /** Schreibt nur bei geändertem Wert; ein Schreibvorgang markiert das Dokument als geändert. */
    private void schreibe(Feld feld, @Nullable String wert) {
        properties.setStringPropertyOhneEvent(feld.propertyName(spieltagNr), Objects.requireNonNullElse(wert, ""));
    }

    private Optional<Instant> zeitpunkt(Feld feld) {
        String wert = lese(feld);
        if (wert.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(wert.strip()));
        } catch (DateTimeParseException e) {
            LOGGER.warn("PTM-Online: ungültiger Zeitpunkt '{}' in {}", wert, feld.propertyName(spieltagNr), e);
            return Optional.empty();
        }
    }

    private static Optional<String> optional(String wert) {
        return wert.isBlank() ? Optional.empty() : Optional.of(wert.strip());
    }
}
