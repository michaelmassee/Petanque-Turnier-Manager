/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Vergleichsschlüssel eines Spielernamens nach derselben Regel wie PTM-Online
 * ({@code normalizePlayerName} in {@code src/worker.js}): ohne Akzente, Groß-/Kleinschreibung,
 * Leer- und Satzzeichen – „Jean-Paul Müller“ und „jean paul muller“ gelten als derselbe Spieler.
 * Beide Seiten müssen gleich vergleichen, sonst legt der Abgleich lokal eine zweite Zeile an, die
 * PTM-Online anschließend als doppelten Spieler ablehnt.
 */
public final class OnlineSpielerName {

    private OnlineSpielerName() {}

    public static String schluessel(@Nullable String vorname, @Nullable String nachname) {
        String roh = Objects.toString(vorname, "") + Objects.toString(nachname, "");
        String schluessel = Normalizer.normalize(roh, Normalizer.Form.NFKD)
                .replaceAll("[\\u0300-\\u036f]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        // Namen ganz ohne lateinische Buchstaben/Ziffern (z.B. kyrillisch) ergäben sonst alle
        // denselben leeren Schlüssel; PTM-Online prüft solche Namen gar nicht auf Duplikate.
        return schluessel.isEmpty() ? roh.strip().toLowerCase(Locale.ROOT) : schluessel;
    }
}
