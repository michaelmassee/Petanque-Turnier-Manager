package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TestSuperMeleeMeldeListeErstellen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Grenzwert-Tests für die Spieleranzahl bei Supermelee-Spielrunden.
 * Ergänzt SpielrundeUITest um Edge Cases: Minimum (6), ungültig (7), NurDoublette-Aufteilung (8).
 */
public class SpielrundeGrenzwertUITest extends BaseCalcUITest {

    private TestSuperMeleeMeldeListeErstellen testMeldeListeErstellen;

    @BeforeEach
    public void setUp() {
        testMeldeListeErstellen = new TestSuperMeleeMeldeListeErstellen(wkingSpreadsheet, doc);
    }

    /**
     * 6 Spieler sind die kleinstmögliche gültige Anzahl (2 Triplettes = 1 Bahn).
     * Prüft, dass das Sheet erstellt wird und genau 1 Zeile mit 6 befüllten Spielernummern enthält.
     */
    @Test
    public void testSpielrundeMitSechsSpielernWirdErstellt() throws GenerateException {
        testMeldeListeErstellen.initMitAlleDieSpielen(6);
        new SpielrundeSheet_Naechste(wkingSpreadsheet).run();

        String sheetName = SpielrundeSheetKonstanten.sheetName(1, 1);
        XSpreadsheet spielrundeSheet = sheetHlp.findByName(sheetName);
        assertThat(spielrundeSheet).as("Sheet '%s' muss für 6 Spieler erstellt werden", sheetName).isNotNull();

        // 2 Triplettes = 1 Bahn = 1 Datenzeile
        RangePosition spielerNrRange = RangePosition.from(
                SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
                SpielrundeSheetKonstanten.LETZTE_SPALTE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE);
        RangeData data = RangeHelper.from(spielrundeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), spielerNrRange)
                .getDataFromRange();

        assertThat(data).as("1 Bahn erwartet (2 Triplettes)").hasSize(1);
        // Alle 6 Spalten (3 Team A + 3 Team B) müssen befüllt sein
        data.get(0).forEach(cell ->
                assertThat(cell.getStringVal()).as("Alle 6 Spielernummern bei 2 Triplettes müssen befüllt sein").isNotBlank());
    }

    /**
     * 7 ist die einzige ungültige Spieleranzahl (valideAnzahlSpieler() = false).
     * Prüft, dass kein Sheet erstellt wird und die Konfiguration unverändert bleibt.
     */
    @Test
    public void testSpielrundeMitSiebenSpielernWirdAbgelehnt() throws GenerateException {
        testMeldeListeErstellen.initMitAlleDieSpielen(7);
        new SpielrundeSheet_Naechste(wkingSpreadsheet).run();

        String sheetName = SpielrundeSheetKonstanten.sheetName(1, 1);
        assertThat(sheetHlp.findByName(sheetName))
                .as("Sheet '%s' darf bei 7 Spielern NICHT erstellt werden", sheetName)
                .isNull();
        assertThat(docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 999))
                .as("Spieltag-Konfig darf sich nicht ändern")
                .isEqualTo(1);
    }

    /**
     * 8 Spieler im Triplette-Modus ergibt 4 Doublettes (kein Triplette möglich, da 8%6=2 → 4D, 0T).
     * Prüft, dass das Sheet 2 Bahnen enthält und jede Zeile exakt 4 Spielernummern (Doublette-Muster) hat.
     */
    @Test
    public void testSpielrundeMitAchtSpielernNurDoubletten() throws GenerateException {
        testMeldeListeErstellen.initMitAlleDieSpielen(8);
        new SpielrundeSheet_Naechste(wkingSpreadsheet).run();

        String sheetName = SpielrundeSheetKonstanten.sheetName(1, 1);
        XSpreadsheet spielrundeSheet = sheetHlp.findByName(sheetName);
        assertThat(spielrundeSheet).as("Sheet '%s' muss für 8 Spieler erstellt werden", sheetName).isNotNull();

        // 4 Doublettes = 2 Bahnen = 2 Datenzeilen
        RangePosition spielerNrRange = RangePosition.from(
                SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
                SpielrundeSheetKonstanten.LETZTE_SPALTE, SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE + 1);
        RangeData data = RangeHelper.from(spielrundeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), spielerNrRange)
                .getDataFromRange();

        assertThat(data).as("2 Bahnen erwartet (4 Doublettes im Triplette-Modus)").hasSize(2);
        for (int i = 0; i < data.size(); i++) {
            var zeile = data.get(i);
            // Doublette-Muster: 2 Spieler Team A, leer, 2 Spieler Team B, leer
            assertThat(zeile.get(0).getStringVal()).as("Zeile %d: Team A Spieler 1", i).isNotBlank();
            assertThat(zeile.get(1).getStringVal()).as("Zeile %d: Team A Spieler 2", i).isNotBlank();
            assertThat(zeile.get(2).getStringVal()).as("Zeile %d: Team A Spieler 3 muss leer sein (Doublette)", i).isBlank();
            assertThat(zeile.get(3).getStringVal()).as("Zeile %d: Team B Spieler 1", i).isNotBlank();
            assertThat(zeile.get(4).getStringVal()).as("Zeile %d: Team B Spieler 2", i).isNotBlank();
            assertThat(zeile.get(5).getStringVal()).as("Zeile %d: Team B Spieler 3 muss leer sein (Doublette)", i).isBlank();
        }
    }
}
