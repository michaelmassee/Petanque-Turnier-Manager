package de.petanqueturniermanager.webserver;

import java.util.Locale;

/**
 * Konstanten und Normierung für die Panel-Ausrichtung (horizontal und vertikal)
 * in Composite Views.
 * <p>
 * Werte werden als Strings persistiert, übers Wire (Gson) an das Frontend gesendet
 * und in der UI als ComboBox-Auswahl genutzt – daher String statt Enum.
 */
public final class PanelAusrichtung {

    public static final String KEIN    = "kein";

    public static final String H_LINKS   = "links";
    public static final String H_MITTE   = "mitte";
    public static final String H_RECHTS  = "rechts";

    public static final String V_OBEN  = "oben";
    public static final String V_MITTE = "mitte";
    public static final String V_UNTEN = "unten";

    private PanelAusrichtung() {}

    /**
     * Normiert einen horizontalen Ausrichtungswert. Unbekannte oder {@code null}-Werte
     * werden auf {@link #KEIN} abgebildet.
     */
    public static String normiereHorizontal(String wert) {
        if (wert == null) return KEIN;
        String normiert = wert.trim().toLowerCase(Locale.ROOT);
        return switch (normiert) {
            case H_LINKS, H_MITTE, H_RECHTS -> normiert;
            default -> KEIN;
        };
    }

    /**
     * Normiert einen vertikalen Ausrichtungswert. Unbekannte oder {@code null}-Werte
     * werden auf {@link #KEIN} abgebildet.
     */
    public static String normiereVertikal(String wert) {
        if (wert == null) return KEIN;
        String normiert = wert.trim().toLowerCase(Locale.ROOT);
        return switch (normiert) {
            case V_OBEN, V_MITTE, V_UNTEN -> normiert;
            default -> KEIN;
        };
    }
}
