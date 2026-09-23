/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.Arrays;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Anmeldestatus einer PTM-Online-Anmeldung (Feld {@code status}). Wird ausschliesslich online
 * verwaltet; der Teilnahme-Zustand nach dem Check-in ist davon getrennt ({@link OnlineTeilnahme}).
 */
public enum OnlineAnmeldeStatus {

    OFFEN("pending", "ptmonline.status.offen"),
    BESTAETIGT("confirmed", "ptmonline.status.bestaetigt"),
    WARTELISTE("waitlist", "ptmonline.status.warteliste"),
    STORNIERT("cancelled", "ptmonline.status.storniert");

    private final String apiWert;
    private final String i18nSchluessel;

    OnlineAnmeldeStatus(String apiWert, String i18nSchluessel) {
        this.apiWert = apiWert;
        this.i18nSchluessel = i18nSchluessel;
    }

    public String apiWert() {
        return apiWert;
    }

    public static Optional<OnlineAnmeldeStatus> aus(@Nullable String apiWert) {
        return Arrays.stream(values()).filter(status -> status.apiWert.equals(apiWert)).findFirst();
    }

    public static boolean istBestaetigt(@Nullable String apiWert) {
        return BESTAETIGT.apiWert.equals(apiWert);
    }

    public static boolean istStorniert(@Nullable String apiWert) {
        return STORNIERT.apiWert.equals(apiWert);
    }

    /** Lesbare Anzeige fuer das Meldungen-Sheet; unbekannte Werte werden unveraendert angezeigt. */
    public static String anzeige(@Nullable String apiWert) {
        if (apiWert == null) {
            return "";
        }
        return aus(apiWert).map(status -> I18n.get(status.i18nSchluessel)).orElse(apiWert);
    }
}
