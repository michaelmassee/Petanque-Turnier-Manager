/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.auftrag.versand.AuftragsSender;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.dto.LiveMatchDto;
import de.petanqueturniermanager.ptmonline.dto.LiveRankingEntryDto;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;
import de.petanqueturniermanager.ptmonline.dto.ServerZuordnungDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;

/**
 * Client fuer die PTM-Online REST-API: legt Turniere an und gleicht Anmeldungen/Ergebnisse
 * zwischen dem lokalen Turnierdokument und PTM-Online bidirektional ab. Benoetigt einen von einem
 * PTM-Online-Administrator freigeschalteten API-Schluessel (siehe
 * {@link de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher}).
 */
public class TournamentSyncClient extends PtmOnlineHttpClient implements AuftragsSender {

    /**
     * Protokollversion dieses PTM: Schreibaufträge tragen Auftrags-ID und Schreibzähler (T-19, T-23). PTM-Online
     * speichert sie mit der Bindung und verlangt danach beides.
     */
    static final int PROTOKOLL_VERSION = 2;

    public TournamentSyncClient(String baseUrl, String apiKey) {
        super(baseUrl, apiKey);
    }

    /** Client für alle schreibenden Aufrufe eines konkret gebundenen Turnierdokuments. */
    public TournamentSyncClient(String baseUrl, String apiKey, String syncDocumentId, String leaseToken) {
        super(HttpClient.newBuilder().connectTimeout(STANDARD_TIMEOUT).build(), baseUrl, apiKey, syncDocumentId,
                leaseToken);
    }

    /**
     * Lesender Client mit kurzem Zeitlimit für Abfragen, auf die der Turnierbetrieb warten muss (Rückfrage vor dem
     * Turnierstart): bei schlechtem Netz lieber schnell ohne Online-Daten weiter als lange blockieren.
     */
    public static TournamentSyncClient mitKurzemTimeout(String baseUrl, String apiKey, Duration timeout) {
        return new TournamentSyncClient(HttpClient.newBuilder().connectTimeout(timeout).build(), baseUrl, apiKey,
                timeout);
    }

    private TournamentSyncClient(HttpClient httpClient, String baseUrl, String apiKey, Duration timeout) {
        super(httpClient, baseUrl, apiKey, null, null, timeout);
    }

    TournamentSyncClient(HttpClient httpClient, String baseUrl, String apiKey) {
        super(httpClient, baseUrl, apiKey);
    }

    /**
     * Listet die Turniere, die der Besitzer des aktiven API-Keys verwalten darf (Owner oder
     * Editor) - Grundlage für die Auswahlliste "Mit Online-Turnier verbinden".
     */
    public List<OnlineTournamentDto> listTournaments() throws IOException, InterruptedException {
        JsonObject payload = leseAntwort(get("/api/sync/tournaments"));

        List<OnlineTournamentDto> turniere = new ArrayList<>();
        for (var element : pflichtArray(payload, "tournaments")) {
            turniere.add(GSON.fromJson(element, OnlineTournamentDto.class));
        }
        return turniere;
    }

