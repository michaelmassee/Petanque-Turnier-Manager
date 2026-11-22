/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

/**
 * Wie eine Meldeliste Teamnamen führt – in allen Turniersystemen und in PTM-Online gleich (das Dokument ist Master):
 * <ul>
 * <li>{@link #KEIN}: nur Spieler; online {@code teamNameEnabled = false}</li>
 * <li>{@link #ZUSATZ}: Spieler und zusätzlich eine Teamname-Spalte; online {@code teamNameEnabled = true}</li>
 * <li>{@link #NUR_TEAMNAME}: Formation „Nur Teamname“, keine Spieler; online Formation „andere“, Teamname immer
 * an</li>
 * </ul>
 */
public enum TeamnameModus {

    KEIN, ZUSATZ, NUR_TEAMNAME;

    /** Modus einer Meldeliste aus Formation und Teamname-Spalte (Konfiguration bzw. Start-Dialog). */
    public static TeamnameModus von(Formation formation, boolean teamnameSpalte) {
        if (formation == Formation.NUR_TEAMNAME) {
            return NUR_TEAMNAME;
        }
        return teamnameSpalte ? ZUSATZ : KEIN;
    }

    /** Modus eines Online-Turniers aus Formation „andere“ und dem Schalter {@code teamNameEnabled}. */
    public static TeamnameModus ausOnline(boolean formationAndere, boolean teamNameEnabled) {
        if (formationAndere) {
            return NUR_TEAMNAME;
        }
        return teamNameEnabled ? ZUSATZ : KEIN;
    }

    /**
     * Ob die Meldeliste eine Teamname-Spalte hat – zugleich {@code teamNameEnabled} in PTM-Online (Teamname im
     * Anmeldeformular).
     */
    public boolean teamnameSpalte() {
        return this != KEIN;
    }
}
