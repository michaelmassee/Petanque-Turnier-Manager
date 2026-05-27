/*
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
import com.sun.star.frame.XController;
import com.sun.star.frame.XController2;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStatusListener;
import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.XInitialization;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.ui.XDeck;
import com.sun.star.ui.XDecks;
import com.sun.star.ui.XSidebarProvider;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.FolderPicker;
import com.sun.star.ui.dialogs.XFolderPicker2;
import com.sun.star.util.URL;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.timer.TimerDialog;
import de.petanqueturniermanager.timer.TimerManager;
import de.petanqueturniermanager.webserver.CompositeViewListeDialog;
import de.petanqueturniermanager.webserver.WebServerManager;
import de.petanqueturniermanager.comp.newrelease.DirectUpdate;
import de.petanqueturniermanager.comp.newrelease.DownloadExtension;
import de.petanqueturniermanager.comp.newrelease.ReleaseUpdateService;
import de.petanqueturniermanager.comp.newrelease.ReleaseInfosAnzeigen;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.perflog.PerfLog;
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
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetNew;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetTestDaten;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXMeldeListeSheetUpdate;
import de.petanqueturniermanager.formulex.meldeliste.FormuleXTeilnehmerSheet;
import de.petanqueturniermanager.formulex.rangliste.FormuleXRanglisteSheet;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXSpielrundeSheetNaechste;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXTurnierTestDaten;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXSpielrundeSheetUpdate;
import de.petanqueturniermanager.kaskade.KaskadeTurnierTestDaten;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetNew;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetTestDaten;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeMeldeListeSheetUpdate;
import de.petanqueturniermanager.kaskade.meldeliste.KaskadeTeilnehmerSheet;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeAktuelleRundeSheet;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeKoFeldSheet;
import de.petanqueturniermanager.kaskade.spielrunde.KaskadeSpielrundeSheet;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_Update;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteDirektvergleichSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheetSortOnly;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJDoublette17TurnierTestDaten;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJTurnierTestDaten;
import de.petanqueturniermanager.konfigdialog.properties.FarbenDialog;
import de.petanqueturniermanager.konfigdialog.properties.KopfFusszeilenDialog;
import de.petanqueturniermanager.konfigdialog.properties.TurnierDialog;
import de.petanqueturniermanager.konfigdialog.properties.TurnierStartseiteDialog;
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
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetUpdate;
import de.petanqueturniermanager.poule.Poule37TeamsTurnierTestDaten;
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
import de.petanqueturniermanager.supermelee.meldeliste.SupermeleeTeilnehmerSheet;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundePlan;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Naechste;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Update;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Validator;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet_SortOnly;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRangliste_Validator;
import de.petanqueturniermanager.toolbar.SpieltagToolbarSteuerung;
import de.petanqueturniermanager.toolbar.ToolbarAktionDispatcher;
import de.petanqueturniermanager.toolbar.ToolbarAnzeigenListener;
import de.petanqueturniermanager.toolbar.TurnierModus;
import de.petanqueturniermanager.toolbar.TurnierSystemAuswahlDialog;
import de.petanqueturniermanager.toolbar.TurnierSystemNeueDateiAuswahlDialog;
import de.petanqueturniermanager.toolbar.TurnierSystemToolbarStrategieRegistry;

/**
 * UNO ProtocolHandler für das benutzerdefinierte Protokoll "ptm:".
 * <p>
 * Implementiert {@link XDispatchProvider} und {@link XDispatch}, um
 * Menüpunkte dynamisch zu aktivieren/deaktivieren basierend auf dem
 * aktuellen Turniersystem im Dokument.
 * <p>
 * Zentraler Einstiegspunkt für alle Menüaktionen aller Turniersysteme.
 */
public class ProtocolHandler extends WeakBase implements XDispatchProvider, XDispatch, XInitialization, XServiceInfo {

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
	public static final String CMD_JGJ_TESTDATEN_TURNIER_DOUBLETTE_17 = "jgj_testdaten_turnier_doublette_17";
	// Schweizer
	public static final String CMD_SCHWEIZER_START = "schweizer_start";
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
	// Formule X
	public static final String CMD_FORMULEX_START                   = "formulex_start";
	public static final String CMD_FORMULEX_UPDATE_MELDELISTE        = "formulex_update_meldeliste";
	public static final String CMD_FORMULEX_TEILNEHMER               = "formulex_teilnehmer";
	public static final String CMD_FORMULEX_NAECHSTE_SPIELRUNDE      = "formulex_naechste_spielrunde";
	public static final String CMD_FORMULEX_AKTUELLE_SPIELRUNDE      = "formulex_aktuelle_spielrunde";
	public static final String CMD_FORMULEX_RANGLISTE                = "formulex_rangliste";
	public static final String CMD_FORMULEX_TESTDATEN_MELDELISTE     = "formulex_testdaten_meldeliste";
	public static final String CMD_FORMULEX_TESTDATEN_TURNIER        = "formulex_testdaten_turnier";
	// Kaskaden-KO
	public static final String CMD_KASKADE_START              = "kaskade_start";
	public static final String CMD_KASKADE_UPDATE_MELDELISTE  = "kaskade_update_meldeliste";
	public static final String CMD_KASKADE_TEILNEHMER         = "kaskade_teilnehmer";
	public static final String CMD_KASKADE_TESTDATEN_MELDELISTE = "kaskade_testdaten_meldeliste";
	public static final String CMD_KASKADE_TESTDATEN_TURNIER   = "kaskade_testdaten_turnier";
	public static final String CMD_KASKADE_NAECHSTE_RUNDE      = "kaskade_naechste_runde";
	public static final String CMD_KASKADE_AKTUELLE_RUNDE      = "kaskade_aktuelle_runde";
	public static final String CMD_KASKADE_KO_FELDER           = "kaskade_ko_felder";
	// Teilnehmer
	public static final String CMD_SCHWEIZER_TEILNEHMER = "schweizer_teilnehmer";
	public static final String CMD_JGJ_TEILNEHMER       = "jgj_teilnehmer";
	// Checkin-Listen (je System, außer Liga)
	public static final String CMD_JGJ_CHECKIN          = "jgj_checkin";
	public static final String CMD_KO_CHECKIN           = "ko_checkin";
	public static final String CMD_KASKADE_CHECKIN      = "kaskade_checkin";
	public static final String CMD_FORMULEX_CHECKIN     = "formulex_checkin";
	public static final String CMD_SCHWEIZER_CHECKIN    = "schweizer_checkin";
	public static final String CMD_POULE_CHECKIN        = "poule_checkin";
	public static final String CMD_MAASTRICHTER_CHECKIN = "maastrichter_checkin";
	// Poule A/B
	public static final String CMD_POULE_START               = "poule_start";
	public static final String CMD_POULE_UPDATE_MELDELISTE   = "poule_update_meldeliste";
	public static final String CMD_POULE_TEILNEHMER          = "poule_teilnehmer";
	public static final String CMD_POULE_TESTDATEN_MELDELISTE = "poule_testdaten_meldeliste";
	public static final String CMD_POULE_VORRUNDE              = "poule_vorrunde";
	public static final String CMD_POULE_SPIELPLAENE           = "poule_spielplaene";
	public static final String CMD_POULE_VORRUNDEN_RANGLISTE   = "poule_vorrunden_rangliste";
	public static final String CMD_POULE_KO                    = "poule_ko";
	public static final String CMD_POULE_TESTDATEN_TURNIER     = "poule_testdaten_turnier";
	public static final String CMD_POULE_TESTDATEN_TURNIER_37  = "poule_testdaten_turnier_37";
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
	// Timer
	public static final String CMD_TIMER_STARTEN_DIALOG   = "timer_starten_dialog";
	public static final String CMD_TIMER_PAUSE_FORTSETZEN = "timer_pause_fortsetzen";
	public static final String CMD_TIMER_STOPPEN          = "timer_stoppen";
	public static final String CMD_TIMER_PLUS_MINUTE      = "timer_plus_minute";
	public static final String CMD_TIMER_MINUS_MINUTE     = "timer_minus_minute";
	public static final String CMD_TIMER_SNOOZE           = "timer_snooze";
	// Spieler-DB
	public static final String CMD_SPIELERDB_OEFFNEN       = "spielerdb_oeffnen";
	public static final String CMD_SPIELERDB_IN_MELDELISTE = "spielerdb_in_meldeliste";
	public static final String CMD_SPIELERDB_VEREINE       = "spielerdb_vereine";
	public static final String CMD_SPIELERDB_LABELS        = "spielerdb_labels";
	public static final String CMD_SPIELERDB_ABGLEICH      = "spielerdb_abgleich";
	public static final String CMD_SPIELERDB_VORLAGE_ERSTELLEN = "spielerdb_vorlage_erstellen";
	public static final String CMD_SPIELERDB_VORLAGE_ABGLEICH  = "spielerdb_vorlage_abgleich";
	public static final String CMD_SPIELERDB_EXPORT        = "spielerdb_export";
	public static final String CMD_SPIELERDB_IMPORT        = "spielerdb_import";
	public static final String CMD_SPIELERDB_WEBVIEW       = "spielerdb_webview";
	// Konfiguration
	public static final String CMD_KONFIGURATION_TURNIER = "konfiguration_turnier";
	public static final String CMD_KONFIGURATION_KOPFFUSSZEILEN = "konfiguration_kopffusszeilen";
	public static final String CMD_KONFIGURATION_FARBEN = "konfiguration_farben";
	public static final String CMD_KONFIGURATION_TURNIER_STARTSEITE = "konfiguration_turnier_startseite";
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
	public static final String CMD_PROJEKT_SEITE_OEFFNEN = "projekt_seite_oeffnen";

