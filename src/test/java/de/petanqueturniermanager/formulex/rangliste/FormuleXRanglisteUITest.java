/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXTurnierTestDaten;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * UI-Test für die Siege-Spalte der Formule X Rangliste.
 * <p>
 * Prüft nach vollständiger Turniergenerierung (39 Teams, 5 Runden), dass:
 * <ul>
 *   <li>jede Team-Siege-Zahl im gültigen Bereich [0, ANZ_RUNDEN] liegt</li>
 *   <li>die Summe aller Siege dem erwarteten Gesamtwert entspricht:
 *       je Runde: (ANZ_TEAMS-1)/2 reguläre Spiele + 1 Freilos = 20 Siege → 5 × 20 = 100</li>
 * </ul>
 */
@Tag("beispielturnier")
class FormuleXRanglisteUITest extends BaseCalcUITest {

    @Test
    void siegeSpalteZeigtKorrekteAnzahlProTeamUndKorrekteGesamtsumme() throws GenerateException {
        new FormuleXTurnierTestDaten(wkingSpreadsheet).generate();

        XSpreadsheet ranglisteSheet = SheetMetadataHelper.findeSheetUndHeile(
                wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, null);
        assertThat(ranglisteSheet).as("Rangliste-Sheet muss vorhanden sein").isNotNull();

        var xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
        RangePosition leseBereich = RangePosition.from(
                FormuleXRanglisteSheet.TEAM_NR_SPALTE,
                FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE,
                FormuleXRanglisteSheet.SIEGE_SPALTE,
                FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE + FormuleXTurnierTestDaten.ANZ_TEAMS - 1);

        RangeData daten = RangeHelper.from(ranglisteSheet, xDoc, leseBereich).getDataFromRange();

        assertThat(daten).as("Rangliste muss Datenzeilen enthalten").isNotEmpty();

        SoftAssertions soft = new SoftAssertions();
        int gesamtSiege = 0;
        int zeile = FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE;

        for (RowData row : daten) {
            int teamNr = row.get(FormuleXRanglisteSheet.TEAM_NR_SPALTE).getIntVal(0);
            if (teamNr <= 0) {
                break;
            }
            int siege = row.get(FormuleXRanglisteSheet.SIEGE_SPALTE).getIntVal(0);
            soft.assertThat(siege)
                    .as("Team %d (Zeile %d): Siege muss in [0, %d] liegen",
                            teamNr, zeile, FormuleXTurnierTestDaten.ANZ_RUNDEN)
                    .isBetween(0, FormuleXTurnierTestDaten.ANZ_RUNDEN);
            gesamtSiege += siege;
            zeile++;
        }
        soft.assertAll();

        // Mit 39 Teams (ungerade): 1 Freilos + 19 reguläre Spiele = 20 Siege je Runde
        // 5 Runden × 20 = 100 Siege gesamt
        int anzTeams = FormuleXTurnierTestDaten.ANZ_TEAMS;
        int anzRunden = FormuleXTurnierTestDaten.ANZ_RUNDEN;
        int regulaereSpieleProRunde = (anzTeams - 1) / 2; // 19
        int freilosProRunde = anzTeams % 2;               // 1
        int erwarteteSiege = (regulaereSpieleProRunde + freilosProRunde) * anzRunden;

        assertThat(gesamtSiege)
                .as("Gesamtanzahl Siege muss %d betragen (%d reguläre Spiele + %d Freilos × %d Runden)",
                        erwarteteSiege, regulaereSpieleProRunde, freilosProRunde, anzRunden)
                .isEqualTo(erwarteteSiege);
    }

