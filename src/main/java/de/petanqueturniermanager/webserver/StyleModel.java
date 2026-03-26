package de.petanqueturniermanager.webserver;

/**
 * Enthält alle visuellen Eigenschaften einer einzelnen Tabellenzelle.
 * <p>
 * Null-Felder bedeuten "Browser-Standard" und werden von Gson nicht serialisiert.
 * Primitive Felder (0, false, 1) werden immer serialisiert.
 * <p>
 * Alle Einheiten entsprechen den UNO-Rohdaten; die Umrechnung in CSS-Werte
 * erfolgt im React-Frontend.
 */
public record StyleModel(
        boolean fett,
        boolean kursiv,
        String hintergrundfarbe,      // "#RRGGBB" oder null = Browser-Standard
        String schriftfarbe,          // "#RRGGBB" oder null = Browser-Standard
        String ausrichtung,           // "left" | "center" | "right" | "justify"
        String vertikaleAusrichtung,  // "top" | "middle" | "bottom"
        int colspan,
        int rowspan,
        int rotationGrad,             // UNO RotateAngle / 100 (0, 90, 270 …)
        boolean zeilenumbruch,        // UNO IsTextWrapped
        String schriftart,            // UNO CharFontName, null = Browser-Standard
        float schriftgroesse,         // UNO CharHeight in Punkt, 0 = Browser-Standard
        // CSS-Border-String z. B. "1px solid #000000"; null = kein Property setzen
        String linienOben,
        String linienUnten,
        String linienLinks,
        String linienRechts
) {}
