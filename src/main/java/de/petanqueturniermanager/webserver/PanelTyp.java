package de.petanqueturniermanager.webserver;

/**
 * Unterscheidet den Anzeigemodus eines Panels in einem Composite View.
 * <ul>
 *   <li>{@link #BLATT} – zeigt ein LibreOffice-Sheet als Tabelle an</li>
 *   <li>{@link #URL} – bettet eine externe URL als {@code <iframe>} ein</li>
 * </ul>
 */
public enum PanelTyp {
    BLATT,
    URL,
    TIMER
}
