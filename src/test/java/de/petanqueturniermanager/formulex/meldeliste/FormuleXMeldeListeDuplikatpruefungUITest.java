/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.meldeliste;

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
 * Spaltenanzahl (die {@code anzNamenSpalten}-Default in
 * {@link de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte}
 * entspricht sonst der Spieleranzahl statt der tatsächlichen Spaltenzahl).
 */
class FormuleXMeldeListeDuplikatpruefungUITest extends BaseCalcUITest {

    @Test
    void unterschiedlicherZweiterSpielerWirdNichtFaelschlichAlsDubletteErkannt() throws Exception {
        FormuleXMeldeListeSheetNew meldeListeNew = new FormuleXMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.DOUBLETTE, false, false, 4);

        int ersteDatenZeile = FormuleXListeDelegate.ERSTE_DATEN_ZEILE;

        // Team 1: Anna Müller / Peter Schmidt
        // Team 2: Anna Müller / Klaus Weber  (gleicher Spieler 1, ANDERER Spieler 2 -> keine echte Dublette)
        RangeData data = new RangeData();
        RowData team1 = data.addNewRow();
        team1.newInt(1);
        team1.newString("Anna");
        team1.newString("Müller");
        team1.newString("Peter");
        team1.newString("Schmidt");
        team1.newEmpty();
        team1.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        RowData team2 = data.addNewRow();
        team2.newInt(2);
        team2.newString("Anna");
        team2.newString("Müller");
        team2.newString("Klaus");
        team2.newString("Weber");
        team2.newEmpty();
        team2.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);

        assertThatCode(() -> new FormuleXMeldeListeSheetUpdate(wkingSpreadsheet).doRun())
                .as("unterschiedliche zweite Spieler duerfen nicht als Dublette blockiert werden")
                .doesNotThrowAnyException();
    }

    @Test
    void echteDubletteWirdWeiterhinErkannt() throws Exception {
        FormuleXMeldeListeSheetNew meldeListeNew = new FormuleXMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(Formation.DOUBLETTE, false, false, 4);

        int ersteDatenZeile = FormuleXListeDelegate.ERSTE_DATEN_ZEILE;

        // Team 1 und Team 2: identische Spieler 1 UND Spieler 2 -> echte Dublette
        RangeData data = new RangeData();
        RowData team1 = data.addNewRow();
        team1.newInt(1);
        team1.newString("Anna");
        team1.newString("Müller");
        team1.newString("Peter");
        team1.newString("Schmidt");
        team1.newEmpty();
        team1.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        RowData team2 = data.addNewRow();
        team2.newInt(2);
        team2.newString("Anna");
        team2.newString("Müller");
        team2.newString("Peter");
        team2.newString("Schmidt");
        team2.newEmpty();
        team2.newInt(FormuleXListeDelegate.AKTIV_WERT_NIMMT_TEIL);

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);

        assertThatThrownBy(() -> new FormuleXMeldeListeSheetUpdate(wkingSpreadsheet).doRun())
                .isInstanceOf(GenerateException.class);
    }

}
