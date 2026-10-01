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
    private static final int HTTP_GONE = 410;
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
     * Schreibzähler veraltet: eine Kopie dieses Dokuments hat inzwischen geschrieben ({@code document_forked}, E-24).
     */
    public boolean istDokumentGeforkt() {
        return hatKonfliktCode("document_forked");
    }

    /**
     * Das Online-Turnier läuft bereits; ein anderes Dokument kann nur als ausdrückliche Wiederherstellung verbunden
     * werden ({@code recovery_required}, E-03). {@link #rundenOnline()} nennt die online vorhandenen Runden.
     */
    public boolean istWiederherstellungNoetig() {
        return hatKonfliktCode("recovery_required");
    }

    /** Anzahl der online vorhandenen Spielrunden aus {@code recovery_required}; 0, wenn nicht angegeben. */
    public int rundenOnline() {
        return details().filter(details -> details.has("roundsOnline"))
                .map(details -> details.get("roundsOnline").getAsInt()).orElse(0);
    }

    /** PTM-Online legt ab {@code running} keine Anmeldung mehr an ({@code tournament_running}, E-13). */
    public boolean istTurnierLaeuft() {
        return hatKonfliktCode("tournament_running");
    }

    /** Das Online-Turnier ist abgeschlossen und nimmt kein Dokument mehr an ({@code tournament_finished}). */
    public boolean istTurnierAbgeschlossen() {
        return hatKonfliktCode("tournament_finished");
    }

    /**
     * Das Online-Turnier wurde in PTM-Online gelöscht: nur der ausdrückliche Löschnachweis (410 bzw. Code
     * {@code tournament_deleted}) zählt. 404, 403 oder ein Netzfehler beenden die Verbindung nie (KP-07, P-31).
     */
    public boolean istTurnierGeloescht() {
        return statusCode == HTTP_GONE || antwortJson().map(json -> json.get("details"))
                .filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject)
                .map(details -> details.get("code")).filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString).filter("tournament_deleted"::equals).isPresent();
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
