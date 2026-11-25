/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * Zuletzt abgeglichener Teamname einer Meldung in der gespeicherten Zuordnung, Gegenstück zu
 * {@link AbgeglicheneBesetzung}. Der gespeicherte Wert ist ein JSON-String, damit „ohne Teamnamen abgeglichen“
 * ({@code ""}) von „noch nie abgeglichen“ (leerer Wert) unterscheidbar bleibt: Nur so erkennt der Abgleich, dass online ein Teamname neu eingetragen wurde.
 */
public final class AbgeglichenerTeamname {

    private static final Logger logger = LogManager.getLogger(AbgeglichenerTeamname.class);
    private static final Gson GSON = new Gson();

    private AbgeglichenerTeamname() {}

    /** Vergleichbare Form: ohne Rand-Leerzeichen, fehlend = leer. */
    public static String normalisiert(@Nullable String teamname) {
        return Objects.toString(teamname, "").strip();
    }

    /** Gespeicherter Wert für die Zuordnung. */
    public static String alsText(@Nullable String teamname) {
        return GSON.toJson(normalisiert(teamname));
    }

    /** Gespeicherter Stand; leer, wenn noch nie abgeglichen oder unlesbar (wird protokolliert). */
    public static Optional<String> lese(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(GSON.fromJson(text, String.class)).map(AbgeglichenerTeamname::normalisiert);
        } catch (JsonParseException e) {
            logger.warn("PTM-Online: zuletzt abgeglichener Teamname unlesbar: {}", text, e);
            return Optional.empty();
        }
    }
}
