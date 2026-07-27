/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Regressionstest für {@code getMeldungenSpalte()}: die Dubletten-Prüfung
 * ({@code MeldeListeHelper.testDoppelteMeldungen()}) muss den vollständigen
 * Namensblock aller Spieler eines Teams vergleichen (Vorname+Nachname je
 * Spieler), nicht nur eine verkürzte, aus der Formation abgeleitete
 * Spaltenanzahl.
 */
class KoMeldeListeDuplikatpruefungUITest extends BaseCalcUITest {

    private KoMeldeListeSheetNew neueMeldeliste() throws Exception {
        KoMeldeListeSheetNew meldeListeNew = new KoMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.getKonfigurationSheet().update();
        meldeListeNew.getKonfigurationSheet().setMeldeListeFormation(Formation.DOUBLETTE);
        meldeListeNew.getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(false);
        meldeListeNew.getKonfigurationSheet().setMeldeListeVereinsnameAnzeigen(false);
        meldeListeNew.getKonfigurationSheet().update();
        meldeListeNew.createMeldelisteWithParams();
        return meldeListeNew;
    }

    @Test
    void unterschiedlicherZweiterSpielerWirdNichtFaelschlichAlsDubletteErkannt() throws Exception {
        KoMeldeListeSheetNew meldeListeNew = neueMeldeliste();
        int ersteDatenZeile = KoListeDelegate.ERSTE_DATEN_ZEILE;

        RangeData data = new RangeData();
        RowData team1 = data.addNewRow();
        team1.newInt(1);
        team1.newString("Anna");
        team1.newString("Müller");
        team1.newString("Peter");
        team1.newString("Schmidt");
        team1.newInt(1); // Ranglistespalte
        team1.newInt(KoListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        RowData team2 = data.addNewRow();
        team2.newInt(2);
        team2.newString("Anna");
        team2.newString("Müller");
        team2.newString("Klaus");
        team2.newString("Weber");
        team2.newInt(2); // Ranglistespalte
        team2.newInt(KoListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);

        assertThatCode(() -> new KoMeldeListeSheetUpdate(wkingSpreadsheet).doRun())
                .as("unterschiedliche zweite Spieler duerfen nicht als Dublette blockiert werden")
                .doesNotThrowAnyException();
    }

    @Test
    void echteDubletteWirdWeiterhinErkannt() throws Exception {
        KoMeldeListeSheetNew meldeListeNew = neueMeldeliste();
        int ersteDatenZeile = KoListeDelegate.ERSTE_DATEN_ZEILE;

        RangeData data = new RangeData();
        RowData team1 = data.addNewRow();
        team1.newInt(1);
        team1.newString("Anna");
        team1.newString("Müller");
        team1.newString("Peter");
        team1.newString("Schmidt");
        team1.newInt(1); // Ranglistespalte
        team1.newInt(KoListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        RowData team2 = data.addNewRow();
        team2.newInt(2);
        team2.newString("Anna");
        team2.newString("Müller");
        team2.newString("Peter");
        team2.newString("Schmidt");
        team2.newInt(2); // Ranglistespalte
        team2.newInt(KoListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);

        assertThatThrownBy(() -> new KoMeldeListeSheetUpdate(wkingSpreadsheet).doRun())
                .isInstanceOf(GenerateException.class);
    }

}
