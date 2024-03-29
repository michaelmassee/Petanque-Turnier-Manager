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

import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.AbstractWindowListener;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.comp.turnierevent.OnProperiesChangedEvent;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.sidebar.layout.Layout;
import de.petanqueturniermanager.sidebar.layout.VerticalLayout;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public abstract class BaseSidebarContent extends ComponentBase
		implements XToolPanel, XSidebarPanel, IGlobalEventListener, ITurnierEventListener {
	private static final Logger logger = LogManager.getLogger(BaseSidebarContent.class);

	private WorkingSpreadsheet currentSpreadsheet;
	private XWindow parentWindow;
	private XSidebar xSidebar;

	private XWindow window;
	private GuiFactoryCreateParam guiFactoryCreateParam;
	private boolean didOnHandleDocReady;
	private Layout layout;
	private volatile boolean changingLayout; // stop mehrere Threads

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
		changingLayout = false; // flag is used to stop the layout managers
		this.parentWindow = checkNotNull(parentWindow);

		newBaseWindow();
		addFields();
		this.parentWindow.addWindowListener(windowAdapter);
		PetanqueTurnierMngrSingleton.addGlobalEventListener(this);
		PetanqueTurnierMngrSingleton.addTurnierEventListener(this);
	}

	private void newBaseWindow() {
		layout = new VerticalLayout(0, 2);
		XMultiComponentFactory xMCF = Lo.qi(XMultiComponentFactory.class,
				currentSpreadsheet.getxContext().getServiceManager());
		XWindowPeer parentWindowPeer = Lo.qi(XWindowPeer.class, parentWindow);
		XToolkit parentToolkit = parentWindowPeer.getToolkit();
		XWindowPeer windowPeer = GuiFactory.createWindow(parentToolkit, parentWindowPeer);
		windowPeer.setBackground(0xffffffff);
		window = Lo.qi(XWindow.class, windowPeer);
		window.setVisible(true);
		guiFactoryCreateParam = new GuiFactoryCreateParam(xMCF, currentSpreadsheet.getxContext(), parentToolkit,
				windowPeer);
	}

	protected void removeAllFieldsAndNewBaseWindow() {
		setChangingLayout(true);
		logger.debug("removeAllFields");
		window.dispose();
		window = null;
		guiFactoryCreateParam.clear();
		guiFactoryCreateParam = null;
		newBaseWindow();
		setChangingLayout(false);
	}

	protected void requestLayout() {
		new RequestLayoutThread().RequestLayout(xSidebar);
	}

	@Override
	public LayoutSize getHeightForWidth(int arg0) {
		if (layout != null) {
			int height = layout.getHeight();
			return new LayoutSize(height, height, height);
		}
		return new LayoutSize(10, 10, 10);
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

	private void refreshCurrentSpreadsheetFromSource(Object source) {
		if (didOnHandleDocReady) {
			return;
		}

		XModel xModel = Lo.qi(XModel.class, source);
		XSpreadsheetDocument xSpreadsheetDocument = Lo.qi(XSpreadsheetDocument.class, xModel);
		XSpreadsheetView xSpreadsheetView = Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());

		// wenn kein XSpreadsheetDocument dann null
		if (xSpreadsheetDocument != null && xSpreadsheetView != null) {
			// sicher gehen das wir das richtige document haben, ist nicht unbedingt das
			// Aktive Doc
			WorkingSpreadsheet workingSpreadsheetFromSource = new WorkingSpreadsheet(currentSpreadsheet.getxContext(),
					xSpreadsheetDocument, xSpreadsheetView);
			// WorkingSpreadsheet workingSpreadsheetFromSource = new
			// WorkingSpreadsheet(currentSpreadsheet.getxContext(), xModel);
			if (!currentSpreadsheet.compareSpreadsheetDocument(workingSpreadsheetFromSource)) {
				// Tatsächlich nicht Aktuell ?
				// bis jetzt nur in Linux ein problem
				currentSpreadsheet = workingSpreadsheetFromSource;
				removeAndAddFields(); // inhalt komplet neu
			} else {
				updateFieldContens(
						new OnProperiesChangedEvent(getCurrentSpreadsheet().getWorkingSpreadsheetDocument()));
			}
			didOnHandleDocReady = true;
		}
	}

	// ----- Implementation of UNO interface XToolPanel -----
	// XToolPanel
	@Override
	public XAccessible createAccessible(XAccessible arg0) {
		if (window == null) {
			throw new DisposedException("Panel is already disposed", this);
		}
		return Lo.qi(XAccessible.class, getWindow());
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
	public void onPropertiesChanged(ITurnierEvent eventObj) {
		// sind wir betroffen ?
		if (!getCurrentSpreadsheet().getWorkingSpreadsheetDocument().equals(eventObj.getWorkingSpreadsheetDocument())) {
			// nein ignore
			return;
		}
		// update fields
		logger.debug("onPropertiesChanged");
		updateFieldContens(eventObj);
	}

	protected void doLayout() {
		try {
			// multi threads nicht weitermachen wenn Panel verändert wird
			if (isChangingLayout()) {
				return;
			}

			// Rectangle posSizeParent = parentWindow.getPosSize();
			// Start offset immer 0,0
			Rectangle posSizeParent = new Rectangle(0, 0, getParentWindow().getPosSize().Width,
					getParentWindow().getPosSize().Height);
			// only when fields are there
			if (getLayout() != null) {
				getLayout().layout(posSizeParent);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private final AbstractWindowListener windowAdapter = new AbstractWindowListener() {
		@Override
		public void windowResized(WindowEvent e) {
			doLayout();
		}

		@Override
		public void disposing(EventObject event) {
			logger.debug("BaseSidebarContent disposing");
			try {
				PetanqueTurnierMngrSingleton.removeGlobalEventListener(BaseSidebarContent.this);
				PetanqueTurnierMngrSingleton.removeTurnierEventListener(BaseSidebarContent.this);
				layout = null;
				BaseSidebarContent.this.disposing(event);
				setCurrentSpreadsheet(null);
				setParentWindow(null);
				getGuiFactoryCreateParam().clear();
				setGuiFactoryCreateParam(null);
				xSidebar = null;
				window.dispose();
				window = null;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
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
	public final WorkingSpreadsheet getCurrentSpreadsheet() {
		return currentSpreadsheet;
	}

	/**
	 * @param currentSpreadsheet the currentSpreadsheet to set
	 */
	protected final void setCurrentSpreadsheet(WorkingSpreadsheet currentSpreadsheet) {
		this.currentSpreadsheet = currentSpreadsheet;
	}

	protected TurnierSystem getTurnierSystemAusDocument() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(getCurrentSpreadsheet());
		return docPropHelper.getTurnierSystemAusDocument();
	}

	public final Layout getLayout() {
		return layout;
	}

	protected final GuiFactoryCreateParam getGuiFactoryCreateParam() {
		return guiFactoryCreateParam;
	}

	protected final void setGuiFactoryCreateParam(GuiFactoryCreateParam guiFactoryCreateParam) {
		this.guiFactoryCreateParam = guiFactoryCreateParam;
	}

	protected boolean isChangingLayout() {
		return changingLayout;
	}

	protected void setChangingLayout(boolean changingLayout) {
		this.changingLayout = changingLayout;
	}

	protected abstract void disposing(EventObject event);

	protected abstract void updateFieldContens(ITurnierEvent eventObj);

	protected abstract void addFields();

	protected abstract void removeAndAddFields();
}
