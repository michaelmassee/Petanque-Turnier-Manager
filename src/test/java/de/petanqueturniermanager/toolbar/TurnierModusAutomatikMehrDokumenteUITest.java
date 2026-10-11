package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XTopWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;

/**
 * Die automatische Aktivierung des Turniermodus nach einer Aktion darf nur das Dokument der
 * Aktion betreffen – auch wenn gerade ein anderes Turnierdokument den Fokus hält.
 */
class TurnierModusAutomatikMehrDokumenteUITest extends BaseCalcUITest {

    private XSpreadsheetDocument zweitesDokument;
    private WorkingSpreadsheet zweitesWs;

    @AfterEach
    void raeumeAuf() {
        var turnierModus = TurnierModus.get();
        turnierModus.wiederherstellenAlleElemente(wkingSpreadsheet);
        warteAufLoLeerlauf();
        if (zweitesDokument != null) {
            turnierModus.wiederherstellenAlleElemente(zweitesWs);
            warteAufLoLeerlauf();
            OfficeDocumentHelper.closeDoc(zweitesDokument);
            zweitesDokument = null;
        }
    }

    @Test
    void aktiviertNurDasDokumentDerAktion() {
        oeffneZweitesTurnierdokument();
        setzeTurnierSystem(wkingSpreadsheet);
        fokussiere(zweitesDokument);
        warteAufLoLeerlauf();

        boolean aktiviert = TurnierModus.get().aktiviereAutomatischFallsNoetig(wkingSpreadsheet, true);
        warteAufLoLeerlauf();

        assertThat(aktiviert).isTrue();
        assertThat(TurnierModus.get().istAktiv(wkingSpreadsheet)).as("Dokument der Aktion").isTrue();
        assertThat(TurnierModus.get().istAktiv(zweitesWs)).as("fokussiertes anderes Dokument").isFalse();
        assertThat(new DocumentPropertiesHelper(wkingSpreadsheet).getTurnierModusAusDocument())
                .as("Zustand im Dokument gemerkt").isTrue();
    }

    @Test
    void ohneOptionBleibtTurniermodusAus() {
        setzeTurnierSystem(wkingSpreadsheet);

        boolean aktiviert = TurnierModus.get().aktiviereAutomatischFallsNoetig(wkingSpreadsheet, false);
        warteAufLoLeerlauf();

        assertThat(aktiviert).isFalse();
        assertThat(TurnierModus.get().istAktiv(wkingSpreadsheet)).isFalse();
    }

    private void oeffneZweitesTurnierdokument() {
        zweitesDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
        assertThat(zweitesDokument).as("zweites Dokument konnte nicht erstellt werden").isNotNull();
        zweitesWs = new WorkingSpreadsheet(starter.getxComponentContext(), zweitesDokument);
        setzeTurnierSystem(zweitesWs);
        warteAufLoLeerlauf();
    }

    private static void setzeTurnierSystem(WorkingSpreadsheet ws) {
        new DocumentPropertiesHelper(ws).setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
    }

    private static void fokussiere(XSpreadsheetDocument dokument) {
        XFrame frame = Lo.qi(XModel.class, dokument).getCurrentController().getFrame();
        frame.activate();
        XTopWindow topWindow = Lo.qi(XTopWindow.class, frame.getContainerWindow());
        if (topWindow != null) {
            topWindow.toFront();
        }
    }
}
