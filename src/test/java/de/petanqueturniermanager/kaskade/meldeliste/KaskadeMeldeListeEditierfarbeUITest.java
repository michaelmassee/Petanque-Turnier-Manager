/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.kaskade.meldeliste;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.sheet.EditierfarbePruefung;

/**
 * Regressionstest: In der Kaskaden-Meldeliste müssen alle editierbaren Spalten (Teamname, Spieler,
 * Verein, SP, Aktiv) die Editierfarbe zeigen. Früher lag die Zeilen-Zebrafarbe als bedingte
 * Formatierung davor und verdeckte sie; Zebra wird jetzt direkt als Zellhintergrund gesetzt.
 */
class KaskadeMeldeListeEditierfarbeUITest extends BaseCalcUITest {

    private static final int ERSTE_DATEN_ZEILE = KaskadeListeDelegate.ERSTE_DATEN_ZEILE;

    @Test
    void alleEditierbarenSpaltenSindHervorgehoben() throws Exception {
        KaskadeMeldeListeSheetNew meldeListeNew = erzeugeMeldeliste();

        pruefe(meldeListeNew);
    }

    @Test
    void zebraBedingteFormatierungAusAltdokumentWirdBeimAktualisierenEntfernt() throws Exception {
        KaskadeMeldeListeSheetNew meldeListeNew = erzeugeMeldeliste();
        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        EditierfarbePruefung.schreibeZweiMeldungen(xSheet, doc, ERSTE_DATEN_ZEILE);
        int letzteZeile = meldeListeNew.getLetzteDatenZeileUseMin();
        for (int spalte = 0; spalte <= meldeListeNew.getAktivSpalte(); spalte++) {
            EditierfarbePruefung.setzeAlteZebraCf(xSheet, spalte, ERSTE_DATEN_ZEILE, letzteZeile);
        }

        new KaskadeMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        pruefe(meldeListeNew);
    }

    private KaskadeMeldeListeSheetNew erzeugeMeldeliste() throws Exception {
        KaskadeMeldeListeSheetNew meldeListeNew = new KaskadeMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.TRIPLETTE, true, true, 2);
        return meldeListeNew;
    }

    private void pruefe(KaskadeMeldeListeSheetNew meldeListeNew) throws Exception {
        var konfig = meldeListeNew.getKonfigurationSheet();
        EditierfarbePruefung.pruefeMeldeliste(meldeListeNew.getXSpreadSheet(), ERSTE_DATEN_ZEILE,
                meldeListeNew.getAktivSpalte(), konfig.getMeldeListeHintergrundFarbeGerade(),
                konfig.getMeldeListeHintergrundFarbeUnGerade());
    }
}
