/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.helper.sheet.EditierfarbePruefung;

/**
 * Regressionstest: In der Formule-X-Meldeliste müssen alle editierbaren Spalten (Teamname, Spieler,
 * Verein, SP, Aktiv) die Editierfarbe zeigen. Früher lag die Zeilen-Zebrafarbe als bedingte
 * Formatierung davor und verdeckte sie; Zebra wird jetzt direkt als Zellhintergrund gesetzt.
 */
class FormuleXMeldeListeEditierfarbeUITest extends BaseCalcUITest {

    private static final int ERSTE_DATEN_ZEILE = FormuleXListeDelegate.ERSTE_DATEN_ZEILE;

    @Test
    void alleEditierbarenSpaltenSindHervorgehoben() throws Exception {
        FormuleXMeldeListeSheetNew meldeListeNew = erzeugeMeldeliste();

        pruefe(meldeListeNew);
    }

    @Test
    void zebraBedingteFormatierungAusAltdokumentWirdBeimAktualisierenEntfernt() throws Exception {
        FormuleXMeldeListeSheetNew meldeListeNew = erzeugeMeldeliste();
        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        EditierfarbePruefung.schreibeZweiMeldungen(xSheet, doc, ERSTE_DATEN_ZEILE);
        int letzteZeile = meldeListeNew.getLetzteDatenZeileUseMin();
        for (int spalte = 0; spalte <= meldeListeNew.getAktivSpalte(); spalte++) {
            EditierfarbePruefung.setzeAlteZebraCf(xSheet, spalte, ERSTE_DATEN_ZEILE, letzteZeile);
        }

        new FormuleXMeldeListeSheetUpdate(wkingSpreadsheet).doRun();

        pruefe(meldeListeNew);
    }

    private FormuleXMeldeListeSheetNew erzeugeMeldeliste() throws Exception {
        FormuleXMeldeListeSheetNew meldeListeNew = new FormuleXMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.TRIPLETTE, true, true, 4);
        return meldeListeNew;
    }

    private void pruefe(FormuleXMeldeListeSheetNew meldeListeNew) throws Exception {
        var konfig = meldeListeNew.getKonfigurationSheet();
        EditierfarbePruefung.pruefeMeldeliste(meldeListeNew.getXSpreadSheet(), ERSTE_DATEN_ZEILE,
                meldeListeNew.getAktivSpalte(), konfig.getMeldeListeHintergrundFarbeGerade(),
                konfig.getMeldeListeHintergrundFarbeUnGerade());
    }
}
