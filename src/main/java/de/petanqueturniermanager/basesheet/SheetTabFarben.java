/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet;

/**
 * Zentrale Standard-Tab-Farben für alle Tabellenblatt-Typen.
 * <p>
 * Allgemeine Sheet-Typen (Meldeliste, Spielrunde, Rangliste, Direktvergleich, Teilnehmer)
 * sind turniertypübergreifend einheitlich. Turnierspezifische Sheets erhalten eigene Farben.
 * <p>
 * Diese Werte sind die Standardfarben — sie können über Document Properties
 * überschrieben werden (konfigurierbar über das Konfigurations-Sheet).
 */
public final class SheetTabFarben {

    private SheetTabFarben() {}

    // ---------------------------------------------------------------
    // Allgemeine Sheet-Typen (turniertypübergreifend)
    // ---------------------------------------------------------------

    /** Meldeliste — alle Turniersysteme. */
    public static final int MELDELISTE = 0x2544dd;

    /** Spielrunde / Spielplan — alle Turniersysteme inkl. Poule-Spielpläne. */
    public static final int SPIELRUNDE = 0xb0f442;

    /** Rangliste / Endrangliste / Spieltagrangliste — alle Turniersysteme. */
    public static final int RANGLISTE = 0xd637e8;

    /** Direktvergleich-Rangliste — alle Turniersysteme. */
    public static final int DIREKTVERGLEICH = 0x42d4f5;

    /** Teilnehmer-/Arbeitsblatt — alle Turniersysteme einheitlich. */
    public static final int TEILNEHMER = 0x98e2d7;

    // ---------------------------------------------------------------
    // Super-Mêlée System
    // ---------------------------------------------------------------

    /** Team-Paarungen-Sheet (Übersicht aller möglichen Paarungen). */
    public static final int SUPERMELEE_TEAM_PAARUNGEN = 0xf4ca46;

    // ---------------------------------------------------------------
    // KO-Turnier
    // ---------------------------------------------------------------

    /** KO-Konfigurationsblatt. */
    public static final int KO_KONFIGURATION = 0xc12439;

    /** KO-Turnierbaum-Sheet. */
    public static final int KO_TURNIERBAUM = 0x8b0000;

    // ---------------------------------------------------------------
    // Poule-A/B System
    // ---------------------------------------------------------------

    /** Poule-Vorrunden-Sheet (Eingabe der Spielergebnisse). */
    public static final int POULE_VORRUNDE = 0xff691f;

    /** Poule-Vorrunden-Rangliste. */
    public static final int POULE_VORRUNDEN_RANGLISTE = 0xfacd73;

    // ---------------------------------------------------------------
    // Forme / Ko-Runde (Hilfssystem für KO-Phasen)
    // ---------------------------------------------------------------

    /** Cadrage-Sheet (Einrundungs-Auslosung). */
    public static final int FORME_CADRAGE = 0xc12439;
}
