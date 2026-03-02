/**
 * Erstellung : 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.util.URL;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.comp.newrelease.DownloadExtension;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetSortOnly;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.konfigdialog.properties.FarbenDialog;
import de.petanqueturniermanager.konfigdialog.properties.KopfFusszeilenDialog;
import de.petanqueturniermanager.konfigdialog.properties.TurnierDialog;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetExport;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetNew;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetTestDaten;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheetUpdate;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheetSortOnly;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheetTestDaten;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet;
import de.petanqueturniermanager.supermelee.endrangliste.EndranglisteSheet_Sort;
import de.petanqueturniermanager.supermelee.meldeliste.AnmeldungenSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_New;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.meldeliste.TielnehmerSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundePlan;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Update;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Validator;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet_SortOnly;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRangliste_Validator;

/**
 * UNO ProtocolHandler für das benutzerdefinierte Protokoll "ptm:".
 * <p>
 * Implementiert {@link XDispatchProvider} und {@link XDispatch}, um
 * Menüpunkte dynamisch zu aktivieren/deaktivieren basierend auf dem
 * aktuellen Turniersystem im Dokument.
 * <p>
 * Zentraler Einstiegspunkt für alle Menüaktionen aller Turniersysteme.
 */
public class ProtocolHandler extends WeakBase implements XDispatchProvider, XDispatch, XServiceInfo {

	private static final Logger logger = LogManager.getLogger(ProtocolHandler.class);

	public static final String PROTOCOL = "ptm:";
	private static final String IMPLEMENTATION_NAME = ProtocolHandler.class.getName();
	private static final String[] SERVICE_NAMES = { "de.petanqueturniermanager.ProtocolHandler" };

	// Shared state across all instances (LibreOffice may create multiple per session)
	private static final Map<String, List<StatusEntry>> STATUS_LISTENERS =
			Collections.synchronizedMap(new HashMap<>());
	private static volatile XComponentContext SHARED_CONTEXT;
	private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

	private final XComponentContext xContext;

	public ProtocolHandler(XComponentContext xContext) {
		this.xContext = xContext;
		SHARED_CONTEXT = xContext;
		PetanqueTurnierMngrSingleton.init(xContext);
		if (REGISTERED.compareAndSet(false, true)) {
			PetanqueTurnierMngrSingleton.addGlobalEventListener(new IGlobalEventListener() {
				@Override
				public void onFocus(Object source) {
					notifyAllListeners();
				}

				@Override
				public void onLoadFinished(Object source) {
					notifyAllListeners();
				}

				@Override
				public void onNew(Object source) {
					notifyAllListeners();
				}
			});
			PetanqueTurnierMngrSingleton.addTurnierEventListener(new ITurnierEventListener() {
				@Override
				public void onPropertiesChanged(ITurnierEvent event) {
					notifyAllListeners();
				}
			});
			SheetRunner.addStateChangeListener(ProtocolHandler::notifyAllListeners);
		}
	}

	// -------------------------------------------------------------------------
	// XDispatchProvider

	@Override
	public XDispatch queryDispatch(URL url, String targetFrameName, int searchFlags) {
		if (PROTOCOL.equalsIgnoreCase(url.Protocol)) {
			return this;
		}
		return null;
	}

	@Override
	public XDispatch[] queryDispatches(DispatchDescriptor[] requests) {
		XDispatch[] dispatches = new XDispatch[requests.length];
		for (int i = 0; i < requests.length; i++) {
			dispatches[i] = queryDispatch(requests[i].FeatureURL, requests[i].FrameName, requests[i].SearchFlags);
		}
		return dispatches;
	}

	// -------------------------------------------------------------------------
	// XDispatch

