/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;

/**
 * „QR-Code Live-Ansicht öffnen“ deutet die Cursor-Zeile nur auf dem Blatt der Meldungen. Regression: Der Vergleich
 * über Objektidentität schlug immer fehl, weil Calc bei {@code getActiveSheet()} jedes Mal ein neues Blatt-Objekt
 * liefert – der Menüpunkt meldete dann trotz markierter Meldungszeile „Bitte eine Zeile markieren“.
 */
class MeldelisteZielBlattUITest extends BaseCalcUITest {

    private MeldelisteZiel ziel;

    @BeforeEach
    void meldelisteAnlegen() throws Exception {
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TETE, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
    }

    @Test
    void aktivesBlattIstDieMeldelisteObwohlCalcJedesMalEinNeuesObjektLiefert() {
        sheetHlp.setActiveSheet(sheetHlp.findByName(SheetNamen.meldeliste()));

        XSpreadsheet aktiv = wkingSpreadsheet.getWorkingSpreadsheetView().getActiveSheet();

        assertThat(ziel.liegtAufBlatt(aktiv)).isTrue();
        assertThat(SheetHelper.istGleichesBlatt(aktiv, wkingSpreadsheet.getWorkingSpreadsheetView().getActiveSheet()))
                .isTrue();
    }

    @Test
    void anderesBlattIstNichtDieMeldeliste() {
        XSpreadsheet anderes = sheetHlp.newIfNotExist("Anderes Blatt", 0);
        sheetHlp.setActiveSheet(anderes);

        assertThat(ziel.liegtAufBlatt(wkingSpreadsheet.getWorkingSpreadsheetView().getActiveSheet())).isFalse();
    }
}
