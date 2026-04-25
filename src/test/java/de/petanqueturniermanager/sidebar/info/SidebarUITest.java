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
import de.petanqueturniermanager.helper.Lo;
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
}
