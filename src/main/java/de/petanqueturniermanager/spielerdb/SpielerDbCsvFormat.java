package de.petanqueturniermanager.spielerdb;

import org.jspecify.annotations.Nullable;

/**
 * Konstanten und Format-Spezifikation für die flache Spieler-DB-CSV. Wird
 * symmetrisch von {@code SpielerDbCsvExporter} und {@code SpielerDbCsvImportReader}
 * verwendet.
 *
 * <p>Datei-Aufbau:
 * <ol>
 *   <li>UTF-8 BOM (optional beim Lesen, Pflicht beim Schreiben für Excel)</li>
 *   <li>Format-Marker {@link #formatMarkerZeile()} (z.B.
 *       {@code # PTM-SpielerDB-CSV;version=1})</li>
 *   <li>Header {@link #HEADER}</li>
 *   <li>Daten-Zeilen mit {@link #SEPARATOR} ({@code ;}) als Trenner und
 *       RFC-4180-konformem Quoting über opencsv</li>
 * </ol>
 *
 * <p>Beim Versions-Bump des Formats wird {@link #VERSION} hochgezählt; der
 * Reader prüft auf bekannte Versionen und liefert eine klare Fehlermeldung,
 * wenn die Datei zu neu ist.
 */
public final class SpielerDbCsvFormat {

    public static final char SEPARATOR = ';';

    public static final int VERSION = 1;

    private static final String MARKER_PRAEFIX = "# PTM-SpielerDB-CSV";

    public static final String[] HEADER = { "vorname", "nachname", "verein", "lizenznr" };

    private SpielerDbCsvFormat() { /* Utility */ }

    /** Vollständige Marker-Zeile für die aktuelle Version. */
    public static String formatMarkerZeile() {
        return MARKER_PRAEFIX + ";version=" + VERSION;
    }

    /**
     * Prüft, ob {@code zeile} eine Format-Marker-Zeile ist (unabhängig von
     * der Version).
     */
    public static boolean istMarkerZeile(String zeile) {
        return zeile.startsWith(MARKER_PRAEFIX);
    }

    /**
     * Extrahiert die Versionsnummer aus einer Marker-Zeile. Liefert
     * {@code null}, wenn die Zeile zwar mit dem Marker-Präfix beginnt, aber
     * keine gültige {@code version=N}-Angabe enthält.
     */
    public static @Nullable Integer leseVersion(String markerZeile) {
        int idx = markerZeile.indexOf("version=");
        if (idx < 0) {
            return null;
        }
        String rest = markerZeile.substring(idx + "version=".length()).strip();
        // Versionsnummer geht bis zum nächsten Nicht-Ziffer-Zeichen.
        int ende = 0;
        while (ende < rest.length() && Character.isDigit(rest.charAt(ende))) {
            ende++;
        }
        if (ende == 0) {
            return null;
        }
        try {
            return Integer.valueOf(rest.substring(0, ende));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
