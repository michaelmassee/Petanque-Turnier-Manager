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
import de.petanqueturniermanager.comp.kompatibilitaet.DokumentKompatibilitaetsHinweis;
import de.petanqueturniermanager.helper.sheetsync.SheetAktivierungsDispatcher;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncNachzieher;
import de.petanqueturniermanager.helper.perflog.PerfLog;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.formulex.blattschutz.FormuleXBlattschutzKonfiguration;
import de.petanqueturniermanager.formulex.konfiguration.FormuleXKonfigurationSheet;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXAbstractSpielrundeSheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.helper.sheetsync.SpielplanFormatiererActivationListener;
import de.petanqueturniermanager.helper.sheetsync.SpielplanFormatiererKonfig;
import de.petanqueturniermanager.helper.sheetsync.SpielplanFormatiererSheetRunner;
import de.petanqueturniermanager.jedergegenjeden.blattschutz.JGJBlattschutzKonfiguration;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.liga.blattschutz.LigaBlattschutzKonfiguration;
import de.petanqueturniermanager.liga.konfiguration.LigaKonfigurationSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.maastrichter.blattschutz.MaastrichterBlattschutzKonfiguration;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.ptmonline.PtmOnlineLiveBeobachter;
import de.petanqueturniermanager.schweizer.blattschutz.SchweizerBlattschutzKonfiguration;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.supermelee.blattschutz.SupermeleeBlattschutzKonfiguration;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;
import de.petanqueturniermanager.triptete.blattschutz.TripTeteBlattschutzKonfiguration;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;
import de.petanqueturniermanager.timer.TimerManager;
import de.petanqueturniermanager.webserver.WebServerManager;
import de.petanqueturniermanager.comp.adapter.GlobalEventListener;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.comp.newrelease.AutoUpdateStartupChecker;
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
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * @author Michael Massee
 *
 */
public class PetanqueTurnierMngrSingleton {
	private static final Logger logger = LogManager.getLogger(PetanqueTurnierMngrSingleton.class);

	private static GlobalEventListener globalEventListener;
	/** Zentraler Verteiler für Sheet-Aktivierungen – hält genau einen UNO-Selection-Listener pro Controller. */
	private static SheetAktivierungsDispatcher sheetAktivierungsDispatcher;
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
		// Vor jeder AWT-/Swing-Berührung (UIManager unten): auf macOS würde die
		// AWT-Initialisierung auf den AppKit-Main-Thread warten, den LibreOffice
		// selbst besitzt → Deadlock.
		MacAwtHeadlessSchutz.aktiviereFallsMacOS();
		sharedContext = context;
		StartupInfoLogger.logStartupInfo(context);
		LoVersionChecker.pruefeUndLoggeKompatibilitaet(context);
		GlobalProperties.setLibreOfficeContext(context);
		GlobalProperties.get(); // just do an init, read properties if not already there
		PtmOnlineAktivierung.initialisieren(context);

