/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.meldeliste;

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
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Regressionstest: Supermelee nutzte {@code MeldeListeHelper}/{@code IMeldeliste}
 * bereits vor der Kompaktierungs-Erweiterung der anderen Systeme unverändert
 * und setzt {@code anzNamenSpalten(2)} explizit (Vorname+Nachname). Prüft, dass
 * die Dubletten-Prüfung ({@code MeldeListeHelper.testDoppelteMeldungen()}) davon
 * unbeeinflusst weiterhin korrekt arbeitet.
 */
class SupermeleeMeldeListeDuplikatpruefungUITest extends BaseCalcUITest {

    private MeldeListeSheet_New neueMeldeliste() throws Exception {
        MeldeListeSheet_New meldeListeNew = new MeldeListeSheet_New(wkingSpreadsheet);
        meldeListeNew.createMeldelisteWithParams(SuperMeleeMode.Triplette);
        return meldeListeNew;
    }

    @Test
    void unterschiedlicheSpielerWerdenNichtFaelschlichAlsDubletteErkannt() throws Exception {
        MeldeListeSheet_New meldeListeNew = neueMeldeliste();

        int ersteDatenZeile = meldeListeNew.getMeldungenSpalte().getErsteDatenZiele();
        int spielerNameErsteSpalte = meldeListeNew.getSpielerNameErsteSpalte();

        RangeData data = new RangeData();
        RowData spieler1 = data.addNewRow();
        spieler1.newString("Anna");
        spieler1.newString("Müller");
        spieler1.newEmpty(); // Setzposition
        spieler1.newInt(1); // alle spielen

        RowData spieler2 = data.addNewRow();
        spieler2.newString("Peter");
        spieler2.newString("Müller");
        spieler2.newEmpty(); // Setzposition
        spieler2.newInt(1); // alle spielen

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(spielerNameErsteSpalte, ersteDatenZeile)))
                .setDataInRange(data);

        assertThatCode(() -> new MeldeListeSheet_Update(wkingSpreadsheet).doRun())
                .as("unterschiedliche Spieler duerfen nicht als Dublette blockiert werden")
                .doesNotThrowAnyException();
    }

    @Test
    void echteDubletteWirdWeiterhinErkannt() throws Exception {
        MeldeListeSheet_New meldeListeNew = neueMeldeliste();

        int ersteDatenZeile = meldeListeNew.getMeldungenSpalte().getErsteDatenZiele();
        int spielerNameErsteSpalte = meldeListeNew.getSpielerNameErsteSpalte();

        RangeData data = new RangeData();
        RowData spieler1 = data.addNewRow();
        spieler1.newString("Anna");
        spieler1.newString("Müller");
        spieler1.newEmpty(); // Setzposition
        spieler1.newInt(1); // alle spielen

        RowData spieler2 = data.addNewRow();
        spieler2.newString("Anna");
        spieler2.newString("Müller");
        spieler2.newEmpty(); // Setzposition
        spieler2.newInt(1); // alle spielen

        XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
        RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(spielerNameErsteSpalte, ersteDatenZeile)))
                .setDataInRange(data);

        assertThatThrownBy(() -> new MeldeListeSheet_Update(wkingSpreadsheet).doRun())
                .isInstanceOf(GenerateException.class);
    }

}
