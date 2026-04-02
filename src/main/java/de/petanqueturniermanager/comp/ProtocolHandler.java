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
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.util.URL;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.webserver.WebServerManager;
import de.petanqueturniermanager.webserver.WebserverKonfigDialog;
import de.petanqueturniermanager.comp.newrelease.DirectUpdate;
import de.petanqueturniermanager.comp.newrelease.DownloadExtension;
import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;
import de.petanqueturniermanager.comp.newrelease.ReleaseInfosAnzeigen;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.ko.KoTurnierTestDaten;
import de.petanqueturniermanager.ko.KoTurnierbaumSheet;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetNew;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetTestDaten;
import de.petanqueturniermanager.ko.meldeliste.KoMeldeListeSheetUpdate;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetSortOnly;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJTurnierTestDaten;
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
import de.petanqueturniermanager.maastrichter.MaastrichterTurnierTestDaten;
import de.petanqueturniermanager.maastrichter.finalrunde.MaastrichterFinalrundeSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetNew;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterTeilnehmerSheet;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetUpdate;
import de.petanqueturniermanager.poule.PouleTurnierTestDaten;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetNew;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetTestDaten;
import de.petanqueturniermanager.poule.meldeliste.PouleMeldeListeSheetUpdate;
import de.petanqueturniermanager.poule.meldeliste.PouleTeilnehmerSheet;
import de.petanqueturniermanager.poule.ko.PouleKoSheet;
import de.petanqueturniermanager.poule.rangliste.PouleVorrundenRanglisteSheet;
import de.petanqueturniermanager.poule.vorrunde.PouleSpielplaeneSheet;
import de.petanqueturniermanager.poule.vorrunde.PouleVorrundeSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetSortOnly;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
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
import de.petanqueturniermanager.supermelee.meldeliste.TeilnehmerSheet;
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
	public static final String CMD_JGJ_TESTDATEN_TURNIER = "jgj_testdaten_turnier";
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
	public static final String CMD_SCHWEIZER_TESTDATEN_TURNIER_19 = "schweizer_testdaten_turnier_19";
	// Maastrichter
	public static final String CMD_MAASTRICHTER_START = "maastrichter_start";
	public static final String CMD_MAASTRICHTER_UPDATE_MELDELISTE = "maastrichter_update_meldeliste";
	public static final String CMD_MAASTRICHTER_NAECHSTE_VORRUNDE = "maastrichter_naechste_vorrunde";
	public static final String CMD_MAASTRICHTER_AKTUELLE_VORRUNDE = "maastrichter_aktuelle_vorrunde";
	public static final String CMD_MAASTRICHTER_VORRUNDEN_RANGLISTE = "maastrichter_vorrunden_rangliste";
	public static final String CMD_MAASTRICHTER_FINALRUNDEN = "maastrichter_finalrunden";
	public static final String CMD_MAASTRICHTER_TEILNEHMER = "maastrichter_teilnehmer";
	public static final String CMD_MAASTRICHTER_TESTDATEN_TURNIER = "maastrichter_testdaten_turnier";
	public static final String CMD_MAASTRICHTER_TESTDATEN_TURNIER_57 = "maastrichter_testdaten_turnier_57";
	public static final String CMD_MAASTRICHTER_TESTDATEN_TURNIER_35 = "maastrichter_testdaten_turnier_35";
	// K.-O.
	public static final String CMD_KO_START = "ko_start";
	public static final String CMD_KO_UPDATE_MELDELISTE = "ko_update_meldeliste";
	public static final String CMD_KO_TURNIERBAUM = "ko_turnierbaum";
	public static final String CMD_KO_TEILNEHMER = "ko_teilnehmer";
	public static final String CMD_KO_TESTDATEN_NUR_MELDELISTE = "ko_testdaten_nur_meldeliste";
	public static final String CMD_KO_TESTDATEN_8_TEAMS = "ko_testdaten_8_teams";
	public static final String CMD_KO_TESTDATEN_16_TEAMS = "ko_testdaten_16_teams";
	public static final String CMD_KO_TESTDATEN_CADRAGE = "ko_testdaten_cadrage";
	// Teilnehmer
	public static final String CMD_SCHWEIZER_TEILNEHMER = "schweizer_teilnehmer";
	public static final String CMD_JGJ_TEILNEHMER       = "jgj_teilnehmer";
	// Poule A/B
	public static final String CMD_POULE_START               = "poule_start";
	public static final String CMD_POULE_NEUE_MELDELISTE     = "poule_neue_meldeliste";
	public static final String CMD_POULE_UPDATE_MELDELISTE   = "poule_update_meldeliste";
	public static final String CMD_POULE_TEILNEHMER          = "poule_teilnehmer";
	public static final String CMD_POULE_TESTDATEN_MELDELISTE = "poule_testdaten_meldeliste";
	public static final String CMD_POULE_VORRUNDE              = "poule_vorrunde";
	public static final String CMD_POULE_SPIELPLAENE           = "poule_spielplaene";
	public static final String CMD_POULE_VORRUNDEN_RANGLISTE   = "poule_vorrunden_rangliste";
	public static final String CMD_POULE_KO                    = "poule_ko";
	public static final String CMD_POULE_TESTDATEN_TURNIER     = "poule_testdaten_turnier";
	// Webserver
	public static final String CMD_WEBSERVER_KONFIGURATION = "webserver_konfiguration";
	public static final String CMD_WEBSERVER_STARTEN = "webserver_starten";
	public static final String CMD_WEBSERVER_STOPPEN = "webserver_stoppen";
	public static final String CMD_WEBSERVER_URL_1  = "webserver_url_1";
	public static final String CMD_WEBSERVER_URL_2  = "webserver_url_2";
	public static final String CMD_WEBSERVER_URL_3  = "webserver_url_3";
	public static final String CMD_WEBSERVER_URL_4  = "webserver_url_4";
	public static final String CMD_WEBSERVER_URL_5  = "webserver_url_5";
	public static final String CMD_WEBSERVER_URL_6  = "webserver_url_6";
	public static final String CMD_WEBSERVER_URL_7  = "webserver_url_7";
	public static final String CMD_WEBSERVER_URL_8  = "webserver_url_8";
	public static final String CMD_WEBSERVER_URL_9  = "webserver_url_9";
	public static final String CMD_WEBSERVER_URL_10 = "webserver_url_10";
	// Konfiguration
	public static final String CMD_KONFIGURATION_TURNIER = "konfiguration_turnier";
	public static final String CMD_KONFIGURATION_KOPFFUSSZEILEN = "konfiguration_kopffusszeilen";
	public static final String CMD_KONFIGURATION_FARBEN = "konfiguration_farben";
	public static final String CMD_KONFIGURATION_UPDATE_ERSTELLT_MIT_VERSION = "konfiguration_update_erstellt_mit_version";
	// Sonstige
	public static final String CMD_DOWNLOAD_EXTENSION = "downloadExtension";
	public static final String CMD_ABBRUCH = "abbruch";
	// Neue Version
	public static final String CMD_RELEASE_INFOS_ANZEIGEN = "releaseInfosAnzeigen";
	public static final String CMD_DIREKT_AKTUALISIEREN  = "direktAktualisieren";
	// Info
	public static final String CMD_LOGFILE_ANZEIGEN      = "logfileAnzeigen";
	public static final String CMD_PLUGIN_KONFIGURATION  = "pluginKonfiguration";
	public static final String CMD_PROCESSBOX_ANZEIGEN   = "processboxAnzeigen";

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

				@Override
				public void onUnload(Object source) {
					// WS stoppen wenn das Owner-Dokument geschlossen wird → andere Dokumente
					// können danach wieder starten
					var geschlossenesDoc = DocumentHelper.getCurrentSpreadsheetDocumentFrom(source);
					if (geschlossenesDoc != null && WebServerManager.get().istOwnerDocument(geschlossenesDoc)) {
						logger.info("Owner-Dokument geschlossen – WebServer wird gestoppt");
						WebServerManager.get().stoppen();
						notifyAllListeners();
					}
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
			// Webserver-Befehle laufen nicht im SheetRunner-Thread → direkt behandeln
			if (behandleWebserverBefehl(command)) {
				return;
			}
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
				new TeilnehmerSheet(ws).testTurnierVorhanden().start();
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
			case CMD_JGJ_TEILNEHMER:
				new TeilnehmerSheet(ws).testTurnierVorhanden().start();
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
			case CMD_JGJ_TESTDATEN_TURNIER:
				new JGJTurnierTestDaten(ws).start();
				break;
			// ------------------------------
			// Schweizer System
			case CMD_SCHWEIZER_START:
				new SchweizerMeldeListeSheetNew(ws).start();
				break;
			case CMD_SCHWEIZER_NEUE_MELDELISTE:
				if (new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument() != TurnierSystem.SCHWEIZER) {
					MessageBox.from(ws.getxContext(), MessageBoxTypeEnum.ERROR_OK)
							.caption(I18n.get("msg.caption.kein.schweizer"))
							.message(I18n.get("msg.text.kein.schweizer")).show();
				} else {
					new SchweizerMeldeListeSheetNew(ws).start();
				}
				break;
			case CMD_SCHWEIZER_UPDATE_MELDELISTE:
				new SchweizerMeldeListeSheetUpdate(ws).start();
				break;
			case CMD_SCHWEIZER_TEILNEHMER:
				new TeilnehmerSheet(ws).testTurnierVorhanden().start();
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
			case CMD_SCHWEIZER_TESTDATEN_TURNIER_19:
				// 19 Teams: ungerade → 1 Freilos pro Runde, Teamname in Spielrunde, Bahn Random
				new SchweizerTurnierTestDaten(ws, 19, SpielplanTeamAnzeige.NAME).start();
				break;
			// ------------------------------
			// Maastrichter System
			case CMD_MAASTRICHTER_START:
				new MaastrichterMeldeListeSheetNew(ws).start();
				break;
			case CMD_MAASTRICHTER_UPDATE_MELDELISTE:
				new MaastrichterMeldeListeSheetUpdate(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_MAASTRICHTER_NAECHSTE_VORRUNDE:
				new MaastrichterSpielrundeSheetNaechste(ws).testTurnierVorhanden().start();
				break;
			case CMD_MAASTRICHTER_AKTUELLE_VORRUNDE:
				new MaastrichterSpielrundeSheetUpdate(ws).testTurnierVorhanden().start();
				break;
			case CMD_MAASTRICHTER_VORRUNDEN_RANGLISTE:
				new MaastrichterVorrundenRanglisteSheet(ws).testTurnierVorhanden().start();
				break;
			case CMD_MAASTRICHTER_FINALRUNDEN:
				new MaastrichterFinalrundeSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_MAASTRICHTER_TEILNEHMER:
				new MaastrichterTeilnehmerSheet(ws).testTurnierVorhanden().start();
				break;
			case CMD_MAASTRICHTER_TESTDATEN_TURNIER:
				new MaastrichterTurnierTestDaten(ws).start();
				break;
			case CMD_MAASTRICHTER_TESTDATEN_TURNIER_57:
				// 57 Teams: 4 Vorrunden, gruppenGroesse=16, minRestGroesse=8
				// → GruppenAufteilungRechner ergibt [16,16,16,9] = 4 KO-Gruppen, D mit Cadrage
				// → 57 Teams ungerade → automatisch Freilos pro Vorrunde
				new MaastrichterTurnierTestDaten(ws, 57, 4, 16, 8).start();
				break;
			case CMD_MAASTRICHTER_TESTDATEN_TURNIER_35:
				// 35 Teams: 3 Vorrunden, gruppenGroesse=16, minRestGroesse=8
				// → GruppenAufteilungRechner ergibt [16,19] = Gruppe A (kein Cadrage) + Gruppe B (mit Cadrage)
				// → 35 Teams ungerade → automatisch Freilos pro Vorrunde
				new MaastrichterTurnierTestDaten(ws, 35, 3, 16, 8).start();
				break;
			// ------------------------------
			// K.-O.
			case CMD_KO_START:
				new KoMeldeListeSheetNew(ws).start();
				break;
			case CMD_KO_UPDATE_MELDELISTE:
				new KoMeldeListeSheetUpdate(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_KO_TEILNEHMER:
				new TeilnehmerSheet(ws).testTurnierVorhanden().start();
				break;
			case CMD_KO_TURNIERBAUM:
				new KoTurnierbaumSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_KO_TESTDATEN_NUR_MELDELISTE:
				new KoMeldeListeSheetTestDaten(ws, 8).start();
				break;
			case CMD_KO_TESTDATEN_8_TEAMS:
				new KoTurnierTestDaten(ws, 8).start();
				break;
			case CMD_KO_TESTDATEN_16_TEAMS:
				new KoTurnierTestDaten(ws, 16).start();
				break;
			case CMD_KO_TESTDATEN_CADRAGE:
				new KoTurnierTestDaten(ws, 10).start();
				break;
			// ------------------------------
			// Poule A/B
			case CMD_POULE_START:
				new PouleMeldeListeSheetNew(ws).start();
				break;
			case CMD_POULE_NEUE_MELDELISTE:
				new PouleMeldeListeSheetNew(ws).start();
				break;
			case CMD_POULE_UPDATE_MELDELISTE:
				new PouleMeldeListeSheetUpdate(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_POULE_TEILNEHMER:
				new PouleTeilnehmerSheet(ws).testTurnierVorhanden().start();
				break;
			case CMD_POULE_TESTDATEN_MELDELISTE:
				new PouleMeldeListeSheetTestDaten(ws).start();
				break;
			case CMD_POULE_VORRUNDE:
				new PouleVorrundeSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_POULE_SPIELPLAENE:
				new PouleSpielplaeneSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_POULE_VORRUNDEN_RANGLISTE:
				new PouleVorrundenRanglisteSheet(ws).testTurnierVorhanden().start();
				break;
			case CMD_POULE_KO:
				new PouleKoSheet(ws).testTurnierVorhanden().backUpDocument().start();
				break;
			case CMD_POULE_TESTDATEN_TURNIER:
				new PouleTurnierTestDaten(ws).start();
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
			case CMD_LOGFILE_ANZEIGEN:
				Log4J.openLogFile();
				break;
			case CMD_PLUGIN_KONFIGURATION:
				new GlobalPropertiesDialog(ws.getxContext()).zeigen();
				break;
			case CMD_PROCESSBOX_ANZEIGEN:
				ProcessBox.zeigeImVordergrund();
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

	/**
	 * Behandelt Webserver-Befehle direkt (ohne SheetRunner-Thread).
	 * Gibt true zurück wenn der Befehl behandelt wurde.
	 */
	private boolean behandleWebserverBefehl(String command) throws com.sun.star.uno.Exception {
		switch (command) {
			case CMD_WEBSERVER_KONFIGURATION -> new WebserverKonfigDialog(xContext).zeigen();
			case CMD_WEBSERVER_STARTEN -> {
				if (GlobalProperties.get().getPortKonfigurationen().isEmpty()) {
					MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
							.caption(I18n.get("webserver.starten"))
							.message(I18n.get("webserver.keine.ports.konfiguriert")).show();
					new WebserverKonfigDialog(xContext).zeigen();
				} else {
					ProcessBox.init(xContext).visible().clear().run();
					try {
						WebServerManager.get().starten(xContext);
						notifyAllListeners();
					} finally {
						ProcessBox.from().ready();
					}
				}
			}
			case CMD_WEBSERVER_STOPPEN -> {
				ProcessBox.init(xContext).visible().clear().run();
				try {
					WebServerManager.get().stoppen();
					notifyAllListeners();
				} finally {
					ProcessBox.from().ready();
				}
			}
			case CMD_WEBSERVER_URL_1  -> oeffneBrowserUrlFuerSlot(0);
			case CMD_WEBSERVER_URL_2  -> oeffneBrowserUrlFuerSlot(1);
			case CMD_WEBSERVER_URL_3  -> oeffneBrowserUrlFuerSlot(2);
			case CMD_WEBSERVER_URL_4  -> oeffneBrowserUrlFuerSlot(3);
			case CMD_WEBSERVER_URL_5  -> oeffneBrowserUrlFuerSlot(4);
			case CMD_WEBSERVER_URL_6  -> oeffneBrowserUrlFuerSlot(5);
			case CMD_WEBSERVER_URL_7  -> oeffneBrowserUrlFuerSlot(6);
			case CMD_WEBSERVER_URL_8  -> oeffneBrowserUrlFuerSlot(7);
			case CMD_WEBSERVER_URL_9  -> oeffneBrowserUrlFuerSlot(8);
			case CMD_WEBSERVER_URL_10 -> oeffneBrowserUrlFuerSlot(9);
			default -> { return false; }
		}
		return true;
	}

	/**
	 * Öffnet den Browser für den konfigurierten URL-Slot.
	 * Loggt die Aktion in der ProcessBox.
	 */
	private void oeffneBrowserUrlFuerSlot(int slot) {
		ProcessBox.init(xContext).visible();
		var url = WebServerManager.get().getUrlFuerSlot(slot);
		if (url != null) {
			oeffneBrowserUrl(url);
		} else {
			ProcessBox.from().info(I18n.get("webserver.prozessbox.slot.nicht.aktiv", slot + 1));
			logger.warn("Dispatch auf URL-Slot {} ohne aktive Instanz", slot);
		}
	}

	/**
	 * Öffnet die übergebene URL im Standard-Browser.
	 * Nutzt {@link java.awt.Desktop#browse} als Primärweg, {@link Runtime#exec} als Fallback.
	 */
	private void oeffneBrowserUrl(String url) {
		ProcessBox.from().info(I18n.get("webserver.prozessbox.browser.oeffnen", url));
		try {
			if (java.awt.Desktop.isDesktopSupported()
					&& java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
				java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
			} else {
				oeffneBrowserUrlFallback(url);
			}
		} catch (Exception e) {
			logger.warn("Desktop.browse fehlgeschlagen, Fallback aktiv: {}", e.getMessage());
			oeffneBrowserUrlFallback(url);
		}
	}

	/**
	 * Fallback: Browser über OS-spezifischen Prozessaufruf öffnen.
	 */
	private void oeffneBrowserUrlFallback(String url) {
		try {
			String osName = System.getProperty("os.name");
			String[] cmd;
			if (osName != null && osName.toLowerCase().contains("win")) {
				cmd = new String[]{ "rundll32", "url.dll,FileProtocolHandler", url };
			} else if (osName != null && osName.toLowerCase().contains("mac")) {
				cmd = new String[]{ "open", url };
			} else {
				cmd = new String[]{ "xdg-open", url };
			}
			new ProcessBuilder(cmd).start();
		} catch (java.io.IOException e) {
			logger.error("Browser öffnen fehlgeschlagen: {}", url, e);
			ProcessBox.from().fehler(I18n.get("webserver.prozessbox.browser.fehler", url));
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
				MessageBox.from(ws.getxContext(), MessageBoxTypeEnum.ERROR_OK)
						.caption(I18n.get("msg.caption.konfiguration"))
						.message(I18n.get("msg.text.kein.turnier")).show();
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
		var aktivesDokument = holeAktivesDokument();
		STATUS_LISTENERS.computeIfAbsent(command, k -> Collections.synchronizedList(new ArrayList<>()))
				.add(new StatusEntry(listener, url, aktivesDokument));
		postStatus(listener, url, isEnabled(command, aktivesDokument));
	}

	@Override
	public void removeStatusListener(XStatusListener listener, URL url) {
		String command = url.Path;
		List<StatusEntry> list = STATUS_LISTENERS.get(command);
		if (list != null) {
			list.removeIf(e -> e.listener == listener);
		}
	}

	/** Liefert das aktuell aktive Spreadsheet-Dokument, oder {@code null} wenn keines aktiv ist. */
	private static XSpreadsheetDocument holeAktivesDokument() {
		try {
			return DocumentHelper.getCurrentSpreadsheetDocument(SHARED_CONTEXT);
		} catch (Exception e) {
			return null;
		}
	}

	// -------------------------------------------------------------------------
	// Zustandsprüfung und Listener-Benachrichtigung (statisch, cross-instance)

	private static boolean isEnabled(String command, XSpreadsheetDocument document) {
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
				 CMD_JGJ_DIREKTVERGLEICH, CMD_JGJ_TEILNEHMER            -> ts == TurnierSystem.JGJ;
			// JGJ-Testdaten: auch wenn kein Turnier vorhanden
			case CMD_JGJ_TESTDATEN_TURNIER                  -> ts == TurnierSystem.KEIN || ts == TurnierSystem.JGJ;
			// Schweizer
			case CMD_SCHWEIZER_START                        -> ts == TurnierSystem.KEIN;
			// Maastrichter
			case CMD_MAASTRICHTER_START                     -> ts == TurnierSystem.KEIN;
			case CMD_MAASTRICHTER_UPDATE_MELDELISTE,
				 CMD_MAASTRICHTER_NAECHSTE_VORRUNDE,
				 CMD_MAASTRICHTER_VORRUNDEN_RANGLISTE,
				 CMD_MAASTRICHTER_FINALRUNDEN,
				 CMD_MAASTRICHTER_TEILNEHMER                -> ts == TurnierSystem.MAASTRICHTER;
			case CMD_MAASTRICHTER_AKTUELLE_VORRUNDE         -> ts == TurnierSystem.MAASTRICHTER && hatMaastrichterVorrunde(ws);
			case CMD_MAASTRICHTER_TESTDATEN_TURNIER,
				 CMD_MAASTRICHTER_TESTDATEN_TURNIER_57,
				 CMD_MAASTRICHTER_TESTDATEN_TURNIER_35      -> ts == TurnierSystem.KEIN || ts == TurnierSystem.MAASTRICHTER;
			// K.-O.
			case CMD_KO_START                               -> ts == TurnierSystem.KEIN;
			case CMD_KO_UPDATE_MELDELISTE,
				 CMD_KO_TURNIERBAUM, CMD_KO_TEILNEHMER     -> ts == TurnierSystem.KO;
			// Poule A/B
			case CMD_POULE_START                            -> ts == TurnierSystem.KEIN;
			case CMD_POULE_NEUE_MELDELISTE,
				 CMD_POULE_UPDATE_MELDELISTE,
				 CMD_POULE_TEILNEHMER                      -> ts == TurnierSystem.POULE;
			case CMD_POULE_TESTDATEN_MELDELISTE             -> ts == TurnierSystem.KEIN || ts == TurnierSystem.POULE;
			case CMD_POULE_VORRUNDE,
				 CMD_POULE_SPIELPLAENE,
				 CMD_POULE_VORRUNDEN_RANGLISTE,
				 CMD_POULE_KO                               -> ts == TurnierSystem.POULE;
			case CMD_POULE_TESTDATEN_TURNIER                -> ts == TurnierSystem.KEIN || ts == TurnierSystem.POULE;
			// K.-O.-Testdaten: auch wenn kein Turnier vorhanden
			case CMD_KO_TESTDATEN_NUR_MELDELISTE,
				 CMD_KO_TESTDATEN_8_TEAMS,
				 CMD_KO_TESTDATEN_16_TEAMS,
				 CMD_KO_TESTDATEN_CADRAGE                   -> ts == TurnierSystem.KEIN || ts == TurnierSystem.KO;
			case CMD_SCHWEIZER_NEUE_MELDELISTE,
				 CMD_SCHWEIZER_UPDATE_MELDELISTE,
				 CMD_SCHWEIZER_TEILNEHMER,
				 CMD_SCHWEIZER_NAECHSTE_SPIELRUNDE,
				 CMD_SCHWEIZER_RANGLISTE,
				 CMD_SCHWEIZER_RANGLISTE_SORTIEREN          -> ts == TurnierSystem.SCHWEIZER;
			// "Spielrunde neu auslosen": nur wenn Schweizer aktiv UND mind. 1 Spielrunde vorhanden
			case CMD_SCHWEIZER_AKTUELLE_SPIELRUNDE          -> ts == TurnierSystem.SCHWEIZER && hatSchweizerSpielrunde(ws);
			// Schweizer-Testdaten: auch wenn kein Turnier vorhanden
			case CMD_SCHWEIZER_TESTDATEN_MELDELISTE,
				 CMD_SCHWEIZER_TESTDATEN_TURNIER,
				 CMD_SCHWEIZER_TESTDATEN_TURNIER_19        -> ts == TurnierSystem.KEIN || ts == TurnierSystem.SCHWEIZER;
			// Konfiguration: nur wenn Turnier vorhanden
			case CMD_KONFIGURATION_TURNIER,
				 CMD_KONFIGURATION_KOPFFUSSZEILEN,
				 CMD_KONFIGURATION_FARBEN,
				 CMD_KONFIGURATION_UPDATE_ERSTELLT_MIT_VERSION -> ts != TurnierSystem.KEIN;
			// Webserver: Konfiguration immer aktiv; starten/stoppen je nach Zustand
			case CMD_WEBSERVER_KONFIGURATION                -> true;
			case CMD_WEBSERVER_STARTEN                      -> !WebServerManager.get().isLaeuft();
			// Stoppen: nur das Owner-Dokument darf den WS stoppen
			case CMD_WEBSERVER_STOPPEN                      -> WebServerManager.get().istOwnerDocument(document);
			// URL-Slots: nur für das Owner-Dokument aktiv und sichtbar
			case CMD_WEBSERVER_URL_1  -> WebServerManager.get().hatInstanzFuerSlot(0)
					&& WebServerManager.get().istOwnerDocument(document);
			case CMD_WEBSERVER_URL_2  -> WebServerManager.get().hatInstanzFuerSlot(1)
					&& WebServerManager.get().istOwnerDocument(document);
			case CMD_WEBSERVER_URL_3  -> WebServerManager.get().hatInstanzFuerSlot(2)
					&& WebServerManager.get().istOwnerDocument(document);
			case CMD_WEBSERVER_URL_4  -> WebServerManager.get().hatInstanzFuerSlot(3)
					&& WebServerManager.get().istOwnerDocument(document);
			case CMD_WEBSERVER_URL_5  -> WebServerManager.get().hatInstanzFuerSlot(4)
					&& WebServerManager.get().istOwnerDocument(document);
			case CMD_WEBSERVER_URL_6  -> WebServerManager.get().hatInstanzFuerSlot(5)
					&& WebServerManager.get().istOwnerDocument(document);
			case CMD_WEBSERVER_URL_7  -> WebServerManager.get().hatInstanzFuerSlot(6)
					&& WebServerManager.get().istOwnerDocument(document);
			case CMD_WEBSERVER_URL_8  -> WebServerManager.get().hatInstanzFuerSlot(7)
					&& WebServerManager.get().istOwnerDocument(document);
			case CMD_WEBSERVER_URL_9  -> WebServerManager.get().hatInstanzFuerSlot(8)
					&& WebServerManager.get().istOwnerDocument(document);
			case CMD_WEBSERVER_URL_10 -> WebServerManager.get().hatInstanzFuerSlot(9)
					&& WebServerManager.get().istOwnerDocument(document);
			// Release-Infos, Download, Direkt-Aktualisieren, Stop: immer aktiv
			case CMD_RELEASE_INFOS_ANZEIGEN,
				 CMD_DOWNLOAD_EXTENSION,
				 CMD_DIREKT_AKTUALISIEREN,
				 CMD_LOGFILE_ANZEIGEN,
				 CMD_PLUGIN_KONFIGURATION,
				 CMD_PROCESSBOX_ANZEIGEN,
				 CMD_ABBRUCH                                -> true;
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
	 * Prüft ob mindestens eine Maastrichter-Vorrunde vorhanden ist.
	 * Unterstützt sowohl lokalisierte Namen als auch ältere Dokumente mit deutschen Blattnamen.
	 */
	private static boolean hatMaastrichterVorrunde(WorkingSpreadsheet ws) {
		try {
			var doc = ws.getWorkingSpreadsheetDocument();
			if (doc == null) {
				return false;
			}
			String legacyName = "1. " + SheetNamen.LEGACY_MAASTRICHTER_VORRUNDE_PRAEFIX;
			return doc.getSheets().hasByName(SheetNamen.maastrichterVorrunde(1))
					|| doc.getSheets().hasByName(legacyName);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Prüft ob mindestens eine Schweizer-Spielrunde vorhanden ist.
	 * Unterstützt sowohl lokalisierte Namen als auch ältere Dokumente mit deutschen Blattnamen.
	 */
	private static boolean hatSchweizerSpielrunde(WorkingSpreadsheet ws) {
		try {
			var doc = ws.getWorkingSpreadsheetDocument();
			if (doc == null) {
				return false;
			}
			String legacyName = "1. " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN;
			return doc.getSheets().hasByName(SheetNamen.spielrunde(1))
					|| doc.getSheets().hasByName(legacyName);
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
			for (StatusEntry e : new ArrayList<>(entry.getValue())) {
				// Dokument pro Listener übergeben: Owner-abhängige Befehle (WS stoppen/URLs)
				// werden für jedes Dokument individuell ausgewertet
				postStatus(e.listener, e.url, isEnabled(entry.getKey(), e.document));
			}
		}
	}

	private static void postStatus(XStatusListener listener, URL url, boolean enabled) {
		try {
			FeatureStateEvent event = new FeatureStateEvent();
			event.FeatureURL = url;
			event.IsEnabled = enabled;
			event.Requery = false;
			setzeUrlSlotState(event, url.Path);
			listener.statusChanged(event);
		} catch (Exception e) {
			logger.warn("Fehler beim Benachrichtigen des Status-Listeners: {}", e.getMessage());
		}
	}

	/**
	 * Setzt FeatureStateEvent.State für URL-Slot-Befehle dynamisch auf "[SheetName] – [URL]".
	 * Platzhalter "—" wenn kein Port aktiv – verhindert Geister-Einträge im Menü.
	 */
	private static void setzeUrlSlotState(FeatureStateEvent event, String command) {
		int slot = switch (command) {
			case CMD_WEBSERVER_URL_1  -> 0;
			case CMD_WEBSERVER_URL_2  -> 1;
			case CMD_WEBSERVER_URL_3  -> 2;
			case CMD_WEBSERVER_URL_4  -> 3;
			case CMD_WEBSERVER_URL_5  -> 4;
			case CMD_WEBSERVER_URL_6  -> 5;
			case CMD_WEBSERVER_URL_7  -> 6;
			case CMD_WEBSERVER_URL_8  -> 7;
			case CMD_WEBSERVER_URL_9  -> 8;
			case CMD_WEBSERVER_URL_10 -> 9;
			default -> -1;
		};
		if (slot >= 0) {
			var label = WebServerManager.get().getMenuLabelFuerSlot(slot);
			event.State = label != null ? label : "—";
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

	private record StatusEntry(XStatusListener listener, URL url, XSpreadsheetDocument document) {
	}
}
