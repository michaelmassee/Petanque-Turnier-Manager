/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Liest ein Ranglisten-Blatt mit einer Zeile je Team bzw. Spieler: Nummer, Platz, Siege, Punkte+ und Punkte−
 * stehen in festen Spalten. Übernommen wird, was das Turnierdokument anzeigt – das Dokument ist Referenz für die
 * Rangliste der Live-Ansicht.
 *
 * @param schluessel      Metadaten-Schlüssel des Ranglisten-Blatts
 * @param fallbackName    Blattname für Dokumente ohne Metadaten
 * @param ersteDatenZeile erste Ranglisten-Zeile (0-basiert)
 */
record RanglistenBlattLeser(String schluessel, String fallbackName, int ersteDatenZeile, Spalten spalten) {

    /** Spalten (0-basiert) von Nummer, Platz, Siegen, Punkte+ und Punkte−. */
    record Spalten(int nummer, int platz, int siege, int punktePlus, int punkteMinus) {

        int letzte() {
            return Math.max(Math.max(Math.max(nummer, platz), Math.max(siege, punktePlus)), punkteMinus);
        }
    }

    /** @return Ranglisten-Einträge in Blattreihenfolge; leer, wenn das Blatt (noch) fehlt */
    List<LiveRanglistenEintrag> lese(WorkingSpreadsheet ws) {
        XSpreadsheet blatt = LiveBlattLeser.blatt(ws, schluessel, fallbackName);
        if (blatt == null) {
            return List.of();
        }
        LiveZellbereich zellen = LiveBlattLeser.datenzeilen(ws, blatt, ersteDatenZeile, spalten.letzte());
        List<LiveRanglistenEintrag> eintraege = new ArrayList<>();
        for (int zeile = zellen.ersteZeile(); zeile <= zellen.letzteZeile(); zeile++) {
            int nummer = zellen.zahl(spalten.nummer(), zeile);
            if (nummer <= 0) {
                break;
            }
            int platz = zellen.zahl(spalten.platz(), zeile);
            if (platz > 0) {
                eintraege.add(LiveRanglistenEintrag.team(platz, nummer, zellen.punkte(spalten.siege(), zeile),
                        zellen.punkte(spalten.punktePlus(), zeile), zellen.punkte(spalten.punkteMinus(), zeile)));
            }
        }
        return eintraege;
    }
}
