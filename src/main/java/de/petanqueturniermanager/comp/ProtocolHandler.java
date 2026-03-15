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
import de.petanqueturniermanager.comp.newrelease.DirectUpdate;
import de.petanqueturniermanager.comp.newrelease.DownloadExtension;
import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;
import de.petanqueturniermanager.comp.newrelease.ReleaseInfosAnzeigen;
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
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetSortOnly;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetUpdate;
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

	// Command-Konstanten
	// SuperMelee
	public static final String CMD_NEUE_MELDELISTE = "neue_meldeliste";
	public static final String CMD_UPDATE_MELDELISTE = "update_meldeliste";
	public static final String CMD_ANMELDUNGEN = "anmeldungen";
	public static final String CMD_TEILNEHMER = "teilnehmer";
	public static final String CMD_NAECHSTE_SPIELTAG = "naechste_spieltag";
	public static final String CMD_AKTUELLE_SPIELRUNDE = "aktuelle_spielrunde";
	public static final String CMD_NAECHSTE_SPIELRUNDE = "naechste_spielrunde";
	public static final String CMD_SUPER_SPIELRUNDEPLAN = "super_spielrundeplan";
	public static final String CMD_SPIELTAG_RANGLISTE_SORT = "spieltag_rangliste_sort";
	public static final String CMD_SPIELTAG_RANGLISTE = "spieltag_rangliste";
	public static final String CMD_SUPERMELEE_ENDRANGLISTE_SORT = "supermelee_endrangliste_sort";
	public static final String CMD_SUPERMELEE_ENDRANGLISTE = "supermelee_endrangliste";
	public static final String CMD_SUPERMELEE_TEAMPAARUNGEN = "supermelee_teampaarungen";
	public static final String CMD_SUPERMELEE_SPIELTAGRANGLISTE_VALIDATE = "supermelee_spieltagrangliste_validate";
	public static final String CMD_SUPERMELEE_VALIDATE = "supermelee_validate";
	public static final String CMD_MELDELISTE_TESTDATEN = "meldeliste_testdaten";
	public static final String CMD_SPIELRUNDEN_TESTDATEN = "spielrunden_testdaten";
	public static final String CMD_SPIELTAGRANGLISTE_TESTDATEN = "SpieltagRanglisteSheet_TestDaten";
	// Liga
	public static final String CMD_LIGA_NEUE_MELDELISTE = "liga_neue_meldeliste";
	public static final String CMD_LIGA_UPDATE_MELDELISTE = "liga_update_meldeliste";
	public static final String CMD_LIGA_SPIELPLAN_TESTDATEN_MIT_FREISPIEL = "liga_spielplan_testdaten_mit_freispiel";
	public static final String CMD_LIGA_SPIELPLAN_TESTDATEN = "liga_spielplan_testdaten";
	public static final String CMD_LIGA_SPIELPLAN = "liga_spielplan";
	public static final String CMD_LIGA_RANGLISTE_SORTIEREN = "liga_rangliste_sortieren";
	public static final String CMD_LIGA_RANGLISTE = "liga_rangliste";
	public static final String CMD_LIGA_DIREKTVERGLEICH = "liga_direktvergleich";
	public static final String CMD_LIGA_EXPORT = "liga_export";
	public static final String CMD_LIGA_TESTDATEN_MELDELISTE = "liga_testdaten_meldeliste";
	// JGJ
	public static final String CMD_JGJ_NEUE_MELDELISTE = "jgj_neue_meldeliste";
	public static final String CMD_JGJ_UPDATE_MELDELISTE = "jgj_update_meldeliste";
	public static final String CMD_JGJ_SPIELPLAN = "jgj_spielplan";
	public static final String CMD_JGJ_RANGLISTE_SORTIEREN = "jgj_rangliste_sortieren";
	public static final String CMD_JGJ_RANGLISTE = "jgj_rangliste";
	public static final String CMD_JGJ_DIREKTVERGLEICH = "jgj_direktvergleich";
	// Schweizer
	public static final String CMD_SCHWEIZER_START = "schweizer_start";
	public static final String CMD_SCHWEIZER_NEUE_MELDELISTE = "schweizer_neue_meldeliste";
	public static final String CMD_SCHWEIZER_UPDATE_MELDELISTE = "schweizer_update_meldeliste";
	public static final String CMD_SCHWEIZER_AKTUELLE_SPIELRUNDE = "schweizer_aktuelle_spielrunde";
	public static final String CMD_SCHWEIZER_NAECHSTE_SPIELRUNDE = "schweizer_naechste_spielrunde";
	public static final String CMD_SCHWEIZER_RANGLISTE = "schweizer_rangliste";
	public static final String CMD_SCHWEIZER_RANGLISTE_SORTIEREN = "schweizer_rangliste_sortieren";
	public static final String CMD_SCHWEIZER_TESTDATEN_MELDELISTE = "schweizer_testdaten_meldeliste";
	public static final String CMD_SCHWEIZER_TESTDATEN_TURNIER = "schweizer_testdaten_turnier";
	// Konfiguration
	public static final String CMD_KONFIGURATION_TURNIER = "konfiguration_turnier";
	public static final String CMD_KONFIGURATION_KOPFFUSSZEILEN = "konfiguration_kopffusszeilen";
	public static final String CMD_KONFIGURATION_FARBEN = "konfiguration_farben";
	public static final String CMD_KONFIGURATION_UPDATE_ERSTELLT_MIT_VERSION = "konfiguration_update_erstellt_mit_version";
	// Sonstige
	public static final String CMD_DOWNLOAD_EXTENSION = "downloadExtension";
	public static final String CMD_ABBRUCH = "abbruch";
	// Neue Version
	public static final String CMD_NEUE_VERSION_MENUE    = "neueVersionMenue";
	public static final String CMD_RELEASE_INFOS_ANZEIGEN = "releaseInfosAnzeigen";
	public static final String CMD_DIREKT_AKTUALISIEREN  = "direktAktualisieren";

	private final XComponentContext xContext;

	public ProtocolHandler(XComponentContext xContext) {
		this.xContext = xContext;
		SHARED_CONTEXT = xContext;
		PetanqueTurnierMngrSingleton.init(xContext);
		if (REGISTERED.compareAndSet(false, true)) {
			NewReleaseChecker.addCacheUpdateCallback(ProtocolHandler::notifyAllListeners);
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
			case CMD_NEUE_MELDELISTE:
				new MeldeListeSheet_New(ws).start();
				break;
			case CMD_UPDATE_MELDELISTE:
				new MeldeListeSheet_Update(ws).testTurnierVorhanden().start();
				break;
			case CMD_ANMELDUNGEN:
				new AnmeldungenSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_TEILNEHMER:
				new TielnehmerSheet(ws).testTurnierVorhanden().start();
				break;
			case CMD_NAECHSTE_SPIELTAG:
				new MeldeListeSheet_NeuerSpieltag(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_MELDELISTE_TESTDATEN:
				new MeldeListeSheet_TestDaten(ws).start();
				break;
			case CMD_SUPERMELEE_TEAMPAARUNGEN:
				new SupermeleeTeamPaarungenSheet(ws).testTurnierVorhanden().start();
				break;
			case CMD_AKTUELLE_SPIELRUNDE:
				new SpielrundeSheet_Update(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_NAECHSTE_SPIELRUNDE:
				new SpielrundeSheet_Naechste(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_SUPER_SPIELRUNDEPLAN:
				new SpielrundePlan(ws).testTurnierVorhanden().start();
				break;
			case CMD_SPIELRUNDEN_TESTDATEN:
				new SpielrundeSheet_TestDaten(ws).start();
				break;
			case CMD_SPIELTAG_RANGLISTE:
				new SpieltagRanglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_SPIELTAG_RANGLISTE_SORT:
				new SpieltagRanglisteSheet_SortOnly(ws).testTurnierVorhanden().start();
				break;
			case CMD_SPIELTAGRANGLISTE_TESTDATEN:
				new SpieltagRanglisteSheet_TestDaten(ws).start();
				break;
			case CMD_SUPERMELEE_ENDRANGLISTE:
				new EndranglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_SUPERMELEE_ENDRANGLISTE_SORT:
				new EndranglisteSheet_Sort(ws).testTurnierVorhanden().start();
				break;
			case CMD_SUPERMELEE_VALIDATE:
				new SpielrundeSheet_Validator(ws).testTurnierVorhanden().start();
				break;
			case CMD_SUPERMELEE_SPIELTAGRANGLISTE_VALIDATE:
				new SpieltagRangliste_Validator(ws).testTurnierVorhanden().start();
				break;
			// ------------------------------
			// Liga
			case CMD_LIGA_NEUE_MELDELISTE:
				new LigaMeldeListeSheetNew(ws).start();
				break;
			case CMD_LIGA_UPDATE_MELDELISTE:
				new LigaMeldeListeSheetUpdate(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_LIGA_TESTDATEN_MELDELISTE:
				new LigaMeldeListeSheetTestDaten(ws, true).start();
				break;
			case CMD_LIGA_SPIELPLAN:
				new LigaSpielPlanSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_LIGA_RANGLISTE:
				new LigaRanglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_LIGA_RANGLISTE_SORTIEREN:
				new LigaRanglisteSheetSortOnly(ws).testTurnierVorhanden().start();
				break;
			case CMD_LIGA_DIREKTVERGLEICH:
				new LigaRanglisteDirektvergleichSheet(ws).testTurnierVorhanden().start();
				break;
			case CMD_LIGA_SPIELPLAN_TESTDATEN:
				new LigaSpielPlanSheetTestDaten(ws, false).start();
				break;
			case CMD_LIGA_SPIELPLAN_TESTDATEN_MIT_FREISPIEL:
				new LigaSpielPlanSheetTestDaten(ws, true).start();
				break;
			case CMD_LIGA_EXPORT:
				new LigaMeldeListeSheetExport(ws).testTurnierVorhanden().start();
				break;
			// ------------------------------
			// Jeder gegen Jeden
			case CMD_JGJ_NEUE_MELDELISTE:
				new JGJMeldeListeSheet_New(ws).start();
				break;
			case CMD_JGJ_UPDATE_MELDELISTE:
				new JGJMeldeListeSheet_Update(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_JGJ_SPIELPLAN:
				new JGJSpielPlanSheet(ws).testTurnierVorhanden().backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_JGJ_RANGLISTE:
				new JGJRanglisteSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_JGJ_RANGLISTE_SORTIEREN:
				new JGJRanglisteSheetSortOnly(ws).testTurnierVorhanden().start();
				break;
			case CMD_JGJ_DIREKTVERGLEICH:
				new JGJRanglisteDirektvergleichSheet(ws).testTurnierVorhanden().start();
				break;
			// ------------------------------
			// Schweizer System
			case CMD_SCHWEIZER_START:
				new SchweizerMeldeListeSheetNew(ws).start();
				break;
			case CMD_SCHWEIZER_NEUE_MELDELISTE:
				if (new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument() != TurnierSystem.SCHWEIZER) {
					MessageBox.from(ws.getxContext(), MessageBoxTypeEnum.ERROR_OK)
							.caption("Kein Schweizer-Turnier-Dokument")
							.message("Kein Schweizer-Turnier vorhanden").show();
				} else {
					new SchweizerMeldeListeSheetNew(ws).start();
				}
				break;
			case CMD_SCHWEIZER_UPDATE_MELDELISTE:
				new SchweizerMeldeListeSheetUpdate(ws).start();
				break;
			case CMD_SCHWEIZER_AKTUELLE_SPIELRUNDE:
				new SchweizerSpielrundeSheetUpdate(ws).start();
				break;
			case CMD_SCHWEIZER_NAECHSTE_SPIELRUNDE:
				new SchweizerSpielrundeSheetNaechste(ws).start();
				break;
			case CMD_SCHWEIZER_RANGLISTE:
				new SchweizerRanglisteSheet(ws).testTurnierVorhanden().start();
				break;
			case CMD_SCHWEIZER_RANGLISTE_SORTIEREN:
				new SchweizerRanglisteSheetSortOnly(ws).testTurnierVorhanden().start();
				break;
			case CMD_SCHWEIZER_TESTDATEN_MELDELISTE:
				new SchweizerMeldeListeSheetTestDaten(ws).start();
				break;
			case CMD_SCHWEIZER_TESTDATEN_TURNIER:
				new SchweizerTurnierTestDaten(ws).start();
				break;
			// ------------------------------
			// Konfiguration
			case CMD_KONFIGURATION_TURNIER:
				handleKonfiguration(command, ws);
				break;
			case CMD_KONFIGURATION_KOPFFUSSZEILEN:
				handleKonfiguration(command, ws);
				break;
			case CMD_KONFIGURATION_FARBEN:
				handleKonfiguration(command, ws);
				break;
			case CMD_KONFIGURATION_UPDATE_ERSTELLT_MIT_VERSION:
				handleKonfiguration(command, ws);
				break;
			// ------------------------------
			// Download / Stop / Neue Version
			case CMD_DOWNLOAD_EXTENSION:
				new DownloadExtension(ws).start();
				break;
			case CMD_RELEASE_INFOS_ANZEIGEN:
				new ReleaseInfosAnzeigen(ws).start();
				break;
			case CMD_DIREKT_AKTUALISIEREN:
				new DirectUpdate(ws).start();
				break;
			case CMD_ABBRUCH:
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
			case CMD_KONFIGURATION_TURNIER:
				new TurnierDialog(ws).createDialog();
				break;
			case CMD_KONFIGURATION_KOPFFUSSZEILEN:
				new KopfFusszeilenDialog(ws).createDialog();
				break;
			case CMD_KONFIGURATION_FARBEN:
				new FarbenDialog(ws).createDialog();
				break;
			case CMD_KONFIGURATION_UPDATE_ERSTELLT_MIT_VERSION:
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
			return CMD_ABBRUCH.equals(command);
		}
		XComponentContext ctx = SHARED_CONTEXT;
		if (ctx == null) {
			return false;
		}
		try {
			WorkingSpreadsheet ws = new WorkingSpreadsheet(ctx);
			TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
			return switch (command) {
			// SuperMelee: neues Turnier nur wenn keins aktiv
			case CMD_NEUE_MELDELISTE                        -> ts == TurnierSystem.KEIN;
			// SuperMelee-Aktionen: nur wenn SuperMelee aktiv
			case CMD_UPDATE_MELDELISTE,
				 CMD_ANMELDUNGEN, CMD_TEILNEHMER,
				 CMD_NAECHSTE_SPIELTAG,
				 CMD_NAECHSTE_SPIELRUNDE, CMD_SUPER_SPIELRUNDEPLAN,
				 CMD_SPIELTAG_RANGLISTE, CMD_SPIELTAG_RANGLISTE_SORT,
				 CMD_SUPERMELEE_ENDRANGLISTE, CMD_SUPERMELEE_ENDRANGLISTE_SORT,
				 CMD_SUPERMELEE_TEAMPAARUNGEN,
				 CMD_SUPERMELEE_VALIDATE, CMD_SUPERMELEE_SPIELTAGRANGLISTE_VALIDATE -> ts == TurnierSystem.SUPERMELEE;
			// "Spielrunde neu auslosen": nur wenn SuperMelee aktiv UND mind. 1 Spielrunde vorhanden
			case CMD_AKTUELLE_SPIELRUNDE                    -> ts == TurnierSystem.SUPERMELEE && hatSupermeleeSpielrunde(ws);
			// SuperMelee-Testdaten: auch wenn kein Turnier vorhanden
			case CMD_MELDELISTE_TESTDATEN, CMD_SPIELRUNDEN_TESTDATEN,
				 CMD_SPIELTAGRANGLISTE_TESTDATEN        -> ts == TurnierSystem.KEIN || ts == TurnierSystem.SUPERMELEE;
			// Liga
			case CMD_LIGA_NEUE_MELDELISTE                   -> ts == TurnierSystem.KEIN;
			case CMD_LIGA_UPDATE_MELDELISTE, CMD_LIGA_SPIELPLAN,
				 CMD_LIGA_RANGLISTE, CMD_LIGA_RANGLISTE_SORTIEREN,
				 CMD_LIGA_DIREKTVERGLEICH, CMD_LIGA_EXPORT    -> ts == TurnierSystem.LIGA;
			// Liga-Testdaten: auch wenn kein Turnier vorhanden
			case CMD_LIGA_TESTDATEN_MELDELISTE,
				 CMD_LIGA_SPIELPLAN_TESTDATEN,
				 CMD_LIGA_SPIELPLAN_TESTDATEN_MIT_FREISPIEL -> ts == TurnierSystem.KEIN || ts == TurnierSystem.LIGA;
			// Jeder gegen Jeden
			case CMD_JGJ_NEUE_MELDELISTE                    -> ts == TurnierSystem.KEIN;
			case CMD_JGJ_UPDATE_MELDELISTE, CMD_JGJ_SPIELPLAN,
				 CMD_JGJ_RANGLISTE, CMD_JGJ_RANGLISTE_SORTIEREN,
				 CMD_JGJ_DIREKTVERGLEICH                    -> ts == TurnierSystem.JGJ;
			// Schweizer
			case CMD_SCHWEIZER_START                        -> ts == TurnierSystem.KEIN;
			case CMD_SCHWEIZER_NEUE_MELDELISTE,
				 CMD_SCHWEIZER_UPDATE_MELDELISTE,
				 CMD_SCHWEIZER_NAECHSTE_SPIELRUNDE,
				 CMD_SCHWEIZER_RANGLISTE,
				 CMD_SCHWEIZER_RANGLISTE_SORTIEREN          -> ts == TurnierSystem.SCHWEIZER;
			// "Spielrunde neu auslosen": nur wenn Schweizer aktiv UND mind. 1 Spielrunde vorhanden
			case CMD_SCHWEIZER_AKTUELLE_SPIELRUNDE          -> ts == TurnierSystem.SCHWEIZER && hatSchweizerSpielrunde(ws);
			// Schweizer-Testdaten: auch wenn kein Turnier vorhanden
			case CMD_SCHWEIZER_TESTDATEN_MELDELISTE,
			 CMD_SCHWEIZER_TESTDATEN_TURNIER             -> ts == TurnierSystem.KEIN || ts == TurnierSystem.SCHWEIZER;
			// Konfiguration: nur wenn Turnier vorhanden
			case CMD_KONFIGURATION_TURNIER,
				 CMD_KONFIGURATION_KOPFFUSSZEILEN,
				 CMD_KONFIGURATION_FARBEN,
				 CMD_KONFIGURATION_UPDATE_ERSTELLT_MIT_VERSION -> ts != TurnierSystem.KEIN;
			// Neue Version: nur aktiv wenn neue Version verfügbar
			case CMD_NEUE_VERSION_MENUE,
				 CMD_RELEASE_INFOS_ANZEIGEN,
				 CMD_DIREKT_AKTUALISIEREN                   -> new NewReleaseChecker().checkForNewRelease(ctx);
			// Download, Stop: immer aktiv
			case CMD_DOWNLOAD_EXTENSION, CMD_ABBRUCH           -> true;
			default -> false;
			};
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Prüft ob mindestens eine SuperMêlée-Spielrunde vorhanden ist.
	 * Spielrunden-Sheets heißen "{spieltag}.{runde}. Spielrunde", z.B. "1.1. Spielrunde".
	 */
	private static boolean hatSupermeleeSpielrunde(WorkingSpreadsheet ws) {
		try {
			var doc = ws.getWorkingSpreadsheetDocument();
			if (doc == null) {
				return false;
			}
			for (var name : doc.getSheets().getElementNames()) {
				if (name.matches("\\d+\\.\\d+\\. Spielrunde")) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Prüft ob mindestens eine Schweizer-Spielrunde vorhanden ist.
	 * Spielrunden-Sheets heißen "{runde}. Spielrunde", z.B. "1. Spielrunde".
	 */
	private static boolean hatSchweizerSpielrunde(WorkingSpreadsheet ws) {
		try {
			var doc = ws.getWorkingSpreadsheetDocument();
			if (doc == null) {
				return false;
			}
			return doc.getSheets().hasByName("1. Spielrunde");
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
			if (CMD_NEUE_VERSION_MENUE.equals(url.Path) && enabled) {
				event.State = new NewReleaseChecker().getMenuTitelKurz();
			}
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
