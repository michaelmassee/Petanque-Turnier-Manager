/**
 * Erstellung 21.01.2020 / Michael Massee
 * Neu geschrieben 2026-03
 */
package de.petanqueturniermanager.sidebar;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.accessibility.XAccessible;
import com.sun.star.awt.PosSize;
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
 * Saubere Basisklasse für Sidebar-Panels.
 * <p>
 * Vereinfachungen gegenüber der alten Implementierung:
 * <ul>
 * <li>Kein RequestLayoutThread (2s-Sleep) – {@code xSidebar.requestLayout()} wird direkt aufgerufen</li>
 * <li>Kein {@code changingLayout}-volatile-Flag</li>
 * <li>Kein {@code didOnHandleDocReady}-Flag</li>
 * <li>{@code onNew}/{@code onLoad} aktualisieren {@code currentSpreadsheet} direkt</li>
 * </ul>
 *
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
	private Layout layout;
	private boolean istBereinigt = false;
	/** Gesetzt wenn GTK-Fenster-Erstellung während FillToolbar verschoben wurde. */
	private boolean ausstehendInit = false;

	/**
	 * Wenn das Panel während FillToolbar (Druckvorschau-Exit) konstruiert wird:
	 * <ul>
	 * <li>GTK-Fenster-Erstellung wird auf {@code onViewCreated} verschoben (re-entrant-Guard)</li>
	 * <li>Keine Registrierung im Delegator oder als {@link ITurnierEventListener} –
	 *     kein Event darf ein uninitialisiertes Panel erreichen</li>
	 * <li>{@link BaseSidebarPanel} registriert sich stattdessen im {@link SidebarPanelDelegator}
	 *     und leitet {@code onViewCreated} hierher weiter, sobald der Controller bereit ist</li>
	 * </ul>
	 * Im Normalfall ({@code ausstehendInit == false}) werden {@code fensterAdapter},
	 * Delegator und TurnierEventListener VOR {@link #felderHinzufuegen()} registriert,
	 * damit das {@code requestLayout()} am Ende korrekt auf {@code windowResized → doLayout()} führt.
	 */
	public BaseSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		currentSpreadsheet = checkNotNull(workingSpreadsheet);
		this.xSidebar = checkNotNull(xSidebar);
		this.parentWindow = checkNotNull(parentWindow);

		this.parentWindow.addWindowListener(fensterAdapter);

		if (PetanqueTurnierMngrSingleton.isDruckvorschauAktiv()) {
			ausstehendInit = true;
			window = Lo.qi(XWindow.class, parentWindow); // Platzhalter für getWindow() bis Initialisierung abgeschlossen
		} else {
			neuesBasisFenster();
			registrieren();
			felderHinzufuegen();
		}
	}

	private void registrieren() {
		SidebarPanelDelegator.get().registrieren(this);
		PetanqueTurnierMngrSingleton.addTurnierEventListener(this);
	}

	private void neuesBasisFenster() {
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

	/**
	 * Entfernt alle Felder und erstellt das Basis-Fenster neu. Kann von Unterklassen
	 * in {@link #felderAktualisieren(ITurnierEvent)} aufgerufen werden, wenn ein
	 * vollständiger Neuaufbau nötig ist.
	 */
	protected void allesFelderEntfernenUndNeuFenster() {
		logger.debug("allesFelderEntfernenUndNeuFenster");
		window.dispose();
		window = null;
		guiFactoryCreateParam.clear();
		guiFactoryCreateParam = null;
		neuesBasisFenster();
	}

	/**
	 * Ruft {@code xSidebar.requestLayout()} auf und erzwingt anschließend sofort
	 * ein {@code doLayout()}, damit die Breite auch dann korrekt gesetzt wird,
	 * wenn LO keinen {@code windowResized}-Event feuert (z.B. bei Rebuild ohne
	 * Höhenänderung).
	 */
	protected void requestLayout() {
		if (xSidebar != null) {
			xSidebar.requestLayout();
		}
		doLayout();
	}

	/**
	 * Räumt diesen Content auf: entfernt Fenster-Listener, Turnier-Event-Listener
	 * und disposed das Kind-Fenster. Wird von {@link BaseSidebarPanel} aufgerufen,
	 * wenn LibreOffice das Panel disposes. Defensiv: alle Abmeldungen sind no-ops,
	 * falls die Registrierung nie stattgefunden hat.
	 */
	void bereinigen() {
		if (istBereinigt) {
			return;
		}
		istBereinigt = true;
		logger.debug("BaseSidebarContent.bereinigen");

		try {
			SidebarPanelDelegator.get().entfernen(this);
		} catch (RuntimeException e) {
			logger.error("Fehler beim Entfernen des GlobalEventListeners", e);
		}
		try {
			PetanqueTurnierMngrSingleton.removeTurnierEventListener(this);
		} catch (RuntimeException e) {
			logger.error("Fehler beim Entfernen des TurnierEventListeners", e);
		}
		if (parentWindow != null) {
			try {
				parentWindow.removeWindowListener(fensterAdapter);
			} catch (RuntimeException e) {
				logger.error("Fehler beim Entfernen des FensterListeners", e);
			}
		}
		layout = null;
		try {
			onDisposing(null);
		} catch (RuntimeException e) {
			logger.error("Fehler in onDisposing", e);
		}
		setCurrentSpreadsheet(null);
		setParentWindow(null);
		if (guiFactoryCreateParam != null) {
			guiFactoryCreateParam.clear();
			guiFactoryCreateParam = null;
		}
		xSidebar = null;
		if (window != null && !ausstehendInit) {
			try {
				window.dispose();
			} catch (RuntimeException e) {
				logger.error("Fehler beim Dispose des Fensters", e);
			}
		}
		window = null;
		ausstehendInit = false;
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

	// ----- IGlobalEventListener -----

	@Override
	public void onUnfocus(Object source) {
		// Sidebar wurde aus-/eingeschaltet oder Druckvorschau
		// Felder sind bereits vorhanden – nichts zu tun
	}

	/**
	 * Schließt die verzögerte Initialisierung ab, sobald der ScTabViewShell-Controller
	 * aktiv ist (nach FillToolbar, also außerhalb des re-entrant-kritischen Fensters).
	 * Wird von {@link BaseSidebarPanel#onViewCreated(Object)} direkt aufgerufen.
	 * Erst hier wird der Content vollständig registriert.
	 */
	@Override
	public void onViewCreated(Object source) {
		if (!ausstehendInit || istBereinigt) {
			return;
		}
		var xModel = Lo.qi(XModel.class, source);
		if (xModel == null) {
			return;
		}
		if (Lo.qi(XSpreadsheetView.class, xModel.getCurrentController()) == null) {
			return;
		}
		logger.debug("onViewCreated: verzögerte Initialisierung wird abgeschlossen");
		ausstehendInit = false;
		window = null;
		neuesBasisFenster();
		registrieren();
		felderHinzufuegen();
	}

	@Override
	public void onNew(Object source) {
		aktualisiereSpreadsheetUndFelder(source);
	}

	@Override
	public void onLoad(Object source) {
		aktualisiereSpreadsheetUndFelder(source);
	}

	private void aktualisiereSpreadsheetUndFelder(Object source) {
		if (istBereinigt || ausstehendInit) {
			return;
		}
		XModel xModel = Lo.qi(XModel.class, source);
		if (xModel == null) {
			return;
		}
		XSpreadsheetDocument xSpreadsheetDocument = Lo.qi(XSpreadsheetDocument.class, xModel);
		XSpreadsheetView xSpreadsheetView = Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());
		if (xSpreadsheetDocument != null && xSpreadsheetView != null) {
			currentSpreadsheet = new WorkingSpreadsheet(currentSpreadsheet.getxContext(), xSpreadsheetDocument);
			onSpreadsheetGewechselt(currentSpreadsheet);
			felderAktualisieren(new OnProperiesChangedEvent(currentSpreadsheet.getWorkingSpreadsheetDocument()));
		}
	}

	/**
	 * Wird aufgerufen nachdem {@code currentSpreadsheet} auf ein neues Dokument
	 * aktualisiert wurde (beim Laden oder Neuerstellung eines Dokuments).
	 * Unterklassen können diesen Hook überschreiben, um Listener auf das neue
	 * Dokument umzuhängen.
	 */
	protected void onSpreadsheetGewechselt(WorkingSpreadsheet neuesSpreadsheet) {
		// Standard: keine Aktion
	}

	// ----- XToolPanel -----

	@Override
	public XAccessible createAccessible(XAccessible arg0) {
		if (window == null) {
			throw new DisposedException("Panel wurde bereits disposed", this);
		}
		return Lo.qi(XAccessible.class, getWindow());
	}

	@Override
	public XWindow getWindow() {
		if (window == null) {
			throw new DisposedException("Panel wurde bereits disposed", this);
		}
		return window;
	}

	// ----- ITurnierEventListener -----

	@Override
	public void onPropertiesChanged(ITurnierEvent eventObj) {
		if (istBereinigt || ausstehendInit) {
			return;
		}
		var doc = getCurrentSpreadsheet().getWorkingSpreadsheetDocument();
		if (doc == null || !doc.equals(eventObj.getWorkingSpreadsheetDocument())) {
			return;
		}
		logger.debug("onPropertiesChanged");
		felderAktualisieren(eventObj);
	}

	protected void doLayout() {
		if (istBereinigt || ausstehendInit) {
			return;
		}
		try {
			Rectangle posSizeParent = new Rectangle(0, 0, getParentWindow().getPosSize().Width,
					getParentWindow().getPosSize().Height);
			if (posSizeParent.Width <= 0) {
				return;
			}
			if (window != null) {
				window.setPosSize(0, 0, posSizeParent.Width, posSizeParent.Height, PosSize.POSSIZE);
			}
			if (getLayout() != null) {
				getLayout().layout(posSizeParent);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private final AbstractWindowListener fensterAdapter = new AbstractWindowListener() {
		@Override
		public void windowResized(WindowEvent e) {
			doLayout();
		}

		@Override
		public void disposing(EventObject event) {
			// parentWindow wird disposed – bereinigen() übernimmt alles
			bereinigen();
		}
	};

	protected final XWindow getParentWindow() {
		return parentWindow;
	}

	protected final void setParentWindow(XWindow parentWindow) {
		this.parentWindow = parentWindow;
	}

	public final WorkingSpreadsheet getCurrentSpreadsheet() {
		return currentSpreadsheet;
	}

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

	/** Wird beim Bereinigen aufgerufen. Ressourcen der Unterklasse freigeben. */
	protected abstract void onDisposing(EventObject event);

	/** Felder einmalig beim Aufbau des Panels hinzufügen. */
	protected abstract void felderHinzufuegen();

	/**
	 * Felder aktualisieren (bei Änderungen von Properties oder neuem Dokument).
	 * Kann bei Bedarf {@link #allesFelderEntfernenUndNeuFenster()} aufrufen.
	 */
	protected abstract void felderAktualisieren(ITurnierEvent event);
}
