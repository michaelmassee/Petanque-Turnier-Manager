/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/** HTTP-Fehlerantwort der PTM-Online-API mit Statuscode und Antworttext. */
public final class PtmOnlineHttpException extends IOException {

    private static final long serialVersionUID = 1L;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    /** Fehlertext der PTM-Online-API, wenn es das Turnier nicht (mehr) gibt; unübersetzt, Teil des API-Vertrags. */
    private static final String TURNIER_NICHT_GEFUNDEN = "Turnier nicht gefunden";
    /** Anfang des Fehlertexts, wenn das Turnier online (nicht im Dokument) durchgeführt wird; Teil des API-Vertrags. */
    private static final String ONLINE_DURCHGEFUEHRT = "Dieses Turnier wird online durchgeführt";

    private final int statusCode;
    private final String antwort;

    public PtmOnlineHttpException(int statusCode, String antwort) {
        super("PTM-Online API Fehler " + statusCode + ": " + antwort);
        this.statusCode = statusCode;
        this.antwort = antwort == null ? "" : antwort;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Ob PTM-Online die Anmeldung inhaltlich abgelehnt hat, weil ein Spieler oder der Teamname im
     * Turnier bereits angemeldet ist. Der Server liefert dafür 409 mit {@code details.field}; andere
     * 409-Konflikte (Dokumentbindung, Ausführungsrevision) haben kein {@code field} und bleiben Fehler.
     */
    public boolean istBereitsAngemeldet() {
        return details().map(details -> details.has("field")).orElse(false);
    }

    /**
     * Das Online-Turnier ist mit einem anderen Turnierdokument verbunden ({@code document_bound} beim Verbinden).
     * Übernehmen lässt es sich per {@code takeover} mit {@link #bindingRevision()}.
     */
    public boolean istAnderesDokumentGebunden() {
        return hatKonfliktCode("document_bound");
    }

    /**
     * Die Bindung dieses Dokuments gilt nicht mehr: ein anderes Dokument hat das Online-Turnier übernommen
     * ({@code document_replaced}), das Lease ist ungültig ({@code lease_invalid}) oder das Turnier ist online gar
     * nicht mehr gebunden ({@code document_unbound}).
     */
    public boolean istBindungAbgeloest() {
        return hatKonfliktCode("document_replaced") || hatKonfliktCode("lease_invalid")
                || hatKonfliktCode("document_unbound");
    }

    /**
     * Die Ausführungsdaten einer Meldung wurden online zwischenzeitlich geändert ({@code execution_conflict}): die
     * lokal bekannte Ausführungsrevision ist veraltet, PTM-Online hat nichts übernommen.
     */
    public boolean istRevisionsKonflikt() {
        return hatKonfliktCode("execution_conflict");
    }

    /**
     * Anzeigename der Online-Anmeldung aus einem Zuordnungskonflikt ({@code registration_mapping_conflict}): die
     * Anmeldung hängt online noch an einer anderen Meldelisten-Zeile als der, die das Dokument ihr gerade zuordnen
     * wollte. Leer bei allen anderen Fehlern.
     */
    public Optional<String> zuordnungsKonfliktAnmeldung() {
        if (!hatKonfliktCode("registration_mapping_conflict")) {
            return Optional.empty();
        }
        return details().map(details -> details.get("registration")).filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject).map(PtmOnlineHttpException::anmeldungsName)
                .filter(name -> !name.isBlank());
    }

    private static String anmeldungsName(JsonObject registration) {
        return Stream.of(name(registration, "firstName", "lastName"),
                name(registration, "partnerFirstName", "partnerLastName"),
                name(registration, "partner2FirstName", "partner2LastName"))
                .filter(name -> !name.isBlank()).collect(Collectors.joining(" / "));
    }

    private static String name(JsonObject registration, String vornameFeld, String nachnameFeld) {
        return (text(registration, vornameFeld) + " " + text(registration, nachnameFeld)).strip();
    }

    private static String text(JsonObject json, String feld) {
        JsonElement wert = json.get(feld);
        return wert != null && wert.isJsonPrimitive() ? wert.getAsString().strip() : "";
    }

    /**
     * Das Online-Turnier existiert nicht mehr – in PTM-Online gelöscht. Andere 404-Antworten (z.&nbsp;B. eine
     * einzelne Anmeldung) zählen nicht dazu.
     */
    public boolean istTurnierGeloescht() {
        return statusCode == HTTP_NOT_FOUND && fehlertext().filter(TURNIER_NICHT_GEFUNDEN::equals).isPresent();
    }

    /**
     * PTM-Online nimmt keine Runden aus dem Dokument an, weil das Turnier dort nicht als im Dokument durchgeführt
     * gilt – entweder wird es tatsächlich online durchgeführt, oder der Turnierstart aus dem Dokument kam nie an.
     */
    public boolean istOnlineDurchgefuehrt() {
        return statusCode == HTTP_CONFLICT && fehlertext().filter(text -> text.startsWith(ONLINE_DURCHGEFUEHRT))
                .isPresent();
    }

    /** Feld {@code error} der Antwort. */
    private Optional<String> fehlertext() {
        return antwortJson().map(json -> json.get("error")).filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString);
    }

    /** Aktuelle Bindungsrevision des Online-Turniers aus einem Bindungskonflikt. */
    public OptionalLong bindingRevision() {
        return details().filter(details -> details.has("bindingRevision"))
                .map(details -> OptionalLong.of(details.get("bindingRevision").getAsLong()))
                .orElse(OptionalLong.empty());
    }

    private boolean hatKonfliktCode(String code) {
        return details().filter(details -> details.has("code"))
                .map(details -> code.equals(details.get("code").getAsString())).orElse(false);
    }

    /** {@code details} eines 409-Konflikts; andere Fehler haben keine auswertbaren Details. */
    private Optional<JsonObject> details() {
        if (statusCode != HTTP_CONFLICT) {
            return Optional.empty();
        }
        return antwortJson().map(json -> json.get("details")).filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject);
    }

    /** Antwort als JSON-Objekt; leer, wenn sie kein JSON-Objekt ist. */
    private Optional<JsonObject> antwortJson() {
        try {
            JsonElement json = JsonParser.parseString(antwort);
            return json.isJsonObject() ? Optional.of(json.getAsJsonObject()) : Optional.empty();
        } catch (JsonParseException e) {
            return Optional.empty();
        }
    }
}
