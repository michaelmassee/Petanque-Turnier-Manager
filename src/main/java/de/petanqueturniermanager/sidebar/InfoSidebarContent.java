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

	InfoLine turnierSystemInfoLine = null;
	InfoLine spielRundeInfoLine = null;
	InfoLine spielTagInfoLine = null;

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

		layout = new VerticalLayout(0, 2);

		xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, currentSpreadsheet.getxContext().getServiceManager());
		XWindowPeer parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);

		toolkit = parentWindowPeer.getToolkit();
		windowPeer = GuiFactory.createWindow(toolkit, parentWindowPeer);
		windowPeer.setBackground(0xffffffff);
		window = UnoRuntime.queryInterface(XWindow.class, windowPeer);
		window.setVisible(true);
		addInfoFields();
	}

	private void addInfoFields() {

		turnierSystemInfoLine = InfoLine.from(xMCF, currentSpreadsheet, toolkit, windowPeer).labelText("Turniersystem :");
		layout.addLayout(turnierSystemInfoLine.getLayout(), 1);

		spielRundeInfoLine = InfoLine.from(xMCF, currentSpreadsheet, toolkit, windowPeer).labelText("Spielrunde :");
		layout.addLayout(spielRundeInfoLine.getLayout(), 1);

		spielTagInfoLine = InfoLine.from(xMCF, currentSpreadsheet, toolkit, windowPeer).labelText("Spieltag :");
		layout.addLayout(spielTagInfoLine.getLayout(), 1);
		updateFields();
	}

	private void updateFields() {
		TurnierSystem turnierSystemAusDocument = TurnierSystem.KEIN;
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(currentSpreadsheet);
		int spielsystem = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM);
		if (spielsystem > -1) {
			turnierSystemAusDocument = TurnierSystem.findById(spielsystem);
		}
		turnierSystemInfoLine.fieldText(turnierSystemAusDocument.getBezeichnung());
		if (turnierSystemAusDocument != TurnierSystem.KEIN) {
			// TODO wenn Turnier vorhanden Konfig lesen ?
			spielRundeInfoLine.fieldText(1);
			spielTagInfoLine.fieldText(1);
		}
	}

	private void updateFields(ITurnierEvent eventObj) {
		turnierSystemInfoLine.fieldText(((OnConfigChangedEvent) eventObj).getTurnierSystem().getBezeichnung());
		spielRundeInfoLine.fieldText(((OnConfigChangedEvent) eventObj).getSpielRundeNr().getNr());
		spielTagInfoLine.fieldText(((OnConfigChangedEvent) eventObj).getSpieltagnr().getNr());
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
		// update fields
		updateFields(eventObj);
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
			turnierSystemInfoLine = null;
			currentSpreadsheet = null;
			parentWindow = null;
		}

	};

}