    /**
     * Verbindet das lokale Dokument mit einem bestehenden Online-Turnier (setzt serverseitig
     * {@code document_managed = 1}, ohne sonstige Metadaten zu ändern). Eine neue Bindung beginnt mit Schreibzähler
     * 0; die Wiederholung derselben Verbindung ({@code connectRequestId}) liefert den aktuellen Zählerstand (P-22).
     *
     * @param connectRequestId stabile ID dieses Verbindungsversuchs, vor dem Senden gespeichert
     * @param wiederherstellung ausdrückliche Bestätigung, ein bereits laufendes Turnier mit diesem Dokument
     *                          wiederherzustellen (E-03); ohne sie lehnt PTM-Online mit {@code recovery_required} ab
     */
    public SyncBindingDto connect(String tournamentId, String syncDocumentId, String leaseToken,
            String connectRequestId, boolean wiederherstellung) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("syncDocumentId", syncDocumentId);
        body.addProperty("leaseToken", leaseToken);
        body.addProperty("protocolVersion", PROTOKOLL_VERSION);
        body.addProperty("connectRequestId", connectRequestId);
        if (wiederherstellung) {
            body.addProperty("recovery", true);
        }
        HttpResponse<String> response = post("/api/sync/tournaments/" + encode(tournamentId) + "/connect", body.toString());
        return pruefeBindung(leseAntwort(response, SyncBindingDto.class), syncDocumentId);
    }

    private static SyncBindingDto pruefeBindung(SyncBindingDto binding, String syncDocumentId) throws IOException {
        if (binding == null || !binding.ok() || binding.syncDocumentId() == null
                || !syncDocumentId.equals(binding.syncDocumentId()) || binding.bindingRevision() < 1) {
            throw new IOException(I18n.get("ptmonline.fehler.server_ohne_dokumentbindung"));
        }
        return binding;
    }

    /**
     * Übernimmt ein Online-Turnier, das mit einem anderen Turnierdokument verbunden ist: das bisherige Dokument
     * verliert seine Bindung (Online-Turnier und Dokument sind immer 1:1 verbunden). {@code expectedBindingRevision}
     * stammt aus dem {@code document_bound}-Konflikt des vorangegangenen {@link #connect}.
     */
    public SyncBindingDto takeover(String tournamentId, String syncDocumentId, String leaseToken,
            long expectedBindingRevision, String takeoverRequestId, boolean wiederherstellung)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("syncDocumentId", syncDocumentId);
        body.addProperty("leaseToken", leaseToken);
        body.addProperty("takeoverRequestId", takeoverRequestId);
        body.addProperty("expectedBindingRevision", expectedBindingRevision);
        body.addProperty("protocolVersion", PROTOKOLL_VERSION);
        if (wiederherstellung) {
            body.addProperty("recovery", true);
        }
        HttpResponse<String> response = post("/api/sync/tournaments/" + encode(tournamentId) + "/takeover",
                body.toString());
        return pruefeBindung(leseAntwort(response, SyncBindingDto.class), syncDocumentId);
    }

    /**
     * Holt online eingegangene Anmeldungen eines Turniers, optional nur die seit {@code since}
     * geaenderten (fuer inkrementellen Abgleich).
     */
    public List<RegistrationDto> fetchRegistrations(String tournamentId, Instant since) throws IOException, InterruptedException {
        String path = "/api/sync/tournaments/" + encode(tournamentId) + "/registrations";
        if (since != null) {
            path += "?since=" + encode(since.toString());
        }

        JsonObject payload = leseAntwort(get(path));

        List<RegistrationDto> registrations = new ArrayList<>();
        for (var element : pflichtArray(payload, "registrations")) {
            registrations.add(GSON.fromJson(element, RegistrationDto.class));
        }
        return registrations;
    }

    /**
     * Zustand des Online-Turniers (Status, Anmeldeschluss, Online-Runden, Schreibzähler), ohne Anmeldungen: der
     * Abruf der Anmeldeliste mit einem Zeitpunkt in der Zukunft liefert nur den Turnierzustand.
     */
    public SyncStandDto fetchSyncStand(String tournamentId) throws IOException, InterruptedException {
        String path = "/api/sync/tournaments/" + encode(tournamentId) + "/registrations?since="
                + encode(Instant.now().plus(Duration.ofDays(1)).toString());
        JsonElement stand = pflichtfeld(leseAntwort(get(path)), "tournament");
        if (!stand.isJsonObject()) {
            throw unvollstaendigeAntwort("tournament");
        }
        return GSON.fromJson(stand, SyncStandDto.class);
    }

    /** Serverseitig bekannte Zuordnungen lokale UUID ↔ Online-Anmeldung (T-21); braucht die Dokumentbindung. */
    public List<ServerZuordnungDto> fetchMapping(String tournamentId) throws IOException, InterruptedException {
        JsonObject payload = leseAntwort(get("/api/sync/tournaments/" + encode(tournamentId) + "/mapping"));
        List<ServerZuordnungDto> zuordnungen = new ArrayList<>();
        for (var element : pflichtArray(payload, "mappings")) {
            zuordnungen.add(GSON.fromJson(element, ServerZuordnungDto.class));
        }
        return zuordnungen;
    }

    /** Sendet einen gezählten Schreibauftrag (siehe {@link PtmOnlineHttpClient#sendeAuftrag}). */
    @Override
    public SyncAntwort sende(SyncAuftrag auftrag) throws IOException, InterruptedException {
        return sendeAuftrag(auftrag);
    }

    // ── Pfade und Nutzlasten der Schreibaufträge ─────────────────────────────

    public static String trennenPfad(String tournamentId) {
        return turnierPfad(tournamentId) + "/disconnect";
    }

    static String startPfad(String tournamentId) {
        return turnierPfad(tournamentId) + "/start";
    }

    static String anmeldungPfad(String tournamentId, String lokaleUuid) {
        return turnierPfad(tournamentId) + "/registrations/" + encode(lokaleUuid);
    }

    static String ergebnissePfad(String tournamentId) {
        return turnierPfad(tournamentId) + "/results";
    }

    static String ranglistePfad(String tournamentId) {
        return turnierPfad(tournamentId) + "/ranking";
    }

    /** Nutzlast des Turnierstarts mit dem lokalen Startzeitpunkt (P-26). */
    static String startBody(Instant lokalerStart) {
        JsonObject body = new JsonObject();
        body.addProperty("localStartedAt", lokalerStart.toString());
        return body.toString();
    }

    /** Nutzlast einer Online-Anlage samt Teilnahme und Setzposition der neuen Meldung. */
    static String anlageBody(NeueOnlineAnmeldung anmeldung, String teilnahme, Integer setzposition) {
        JsonObject body = GSON.toJsonTree(anmeldung).getAsJsonObject();
        body.addProperty("participation", teilnahme);
        if (setzposition != null) {
            body.addProperty("seedingPosition", setzposition);
        }
        return body.toString();
    }

    /** Nutzlast einer Namenskorrektur aus dem Dokument (siehe PTM-Online {@code documentMaster}). */
    static String aenderungBody(NeueOnlineAnmeldung anmeldung, String onlineRegistrationId,
            int expectedExecutionRevision) {
        JsonObject body = GSON.toJsonTree(anmeldung).getAsJsonObject();
        body.addProperty("documentMaster", true);
        body.addProperty("onlineRegistrationId", onlineRegistrationId);
        body.addProperty("expectedExecutionRevision", expectedExecutionRevision);
        return body.toString();
    }

    static String ergebnisseBody(List<RegistrationResultDto> results) {
        JsonArray registrationsArray = new JsonArray();
        results.stream().map(GSON::toJsonTree).forEach(registrationsArray::add);
        JsonObject body = new JsonObject();
        body.add("registrations", registrationsArray);
        return body.toString();
    }

    static String rundeBody(List<LiveMatchDto> matches) {
        JsonObject body = new JsonObject();
        body.add("matches", GSON.toJsonTree(matches));
        return body.toString();
    }

    static String ranglisteBody(List<LiveRankingEntryDto> entries) {
        JsonObject body = new JsonObject();
        body.add("entries", GSON.toJsonTree(entries));
        return body.toString();
    }

    // ── Antworten der Schreibaufträge ────────────────────────────────────────

    /** Anmeldung aus der Antwort einer Anlage oder Änderung. */
    static RegistrationDto registrationAus(SyncAntwort antwort) throws IOException {
        JsonElement registration = pflichtfeld(leseAntwort(antwort.body(), JsonObject.class), "registration");
        if (!registration.isJsonObject()) {
            throw unvollstaendigeAntwort("registration");
        }
        return GSON.fromJson(registration, RegistrationDto.class);
    }

    /** Ganzzahliges Pflichtfeld der Antwort (z.&nbsp;B. {@code updatedCount}). */
    static int zahlAus(SyncAntwort antwort, String feld) throws IOException {
        return pflichtZahl(leseAntwort(antwort.body(), JsonObject.class), feld);
    }

    private static String turnierPfad(String tournamentId) {
        return "/api/sync/tournaments/" + encode(tournamentId);
    }

    static String rundePfad(String tournamentId, int roundNumber) {
        return turnierPfad(tournamentId) + "/rounds/" + roundNumber;
    }

    private static int pflichtZahl(JsonObject payload, String feld) throws IOException {
        JsonElement wert = pflichtfeld(payload, feld);
        if (!wert.isJsonPrimitive() || !wert.getAsJsonPrimitive().isNumber()) {
            throw unvollstaendigeAntwort(feld);
        }
        return wert.getAsInt();
    }

    private static JsonObject leseAntwort(HttpResponse<String> response) throws IOException {
        return leseAntwort(response, JsonObject.class);
    }

    /**
     * Parst den Antwort-Body. Leere oder syntaktisch kaputte Antworten werden zu einer
     * {@link IOException}, damit Aufrufer sie wie jeden anderen Verbindungsfehler behandeln.
     */
    private static <T> T leseAntwort(HttpResponse<String> response, Class<T> typ) throws IOException {
        return leseAntwort(response.body(), typ);
    }

    private static <T> T leseAntwort(String antwort, Class<T> typ) throws IOException {
        T payload;
        try {
            payload = GSON.fromJson(antwort, typ);
        } catch (JsonParseException e) {
            throw new IOException(I18n.get("ptmonline.fehler.antwort_ungueltig"), e);
        }
        if (payload == null) {
            throw new IOException(I18n.get("ptmonline.fehler.antwort_ungueltig"));
        }
        return payload;
    }

    private static JsonElement pflichtfeld(JsonObject payload, String feld) throws IOException {
        JsonElement wert = payload.get(feld);
        if (wert == null || wert.isJsonNull()) {
            throw unvollstaendigeAntwort(feld);
        }
        return wert;
    }

    private static JsonArray pflichtArray(JsonObject payload, String feld) throws IOException {
        JsonElement wert = pflichtfeld(payload, feld);
        if (!wert.isJsonArray()) {
            throw unvollstaendigeAntwort(feld);
        }
        return wert.getAsJsonArray();
    }

    private static IOException unvollstaendigeAntwort(String feld) {
        return new IOException(I18n.get("ptmonline.fehler.antwort_unvollstaendig", feld));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
