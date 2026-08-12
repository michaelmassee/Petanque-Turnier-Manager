package de.petanqueturniermanager.ko;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetUpdate;

/**
 * Dokumentiert das aktuelle (statische) Verhalten von KO bei einem Ausstieg NACH bereits erstelltem
 * Turnierbaum: der komplette Baum (alle Runden) wird bei der Erstellung in EINEM Schritt als
 * Formel-Baum (WENN-Sieger-Formeln) angelegt ({@link KoTurnierTestDaten#generate()}) – es gibt kein
 * "nächste Runde"-Konzept wie bei Schweizer/FormuleX/Kaskade. Die Aktiv-Spalte der Meldeliste wird
 * ausschließlich EINMALIG bei der Baum-Erstellung gelesen (Auslosung der Startpositionen); danach
 * lösen sich alle Folgerunden rein über Zellformeln auf, ohne die Meldeliste erneut zu befragen.
 * Ein nachträglicher Ausstieg bleibt daher folgenlos für den bereits erstellten Baum. Dieser Test
 * hält das bewusst fest.
 */
public class KoAusstiegMittenImTurnierUITest extends BaseCalcUITest {

    private static final int ANZ_TEAMS = 8;
    private static final int AUSGESTIEGENES_TEAM_NR = 1;

    private KoTurnierTestDaten testDaten;

    @BeforeEach
    public void setup() {
        testDaten = new KoTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS);
    }

    @Test
    public void ausstiegNachTurnierbaumErstellung_aendertBaumNicht() throws GenerateException {
        testDaten.generate();

        String baumSheetName = SheetNamen.koTurnierbaumEinzel();
        List<List<String>> baumVorAusstieg = leseBaumWerte(baumSheetName);

        // Team als "ausgestiegen" markieren
        KoMeldeListeSheetUpdate meldeliste = new KoMeldeListeSheetUpdate(wkingSpreadsheet);
        int zeile = meldeliste.getSpielerZeileNr(AUSGESTIEGENES_TEAM_NR);
        assertThat(zeile).as("Team %d muss in der Meldeliste gefunden werden", AUSGESTIEGENES_TEAM_NR)
                .isGreaterThan(0);
        sheetHlp.setNumberValueInCell(NumberCellValue
                .from(meldeliste.getXSpreadSheet(), Position.from(meldeliste.getAktivSpalte(), zeile))
                .setValue(KoMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN));

        // Neu berechnen, falls irgendeine Formel doch (indirekt) auf die Meldeliste verweisen sollte
        XCalculatable xCal = Lo.qi(XCalculatable.class, wkingSpreadsheet.getWorkingSpreadsheetDocument());
        if (xCal != null) {
            xCal.calculateAll();
        }

        List<List<String>> baumNachAusstieg = leseBaumWerte(baumSheetName);
        assertThat(baumNachAusstieg)
                .as("Turnierbaum darf durch einen nachträglichen Ausstieg nicht verändert werden "
                        + "(kein Regenerierungs-Mechanismus vorhanden)")
                .isEqualTo(baumVorAusstieg);
    }

    private List<List<String>> leseBaumWerte(String sheetName) throws GenerateException {
        XSpreadsheet sheet = sheetHlp.findByName(sheetName);
        assertThat(sheet).as(sheetName + " muss vorhanden sein").isNotNull();
        RangePosition range = RangePosition.from(0, 0, 30, 150);
        RangeData data = RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range)
                .getDataFromRange();

        List<List<String>> werte = new ArrayList<>();
        for (var row : data) {
            List<String> zeile = new ArrayList<>();
            for (var cell : row) {
                zeile.add(cell.getStringVal());
            }
            werte.add(zeile);
        }
        return werte;
    }
}
