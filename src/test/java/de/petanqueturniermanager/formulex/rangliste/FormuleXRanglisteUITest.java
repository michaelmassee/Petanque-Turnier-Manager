/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
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
}
