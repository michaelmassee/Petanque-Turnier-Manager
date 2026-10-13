package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
import de.petanqueturniermanager.comp.PtmOnlineMenuController;

class PtmOnlineAktivierungUITest extends BaseCalcUITest {
    @AfterEach
    void resetOnline() {
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
            assertThat(popup.getItemCount()).as("Alle sechs Online-Befehle wiederhergestellt").isEqualTo((short) 6);
            var instanz = PtmOnlineLiveBeobachter.class.getDeclaredField("instanz");
            instanz.setAccessible(true);
            Object beobachter = instanz.get(null);
            assertThat(beobachter).isNotNull();
            var feld = PtmOnlineLiveBeobachter.class.getDeclaredField("beobachtungen");
            feld.setAccessible(true);
            assertThat((Map<?, ?>) feld.get(beobachter)).as("Bereits offenes Dokument wird übernommen").hasSize(1);

            schalte(menuController, new LibreOfficePtmOnlineSpeicher.Zugangsdaten(i == 0 ? "" : "test", "http://127.0.0.1", i == 0));
            assertThat(popup.getItemCount()).as("Keine Online-Menüeinträge ohne Freigabe").isZero();
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
            } catch (Throwable e) {
                fehler.set(e);
            } finally {
                fertig.countDown();
            }
        });
        assertThat(fertig.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(fehler.get()).isNull();
    }

}