	private static final String PROJEKT_SEITE_URL =
			"https://michaelmassee.github.io/Petanque-Turnier-Manager/";
	// Symbolleiste
	public static final String CMD_TOOLBAR_START                 = "toolbar_start";
	public static final String CMD_TOOLBAR_WEITER                = "toolbar_weiter";
	public static final String CMD_TOOLBAR_VORRUNDEN_RANGLISTE   = "toolbar_vorrunden_rangliste";
	public static final String CMD_TOOLBAR_TEILNEHMER            = "toolbar_teilnehmer";
	public static final String CMD_TOOLBAR_NEU_IN_NEUER_DATEI    = "toolbar_neu_in_neuer_datei";
	public static final String CMD_TOOLBAR_OEFFNEN               = "toolbar_oeffnen";
	public static final String CMD_TOOLBAR_NEU_AUSLOSEN          = "toolbar_neu_auslosen";
	public static final String CMD_TOOLBAR_ABSCHLUSS             = "toolbar_abschluss";
	public static final String CMD_TOOLBAR_NAECHSTER_SPIELTAG    = "toolbar_naechster_spieltag";
	public static final String CMD_TOOLBAR_GESAMTRANGLISTE       = "toolbar_gesamtrangliste";
	public static final String CMD_TOOLBAR_DRUCKEN               = "toolbar_drucken";
	public static final String CMD_TOOLBAR_DRUCKVORSCHAU         = "toolbar_druckvorschau";
	public static final String CMD_SIDEBAR_TOGGLE               = "sidebar_toggle";
	/** Deck-ID der PétTurnMngr-Seitenleiste, siehe registry/.../UI/Sidebar.xcu. */
	private static final String SIDEBAR_DECK_ID = "PetanqueTurnierManagerDeck";
	// Turnier Modus
	public static final String CMD_TURNIER_MODUS                 = "turnier_modus";
	private final XComponentContext xContext;

	/**
	 * Frame, dessen Menü/Toolbar diesen ProtocolHandler aufruft. Wird per
	 * {@link #initialize(Object[])} von LibreOffice gesetzt — UNO-Standard für
	 * Protocol Handler. Über diesen Frame wird das Ziel-Dokument deterministisch
	 * aufgelöst statt über den globalen {@code desktop.getCurrentComponent()},
	 * der durch zwischenzeitliche Focus-Wechsel (z.B. ProcessBox-Anzeige) auf
	 * ein anderes Dokument zeigen kann.
	 */
	private XFrame frame;

	private static final java.util.concurrent.atomic.AtomicInteger CTOR_COUNTER =
			new java.util.concurrent.atomic.AtomicInteger();

	public ProtocolHandler(XComponentContext xContext) {
		this.xContext = xContext;
		SHARED_CONTEXT = xContext;
		long ctorStartNs = System.nanoTime();
		int ctorNum = CTOR_COUNTER.incrementAndGet();
		logger.trace("[FOKUS-TRACE] ProtocolHandler-ctor #{} handlerHash={} thread={}",
				ctorNum, System.identityHashCode(this), Thread.currentThread().getName());
		PetanqueTurnierMngrSingleton.init(xContext);
		long t = System.nanoTime();
		PerfLog.log(logger, "[STARTUP-TIMING] ProtocolHandler-ctor PetanqueTurnierMngrSingleton.init: {} ms",
				(t - ctorStartNs) / 1_000_000L);
		// Symbolleiste sofort einblenden – deckt das erste Dokument ab, das geöffnet wurde
		// bevor der GlobalEventListener registriert war (ProtocolHandler wird lazy erzeugt).
		// Guard: Konstruktor kann von LO's FillToolbar() aufgerufen werden während die Toolbar
		// nach dem Druckvorschau-Exit neu aufgebaut wird. showElement()/requestElement() in
		// diesem Moment erzeugen Re-Entranz in LO → SIGSEGV. Daher: überspringen wenn Preview aktiv.
		if (!PetanqueTurnierMngrSingleton.isDruckvorschauAktiv()) {
			// Verzögert auf den Main-Thread posten: der ctor kann re-entrant innerhalb von
			// LO's FillToolbar() laufen – ein synchroner Aufbau verursacht dann schwarze
			// Icon-Flächen (Windows, Calc-Start). Siehe ToolbarAnzeigenListener.
			ToolbarAnzeigenListener.zeigeToolbarInAllenFramesVerzoegert(xContext);
			long tNachToolbar = System.nanoTime();
			PerfLog.log(logger, "[STARTUP-TIMING] ProtocolHandler-ctor Toolbar-Anzeige (verzögert gepostet): {} ms",
					(tNachToolbar - t) / 1_000_000L);
			t = tNachToolbar;
			// Ebenfalls verzögert posten: gleiches Re-Entranz-Fenster wie die Haupt-Toolbar.
			LoMainThread.post(xContext, () -> SpieltagToolbarSteuerung.aktualisiereInAllenFrames(xContext));
			long tNachSpieltag = System.nanoTime();
			PerfLog.log(logger, "[STARTUP-TIMING] ProtocolHandler-ctor Spieltag-Toolbar (verzögert gepostet): {} ms",
					(tNachSpieltag - t) / 1_000_000L);
			t = tNachSpieltag;
		} else {
			logger.debug("ProtocolHandler Konstruktor: Druckvorschau aktiv – Toolbar-Initialisierung übersprungen");
		}
		if (REGISTERED.compareAndSet(false, true)) {
			try {
				ReleaseUpdateService.get().addStatusListener(ProtocolHandler::notifyAllListeners);
			} catch (IllegalStateException e) {
				logger.debug("ReleaseUpdateService noch nicht initialisiert – Listener wird übersprungen");
			}
			PetanqueTurnierMngrSingleton.addGlobalEventListener(new IGlobalEventListener() {
				@Override
				public void onFocus(Object source) {
					logger.trace("[FOKUS-TRACE] onFocus: source={}", beschreibeSource(source));
					notifyAllListeners();
				}

				@Override
				public void onLoadFinished(Object source) {
					logger.trace("[FOKUS-TRACE] onLoadFinished: source={}", beschreibeSource(source));
					notifyAllListeners();
				}

				@Override
				public void onNew(Object source) {
					logger.trace("[FOKUS-TRACE] onNew: source={}", beschreibeSource(source));
					notifyAllListeners();
				}

				@Override
				public void onLoad(Object source) {
					logger.trace("[FOKUS-TRACE] onLoad: source={}", beschreibeSource(source));
					var doc = DocumentHelper.getCurrentSpreadsheetDocumentFrom(source);
					if (doc != null) {
						var ws = new WorkingSpreadsheet(xContext, doc);
						// Gewünschten Zustand zuerst lesen (unabhängig vom UI-Zustand)
						boolean kiosk = new DocumentPropertiesHelper(ws).getTurnierModusAusDocument();
						// Safety-Restore: einmal pro Session – garantiert neutralen Ausgangszustand
						if (TurnierModus.get().startupNochNichtDurchgefuehrt()) {
							TurnierModus.get().wiederherstellenAlleElemente(ws);
						}
						// Deterministisch aktivieren: neutral → kiosk wenn Property gesetzt
						if (kiosk) {
							TurnierModus.get().aktivieren(ws);
						}
					}
					notifyAllListeners();
				}

				@Override
				public void onUnload(Object source) {
					logger.trace("[FOKUS-TRACE] onUnload: source={}", beschreibeSource(source));
					// WS stoppen wenn das Owner-Dokument geschlossen wird → andere Dokumente
					// können danach wieder starten
					var geschlossenesDoc = DocumentHelper.getCurrentSpreadsheetDocumentFrom(source);
					if (geschlossenesDoc != null && WebServerManager.get().istOwnerDocument(geschlossenesDoc)) {
						logger.info("Owner-Dokument geschlossen – WebServer wird gestoppt");
						WebServerManager.get().stoppen();
						notifyAllListeners();
					}
				}

				@Override
				public void onViewClosed(Object source) {
					logger.trace("[FOKUS-TRACE] onViewClosed: source={}", beschreibeSource(source));
					// Druckvorschau-Übergang loggen: Controller bereits gewechselt wenn dieses Event feuert.
					// DRUCKVORSCHAU_AKTIV wird hier NICHT zurückgesetzt – FillToolbar läuft noch.
					// Der Reset erfolgt erst in onViewCreated, wenn der neue Controller vollständig aktiv ist.
					try {
						var xModel = Lo.qi(XModel.class, source);
						if (xModel == null) return;
						var controller = xModel.getCurrentController();
						boolean jetzt = controller == null || Lo.qi(XSpreadsheetView.class, controller) == null;
						logger.trace("[FOKUS-TRACE] onViewClosed: aktuellerController={} DRUCKVORSCHAU_AKTIV={}→bleibt",
								jetzt ? "Druckvorschau" : "ScTabViewShell", PetanqueTurnierMngrSingleton.isDruckvorschauAktiv());
					} catch (Exception e) {
						logger.error("Fehler in onViewClosed beim Druckvorschau-Tracking", e);
					}
				}

				@Override
				public void onViewCreated(Object source) {
					logger.trace("[FOKUS-TRACE] onViewCreated: source={}", beschreibeSource(source));
					// Druckvorschau-Tracking: Controller-Typ des neuen Views bestimmen.
					// ScPreviewController implementiert XSpreadsheetView nicht.
					try {
						var xModel = Lo.qi(XModel.class, source);
						if (xModel == null) return;
						var controller = xModel.getCurrentController();
						boolean istDruckvorschau = controller == null
								|| Lo.qi(XSpreadsheetView.class, controller) == null;
						if (istDruckvorschau != PetanqueTurnierMngrSingleton.isDruckvorschauAktiv()) {
							PetanqueTurnierMngrSingleton.setDruckvorschauAktiv(istDruckvorschau);
							logger.trace("[FOKUS-TRACE] onViewCreated: DRUCKVORSCHAU_AKTIV={}", istDruckvorschau);
						}
						if (!istDruckvorschau) {
							// Toolbar-Rebind nach View-Wechsel anstoßen. Verzögert auf den
							// Main-Thread posten (nie re-entrant in ein laufendes FillToolbar),
							// damit die Toolbar-Controller/XStatusListener sauber neu erzeugt
							// werden, ohne schwarze Icon-Flächen.
							ToolbarAnzeigenListener.zeigeToolbarInAllenFramesVerzoegert(xContext);
							notifyAllListeners();
						}
					} catch (Exception e) {
						logger.error("Fehler in onViewCreated beim Druckvorschau-Tracking", e);
					}
				}
			});
			PetanqueTurnierMngrSingleton.addTurnierEventListener(new ITurnierEventListener() {
				@Override
				public void onPropertiesChanged(ITurnierEvent event) {
					XSpreadsheetDocument quelldoc = event == null ? null : event.getWorkingSpreadsheetDocument();
					XSpreadsheetDocument globalDoc = holeAktivesDokument();
					logger.trace("[FOKUS-TRACE] TurnierEvent onPropertiesChanged: quelldoc={} globalAktivesDoc={} match={}",
							beschreibeDokument(quelldoc), beschreibeDokument(globalDoc),
							quelldoc != null && quelldoc.equals(globalDoc));
					notifyAllListeners();
				}
			});
			SheetRunner.addStateChangeListener(ProtocolHandler::notifyAllListeners);
			long tNachRegistriert = System.nanoTime();
			PerfLog.log(logger, "[STARTUP-TIMING] ProtocolHandler-ctor REGISTERED-Block (IGlobalEventListener+ITurnierEventListener+SheetRunner): {} ms",
					(tNachRegistriert - t) / 1_000_000L);
			t = tNachRegistriert;
		}
		long ctorGesamtMs = (System.nanoTime() - ctorStartNs) / 1_000_000L;
		PerfLog.log(logger, "[STARTUP-TIMING] ProtocolHandler-ctor GESAMT={} ms", ctorGesamtMs);
	}

