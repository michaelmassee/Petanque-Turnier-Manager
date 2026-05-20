/*
 * Erstellung 21.01.2020 / Michael Massee
 * Neu geschrieben 2026-03
 */
package de.petanqueturniermanager.sidebar;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.atomic.AtomicInteger;

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
import de.petanqueturniermanager.helper.perflog.PerfLog;
import de.petanqueturniermanager.sidebar.layout.Layout;
import de.petanqueturniermanager.sidebar.layout.VerticalLayout;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

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
	/**
	 * Das Dokument, an dessen Frame dieses Panel gebunden ist. Wird im Konstruktor gesetzt
	 * (sofern bereits ein Doc vorhanden ist) bzw. beim ersten passenden {@code onLoad}/{@code onNew}.
	 * <p>
	 * Hintergrund: {@code OnNew}/{@code OnLoad} werden vom {@code XGlobalEventBroadcaster} global
	 * gefeuert und vom {@link SidebarPanelDelegator} an ALLE Panels verteilt – auch an die
	 * Sidebars anderer Calc-Fenster. Ohne diesen Filter würde z.B. ein zusätzlich geöffnetes
	 * leeres Calc-Dokument das Turnier-Sidebar im Turnier-Fenster auf das leere Doc umschalten
	 * (Sheet-Liste leer, Turniersystem „kein").
	 */
	private XSpreadsheetDocument eigenesDokument;
	private XWindow parentWindow;
	private XSidebar xSidebar;
	private XWindow window;
	private GuiFactoryCreateParam guiFactoryCreateParam;
	private Layout layout;
	private volatile boolean istBereinigt = false;
	/** Gesetzt wenn GTK-Fenster-Erstellung während FillToolbar verschoben wurde. */
	private volatile boolean ausstehendInit = false;
	/** Zählt jeden Rebuild; Callbacks prüfen die Generation, um veraltete Events zu verwerfen. */
	private final AtomicInteger uiGeneration = new AtomicInteger(0);
	/**
	 * Letzte Generation, für die {@code requestLayout()} + vollständiger UI-Aufbau abgeschlossen wurden.
	 * Ist nur dann {@code == uiGeneration}, wenn die UI stabil interaktiv ist.
	 * Initialisiert auf {@code -1}, damit {@link #isUiReady()} vor dem ersten Build {@code false} liefert.
	 */
	private volatile int uiReadyGeneration = -1;
	/**
	 * Event, das während der Init-Phase (bevor {@code doLayout()} erstmals erfolgreich lief) eintraf
	 * und deshalb nicht sofort verarbeitet werden konnte. Wird in {@link #doLayout()} nachgezogen,
	 * sobald die UI stabil ist. Volatile, da es von Event-Threads geschrieben und vom VCL-Thread gelesen wird.
	 */
	private volatile ITurnierEvent ausstehendAktualisierung = null;

	/**
	 * Bewusst minimal: nur Felder zuweisen und {@code fensterAdapter} registrieren.
	 * <p>
	 * UI-Aufbau und Listener-Registrierung erfolgen ausschließlich in
	 * {@link #initialisieren()}, das vom {@link BaseSidebarPanel}-Konstruktor
	 * aufgerufen wird, NACHDEM der Subklassen-Konstruktor (inkl. aller
	 * Field-Initializers) vollständig durchgelaufen ist. Sonst sieht
	 * {@code felderHinzufuegen()} ggf. {@code null}-Listener der Subklasse –
	 * das hat beim Sidebar-Toggle (Aus/Ein mit geladenem Dokument) zu
	 * SIGSEGV-Crashes beim Klick in der Sheet-Liste geführt, weil ein
	 * {@code null}-Listener an die UNO-Bridge übergeben wurde.
	 */
	public BaseSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		currentSpreadsheet = checkNotNull(workingSpreadsheet);
		eigenesDokument = workingSpreadsheet.getWorkingSpreadsheetDocument();
		this.xSidebar = checkNotNull(xSidebar);
		this.parentWindow = checkNotNull(parentWindow);

		this.parentWindow.addWindowListener(fensterAdapter);
	}

	/**
	 * Schließt die Initialisierung ab, sobald die Subklasse vollständig konstruiert
	 * ist. Wird vom {@link BaseSidebarPanel}-Konstruktor aufgerufen.
	 * <p>
	 * Im Druckvorschau-Modus wird die GTK-Fenster-Erstellung auf
	 * {@link #onViewCreated} verschoben (re-entrant-Guard); bis dahin existiert
	 * nur ein Platzhalter-Fenster und {@link BaseSidebarPanel} registriert sich
	 * stattdessen im {@link SidebarPanelDelegator}.
	 * <p>
	 * Im Normalfall werden Delegator und TurnierEventListener VOR
	 * {@link #felderHinzufuegen()} registriert, damit das anschließende
	 * {@code requestLayout()} korrekt auf {@code windowResized → doLayout()}
	 * führt.
	 */
	final void initialisieren() {
		if (PetanqueTurnierMngrSingleton.isDruckvorschauAktiv()) {
			ausstehendInit = true;
			window = Lo.qi(XWindow.class, parentWindow); // Platzhalter für getWindow() bis Initialisierung abgeschlossen
			return;
		}
		neuesBasisFenster();
		registrieren();
		felderHinzufuegen();
		requestLayout();
	}

	private void registrieren() {
		SidebarPanelDelegator.get().registrieren(this);
		PetanqueTurnierMngrSingleton.addTurnierEventListener(this);
	}

	/**
	 * Opak-weiß für den Panel-Hintergrund.
	 * <p>
	 * LibreOffice interpretiert den Wert mit {@code ColorTransparency}-Semantik
	 * ({@code Color aColor(ColorTransparency, nColor)} in
	 * {@code toolkit/source/awt/vclxwindow.cxx}): das hohe Byte steuert die
	 * Transparenz – {@code 0xFF} = transparent, {@code 0x00} = opak. Der frühere
	 * Wert {@code 0xFFFFFFFF} war damit <em>transparent</em>, nicht weiß. Auf
	 * Windows mit D3D-Backend zeigt eine transparente Region uninitialisierte
	 * Pixel als schwarz – exakt das Symptom „schwarze Sidebar während Lauf".
	 */
	private static final int BACKGROUND_OPAQUE_WHITE = 0x00FFFFFF;

	private void neuesBasisFenster() {
		layout = new VerticalLayout(0, 2);
		XMultiComponentFactory xMCF = Lo.qi(XMultiComponentFactory.class,
				currentSpreadsheet.getxContext().getServiceManager());
		XWindowPeer parentWindowPeer = Lo.qi(XWindowPeer.class, parentWindow);
		XToolkit parentToolkit = parentWindowPeer.getToolkit();
		XWindowPeer windowPeer = GuiFactory.createWindow(parentToolkit, parentWindowPeer);
		windowPeer.setBackground(BACKGROUND_OPAQUE_WHITE);
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
		String panel = this.getClass().getSimpleName();
		long startNs = System.nanoTime();
		long t = startNs;
		try {
			vorFensterDispose();
		} catch (Exception e) {
			logger.error("Fehler beim Freigeben vor Window-Dispose", e);
		}
		long tVorFensterDispose = System.nanoTime();
		if (window != null) {
			window.dispose();
		}
		long tWindowDisposed = System.nanoTime();
		uiGeneration.incrementAndGet(); // erst nach dispose – alte UI ist jetzt tot, neue Generation beginnt
		window = null;
		if (guiFactoryCreateParam != null) {
			guiFactoryCreateParam.clear();
			guiFactoryCreateParam = null;
		}
		ausstehendAktualisierung = null; // Rebuild ist vollständige Aktualisierung – pending Event obsolet
		neuesBasisFenster();
		long tNeuesBasisFenster = System.nanoTime();
		felderHinzufuegen();
		long tFelderHinzu = System.nanoTime();
		requestLayout();
		long tFertig = System.nanoTime();
		PerfLog.log(logger,
				"[SIDEBAR-TIMING] {} allesFelderEntfernenUndNeuFenster: vorDispose={} ms, dispose={} ms, neuesFenster={} ms, felderHinzufuegen={} ms, requestLayout={} ms, GESAMT={} ms, thread={}",
				panel,
				(tVorFensterDispose - t) / 1_000_000L,
				(tWindowDisposed - tVorFensterDispose) / 1_000_000L,
				(tNeuesBasisFenster - tWindowDisposed) / 1_000_000L,
				(tFelderHinzu - tNeuesBasisFenster) / 1_000_000L,
				(tFertig - tFelderHinzu) / 1_000_000L,
				(tFertig - startNs) / 1_000_000L,
				Thread.currentThread().getName());
	}

	/**
	 * Hook: unmittelbar vor {@code window.dispose()} in einem Rebuild aufgerufen.
	 * Unterklassen nullen hier ihre UNO-Control-Referenzen, damit ausstehende
	 * VCL-Events (z.B. {@code itemStateChanged}) die Controls nicht mehr vorfinden
	 * und sauber abbrechen. Globale Listener (SheetRunner, Timer usw.) NICHT hier
	 * entfernen – die überleben den Rebuild und gehören in {@link #onDisposing}.
	 */
	protected void vorFensterDispose() {
		// Standard: keine Aktion
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
		uiReadyGeneration = -1;
		ausstehendAktualisierung = null;
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
			parentWindow = null;
		}
		layout = null;
		try {
			onDisposing(null);
		} catch (RuntimeException e) {
			logger.error("Fehler in onDisposing", e);
		}
		currentSpreadsheet = null;
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

	@Override
	public void onFocus(Object source) {
		if (istBereinigt || ausstehendInit) {
			return;
		}
		var xModel = Lo.qi(XModel.class, source);
		if (xModel == null) {
			return;
		}
		var controller = xModel.getCurrentController();
		if (controller == null || Lo.qi(XSpreadsheetView.class, controller) == null) {
			return;
		}
		nachControllerWechsel(source);
	}

	protected final int getUiGeneration() {
		return uiGeneration.get();
	}

	/**
	 * {@code true} sobald {@link #felderHinzufuegen()} + {@link #doLayout()} für die aktuelle
	 * Generation vollständig abgeschlossen sind und das Panel interaktiv ist.
	 * Nur dann dürfen UI-Events (itemStateChanged, selectionChanged) verarbeitet werden.
	 */
	protected final boolean isUiReady() {
		return uiReadyGeneration == uiGeneration.get() && !istBereinigt && !ausstehendInit;
	}

	/**
	 * Prüft, ob die UI noch zur Generation {@code gen} gehört und stabil interaktiv ist.
	 * Zu verwenden in allen deferred Callbacks (via {@code itemDispatcher}),
	 * um veraltete Events sicher zu verwerfen.
	 */
	protected final boolean isUiAlive(int gen) {
		return isUiReady() && gen == uiGeneration.get();
	}

	/**
	 * Hook für Unterklassen: wird nach einem Controller-Wechsel aufgerufen
	 * (sowohl nach verzögerter Initialisierung als auch für aktive Panels bei {@code onViewCreated}).
	 * Typischer Anwendungsfall: UNO-Listener neu binden.
	 */
	protected void nachControllerWechsel(Object source) {
		// Standard: keine Aktion
	}

	/**
	 * Schließt die verzögerte Initialisierung ab, sobald der ScTabViewShell-Controller
	 * aktiv ist (nach FillToolbar, also außerhalb des re-entrant-kritischen Fensters).
	 * Wird von {@link BaseSidebarPanel#onViewCreated(Object)} direkt aufgerufen.
	 * Erst hier wird der Content vollständig registriert.
	 * <p>
	 * Für bereits aktive Panels (kein {@code ausstehendInit}) wird {@link #nachControllerWechsel}
	 * aufgerufen – der verlässlichste Lifecycle-Punkt für Controller-Wechsel.
	 */
	@Override
	public void onViewCreated(Object source) {
		if (istBereinigt) {
			return;
		}
		var xModel = Lo.qi(XModel.class, source);
		if (xModel == null) {
			return;
		}
		if (Lo.qi(XSpreadsheetView.class, xModel.getCurrentController()) == null) {
			return;
		}
		if (ausstehendInit) {
			logger.debug("onViewCreated: verzögerte Initialisierung wird abgeschlossen");
			ausstehendInit = false;
			window = null;
			neuesBasisFenster();
			registrieren();
			felderHinzufuegen();
			requestLayout();
			onSpreadsheetGewechselt(currentSpreadsheet); // nach Layout – Listener dürfen erst jetzt UI anfassen
		}
		nachControllerWechsel(source);
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
			// Globale OnNew/OnLoad-Events werden an alle Sidebars aller Calc-Fenster verteilt.
			// Nur reagieren, wenn das Event zu UNSEREM Dokument gehört.
			if (eigenesDokument != null && !eigenesDokument.equals(xSpreadsheetDocument)) {
				return;
			}
			eigenesDokument = xSpreadsheetDocument;
			currentSpreadsheet = new WorkingSpreadsheet(currentSpreadsheet.getxContext(), xSpreadsheetDocument);
			onSpreadsheetGewechselt(currentSpreadsheet);
			var event = new OnProperiesChangedEvent(currentSpreadsheet.getWorkingSpreadsheetDocument());
			if (isUiReady()) {
				felderAktualisieren(event);
			} else {
				ausstehendAktualisierung = event;
			}
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
		if (!isUiReady()) {
			ausstehendAktualisierung = eventObj;
			return;
		}
		String panel = this.getClass().getSimpleName();
		long startNs = System.nanoTime();
		try {
			felderAktualisieren(eventObj);
		} finally {
			long dauerMs = (System.nanoTime() - startNs) / 1_000_000L;
			PerfLog.log(logger, "[SIDEBAR-TIMING] {} onPropertiesChanged.felderAktualisieren: {} ms, thread={}",
					panel, dauerMs, Thread.currentThread().getName());
		}
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
			int genJetzt = uiGeneration.get();
			if (uiReadyGeneration != genJetzt) {
				uiReadyGeneration = genJetzt;
				var pending = ausstehendAktualisierung;
				if (pending != null) {
					ausstehendAktualisierung = null;
					felderAktualisieren(pending);
				}
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

	public final WorkingSpreadsheet getCurrentSpreadsheet() {
		return currentSpreadsheet;
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
