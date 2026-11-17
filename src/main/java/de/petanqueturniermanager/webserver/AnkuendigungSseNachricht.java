/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.webserver;

/**
 * SSE-Nachricht {@code ankuendigung}: aktuelle Ankündigung der Turnierleitung als Banner über jeder Webserver-Seite.
 * Ein leerer Text blendet das Banner aus. JSON-serialisiert via Gson.
 */
public record AnkuendigungSseNachricht(String typ, String text, String titel) {

    static final String TYP = "ankuendigung";

    public static AnkuendigungSseNachricht von(String text, String titel) {
        return new AnkuendigungSseNachricht(TYP, text == null ? "" : text, titel);
    }
}
