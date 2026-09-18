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

import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.NeueOnlineAnmeldung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;

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

    /**
     * Listet die Turniere, die der Besitzer des aktiven API-Keys verwalten darf (Owner oder
     * Editor) - Grundlage für die Auswahlliste "Mit Online-Turnier verbinden".
     */
    public List<OnlineTournamentDto> listTournaments() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/sync/tournaments");
        JsonObject payload = GSON.fromJson(response.body(), JsonObject.class);

        List<OnlineTournamentDto> turniere = new ArrayList<>();
        for (var element : payload.getAsJsonArray("tournaments")) {
            turniere.add(GSON.fromJson(element, OnlineTournamentDto.class));
        }
        return turniere;
    }

    /**
     * Verbindet das lokale Dokument mit einem bestehenden Online-Turnier (setzt serverseitig
     * {@code document_managed = 1}, ohne sonstige Metadaten zu ändern).
     */
    public void connect(String tournamentId) throws IOException, InterruptedException {
        post("/api/sync/tournaments/" + encode(tournamentId) + "/connect", "{}");
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
        JsonObject payload = GSON.fromJson(response.body(), JsonObject.class);
        return GSON.fromJson(payload.get("registration"), RegistrationDto.class);
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
