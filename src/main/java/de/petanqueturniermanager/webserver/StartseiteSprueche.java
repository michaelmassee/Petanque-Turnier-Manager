package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Liest die rotierenden Boule-Sprüche für die Turnier-Startseite aus den i18n-Properties.
 * Konvention: {@code startseite.spruch.1}, {@code .2}, … bis zum ersten fehlenden Schlüssel.
 * Damit lassen sich Sprüche pro Sprache pflegen, ohne den Code anzufassen.
 */
public final class StartseiteSprueche {

    private static final String KEY_PREFIX = "startseite.spruch.";
    private static final int MAX_SPRUECHE = 200;

    // Statische Referenz für den I18n-Referenzdatei-Test: die Sprüche werden dynamisch
    // über KEY_PREFIX + Index geladen und sind sonst im Code unsichtbar. Diese Liste
    // muss synchron zu den in messages.properties gepflegten Schlüsseln bleiben.
    static final String[] BEKANNTE_SCHLUESSEL = {
            "startseite.spruch.1",
            "startseite.spruch.2",
            "startseite.spruch.3",
            "startseite.spruch.4",
            "startseite.spruch.5",
            "startseite.spruch.6",
            "startseite.spruch.7",
            "startseite.spruch.8",
            "startseite.spruch.9",
            "startseite.spruch.10",
            "startseite.spruch.11",
            "startseite.spruch.12",
            "startseite.spruch.13",
            "startseite.spruch.14",
            "startseite.spruch.15",
            "startseite.spruch.16",
            "startseite.spruch.17",
            "startseite.spruch.18",
            "startseite.spruch.19",
            "startseite.spruch.20",
    };

    private StartseiteSprueche() {
    }

    public static List<String> alle() {
        var result = new ArrayList<String>();
        for (int i = 1; i <= MAX_SPRUECHE; i++) {
            String key = KEY_PREFIX + i;
            String text = I18n.get(key);
            // I18n.get gibt den Schlüssel selbst zurück, wenn keine Übersetzung existiert
            if (text == null || text.equals(key) || text.isBlank()) {
                break;
            }
            result.add(text);
        }
        return result;
    }
}
