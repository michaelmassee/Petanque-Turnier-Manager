/*
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.UIManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.document.XEventBroadcaster;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.addins.UpdatePropertieFunctionsSheetRecalcOnLoad;
import de.petanqueturniermanager.helper.rangliste.RanglisteEingabeSignatur;
import de.petanqueturniermanager.helper.rangliste.RanglisteRefreshListener;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetUpdate;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeGruppenRanglisteSheetUpdate;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheetUpdate;
import de.petanqueturniermanager.timer.TimerManager;
import de.petanqueturniermanager.webserver.WebServerManager;
import de.petanqueturniermanager.comp.adapter.GlobalEventListener;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.comp.newrelease.ReleaseUpdateService;
import de.petanqueturniermanager.sidebar.SidebarAnzeigenListener;
import de.petanqueturniermanager.sidebar.SidebarPanelDelegator;
import de.petanqueturniermanager.toolbar.TimerToolbarSteuerung;
import de.petanqueturniermanager.toolbar.ToolbarAnzeigenListener;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.comp.turnierevent.TurnierEventHandler;
import de.petanqueturniermanager.comp.turnierevent.TurnierEventType;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * @author Michael Massee
 *
 */
public class PetanqueTurnierMngrSingleton {
	private static final Logger logger = LogManager.getLogger(PetanqueTurnierMngrSingleton.class);

	private static GlobalEventListener globalEventListener;
	private static final TurnierEventHandler turnierEventHandler = new TurnierEventHandler();
	private static XComponentContext sharedContext;

	private static AtomicBoolean didRun = new AtomicBoolean(); // is volatile

	/**
	 * Wird auf {@code true} gesetzt wenn ein Druckvorschau-Controller aktiv ist.
	 * Verhindert GTK-Fenster-Erstellung in der Sidebar während des FillToolbar-Übergangs.
	 */
	private static volatile boolean druckvorschauAktiv = false;

	public static boolean isDruckvorschauAktiv() {
		return druckvorschauAktiv;
	}

	public static void setDruckvorschauAktiv(boolean aktiv) {
		druckvorschauAktiv = aktiv;
	}

	private PetanqueTurnierMngrSingleton() {
	}

	/** Liefert den LibreOffice-Komponentenkontext (nach {@link #init} verfügbar). */
	public static XComponentContext getContext() {
		return sharedContext;
	}

	/**
	 * der erste Konstruktur macht Init <br>
	 * wird und kann mehrmals aufgerufen !!
	 *
	 * @param context
	 */
	public static final void init(XComponentContext context) {
		if (didRun.getAndSet(true)) {
			return;
		}
		sharedContext = context;
		StartupInfoLogger.logStartupInfo(context);
		GlobalProperties.get(); // just do an init, read properties if not already there

		logger.debug("PetanqueTurnierMngrSingleton.init");
		logger.info("[STARTUP-TIMING] PetanqueTurnierMngrSingleton.init START jvm-uptime={} ms",
				StartupClock.uptimeMs());
		long initStartNs = System.nanoTime();
		long t = initStartNs;
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// only log
			logger.error(e.getMessage(), e);
		}
		t = logTimingAndReset("UIManager.setSystemLookAndFeel", t);

