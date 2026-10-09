/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.spielerdb;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;

/**
 * Spalten- und Zeilenlayout einer Meldeliste für die Spieler-DB-Übernahme. Formation und
 * Anzeige-Flags stammen aus dem system-spezifischen KonfigurationSheet; erste Datenzeile und
 * Lage der Aktiv-Spalte sind fest je Meldelisten-Typ – sie aus dem Sheet zu erraten scheitert an
 * der leeren Meldeliste (keine Nummern), dort hielt eine Heuristik die Spaltenkopf-Zeile für ein
 * Team.
 *
 * @param aktivSpaltenAbstand Abstand der Aktiv-Spalte (Checkin) zur letzten Spielerdaten-Spalte:
 *        2 mit SP-Spalte dazwischen, 1 ohne (Trip-Tête), bei Supermelee 1 + aktiver Spieltag
 */
record MeldelisteLayout(Formation formation, boolean teamnameAktiv, boolean vereinsnameAktiv,
        int ersteDatenZeile, int aktivSpaltenAbstand) {

    private static final int AKTIV_HINTER_SP_SPALTE = 2;
    private static final int AKTIV_DIREKT_HINTER_SPIELERN = 1;

    /** Team-Meldeliste mit drei Header-Zeilen und SP-Spalte vor der Aktiv-Spalte. */
    static MeldelisteLayout team(Formation formation, boolean teamnameAktiv, boolean vereinsnameAktiv) {
        return new MeldelisteLayout(formation, teamnameAktiv, vereinsnameAktiv,
                MeldeListeKonstanten.TEAM_MELDELISTE_ERSTE_DATEN_ZEILE, AKTIV_HINTER_SP_SPALTE);
    }

    /** Trip-Tête: feste Triplette, keine SP-Spalte – Aktiv folgt direkt auf die Spieler. */
    static MeldelisteLayout tripTete(boolean teamnameAktiv, boolean vereinsnameAktiv) {
        return new MeldelisteLayout(Formation.TRIPLETTE, teamnameAktiv, vereinsnameAktiv,
                MeldeListeKonstanten.TEAM_MELDELISTE_ERSTE_DATEN_ZEILE, AKTIV_DIREKT_HINTER_SPIELERN);
    }

    /**
     * Supermelee: Einzelspieler ohne Teamname-/Vereinsspalten, zwei Header-Zeilen. Nach der SP-Spalte
     * folgt je Spieltag eine Aktiv-Spalte; maßgeblich ist die des aktiven Spieltags.
     */
    static MeldelisteLayout supermelee(int aktiverSpieltag) {
        return new MeldelisteLayout(Formation.MELEE, false, false, MeldeListeKonstanten.ERSTE_DATEN_ZEILE,
                1 + Math.max(1, aktiverSpieltag));
    }
}
