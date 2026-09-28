/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;

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
     * Das Online-Turnier existiert nicht mehr – in PTM-Online gelöscht. Andere 404-Antworten (z.&nbsp;B. eine
     * einzelne Anmeldung) zählen nicht dazu.
     */
    public boolean istTurnierGeloescht() {
        return statusCode == HTTP_NOT_FOUND && antwortJson().map(json -> json.get("error"))
                .filter(JsonElement::isJsonPrimitive)
                .map(fehler -> TURNIER_NICHT_GEFUNDEN.equals(fehler.getAsString())).orElse(false);
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
