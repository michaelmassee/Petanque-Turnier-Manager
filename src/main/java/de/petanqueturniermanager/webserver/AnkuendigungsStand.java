/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.webserver;

import java.util.Objects;

/**
 * Zuletzt an eine Webserver-Instanz verteilte Ankündigung (fertiges SSE-JSON). Neu verbundene Browser erhalten sie
 * direkt nach dem Init; unveränderte Stände werden nicht erneut gepusht.
 */
final class AnkuendigungsStand {

    private String json;

    /** @return {@code true}, wenn sich der Stand geändert hat und gepusht werden muss */
    synchronized boolean setze(String neuesJson) {
        if (Objects.equals(json, neuesJson)) {
            return false;
        }
        json = neuesJson;
        return true;
    }

    /** Zusatz-Nachrichten für neue SSE-Verbindungen. */
    synchronized String[] initZusatzJsons() {
        return json == null ? new String[0] : new String[] { json };
    }
}
