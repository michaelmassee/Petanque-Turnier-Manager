/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.ptmonline.live.LiveRanglistenEintrag.Wertungskriterien;

/**
 * Liest ein Ranglisten-Blatt mit einer Zeile je Team bzw. Spieler: Nummer, Platz, Siege, Punkte+ und Punkte−
 * stehen in festen Spalten, dazu die systemabhängigen Wertungskriterien. Übernommen wird, was das Turnierdokument
 * anzeigt – das Dokument ist Referenz für die Rangliste der Live-Ansicht.
 *
 * @param schluessel      Metadaten-Schlüssel des Ranglisten-Blatts
 * @param fallbackName    Blattname für Dokumente ohne Metadaten
 * @param ersteDatenZeile erste Ranglisten-Zeile (0-basiert)
 */
record RanglistenBlattLeser(String schluessel, String fallbackName, int ersteDatenZeile, Spalten spalten) {

    /** Spaltenwert für ein Wertungskriterium, das das Blatt nicht führt. */
    static final int KEINE = -1;

    /**
     * Spalten (0-basiert) von Nummer, Platz, Siegen, Punkte+ und Punkte− sowie der Wertungskriterien ({@link #KEINE},
     * wenn das Blatt sie nicht führt).
     */
    record Spalten(int nummer, int platz, int siege, int punktePlus, int punkteMinus, int niederlagen,
            int spielDifferenz, int buchholz, int feinBuchholz, int formuleXWertung) {

        Spalten(int nummer, int platz, int siege, int punktePlus, int punkteMinus) {
            this(nummer, platz, siege, punktePlus, punkteMinus, KEINE, KEINE, KEINE, KEINE, KEINE);
        }

        Spalten mitNiederlagen(int niederlagenSpalte, int spielDifferenzSpalte) {
            return new Spalten(nummer, platz, siege, punktePlus, punkteMinus, niederlagenSpalte, spielDifferenzSpalte,
                    buchholz, feinBuchholz, formuleXWertung);
        }

        Spalten mitBuchholz(int buchholzSpalte, int feinBuchholzSpalte) {
            return new Spalten(nummer, platz, siege, punktePlus, punkteMinus, niederlagen, spielDifferenz,
                    buchholzSpalte, feinBuchholzSpalte, formuleXWertung);
        }

        Spalten mitFormuleXWertung(int wertungSpalte) {
            return new Spalten(nummer, platz, siege, punktePlus, punkteMinus, niederlagen, spielDifferenz, buchholz,
                    feinBuchholz, wertungSpalte);
        }

        int letzte() {
            return IntStream.of(nummer, platz, siege, punktePlus, punkteMinus, niederlagen, spielDifferenz, buchholz,
                    feinBuchholz, formuleXWertung).max().orElse(nummer);
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
                        zellen.punkte(spalten.punktePlus(), zeile), zellen.punkte(spalten.punkteMinus(), zeile),
                        wertung(zellen, zeile)));
            }
        }
        return eintraege;
    }

    private Wertungskriterien wertung(LiveZellbereich zellen, int zeile) {
        return new Wertungskriterien(anzahl(zellen, spalten.niederlagen(), zeile),
                spalten.spielDifferenz() == KEINE ? null : zellen.differenz(spalten.spielDifferenz(), zeile),
                anzahl(zellen, spalten.buchholz(), zeile), anzahl(zellen, spalten.feinBuchholz(), zeile),
                anzahl(zellen, spalten.formuleXWertung(), zeile));
    }

    private static @Nullable Integer anzahl(LiveZellbereich zellen, int spalte, int zeile) {
        return spalte == KEINE ? null : zellen.punkte(spalte, zeile);
    }
}
