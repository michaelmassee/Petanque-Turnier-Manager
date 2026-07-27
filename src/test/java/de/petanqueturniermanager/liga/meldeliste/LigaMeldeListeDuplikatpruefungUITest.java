/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.liga.meldeliste;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Regressionstest: Liga nutzte {@code MeldeListeHelper}/{@code IMeldeliste}
 * bereits vor der Kompaktierungs-Erweiterung der anderen Systeme unverändert.
 * Prüft, dass die Dubletten-Prüfung ({@code MeldeListeHelper.testDoppelteMeldungen()})
 * bei Liga (eine einzige Namensspalte je Mannschaft) weiterhin korrekt arbeitet.
 */
class LigaMeldeListeDuplikatpruefungUITest extends BaseCalcUITest {

    private static final int ERSTE_DATEN_ZEILE = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;

    private LigaMeldeListeSheetNew neueMeldeliste() throws Exception {
        LigaMeldeListeSheetNew meldeListeNew = new LigaMeldeListeSheetNew(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams("TestGruppe");
        return meldeListeNew;
    }

    @Test
    void unterschiedlicheMannschaftsnamenWerdenNichtAlsDubletteErkannt() throws Exception {
        LigaMeldeListeSheetNew meldeListeNew = neueMeldeliste();

        RangeData data = new RangeData();
        RowData team1 = data.addNewRow();
        team1.newEmpty(); // Nr
        team1.newString("BC-Linden 1");

        RowData team2 = data.addNewRow();
        team2.newEmpty(); // Nr
        team2.newString("Boule Biebertal");

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ERSTE_DATEN_ZEILE))).setDataInRange(data);

        assertThatCode(() -> new LigaMeldeListeSheetUpdate(wkingSpreadsheet).doRun())
                .as("unterschiedliche Mannschaftsnamen duerfen nicht als Dublette blockiert werden")
                .doesNotThrowAnyException();
    }

    @Test
    void echteDubletteWirdWeiterhinErkannt() throws Exception {
        LigaMeldeListeSheetNew meldeListeNew = neueMeldeliste();

        RangeData data = new RangeData();
        RowData team1 = data.addNewRow();
        team1.newEmpty(); // Nr
        team1.newString("BC-Linden 1");

        RowData team2 = data.addNewRow();
        team2.newEmpty(); // Nr
        team2.newString("BC-Linden 1");

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ERSTE_DATEN_ZEILE))).setDataInRange(data);

        assertThatThrownBy(() -> new LigaMeldeListeSheetUpdate(wkingSpreadsheet).doRun())
                .isInstanceOf(GenerateException.class);
    }

}
