package de.petanqueturniermanager.poule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetUpdate;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.poule.vorrunde.AbstractPouleVorrundeSheet;

/**
 * Dokumentiert das aktuelle (statische) Verhalten von Poule-A/B bei einem Ausstieg NACH bereits
 * gespieltem Turnier: anders als bei Schweizer/FormuleX/Kaskade gibt es bei Poule kein "nächste
 * Runde"-Konzept – die komplette Vorrunde (alle Gruppenspiele) wird in einem einzigen Schritt
 * generiert ({@link PouleTurnierTestDaten#generate()}). Ein Ausstieg kann daher keine zukünftige
 * Paarung mehr beeinflussen, weil es keine gibt. Auch die Rangliste ({@link PouleVorrundenRanglisteSheet})
 * liest ausschließlich die bereits eingetragenen Vorrunden-Ergebnisse, nicht die Aktiv-Spalte der
 * Meldeliste – ein nachträglicher Ausstieg bleibt daher folgenlos für Vorrunde UND Rangliste. Dieser
 * Test hält das bewusst fest, damit eine künftige Änderung an dieser Stelle (z.B. Filterung nach
 * Aktiv-Status in der Rangliste) hier auffällt.
 */
public class PouleAusstiegMittenImTurnierUITest extends BaseCalcUITest {

    private static final int AUSGESTIEGENES_TEAM_NR = 1;

    private PouleTurnierTestDaten testDaten;

    @BeforeEach
    public void setup() {
        testDaten = new PouleTurnierTestDaten(wkingSpreadsheet);
    }

    @Test
    public void ausstiegNachTurnierende_aendertVorrundeUndRanglisteNicht() throws GenerateException {
        testDaten.generate();

        List<List<Integer>> vorrundeVorAusstieg = leseVorrundeWerte();
        List<List<Integer>> ranglisteVorAusstieg = leseRanglisteWerte();

        // Team als "ausgestiegen" markieren
        PouleMeldeListeSheetUpdate meldeliste = new PouleMeldeListeSheetUpdate(wkingSpreadsheet);
        int zeile = meldeliste.getSpielerZeileNr(AUSGESTIEGENES_TEAM_NR);
        assertThat(zeile).as("Team %d muss in der Meldeliste gefunden werden", AUSGESTIEGENES_TEAM_NR)
                .isGreaterThan(0);
        sheetHlp.setNumberValueInCell(NumberCellValue
                .from(meldeliste.getXSpreadSheet(), Position.from(meldeliste.getAktivSpalte(), zeile))
                .setValue(PouleMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN));

        // Rangliste erneut aufbauen: darf nicht crashen, und da sie rein aus den bereits
        // eingetragenen Vorrunden-Ergebnissen berechnet wird, muss sie unverändert bleiben.
        new PouleVorrundenRanglisteSheetUpdate(wkingSpreadsheet).doRun();

        List<List<Integer>> vorrundeNachAusstieg = leseVorrundeWerte();
        assertThat(vorrundeNachAusstieg).as("Vorrunde darf durch einen nachträglichen Ausstieg nicht verändert werden")
                .isEqualTo(vorrundeVorAusstieg);

        List<List<Integer>> ranglisteNachAusstieg = leseRanglisteWerte();
        assertThat(ranglisteNachAusstieg)
                .as("Rangliste ignoriert die Aktiv-Spalte und bleibt daher nach einem Ausstieg unverändert")
                .isEqualTo(ranglisteVorAusstieg);

        boolean ausgestiegenesTeamWeiterhinInRangliste = ranglisteNachAusstieg.stream()
                .anyMatch(row -> row.get(PouleVorrundenRanglisteSheet.SPALTE_NR) == AUSGESTIEGENES_TEAM_NR);
        assertThat(ausgestiegenesTeamWeiterhinInRangliste)
                .as("Ausgestiegenes Team bleibt (aktuelles Verhalten) unverändert in der Rangliste")
                .isTrue();
    }

    private List<List<Integer>> leseVorrundeWerte() throws GenerateException {
        XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.pouleVorrunde());
        assertThat(sheet).as("Vorrunde-Sheet muss vorhanden sein").isNotNull();
        RangePosition range = RangePosition.from(
                AbstractPouleVorrundeSheet.SPALTE_POULE_NR, AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE,
                AbstractPouleVorrundeSheet.SPALTE_ERG_B, AbstractPouleVorrundeSheet.ERSTE_DATEN_ZEILE + 200);
        return alsIntListe(RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range)
                .getDataFromRange());
    }

    private List<List<Integer>> leseRanglisteWerte() throws GenerateException {
        XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.pouleVorrundenRangliste());
        assertThat(sheet).as("Rangliste-Sheet muss vorhanden sein").isNotNull();
        RangePosition range = RangePosition.from(
                PouleVorrundenRanglisteSheet.SPALTE_PLATZ, PouleVorrundenRanglisteSheet.HEADER_ZEILEN,
                PouleVorrundenRanglisteSheet.SPALTE_TURNIER, PouleVorrundenRanglisteSheet.HEADER_ZEILEN + 100);
        return alsIntListe(RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range)
                .getDataFromRange());
    }

    /**
     * Liest die Werte als {@code List<List<Integer>>} statt {@code RangeData} –
     * {@link de.petanqueturniermanager.helper.sheet.rangedata.CellData} hat kein {@code equals()},
     * ein direkter {@code RangeData}-Vergleich wäre daher immer Objekt-Identität statt Werte-Vergleich.
     */
    private List<List<Integer>> alsIntListe(RangeData data) {
        List<List<Integer>> werte = new ArrayList<>();
        for (var row : data) {
            List<Integer> zeile = new ArrayList<>();
            for (var cell : row) {
                zeile.add(cell.getIntVal(0));
            }
            werte.add(zeile);
        }
        return werte;
    }
}
