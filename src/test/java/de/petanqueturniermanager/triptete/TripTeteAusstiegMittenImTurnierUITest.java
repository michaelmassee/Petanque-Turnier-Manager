package de.petanqueturniermanager.triptete;

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
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetUpdate;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheetTestDaten;

/**
 * Dokumentiert das aktuelle Verhalten von TripTete bei einem Ausstieg NACH bereits erstelltem
 * Spielplan: wie bei Poule/KO wird der komplette Spielplan (Jeder-gegen-Jeden aller aktiven Teams)
 * in einem einzigen Schritt erzeugt ({@link TripTeteSpielPlanSheetTestDaten#generate()}) – es gibt
 * kein "nächste Runde"-Konzept. Ein Ausstieg kann daher keine zukünftige Paarung mehr beeinflussen.
 * <p>
 * Die Rangliste ({@link TripTeteRanglisteSheet#upDateSheet()}) registriert zwar vorab nur über
 * {@code getAlleMeldungen()} gefilterte (Aktiv==1) Teams, aber {@code TripTeteRangliste.addBegegnung()}
 * registriert beide Teams jeder gespielten Begegnung automatisch nach (dokumentierte Absicht, siehe
 * dortiger Javadoc). Da der Spielplan statisch bleibt, taucht ein Team mit bereits gespielten
 * Begegnungen daher IMMER weiter in der Rangliste auf – exakt dieselbe "Rangliste = historischer
 * Spielstand, kein Live-Roster"-Philosophie wie bei Poule. Dieser Test hält das bewusst fest.
 */
public class TripTeteAusstiegMittenImTurnierUITest extends BaseCalcUITest {

    private static final int AUSGESTIEGENES_TEAM_NR = 1;

    private TripTeteSpielPlanSheetTestDaten testDaten;

    @BeforeEach
    public void setup() {
        testDaten = new TripTeteSpielPlanSheetTestDaten(wkingSpreadsheet);
    }

    @Test
    public void ausstiegNachTurnierende_aendertSpielplanNichtBleibtAberInRangliste() throws GenerateException {
        testDaten.generate();

        List<List<Integer>> spielplanVorAusstieg = leseSpielplanWerte();

        // Team als "ausgestiegen" markieren
        TripTeteMeldeListeSheetUpdate meldeliste = new TripTeteMeldeListeSheetUpdate(wkingSpreadsheet);
        int zeile = meldeliste.getSpielerZeileNr(AUSGESTIEGENES_TEAM_NR);
        assertThat(zeile).as("Team %d muss in der Meldeliste gefunden werden", AUSGESTIEGENES_TEAM_NR)
                .isGreaterThan(0);
        sheetHlp.setNumberValueInCell(NumberCellValue
                .from(meldeliste.getXSpreadSheet(), Position.from(meldeliste.getAktivSpalte(), zeile))
                .setValue(TripTeteMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN));

        // Spielplan darf durch den nachträglichen Ausstieg nicht verändert werden - es gibt keinen
        // Regenerierungs-Mechanismus, der ihn überhaupt anfassen würde.
        List<List<Integer>> spielplanNachAusstieg = leseSpielplanWerte();
        assertThat(spielplanNachAusstieg)
                .as("Spielplan darf durch einen nachträglichen Ausstieg nicht verändert werden")
                .isEqualTo(spielplanVorAusstieg);

        // Rangliste erneut aufbauen: darf nicht crashen, das ausgestiegene Team bleibt (mit seinen
        // bereits gespielten Begegnungen) weiterhin in der Rangliste stehen.
        new TripTeteRanglisteSheet(wkingSpreadsheet).upDateSheet();

        XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
        assertThat(rangliste).as("Rangliste-Sheet").isNotNull();
        RangePosition ranglisteRange = RangePosition.from(
                TripTeteRanglisteSheet.TEAM_NR_SPALTE, TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE,
                TripTeteRanglisteSheet.LETZTE_SPALTE, TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE + 20);
        RangeData ranglisteDaten = RangeHelper
                .from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
                .getDataFromRange();
        boolean ausgestiegenesTeamInRangliste = ranglisteDaten.stream()
                .anyMatch(row -> !row.isEmpty()
                        && row.get(TripTeteRanglisteSheet.TEAM_NR_SPALTE).getIntVal(-1) == AUSGESTIEGENES_TEAM_NR);
        assertThat(ausgestiegenesTeamInRangliste)
                .as("Ausgestiegenes Team bleibt (aktuelles Verhalten, wie bei Poule) in der Rangliste stehen")
                .isTrue();
    }

    private List<List<Integer>> leseSpielplanWerte() throws GenerateException {
        XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.spielplan());
        assertThat(sheet).as("Spielplan-Sheet muss vorhanden sein").isNotNull();
        RangePosition range = RangePosition.from(
                TripTeteSpielPlanSheet.SPIEL_NR_SPALTE, TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE,
                TripTeteSpielPlanSheet.SP_PUNKTE_B, TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE + 100);
        RangeData data = RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range)
                .getDataFromRange();

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
