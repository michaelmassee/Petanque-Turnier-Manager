package de.petanqueturniermanager.kaskade;

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
import de.petanqueturniermanager.kaskade.konfiguration.KaskadeKonfigurationSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetUpdate;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet;

/**
 * Regression: ein Team steigt NACH bereits gespielter Kaskadenrunde 1 aus (Aktiv-Spalte = 2).
 * <p>
 * Die Plan-Form ({@code KaskadenKoRundenPlaner}) bleibt ab Runde 1 fixiert (siehe
 * {@code KaskadeKonfigurationSheet.getAnzahlTeamsRunde1()}) – ein Ausstieg ändert also nicht mehr
 * die Team-Anzahl, mit der Folgerunden geplant werden. Ein ausgestiegenes Team bekommt stattdessen
 * ab seiner nächsten fälligen Partie automatisch ein Freispiel-Walkover-Ergebnis und läuft über den
 * Verlierer-Pfad aus dem Turnier, statt echt weitergepaart zu werden. Dieser Test prüft: Runde 2
 * paart das ausgestiegene Team nicht mehr, Runde-1-Ergebnisse bleiben unverändert, und die
 * anschließende Gruppenrangliste-Aktualisierung (Teil von {@code naechsteRunde()}) läuft ohne
 * Fehler durch.
 */
public class KaskadeAusstiegMittenImTurnierUITest extends BaseCalcUITest {

    private static final int AUSGESTIEGENES_TEAM_NR = 1;

    private KaskadeTurnierTestDaten testDaten;

    @BeforeEach
    public void setup() {
        testDaten = new KaskadeTurnierTestDaten(wkingSpreadsheet);
    }

