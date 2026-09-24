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
    private static final int HTTP_CONFLICT = 409;

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
        try {
            JsonElement json = JsonParser.parseString(antwort);
            if (!json.isJsonObject()) {
                return Optional.empty();
            }
            JsonElement details = json.getAsJsonObject().get("details");
            return details != null && details.isJsonObject() ? Optional.of(details.getAsJsonObject())
                    : Optional.empty();
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            return Optional.empty();
        }
    }
}
