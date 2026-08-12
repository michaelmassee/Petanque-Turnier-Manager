package de.petanqueturniermanager.maastrichter;

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
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Regression: ein Team steigt NACH bereits gespielter Vorrunde 1 aus (Aktiv-Spalte = 2). Die
 * Vorrunden laufen komplett über das Schweizer-System (Maastrichter nutzt {@code SchweizerListeDelegate}
 * / {@code SchweizerRanglisteSheet} 1:1), das grundsätzliche Ausstieg-Verhalten dort ist bereits über
 * {@code SchweizerAusstiegMittenImTurnierUITest} abgesichert. Dieser Test prüft zusätzlich den
 * Maastrichter-spezifischen Teil: die Finalrunden-Erstellung (Gruppeneinteilung nach Vorrunden-Rangliste)
 * darf mit einem ausgestiegenen Team nicht crashen.
 */
public class MaastrichterAusstiegMittenImTurnierUITest extends BaseCalcUITest {

    private static final int ANZ_VORRUNDEN = 3;
    private static final int ANZ_TEAMS = 12;
    private static final int AUSGESTIEGENES_TEAM_NR = 1;

    private MaastrichterTurnierTestDaten testDaten;

    @BeforeEach
    public void setup() {
        testDaten = new MaastrichterTurnierTestDaten(wkingSpreadsheet);
    }

    @Test
    public void ausstiegNachVorrunde1_wirdInVorrunde2NichtMehrGepaartUndFinaleErstelltSichKorrekt()
            throws GenerateException {
        // Vorrunde 1 spielen, noch keine Rangliste/Finale
        testDaten.generate(1, false);

        String vorrunde1SheetName = SheetNamen.spielrunde(1);
        List<List<Integer>> vorrunde1VorAusstieg = leseSpielpaarungWerte(vorrunde1SheetName);

        // Team als "ausgestiegen" markieren
        MaastrichterMeldeListeSheetUpdate meldeliste = new MaastrichterMeldeListeSheetUpdate(wkingSpreadsheet);
        int zeile = meldeliste.getSpielerZeileNr(AUSGESTIEGENES_TEAM_NR);
        assertThat(zeile).as("Team %d muss in der Meldeliste gefunden werden", AUSGESTIEGENES_TEAM_NR)
                .isGreaterThan(0);
        sheetHlp.setNumberValueInCell(NumberCellValue
                .from(meldeliste.getXSpreadSheet(), Position.from(meldeliste.getAktivSpalte(), zeile))
                .setValue(MaastrichterMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN));

        // Vorrunden 2 und 3 auf der bestehenden Meldeliste erzeugen (ohne diese neu anzulegen)
        for (int runde = 2; runde <= ANZ_VORRUNDEN; runde++) {
            testDaten.naechsteVorrunde.erstelleNaechsteVorrunde();
            String legacyName = runde + ". " + SheetNamen.LEGACY_MAASTRICHTER_VORRUNDE_PRAEFIX;
            XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(
                    wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                    SheetMetadataHelper.schluesselMaastrichterVorrunde(runde), legacyName);
            assertThat(sheet).as("Vorrunde " + runde + " muss vorhanden sein").isNotNull();
            testDaten.ergebnisseEinfuegen(sheet);
        }

        // Vorrunde 1: Ergebnisse unverändert (Ausstieg wirkt nicht rückwirkend)
        List<List<Integer>> vorrunde1NachAusstieg = leseSpielpaarungWerte(vorrunde1SheetName);
        assertThat(vorrunde1NachAusstieg).as("Vorrunde 1 darf durch den Ausstieg nicht verändert werden")
                .isEqualTo(vorrunde1VorAusstieg);

        // Vorrunde 2: das ausgestiegene Team taucht in keiner Paarung mehr auf
        String vorrunde2SheetName = SheetNamen.spielrunde(2);
        List<List<Integer>> vorrunde2Paarungen = leseSpielpaarungWerte(vorrunde2SheetName);
        for (var row : vorrunde2Paarungen) {
            assertThat(row.get(0)).as("Ausgestiegenes Team darf nicht mehr als Team A gepaart werden")
                    .isNotEqualTo(AUSGESTIEGENES_TEAM_NR);
            assertThat(row.get(1)).as("Ausgestiegenes Team darf nicht mehr als Team B gepaart werden")
                    .isNotEqualTo(AUSGESTIEGENES_TEAM_NR);
        }

        // Rangliste + Finalrunden erzeugen: darf mit dem ausgestiegenen Team nicht crashen
        testDaten.ranglisteSheet.doRun();

        RangePosition ranglisteRange = RangePosition.from(
                MaastrichterVorrundenRanglisteSheet.TEAM_NR_SPALTE, MaastrichterVorrundenRanglisteSheet.ERSTE_DATEN_ZEILE,
                MaastrichterVorrundenRanglisteSheet.SIEGE_SPALTE,
                MaastrichterVorrundenRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 2);
        XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.maastrichterVorrundenRangliste());
        assertThat(rangliste).as("Rangliste-Sheet").isNotNull();
        RangeData ranglisteDaten = RangeHelper
                .from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
                .getDataFromRange();
        assertThat(ranglisteDaten)
                .as("Rangliste muss die " + (ANZ_TEAMS - 1) + " weiterhin aktiven Teams listen")
                .hasSize(ANZ_TEAMS - 1);
        boolean ausgestiegenesTeamInRangliste = ranglisteDaten.stream()
                .anyMatch(row -> row.get(MaastrichterVorrundenRanglisteSheet.TEAM_NR_SPALTE).getIntVal(-1)
                        == AUSGESTIEGENES_TEAM_NR);
        assertThat(ausgestiegenesTeamInRangliste)
                .as("Ausgestiegenes Team wird konsequent nicht mehr in der Rangliste geführt")
                .isFalse();

        testDaten.finalrundeSheet.doRun();
        XSpreadsheet finaleGruppeA = sheetHlp.findByName(SheetNamen.koFinaleGruppe("A"));
        assertThat(finaleGruppeA).as("Finalrunden-Gruppe A muss nach Turnierabschluss vorhanden sein").isNotNull();
    }

    private List<List<Integer>> leseSpielpaarungWerte(String sheetName) throws GenerateException {
        XSpreadsheet sheet = sheetHlp.findByName(sheetName);
        assertThat(sheet).as(sheetName + " muss vorhanden sein").isNotNull();
        RangePosition paarungenRange = RangePosition.from(
                SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
                SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 100);
        RangeData data = RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), paarungenRange)
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
