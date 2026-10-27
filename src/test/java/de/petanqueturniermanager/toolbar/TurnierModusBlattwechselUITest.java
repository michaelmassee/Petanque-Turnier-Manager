package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XWindow2;
import com.sun.star.container.XNamed;
import com.sun.star.frame.XModel;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;

/** Native Calc-Regression: Turniermodus, Eingabezeilenabbau und anschließender Online-Sync-Blattwechsel. */
class TurnierModusBlattwechselUITest extends BaseCalcUITest {

    @AfterEach
    void turniermodusZuruecksetzen() {
        TurnierModus.get().wiederherstellenAlleElemente(wkingSpreadsheet);
        warteAufLoLeerlauf();
    }

    @Test
    void verstecktesDokumentUeberstehtTurniermodusUndSyncBlattwechsel() throws Exception {
        pruefeBlattwechselNachTurniermodus(false);
    }

    @Test
    void sichtbaresDokumentUeberstehtTurniermodusUndSyncBlattwechsel() throws Exception {
        pruefeBlattwechselNachTurniermodus(true);
    }

    private void pruefeBlattwechselNachTurniermodus(boolean sichtbar) throws Exception {
        if (sichtbar) {
            OfficeDocumentHelper.setVisible(doc, true);
            warteAufLoLeerlauf();
        }
        var frame = Lo.qi(XModel.class, doc).getCurrentController().getFrame();
        assertThat(Lo.qi(XWindow2.class, frame.getContainerWindow()).isVisible()).isEqualTo(sichtbar);
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TRIPLETTE,
                false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        warteAufLoLeerlauf();
        TurnierModus.get().wiederherstellenAlleElemente(wkingSpreadsheet);
        warteAufLoLeerlauf();
        TurnierModus.get().aktivieren(wkingSpreadsheet);
        warteAufLoLeerlauf();
        wieImSheetRunner(() -> new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null)
                .sicherstellen());
        assertThat(Lo.qi(XNamed.class, wkingSpreadsheet.getWorkingSpreadsheetView().getActiveSheet()).getName())
                .as("Sync-Blatt bleibt nach dem Turniermodus-Aufbau erreichbar").isEqualTo(SheetNamen.ptmOnlineSync());
    }
}