	// -------------------------------------------------------------------------
	// XInitialization

	/**
	 * Wird von LibreOffice direkt nach dem Konstruktor aufgerufen. Argumente[0]
	 * ist der {@link XFrame}, dem dieser Protocol Handler zugeordnet ist.
	 */
	@Override
	public void initialize(Object[] arguments) {
		if (arguments == null || arguments.length == 0) {
			logger.warn("[FOKUS-TRACE] initialize: ohne Argumente (handlerHash={})", System.identityHashCode(this));
			return;
		}
		XFrame xFrame = Lo.qi(XFrame.class, arguments[0]);
		if (xFrame != null) {
			frame = xFrame;
			logger.trace("[FOKUS-TRACE] initialize: handlerHash={} frameHash={} frameTitle='{}'",
					System.identityHashCode(this), System.identityHashCode(xFrame), holeFrameTitle(xFrame));
		}
	}

	private static String holeFrameTitle(XFrame f) {
		if (f == null) return "<null>";
		try {
			var titled = Lo.qi(com.sun.star.frame.XTitle.class, f);
			if (titled != null) return titled.getTitle();
		} catch (Exception ignore) { }
		return "<no-title>";
	}

	/**
	 * Liefert eine {@link WorkingSpreadsheet}-Instanz für das Dokument, dessen
	 * Menü/Toolbar diesen Dispatch ausgelöst hat. Auflösung über den per
	 * {@link #initialize(Object[])} gesetzten Frame; Fallback auf das aktuell
	 * fokussierte Dokument, falls kein Frame verfügbar.
	 */
	private WorkingSpreadsheet erzeugeWorkingSpreadsheetFuerDispatch() {
		XSpreadsheetDocument doc = ermittleDokumentAusFrame();
		if (doc != null) {
			logger.trace("[FOKUS-TRACE] erzeugeWS: handlerHash={} frameHash={} frameTitle='{}' doc={}",
					System.identityHashCode(this), System.identityHashCode(frame),
					holeFrameTitle(frame), beschreibeDokument(doc));
			return new WorkingSpreadsheet(xContext, doc);
		}
		logger.warn("[FOKUS-TRACE] erzeugeWS: FALLBACK auf getCurrentSpreadsheetDocument (frame={}) – Ziel nicht deterministisch!",
				frame == null ? "null" : System.identityHashCode(frame));
		return new WorkingSpreadsheet(xContext);
	}

	private XSpreadsheetDocument ermittleDokumentAusFrame() {
		XFrame f = frame;
		if (f == null) {
			logger.warn("[FOKUS-TRACE] ermittleDokumentAusFrame: frame==null – initialize() wurde nicht aufgerufen");
			return null;
		}
		try {
			XController controller = f.getController();
			if (controller == null) {
				logger.warn("[FOKUS-TRACE] ermittleDokumentAusFrame: controller==null (frameHash={})", System.identityHashCode(f));
				return null;
			}
			XModel model = controller.getModel();
			if (model == null) {
				logger.warn("[FOKUS-TRACE] ermittleDokumentAusFrame: model==null (frameHash={})", System.identityHashCode(f));
				return null;
			}
			return Lo.qi(XSpreadsheetDocument.class, model);
		} catch (DisposedException e) {
			logger.debug("Frame disposed beim Auflösen des Ziel-Dokuments – Fallback auf aktuelles Dokument");
			return null;
		}
	}

	/**
	 * Hilfs-Logging für GlobalEventListener-Quellen (XModel/XComponent). Versucht
	 * URL und identityHash zu extrahieren – defensiv, niemals throw.
	 */
	static String beschreibeSource(Object source) {
		if (source == null) return "null";
		try {
			XModel m = Lo.qi(XModel.class, source);
			String url = m != null && m.getURL() != null && !m.getURL().isEmpty() ? m.getURL() : "<unbenannt>";
			return url + "#" + System.identityHashCode(source);
		} catch (Exception e) {
			return "<err:" + e.getMessage() + ">#" + System.identityHashCode(source);
		}
	}

