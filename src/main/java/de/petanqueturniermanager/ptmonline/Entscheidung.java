/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Wahl der Turnierleitung zu einem Fall der Konfliktliste. Entscheidungen ohne eigenen Schreibvorgang werden online
 * protokolliert ({@code POST …/decisions}); die übrigen protokolliert PTM-Online über den Schreibvorgang selbst.
 */
public enum Entscheidung {

    /** Lokale Meldung mit der gleichnamigen Online-Anmeldung verknüpfen (KP-06 a2). */
    VERKNUEPFEN("link"),
    /** Bewusst getrennt: die lokale Meldung wird normal online angelegt (KP-06 a2). */
    GETRENNT("separate"),
    /** Online-Besetzung in die Meldeliste übernehmen (KP-16). */
    ONLINE_UEBERNEHMEN(null),
    /** Lokale Besetzung behalten und online übertragen (KP-16). */
    LOKAL_BEHALTEN(null),
    /** Online stornierte Meldung bewusst behalten; der Ausschluss entfällt (KP-14). */
    BEHALTEN("keep_despite_online_status"),
    /** Fehlende Meldung nur lokal entfernen; kein erneuter Import (KP-15). */
    NUR_LOKAL_ENTFERNEN("remove_local"),
    /** Fehlende Meldung online stornieren (KP-15). */
    ONLINE_STORNIEREN(null),
    /** Nach dem Turnierstart eingegangene Anmeldung übernehmen (KP-05). */
    UEBERNEHMEN("link"),
    /** Erste Runde trotz offener Ausschlussgründe im Vorabcheck gestartet (A-29). */
    TROTZDEM_STARTEN("start_despite_findings");

    private final @Nullable String serverCode;

    Entscheidung(@Nullable String serverCode) {
        this.serverCode = serverCode;
    }

    /** Entscheidungscode von PTM-Online, leer wenn der Schreibvorgang selbst protokolliert wird. */
    public Optional<String> serverCode() {
        return Optional.ofNullable(serverCode);
    }

    public String anzeige() {
        return I18n.get("ptmonline.entscheidung." + name().toLowerCase(Locale.ROOT));
    }

    /**
     * Die Entscheidung zu einem Zelltext, sofern sie zu den Optionen des Falls gehört. Erkannt werden der Anzeigetext
     * der aktuellen Sprache und der sprachneutrale Name.
     */
    public static Optional<Entscheidung> aus(@Nullable String text, List<Entscheidung> optionen) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String gesucht = text.strip();
        return optionen.stream()
                .filter(option -> option.anzeige().equalsIgnoreCase(gesucht) || option.name().equalsIgnoreCase(gesucht))
                .findFirst();
    }
}
