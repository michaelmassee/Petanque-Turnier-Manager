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
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.AbstractWindowListener;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.comp.turnierevent.OnConfigChangedEvent;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
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
public class InfoSidebarContent extends ComponentBase implements XToolPanel, XSidebarPanel, IGlobalEventListener, ITurnierEventListener {

	static final Logger logger = LogManager.getLogger(InfoSidebarContent.class);

	private final XWindow window;
	private final XMultiComponentFactory xMCF;
	private final XToolkit toolkit;
	private final XWindowPeer windowPeer;
	private boolean didOnHandleDocReady;

	WorkingSpreadsheet currentSpreadsheet;
	XWindow parentWindow;
	final Layout layout;
	private XFixedText turnierSystemLabel = null;

	/**
	 * Jedes Document eigene Instance
	 *
	 * @param context
	 * @param parentWindow
	 */
	public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow) {
		currentSpreadsheet = checkNotNull(workingSpreadsheet);
		this.parentWindow = checkNotNull(parentWindow);

		this.parentWindow.addWindowListener(windowAdapter);
		didOnHandleDocReady = false;
		PetanqueTurnierMngrSingleton.addGlobalEventListener(this);
		PetanqueTurnierMngrSingleton.addTurnierEventListener(this);

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
		int lineHeight = 20;
		int lineWidth = 200;

		XControl turnierSystemLabelControl = GuiFactory.createLabel(xMCF, currentSpreadsheet.getxContext(), toolkit, windowPeer, "Turniersystem : " + TurnierSystem.KEIN,
				new Rectangle(0, 0, lineWidth, lineHeight), null);
		layout.addControl(turnierSystemLabelControl);
		turnierSystemLabel = UnoRuntime.queryInterface(XFixedText.class, turnierSystemLabelControl);
		updateFields();
	}

	private void updateFields() {
		TurnierSystem turnierSystemAusDocument = TurnierSystem.KEIN;
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		int spielsystem = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM);
		if (spielsystem > -1) {
			turnierSystemAusDocument = TurnierSystem.findById(spielsystem);
		}
		updateFields(turnierSystemAusDocument);
	}

	private void updateFields(TurnierSystem turnierSystem) {
		if (turnierSystemLabel != null) {
			turnierSystemLabel.setText("Turniersystem : " + turnierSystem);
		}
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

	// ----- Implementation of interface IGlobalEventListener -----
	@Override
	public void onUnfocus(Object source) {
		// dann der fall wenn kein onLoad oder onNew
		// passiert wenn einfach nur die sidebar aus / an geschalted wird
		didOnHandleDocReady = true;
	}

	@Override
	public void onLoad(Object source) {
		if (didOnHandleDocReady) {
			return;
		}
		didOnHandleDocReady = true;
		// update fields
		updateFields();
	}

	@Override
	public void onNew(Object source) {
		if (didOnHandleDocReady) {
			return;
		}
		didOnHandleDocReady = true;
		// wenn ein Document neu erstellt wird dann currentSpreadsheet updaten
		currentSpreadsheet = new WorkingSpreadsheet(currentSpreadsheet.getxContext());

		// update fields
		updateFields();
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

	// ----- Implementation of interface ITurnierEventListener -----
	@Override
	public void onConfigChanged(ITurnierEvent eventObj) {
		TurnierSystem turnierSystem = ((OnConfigChangedEvent) eventObj).getTurnierSystem();
		// update fields
		updateFields(turnierSystem);
	}

	private final AbstractWindowListener windowAdapter = new AbstractWindowListener() {
		@Override
		public void windowResized(WindowEvent e) {
			// Rectangle posSizeParent = parentWindow.getPosSize();
			// Start offset immer 0,0
			Rectangle posSizeParent = new Rectangle(0, 0, parentWindow.getPosSize().Width, parentWindow.getPosSize().Height);
			layout.layout(posSizeParent);
		}

		@Override
		public void disposing(EventObject event) {
			super.disposing(event);
			PetanqueTurnierMngrSingleton.removeGlobalEventListener(InfoSidebarContent.this);
			PetanqueTurnierMngrSingleton.removeTurnierEventListener(InfoSidebarContent.this);
			currentSpreadsheet = null;
			parentWindow = null;
		}

		// @Override
		// public void windowHidden(EventObject event) {
		// logger.debug("InfoSidebarContent:windowHidden");
		// }

	};

}
