package de.petanqueturniermanager.triptete;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheetTestDaten;

/**
 * UITest für ein vollständiges Trip-Tête-Beispielturnier mit 6 Teams.
 * <p>
 * Prüft Meldeliste, Spielplan und Rangliste gegen JSON-Referenzdateien.
 * Reproduzierbarkeit über {@link RandomSource#setSeed(long)}.
 */
public class TripTeteTurnierTestDatenUITest extends BaseCalcUITest {

    private static final long SEED_FUER_TESTS = 42L;
    private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;
    private static final int ANZ_TEAMS = 6;

    @BeforeEach
    @Override
    public void beforeTest() {
        super.beforeTest();
        RandomSource.setSeed(SEED_FUER_TESTS);
    }

    @AfterEach
    public void resetRandom() {
        RandomSource.reset();
    }

    @Test
    public void testTripTeteTurnier6Teams() throws GenerateException {
        new TripTeteSpielPlanSheetTestDaten(wkingSpreadsheet).generate();

        validiereGrundstruktur();
        validiereMeldelistePerJson("triptete-meldeliste.json");
        validiereSpielplanPerJson("triptete-spielplan.json");
        validiereRanglistePerJson("triptete-rangliste.json");
    }

    @Test
    public void jedesBlattTraegtKorrektenSchluessel() throws GenerateException {
        new TripTeteSpielPlanSheetTestDaten(wkingSpreadsheet).generate();

        Map<String, String> erwartung = new LinkedHashMap<>();
        erwartung.put(SheetNamen.meldeliste(), SheetMetadataHelper.SCHLUESSEL_TRIPTETE_MELDELISTE);
        erwartung.put(SheetNamen.spielplan(), SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN);
        erwartung.put(SheetNamen.rangliste(), SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE);
        pruefeJedesBlattTraegtKorrektenSchluessel(erwartung);
    }

    private void validiereGrundstruktur() {
        assertThat(sheetHlp.findByName(SheetNamen.meldeliste()))
                .as("Meldeliste-Sheet muss existieren").isNotNull();
        assertThat(sheetHlp.findByName(SheetNamen.spielplan()))
                .as("Spielplan-Sheet muss existieren").isNotNull();
        assertThat(sheetHlp.findByName(SheetNamen.rangliste()))
                .as("Rangliste-Sheet muss existieren").isNotNull();
    }

    private void validiereMeldelistePerJson(String referenzDatei) throws GenerateException {
        XSpreadsheet meldeliste = sheetHlp.findByName(SheetNamen.meldeliste());
        RangePosition bereich = RangePosition.from(
                0, MELDELISTE_ERSTE_DATEN_ZEILE,
                2, MELDELISTE_ERSTE_DATEN_ZEILE + ANZ_TEAMS - 1);

        // writeToJson(referenzDatei, bereich, meldeliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

        RangeData rangeData = rangeDateFromRangePosition(bereich, meldeliste,
                wkingSpreadsheet.getWorkingSpreadsheetDocument());
        InputStream jsonFile = TripTeteTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
        validateWithJson(rangeData, jsonFile);
    }

    private void validiereSpielplanPerJson(String referenzDatei) throws GenerateException {
        XSpreadsheet spielplan = sheetHlp.findByName(SheetNamen.spielplan());
        RangePosition bereich = RangePosition.from(0, 0, 20, 50);

        // writeToJson(referenzDatei, bereich, spielplan, wkingSpreadsheet.getWorkingSpreadsheetDocument());

        RangeData rangeData = rangeDateFromRangePosition(bereich, spielplan,
                wkingSpreadsheet.getWorkingSpreadsheetDocument());
        InputStream jsonFile = TripTeteTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
        validateWithJson(rangeData, jsonFile);
    }

    private void validiereRanglistePerJson(String referenzDatei) throws GenerateException {
        XSpreadsheet rangliste = sheetHlp.findByName(SheetNamen.rangliste());
        RangePosition bereich = RangePosition.from(
                TripTeteRanglisteSheet.TEAM_NR_SPALTE,
                TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE,
                TripTeteRanglisteSheet.BEGEGNUNGEN_SPALTE,
                TripTeteRanglisteSheet.ERSTE_DATEN_ZEILE + ANZ_TEAMS + 1);

        // writeToJson(referenzDatei, bereich, rangliste, wkingSpreadsheet.getWorkingSpreadsheetDocument());

        RangeData rangeData = rangeDateFromRangePosition(bereich, rangliste,
                wkingSpreadsheet.getWorkingSpreadsheetDocument());
        InputStream jsonFile = TripTeteTurnierTestDatenUITest.class.getResourceAsStream(referenzDatei);
        validateWithJson(rangeData, jsonFile);
    }
}
