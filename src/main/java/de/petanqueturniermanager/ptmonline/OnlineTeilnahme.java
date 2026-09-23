/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

/**
 * Teilnahme-Zustand einer Meldung nach dem Check-in, gespiegelt aus der Aktiv-Spalte der
 * Meldeliste (leer = inaktiv, 1 = aktiv, 2 = ausgesetzt). Bewusst getrennt vom Anmeldestatus
 * ({@link OnlineAnmeldeStatus}), der ausschliesslich online verwaltet wird.
 */
public enum OnlineTeilnahme {

    INAKTIV("inactive"),
    AKTIV("active"),
    AUSGESETZT("withdrawn");

    private final String apiWert;

    OnlineTeilnahme(String apiWert) {
        this.apiWert = apiWert;
    }

    /** Wert des Feldes {@code participation} der PTM-Online-API. */
    public String apiWert() {
        return apiWert;
    }

    /** Ausgesetzt hat Vorrang vor aktiv; weder noch entspricht einer leeren Aktiv-Zelle (inaktiv). */
    public static OnlineTeilnahme aus(boolean aktiv, boolean ausgesetzt) {
        if (ausgesetzt) {
            return AUSGESETZT;
        }
        return aktiv ? AKTIV : INAKTIV;
    }
}