		globalEventListener(context);
		t = logTimingAndReset("globalEventListener (UNO-Broadcaster)", t);
		I18n.init(context); // muss vor ProcessBox, da ProcessBox I18n-Texte beim Aufbau verwendet
		t = logTimingAndReset("I18n.init", t);
		ProcessBox.init(context);
		t = logTimingAndReset("ProcessBox.init (UNO-Dialog-Aufbau)", t);
		TimerManager.init(context);
		TimerManager.get().addListener(ProcessBox.from());
		TimerManager.get().addListener(WebServerManager.get());
		TimerManager.get().addListener(new TimerToolbarSteuerung(context));
		TimerManager.get().addListener(state -> ProtocolHandler.notifyAllListeners());
		t = logTimingAndReset("TimerManager.init + 4 Listener", t);
		TerminateListener.addThisListenerOnce(context);
		t = logTimingAndReset("TerminateListener.addThisListenerOnce", t);
		ReleaseUpdateService.init(context);
		t = logTimingAndReset("ReleaseUpdateService.init (async)", t);
		if (GlobalProperties.get().isWebserverAktiv()) {
			WebServerManager.get().starten(context);
			t = logTimingAndReset("WebServerManager.starten", t);
		}
		addGlobalEventListener(new ToolbarAnzeigenListener());
		t = logTimingAndReset("addGlobalEventListener ToolbarAnzeigenListener", t);
		addGlobalEventListener(new SidebarAnzeigenListener());
		t = logTimingAndReset("addGlobalEventListener SidebarAnzeigenListener", t);
		addGlobalEventListener(SidebarPanelDelegator.get());
		t = logTimingAndReset("addGlobalEventListener SidebarPanelDelegator", t);
		addGlobalEventListener(new UpdatePropertieFunctionsSheetRecalcOnLoad());
		t = logTimingAndReset("addGlobalEventListener UpdatePropertieFunctionsSheetRecalcOnLoad", t);
		addGlobalEventListener(RanglisteRefreshListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE,
				TurnierSystem.SCHWEIZER,
				new RanglisteEingabeSignatur(SignaturQuellen::fuerSchweizer),
				(ws, ignored) -> new SchweizerRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("RanglisteRefreshListener SCHWEIZER", t);
		addGlobalEventListener(RanglisteRefreshListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
				TurnierSystem.MAASTRICHTER,
				new RanglisteEingabeSignatur(SignaturQuellen::fuerMaastrichter),
				(ws, ignored) -> new MaastrichterVorrundenRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("RanglisteRefreshListener MAASTRICHTER", t);
		addGlobalEventListener(RanglisteRefreshListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE,
				TurnierSystem.POULE,
				new RanglisteEingabeSignatur(SignaturQuellen::fuerPoule),
				(ws, ignored) -> new PouleVorrundenRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("RanglisteRefreshListener POULE", t);
		addGlobalEventListener(RanglisteRefreshListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE,
				TurnierSystem.FORMULEX,
				new RanglisteEingabeSignatur(SignaturQuellen::fuerFormuleX),
				(ws, ignored) -> new FormuleXRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("RanglisteRefreshListener FORMULEX", t);
		addGlobalEventListener(RanglisteRefreshListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE,
				TurnierSystem.JGJ,
				new RanglisteEingabeSignatur(SignaturQuellen::fuerJGJ),
				(ws, ignored) -> new JGJRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("RanglisteRefreshListener JGJ", t);
		addGlobalEventListener(RanglisteRefreshListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE,
				TurnierSystem.KASKADE,
				new RanglisteEingabeSignatur(SignaturQuellen::fuerKaskade),
				(ws, ignored) -> new KaskadeGruppenRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("RanglisteRefreshListener KASKADE", t);
		addGlobalEventListener(RanglisteRefreshListener.fuerSpieltagRangliste(context,
				TurnierSystem.SUPERMELEE,
				spieltagNr -> new RanglisteEingabeSignatur(
						xDoc -> SignaturQuellen.fuerSupermeleeSpieltag(xDoc, spieltagNr)),
				(ws, xSheet) -> {
					SpielTagNr nr = SheetMetadataHelper
							.findeSpieltagNr(ws.getWorkingSpreadsheetDocument(), xSheet)
							.orElse(null);
					return new SpieltagRanglisteSheetUpdate(ws, nr);
				}));
		t = logTimingAndReset("RanglisteRefreshListener SUPERMELEE-SPIELTAG", t);
		addGlobalEventListener(RanglisteRefreshListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE,
				TurnierSystem.SUPERMELEE,
				new RanglisteEingabeSignatur(SignaturQuellen::fuerSupermeleeEnd),
				(ws, ignored) -> new EndranglisteSheetUpdate(ws)));
		logTimingAndReset("RanglisteRefreshListener SUPERMELEE-END", t);

		long initGesamtMs = (System.nanoTime() - initStartNs) / 1_000_000L;
		logger.info("[STARTUP-TIMING] PetanqueTurnierMngrSingleton.init GESAMT={} ms (jvm-uptime={} ms)",
				initGesamtMs, StartupClock.uptimeMs());
	}

	private static long logTimingAndReset(String abschnitt, long startNs) {
		long jetzt = System.nanoTime();
		long dauerMs = (jetzt - startNs) / 1_000_000L;
		logger.info("[STARTUP-TIMING] {}: {} ms", abschnitt, dauerMs);
		return jetzt;
	}

	// register global EventListener
	private static final synchronized void globalEventListener(XComponentContext context) {
		if (globalEventListener != null) {
			return;
		}
		try {
			globalEventListener = new GlobalEventListener();
			Object globalEventBroadcaster = context.getServiceManager()
					.createInstanceWithContext("com.sun.star.frame.GlobalEventBroadcaster", context);
			XEventBroadcaster eventBroadcaster = Lo.qi(XEventBroadcaster.class, globalEventBroadcaster);
			eventBroadcaster.addEventListener(globalEventListener);
		} catch (Exception e) {
			// alles ignorieren nur logen
			logger.error("", e);
		}
	}

	public static void addGlobalEventListener(IGlobalEventListener listner) {
		if (globalEventListener != null) {
			globalEventListener.addGlobalEventListener(listner);
		}
	}

	public static void removeGlobalEventListener(IGlobalEventListener listner) {
		if (globalEventListener != null) {
			globalEventListener.removeGlobalEventListener(listner);
		}
	}

	// ---------------------------------------------------------------------------------------------
	public static void addTurnierEventListener(ITurnierEventListener listner) {
		turnierEventHandler.addTurnierEventListener(listner);
	}

	public static void removeTurnierEventListener(ITurnierEventListener listner) {
		turnierEventHandler.removeTurnierEventListener(listner);
	}

	/**
	 * Feuert einen ggf. während eines aktiven {@code SheetRunner}-Laufs
	 * koaleszierten TurnierEvent. Wird aus dem {@code SheetRunner}-{@code finally}-
	 * Block aufgerufen, nachdem der ControllerLock freigegeben wurde.
	 */
	public static void flushPendingTurnierEvent() {
		turnierEventHandler.flushPending();
	}

	public static void triggerTurnierEventListener(TurnierEventType type, ITurnierEvent eventObj) {
		turnierEventHandler.trigger(type, eventObj);
	}

	// ---------------------------------------------------------------------------------------------
	public static void dispose() {
		TimerManager.dispose();
		WebServerManager.get().stoppen();
		try {
			ReleaseUpdateService.get().dispose();
		} catch (IllegalStateException e) {
			// Service nie initialisiert – ok.
		}
		if (globalEventListener != null) {
			globalEventListener.disposing(null);
		}
		turnierEventHandler.disposing();
		// didRun zurücksetzen, damit ein erneuter init() nach dispose() wieder
		// die abhängigen Subsysteme (TimerManager, ProcessBox, …) hochfährt.
		// Sonst bleibt z.B. TimerManager null und nachfolgende UI-Tests scheitern
		// in der Sidebar mit "TimerManager nicht initialisiert".
		didRun.set(false);
	}

}
