/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

/**
 * Ein offener Fall der Konfliktliste (A-29).
 *
 * @param lokaleUuid lokale Meldung, falls beteiligt
 * @param onlineIds  beteiligte Online-Anmeldungen
 * @param lokal      lokale Bezeichnung oder leer
 * @param online     Online-Bezeichnung oder leer
 * @param hinweis    was zu tun ist bzw. was die Optionen bewirken
 * @param optionen   mögliche Entscheidungen; leer für reine Hinweise
 */
public record KonfliktFall(KonfliktArt art, @Nullable String lokaleUuid, List<String> onlineIds, String lokal,
        String online, String hinweis, List<Entscheidung> optionen) {

    public KonfliktFall {
        onlineIds = List.copyOf(onlineIds);
        optionen = List.copyOf(optionen);
        lokal = Objects.toString(lokal, "");
        online = Objects.toString(online, "");
    }

    /**
     * Sprachneutraler, stabiler Schlüssel: Art, lokale UUID und Online-IDs. Ein Namenskonflikt trägt zusätzlich die
     * beiden Stände, damit eine Wahl nicht auf einen inzwischen anders geänderten Konflikt angewendet wird. Fälle
     * ohne IDs werden über ihre Bezeichnungen unterschieden.
     */
    public String schluessel() {
        String basis = schluessel(art, lokaleUuid, onlineIds);
        if (art == KonfliktArt.NAMENSKONFLIKT || (lokaleUuid == null && onlineIds.isEmpty())) {
            return basis + "|" + Integer.toHexString((lokal + "\u0000" + online).hashCode());
        }
        return basis;
    }

    static String schluessel(KonfliktArt art, @Nullable String lokaleUuid, List<String> onlineIds) {
        return art.name() + "|" + Objects.toString(lokaleUuid, "") + "|"
                + onlineIds.stream().sorted().collect(Collectors.joining(","));
    }

    /** Erste beteiligte Online-Anmeldung oder {@code null}. */
    public @Nullable String onlineId() {
        return onlineIds.isEmpty() ? null : onlineIds.getFirst();
    }
}
