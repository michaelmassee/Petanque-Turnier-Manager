/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;

import com.google.gson.JsonElement;
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
        if (statusCode != HTTP_CONFLICT) {
            return false;
        }
        try {
            JsonElement json = JsonParser.parseString(antwort);
            if (!json.isJsonObject()) {
                return false;
            }
            JsonElement details = json.getAsJsonObject().get("details");
            return details != null && details.isJsonObject() && details.getAsJsonObject().has("field");
        } catch (JsonParseException e) {
            return false;
        }
    }
}
