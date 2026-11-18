/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

/**
 * Regression: Neue Ergebnismeldungen schreiben die Konfliktliste im Hintergrund. Das Fixieren der Kopfzeilen
 * aktivierte dabei das Konfliktblatt, sodass der Turnierleitung mitten in der Ergebniseingabe der Fokus wegsprang.
 */
class PtmOnlineKonfliktSheetFokusUITest extends BaseCalcUITest {

    private static final String PRAEFIX = "ERGEBNIS_GEMELDET|";

    @Test
    void schreibenImHintergrundWechseltNichtDasAktiveBlatt() throws Exception {
        PtmOnlineKonfliktSheet konfliktListe = new PtmOnlineKonfliktSheet(wkingSpreadsheet, TurnierSystem.SCHWEIZER);
        XSpreadsheet spielrunde = sheetHlp.newIfNotExist("Spielrunde", 0);

        wkingSpreadsheet.getWorkingSpreadsheetView().setActiveSheet(spielrunde);
        konfliktListe.ersetzeBlock(PRAEFIX, List.of(zeile("m1")), "Test");
        assertThat(SheetHelper.istGleichesBlatt(aktivesBlatt(), spielrunde)).as("Blatt neu angelegt").isTrue();

        konfliktListe.ersetzeBlock(PRAEFIX, List.of(zeile("m1"), zeile("m2")), "Test");
        assertThat(SheetHelper.istGleichesBlatt(aktivesBlatt(), spielrunde)).as("Block ersetzt").isTrue();
        assertThat(konfliktListe.leseBlock(PRAEFIX)).hasSize(2);
    }

    private XSpreadsheet aktivesBlatt() {
        return wkingSpreadsheet.getWorkingSpreadsheetView().getActiveSheet();
    }

    private static PtmOnlineKonfliktSheet.Zeile zeile(String id) {
        return new PtmOnlineKonfliktSheet.Zeile("Ergebnis gemeldet", "Runde 1: 3 – 4", "13:7", "", PRAEFIX + id,
                List.of("Ergebnis übernehmen", "Meldung verwerfen"), "");
    }
}
