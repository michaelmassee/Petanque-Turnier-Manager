package de.petanqueturniermanager.webserver;

import java.util.ArrayList;
import java.util.List;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Liest die rotierenden Boule-Sprüche für die Turnier-Startseite aus den i18n-Properties.
 * Die Liste {@link #BEKANNTE_SCHLUESSEL} ist die Wahrheit – sie muss synchron zu den
 * in {@code messages.properties} gepflegten Schlüsseln bleiben (vom I18n-Referenzdatei-Test
 * abgesichert).
 */
public final class StartseiteSprueche {

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
        var result = new ArrayList<String>(BEKANNTE_SCHLUESSEL.length);
        for (String key : BEKANNTE_SCHLUESSEL) {
            String text = I18n.get(key);
            if (text == null || text.equals(key) || text.isBlank()) {
                continue;
            }
            result.add(text);
        }
        return result;
    }
}
