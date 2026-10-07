/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.triptete.meldeliste;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.sheet.EditierfarbePruefung;

/**
 * Regressionstest: In der Trip-Tête-Meldeliste müssen alle editierbaren Spalten (Teamname, Spieler,
 * Verein, Aktiv) die Editierfarbe zeigen. Früher lag die Zeilen-Zebrafarbe als bedingte
 * Formatierung davor und verdeckte sie; Zebra wird jetzt direkt als Zellhintergrund gesetzt.
 */
class TripTeteMeldeListeEditierfarbeUITest extends BaseCalcUITest {

    private static final int ERSTE_DATEN_ZEILE = TripTeteMeldeListeDelegate.ERSTE_DATEN_ZEILE_OVERRIDE;

    @Test
    void alleEditierbarenSpaltenSindHervorgehoben() throws Exception {
        TripTeteMeldeListeSheetNew meldeListeNew = erzeugeMeldeliste();

        pruefe(meldeListeNew);
    }

    @Test
    void zebraBedingteFormatierungAusAltdokumentWirdBeimAktualisierenEntfernt() throws Exception {
        TripTeteMeldeListeSheetNew meldeListeNew = erzeugeMeldeliste();
        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        EditierfarbePruefung.schreibeZweiMeldungen(xSheet, doc, ERSTE_DATEN_ZEILE);
        int letzteZeile = meldeListeNew.getLetzteDatenZeileUseMin();
        int aktivSpalte = aktivSpalte(meldeListeNew);
        for (int spalte = 0; spalte <= aktivSpalte; spalte++) {
            EditierfarbePruefung.setzeAlteZebraCf(xSheet, spalte, ERSTE_DATEN_ZEILE, letzteZeile);
        }

        new TripTeteMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        pruefe(meldeListeNew);
    }

    private TripTeteMeldeListeSheetNew erzeugeMeldeliste() throws Exception {
        TripTeteMeldeListeSheetNew meldeListeNew = new TripTeteMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(true);
        meldeListeNew.createMeldeliste();
        return meldeListeNew;
    }

    private void pruefe(TripTeteMeldeListeSheetNew meldeListeNew) throws Exception {
        var konfig = meldeListeNew.getKonfigurationSheet();
        EditierfarbePruefung.pruefeMeldeliste(meldeListeNew.getXSpreadSheet(), ERSTE_DATEN_ZEILE,
                aktivSpalte(meldeListeNew), konfig.getMeldeListeHintergrundFarbeGerade(),
                konfig.getMeldeListeHintergrundFarbeUnGerade());
    }

    private int aktivSpalte(TripTeteMeldeListeSheetNew meldeListeNew) throws Exception {
        return new TripTeteMeldeListeDelegate(meldeListeNew, wkingSpreadsheet).getAktivSpalte();
    }
}