    @Test
    public void ausstiegNachRunde1_erhaeltInRunde2AutomatischesWalkover() throws GenerateException {
        // Kaskadenrunde 1 spielen, kein Abschluss (keine Gruppenrangliste/KO-Felder)
        testDaten.generate(1, false);

        String runde1SheetName = SheetNamen.kaskadenRunde(1);
        List<List<Integer>> runde1VorAusstieg = leseSpielpaarungWerte(runde1SheetName);

        // Team als "ausgestiegen" markieren
        KaskadeMeldeListeSheetUpdate meldeliste = new KaskadeMeldeListeSheetUpdate(wkingSpreadsheet);
        int zeile = meldeliste.getSpielerZeileNr(AUSGESTIEGENES_TEAM_NR);
        assertThat(zeile).as("Team %d muss in der Meldeliste gefunden werden", AUSGESTIEGENES_TEAM_NR)
                .isGreaterThan(0);
        sheetHlp.setNumberValueInCell(NumberCellValue
                .from(meldeliste.getXSpreadSheet(), Position.from(meldeliste.getAktivSpalte(), zeile))
                .setValue(KaskadeMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN));

        // Kaskadenrunde 2 erzeugen (löst intern auch die Gruppenrangliste-Aktualisierung für Runde 1 aus)
        testDaten.spielrundeSheet.setForceOk(true);
        testDaten.spielrundeSheet.naechsteRunde();
        testDaten.ergebnisseEinfuegen(2);

        String runde2SheetName = SheetNamen.kaskadenRunde(2);
        XSpreadsheet runde2Sheet = sheetHlp.findByName(runde2SheetName);
        assertThat(runde2Sheet).as(runde2SheetName + " muss vorhanden sein").isNotNull();

        // Runde 1: Ergebnisse unverändert (Ausstieg wirkt nicht rückwirkend)
        List<List<Integer>> runde1NachAusstieg = leseSpielpaarungWerte(runde1SheetName);
        assertThat(runde1NachAusstieg).as("Runde 1 darf durch den Ausstieg nicht verändert werden")
                .isEqualTo(runde1VorAusstieg);

        // Runde 2: taucht das ausgestiegene Team in seiner Paarung auf, MUSS es sich um ein
        // automatisch eingetragenes Freispiel-Walkover handeln (Ergebnis bereits vorbelegt,
        // Gegner gewinnt kampflos) - niemals um eine echte, noch offene Partie.
        KaskadeKonfigurationSheet konfig = new KaskadeKonfigurationSheet(wkingSpreadsheet);
        int freispielPlus = konfig.getFreispielPunktePlus();
        int freispielMinus = konfig.getFreispielPunkteMinus();

        List<List<Integer>> runde2Paarungen = leseSpielpaarungWerte(runde2SheetName);
        boolean ausgestiegenesTeamGefunden = false;
        for (var row : runde2Paarungen) {
            int nrA = row.get(1); // TEAM_A_SPALTE relativ (Spalte 0 = SPIEL_NR)
            int nrB = row.get(2); // TEAM_B_SPALTE relativ
            int ergA = row.get(3);
            int ergB = row.get(4);
            if (nrA == AUSGESTIEGENES_TEAM_NR) {
                ausgestiegenesTeamGefunden = true;
                assertThat(ergA).as("Ausgestiegenes Team muss den Walkover als Team A verlieren")
                        .isEqualTo(freispielMinus);
                assertThat(ergB).as("Gegner muss den Walkover kampflos gewinnen").isEqualTo(freispielPlus);
            } else if (nrB == AUSGESTIEGENES_TEAM_NR) {
                ausgestiegenesTeamGefunden = true;
                assertThat(ergB).as("Ausgestiegenes Team muss den Walkover als Team B verlieren")
                        .isEqualTo(freispielMinus);
                assertThat(ergA).as("Gegner muss den Walkover kampflos gewinnen").isEqualTo(freispielPlus);
            }
        }
        assertThat(ausgestiegenesTeamGefunden)
                .as("Ausgestiegenes Team muss in Runde 2 als Walkover-Fall auftauchen (sonst prüft der Test nichts)")
                .isTrue();

        // Restliche Kaskadenrunde(n) + Abschluss (Gruppenrangliste + KO-Felder A–H) erzeugen:
        // darf mit dem dauerhaft verlierenden ausgestiegenen Team nicht crashen.
        testDaten.spielrundeSheet.naechsteRunde();
        testDaten.ergebnisseEinfuegen(3);
        testDaten.spielrundeSheet.naechsteRunde(); // Abschluss: Gruppenrangliste + KO-Felder A–H

        XSpreadsheet gruppenrangliste = sheetHlp.findByName(SheetNamen.kaskadeGruppenrangliste());
        assertThat(gruppenrangliste).as("Gruppenrangliste-Sheet muss nach Turnierabschluss vorhanden sein").isNotNull();

        XSpreadsheet koFeldA = sheetHlp.findByName(SheetNamen.kaskadenFeld("A"));
        assertThat(koFeldA).as("KO-Feld A muss nach Turnierabschluss vorhanden sein").isNotNull();
    }

    /**
     * Liest SPIEL_NR-/Team-A-/Team-B-/Ergebnis-Werte als {@code List<List<Integer>>} statt
     * {@code RangeData} – {@link de.petanqueturniermanager.helper.sheet.rangedata.CellData} hat kein
     * {@code equals()}, ein direkter {@code RangeData}-Vergleich wäre daher immer Objekt-Identität
     * statt Werte-Vergleich.
     */
    private List<List<Integer>> leseSpielpaarungWerte(String sheetName) throws GenerateException {
        XSpreadsheet sheet = sheetHlp.findByName(sheetName);
        assertThat(sheet).as(sheetName + " muss vorhanden sein").isNotNull();
        RangePosition paarungenRange = RangePosition.from(
                KaskadeSpielrundeSheet.SPIEL_NR_SPALTE, KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE,
                KaskadeSpielrundeSheet.ERG_TEAM_B_SPALTE, KaskadeSpielrundeSheet.ERSTE_DATEN_ZEILE + 200);
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
