package de.petanqueturniermanager.webserver;

import java.util.Locale;

/**
 * Konfiguration des Gesamtrahmens einer Composite View (Dicke, Linienart, Farbe,
 * Transparenz, Animation). Werte werden als Property-Strings persistiert und über
 * {@link #toDaten()} in das für das Frontend serialisierbare {@link RandDaten}
 * (fertiger CSS-{@code rgba(...)}-Farbwert) umgerechnet.
 */
public record RandKonfiguration(int dicke, String art, int farbe, int transparenz, String animation) {

    public static final String ART_KEIN = "kein";
    public static final String ART_SOLID = "solid";
    public static final String ART_DASHED = "dashed";
    public static final String ART_DOTTED = "dotted";
    public static final String ART_DOUBLE = "double";

    public static final String ANIMATION_KEINE = "keine";
    public static final String ANIMATION_AMEISEN = "ameisen";
    public static final String ANIMATION_PULSIEREN = "pulsieren";
    public static final String ANIMATION_FARBWECHSEL = "farbwechsel";

    /** Maximal zulässige Randdicke in Pixeln. */
    public static final int MAX_DICKE = 50;

    /** Default: kein Rahmen. */
    public static final RandKonfiguration KEINER = new RandKonfiguration(0, ART_KEIN, 0x000000, 0, ANIMATION_KEINE);

    public RandKonfiguration {
        dicke = Math.max(0, Math.min(MAX_DICKE, dicke));
        art = normiereArt(art);
        farbe = farbe & 0xFFFFFF;
        transparenz = Math.max(0, Math.min(100, transparenz));
        animation = normiereAnimation(animation);
    }

    /** Normiert eine Rand-Art. Unbekannte oder {@code null}-Werte werden auf {@link #ART_KEIN} abgebildet. */
    public static String normiereArt(String wert) {
        if (wert == null) return ART_KEIN;
        return switch (wert.trim().toLowerCase(Locale.ROOT)) {
            case ART_SOLID, ART_DASHED, ART_DOTTED, ART_DOUBLE -> wert.trim().toLowerCase(Locale.ROOT);
            default -> ART_KEIN;
        };
    }

    /** Normiert einen Animationswert. Unbekannte oder {@code null}-Werte werden auf {@link #ANIMATION_KEINE} abgebildet. */
    public static String normiereAnimation(String wert) {
        if (wert == null) return ANIMATION_KEINE;
        return switch (wert.trim().toLowerCase(Locale.ROOT)) {
            case ANIMATION_AMEISEN, ANIMATION_PULSIEREN, ANIMATION_FARBWECHSEL -> wert.trim().toLowerCase(Locale.ROOT);
            default -> ANIMATION_KEINE;
        };
    }

    /**
     * Liefert die für das Frontend serialisierbaren Rand-Daten, oder {@code null}
     * wenn kein Rahmen dargestellt werden soll (Dicke 0 oder Art "kein").
     */
    public RandDaten toDaten() {
        if (dicke <= 0 || ART_KEIN.equals(art)) {
            return null;
        }
        double alpha = (100 - transparenz) / 100.0;
        String rgba = String.format(Locale.ROOT, "rgba(%d,%d,%d,%.2f)",
                (farbe >> 16) & 0xFF, (farbe >> 8) & 0xFF, farbe & 0xFF, alpha);
        return new RandDaten(dicke, art, rgba, animation);
    }

    /** Für das Frontend serialisierte Rand-Daten (fertiger CSS-Farbwert). */
    public record RandDaten(int dicke, String art, String farbe, String animation) {
    }
}
