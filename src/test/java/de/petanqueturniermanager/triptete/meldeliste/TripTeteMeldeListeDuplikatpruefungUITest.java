/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.triptete.meldeliste;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Regressionstest für {@code getMeldungenSpalte()}: die Dubletten-Prüfung
 * ({@code MeldeListeHelper.testDoppelteMeldungen()}) muss den vollständigen
 * Namensblock aller drei Spieler eines Teams vergleichen, nicht nur einen
 * abgeschnittenen Block.
 */
class TripTeteMeldeListeDuplikatpruefungUITest extends BaseCalcUITest {

    private static final int ERSTE_DATEN_ZEILE = TripTeteMeldeListeDelegate.ERSTE_DATEN_ZEILE_OVERRIDE;

    private TripTeteMeldeListeSheetNew neueMeldeliste() throws Exception {
        TripTeteMeldeListeSheetNew meldeListeNew = new TripTeteMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.getKonfigurationSheet().setMeldeListeTeamnameAnzeigen(false);
        meldeListeNew.createMeldeliste();
        return meldeListeNew;
    }

    @Test
    void unterschiedlicherDritterSpielerWirdNichtFaelschlichAlsDubletteErkannt() throws Exception {
        TripTeteMeldeListeSheetNew meldeListeNew = neueMeldeliste();

        RangeData data = new RangeData();
        RowData team1 = data.addNewRow();
        team1.newInt(1);
        team1.newString("Anna");
        team1.newString("Müller");
        team1.newString("Peter");
        team1.newString("Schmidt");
        team1.newString("Klaus");
        team1.newString("Weber");
        team1.newInt(1); // Aktiv

        RowData team2 = data.addNewRow();
        team2.newInt(2);
        team2.newString("Anna");
        team2.newString("Müller");
        team2.newString("Peter");
        team2.newString("Schmidt");
        team2.newString("Sabine");
        team2.newString("Roth");
        team2.newInt(1); // Aktiv

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ERSTE_DATEN_ZEILE))).setDataInRange(data);

        assertThatCode(() -> new TripTeteMeldeListeSheetUpdate(wkingSpreadsheet).doRun())
                .as("unterschiedlicher dritter Spieler darf nicht als Dublette blockiert werden")
                .doesNotThrowAnyException();
    }

    @Test
    void echteDubletteWirdWeiterhinErkannt() throws Exception {
        TripTeteMeldeListeSheetNew meldeListeNew = neueMeldeliste();

        RangeData data = new RangeData();
        RowData team1 = data.addNewRow();
        team1.newInt(1);
        team1.newString("Anna");
        team1.newString("Müller");
        team1.newString("Peter");
        team1.newString("Schmidt");
        team1.newString("Klaus");
        team1.newString("Weber");
        team1.newInt(1); // Aktiv

        RowData team2 = data.addNewRow();
        team2.newInt(2);
        team2.newString("Anna");
        team2.newString("Müller");
        team2.newString("Peter");
        team2.newString("Schmidt");
        team2.newString("Klaus");
        team2.newString("Weber");
        team2.newInt(1); // Aktiv

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ERSTE_DATEN_ZEILE))).setDataInRange(data);

        assertThatThrownBy(() -> new TripTeteMeldeListeSheetUpdate(wkingSpreadsheet).doRun())
                .isInstanceOf(GenerateException.class);
    }

}