    /**
     * Regression: {@code insertHeader()} rief zuerst {@code setCellBackColor(headerColor)} und
     * danach {@code setCellProperties(headerCellProps)} auf derselben {@link StringCellValue} auf.
     * {@code setCellProperties()} ersetzt das interne Property-Objekt komplett (keine Merge-Semantik),
     * wodurch die zuvor gesetzte Hintergrundfarbe verworfen wurde und der Header ungefärbt blieb.
     */
    @Test
    void headerZeigtKonfigurierteHintergrundfarbe() throws GenerateException, com.sun.star.lang.IndexOutOfBoundsException {
        new FormuleXTurnierTestDaten(wkingSpreadsheet).generate();

        var konfig = new FormuleXKonfigurationSheet(wkingSpreadsheet);
        Integer konfigFarbe = konfig.getRanglisteHeaderFarbe();

        XSpreadsheet ranglisteSheet = SheetMetadataHelper.findeSheetUndHeile(
                wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, null);
        assertThat(ranglisteSheet).isNotNull();

        // Zeile 1 (HEADER_ZEILE): Einzel-Spalten (vertikal gemergt) + Anker der horizontal
        // gemergten "Punkte"-Gruppe. Die von der Gruppe überdeckten Spalten (Minus/Diff) tragen
        // in Zeile 1 bewusst keine eigene Farbe (LO rendert den gesamten Merge-Bereich über die
        // Anker-Zelle) und werden hier nicht geprüft.
        int[] zeile1AnkerSpalten = {
                FormuleXRanglisteSheet.TEAM_NR_SPALTE, FormuleXRanglisteSheet.TEAM_NAME_SPALTE,
                FormuleXRanglisteSheet.PLATZ_SPALTE, FormuleXRanglisteSheet.SIEGE_SPALTE,
                FormuleXRanglisteSheet.WERTUNG_SPALTE, FormuleXRanglisteSheet.PUNKTE_PLUS_SPALTE,
        };
        for (int spalte : zeile1AnkerSpalten) {
            pruefeCellBackColor(ranglisteSheet, spalte, FormuleXRanglisteSheet.HEADER_ZEILE, konfigFarbe);
        }

        // Zeile 2 (ZWEITE_HEADER_ZEILE): die drei Punkte-Sub-Header (+/-/Δ) sind einzeln gefärbt.
        int[] zeile2Spalten = {
                FormuleXRanglisteSheet.PUNKTE_PLUS_SPALTE, FormuleXRanglisteSheet.PUNKTE_MINUS_SPALTE,
                FormuleXRanglisteSheet.PUNKTE_DIFF_SPALTE,
        };
        for (int spalte : zeile2Spalten) {
            pruefeCellBackColor(ranglisteSheet, spalte, FormuleXRanglisteSheet.ZWEITE_HEADER_ZEILE, konfigFarbe);
        }
    }