	@Override
	public void dispatch(URL url, PropertyValue[] args) {
		String command = url.Path;
		try {
			ProcessBox.from().visible().clearWennNotRunning().info("Start " + command);
			WorkingSpreadsheet ws = new WorkingSpreadsheet(xContext);
			switch (command) {
			// ------------------------------
			// SuperMelee
			case "neue_meldeliste":
				new MeldeListeSheet_New(ws).start();
				break;
			case "update_meldeliste":
				new MeldeListeSheet_Update(ws).testTurnierVorhanden().start();
				break;
			case "anmeldungen":
				new AnmeldungenSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case "teilnehmer":
				new TielnehmerSheet(ws).testTurnierVorhanden().start();
				break;
			case "naechste_spieltag":
				new MeldeListeSheet_NeuerSpieltag(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case "meldeliste_testdaten":
				new MeldeListeSheet_TestDaten(ws).start();
				break;
			case "supermelee_teampaarungen":
				new SupermeleeTeamPaarungenSheet(ws).testTurnierVorhanden().start();
				break;
			case "aktuelle_spielrunde":
				new SpielrundeSheet_Update(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
				break;
			case "naechste_spielrunde":
				new SpielrundeSheet_Naechste(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
				break;
			case "super_spielrundeplan":
				new SpielrundePlan(ws).testTurnierVorhanden().start();
				break;
			case "spielrunden_testdaten":
				new SpielrundeSheet_TestDaten(ws).start();
				break;
			case "spieltag_rangliste":
				new SpieltagRanglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case "spieltag_rangliste_sort":
				new SpieltagRanglisteSheet_SortOnly(ws).testTurnierVorhanden().start();
				break;
			case "SpieltagRanglisteSheet_TestDaten":
				new SpieltagRanglisteSheet_TestDaten(ws).start();
				break;
			case "supermelee_endrangliste":
				new EndranglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case "supermelee_endrangliste_sort":
				new EndranglisteSheet_Sort(ws).testTurnierVorhanden().start();
				break;
			case "supermelee_validate":
				new SpielrundeSheet_Validator(ws).testTurnierVorhanden().start();
				break;
			case "supermelee_spieltagrangliste_validate":
				new SpieltagRangliste_Validator(ws).testTurnierVorhanden().start();
				break;
			// ------------------------------
			// Liga
			case "liga_neue_meldeliste":
				new LigaMeldeListeSheetNew(ws).start();
				break;
			case "liga_update_meldeliste":
				new LigaMeldeListeSheetUpdate(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case "liga_testdaten_meldeliste":
				new LigaMeldeListeSheetTestDaten(ws, true).start();
				break;
			case "liga_spielplan":
				new LigaSpielPlanSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
				break;
			case "liga_rangliste":
				new LigaRanglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case "liga_rangliste_sortieren":
				new LigaRanglisteSheetSortOnly(ws).testTurnierVorhanden().start();
				break;
			case "liga_direktvergleich":
				new LigaRanglisteDirektvergleichSheet(ws).testTurnierVorhanden().start();
				break;
			case "liga_spielplan_testdaten":
				new LigaSpielPlanSheetTestDaten(ws, false).start();
				break;
			case "liga_spielplan_testdaten_mit_freispiel":
				new LigaSpielPlanSheetTestDaten(ws, true).start();
				break;
			case "liga_export":
				new LigaMeldeListeSheetExport(ws).testTurnierVorhanden().start();
				break;
			// ------------------------------
			// Jeder gegen Jeden
			case "jgj_neue_meldeliste":
				new JGJMeldeListeSheet_New(ws).start();
				break;
			case "jgj_update_meldeliste":
				new JGJMeldeListeSheet_Update(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case "jgj_spielplan":
				new JGJSpielPlanSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
				break;
			case "jgj_rangliste":
				new JGJRanglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case "jgj_rangliste_sortieren":
				new JGJRanglisteSheetSortOnly(ws).testTurnierVorhanden().start();
				break;
			case "jgj_direktvergleich":
				new JGJRanglisteDirektvergleichSheet(ws).testTurnierVorhanden().start();
				break;
			// ------------------------------
			// Schweizer System
			case "schweizer_start":
				new SchweizerMeldeListeSheetNew(ws).start();
				break;
			case "schweizer_neue_meldeliste":
				if (new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument() != TurnierSystem.SCHWEIZER) {
					MessageBox.from(ws.getxContext(), MessageBoxTypeEnum.ERROR_OK)
							.caption("Kein Schweizer-Turnier-Dokument")
							.message("Kein Schweizer-Turnier vorhanden").show();
				} else {
					new SchweizerMeldeListeSheetNew(ws).start();
				}
				break;
			case "schweizer_update_meldeliste":
				new SchweizerMeldeListeSheetUpdate(ws).start();
				break;
			case "schweizer_testdaten_meldeliste":
				new SchweizerMeldeListeSheetTestDaten(ws).start();
				break;
			// ------------------------------
			// Konfiguration
			case "konfiguration_turnier":
				handleKonfiguration(command, ws);
				break;
			case "konfiguration_kopffusszeilen":
				handleKonfiguration(command, ws);
				break;
			case "konfiguration_farben":
				handleKonfiguration(command, ws);
				break;
			case "konfiguration_update_erstellt_mit_version":
				handleKonfiguration(command, ws);
				break;
			// ------------------------------
			// Download / Stop
			case "downloadExtension":
				new DownloadExtension(ws).start();
				break;
			case "abbruch":
				SheetRunner.cancelRunner();
				break;
			default:
				ProcessBox.from().fehler("ungueltige Aktion " + command);
				logger.warn("Unbekannter Befehl: {}", command);
			}
		} catch (Exception e) {
			ProcessBox.from().fehler(e.getMessage());
			logger.error("Fehler beim Ausführen von '{}': {}", command, e.getMessage(), e);
		}
	}

	private void handleKonfiguration(String command, WorkingSpreadsheet ws) {
		TurnierSystem ts;
		try {
			ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}
		if (ts == TurnierSystem.KEIN) {
			try {
				MessageBox.from(ws.getxContext(), MessageBoxTypeEnum.ERROR_OK).caption("Konfiguration")
						.message("Kein Turnier vorhanden").show();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			return;
		}
		try {
			switch (command) {
			case "konfiguration_turnier":
				new TurnierDialog(ws).createDialog();
				break;
			case "konfiguration_kopffusszeilen":
				new KopfFusszeilenDialog(ws).createDialog();
				break;
			case "konfiguration_farben":
				new FarbenDialog(ws).createDialog();
				break;
			case "konfiguration_update_erstellt_mit_version":
				DocumentHelper.setDocErstelltMitVersion(ws);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void addStatusListener(XStatusListener listener, URL url) {
		String command = url.Path;
		STATUS_LISTENERS.computeIfAbsent(command, k -> Collections.synchronizedList(new ArrayList<>()))
				.add(new StatusEntry(listener, url));
		postStatus(listener, url, isEnabled(command));
	}

	@Override
	public void removeStatusListener(XStatusListener listener, URL url) {
		String command = url.Path;
		List<StatusEntry> list = STATUS_LISTENERS.get(command);
		if (list != null) {
			list.removeIf(e -> e.listener == listener);
		}
	}

	// -------------------------------------------------------------------------
	// Zustandsprüfung und Listener-Benachrichtigung (statisch, cross-instance)

	private static boolean isEnabled(String command) {
		if (SheetRunner.isRunning()) {
			return "abbruch".equals(command);
		}
		XComponentContext ctx = SHARED_CONTEXT;
		if (ctx == null) {
			return false;
		}
		try {
			TurnierSystem ts = new DocumentPropertiesHelper(new WorkingSpreadsheet(ctx))
					.getTurnierSystemAusDocument();
			return switch (command) {
			// SuperMelee: neues Turnier nur wenn keins aktiv
			case "neue_meldeliste"                        -> ts == TurnierSystem.KEIN;
			// SuperMelee-Aktionen: nur wenn SuperMelee aktiv
			case "update_meldeliste",
				 "anmeldungen", "teilnehmer",
				 "naechste_spieltag", "aktuelle_spielrunde",
				 "naechste_spielrunde", "super_spielrundeplan",
				 "spieltag_rangliste", "spieltag_rangliste_sort",
				 "supermelee_endrangliste", "supermelee_endrangliste_sort",
				 "supermelee_teampaarungen",
				 "supermelee_validate", "supermelee_spieltagrangliste_validate" -> ts == TurnierSystem.SUPERMELEE;
			// SuperMelee-Testdaten: auch wenn kein Turnier vorhanden
			case "meldeliste_testdaten", "spielrunden_testdaten",
				 "SpieltagRanglisteSheet_TestDaten"        -> ts == TurnierSystem.KEIN || ts == TurnierSystem.SUPERMELEE;
			// Liga
			case "liga_neue_meldeliste"                   -> ts == TurnierSystem.KEIN;
			case "liga_update_meldeliste", "liga_spielplan",
				 "liga_rangliste", "liga_rangliste_sortieren",
				 "liga_direktvergleich", "liga_export"    -> ts == TurnierSystem.LIGA;
			// Liga-Testdaten: auch wenn kein Turnier vorhanden
			case "liga_testdaten_meldeliste",
				 "liga_spielplan_testdaten",
				 "liga_spielplan_testdaten_mit_freispiel" -> ts == TurnierSystem.KEIN || ts == TurnierSystem.LIGA;
			// Jeder gegen Jeden
			case "jgj_neue_meldeliste"                    -> ts == TurnierSystem.KEIN;
			case "jgj_update_meldeliste", "jgj_spielplan",
				 "jgj_rangliste", "jgj_rangliste_sortieren",
				 "jgj_direktvergleich"                    -> ts == TurnierSystem.JGJ;
			// Schweizer
			case "schweizer_start"                        -> ts == TurnierSystem.KEIN;
			case "schweizer_neue_meldeliste",
				 "schweizer_update_meldeliste"            -> ts == TurnierSystem.SCHWEIZER;
			// Schweizer-Testdaten: auch wenn kein Turnier vorhanden
			case "schweizer_testdaten_meldeliste"         -> ts == TurnierSystem.KEIN || ts == TurnierSystem.SCHWEIZER;
			// Konfiguration: nur wenn Turnier vorhanden
			case "konfiguration_turnier",
				 "konfiguration_kopffusszeilen",
				 "konfiguration_farben",
				 "konfiguration_update_erstellt_mit_version" -> ts != TurnierSystem.KEIN;
			// Download, Stop: immer aktiv
			case "downloadExtension", "abbruch"           -> true;
			default -> false;
			};
		} catch (Exception e) {
			return false;
		}
	}

	private static void notifyAllListeners() {
		Map<String, List<StatusEntry>> snapshot;
		synchronized (STATUS_LISTENERS) {
			snapshot = new HashMap<>(STATUS_LISTENERS);
		}
		for (Map.Entry<String, List<StatusEntry>> entry : snapshot.entrySet()) {
			boolean enabled = isEnabled(entry.getKey());
			for (StatusEntry e : new ArrayList<>(entry.getValue())) {
				postStatus(e.listener, e.url, enabled);
			}
		}
	}

	private static void postStatus(XStatusListener listener, URL url, boolean enabled) {
		try {
			FeatureStateEvent event = new FeatureStateEvent();
			event.FeatureURL = url;
			event.IsEnabled = enabled;
			event.Requery = false;
			listener.statusChanged(event);
		} catch (Exception e) {
			logger.warn("Fehler beim Benachrichtigen des Status-Listeners: {}", e.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// XServiceInfo

	@Override
	public String getImplementationName() {
		return IMPLEMENTATION_NAME;
	}

	@Override
	public boolean supportsService(String serviceName) {
		return Arrays.asList(SERVICE_NAMES).contains(serviceName);
	}

	@Override
	public String[] getSupportedServiceNames() {
		return SERVICE_NAMES;
	}

	// -------------------------------------------------------------------------
	// UNO Factory

	public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
		return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME, SERVICE_NAMES, xRegistryKey);
	}

	public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
		if (sImplementationName.equals(IMPLEMENTATION_NAME)) {
			return Factory.createComponentFactory(ProtocolHandler.class, SERVICE_NAMES);
		}
		return null;
	}

	// -------------------------------------------------------------------------

	private record StatusEntry(XStatusListener listener, URL url) {
	}
}
