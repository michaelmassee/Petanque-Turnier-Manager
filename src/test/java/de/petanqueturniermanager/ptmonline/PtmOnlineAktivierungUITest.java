package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;
import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.PtmOnlineMenuController;
import de.petanqueturniermanager.toolbar.PtmOnlineToolbarSteuerung;

class PtmOnlineAktivierungUITest extends BaseCalcUITest {
    private static final String PTMONLINE_TOOLBAR_URL =
            "private:resource/toolbar/addon_de.petanqueturniermanager.toolbar.ptmonline";

    /** Toolbar-Sichtbarkeit meldet LO nur für sichtbare Fenster; das Testdokument ist versteckt. */
    private XSpreadsheetDocument sichtbaresDokument;

    @AfterEach
    void resetOnline() {
        if (sichtbaresDokument != null) {
            OfficeDocumentHelper.closeDoc(sichtbaresDokument);
            sichtbaresDokument = null;
        }
        PtmOnlineLiveBeobachter.zuruecksetzenFuerTest();
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
    }

    @Test
    void keyUndOptionSchaltenMenueUndBeobachterOhneNeustart() throws Exception {
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TETE, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.SCHWEIZER.getId());
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = "t1";
        turnier.name = "Testturnier";
        new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null)
                .verbinden(turnier, new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                        "01234567890123456789012345678901");
        sichtbaresDokument = OfficeDocumentHelper.from(loader).createSichtbaresCalc();
        var sichtbaresFenster = Lo.qi(XModel.class, sichtbaresDokument).getCurrentController().getFrame();
        var context = starter.getxComponentContext();
        var frame = Lo.qi(com.sun.star.frame.XModel.class, doc).getCurrentController().getFrame();
        var popup = Lo.qi(com.sun.star.awt.XPopupMenu.class, context.getServiceManager()
                .createInstanceWithContext("com.sun.star.awt.PopupMenu", context));
        var menuController = new PtmOnlineMenuController(context);
        var frameArg = new com.sun.star.beans.PropertyValue();
        frameArg.Name = "Frame";
        frameArg.Value = frame;
        menuController.initialize(new Object[] {frameArg});
        menuController.setPopupMenu(popup);
        Object service = context.getServiceManager().createInstanceWithArgumentsAndContext(
                "de.petanqueturniermanager.PtmOnlineMenuController", new Object[] {frameArg}, context);
        assertThat(Lo.qi(com.sun.star.frame.XPopupMenuController.class, service)).as("Controller-Service registriert").isNotNull();
        for (int i = 0; i < 2; i++) {
            schalte(menuController, new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test", "http://127.0.0.1", true));
            assertThat(popup.getItemCount()).as("Alle neun Online-Befehle wiederhergestellt").isEqualTo((short) 9);
            assertThat(toolbarSichtbar(sichtbaresFenster)).as("PTM-Online-Toolbar mit Freigabe sichtbar").isTrue();
            var instanz = PtmOnlineLiveBeobachter.class.getDeclaredField("instanz");
            instanz.setAccessible(true);
            Object beobachter = instanz.get(null);
            assertThat(beobachter).isNotNull();
            var feld = PtmOnlineLiveBeobachter.class.getDeclaredField("beobachtungen");
            feld.setAccessible(true);
            assertThat((Map<?, ?>) feld.get(beobachter)).as("Bereits offenes Dokument wird übernommen").hasSize(1);

            schalte(menuController, new LibreOfficePtmOnlineSpeicher.Zugangsdaten(i == 0 ? "" : "test", "http://127.0.0.1", i == 0));
            assertThat(popup.getItemCount()).as("Keine Online-Menüeinträge ohne Freigabe").isZero();
            assertThat(toolbarSichtbar(sichtbaresFenster)).as("PTM-Online-Toolbar ohne Freigabe ausgeblendet").isFalse();
            PtmOnlineLiveBeobachter.zuruecksetzenFuerTest();
            assertThat(instanz.get(null)).isNull();
        }
    }

    private void schalte(PtmOnlineMenuController menuController, LibreOfficePtmOnlineSpeicher.Zugangsdaten daten) throws Exception {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(daten);
        CountDownLatch fertig = new CountDownLatch(1);
        AtomicReference<Throwable> fehler = new AtomicReference<>();
        LoMainThread.post(starter.getxComponentContext(), () -> {
            try {
                PtmOnlineLiveBeobachter.konfigurationGeaendert(starter.getxComponentContext());
                menuController.updatePopupMenu();
                PtmOnlineToolbarSteuerung.aktualisiereInAllenFrames(starter.getxComponentContext());
            } catch (Throwable e) {
                fehler.set(e);
            } finally {
                fertig.countDown();
            }
        });
        assertThat(fertig.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(fehler.get()).isNull();
    }

    private static boolean toolbarSichtbar(XFrame frame) throws Exception {
        var layoutManager = Lo.qi(XLayoutManager.class,
                Lo.qi(XPropertySet.class, frame).getPropertyValue("LayoutManager"));
        return layoutManager.isElementVisible(PTMONLINE_TOOLBAR_URL);
    }
}
