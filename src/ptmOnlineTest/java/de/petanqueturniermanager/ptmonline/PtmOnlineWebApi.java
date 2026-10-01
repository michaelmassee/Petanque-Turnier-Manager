/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Die Online-Seite aus Sicht von Organisator und Teilnehmern – dieselben HTTP-Aufrufe wie die Web-App. Damit legen
 * die Tests Turniere und Anmeldungen an und prüfen den Stand, den PTM online hinterlassen hat.
 */
final class PtmOnlineWebApi {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final LokalerPtmOnlineServer server;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    PtmOnlineWebApi(LokalerPtmOnlineServer server) {
        this.server = server;
    }

    /** Öffentliches Turnier mit offener Online-Anmeldung, angelegt mit dem API-Schlüssel des Organisators. */
    String turnierAnlegen(String name, String typ, String anmeldeTyp, String formation, LocalDate datum)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("date", datum.toString());
        // Spät am Tag: die Online-Anmeldung schließt automatisch zum Turnierbeginn (E-02), auch bei Turnieren „heute“.
        body.addProperty("startTime", "23:59");
        body.addProperty("location", "Testplatz");
        body.addProperty("type", typ);
        body.addProperty("formation", formation);
        body.addProperty("registrationType", anmeldeTyp);
        body.addProperty("status", "registration");
        body.addProperty("visibility", "public");
        body.addProperty("participantsPublic", true);
        // Mit Koordinaten entfällt die Geokodierung.
        body.addProperty("latitude", 49.4);
        body.addProperty("longitude", 8.7);
        return senden(mitApiKey("POST", "/api/tournaments", body), 201).getAsJsonObject("tournament").get("id")
                .getAsString();
    }

    /** Eine Person der Anmeldung: Name und optionale Slot-E-Mail (verknüpft ein verifiziertes Konto, E-22). */
    record Person(String vorname, String nachname, String email) {
        static Person gast(String vorname, String nachname) {
            return new Person(vorname, nachname, null);
        }

        static Person konto(String vorname, String nachname, int kontoNr) {
            return new Person(vorname, nachname, LokalerPtmOnlineServer.spielerEmail(kontoNr));
        }

        String name() {
            return vorname + " " + nachname;
        }
    }

    /** Öffentliche Anmeldung ohne Konto (wie ein Gast über das Formular) mit beiden Einverständnissen. */
    String anmelden(String turnierId, Person... personen) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("email", "kontakt@example.test");
        body.addProperty("publicationNoticeAccepted", true);
        body.addProperty("personsConsentAccepted", true);
        body.add("feeSelections", new JsonArray());
        body.add("registrationAnswers", new JsonArray());
        String[] praefixe = { "", "partner", "partner2" };
        for (int i = 0; i < personen.length; i++) {
            Person person = personen[i];
            String praefix = praefixe[i];
            body.addProperty(praefix.isEmpty() ? "firstName" : praefix + "FirstName", person.vorname());
            body.addProperty(praefix.isEmpty() ? "lastName" : praefix + "LastName", person.nachname());
            if (person.email() != null) {
                body.addProperty(praefix.isEmpty() ? "playerEmail" : praefix + "Email", person.email());
            }
        }
        HttpRequest anfrage = anfrage("/api/tournaments/" + turnierId + "/registrations")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return senden(anfrage, 201).getAsJsonObject("registration").get("id").getAsString();
    }

    /** Alle Anmeldungen in der Verwaltungssicht des Organisators. */
    List<JsonObject> anmeldungen(String turnierId) throws IOException, InterruptedException {
        HttpRequest anfrage = mitSession("/api/tournaments/" + turnierId + "/registrations").GET().build();
        List<JsonObject> liste = new ArrayList<>();
        for (JsonElement element : senden(anfrage, 200).getAsJsonArray("registrations")) {
            liste.add(element.getAsJsonObject());
        }
        return liste;
    }

    Optional<JsonObject> anmeldung(String turnierId, String anmeldungId) throws IOException, InterruptedException {
        return anmeldungen(turnierId).stream().filter(a -> anmeldungId.equals(a.get("id").getAsString())).findFirst();
    }

    /** Storniert eine Anmeldung in der Verwaltung (Organisator, nach dem Import durch PTM). */
    void stornieren(String turnierId, String anmeldungId) throws IOException, InterruptedException {
        JsonObject anmeldung = anmeldung(turnierId, anmeldungId).orElseThrow();
        anmeldung.addProperty("status", "cancelled");
        senden(mitApiKey("PUT", "/api/registrations/" + anmeldungId, anmeldung), 200);
    }

    JsonObject turnier(String turnierId) throws IOException, InterruptedException {
        return senden(mitSession("/api/tournaments/" + turnierId).GET().build(), 200).getAsJsonObject("tournament");
    }

    private HttpRequest mitApiKey(String methode, String pfad, JsonObject body) {
        return anfrage(pfad).header("Authorization", "Bearer " + server.apiKey())
                .method(methode, HttpRequest.BodyPublishers.ofString(body.toString())).build();
    }

    private HttpRequest.Builder mitSession(String pfad) {
        return anfrage(pfad).header("Cookie", "ptm_session=" + server.sessionId());
    }

    private HttpRequest.Builder anfrage(String pfad) {
        return HttpRequest.newBuilder(URI.create(server.baseUrl() + pfad)).timeout(TIMEOUT)
                .header("Content-Type", "application/json").header("Accept", "application/json");
    }

    private JsonObject senden(HttpRequest anfrage, int erwartet) throws IOException, InterruptedException {
        HttpResponse<String> antwort = client.send(anfrage, HttpResponse.BodyHandlers.ofString());
        if (antwort.statusCode() != erwartet) {
            throw new IOException(anfrage.method() + " " + anfrage.uri().getPath() + " → HTTP " + antwort.statusCode()
                    + " (erwartet " + erwartet + "): " + antwort.body());
        }
        return JsonParser.parseString(antwort.body()).getAsJsonObject();
    }
}
