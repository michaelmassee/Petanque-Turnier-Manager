package de.petanqueturniermanager.formulex.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetUpdate;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Regression: ein Team steigt NACH bereits gespielter Runde 1 aus (Aktiv-Spalte = 2).
 * Erwartet (aktuelles, konsistentes Verhalten, Pétanque-Konvention Schweizer System): Runde 2 paart
 * das Team nicht mehr, Runde-1-Ergebnisse bleiben unverändert stehen. Die Rangliste bleibt dagegen
 * mit dem bisherigen Ergebnis des ausgestiegenen Teams stehen ({@code getAktiveUndAusgesetztMeldungen()})
 * – ein vorzeitiger Ausstieg darf weder das eigene, bereits erspielte Ergebnis noch das der Gegner
 * verfälschen.
 */
public class FormuleXAusstiegMittenImTurnierUITest extends BaseCalcUITest {

    private static final int ANZ_TEAMS = FormuleXTurnierTestDaten.ANZ_TEAMS;
    private static final int AUSGESTIEGENES_TEAM_NR = 1;

    private FormuleXTurnierTestDaten testDaten;

    @BeforeEach
    public void setup() {
        testDaten = new FormuleXTurnierTestDaten(wkingSpreadsheet);
    }

    @Test
    public void ausstiegNachRunde1_wirdInRunde2NichtMehrGepaart() throws GenerateException {
        // Runde 1 spielen, noch keine Rangliste
        testDaten.generate(1, false);

        String runde1SheetName = testDaten.naechsteSpielrunde.getSheetName(SpielRundeNr.from(1));
        List<List<Integer>> runde1VorAusstieg = leseSpielpaarungWerte(runde1SheetName);

        // Team als "ausgestiegen" markieren
        FormuleXMeldeListeSheetUpdate meldeliste = new FormuleXMeldeListeSheetUpdate(wkingSpreadsheet);
        int zeile = meldeliste.getSpielerZeileNr(AUSGESTIEGENES_TEAM_NR);
        assertThat(zeile).as("Team %d muss in der Meldeliste gefunden werden", AUSGESTIEGENES_TEAM_NR)
                .isGreaterThan(0);
        sheetHlp.setNumberValueInCell(NumberCellValue
                .from(meldeliste.getXSpreadSheet(), Position.from(meldeliste.getAktivSpalte(), zeile))
                .setValue(FormuleXMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN));

        // Runde 2 erzeugen und mit Ergebnissen füllen
        testDaten.naechsteSpielrunde.doRun();
        String runde2SheetName = testDaten.naechsteSpielrunde.getSheetName(SpielRundeNr.from(2));
        XSpreadsheet runde2Sheet = sheetHlp.findByName(runde2SheetName);
        assertThat(runde2Sheet).as(runde2SheetName + " muss vorhanden sein").isNotNull();
        testDaten.ergebnisseEinfuegen(runde2Sheet);

        // Runde 1: Ergebnisse unverändert (Ausstieg wirkt nicht rückwirkend)
        List<List<Integer>> runde1NachAusstieg = leseSpielpaarungWerte(runde1SheetName);
        assertThat(runde1NachAusstieg).as("Runde 1 darf durch den Ausstieg nicht verändert werden")
                .isEqualTo(runde1VorAusstieg);

        // Runde 2: das ausgestiegene Team taucht in keiner Paarung mehr auf
        List<List<Integer>> runde2Paarungen = leseSpielpaarungWerte(runde2SheetName);
        var gepaarteNrn = new java.util.HashSet<Integer>();
        for (var row : runde2Paarungen) {
            int nrA = row.get(0);
            int nrB = row.get(1);
            assertThat(nrA).as("Ausgestiegenes Team darf nicht mehr als Team A gepaart werden")
                    .isNotEqualTo(AUSGESTIEGENES_TEAM_NR);
            assertThat(nrB).as("Ausgestiegenes Team darf nicht mehr als Team B gepaart werden")
                    .isNotEqualTo(AUSGESTIEGENES_TEAM_NR);
            if (nrA > 0) {
                gepaarteNrn.add(nrA);
            }
            if (nrB > 0) {
                gepaarteNrn.add(nrB);
            }
        }
        assertThat(gepaarteNrn).as("Alle " + (ANZ_TEAMS - 1) + " weiterhin aktiven Teams müssen in Runde 2 gepaart sein")
                .hasSize(ANZ_TEAMS - 1)
                .doesNotContain(AUSGESTIEGENES_TEAM_NR);

        // Rangliste erzeugen: darf nicht crashen. FormuleXRanglisteSheet baut aus
        // getAktiveUndAusgesetztMeldungen() auf – das ausgestiegene Team bleibt mit seinem
        // bisherigen (Runde-1-)Ergebnis in der Rangliste stehen, wird aber (siehe oben) nicht mehr
        // neu gepaart.
        FormuleXRanglisteSheet ranglisteSheet = new FormuleXRanglisteSheet(wkingSpreadsheet);
        ranglisteSheet.doRun();

        XSpreadsheet rangliste = sheetHlp.findByName(de.petanqueturniermanager.helper.i18n.SheetNamen.formulexRangliste());
        assertThat(rangliste).as("Rangliste-Sheet").isNotNull();

        RangePosition ranglisteRange = RangePosition.from(
                FormuleXRanglisteSheet.TEAM_NR_SPALTE, FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE,
                FormuleXRanglisteSheet.SIEGE_SPALTE, FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);
        RangeData ranglisteData = RangeHelper
                .from(rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument(), ranglisteRange)
                .getDataFromRange();
        assertThat(ranglisteData)
                .as("Rangliste muss weiterhin alle " + ANZ_TEAMS + " Teams listen (inkl. ausgestiegenem)")
                .hasSize(ANZ_TEAMS);

        boolean ausgestiegenesTeamInRangliste = ranglisteData.stream()
                .anyMatch(row -> row.get(FormuleXRanglisteSheet.TEAM_NR_SPALTE).getIntVal(-1)
                        == AUSGESTIEGENES_TEAM_NR);
        assertThat(ausgestiegenesTeamInRangliste)
                .as("Ausgestiegenes Team bleibt mit seinem bisherigen Ergebnis in der Rangliste")
                .isTrue();

        // Regression: jede echte Paarungszeile (Runde 1 UND Runde 2, beide sind zu diesem Zeitpunkt
        // bereits gespielt) vergibt genau einen Sieg (echter Gewinn oder Freilos). Die Siege-Summe
        // über die GESAMTE Rangliste muss dieser Zeilenzahl entsprechen - würde der Gegner des
        // ausgestiegenen Teams seinen bereits erspielten Sieg verlieren, fehlte hier ein Sieg.
        int erwarteteSiege = zaehleGueltigeZeilen(runde1VorAusstieg) + zaehleGueltigeZeilen(runde2Paarungen);
        int siegeSumme = ranglisteData.stream()
                .mapToInt(row -> row.get(FormuleXRanglisteSheet.SIEGE_SPALTE).getIntVal(0))
                .sum();
        assertThat(siegeSumme)
                .as("Kein Sieg darf durch den Ausstieg des Gegners aus der Rangliste-Summe verschwinden")
                .isEqualTo(erwarteteSiege);
    }

    /**
     * Zählt die echten Paarungszeilen (bis zur ersten Zeile ohne gültige Team-A-Nr).
     */
    private int zaehleGueltigeZeilen(List<List<Integer>> paarungen) {
        int anzahl = 0;
        for (var row : paarungen) {
            if (row.get(0) <= 0) {
                break;
            }
            anzahl++;
        }
        return anzahl;
    }

    /**
     * Liest Team-A-/Team-B-/Ergebnis-Werte als {@code List<List<Integer>>} statt {@code RangeData} –
     * {@link de.petanqueturniermanager.helper.sheet.rangedata.CellData} hat kein {@code equals()},
     * ein direkter {@code RangeData}-Vergleich wäre daher immer Objekt-Identität statt Werte-Vergleich.
     */
    private List<List<Integer>> leseSpielpaarungWerte(String sheetName) throws GenerateException {
        XSpreadsheet sheet = sheetHlp.findByName(sheetName);
        assertThat(sheet).as(sheetName + " muss vorhanden sein").isNotNull();
        RangePosition paarungenRange = RangePosition.from(
                FormuleXAbstractSpielrundeSheet.TEAM_A_SPALTE, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
                FormuleXAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 100);
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
