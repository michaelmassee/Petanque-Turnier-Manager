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
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.LayoutSize;
import com.sun.star.ui.XSidebarPanel;
import com.sun.star.ui.XToolPanel;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.AbstractWindowListener;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.sidebar.layout.Layout;
import de.petanqueturniermanager.sidebar.layout.VerticalLayout;

/**
 * @author Michael Massee
 *
 */
public abstract class BaseSidebarContent extends ComponentBase implements XToolPanel, XSidebarPanel, IGlobalEventListener, ITurnierEventListener {
	static final Logger logger = LogManager.getLogger(BaseSidebarContent.class);

	private final XWindow window;
	private final XMultiComponentFactory xMCF;
	private final XToolkit toolkit;
	private final XWindowPeer windowPeer;
	private boolean didOnHandleDocReady;

	private WorkingSpreadsheet currentSpreadsheet;
	private XWindow parentWindow;
	final Layout layout;

	public BaseSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow) {
		currentSpreadsheet = checkNotNull(workingSpreadsheet);
		didOnHandleDocReady = false;
		this.parentWindow = checkNotNull(parentWindow);
		layout = new VerticalLayout(0, 2);

		this.parentWindow.addWindowListener(windowAdapter);
		PetanqueTurnierMngrSingleton.addGlobalEventListener(this);
		PetanqueTurnierMngrSingleton.addTurnierEventListener(this);

		xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, currentSpreadsheet.getxContext().getServiceManager());
		XWindowPeer parentWindowPeer = UnoRuntime.queryInterface(XWindowPeer.class, parentWindow);

		toolkit = parentWindowPeer.getToolkit();
		windowPeer = GuiFactory.createWindow(getToolkit(), parentWindowPeer);
		getWindowPeer().setBackground(0xffffffff);
		window = UnoRuntime.queryInterface(XWindow.class, getWindowPeer());
		window.setVisible(true);
		addFields();
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
		// sicher gehen das wir das richtige document haben
		currentSpreadsheet = new WorkingSpreadsheet(currentSpreadsheet.getxContext());

		// update fields
		initFields();
	}

	@Override
	public void onNew(Object source) {
		if (didOnHandleDocReady) {
			return;
		}
		didOnHandleDocReady = true;
		// sicher gehen das wir das richtige document haben
		currentSpreadsheet = new WorkingSpreadsheet(currentSpreadsheet.getxContext());

		// update fields
		initFields();
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
			Rectangle posSizeParent = new Rectangle(0, 0, getParentWindow().getPosSize().Width, getParentWindow().getPosSize().Height);
			layout.layout(posSizeParent);
		}

		@Override
		public void disposing(EventObject event) {
			super.disposing(event);
			PetanqueTurnierMngrSingleton.removeGlobalEventListener(BaseSidebarContent.this);
			PetanqueTurnierMngrSingleton.removeTurnierEventListener(BaseSidebarContent.this);
			BaseSidebarContent.this.disposing(event);
			setCurrentSpreadsheet(null);
			setParentWindow(null);
		}
	};

	/**
	 * @return the parentWindow
	 */
	protected final XWindow getParentWindow() {
		return parentWindow;
	}

	/**
	 * @return the xMCF
	 */
	protected final XMultiComponentFactory getxMCF() {
		return xMCF;
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

	/**
	 * @return the toolkit
	 */
	protected XToolkit getToolkit() {
		return toolkit;
	}

	/**
	 * @return the windowPeer
	 */
	protected XWindowPeer getWindowPeer() {
		return windowPeer;
	}

	protected abstract void disposing(EventObject event);

	protected abstract void updateFields(ITurnierEvent eventObj);

	protected abstract void initFields();

	protected abstract void addFields();

}
