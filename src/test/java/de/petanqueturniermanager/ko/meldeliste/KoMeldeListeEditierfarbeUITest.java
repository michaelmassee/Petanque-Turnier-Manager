/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.sheet.EditierfarbePruefung;

/**
 * Regressionstest: In der K.-O.-Meldeliste müssen alle editierbaren Spalten (Teamname, Spieler,
 * Verein, RNG, Aktiv) die Editierfarbe zeigen. Früher lag die Zeilen-Zebrafarbe als bedingte
 * Formatierung davor und verdeckte sie; Zebra wird jetzt direkt als Zellhintergrund gesetzt.
 */
class KoMeldeListeEditierfarbeUITest extends BaseCalcUITest {

    private static final int ERSTE_DATEN_ZEILE = KoListeDelegate.ERSTE_DATEN_ZEILE;

    @Test
    void alleEditierbarenSpaltenSindHervorgehoben() throws Exception {
        KoMeldeListeSheetNew meldeListeNew = erzeugeMeldeliste();

        pruefe(meldeListeNew);
    }

    @Test
    void zebraBedingteFormatierungAusAltdokumentWirdBeimAktualisierenEntfernt() throws Exception {
        KoMeldeListeSheetNew meldeListeNew = erzeugeMeldeliste();
        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        EditierfarbePruefung.schreibeZweiMeldungen(xSheet, doc, ERSTE_DATEN_ZEILE);
        int letzteZeile = meldeListeNew.getLetzteDatenZeileUseMin();
        for (int spalte = 0; spalte <= meldeListeNew.getAktivSpalte(); spalte++) {
            EditierfarbePruefung.setzeAlteZebraCf(xSheet, spalte, ERSTE_DATEN_ZEILE, letzteZeile);
        }

        new KoMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        pruefe(meldeListeNew);
    }

    private KoMeldeListeSheetNew erzeugeMeldeliste() throws Exception {
        KoMeldeListeSheetNew meldeListeNew = new KoMeldeListeSheetNew(wkingSpreadsheet);
        var konfig = meldeListeNew.getKonfigurationSheet();
        konfig.update();
        konfig.setMeldeListeFormation(Formation.TRIPLETTE);
        konfig.setMeldeListeTeamnameAnzeigen(true);
        konfig.setMeldeListeVereinsnameAnzeigen(true);
        konfig.update();
        meldeListeNew.createMeldelisteWithParams();
        return meldeListeNew;
    }

    private void pruefe(KoMeldeListeSheetNew meldeListeNew) throws Exception {
        var konfig = meldeListeNew.getKonfigurationSheet();
        EditierfarbePruefung.pruefeMeldeliste(meldeListeNew.getXSpreadSheet(), ERSTE_DATEN_ZEILE,
                meldeListeNew.getAktivSpalte(), konfig.getMeldeListeHintergrundFarbeGerade(),
                konfig.getMeldeListeHintergrundFarbeUnGerade());
    }
}
