/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Liest Spielrunden im Team-Layout „Bahn | Team A | Team B | Ergebnis A | Ergebnis B“ (Schweizer, Formule X,
 * Maastrichter-Vorrunden): ein Blatt je Runde, eine Partie je Zeile, Team B leer = Freilos. Gelesen wird ab
 * Runde 1, bis ein Rundenblatt fehlt.
 */
final class TeamSpielrundenLeser {

    private static final int BAHN = 0;
    private static final int TEAM_A = 1;
    private static final int TEAM_B = 2;
    private static final int ERGEBNIS_A = 3;
    private static final int ERGEBNIS_B = 4;

    private final WorkingSpreadsheet ws;
    private final IntFunction<String> schluessel;
    private final IntFunction<String> fallbackName;
    private final int ersteDatenZeile;
    private final boolean mitBahn;

    /**
     * @param schluessel      Metadaten-Schlüssel des Rundenblatts je Rundennummer
     * @param fallbackName    Blattname je Rundennummer für Dokumente ohne Metadaten
     * @param ersteDatenZeile erste Partie-Zeile (0-basiert)
     * @param mitBahn         {@code true}, wenn die erste Spalte Spielbahnen enthält (sonst laufende Nummer)
     */
    TeamSpielrundenLeser(WorkingSpreadsheet ws, IntFunction<String> schluessel, IntFunction<String> fallbackName,
            int ersteDatenZeile, boolean mitBahn) {
        this.ws = ws;
        this.schluessel = schluessel;
        this.fallbackName = fallbackName;
        this.ersteDatenZeile = ersteDatenZeile;
        this.mitBahn = mitBahn;
    }

    /** @return Partien je Runde, in Rundenreihenfolge */
    List<List<LivePartie>> leseRunden() throws GenerateException {
        List<List<LivePartie>> runden = new ArrayList<>();
        for (int nr = 1; nr <= LiveBlattLeser.MAX_RUNDEN; nr++) {
            SheetRunner.testDoCancelTask();
            XSpreadsheet blatt = LiveBlattLeser.blatt(ws, schluessel.apply(nr), fallbackName.apply(nr));
            if (blatt == null) {
                break;
            }
            runden.add(lesePartien(LiveBlattLeser.datenzeilen(ws, blatt, ersteDatenZeile, ERGEBNIS_B)));
        }
        return runden;
    }

    private List<LivePartie> lesePartien(LiveZellbereich zellen) {
        List<LivePartie> partien = new ArrayList<>();
        for (int zeile = zellen.ersteZeile(); zeile <= zellen.letzteZeile(); zeile++) {
            int teamA = zellen.teamNr(TEAM_A, zeile);
            if (teamA <= 0) {
                break;
            }
            String bahn = mitBahn ? zellen.text(BAHN, zeile) : null;
            int teamB = zellen.teamNr(TEAM_B, zeile);
            partien.add(teamB <= 0 ? LivePartie.freilos(teamA, bahn, null)
                    : LivePartie.teams(teamA, teamB, zellen.punkte(ERGEBNIS_A, zeile),
                            zellen.punkte(ERGEBNIS_B, zeile), bahn, null));
        }
        return partien;
    }
}
