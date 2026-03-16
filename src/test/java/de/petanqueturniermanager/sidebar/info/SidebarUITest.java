package de.petanqueturniermanager.sidebar.info;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.ui.XSidebar;
import com.sun.star.ui.XToolPanel;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.sidebar.PetanqueTurnierManagerPanelFactory;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * UITests für die Sidebar: Factory-Registrierung, Panel-Erstellung und Label-Inhalt.
 * <p>
 * DEAKTIVIERT – Sidebar funktioniert in LibreOffice noch nicht korrekt
 * (Factory-Registrierung via UIElementFactoryManager.xcu wird ignoriert,
 * Panel-Inhalt wird nicht angezeigt). Erst wieder aktivieren wenn die
 * Sidebar-Integration vollständig funktioniert.
 */
@Disabled("Sidebar buggy – Panel-Inhalt wird in LibreOffice nicht angezeigt")
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
	@DisplayName("InfoPanel: kein Turniersystem → Label zeigt '–'")
	void infoPanelLabel_OhneTurniersystem_ZeigtStrich() {
		InfoSidebarPanel p = neuesPanel();
		InfoSidebarContent content = (InfoSidebarContent) p.getRealInterface();

		assertThat(content.getTurnierSystemBezeichnung()).isEqualTo("–");
	}

	@Test
	@DisplayName("InfoPanel: nach Turniersystem-Zuweisung zeigt Label den Systemnamen")
	void infoPanelLabel_MitTurniersystem_ZeigtBezeichnung() {
		docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
				TurnierSystem.SUPERMELEE.getId());

		InfoSidebarPanel p = neuesPanel();
		InfoSidebarContent content = (InfoSidebarContent) p.getRealInterface();

		assertThat(content.getTurnierSystemBezeichnung())
				.isEqualTo(TurnierSystem.SUPERMELEE.getBezeichnung());
	}

	@Test
	@DisplayName("InfoPanel: dispose() wirft keine Exception")
	void infoPanelDispose_KeineFehler() {
		InfoSidebarPanel p = neuesPanel();
		panel = null; // AfterEach soll nicht nochmal disposen
		assertThatCode(() -> Lo.qi(XComponent.class, p).dispose()).doesNotThrowAnyException();
	}
}