		logger.debug("PetanqueTurnierMngrSingleton.init");
		PerfLog.log(logger, "[STARTUP-TIMING] PetanqueTurnierMngrSingleton.init START jvm-uptime={} ms",
				StartupClock.uptimeMs());
		long initStartNs = System.nanoTime();
		long t = initStartNs;
		// Swing-LookAndFeel nur dort setzen, wo AWT nicht headless läuft (siehe
		// MacAwtHeadlessSchutz): auf macOS gibt es keine Swing-Dialoge im LO-Prozess.
		if (!java.awt.GraphicsEnvironment.isHeadless()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				// only log
				logger.error(e.getMessage(), e);
			}
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
		var autoUpdateStartupChecker = new AutoUpdateStartupChecker(context);
		ReleaseUpdateService.get().addStatusListener(autoUpdateStartupChecker::pruefeUndZeige);
		autoUpdateStartupChecker.pruefeUndZeige();
		t = logTimingAndReset("ReleaseUpdateService.init (async) + AutoUpdateStartupChecker", t);
		// Kein Webserver-Bootstrap-Start hier: WebServerManager.starten() braucht ein konkretes
		// Owner-Dokument (kein fokus-basiertes Raten, siehe CLAUDE.md „Mehrere offene
		// Turnier-Dokumente"). Die Bindung übernimmt ProtocolHandler deterministisch, sobald ein
		// Dokument tatsächlich da ist (onLoad-Handler bzw. Frame-Scan-Catch-up im Ctor).
		addGlobalEventListener(new ToolbarAnzeigenListener());
		t = logTimingAndReset("addGlobalEventListener ToolbarAnzeigenListener", t);
		addGlobalEventListener(new SidebarAnzeigenListener());
		t = logTimingAndReset("addGlobalEventListener SidebarAnzeigenListener", t);
		addGlobalEventListener(SidebarPanelDelegator.get());
		if (PtmOnlineAktivierung.istAktiv()) {
			addGlobalEventListener(PtmOnlineLiveBeobachter.init(context));
			LoMainThread.post(context, () -> PtmOnlineLiveBeobachter.konfigurationGeaendert(context));
		}
		t = logTimingAndReset("addGlobalEventListener SidebarPanelDelegator", t);
		addGlobalEventListener(new UpdatePropertieFunctionsSheetRecalcOnLoad());
		t = logTimingAndReset("addGlobalEventListener UpdatePropertieFunctionsSheetRecalcOnLoad", t);
		addGlobalEventListener(new DokumentKompatibilitaetsHinweis(context));
		sheetAktivierungsDispatcher = new SheetAktivierungsDispatcher();
		addGlobalEventListener(sheetAktivierungsDispatcher);
		t = logTimingAndReset("addGlobalEventListener SheetAktivierungsDispatcher", t);
		for (SheetSyncKatalog.Eintrag eintrag : SheetSyncKatalog.erstelle(context)) {
			sheetAktivierungsDispatcher.registriere(eintrag.listener());
			SheetSyncNachzieher.get().registriere(eintrag.listener());
			if (eintrag.mitPropertyTrigger()) {
				addTurnierEventListener(eintrag.listener());
			}
		}
		logTimingAndReset("SheetSyncKatalog registriert", t);

