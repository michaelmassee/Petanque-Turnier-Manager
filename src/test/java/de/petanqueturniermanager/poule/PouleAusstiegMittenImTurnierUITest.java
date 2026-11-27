package de.petanqueturniermanager.poule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.poule.ko.PouleKoSheet;
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
 * Meldeliste – ein nachträglicher Ausstieg ändert daher weder Vorrunde noch Ranglisten-Werte.
 * <p>
 * Nur die Zuordnung zum KO-Turnier (Spalte „Turnier“, KO-Baum) berücksichtigt den Ausstieg: Die Teams dahinter
 * rücken in ihrer Gruppe nach.
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

    /**
     * Steigt ein Gruppensieger vor der KO-Runde aus, rücken die Teams dahinter in seiner Gruppe nach: der Gruppendritte
     * spielt das A-Turnier. Rangliste (Spalte „Turnier“) und KO-Baum zeigen dasselbe.
     */
    @Test
    public void ausstiegGruppensieger_gruppendritterRuecktInsATurnierNach() throws GenerateException {
        testDaten.generate();
        int gruppensieger = teamAufPlatz(1, 1);
        int gruppendritter = teamAufPlatz(1, 3);

        PouleMeldeListeSheetUpdate meldeliste = new PouleMeldeListeSheetUpdate(wkingSpreadsheet);
        sheetHlp.setNumberValueInCell(NumberCellValue
                .from(meldeliste.getXSpreadSheet(),
                        Position.from(meldeliste.getAktivSpalte(), meldeliste.getSpielerZeileNr(gruppensieger)))
                .setValue(PouleMeldeListeSheetUpdate.AKTIV_WERT_AUSGESTIEGEN));

        new PouleVorrundenRanglisteSheetUpdate(wkingSpreadsheet).doRun();
        assertThat(turnierInRangliste(gruppensieger)).as("ausgestiegener Gruppensieger spielt kein KO").isEmpty();
        assertThat(turnierInRangliste(gruppendritter)).as("Gruppendritter rückt ins A-Turnier nach").isEqualTo("A");
        assertThat(teamAufPlatz(1, 1)).as("Vorrunden-Platz bleibt unverändert").isEqualTo(gruppensieger);

        new PouleKoSheet(wkingSpreadsheet).doRun();
        assertThat(teamsImKoBaum("A")).as("A-Turnier").contains(gruppendritter).doesNotContain(gruppensieger);
        assertThat(teamsImKoBaum("B")).as("B-Turnier").doesNotContain(gruppendritter, gruppensieger);
    }

    private int teamAufPlatz(int gruppe, int platz) throws GenerateException {
        return leseRanglisteWerte().stream()
                .filter(row -> row.get(PouleVorrundenRanglisteSheet.SPALTE_GRUPPE) == gruppe
                        && row.get(PouleVorrundenRanglisteSheet.SPALTE_PLATZ) == platz)
                .mapToInt(row -> row.get(PouleVorrundenRanglisteSheet.SPALTE_NR)).findFirst().orElseThrow();
    }

    private String turnierInRangliste(int teamNr) throws GenerateException {
        XSpreadsheet sheet = sheetHlp.findByName(SheetNamen.pouleVorrundenRangliste());
        RangePosition range = RangePosition.from(PouleVorrundenRanglisteSheet.SPALTE_PLATZ,
                PouleVorrundenRanglisteSheet.HEADER_ZEILEN, PouleVorrundenRanglisteSheet.SPALTE_TURNIER,
                PouleVorrundenRanglisteSheet.HEADER_ZEILEN + 100);
        return RangeHelper.from(sheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), range).getDataFromRange()
                .stream()
                .filter(row -> row.size() > PouleVorrundenRanglisteSheet.SPALTE_TURNIER
                        && row.get(PouleVorrundenRanglisteSheet.SPALTE_NR).getIntVal(0) == teamNr)
                .map(row -> row.get(PouleVorrundenRanglisteSheet.SPALTE_TURNIER).getStringVal())
                .map(wert -> wert == null ? "" : wert).findFirst().orElseThrow();
    }

    /** Teamnummern aller Teams, die im KO-Baum über ihre UUID-Formel referenziert werden. */
    private Set<Integer> teamsImKoBaum(String gruppe) {
        XSpreadsheet baum = sheetHlp.findByName(SheetNamen.koFinaleGruppe(gruppe));
        assertThat(baum).as("KO-Baum %s", gruppe).isNotNull();
        var uuidIndex = MeldelisteUuidIndex.lese(wkingSpreadsheet);
        String[][] formeln = sheetHlp.getFormulaArrayFromRange(baum, RangePosition.from(0, 0, 40, 200));
        return Arrays.stream(formeln).flatMap(Arrays::stream).map(TeamUuidFormel::findeUuid)
                .flatMap(Optional::stream).map(uuidIndex::nr).filter(OptionalInt::isPresent)
                .map(OptionalInt::getAsInt).collect(Collectors.toSet());
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
