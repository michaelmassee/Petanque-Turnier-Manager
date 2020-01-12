/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.sidebar.adapter.AbstractWindowListener;
import de.petanqueturniermanager.sidebar.layout.Layout;
import de.petanqueturniermanager.sidebar.layout.VerticalLayout;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 * vorlage<br>
 * de.muenchen.allg.itd51.wollmux.sidebar.SeriendruckSidebarContent;
 *
 */
public class InfoSidebarContent extends ComponentBase implements XToolPanel, XSidebarPanel {

	private static final Logger logger = LogManager.getLogger(InfoSidebarContent.class);

	private final XWindow window;
	private final XMultiComponentFactory xMCF;
	private final WorkingSpreadsheet currentSpreadsheet;
	private final XToolkit toolkit;
	private final XWindowPeer windowPeer;

	final XWindow parentWindow;
	final Layout layout;

	private final AbstractWindowListener windowAdapter = new AbstractWindowListener() {
		@Override
		public void windowResized(WindowEvent e) {
			// Rectangle posSizeParent = parentWindow.getPosSize();
			// Start offset immer 0,0
			Rectangle posSizeParent = new Rectangle(0, 0, parentWindow.getPosSize().Width, parentWindow.getPosSize().Height);
			layout.layout(posSizeParent);
		}
	};

	/**
	 * @param context
	 * @param parentWindow
	 */
	public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow) {
		currentSpreadsheet = checkNotNull(workingSpreadsheet);
		this.parentWindow = checkNotNull(parentWindow);

		this.parentWindow.addWindowListener(windowAdapter);
		layout = new VerticalLayout(0, 10);

		xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, currentSpreadsheet.getxContext().getServiceManager());
		XWindowPeer parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);

		toolkit = parentWindowPeer.getToolkit();
		windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
		windowPeer.setBackground(0xffffffff);
		window = UnoRuntime.queryInterface(XWindow.class, windowPeer);
		window.setVisible(true);
		initContent();
	}

	private void initContent() {
		addInfoFields();
	}

	private void addInfoFields() {
		int lineHeight = 15;
		int lineWidth = 200;

		// TODO: Wenn bereits ein Dokument Aktiv mit Turniersystem vorhanden, dann false currentSpreadsheet
		TurnierSystem turnierSystemAusDocument = TurnierSystem.KEIN;
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		int spielsystem = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM);
		if (spielsystem > -1) {
			turnierSystemAusDocument = TurnierSystem.findById(spielsystem);
		}
		layout.addControl(GuiFactory.createLabel(xMCF, currentSpreadsheet.getxContext(), toolkit, windowPeer, "Turniersystem : " + turnierSystemAusDocument.toString(),
				new Rectangle(0, 0, lineWidth, lineHeight), null));
	}

	@Override
	public LayoutSize getHeightForWidth(int arg0) {
		int height = layout.getHeight();
		return new LayoutSize(height, height, height);
	}

	@Override
	public int getMinimalWidth() {
		return 200;
	}

	// ----- Implementation of UNO interface XToolPanel -----
	// XToolPanel
	@Override
	public XAccessible createAccessible(XAccessible arg0) {
		if (window == null) {
			throw new DisposedException("Panel is already disposed", this);
		}
		return UnoRuntime.queryInterface(XAccessible.class, getWindow());
	}

	// XToolPanel
	@Override
	public XWindow getWindow() {
		if (window == null) {
			throw new DisposedException("Panel is already disposed", this);
		}
		return window;
	}

}