		// Spielplan-Formatierer: reparieren Zebra und editierbare-Felder-CF beim Tab-Aktivieren
		sheetAktivierungsDispatcher.registriere(SpielplanFormatiererActivationListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_LIGA_SPIELPLAN,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					int letzteZeile = LigaSpielPlanSheet.letzteSpielZeile(iSheet);
					if (letzteZeile < LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE) return null;
					var konfig = new LigaKonfigurationSheet(ws);
					var datenRange = RangePosition.from(LigaSpielPlanSheet.SPIEL_NR_SPALTE,
							LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
							LigaSpielPlanSheet.SPIELPNKT_B_SPALTE, letzteZeile);
					var editierbar = java.util.List.of(
							RangePosition.from(LigaSpielPlanSheet.DATUM_SPALTE,
									LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
									LigaSpielPlanSheet.ORT_SPALTE, letzteZeile),
							RangePosition.from(LigaSpielPlanSheet.SPIELE_A_SPALTE,
									LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
									LigaSpielPlanSheet.SPIELPNKT_B_SPALTE, letzteZeile));
					return new SpielplanFormatiererKonfig(datenRange, editierbar,
							konfig.getSpielPlanHintergrundFarbeGerade(),
							konfig.getSpielPlanHintergrundFarbeUnGerade(),
							LigaBlattschutzKonfiguration.get());
				})));

		sheetAktivierungsDispatcher.registriere(SpielplanFormatiererActivationListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							JGJSpielPlanSheet.SPIEL_NR_SPALTE, 0, JGJSpielPlanSheet.SPIEL_NR_SPALTE, 999))
							.searchLastNotEmptyInSpalte();
					if (letzte == null) return null;
					int letzteZeile = letzte.getZeile();
					if (letzteZeile < JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE) return null;
					var konfig = new JGJKonfigurationSheet(ws);
					var datenRange = RangePosition.from(JGJSpielPlanSheet.SPIEL_NR_SPALTE,
							JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
							JGJSpielPlanSheet.SPIELPNKT_B_SPALTE, letzteZeile);
					var editierbar = java.util.List.of(RangePosition.from(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE,
							JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
							JGJSpielPlanSheet.SPIELPNKT_B_SPALTE, letzteZeile));
					return new SpielplanFormatiererKonfig(datenRange, editierbar,
							konfig.getSpielPlanHintergrundFarbeGerade(),
							konfig.getSpielPlanHintergrundFarbeUnGerade(),
							JGJBlattschutzKonfiguration.get());
				})));

		sheetAktivierungsDispatcher.registriere(SpielplanFormatiererActivationListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_SPIELPLAN,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							TripTeteSpielPlanSheet.SPIEL_NR_SPALTE, 0, TripTeteSpielPlanSheet.SPIEL_NR_SPALTE, 999))
							.searchLastNotEmptyInSpalte();
					if (letzte == null) return null;
					int letzteZeile = letzte.getZeile();
					if (letzteZeile < TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE) return null;
					var konfig = new TripTeteKonfigurationSheet(ws);
					var datenRange = RangePosition.from(TripTeteSpielPlanSheet.SPIEL_NR_SPALTE,
							TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE,
							TripTeteSpielPlanSheet.TETE_B_SPALTE, letzteZeile);
					var editierbar = java.util.List.of(
							RangePosition.from(TripTeteSpielPlanSheet.TRI_A_SPALTE,
									TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE,
									TripTeteSpielPlanSheet.TETE_B_SPALTE, letzteZeile));
					return new SpielplanFormatiererKonfig(datenRange, editierbar,
							konfig.getSpielPlanHintergrundFarbeGerade(),
							konfig.getSpielPlanHintergrundFarbeUnGerade(),
							TripTeteBlattschutzKonfiguration.get());
				})));

		sheetAktivierungsDispatcher.registriere(SpielplanFormatiererActivationListener.fuerPraefix(context,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, 0,
							SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, 999))
							.searchLastNotEmptyInSpalte();
					if (letzte == null) return null;
					int letzteZeile = letzte.getZeile();
					if (letzteZeile < SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE) return null;
					var konfig = new SchweizerKonfigurationSheet(ws);
					var datenRange = RangePosition.from(SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE,
							SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
							SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, letzteZeile);
					var editierbar = java.util.List.of(RangePosition.from(
							SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
							SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
							SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, letzteZeile));
					return new SpielplanFormatiererKonfig(datenRange, editierbar,
							konfig.getSpielRundeHintergrundFarbeGerade(),
							konfig.getSpielRundeHintergrundFarbeUnGerade(),
							SchweizerBlattschutzKonfiguration.get());
				})));

		sheetAktivierungsDispatcher.registriere(SpielplanFormatiererActivationListener.fuerPraefix(context,
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							FormuleXAbstractSpielrundeSheet.TEAM_A_SPALTE, 0,
							FormuleXAbstractSpielrundeSheet.TEAM_A_SPALTE, 999))
							.searchLastNotEmptyInSpalte();
					if (letzte == null) return null;
					int letzteZeile = letzte.getZeile();
					if (letzteZeile < FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE) return null;
					var konfig = new FormuleXKonfigurationSheet(ws);
					var datenRange = RangePosition.from(FormuleXAbstractSpielrundeSheet.BAHN_NR_SPALTE,
							FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
							FormuleXAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, letzteZeile);
					// Freilos-Zellen haben keine editierbare CF → CF-Repair nur bei komplett fehlendem CF
					var editierbar = java.util.List.of(RangePosition.from(
							FormuleXAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
							FormuleXAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
							FormuleXAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, letzteZeile));
					return new SpielplanFormatiererKonfig(datenRange, editierbar,
							konfig.getSpielRundeHintergrundFarbeGerade(),
							konfig.getSpielRundeHintergrundFarbeUnGerade(),
							FormuleXBlattschutzKonfiguration.get());
				})));

		sheetAktivierungsDispatcher.registriere(SpielplanFormatiererActivationListener.fuerPraefix(context,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_SPIELRUNDE_PREFIX,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							SpielrundeSheetKonstanten.PAARUNG_CNTR_SPALTE, 0,
							SpielrundeSheetKonstanten.PAARUNG_CNTR_SPALTE, 999))
							.searchLastNotEmptyInSpalte();
					if (letzte == null) return null;
					int letzteZeile = letzte.getZeile();
					if (letzteZeile < SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE) return null;
					var konfig = new SuperMeleeKonfigurationSheet(ws);
					var datenRange = RangePosition.from(SpielrundeSheetKonstanten.NUMMER_SPALTE_RUNDESPIELPLAN,
							SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
							SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE + 1, letzteZeile);
					var editierbar = java.util.List.of(RangePosition.from(
							SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE,
							SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE,
							SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE + 1, letzteZeile));
					return new SpielplanFormatiererKonfig(datenRange, editierbar,
							konfig.getSpielRundeHintergrundFarbeGerade(),
							konfig.getSpielRundeHintergrundFarbeUnGerade(),
							SupermeleeBlattschutzKonfiguration.get());
				})));

		sheetAktivierungsDispatcher.registriere(SpielplanFormatiererActivationListener.fuerPraefix(context,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, 0,
							SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, 999))
							.searchLastNotEmptyInSpalte();
					if (letzte == null) return null;
					int letzteZeile = letzte.getZeile();
					if (letzteZeile < SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE) return null;
					var konfig = new MaastrichterKonfigurationSheet(ws);
					var datenRange = RangePosition.from(SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE,
							SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
							SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, letzteZeile);
					var editierbar = java.util.List.of(RangePosition.from(
							SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE,
							SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
							SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, letzteZeile));
					return new SpielplanFormatiererKonfig(datenRange, editierbar,
							konfig.getSpielRundeHintergrundFarbeGerade(),
							konfig.getSpielRundeHintergrundFarbeUnGerade(),
							MaastrichterBlattschutzKonfiguration.get());
				})));

		long initGesamtMs = (System.nanoTime() - initStartNs) / 1_000_000L;
		PerfLog.log(logger, "[STARTUP-TIMING] PetanqueTurnierMngrSingleton.init GESAMT={} ms (jvm-uptime={} ms)",
				initGesamtMs, StartupClock.uptimeMs());
	}

	private static long logTimingAndReset(String abschnitt, long startNs) {
		long jetzt = System.nanoTime();
		long dauerMs = (jetzt - startNs) / 1_000_000L;
		PerfLog.log(logger, "[STARTUP-TIMING] {}: {} ms", abschnitt, dauerMs);
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
		PtmOnlineAktivierung.beenden();
		PtmOnlineLiveBeobachter.dispose();
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
		sharedContext = null;
		// didRun zurücksetzen, damit ein erneuter init() nach dispose() wieder
		// die abhängigen Subsysteme (TimerManager, ProcessBox, …) hochfährt.
		// Sonst bleibt z.B. TimerManager null und nachfolgende UI-Tests scheitern
		// in der Sidebar mit "TimerManager nicht initialisiert".
		didRun.set(false);
	}

	/**
	 * Reset des statischen Zustands für UI-Tests. Wird zwischen Test-Klassen aufgerufen
	 * (BaseCalcUITest.@AfterAll), damit der nachfolgende Test mit frischem LO-Prozess nicht auf
	 * abgehängte Bridge-Proxies (Dispatcher, sharedContext) der vorherigen Office-Instanz trifft.
	 * <p>
	 * Im Unterschied zu {@link #dispose()} ohne Side-effects auf Subsysteme, die im Test-JVM
	 * gar nicht initialisiert wurden (TimerManager, WebServer, ReleaseUpdateService).
	 */
	public static void resetForTest() {
		PtmOnlineLiveBeobachter.zuruecksetzenFuerTest();
		turnierEventHandler.disposing();
		sharedContext = null;
		didRun.set(false);
	}

}
