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

    /** Wert der Aktiv-Spalte für „nimmt teil“. */
    static final int AKTIV_WERT_NIMMT_TEIL = 1;
    /** Wert der Aktiv-Spalte für „ausgesetzt/ausgestiegen“, in allen Meldelisten einheitlich. */
    static final int AKTIV_WERT_AUSGESETZT = 2;

    /** Teilnahme laut Wert der Aktiv-Spalte; alles außer 1 und 2 gilt als inaktiv. */
    public static OnlineTeilnahme ausAktivWert(int aktivWert) {
        return aus(aktivWert == AKTIV_WERT_NIMMT_TEIL, aktivWert == AKTIV_WERT_AUSGESETZT);
    }

    /** Ausgesetzt hat Vorrang vor aktiv; weder noch entspricht einer leeren Aktiv-Zelle (inaktiv). */
    public static OnlineTeilnahme aus(boolean aktiv, boolean ausgesetzt) {
        if (ausgesetzt) {
            return AUSGESETZT;
        }
        return aktiv ? AKTIV : INAKTIV;
    }
}
