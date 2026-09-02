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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.ptmonline.dto.CreateTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;
import de.petanqueturniermanager.ptmonline.dto.TournamentMetadataDto;

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

    TournamentSyncClient(HttpClient httpClient, String baseUrl, String apiKey) {
        super(httpClient, baseUrl, apiKey);
    }

    /** Leichter Verbindungstest: listet die eigenen Turniere; wirft bei Fehler eine {@link IOException}. */
    public void pruefeVerbindung() throws IOException, InterruptedException {
        get("/api/tournaments");
    }

    /**
     * Legt ein neues Turnier online an und liefert dessen PTM-Online-ID.
     */
    public String createTournament(CreateTournamentDto tournament) throws IOException, InterruptedException {
        HttpResponse<String> response = post("/api/tournaments", GSON.toJson(tournament));
        JsonObject payload = GSON.fromJson(response.body(), JsonObject.class);
        return payload.getAsJsonObject("tournament").get("id").getAsString();
    }

    /** Überträgt die allein im Turnierdokument gepflegten Eckdaten und markiert das Turnier online als dokumentverwaltet. */
    public void pushTournamentMetadata(String tournamentId, TournamentMetadataDto metadata)
            throws IOException, InterruptedException {
        put("/api/sync/tournaments/" + encode(tournamentId) + "/metadata", GSON.toJson(metadata));
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

        HttpResponse<String> response = get(path);
        JsonObject payload = GSON.fromJson(response.body(), JsonObject.class);

        List<RegistrationDto> registrations = new ArrayList<>();
        for (var element : payload.getAsJsonArray("registrations")) {
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

        HttpResponse<String> response = post("/api/sync/tournaments/" + encode(tournamentId) + "/results", body.toString());
        JsonObject payload = GSON.fromJson(response.body(), JsonObject.class);
        return payload.get("updatedCount").getAsInt();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
