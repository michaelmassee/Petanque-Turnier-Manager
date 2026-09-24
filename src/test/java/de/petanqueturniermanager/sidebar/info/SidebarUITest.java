package de.petanqueturniermanager.sidebar.info;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.sidebar.PetanqueTurnierManagerPanelFactory;

/**
 * UITests für die Sidebar: Factory-Registrierung, Panel-Erstellung und Versionsanzeige.
 * <p>
 * Voraussetzung: Extension muss via {@code ./gradlew reinstallExtension} installiert sein.
 */
@DisplayName("Sidebar")
public class SidebarUITest extends BaseCalcUITest {

	/** Keine echte Sidebar nötig – requestLayout() ist im Test ein No-Op. */
	private static class MinimalXSidebar implements XSidebar {
		@Override
		public void requestLayout() {
			// intentional no-op
		}
	}

	private InfoSidebarPanel panel;

	@AfterEach
	void disposePanel() {
		if (panel != null) {
			Lo.qi(XComponent.class, panel).dispose();
			panel = null;
		}
	}

	private InfoSidebarPanel neuesPanel() {
		XModel model = Lo.qi(XModel.class, doc);
		XWindow containerWindow = model.getCurrentController().getFrame().getContainerWindow();
		panel = new InfoSidebarPanel(wkingSpreadsheet, containerWindow,
				PetanqueTurnierManagerPanelFactory.URL_PREFIX + "/InfoPanel",
				new MinimalXSidebar());
		return panel;
	}

	// ─── Tests ────────────────────────────────────────────────────────────────

	@Test
	@DisplayName("PanelFactory: Implementierungsname und Service-Support korrekt")
	void panelFactoryServiceInfo() {
		PetanqueTurnierManagerPanelFactory factory = new PetanqueTurnierManagerPanelFactory(
				starter.getxComponentContext());

		assertThat(factory.getImplementationName())
				.isEqualTo("de.petanqueturniermanager.sidebar.PetanqueTurnierManagerPanelFactory");
		assertThat(factory.supportsService(
				"de.petanqueturniermanager.sidebar.PetanqueTurnierManagerPanelFactory")).isTrue();
	}

	@Test
	@DisplayName("InfoPanel: getRealInterface() liefert XToolPanel mit XWindow")
	void infoPanelHatXToolPanelMitXWindow() {
		InfoSidebarPanel p = neuesPanel();

		XToolPanel toolPanel = Lo.qi(XToolPanel.class, p.getRealInterface());
		assertThat(toolPanel).isNotNull();
		assertThat(toolPanel.getWindow()).isNotNull();
	}

	@Test
	@DisplayName("InfoPanel: ResourceURL korrekt gesetzt")
	void infoPanelResourceUrl() {
		InfoSidebarPanel p = neuesPanel();
		assertThat(p.getResourceURL())
				.isEqualTo(PetanqueTurnierManagerPanelFactory.URL_PREFIX + "/InfoPanel");
	}

	@Test
	@DisplayName("InfoPanel: Label zeigt installierte Versionsnummer")
	void infoPanelLabel_ZeigtVersion() {
		InfoSidebarPanel p = neuesPanel();
		InfoSidebarContent content = (InfoSidebarContent) p.getRealInterface();

		assertThat(content.getPluginVersion()).isNotEmpty();
	}

	@Test
	@DisplayName("InfoPanel: dispose() wirft keine Exception")
	void infoPanelDispose_KeineFehler() {
		InfoSidebarPanel p = neuesPanel();
		panel = null; // AfterEach soll nicht nochmal disposen
		assertThatCode(() -> Lo.qi(XComponent.class, p).dispose()).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("InfoPanel: PTM-Online-Status nicht verbunden → verbunden → pausiert")
	void infoPanel_ZeigtPtmOnlineStatus() throws Exception {
		new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TETE, false, false);
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SCHWEIZER.getId());
		InfoSidebarContent content = (InfoSidebarContent) neuesPanel().getRealInterface();
		assertThat(content.ptmOnlineStatusAnzeige()).isEqualTo(I18n.get("sidebar.info.ptmonline.nicht_verbunden"));

		PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet,
				TurnierSystem.SCHWEIZER, null);
		OnlineTournamentDto turnier = new OnlineTournamentDto();
		turnier.id = "t1";
		turnier.name = "Testturnier";
		mapping.verbinden(turnier, new SyncBindingDto(true, "e9e9caec-e0b1-4fe0-8fee-a229279b9f73", 1),
				"01234567890123456789012345678901");
		assertThat(content.ptmOnlineStatusAnzeige()).isEqualTo(I18n.get("sidebar.info.ptmonline.verbunden_ohne_sync"));

		mapping.setPausiert(true);
		assertThat(content.ptmOnlineStatusAnzeige()).isEqualTo(I18n.get("sidebar.info.ptmonline.pausiert"));
	}

	@Test
	@DisplayName("InfoPanel: Erstellung läuft auch unter Kiosk-Modus")
	void kioskModus_panelLaesstSichErstellen() throws Exception {
		mitKioskModusOhneSchutz(() -> {
			InfoSidebarPanel p = neuesPanel();
			InfoSidebarContent content = (InfoSidebarContent) p.getRealInterface();
			assertThat(content.getPluginVersion()).isNotEmpty();
		});
	}
}
