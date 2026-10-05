package de.petanqueturniermanager.schweizer.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.ValidationAlertStyle;
import com.sun.star.sheet.ValidationType;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.util.CellProtection;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.schweizer.blattschutz.SchweizerBlattschutzKonfiguration;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.toolbar.TurnierModus;

/**
 * Eine leere Bahnspalte ist eine manuelle Eingabe und muss daher sowohl farblich
 * hervorgehoben als auch im Turniermodus entsperrt sein.
 */
class SchweizerLeereBahnSpalteUITest extends BaseCalcUITest {

    @Test
    void leereBahnspalteIstHervorgehobenUndImTurniermodusEditierbar() throws Exception {
        new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet).doRun();
        var spielrunde = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
        spielrunde.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.L);
        spielrunde.doRun();

        XSpreadsheet sheet = spielrunde.getXSpreadSheet();
        Position bahn = Position.from(SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE,
                SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE);
        assertThat(istLeereZelle(sheet, bahn)).as("Bahnspalte wird bei Option L nicht vorbefuellt").isTrue();
        assertThat(hatEditierbareFelderFormatierung(sheet, bahn))
                .as("leere Bahnspalte muss die Editierfarben-Formatierung erhalten")
                .isTrue();
        XPropertySet validation = Lo.qi(XPropertySet.class, Lo.qi(XPropertySet.class,
                sheet.getCellByPosition(bahn.getSpalte(), bahn.getZeile())).getPropertyValue("Validation"));
        assertThat(validation.getPropertyValue("Type")).isEqualTo(ValidationType.CUSTOM);
        assertThat(validation.getPropertyValue("ErrorAlertStyle"))
                .as("Bahnspalte warnt nur, statt Eingaben abzulehnen").isEqualTo(ValidationAlertStyle.WARNING);
        assertThat(Lo.qi(XSheetCondition.class, validation).getFormula1())
                .as("Bahnbezeichnungen als Text erlaubt, keine Eindeutigkeitsprüfung")
                .contains("ISTEXT").doesNotContain("COUNTIF");

        TurnierModus.get().setAktivForTest(true);
        try {
            BlattschutzManager.get().schuetzen(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
            assertThat(Lo.qi(XProtectable.class, sheet).isProtected()).isTrue();
            assertThat(istGesperrt(sheet, bahn)).as("leere Bahnspalte bleibt im Turniermodus editierbar").isFalse();
        } finally {
            BlattschutzManager.get().entsperren(SchweizerBlattschutzKonfiguration.get(), wkingSpreadsheet);
            TurnierModus.get().setAktivForTest(false);
        }
    }

    private boolean istLeereZelle(XSpreadsheet sheet, Position position) {
        return sheetHlp.getTextFromCell(sheet, position).isBlank();
    }

    private boolean istGesperrt(XSpreadsheet sheet, Position position) throws Exception {
        XPropertySet props = Lo.qi(XPropertySet.class,
                sheet.getCellByPosition(position.getSpalte(), position.getZeile()));
        return ((CellProtection) props.getPropertyValue("CellProtection")).IsLocked;
    }

    private boolean hatEditierbareFelderFormatierung(XSpreadsheet sheet, Position position) throws Exception {
        XPropertySet props = Lo.qi(XPropertySet.class,
                sheet.getCellByPosition(position.getSpalte(), position.getZeile()));
        XSheetConditionalEntries entries = Lo.qi(XSheetConditionalEntries.class,
                props.getPropertyValue("ConditionalFormat"));
        for (int i = 0; i < entries.getCount(); i++) {
            XSheetCondition condition = Lo.qi(XSheetCondition.class, entries.getByIndex(i));
            if (condition.getFormula1().contains(EditierbaresZelleFormatHelper.PROPERTY_KEY)) {
                return true;
            }
        }
        return false;
    }
}
