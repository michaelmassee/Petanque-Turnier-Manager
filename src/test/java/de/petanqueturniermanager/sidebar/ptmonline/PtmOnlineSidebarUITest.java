/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.ui.XSidebar;
import com.sun.star.ui.XToolPanel;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.sidebar.PetanqueTurnierManagerPanelFactory;
import de.petanqueturniermanager.sidebar.ptmonline.PtmOnlineSidebarContent.Zeilen;

/**
 * Panel „PTM-Online“ unter „PTM Info“: eine Zeile je Angabe, Zeilen ohne Inhalt entfallen.
 * <p>
 * Voraussetzung: Extension muss via {@code ./gradlew reinstallExtension} installiert sein.
 */
@DisplayName("Sidebar PTM-Online")
class PtmOnlineSidebarUITest extends BaseCalcUITest {

    /** Keine echte Sidebar nötig – requestLayout() ist im Test ein No-Op. */
    private static final class MinimalXSidebar implements XSidebar {
        @Override
        public void requestLayout() {
            // bewusst leer
        }
    }

    private PtmOnlineSidebarPanel panel;

    @AfterEach
    void aufraeumen() {
        if (panel != null) {
            Lo.qi(XComponent.class, panel).dispose();
            panel = null;
        }
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(null);
    }

    @Test
    @DisplayName("Panel lässt sich über die Factory-URL erzeugen")
    void panelHatXToolPanelMitFenster() {
        XToolPanel toolPanel = Lo.qi(XToolPanel.class, neuesPanel().getRealInterface());

        assertThat(toolPanel).isNotNull();
        assertThat(toolPanel.getWindow()).isNotNull();
        assertThat(panel.getResourceURL()).isEqualTo(PetanqueTurnierManagerPanelFactory.URL_PREFIX + "/PtmOnlinePanel");
    }

    @Test
    @DisplayName("Nicht aktiviert, nicht verbunden, verbunden mit Status, pausiert")
    void zeigtJeAngabeEineZeile() throws Exception {
        zugang("test", true);
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TETE, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        PtmOnlineSidebarContent content = (PtmOnlineSidebarContent) neuesPanel().getRealInterface();
        assertThat(content.zeilen()).as("nicht verbunden: nur eine Zeile")
                .isEqualTo(new Zeilen(I18n.get("sidebar.ptmonline.nicht_verbunden"), Optional.empty(),
                        Optional.empty()));

        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet,
                TurnierSystem.SCHWEIZER, null);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = "t1";
        turnier.name = "Testturnier";
        turnier.status = "draft";
        mapping.verbinden(turnier, new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
                "01234567890123456789012345678901");
        assertThat(content.zeilen()).as("verbunden, noch ohne Sync")
                .isEqualTo(new Zeilen(I18n.get("sidebar.ptmonline.verbunden"), Optional.empty(),
                        Optional.of(I18n.get("sidebar.ptmonline.onlinestatus",
                                I18n.get("ptmonline.onlinestatus.draft")))));

        mapping.setLastSync(Instant.parse("2026-11-13T20:20:00Z"));
        assertThat(content.zeilen().letzterSync()).as("mit Sync drei Zeilen").isPresent();

        mapping.setPausiert(true);
        assertThat(content.zeilen().verbindung()).isEqualTo(I18n.get("sidebar.ptmonline.pausiert"));

        zugang("", true);
        assertThat(content.zeilen()).as("ohne API-Key")
                .isEqualTo(new Zeilen(I18n.get("sidebar.ptmonline.nicht_aktiv"), Optional.empty(),
                        Optional.empty()));
        zugang("test", false);
        assertThat(content.zeilen().verbindung()).as("bei deaktivierter Verbindung")
                .isEqualTo(I18n.get("sidebar.ptmonline.nicht_aktiv"));
    }

    private PtmOnlineSidebarPanel neuesPanel() {
        XWindow containerWindow = Lo.qi(XModel.class, doc).getCurrentController().getFrame().getContainerWindow();
        panel = new PtmOnlineSidebarPanel(wkingSpreadsheet, containerWindow,
                PetanqueTurnierManagerPanelFactory.URL_PREFIX + "/PtmOnlinePanel", new MinimalXSidebar());
        return panel;
    }

    private static void zugang(String apiKey, boolean aktiv) {
        LibreOfficePtmOnlineSpeicher.setZugangsdatenForTest(
                new LibreOfficePtmOnlineSpeicher.Zugangsdaten(apiKey, "http://127.0.0.1", aktiv));
    }
}