	/** Hilfs-Logging: liefert URL+identityHash eines Dokuments für Fokus-Trace. */
	public static String beschreibeDokument(XSpreadsheetDocument doc) {
		if (doc == null) {
			return "null";
		}
		try {
			XModel m = Lo.qi(XModel.class, doc);
			String url = m != null && m.getURL() != null && !m.getURL().isEmpty() ? m.getURL() : "<unbenannt>";
			return url + "#" + System.identityHashCode(doc);
		} catch (Exception e) {
			return "<err:" + e.getMessage() + ">#" + System.identityHashCode(doc);
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
		logger.trace("[FOKUS-TRACE] dispatch: cmd='{}' handlerHash={} frameHash={} frameTitle='{}'",
				command, System.identityHashCode(this),
				frame == null ? "null" : System.identityHashCode(frame),
				holeFrameTitle(frame));
		StartupClock.logErstesVorkommen("dispatch:first", "ProtocolHandler.dispatch erster Aufruf (" + command + ")");
		// Guard: In der Druckvorschau bleibt die Add-on-Toolbar (LO-Bug) fälschlich aktiviert.
		// Ein Klick würde über erzeugeWorkingSpreadsheetFuerDispatch() ein WorkingSpreadsheet
		// ohne XSpreadsheetView aufbauen → nachfolgende Sheet-/Dialog-Operationen crashen.
		// Daher alle Befehle in der Vorschau blocken und den Nutzer informieren.
		if (PetanqueTurnierMngrSingleton.isDruckvorschauAktiv()) {
			logger.warn("dispatch: cmd='{}' in Druckvorschau ignoriert", command);
			MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
					.caption(I18n.get("druckvorschau.aktion.nicht.verfuegbar.titel"))
					.message(I18n.get("druckvorschau.aktion.nicht.verfuegbar.info"))
					.show();
			return;
		}
		try {
			// Timer-Befehle laufen nicht im SheetRunner-Thread → direkt behandeln
			if (behandleTimerBefehl(command)) {
				return;
			}
			// Webserver-Befehle laufen nicht im SheetRunner-Thread → direkt behandeln
			if (behandleWebserverBefehl(command)) {
				return;
			}
			// Reine Dialog-/Anzeige-Befehle ohne ProcessBox-Aktivierung,
			// damit die ProcessBox nicht in den Vordergrund kommt und Dialoge nicht überdeckt.
			if (behandleDialogBefehl(command)) {
				return;
			}
			// WICHTIG: WorkingSpreadsheet VOR ProcessBox.visibleWennAutomatisch erzeugen
			// wäre zwar zusätzlicher Schutz, reicht aber nicht — der Bezug zum richtigen
			// Dokument muss über den Frame laufen (siehe initialize/ermittleDokumentAusFrame),
			// damit Focus-Wechsel zwischen Klick und Dispatch nicht das Ziel-Doc verändern.
			WorkingSpreadsheet ws = erzeugeWorkingSpreadsheetFuerDispatch();
			ProcessBox.from().visibleWennAutomatisch().clearWennNotRunning().info("Start " + command);
			switch (command) {
			// ------------------------------
			// SuperMelee
			case CMD_NEUE_MELDELISTE:
				new MeldeListeSheet_New(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_UPDATE_MELDELISTE:
				new MeldeListeSheet_Update(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).start();
				break;
			case CMD_ANMELDUNGEN:
				new AnmeldungenSheet(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).backUpDocument().start();
				break;
			case CMD_TEILNEHMER:
				new SupermeleeTeilnehmerSheet(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).start();
				break;
			case CMD_NAECHSTE_SPIELTAG:
				new MeldeListeSheet_NeuerSpieltag(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).backUpDocument().start();
				break;
			case CMD_MELDELISTE_TESTDATEN:
				new MeldeListeSheet_TestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_SUPERMELEE_TEAMPAARUNGEN:
				new SupermeleeTeamPaarungenSheet(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).start();
				break;
			case CMD_AKTUELLE_SPIELRUNDE:
				new SpielrundeSheet_Update(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_NAECHSTE_SPIELRUNDE:
				new SpielrundeSheet_Naechste(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_SUPER_SPIELRUNDEPLAN:
				new SpielrundePlan(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_SPIELRUNDEN_TESTDATEN:
				new SpielrundeSheet_TestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_SPIELTAG_RANGLISTE:
				new SpieltagRanglisteSheet(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).backUpDocument().start();
				break;
			case CMD_SPIELTAG_RANGLISTE_SORT:
				new SpieltagRanglisteSheet_SortOnly(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).start();
				break;
			case CMD_SPIELTAGRANGLISTE_TESTDATEN:
				new SpieltagRanglisteSheet_TestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_SUPERMELEE_ENDRANGLISTE:
				new EndranglisteSheet(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).backUpDocument().start();
				break;
			case CMD_SUPERMELEE_ENDRANGLISTE_SORT:
				new EndranglisteSheet_Sort(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).start();
				break;
			case CMD_SUPERMELEE_VALIDATE:
				new SpielrundeSheet_Validator(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).start();
				break;
			case CMD_SUPERMELEE_SPIELTAGRANGLISTE_VALIDATE:
				new SpieltagRangliste_Validator(ws).testTurnierSystem(TurnierSystem.SUPERMELEE).start();
				break;
			// ------------------------------
			// Liga
			case CMD_LIGA_NEUE_MELDELISTE:
				new LigaMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_LIGA_UPDATE_MELDELISTE:
				new LigaMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.LIGA).backUpDocument().start();
				break;
			case CMD_LIGA_TESTDATEN_MELDELISTE:
				new LigaMeldeListeSheetTestDaten(ws, true).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_LIGA_SPIELPLAN:
				new LigaSpielPlanSheet(ws).testTurnierSystem(TurnierSystem.LIGA).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_LIGA_RANGLISTE:
				new LigaRanglisteSheet(ws).testTurnierSystem(TurnierSystem.LIGA).backUpDocument().start();
				break;
			case CMD_LIGA_RANGLISTE_SORTIEREN:
				new LigaRanglisteSheetSortOnly(ws).testTurnierSystem(TurnierSystem.LIGA).start();
				break;
			case CMD_LIGA_DIREKTVERGLEICH:
				new LigaRanglisteDirektvergleichSheet(ws).testTurnierSystem(TurnierSystem.LIGA).start();
				break;
			case CMD_LIGA_SPIELPLAN_TESTDATEN:
				new LigaSpielPlanSheetTestDaten(ws, false).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_LIGA_SPIELPLAN_TESTDATEN_MIT_FREISPIEL:
				new LigaSpielPlanSheetTestDaten(ws, true).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_LIGA_EXPORT:
				new LigaMeldeListeSheetExport(ws).testTurnierSystem(TurnierSystem.LIGA).start();
				break;
			// ------------------------------
			// Jeder gegen Jeden
			case CMD_JGJ_NEUE_MELDELISTE:
				new JGJMeldeListeSheet_New(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_JGJ_UPDATE_MELDELISTE:
				new JGJMeldeListeSheet_Update(ws).testTurnierSystem(TurnierSystem.JGJ).backUpDocument().start();
				break;
			case CMD_JGJ_TEILNEHMER:
				new de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJTeilnehmerSheet(ws).testTurnierSystem(TurnierSystem.JGJ).start();
				break;
			case CMD_JGJ_CHECKIN:
				new de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJCheckinListeSheet(ws).testTurnierSystem(TurnierSystem.JGJ).backUpDocument().start();
				break;
			case CMD_JGJ_SPIELPLAN:
				new JGJSpielPlanSheet(ws).testTurnierSystem(TurnierSystem.JGJ).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_JGJ_RANGLISTE:
				new JGJRanglisteSheet(ws).testTurnierSystem(TurnierSystem.JGJ).backUpDocument().start();
				break;
			case CMD_JGJ_RANGLISTE_SORTIEREN:
				new JGJRanglisteSheetSortOnly(ws).testTurnierSystem(TurnierSystem.JGJ).start();
				break;
			case CMD_JGJ_DIREKTVERGLEICH:
				new JGJRanglisteDirektvergleichSheet(ws).testTurnierSystem(TurnierSystem.JGJ).start();
				break;
			case CMD_JGJ_TESTDATEN_TURNIER:
				new JGJTurnierTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_JGJ_TESTDATEN_TURNIER_DOUBLETTE_17:
				new JGJDoublette17TurnierTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			// ------------------------------
			// Schweizer System
			case CMD_SCHWEIZER_START:
				new SchweizerMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_SCHWEIZER_UPDATE_MELDELISTE:
				new SchweizerMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.SCHWEIZER).start();
				break;
			case CMD_SCHWEIZER_TEILNEHMER:
				new de.petanqueturniermanager.schweizer.meldeliste.SchweizerTeilnehmerSheet(ws).testTurnierSystem(TurnierSystem.SCHWEIZER).start();
				break;
			case CMD_SCHWEIZER_CHECKIN:
				new de.petanqueturniermanager.schweizer.meldeliste.SchweizerCheckinListeSheet(ws).testTurnierSystem(TurnierSystem.SCHWEIZER).backUpDocument().start();
				break;
			case CMD_SCHWEIZER_AKTUELLE_SPIELRUNDE:
				new SchweizerSpielrundeSheetUpdate(ws).testTurnierSystem(TurnierSystem.SCHWEIZER).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_SCHWEIZER_NAECHSTE_SPIELRUNDE:
				new SchweizerSpielrundeSheetNaechste(ws).testTurnierSystem(TurnierSystem.SCHWEIZER).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_SCHWEIZER_RANGLISTE:
				new SchweizerRanglisteSheet(ws).testTurnierSystem(TurnierSystem.SCHWEIZER).start();
				break;
			case CMD_SCHWEIZER_RANGLISTE_SORTIEREN:
				new SchweizerRanglisteSheetSortOnly(ws).testTurnierSystem(TurnierSystem.SCHWEIZER).start();
				break;
			case CMD_SCHWEIZER_TESTDATEN_MELDELISTE:
				new SchweizerMeldeListeSheetTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_SCHWEIZER_TESTDATEN_TURNIER:
				new SchweizerTurnierTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_SCHWEIZER_TESTDATEN_TURNIER_19:
				// 19 Teams: ungerade → 1 Freilos pro Runde, Teamname in Spielrunde, Bahn Random
				new SchweizerTurnierTestDaten(ws, 19, SpielplanTeamAnzeige.NAME).testKeinAnderesTurnierVorhanden().start();
				break;
			// ------------------------------
			// Maastrichter System
			case CMD_MAASTRICHTER_START:
				new MaastrichterMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_MAASTRICHTER_UPDATE_MELDELISTE:
				new MaastrichterMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.MAASTRICHTER).backUpDocument().start();
				break;
			case CMD_MAASTRICHTER_NAECHSTE_VORRUNDE:
				new MaastrichterSpielrundeSheetNaechste(ws).testTurnierSystem(TurnierSystem.MAASTRICHTER).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_MAASTRICHTER_AKTUELLE_VORRUNDE:
				new MaastrichterSpielrundeSheetUpdate(ws).testTurnierSystem(TurnierSystem.MAASTRICHTER).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_MAASTRICHTER_VORRUNDEN_RANGLISTE:
				new MaastrichterVorrundenRanglisteSheet(ws).testTurnierSystem(TurnierSystem.MAASTRICHTER).start();
				break;
			case CMD_MAASTRICHTER_FINALRUNDEN:
				new MaastrichterFinalrundeSheet(ws).testTurnierSystem(TurnierSystem.MAASTRICHTER).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_MAASTRICHTER_TEILNEHMER:
				new MaastrichterTeilnehmerSheet(ws).testTurnierSystem(TurnierSystem.MAASTRICHTER).start();
				break;
			case CMD_MAASTRICHTER_CHECKIN:
				new de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterCheckinListeSheet(ws).testTurnierSystem(TurnierSystem.MAASTRICHTER).backUpDocument().start();
				break;
			case CMD_MAASTRICHTER_TESTDATEN_TURNIER:
				new MaastrichterTurnierTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_MAASTRICHTER_TESTDATEN_TURNIER_57:
				// 57 Teams: 4 Vorrunden, gruppenGroesse=16
				// → Aufteilung [16,16,16,9] = 4 KO-Gruppen, D mit Cadrage
				// → 57 Teams ungerade → automatisch Freilos pro Vorrunde
				new MaastrichterTurnierTestDaten(ws, 57, 4, 16).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_MAASTRICHTER_TESTDATEN_TURNIER_35:
				// 35 Teams: 3 Vorrunden, gruppenGroesse=16
				// → Aufteilung [16,16,3] = 3 Gruppen; mit 1-Team-Fold-Schutz wird der
				//   3er-Rest als eigene Gruppe C beibehalten (≥2 Teams)
				// → 35 Teams ungerade → automatisch Freilos pro Vorrunde
				new MaastrichterTurnierTestDaten(ws, 35, 3, 16).testKeinAnderesTurnierVorhanden().start();
				break;
			// ------------------------------
			// K.-O.
			case CMD_KO_START:
				new KoMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_KO_UPDATE_MELDELISTE:
				new KoMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.KO).backUpDocument().start();
				break;
			case CMD_KO_TEILNEHMER:
				new de.petanqueturniermanager.ko.meldeliste.KoTeilnehmerSheet(ws).testTurnierSystem(TurnierSystem.KO).start();
				break;
			case CMD_KO_CHECKIN:
				new de.petanqueturniermanager.ko.meldeliste.KoCheckinListeSheet(ws).testTurnierSystem(TurnierSystem.KO).backUpDocument().start();
				break;
			case CMD_KO_TURNIERBAUM:
				new KoTurnierbaumSheet(ws).testTurnierSystem(TurnierSystem.KO).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_KO_TESTDATEN_NUR_MELDELISTE:
				new KoMeldeListeSheetTestDaten(ws, 8).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_KO_TESTDATEN_8_TEAMS:
				new KoTurnierTestDaten(ws, 8).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_KO_TESTDATEN_16_TEAMS:
				new KoTurnierTestDaten(ws, 16).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_KO_TESTDATEN_CADRAGE:
				new KoTurnierTestDaten(ws, 10).testKeinAnderesTurnierVorhanden().start();
				break;
			// ------------------------------
			// Formule X
			case CMD_FORMULEX_START:
				new FormuleXMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_FORMULEX_UPDATE_MELDELISTE:
				new FormuleXMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.FORMULEX).start();
				break;
			case CMD_FORMULEX_TEILNEHMER:
				new FormuleXTeilnehmerSheet(ws).testTurnierSystem(TurnierSystem.FORMULEX).start();
				break;
			case CMD_FORMULEX_CHECKIN:
				new de.petanqueturniermanager.formulex.meldeliste.FormuleXCheckinListeSheet(ws).testTurnierSystem(TurnierSystem.FORMULEX).backUpDocument().start();
				break;
			case CMD_FORMULEX_NAECHSTE_SPIELRUNDE:
				new FormuleXSpielrundeSheetNaechste(ws).testTurnierSystem(TurnierSystem.FORMULEX).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_FORMULEX_AKTUELLE_SPIELRUNDE:
				new FormuleXSpielrundeSheetUpdate(ws).testTurnierSystem(TurnierSystem.FORMULEX).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_FORMULEX_RANGLISTE:
				new FormuleXRanglisteSheet(ws).testTurnierSystem(TurnierSystem.FORMULEX).start();
				break;
			case CMD_FORMULEX_TESTDATEN_MELDELISTE:
				new FormuleXMeldeListeSheetTestDaten(ws, 17).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_FORMULEX_TESTDATEN_TURNIER:
				new FormuleXTurnierTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			// ------------------------------
			// Kaskaden-KO
			case CMD_KASKADE_START:
				new KaskadeMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_KASKADE_UPDATE_MELDELISTE:
				new KaskadeMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.KASKADE).backUpDocument().start();
				break;
			case CMD_KASKADE_TEILNEHMER:
				new KaskadeTeilnehmerSheet(ws).testTurnierSystem(TurnierSystem.KASKADE).start();
				break;
			case CMD_KASKADE_CHECKIN:
				new de.petanqueturniermanager.kaskade.meldeliste.KaskadeCheckinListeSheet(ws).testTurnierSystem(TurnierSystem.KASKADE).backUpDocument().start();
				break;
			case CMD_KASKADE_TESTDATEN_MELDELISTE:
				new KaskadeMeldeListeSheetTestDaten(ws, 73).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_KASKADE_TESTDATEN_TURNIER:
				new KaskadeTurnierTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_KASKADE_NAECHSTE_RUNDE:
				new KaskadeSpielrundeSheet(ws).testTurnierSystem(TurnierSystem.KASKADE).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_KASKADE_AKTUELLE_RUNDE:
				new KaskadeAktuelleRundeSheet(ws).testTurnierSystem(TurnierSystem.KASKADE).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_KASKADE_KO_FELDER: {
				var koFelder = new KaskadeKoFeldSheet(ws);
				koFelder.setForceOk(true);
				koFelder.testTurnierSystem(TurnierSystem.KASKADE).backUpDocument().backupDocumentAfterRun().start();
				break;
			}
			// ------------------------------
			// Poule A/B
			case CMD_POULE_START:
				new PouleMeldeListeSheetNew(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_POULE_UPDATE_MELDELISTE:
				new PouleMeldeListeSheetUpdate(ws).testTurnierSystem(TurnierSystem.POULE).backUpDocument().start();
				break;
			case CMD_POULE_TEILNEHMER:
				new PouleTeilnehmerSheet(ws).testTurnierSystem(TurnierSystem.POULE).start();
				break;
			case CMD_POULE_CHECKIN:
				new de.petanqueturniermanager.poule.meldeliste.PouleCheckinListeSheet(ws).testTurnierSystem(TurnierSystem.POULE).backUpDocument().start();
				break;
			case CMD_POULE_TESTDATEN_MELDELISTE:
				new PouleMeldeListeSheetTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_POULE_VORRUNDE:
				new PouleVorrundeSheet(ws).testTurnierSystem(TurnierSystem.POULE).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_POULE_SPIELPLAENE:
				new PouleSpielplaeneSheet(ws).testTurnierSystem(TurnierSystem.POULE).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_POULE_VORRUNDEN_RANGLISTE:
				new PouleVorrundenRanglisteSheet(ws).testTurnierSystem(TurnierSystem.POULE).start();
				break;
			case CMD_POULE_KO:
				new PouleKoSheet(ws).testTurnierSystem(TurnierSystem.POULE).backUpDocument().backupDocumentAfterRun().start();
				break;
			case CMD_POULE_TESTDATEN_TURNIER:
				new PouleTurnierTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
				break;
			case CMD_POULE_TESTDATEN_TURNIER_37:
				new Poule37TeamsTurnierTestDaten(ws).testKeinAnderesTurnierVorhanden().start();
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
			// ------------------------------
			// Spieler-DB-Aktionen und Turnier-Startseite werden in behandleDialogBefehl() ohne ProcessBox abgewickelt.
			case CMD_KONFIGURATION_UPDATE_ERSTELLT_MIT_VERSION:
				handleKonfiguration(command, ws);
				break;
			// ------------------------------
			// Download / Stop / Neue Version
			case CMD_RELEASE_INFOS_ANZEIGEN:
				new ReleaseInfosAnzeigen(ws).start();
				break;
			case CMD_DIREKT_AKTUALISIEREN:
				new DirectUpdate(ws).start();
				break;
			case CMD_LOGFILE_ANZEIGEN:
				Log4J.openLogFile();
				break;
			case CMD_PROCESSBOX_ANZEIGEN:
				ProcessBox.zeigeImVordergrund();
				break;
			case CMD_PROJEKT_SEITE_OEFFNEN:
				oeffneBrowserUrl(PROJEKT_SEITE_URL);
				break;
			case CMD_ABBRUCH:
				SheetRunner.cancelRunner();
				break;
			// ------------------------------
			// Symbolleiste
			// Öffnen / Drucken / Druckvorschau werden in behandleDialogBefehl() ohne ProcessBox abgewickelt.
			case CMD_TOOLBAR_WEITER:
				ToolbarAktionDispatcher.weiter(ws);
				break;
			case CMD_TOOLBAR_NEU_AUSLOSEN:
				ToolbarAktionDispatcher.neuAuslosen(ws);
				break;
			case CMD_TOOLBAR_ABSCHLUSS:
				ToolbarAktionDispatcher.abschluss(ws);
				break;
			case CMD_TOOLBAR_VORRUNDEN_RANGLISTE:
				ToolbarAktionDispatcher.vorrundenRangliste(ws);
				break;
			case CMD_TOOLBAR_TEILNEHMER:
				ToolbarAktionDispatcher.teilnehmer(ws);
				break;
			case CMD_TOOLBAR_NAECHSTER_SPIELTAG:
				ToolbarAktionDispatcher.naechsterSpieltag(ws);
				break;
			case CMD_TOOLBAR_GESAMTRANGLISTE:
				ToolbarAktionDispatcher.gesamtrangliste(ws);
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
	 * Behandelt Timer-Befehle direkt (ohne SheetRunner-Thread).
	 * Gibt true zurück wenn der Befehl behandelt wurde.
	 */
	private boolean behandleTimerBefehl(String command) throws com.sun.star.uno.Exception {
		switch (command) {
			case CMD_TIMER_STARTEN_DIALOG   -> new TimerDialog(xContext).zeigen();
			case CMD_TIMER_PAUSE_FORTSETZEN -> TimerManager.get().pauseOderFortsetzen();
			case CMD_TIMER_STOPPEN          -> TimerManager.get().stoppen();
			case CMD_TIMER_PLUS_MINUTE      -> TimerManager.get().zeitAnpassen(+60);
			case CMD_TIMER_MINUS_MINUTE     -> TimerManager.get().zeitAnpassen(-60);
			case CMD_TIMER_SNOOZE           -> TimerManager.get().snooze();
			default -> { return false; }
		}
		return true;
	}

	/**
	 * Behandelt reine Dialog-/Anzeige-Befehle, die keine ProcessBox brauchen.
	 * Dadurch öffnet sich der jeweilige Dialog im Vordergrund und wird nicht
	 * von der ProcessBox überdeckt.
	 */
	private boolean behandleDialogBefehl(String command) throws com.sun.star.uno.Exception {
		switch (command) {
			case CMD_PLUGIN_KONFIGURATION -> new GlobalPropertiesDialog(xContext).zeigen();
			case CMD_KONFIGURATION_TURNIER_STARTSEITE ->
					new TurnierStartseiteDialog(erzeugeWorkingSpreadsheetFuerDispatch()).zeigen();
			case CMD_DOWNLOAD_EXTENSION   -> starteDownloadExtension();
			case CMD_TOOLBAR_START        -> oeffneTurnierStartDialog();
			case CMD_TOOLBAR_NEU_IN_NEUER_DATEI ->
					new TurnierSystemNeueDateiAuswahlDialog(erzeugeWorkingSpreadsheetFuerDispatch()).zeige();
			case CMD_TOOLBAR_OEFFNEN            -> erzeugeWorkingSpreadsheetFuerDispatch()
					.executeDispatch(".uno:Open", "_self", 0, new PropertyValue[0]);
			case CMD_TOOLBAR_DRUCKEN            -> erzeugeWorkingSpreadsheetFuerDispatch()
					.executeDispatch(".uno:Print", "_self", 0, new PropertyValue[0]);
			case CMD_TOOLBAR_DRUCKVORSCHAU      -> erzeugeWorkingSpreadsheetFuerDispatch()
					.executeDispatch(".uno:PrintPreview", "_self", 0, new PropertyValue[0]);
			case CMD_SIDEBAR_TOGGLE            -> toggleSidebar();
			// Spieler-DB-Aktionen: alle ohne ProcessBox, damit Dialoge nicht überdeckt werden
			case CMD_SPIELERDB_OEFFNEN          -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.oeffneSpielerVerwaltung(erzeugeWorkingSpreadsheetFuerDispatch());
			case CMD_SPIELERDB_IN_MELDELISTE    -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.uebernehmenInMeldeliste(erzeugeWorkingSpreadsheetFuerDispatch());
			case CMD_SPIELERDB_VEREINE          -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.oeffneVereinsVerwaltung(erzeugeWorkingSpreadsheetFuerDispatch());
			case CMD_SPIELERDB_LABELS           -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.oeffneLabelVerwaltung(erzeugeWorkingSpreadsheetFuerDispatch());
			case CMD_SPIELERDB_ABGLEICH         -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.abgleichMitMeldeliste(erzeugeWorkingSpreadsheetFuerDispatch());
			case CMD_SPIELERDB_VORLAGE_ERSTELLEN -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.vorlageErstellen(erzeugeWorkingSpreadsheetFuerDispatch());
			case CMD_SPIELERDB_VORLAGE_ABGLEICH -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.abgleichMitVorlage(erzeugeWorkingSpreadsheetFuerDispatch());
			case CMD_SPIELERDB_EXPORT           -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.exportSpielerDb(erzeugeWorkingSpreadsheetFuerDispatch());
			case CMD_SPIELERDB_IMPORT           -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.importSpielerDb(erzeugeWorkingSpreadsheetFuerDispatch());
			case CMD_SPIELERDB_WEBVIEW          -> de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher
					.zeigeWebView(erzeugeWorkingSpreadsheetFuerDispatch());
			default -> { return false; }
		}
		return true;
	}

	/**
	 * Toolbar "Turnier starten": ist im aktuellen Dokument bereits ein Turnier
	 * angelegt, erscheint eine Info-MessageBox und der Auswahl-Dialog wird
	 * automatisch in den "Neue Datei"-Modus geleitet — das aktuelle Dokument
	 * bleibt unverändert.
	 */
	private void oeffneTurnierStartDialog() throws com.sun.star.uno.Exception {
		WorkingSpreadsheet ws = erzeugeWorkingSpreadsheetFuerDispatch();
		boolean turnierVorhanden = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument() != TurnierSystem.KEIN;
		if (turnierVorhanden) {
			MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
					.caption(I18n.get("toolbar.start.bestehendes.turnier.warnung.titel"))
					.message(I18n.get("toolbar.start.bestehendes.turnier.info"))
					.show();
			new TurnierSystemNeueDateiAuswahlDialog(ws).zeige();
		} else {
			new TurnierSystemAuswahlDialog(ws).zeige();
		}
	}

	/**
	 * Blendet die PétTurnMngr-Seitenleiste ({@link #SIDEBAR_DECK_ID}) ein bzw. aus.
	 * <p>
	 * Bewusst über die UNO-Sidebar-API ({@link XController2#getSidebar()}) statt über
	 * {@code .uno:SidebarDeck.<DeckId>}: jener Master/Slave-Command bekommt die Deck-ID
	 * nur bei per URLTransformer geparster URL als Slot-Argument; aus einer Add-on-Toolbar
	 * wird er ohne dieses Argument dispatcht und bleibt wirkungslos.
	 * <p>
	 * Toggle-Logik: Ist die Seitenleiste sichtbar und unser Deck aktiv, wird sie
	 * ausgeblendet; andernfalls eingeblendet und unser Deck aktiviert.
	 */
	private void toggleSidebar() {
		XFrame f = frame;
		if (f == null) {
			logger.warn("Sidebar-Toggle: frame==null – initialize() wurde nicht aufgerufen");
			return;
		}
		XController2 controller = Lo.qi(XController2.class, f.getController());
		if (controller == null) {
			logger.warn("Sidebar-Toggle: XController2 nicht verfügbar");
			return;
		}
		XSidebarProvider sidebar = controller.getSidebar();
		if (sidebar == null) {
			logger.warn("Sidebar-Toggle: XSidebarProvider==null");
			return;
		}
		XDeck deck = ermittleSidebarDeck(sidebar);
		boolean sichtbar = sidebar.isVisible();
		boolean unserDeckAktiv = deck != null && deck.isActive();
		if (sichtbar && unserDeckAktiv) {
			sidebar.setVisible(false);
		} else {
			sidebar.setVisible(true);
			if (deck != null) {
				deck.activate(true);
			}
		}
	}

	/** Liefert das PétTurnMngr-Deck der Seitenleiste oder {@code null}, wenn nicht vorhanden. */
	private XDeck ermittleSidebarDeck(XSidebarProvider sidebar) {
		try {
			XDecks decks = sidebar.getDecks();
			if (decks != null && decks.hasByName(SIDEBAR_DECK_ID)) {
				return Lo.qi(XDeck.class, decks.getByName(SIDEBAR_DECK_ID));
			}
		} catch (com.sun.star.uno.Exception e) {
			logger.warn("Sidebar-Toggle: Deck '{}' nicht ermittelbar: {}", SIDEBAR_DECK_ID, e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Öffnet den FolderPicker auf dem LO-Main-Thread (vor dem Aufpoppen der
	 * ProcessBox) und startet den Download erst nach Bestätigung des Nutzers.
	 * Würde der Picker im SheetRunner-Thread geöffnet, läge er hinter der
	 * ProcessBox und wäre für den Nutzer unsichtbar.
	 */
	private void starteDownloadExtension() {
		XFolderPicker2 picker = FolderPicker.create(xContext);
		picker.setTitle(I18n.get("download.verzeichnis.title"));
		if (picker.execute() != ExecutableDialogResults.OK) {
			return;
		}
		java.io.File zielVerzeichnis;
		try {
			zielVerzeichnis = new java.io.File(new java.net.URI(picker.getDirectory()));
		} catch (java.net.URISyntaxException e) {
			logger.error("Ungültiges Download-Verzeichnis: {}", e.getMessage(), e);
			return;
		}
		WorkingSpreadsheet ws = erzeugeWorkingSpreadsheetFuerDispatch();
		ProcessBox.from().visibleWennAutomatisch().clearWennNotRunning().info("Start " + CMD_DOWNLOAD_EXTENSION);
		new DownloadExtension(ws, zielVerzeichnis).start();
	}

	/**
	 * Behandelt Webserver-Befehle direkt (ohne SheetRunner-Thread).
	 * Gibt true zurück wenn der Befehl behandelt wurde.
	 */
	private boolean behandleWebserverBefehl(String command) throws com.sun.star.uno.Exception {
		switch (command) {
			case CMD_WEBSERVER_KONFIGURATION -> new CompositeViewListeDialog(xContext).zeigen(null);
			case CMD_WEBSERVER_STARTEN -> {
				var props = GlobalProperties.get();
				if (props.getCompositeViewKonfigurationen().isEmpty()) {
					MessageBox.from(xContext, MessageBoxTypeEnum.INFO_OK)
							.caption(I18n.get("webserver.starten"))
							.message(I18n.get("webserver.keine.ports.konfiguriert")).show();
					new CompositeViewListeDialog(xContext).zeigen(null);
				} else {
					ProcessBox.init(xContext).visibleWennAutomatisch().clear().run();
					try {
						WebServerManager.get().starten(xContext);
						notifyAllListeners();
					} finally {
						ProcessBox.from().ready();
					}
				}
			}
			case CMD_WEBSERVER_STOPPEN -> {
				ProcessBox.init(xContext).visibleWennAutomatisch().clear().run();
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
			case CMD_TURNIER_MODUS -> {
				var ws = new WorkingSpreadsheet(xContext);
				TurnierModus.get().umschalten(ws);
				notifyAllListeners();
			}
			default -> { return false; }
		}
		return true;
	}

	/**
	 * Öffnet den Browser für den konfigurierten URL-Slot.
	 * Loggt die Aktion in der ProcessBox.
	 */
	private void oeffneBrowserUrlFuerSlot(int slot) {
		ProcessBox.init(xContext).visibleWennAutomatisch();
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
		List<StatusEntry> list = STATUS_LISTENERS.computeIfAbsent(command,
				k -> Collections.synchronizedList(new ArrayList<>()));
		list.add(new StatusEntry(listener, url));
		logger.trace("[FOKUS-TRACE] addStatusListener: cmd='{}' handlerHash={} frameHash={} listeners[{}]={} listenerClass={} thread={} druckvorschau={}",
				command, System.identityHashCode(this),
				frame == null ? "null" : System.identityHashCode(frame),
				command, list.size(),
				listener == null ? "null" : listener.getClass().getName(),
				Thread.currentThread().getName(),
				PetanqueTurnierMngrSingleton.isDruckvorschauAktiv());
		if (PetanqueTurnierMngrSingleton.isDruckvorschauAktiv()) {
			// C++-Toolbar-Controller werden während FillToolbar (Druckvorschau-Exit) angelegt.
			// postStatus() → statusChanged() als Re-Entrant-Callback in LO C++ korrumpiert
			// den Frame-Zustand → SIGSEGV nach OnCopyToDone. Guard: erst nach OnViewCreated
			// (dort ruft notifyAllListeners() alle neuen Controller korrekt auf).
			logger.trace("[FOKUS-TRACE] addStatusListener: cmd='{}' – postStatus übersprungen (Druckvorschau aktiv)", command);
			return;
		}
		// URL-basierter Override: für alle in TOOLBAR_ONLY_CMDS aufgelisteten Befehle
		// dauerhaft enabled (Workaround tdf#172207 — Toolbar-Listener sind nicht von
		// Menü-Listenern unterscheidbar, daher wird der Override für diese URLs auch
		// im Menü wirksam). Für übrige Befehle echte isEnabled-Bewertung.
		boolean enabled = TOOLBAR_ONLY_CMDS.contains(command) || isEnabled(command, holeAktivesDokument());
		postStatus(listener, url, enabled);
	}

	@Override
	public void removeStatusListener(XStatusListener listener, URL url) {
		String command = url.Path;
		List<StatusEntry> list = STATUS_LISTENERS.get(command);
		int sizeBefore = list == null ? 0 : list.size();
		if (list != null) {
			list.removeIf(e -> e.listener == listener);
		}
		int sizeAfter = list == null ? 0 : list.size();
		logger.trace("[FOKUS-TRACE] removeStatusListener: cmd='{}' handlerHash={} listeners[{}]: {}→{} thread={}",
				command, System.identityHashCode(this), command, sizeBefore, sizeAfter,
				Thread.currentThread().getName());
	}

	/** Liefert das aktuell aktive Spreadsheet-Dokument, oder {@code null} wenn keines aktiv ist. */
	private static XSpreadsheetDocument holeAktivesDokument() {
		try {
			XSpreadsheetDocument doc = DocumentHelper.getCurrentSpreadsheetDocument(SHARED_CONTEXT);
			if (logger.isDebugEnabled()) {
				logger.debug("[FOKUS-TRACE] holeAktivesDokument: {}", beschreibeDokument(doc));
			}
			return doc;
		} catch (Exception e) {
			logger.debug("[FOKUS-TRACE] holeAktivesDokument: Exception {}", e.getMessage());
			return null;
		}
	}

	// -------------------------------------------------------------------------
	// Zustandsprüfung und Listener-Benachrichtigung (statisch, cross-instance)
	//
	// Hinweis zu LO-Bug tdf#172207
	// (https://bugs.documentfoundation.org/show_bug.cgi?id=172207):
	// Die folgende isEnabled-Logik bestimmt das korrekte Enabled-State pro
	// Command. Für das Menü wird sie zuverlässig ausgewertet, weil LO bei
	// jedem Menü-Öffnen frische addStatusListener-Calls macht. Für die
	// Addon-Toolbar wird das Ergebnis nach einem internen LO-Lifecycle-
	// Event eingefroren (LO disposed die Status-Controller ohne Re-Register).
	// Click-time-Validierung in dispatch() (testKeinAnderesTurnierVorhanden /
	// testTurnierSystem) fängt den daraus resultierenden Datenverlust ab,
	// falls der User auf einen eingefrorenen, fälschlich-enabled Button klickt.
	//
	// Workaround tdf#172207: URL-Commands die NUR auf der PétTurnMngr-Toolbar
	// vorkommen (toolbar_*-Prefix) werden permanent als enabled zurückgegeben.
	// Damit erscheinen sie nach dem LO-Bug-Trigger nicht „eingefroren disabled".
	// Geteilte URLs (auch im Menü vorhanden — abbruch, turnier_modus,
	// konfiguration_turnier, spielerdb_in_meldeliste, webserver_*, timer_*)
	// folgen weiterhin der normalen Logik, weil das Menü ihre State-Genauigkeit
	// braucht.

	/**
	 * URLs die auf einer PétTurnMngr-Addon-Toolbar (Z2/Z3/Z4) liegen. Diese
	 * werden dauerhaft als enabled gemeldet, weil LO-Bug tdf#172207 die
	 * Status-Controller der Addon-Toolbar nach Lifecycle-Events einfriert.
	 *
	 * <p>Hinweis: LO liefert Listener als UNO-Bridge-Proxies (z.B.
	 * {@code jdk.proxy1.$Proxy10}) — Toolbar-Listener und Menü-Listener sind
	 * extension-seitig nicht unterscheidbar. Geteilte URLs (in Toolbar UND
	 * Menü) erscheinen im Menü daher ebenfalls als enabled; das ist der
	 * akzeptierte Trade-off. Daten-Verlust durch fälschlich-enabled Klicks
	 * wird in dispatch() via testKeinAnderesTurnierVorhanden/testTurnierSystem
	 * abgefangen.
	 */
	private static final java.util.Set<String> TOOLBAR_ONLY_CMDS = java.util.Set.of(
			// Z2 Toolbar
			CMD_ABBRUCH,
			CMD_TOOLBAR_START,
			CMD_TOOLBAR_WEITER,
			CMD_TOOLBAR_NEU_AUSLOSEN,
			CMD_TOOLBAR_ABSCHLUSS,
			CMD_TOOLBAR_VORRUNDEN_RANGLISTE,
			CMD_TOOLBAR_TEILNEHMER,
			CMD_TOOLBAR_NEU_IN_NEUER_DATEI,
			CMD_TOOLBAR_OEFFNEN,
			CMD_TOOLBAR_DRUCKEN,
			CMD_TOOLBAR_DRUCKVORSCHAU,
			CMD_SIDEBAR_TOGGLE,
			CMD_KONFIGURATION_TURNIER,
			CMD_SPIELERDB_IN_MELDELISTE,
			CMD_TURNIER_MODUS,
			CMD_WEBSERVER_STARTEN,
			CMD_WEBSERVER_STOPPEN,
			// Z3 SpieltagToolbar
			CMD_TOOLBAR_GESAMTRANGLISTE,
			CMD_TOOLBAR_NAECHSTER_SPIELTAG,
			// Z4 TimerToolbar
			CMD_TIMER_STARTEN_DIALOG,
			CMD_TIMER_PAUSE_FORTSETZEN,
			CMD_TIMER_STOPPEN,
			CMD_TIMER_PLUS_MINUTE,
			CMD_TIMER_MINUS_MINUTE,
			CMD_TIMER_SNOOZE);

	private static boolean isEnabled(String command, XSpreadsheetDocument document) {
		if (SheetRunner.isRunning()) {
			return CMD_ABBRUCH.equals(command);
		}
		XComponentContext ctx = SHARED_CONTEXT;
		if (ctx == null) {
			return false;
		}
		try {
			WorkingSpreadsheet ws = document != null
					? new WorkingSpreadsheet(ctx, document)
					: new WorkingSpreadsheet(ctx);
			TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
			if (logger.isDebugEnabled()) {
				logger.debug("[FOKUS-TRACE] isEnabled cmd='{}' doc-arg={} effective-doc={} turnierSystem={}",
						command, beschreibeDokument(document),
						beschreibeDokument(ws.getWorkingSpreadsheetDocument()), ts);
			}
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
			case CMD_AKTUELLE_SPIELRUNDE                    -> ts == TurnierSystem.SUPERMELEE && hatSupermeleeSpielrunde(ws);
			case CMD_MELDELISTE_TESTDATEN, CMD_SPIELRUNDEN_TESTDATEN,
				 CMD_SPIELTAGRANGLISTE_TESTDATEN        -> ts == TurnierSystem.KEIN || ts == TurnierSystem.SUPERMELEE;
			case CMD_LIGA_NEUE_MELDELISTE                   -> ts == TurnierSystem.KEIN;
			case CMD_LIGA_UPDATE_MELDELISTE, CMD_LIGA_SPIELPLAN,
				 CMD_LIGA_RANGLISTE, CMD_LIGA_RANGLISTE_SORTIEREN,
				 CMD_LIGA_DIREKTVERGLEICH, CMD_LIGA_EXPORT    -> ts == TurnierSystem.LIGA;
			case CMD_LIGA_TESTDATEN_MELDELISTE,
				 CMD_LIGA_SPIELPLAN_TESTDATEN,
				 CMD_LIGA_SPIELPLAN_TESTDATEN_MIT_FREISPIEL -> ts == TurnierSystem.KEIN || ts == TurnierSystem.LIGA;
			case CMD_JGJ_NEUE_MELDELISTE                    -> ts == TurnierSystem.KEIN;
			case CMD_JGJ_UPDATE_MELDELISTE, CMD_JGJ_SPIELPLAN,
				 CMD_JGJ_RANGLISTE, CMD_JGJ_RANGLISTE_SORTIEREN,
				 CMD_JGJ_DIREKTVERGLEICH, CMD_JGJ_TEILNEHMER, CMD_JGJ_CHECKIN -> ts == TurnierSystem.JGJ;
			case CMD_JGJ_TESTDATEN_TURNIER,
				 CMD_JGJ_TESTDATEN_TURNIER_DOUBLETTE_17     -> ts == TurnierSystem.KEIN || ts == TurnierSystem.JGJ;
			case CMD_SCHWEIZER_START                        -> ts == TurnierSystem.KEIN;
			case CMD_MAASTRICHTER_START                     -> ts == TurnierSystem.KEIN;
			case CMD_MAASTRICHTER_UPDATE_MELDELISTE,
				 CMD_MAASTRICHTER_NAECHSTE_VORRUNDE,
				 CMD_MAASTRICHTER_VORRUNDEN_RANGLISTE,
				 CMD_MAASTRICHTER_FINALRUNDEN,
				 CMD_MAASTRICHTER_TEILNEHMER, CMD_MAASTRICHTER_CHECKIN -> ts == TurnierSystem.MAASTRICHTER;
			case CMD_MAASTRICHTER_AKTUELLE_VORRUNDE         -> ts == TurnierSystem.MAASTRICHTER && hatMaastrichterVorrunde(ws);
			case CMD_MAASTRICHTER_TESTDATEN_TURNIER,
				 CMD_MAASTRICHTER_TESTDATEN_TURNIER_57,
				 CMD_MAASTRICHTER_TESTDATEN_TURNIER_35      -> ts == TurnierSystem.KEIN || ts == TurnierSystem.MAASTRICHTER;
			case CMD_KO_START                               -> ts == TurnierSystem.KEIN;
			case CMD_KO_UPDATE_MELDELISTE,
				 CMD_KO_TURNIERBAUM, CMD_KO_TEILNEHMER, CMD_KO_CHECKIN -> ts == TurnierSystem.KO;
			case CMD_FORMULEX_START                         -> ts == TurnierSystem.KEIN;
			case CMD_FORMULEX_UPDATE_MELDELISTE,
				 CMD_FORMULEX_TEILNEHMER, CMD_FORMULEX_CHECKIN,
				 CMD_FORMULEX_NAECHSTE_SPIELRUNDE,
				 CMD_FORMULEX_RANGLISTE                     -> ts == TurnierSystem.FORMULEX;
			case CMD_FORMULEX_AKTUELLE_SPIELRUNDE           -> ts == TurnierSystem.FORMULEX && hatFormuleXSpielrunde(ws);
			case CMD_FORMULEX_TESTDATEN_MELDELISTE,
				 CMD_FORMULEX_TESTDATEN_TURNIER             -> ts == TurnierSystem.KEIN || ts == TurnierSystem.FORMULEX;
			case CMD_KASKADE_START                          -> ts == TurnierSystem.KEIN;
			case CMD_KASKADE_UPDATE_MELDELISTE,
				 CMD_KASKADE_TEILNEHMER, CMD_KASKADE_CHECKIN -> ts == TurnierSystem.KASKADE;
			case CMD_KASKADE_TESTDATEN_MELDELISTE,
				 CMD_KASKADE_TESTDATEN_TURNIER             -> ts == TurnierSystem.KEIN || ts == TurnierSystem.KASKADE;
			case CMD_KASKADE_NAECHSTE_RUNDE,
				 CMD_KASKADE_AKTUELLE_RUNDE,
				 CMD_KASKADE_KO_FELDER                     -> ts == TurnierSystem.KASKADE;
			case CMD_POULE_START                            -> ts == TurnierSystem.KEIN;
			case CMD_POULE_UPDATE_MELDELISTE,
				 CMD_POULE_TEILNEHMER, CMD_POULE_CHECKIN   -> ts == TurnierSystem.POULE;
			case CMD_POULE_TESTDATEN_MELDELISTE             -> ts == TurnierSystem.KEIN || ts == TurnierSystem.POULE;
			case CMD_POULE_VORRUNDE,
				 CMD_POULE_SPIELPLAENE,
				 CMD_POULE_VORRUNDEN_RANGLISTE,
				 CMD_POULE_KO                               -> ts == TurnierSystem.POULE;
			case CMD_POULE_TESTDATEN_TURNIER,
				 CMD_POULE_TESTDATEN_TURNIER_37             -> ts == TurnierSystem.KEIN || ts == TurnierSystem.POULE;
			case CMD_KO_TESTDATEN_NUR_MELDELISTE,
				 CMD_KO_TESTDATEN_8_TEAMS,
				 CMD_KO_TESTDATEN_16_TEAMS,
				 CMD_KO_TESTDATEN_CADRAGE                   -> ts == TurnierSystem.KEIN || ts == TurnierSystem.KO;
			case CMD_SCHWEIZER_UPDATE_MELDELISTE,
				 CMD_SCHWEIZER_TEILNEHMER, CMD_SCHWEIZER_CHECKIN,
				 CMD_SCHWEIZER_NAECHSTE_SPIELRUNDE,
				 CMD_SCHWEIZER_RANGLISTE,
				 CMD_SCHWEIZER_RANGLISTE_SORTIEREN          -> ts == TurnierSystem.SCHWEIZER;
			case CMD_SCHWEIZER_AKTUELLE_SPIELRUNDE          -> ts == TurnierSystem.SCHWEIZER && hatSchweizerSpielrunde(ws);
			case CMD_SCHWEIZER_TESTDATEN_MELDELISTE,
				 CMD_SCHWEIZER_TESTDATEN_TURNIER,
				 CMD_SCHWEIZER_TESTDATEN_TURNIER_19        -> ts == TurnierSystem.KEIN || ts == TurnierSystem.SCHWEIZER;
			case CMD_KONFIGURATION_TURNIER,
				 CMD_KONFIGURATION_KOPFFUSSZEILEN,
				 CMD_KONFIGURATION_FARBEN,
				 CMD_KONFIGURATION_TURNIER_STARTSEITE,
				 CMD_KONFIGURATION_UPDATE_ERSTELLT_MIT_VERSION -> ts != TurnierSystem.KEIN;
			case CMD_WEBSERVER_KONFIGURATION                -> true;
			case CMD_WEBSERVER_STARTEN                      -> !WebServerManager.get().isLaeuft();
			case CMD_WEBSERVER_STOPPEN                      -> WebServerManager.get().istOwnerDocument(document);
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
			case CMD_RELEASE_INFOS_ANZEIGEN,
				 CMD_DOWNLOAD_EXTENSION,
				 CMD_DIREKT_AKTUALISIEREN,
				 CMD_LOGFILE_ANZEIGEN,
				 CMD_PLUGIN_KONFIGURATION,
				 CMD_PROCESSBOX_ANZEIGEN,
				 CMD_PROJEKT_SEITE_OEFFNEN                  -> true;
			case CMD_ABBRUCH                                -> false;
			case CMD_TOOLBAR_START                          -> ts == TurnierSystem.KEIN;
			case CMD_TOOLBAR_WEITER                         -> ts != TurnierSystem.KEIN;
			case CMD_TOOLBAR_VORRUNDEN_RANGLISTE,
				 CMD_TOOLBAR_TEILNEHMER                     -> ts != TurnierSystem.KEIN && ts != TurnierSystem.LIGA;
			case CMD_TOOLBAR_NEU_AUSLOSEN -> ts != TurnierSystem.KEIN
					&& TurnierSystemToolbarStrategieRegistry.get(ts).hatNeuAuslosen();
			case CMD_TOOLBAR_ABSCHLUSS    -> ts != TurnierSystem.KEIN
					&& TurnierSystemToolbarStrategieRegistry.get(ts).hatAbschlussphase();
			case CMD_TOOLBAR_NAECHSTER_SPIELTAG,
				 CMD_TOOLBAR_GESAMTRANGLISTE                -> ts.hatMehrereSpielTage();
			case CMD_TOOLBAR_NEU_IN_NEUER_DATEI,
				 CMD_TOOLBAR_OEFFNEN,
				 CMD_TOOLBAR_DRUCKEN,
				 CMD_TOOLBAR_DRUCKVORSCHAU,
				 CMD_SIDEBAR_TOGGLE                         -> true;
			case CMD_TURNIER_MODUS                          -> true;
			case CMD_SPIELERDB_OEFFNEN,
				 CMD_SPIELERDB_VEREINE,
				 CMD_SPIELERDB_LABELS,
				 CMD_SPIELERDB_ABGLEICH,
				 CMD_SPIELERDB_VORLAGE_ERSTELLEN,
				 CMD_SPIELERDB_VORLAGE_ABGLEICH,
				 CMD_SPIELERDB_EXPORT,
				 CMD_SPIELERDB_IMPORT,
				 CMD_SPIELERDB_WEBVIEW                      -> true;
			case CMD_SPIELERDB_IN_MELDELISTE                -> ts != TurnierSystem.KEIN;
			case CMD_TIMER_STARTEN_DIALOG                   -> timerInaktivOderBeendet();
			case CMD_TIMER_PAUSE_FORTSETZEN,
				 CMD_TIMER_STOPPEN,
				 CMD_TIMER_PLUS_MINUTE,
				 CMD_TIMER_MINUS_MINUTE                     -> timerLaeuftOderPausiert();
			case CMD_TIMER_SNOOZE                           -> timerBeendetUndNichtSnoozed();
			default -> false;
			};
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean hatSupermeleeSpielrunde(WorkingSpreadsheet ws) {
		try {
			var doc = ws.getWorkingSpreadsheetDocument();
			if (doc == null) return false;
			for (var name : doc.getSheets().getElementNames()) {
				if (name.matches("\\d+\\.\\d+\\. Spielrunde")) return true;
			}
			return false;
		} catch (Exception e) { return false; }
	}

	private static boolean hatMaastrichterVorrunde(WorkingSpreadsheet ws) {
		try {
			var doc = ws.getWorkingSpreadsheetDocument();
			if (doc == null) return false;
			String legacyName = "1. " + de.petanqueturniermanager.helper.i18n.SheetNamen.LEGACY_MAASTRICHTER_VORRUNDE_PRAEFIX;
			return doc.getSheets().hasByName(de.petanqueturniermanager.helper.i18n.SheetNamen.maastrichterVorrunde(1))
					|| doc.getSheets().hasByName(legacyName);
		} catch (Exception e) { return false; }
	}

	private static boolean hatSchweizerSpielrunde(WorkingSpreadsheet ws) {
		try {
			var doc = ws.getWorkingSpreadsheetDocument();
			if (doc == null) return false;
			String legacyName = "1. " + de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet.SHEET_NAMEN;
			return doc.getSheets().hasByName(de.petanqueturniermanager.helper.i18n.SheetNamen.spielrunde(1))
					|| doc.getSheets().hasByName(legacyName);
		} catch (Exception e) { return false; }
	}

	private static boolean hatFormuleXSpielrunde(WorkingSpreadsheet ws) {
		try {
			var doc = ws.getWorkingSpreadsheetDocument();
			if (doc == null) return false;
			return doc.getSheets().hasByName(de.petanqueturniermanager.helper.i18n.SheetNamen.formulexSpielrunde(1))
					|| doc.getSheets().hasByName("1. Spielrunde");
		} catch (Exception e) { return false; }
	}

	private static boolean timerInaktivOderBeendet() {
		var zustand = de.petanqueturniermanager.timer.TimerManager.get().getZustand();
		return zustand == de.petanqueturniermanager.timer.TimerZustand.INAKTIV
				|| zustand == de.petanqueturniermanager.timer.TimerZustand.BEENDET;
	}

	private static boolean timerLaeuftOderPausiert() {
		var zustand = de.petanqueturniermanager.timer.TimerManager.get().getZustand();
		return zustand == de.petanqueturniermanager.timer.TimerZustand.LAEUFT
				|| zustand == de.petanqueturniermanager.timer.TimerZustand.PAUSIERT;
	}

	private static boolean timerBeendetUndNichtSnoozed() {
		var tm = de.petanqueturniermanager.timer.TimerManager.get();
		return tm.getZustand() == de.petanqueturniermanager.timer.TimerZustand.BEENDET && !tm.getAktuellerZustand().snoozed();
	}

	private static final AtomicBoolean NOTIFY_ALL_FIRST_LOG = new AtomicBoolean(true);

	private static final java.util.concurrent.atomic.AtomicInteger NOTIFY_COUNTER =
			new java.util.concurrent.atomic.AtomicInteger();

	static void notifyAllListeners() {
		if (PetanqueTurnierMngrSingleton.isDruckvorschauAktiv()) {
			logger.trace("[FOKUS-TRACE] notifyAllListeners: Druckvorschau aktiv – übersprungen (thread={})",
					Thread.currentThread().getName());
			return;
		}
		int notifyId = NOTIFY_COUNTER.incrementAndGet();
		long startNs = System.nanoTime();
		Map<String, List<StatusEntry>> snapshot;
		synchronized (STATUS_LISTENERS) {
			snapshot = new HashMap<>(STATUS_LISTENERS);
		}
		// Top-Frames im Aufruf-Stack loggen damit klar ist, wer notifyAllListeners triggert
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StringBuilder caller = new StringBuilder();
		for (int i = 2; i < Math.min(stack.length, 6); i++) {
			if (caller.length() > 0) caller.append(" ← ");
			caller.append(stack[i].getClassName().substring(stack[i].getClassName().lastIndexOf('.') + 1))
					.append('.').append(stack[i].getMethodName())
					.append(':').append(stack[i].getLineNumber());
		}
		var aktivesDokument = holeAktivesDokument();
		int totalListeners = snapshot.values().stream().mapToInt(List::size).sum();
		logger.trace("[FOKUS-TRACE] notifyAllListeners #{} START thread={} aktiverDoc={} commands={} listeners={} caller={}",
				notifyId, Thread.currentThread().getName(), beschreibeDokument(aktivesDokument),
				snapshot.size(), totalListeners, caller);
		// URL-basierter Override (tdf#172207): Befehle in TOOLBAR_ONLY_CMDS dauerhaft
		// enabled; Rest echte isEnabled-Bewertung. Pro command nur einmal berechnen.
		int listenerAnzahl = 0;
		for (Map.Entry<String, List<StatusEntry>> entry : snapshot.entrySet()) {
			String cmd = entry.getKey();
			boolean enabled = TOOLBAR_ONLY_CMDS.contains(cmd) || isEnabled(cmd, aktivesDokument);
			for (StatusEntry e : new ArrayList<>(entry.getValue())) {
				postStatus(e.listener, e.url, enabled);
				listenerAnzahl++;
			}
		}
		var ctx = SHARED_CONTEXT;
		if (ctx != null) {
			SpieltagToolbarSteuerung.aktualisiereInAllenFrames(ctx);
		}
		long dauerMs = (System.nanoTime() - startNs) / 1_000_000L;
		logger.trace("[FOKUS-TRACE] notifyAllListeners #{} ENDE listenerAnzahl={} dauerMs={}",
				notifyId, listenerAnzahl, dauerMs);
		if (NOTIFY_ALL_FIRST_LOG.compareAndSet(true, false)) {
			PerfLog.log(logger, "[STARTUP-TIMING] notifyAllListeners (erster Aufruf): {} ms, Listener-Anzahl={}, thread={}",
					dauerMs, listenerAnzahl, Thread.currentThread().getName());
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
		} catch (DisposedException e) {
			// Listener wurde disposed (Toolbar-Abbau beim Controller-Wechsel) → bereinigen
			logger.debug("postStatus: Listener disposed, wird entfernt");
			STATUS_LISTENERS.values().forEach(list -> list.removeIf(entry -> entry.listener == listener));
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
		if (CMD_TURNIER_MODUS.equals(command)) {
			event.State = TurnierModus.get().istAktiv();
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