    private void pruefeCellBackColor(XSpreadsheet sheet, int spalte, int zeile, Integer erwarteteFarbe)
            throws com.sun.star.lang.IndexOutOfBoundsException {
        XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class,
                sheet.getCellByPosition(spalte, zeile));
        Object cellBackColor;
        try {
            cellBackColor = props.getPropertyValue("CellBackColor");
        } catch (Exception e) {
            throw new AssertionError("CellBackColor konnte nicht gelesen werden", e);
        }
        assertThat(cellBackColor)
                .as("Header-Hintergrundfarbe in Spalte %d, Zeile %d muss der Konfiguration entsprechen", spalte, zeile)
                .isEqualTo(erwarteteFarbe);
    }

    /**
     * Regression: {@code headerCellProps} wird von Nr/Team/Siege/Wertung geteilt (setCellProperties()
     * weist nur die Referenz zu). Ein direktes {@code setRotate90()} auf diesem gemeinsamen Objekt
     * für die Platz-Spalte hätte sich in-place in alle danach verarbeiteten Spalten (Siege, Wertung)
     * "eingebrannt". Nur Platz darf hochkant stehen.
     */
    @Test
    void nurPlatzSpalteIstHochkantGedreht() throws GenerateException, com.sun.star.lang.IndexOutOfBoundsException {
        new FormuleXTurnierTestDaten(wkingSpreadsheet).generate();

        XSpreadsheet ranglisteSheet = SheetMetadataHelper.findeSheetUndHeile(
                wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, null);
        assertThat(ranglisteSheet).isNotNull();

        pruefeRotateAngle(ranglisteSheet, FormuleXRanglisteSheet.TEAM_NR_SPALTE, 0);
        pruefeRotateAngle(ranglisteSheet, FormuleXRanglisteSheet.TEAM_NAME_SPALTE, 0);
        pruefeRotateAngle(ranglisteSheet, FormuleXRanglisteSheet.PLATZ_SPALTE, 27000);
        pruefeRotateAngle(ranglisteSheet, FormuleXRanglisteSheet.SIEGE_SPALTE, 0);
        pruefeRotateAngle(ranglisteSheet, FormuleXRanglisteSheet.WERTUNG_SPALTE, 0);
    }

    private void pruefeRotateAngle(XSpreadsheet sheet, int spalte, int erwarteterWinkel)
            throws com.sun.star.lang.IndexOutOfBoundsException {
        XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class,
                sheet.getCellByPosition(spalte, FormuleXRanglisteSheet.HEADER_ZEILE));
        Object rotateAngle;
        try {
            rotateAngle = props.getPropertyValue("RotateAngle");
        } catch (Exception e) {
            throw new AssertionError("RotateAngle konnte nicht gelesen werden", e);
        }
        assertThat(rotateAngle)
                .as("RotateAngle in Spalte %d muss %d sein", spalte, erwarteterWinkel)
                .isEqualTo(erwarteterWinkel);
    }

    @Test
    void jederTeamHatEinenRanglistenEintrag() throws GenerateException {
        new FormuleXTurnierTestDaten(wkingSpreadsheet).generate();

        XSpreadsheet ranglisteSheet = SheetMetadataHelper.findeSheetUndHeile(
                wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, null);
        assertThat(ranglisteSheet).as("Rangliste-Sheet muss vorhanden sein").isNotNull();

        var xDoc = wkingSpreadsheet.getWorkingSpreadsheetDocument();
        RangeData daten = RangeHelper.from(ranglisteSheet, xDoc,
                RangePosition.from(
                        Position.from(FormuleXRanglisteSheet.TEAM_NR_SPALTE,
                                FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE),
                        Position.from(FormuleXRanglisteSheet.TEAM_NR_SPALTE,
                                FormuleXRanglisteSheet.ERSTE_DATEN_ZEILE
                                        + FormuleXTurnierTestDaten.ANZ_TEAMS - 1)))
                .getDataFromRange();

        long anzahlTeams = daten.stream()
                .map(row -> row.get(0).getIntVal(0))
                .filter(nr -> nr > 0)
                .count();

        assertThat(anzahlTeams)
                .as("Rangliste muss alle %d Teams enthalten", FormuleXTurnierTestDaten.ANZ_TEAMS)
                .isEqualTo(FormuleXTurnierTestDaten.ANZ_TEAMS);
    }

    /**
     * Regression im Kiosk-Modus: nach Vollaufbau muss ein erneutes
     * {@link FormuleXRanglisteSheetUpdate#doRun()} unter aktivem TurnierModus +
     * FormuleX-Blattschutz die Rangliste reaktiv aktualisieren.
     */
    @Test
    void kioskModus_ranglisteUpdateUnterSchutz() throws GenerateException {
        new FormuleXTurnierTestDaten(wkingSpreadsheet).generate();
        mitKioskModus(TurnierSystem.FORMULEX, () ->
                new FormuleXRanglisteSheetUpdate(wkingSpreadsheet).doRun());

        XSpreadsheet ranglisteSheet = SheetMetadataHelper.findeSheetUndHeile(
                wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE, null);
        assertThat(ranglisteSheet)
                .as("FormuleX-Rangliste muss nach Kiosk-Update weiterhin existieren")
                .isNotNull();
    }
}
