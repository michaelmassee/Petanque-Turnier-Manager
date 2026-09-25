/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;

/**
 * Client fuer die PTM-Online REST-API: legt Turniere an und gleicht Anmeldungen/Ergebnisse
 * zwischen dem lokalen Turnierdokument und PTM-Online bidirektional ab. Benoetigt einen von einem
 * PTM-Online-Administrator freigeschalteten API-Schluessel (siehe
 * {@link de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher}).
 */
public class TournamentSyncClient extends PtmOnlineHttpClient {

    public TournamentSyncClient(String baseUrl, String apiKey) {
        super(baseUrl, apiKey);
    }

    /** Client für alle schreibenden Aufrufe eines konkret gebundenen Turnierdokuments. */
    public TournamentSyncClient(String baseUrl, String apiKey, String syncDocumentId, String leaseToken) {
        super(HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build(), baseUrl, apiKey,
                syncDocumentId, leaseToken);
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
     * {@code document_managed = 1}, ohne sonstige Metadaten zu ändern).
     */
    public SyncBindingDto connect(String tournamentId, String syncDocumentId, String leaseToken)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("syncDocumentId", syncDocumentId);
        body.addProperty("leaseToken", leaseToken);
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
            long expectedBindingRevision) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("syncDocumentId", syncDocumentId);
        body.addProperty("leaseToken", leaseToken);
        body.addProperty("takeoverRequestId", UUID.randomUUID().toString());
        body.addProperty("expectedBindingRevision", expectedBindingRevision);
        HttpResponse<String> response = post("/api/sync/tournaments/" + encode(tournamentId) + "/takeover",
                body.toString());
        return pruefeBindung(leseAntwort(response, SyncBindingDto.class), syncDocumentId);
    }

    /**
     * Löst die Verbindung des lokalen Dokuments wieder (setzt serverseitig
     * {@code document_managed = 0}, hebt damit auch die Web-UI-Bearbeitungssperre wieder auf).
     */
    public void disconnect(String tournamentId) throws IOException, InterruptedException {
        post("/api/sync/tournaments/" + encode(tournamentId) + "/disconnect", "{}");
    }

    /**
     * Startet das verbundene Online-Turnier (Statuswechsel auf {@code running}) aus dem
     * Turnierdokument heraus - anders als der Web-UI-Weg ({@code POST /api/tournaments/{id}/start})
     * funktioniert dieser Endpoint auch fuer dokumentverwaltete Turniere (die Web-UI-Variante
     * verweigert das bewusst). Idempotent, wenn das Turnier bereits laeuft.
     */
    public void start(String tournamentId) throws IOException, InterruptedException {
        post("/api/sync/tournaments/" + encode(tournamentId) + "/start", "{}");
    }

    /**
     * Legt eine neue Anmeldung ohne die oeffentliche Registrierungsmaske an - fuer lokal (im
     * Turnierdokument) erfasste Teams, die online noch nicht bekannt sind. Liefert die neue
     * Anmeldung inkl. Online-Id fuer das lokale Mapping zurueck.
     */
    public RegistrationDto createRegistration(String tournamentId, NeueOnlineAnmeldung anmeldung)
            throws IOException, InterruptedException {
        HttpResponse<String> response = post(
                "/api/sync/tournaments/" + encode(tournamentId) + "/registrations", GSON.toJson(anmeldung));
        return leseRegistration(response);
    }

    /** Idempotente Neuanlage durch die lokale, dauerhafte Meldelisten-UUID. */
    public RegistrationDto upsertRegistration(String tournamentId, String lokaleUuid, NeueOnlineAnmeldung anmeldung)
            throws IOException, InterruptedException {
        HttpResponse<String> response = put("/api/sync/tournaments/" + encode(tournamentId)
                + "/registrations/" + encode(lokaleUuid), GSON.toJson(anmeldung));
        return leseRegistration(response);
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
     * Schreibt lokal geaenderte Status-/Ranglisten-Werte zurueck nach PTM-Online. Legt keine neuen
     * Anmeldungen an, aktualisiert nur bestehende (per {@code id} referenziert).
     */
    public int pushResults(String tournamentId, List<RegistrationResultDto> results) throws IOException, InterruptedException {
        JsonArray registrationsArray = new JsonArray();
        results.stream().map(GSON::toJsonTree).forEach(registrationsArray::add);

        JsonObject body = new JsonObject();
        body.add("registrations", registrationsArray);

        JsonObject payload = leseAntwort(
                post("/api/sync/tournaments/" + encode(tournamentId) + "/results", body.toString()));
        JsonElement updatedCount = pflichtfeld(payload, "updatedCount");
        if (!updatedCount.isJsonPrimitive() || !updatedCount.getAsJsonPrimitive().isNumber()) {
            throw unvollstaendigeAntwort("updatedCount");
        }
        return updatedCount.getAsInt();
    }

    private static RegistrationDto leseRegistration(HttpResponse<String> response) throws IOException {
        JsonElement registration = pflichtfeld(leseAntwort(response), "registration");
        if (!registration.isJsonObject()) {
            throw unvollstaendigeAntwort("registration");
        }
        return GSON.fromJson(registration, RegistrationDto.class);
    }

    private static JsonObject leseAntwort(HttpResponse<String> response) throws IOException {
        return leseAntwort(response, JsonObject.class);
    }

    /**
     * Parst den Antwort-Body. Leere oder syntaktisch kaputte Antworten werden zu einer
     * {@link IOException}, damit Aufrufer sie wie jeden anderen Verbindungsfehler behandeln.
     */
    private static <T> T leseAntwort(HttpResponse<String> response, Class<T> typ) throws IOException {
        T payload;
        try {
            payload = GSON.fromJson(response.body(), typ);
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
