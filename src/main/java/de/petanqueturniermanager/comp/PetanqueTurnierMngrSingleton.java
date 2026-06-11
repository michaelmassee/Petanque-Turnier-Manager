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
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncListener;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.perflog.PerfLog;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXCheckinListeSheetUpdate;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXTeilnehmerSheetUpdate;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJCheckinListeSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJTeilnehmerSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetUpdate;
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
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheetUpdate;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.maastrichter.blattschutz.MaastrichterBlattschutzKonfiguration;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.blattschutz.SchweizerBlattschutzKonfiguration;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.supermelee.blattschutz.SupermeleeBlattschutzKonfiguration;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;
import de.petanqueturniermanager.triptete.blattschutz.TripTeteBlattschutzKonfiguration;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;
import de.petanqueturniermanager.triptete.spielplan.TripTeteSpielPlanSheet;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeCheckinListeSheetUpdate;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeTeilnehmerSheetUpdate;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeGruppenRanglisteSheetUpdate;
import de.petanqueturniermanager.ko.meldeliste.KoCheckinListeSheetUpdate;
import de.petanqueturniermanager.ko.meldeliste.KoTeilnehmerSheetUpdate;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterCheckinListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterTeilnehmerSheetUpdate;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.poule.meldeliste.PouleCheckinListeSheetUpdate;
import de.petanqueturniermanager.poule.meldeliste.PouleTeilnehmerSheetUpdate;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheetUpdate;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerCheckinListeSheetUpdate;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerTeilnehmerSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteCheckinListeSheetUpdate;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteTeilnehmerSheetUpdate;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.SupermeleeTeilnehmerSheetUpdate;
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
		// Vor jeder AWT-/Swing-Berührung (UIManager unten, javax.swing.Timer im
		// SeitenstileDebouncer): auf macOS würde die AWT-Initialisierung auf den
		// AppKit-Main-Thread warten, den LibreOffice selbst besitzt → Deadlock.
		MacAwtHeadlessSchutz.aktiviereFallsMacOS();
		sharedContext = context;
		StartupInfoLogger.logStartupInfo(context);
		GlobalProperties.get(); // just do an init, read properties if not already there

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
		addGlobalEventListener(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE,
				TurnierSystem.SCHWEIZER,
				new EingabeSignatur(SignaturQuellen::fuerSchweizer),
				(ws, ignored) -> new SchweizerRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener SCHWEIZER", t);
		addGlobalEventListener(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
				TurnierSystem.MAASTRICHTER,
				new EingabeSignatur(SignaturQuellen::fuerMaastrichter),
				(ws, ignored) -> new MaastrichterVorrundenRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener MAASTRICHTER", t);
		addGlobalEventListener(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_POULE_VORRUNDEN_RANGLISTE,
				TurnierSystem.POULE,
				new EingabeSignatur(SignaturQuellen::fuerPoule),
				(ws, ignored) -> new PouleVorrundenRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener POULE", t);
		addGlobalEventListener(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_RANGLISTE,
				TurnierSystem.FORMULEX,
				new EingabeSignatur(SignaturQuellen::fuerFormuleX),
				(ws, ignored) -> new FormuleXRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener FORMULEX", t);
		addGlobalEventListener(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_JGJ_RANGLISTE,
				TurnierSystem.JGJ,
				new EingabeSignatur(SignaturQuellen::fuerJGJ),
				(ws, ignored) -> new JGJRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener JGJ", t);
		addGlobalEventListener(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_LIGA_RANGLISTE,
				TurnierSystem.LIGA,
				new EingabeSignatur(SignaturQuellen::fuerLiga),
				(ws, ignored) -> new LigaRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener LIGA", t);
		addGlobalEventListener(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_KASKADE_GRUPPENRANGLISTE,
				TurnierSystem.KASKADE,
				new EingabeSignatur(SignaturQuellen::fuerKaskade),
				(ws, ignored) -> new KaskadeGruppenRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener TRIPTETE", t);
		addGlobalEventListener(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_RANGLISTE,
				TurnierSystem.TRIPTETE,
				new EingabeSignatur(SignaturQuellen::fuerTripTete),
				(ws, ignored) -> new TripTeteRanglisteSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener KASKADE", t);
		addGlobalEventListener(SheetSyncListener.fuerSpieltagRangliste(context,
				TurnierSystem.SUPERMELEE,
				spieltagNr -> new EingabeSignatur(
						xDoc -> SignaturQuellen.fuerSupermeleeSpieltag(xDoc, spieltagNr)),
				(ws, xSheet) -> {
					SpielTagNr nr = SheetMetadataHelper
							.findeSpieltagNr(ws.getWorkingSpreadsheetDocument(), xSheet)
							.orElse(null);
					return new SpieltagRanglisteSheetUpdate(ws, nr);
				}));
		t = logTimingAndReset("SheetSyncListener SUPERMELEE-SPIELTAG", t);
		addGlobalEventListener(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_SUPERMELEE_ENDRANGLISTE,
				TurnierSystem.SUPERMELEE,
				new EingabeSignatur(SignaturQuellen::fuerSupermeleeEnd),
				(ws, ignored) -> new EndranglisteSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener SUPERMELEE-END", t);
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSpieltagTeilnehmer(context,
				TurnierSystem.SUPERMELEE,
				spieltagNr -> new EingabeSignatur(
						xDoc -> SignaturQuellen.fuerSupermeleeTeilnehmer(xDoc, spieltagNr),
						SignaturQuellen::teilnehmerSortKontext),
				(ws, xSheet) -> {
					SpielTagNr nr = SheetMetadataHelper
							.findeTeilnehmerSpieltagNr(ws.getWorkingSpreadsheetDocument(), xSheet)
							.orElse(null);
					SupermeleeTeilnehmerSheetUpdate update = new SupermeleeTeilnehmerSheetUpdate(ws);
					if (nr != null) {
						update.setSpielTagNr(nr);
					}
					return update;
				}));
		t = logTimingAndReset("SheetSyncListener SUPERMELEE-TEILNEHMER", t);

		// Teilnehmerlisten der Einzel-Sheet-Systeme: alle unter dem generischen
		// SCHLUESSEL_TEILNEHMER registriert; die Eindeutigkeit ergibt sich aus dem
		// TurnierSystem-Gate. Synchronisiert die Liste beim Tab-Wechsel mit der Meldeliste.
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				TurnierSystem.SCHWEIZER,
				new EingabeSignatur(SignaturQuellen::fuerSchweizerTeilnehmer,
						SignaturQuellen::teilnehmerSortKontext),
				(ws, ignored) -> new SchweizerTeilnehmerSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				TurnierSystem.JGJ,
				new EingabeSignatur(SignaturQuellen::fuerJGJTeilnehmer,
						SignaturQuellen::teilnehmerSortKontext),
				(ws, ignored) -> new JGJTeilnehmerSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				TurnierSystem.KO,
				new EingabeSignatur(SignaturQuellen::fuerKoTeilnehmer,
						SignaturQuellen::teilnehmerSortKontext),
				(ws, ignored) -> new KoTeilnehmerSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				TurnierSystem.MAASTRICHTER,
				new EingabeSignatur(SignaturQuellen::fuerMaastrichterTeilnehmer,
						SignaturQuellen::teilnehmerSortKontext),
				(ws, ignored) -> new MaastrichterTeilnehmerSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				TurnierSystem.KASKADE,
				new EingabeSignatur(SignaturQuellen::fuerKaskadeTeilnehmer,
						SignaturQuellen::teilnehmerSortKontext),
				(ws, ignored) -> new KaskadeTeilnehmerSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				TurnierSystem.FORMULEX,
				new EingabeSignatur(SignaturQuellen::fuerFormuleXTeilnehmer,
						SignaturQuellen::teilnehmerSortKontext),
				(ws, ignored) -> new FormuleXTeilnehmerSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				TurnierSystem.POULE,
				new EingabeSignatur(SignaturQuellen::fuerPouleTeilnehmer,
						SignaturQuellen::teilnehmerSortKontext),
				(ws, ignored) -> new PouleTeilnehmerSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TEILNEHMER,
				TurnierSystem.TRIPTETE,
				new EingabeSignatur(SignaturQuellen::fuerTripTeteTeilnehmer,
						SignaturQuellen::teilnehmerSortKontext),
				(ws, ignored) -> new TripTeteTeilnehmerSheetUpdate(ws)));
		t = logTimingAndReset("SheetSyncListener TEILNEHMER (Einzel-Sheet-Systeme)", t);

		// Checkin-Listen: synchronisieren beim Tab-Wechsel mit der Meldeliste – analog zu den
		// Teilnehmerlisten. Eingabe-Quelle ist (wie dort) ausschließlich die Meldeliste, daher
		// werden die vorhandenen SignaturQuellen.fuer*Teilnehmer wiederverwendet. Jedes System
		// hat einen eigenen Checkin-Schlüssel; die Eindeutigkeit ergibt sich aus dem
		// TurnierSystem-Gate.
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_POULE_CHECKIN_LISTE, TurnierSystem.POULE,
				new EingabeSignatur(SignaturQuellen::fuerPouleTeilnehmer,
						SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new PouleCheckinListeSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_CHECKIN_LISTE, TurnierSystem.SCHWEIZER,
				new EingabeSignatur(SignaturQuellen::fuerSchweizerTeilnehmer,
						SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new SchweizerCheckinListeSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_KO_CHECKIN_LISTE, TurnierSystem.KO,
				new EingabeSignatur(SignaturQuellen::fuerKoTeilnehmer,
						SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new KoCheckinListeSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_CHECKIN_LISTE, TurnierSystem.MAASTRICHTER,
				new EingabeSignatur(SignaturQuellen::fuerMaastrichterTeilnehmer,
						SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new MaastrichterCheckinListeSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_KASKADE_CHECKIN_LISTE, TurnierSystem.KASKADE,
				new EingabeSignatur(SignaturQuellen::fuerKaskadeTeilnehmer,
						SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new KaskadeCheckinListeSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_CHECKIN_LISTE, TurnierSystem.FORMULEX,
				new EingabeSignatur(SignaturQuellen::fuerFormuleXTeilnehmer,
						SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new FormuleXCheckinListeSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_JGJ_CHECKIN_LISTE, TurnierSystem.JGJ,
				new EingabeSignatur(SignaturQuellen::fuerJGJTeilnehmer,
						SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new JGJCheckinListeSheetUpdate(ws)));
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSchluessel(context,
				SheetMetadataHelper.SCHLUESSEL_TRIPTETE_CHECKIN_LISTE, TurnierSystem.TRIPTETE,
				new EingabeSignatur(SignaturQuellen::fuerTripTeteTeilnehmer,
						SignaturQuellen::checkinSortKontext),
				(ws, ignored) -> new TripTeteCheckinListeSheetUpdate(ws)));
		// Supermelee-Anmeldungen (Checkin-Liste) sind spieltag-variabel – eigener Schlüssel je Nr.
		addSheetSyncMitPropertyTrigger(SheetSyncListener.fuerSpieltagSheet(context,
				TurnierSystem.SUPERMELEE,
				SheetMetadataHelper::findeAnmeldungenSpieltagNr,
				"SUPERMELEE_ANMELDUNGEN_",
				spieltagNr -> new EingabeSignatur(
						xDoc -> SignaturQuellen.fuerSupermeleeTeilnehmer(xDoc, spieltagNr),
						SignaturQuellen::checkinSortKontext),
				(ws, xSheet) -> {
					SpielTagNr nr = SheetMetadataHelper
							.findeAnmeldungenSpieltagNr(ws.getWorkingSpreadsheetDocument(), xSheet)
							.orElse(null);
					AnmeldungenSheetUpdate update = new AnmeldungenSheetUpdate(ws);
					if (nr != null) {
						update.setSpielTag(nr);
					}
					return update;
				}));
		logTimingAndReset("SheetSyncListener CHECKIN-LISTEN", t);

		// Spielplan-Formatierer: reparieren Zebra und editierbare-Felder-CF beim Tab-Aktivieren
		addGlobalEventListener(SpielplanFormatiererActivationListener.fuerSchluessel(context,
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

		addGlobalEventListener(SpielplanFormatiererActivationListener.fuerSchluessel(context,
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

		addGlobalEventListener(SpielplanFormatiererActivationListener.fuerSchluessel(context,
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
							RangePosition.from(TripTeteSpielPlanSheet.BAHN_TRI_SPALTE,
									TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE,
									TripTeteSpielPlanSheet.BAHN_TETE_SPALTE, letzteZeile),
							RangePosition.from(TripTeteSpielPlanSheet.TRI_A_SPALTE,
									TripTeteSpielPlanSheet.ERSTE_DATEN_ZEILE,
									TripTeteSpielPlanSheet.TETE_B_SPALTE, letzteZeile));
					return new SpielplanFormatiererKonfig(datenRange, editierbar,
							konfig.getSpielPlanHintergrundFarbeGerade(),
							konfig.getSpielPlanHintergrundFarbeUnGerade(),
							TripTeteBlattschutzKonfiguration.get());
				})));

		addGlobalEventListener(SpielplanFormatiererActivationListener.fuerPraefix(context,
				SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_SPIELRUNDE_PREFIX,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, 0,
							SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, 999))
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

		addGlobalEventListener(SpielplanFormatiererActivationListener.fuerPraefix(context,
				SheetMetadataHelper.SCHLUESSEL_FORMULEX_SPIELRUNDE_PREFIX,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							FormuleXAbstractSpielrundeSheet.BAHN_NR_SPALTE, 0,
							FormuleXAbstractSpielrundeSheet.BAHN_NR_SPALTE, 999))
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

		addGlobalEventListener(SpielplanFormatiererActivationListener.fuerPraefix(context,
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

		addGlobalEventListener(SpielplanFormatiererActivationListener.fuerPraefix(context,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, 0,
							SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, 999))
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

		addGlobalEventListener(SpielplanFormatiererActivationListener.fuerPraefix(context,
				SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_FINALRUNDE_PREFIX,
				(ws, xSheet) -> new SpielplanFormatiererSheetRunner(ws, xSheet, iSheet -> {
					Position letzte = RangeSearchHelper.from(iSheet, RangePosition.from(
							SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, 0,
							SchweizerAbstractSpielrundeSheet.BAHN_NR_SPALTE, 999))
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

	/**
	 * Registriert einen {@link SheetSyncListener} sowohl für View-Events (Tab-Wechsel, Fokus)
	 * als auch für {@code PropertiesChanged}-Events. Letzteres ist nötig für Listen, deren
	 * Signatur einen Konfig-Zusatzkontext enthält (z.B. Sortiermodus der Teilnehmer-/Checkin-Liste):
	 * Ein Wechsel im Konfig-Dialog löst sonst keinen View-Trigger aus und die Liste würde erst
	 * beim nächsten Tab-Wechsel neu sortiert.
	 */
	private static void addSheetSyncMitPropertyTrigger(SheetSyncListener listner) {
		addGlobalEventListener(listner);
		addTurnierEventListener(listner);
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
		turnierEventHandler.disposing();
		sharedContext = null;
		didRun.set(false);
	}

}
