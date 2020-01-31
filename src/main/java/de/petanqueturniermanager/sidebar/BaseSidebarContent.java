/**
 * Erstellung 21.01.2020 / Michael Massee
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
import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebar;
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
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.sidebar.layout.Layout;
import de.petanqueturniermanager.sidebar.layout.VerticalLayout;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public abstract class BaseSidebarContent extends ComponentBase implements XToolPanel, XSidebarPanel, IGlobalEventListener, ITurnierEventListener {
	static final Logger logger = LogManager.getLogger(BaseSidebarContent.class);

	private XWindow window;
	private GuiFactoryCreateParam guiFactoryCreateParam;

	private boolean didOnHandleDocReady;

	private WorkingSpreadsheet currentSpreadsheet;
	private XWindow parentWindow;
	private Layout layout;
	private XSidebar xSidebar;

	/**
	 * WorkingSpreadsheet ist nicht immer das Aktuelle Document was wir brauchen. <br>
	 * 1. Sidebar aus wieder an dann okay<br>
	 * 2. nach Druckvorschau dann okay<br>
	 * 3. Bei Neu oder Load, wenn bereits eine Tabelle offen dann dann nicht! okay<br>
	 * <br>
	 *
	 * @param workingSpreadsheet
	 * @param parentWindow
	 * @param xSidebar
	 */

	public BaseSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		currentSpreadsheet = checkNotNull(workingSpreadsheet);
		this.xSidebar = checkNotNull(xSidebar);
		didOnHandleDocReady = false;
		this.parentWindow = checkNotNull(parentWindow);
		layout = new VerticalLayout(0, 2);

		this.parentWindow.addWindowListener(windowAdapter);
		PetanqueTurnierMngrSingleton.addGlobalEventListener(this);
		PetanqueTurnierMngrSingleton.addTurnierEventListener(this);

		newBaseWindow();
		addFields();
	}

	private void newBaseWindow() {
		XMultiComponentFactory xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, currentSpreadsheet.getxContext().getServiceManager());
		XWindowPeer parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);
		XToolkit parentToolkit = parentWindowPeer.getToolkit();
		XWindowPeer windowPeer = GuiFactory.createWindow(parentToolkit, parentWindowPeer);
		windowPeer.setBackground(0xffffffff);
		window = UnoRuntime.queryInterface(XWindow.class, windowPeer);
		window.setVisible(true);
		guiFactoryCreateParam = new GuiFactoryCreateParam(xMCF, currentSpreadsheet.getxContext(), parentToolkit, windowPeer);
	}

	protected void removeAllFields() {
		logger.debug("removeAllFields");
		layout = new VerticalLayout(0, 2);
		guiFactoryCreateParam.getWindowPeer().dispose();
		window.dispose();
		guiFactoryCreateParam.clear();
		guiFactoryCreateParam = null;
		newBaseWindow();
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
		// Druck vorschau
		if (didOnHandleDocReady) {
			return;
		}

		// hier kein update von WorkingSpreadsheet weil im Konstruktor das richtige
		// document vorhanden.
		// felder sind bereits vorhanden
		didOnHandleDocReady = true;
	}

	@Override
	public void onNew(Object source) {
		refreshCurrentSpreadsheetFromSource(source);
	}

	@Override
	public void onLoad(Object source) {
		refreshCurrentSpreadsheetFromSource(source);
	}

	// kommt wenn new,load UND nach der umschaltung von Druckvorschau
	// @Override
	// public void onViewCreated(Object source) {
	// if (didOnHandleDocReady) {
	// return;
	// }
	// refreshCurrentSpreadsheetFromSource(source);
	// }

	private void refreshCurrentSpreadsheetFromSource(Object source) {
		if (didOnHandleDocReady) {
			return;
		}

		XModel compo = UnoRuntime.queryInterface(XModel.class, source);
		XSpreadsheetDocument xSpreadsheetDocument = UnoRuntime.queryInterface(XSpreadsheetDocument.class, compo);
		XSpreadsheetView xSpreadsheetView = UnoRuntime.queryInterface(XSpreadsheetView.class, compo.getCurrentController());

		if (xSpreadsheetDocument != null && xSpreadsheetView != null) {
			didOnHandleDocReady = true;
			// sicher gehen das wir das richtige document haben, ist nicht unbedingt das
			// Aktive Doc
			currentSpreadsheet = new WorkingSpreadsheet(currentSpreadsheet.getxContext(), xSpreadsheetDocument, xSpreadsheetView);
			addFields();
		}
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
		// sind wir betroffen ?
		if (!getCurrentSpreadsheet().compareSpreadsheetDocument(eventObj.getWorkingSpreadsheet())) {
			// nein ignore
			return;
		}

		// update fields
		updateFieldContens(eventObj);
	}

	protected void doLayout() {
		// Rectangle posSizeParent = parentWindow.getPosSize();
		// Start offset immer 0,0
		Rectangle posSizeParent = new Rectangle(0, 0, getParentWindow().getPosSize().Width, getParentWindow().getPosSize().Height);
		getLayout().layout(posSizeParent);
	}

	private final AbstractWindowListener windowAdapter = new AbstractWindowListener() {
		@Override
		public void windowResized(WindowEvent e) {
			doLayout();
		}

		@Override
		public void disposing(EventObject event) {
			super.disposing(event);
			try {
				PetanqueTurnierMngrSingleton.removeGlobalEventListener(BaseSidebarContent.this);
				PetanqueTurnierMngrSingleton.removeTurnierEventListener(BaseSidebarContent.this);
				BaseSidebarContent.this.disposing(event);
				setCurrentSpreadsheet(null);
				setParentWindow(null);
				setGuiFactoryCreateParam(null);
				setxSidebar(null);
			} catch (Exception e) {
				// ignore
			}
		}
	};

	/**
	 * @return the parentWindow
	 */
	protected final XWindow getParentWindow() {
		return parentWindow;
	}

	/**
	 * @param parentWindow the parentWindow to set
	 */
	protected final void setParentWindow(XWindow parentWindow) {
		this.parentWindow = parentWindow;
	}

	/**
	 * @return the currentSpreadsheet
	 */
	protected final WorkingSpreadsheet getCurrentSpreadsheet() {
		return currentSpreadsheet;
	}

	/**
	 * @param currentSpreadsheet the currentSpreadsheet to set
	 */
	protected final void setCurrentSpreadsheet(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = currentSpreadsheet;
	}

	protected TurnierSystem getTurnierSystemAusDocument() {
		TurnierSystem turnierSystemAusDocument = TurnierSystem.KEIN;
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getCurrentSpreadsheet());
		int spielsystem = docPropHelper.getIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM, TurnierSystem.KEIN.getId());
		if (spielsystem > -1) {
			turnierSystemAusDocument = TurnierSystem.findById(spielsystem);
		}
		return turnierSystemAusDocument;
	}

	protected abstract void disposing(EventObject event);

	protected abstract void updateFieldContens(ITurnierEvent eventObj);

	protected abstract void addFields();

	public final Layout getLayout() {
		return layout;
	}

	protected final GuiFactoryCreateParam getGuiFactoryCreateParam() {
		return guiFactoryCreateParam;
	}

	protected final void setGuiFactoryCreateParam(GuiFactoryCreateParam guiFactoryCreateParam) {
		this.guiFactoryCreateParam = guiFactoryCreateParam;
	}

	protected final XSidebar getxSidebar() {
		return xSidebar;
	}

	protected final void setxSidebar(XSidebar xSidebar) {
		this.xSidebar = xSidebar;
	}

}
