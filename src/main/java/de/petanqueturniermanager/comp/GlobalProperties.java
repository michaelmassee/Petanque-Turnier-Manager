package de.petanqueturniermanager.comp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.perflog.PerfLog;
import de.petanqueturniermanager.helper.upload.UploadProtokoll;
import de.petanqueturniermanager.webserver.CompositeViewKonfiguration;
import de.petanqueturniermanager.webserver.PanelAusrichtung;
import de.petanqueturniermanager.webserver.PanelKonfiguration;
import de.petanqueturniermanager.webserver.PanelTyp;
import de.petanqueturniermanager.webserver.RandKonfiguration;
import de.petanqueturniermanager.webserver.RegieSlug;
import de.petanqueturniermanager.webserver.SheetResolverFactory;
import de.petanqueturniermanager.webserver.SplitBlatt;
import de.petanqueturniermanager.webserver.SplitKnoten;
import de.petanqueturniermanager.webserver.SplitKnotenAdapter;
import de.petanqueturniermanager.webserver.SplitTeilung;

public class GlobalProperties {

	private static final Logger logger = LogManager.getLogger(GlobalProperties.class);

	private static final String FILENAME = "PetanqueTurnierManager.properties";

	private static final String LOG_LEVEL_PROP = "loglevel";
	private static final String CREATE_BACKUP_PROP = "backup";
	private static final String AUTOSAVE_PROP = "autosave";
	private static final String NEW_VERSION_CHECK_PROP = "newversioncheck";
	private static final String AUTO_UPDATE_DIALOG_PROP = "auto.update.dialog.beim.start";
	private static final String UPDATE_SKIP_VERSION_PROP = "auto.update.skip.version";
	private static final String PROZESSBOX_AUTOMATISCH_ANZEIGEN_PROP = "prozessbox.automatisch.anzeigen";
	private static final String PROZESSBOX_AUTOMATISCH_SCHLIESSEN_PROP = "prozessbox.automatisch.schliessen";
	private static final String PERFORMANCE_LOGGING_PROP = "performance.logging";

	private static final String WEBSERVER_AKTIV_PROP = "webserver_aktiv";

	private static final String WEBSERVER_REGIE_AKTIV_PROP = "webserver_regie_aktiv";
	private static final String WEBSERVER_REGIE_PORT_PROP = "webserver_regie_port";
	private static final String WEBSERVER_REGIE_ZIELE_PROP = "webserver_regie_ziele";
	public static final int WEBSERVER_REGIE_DEFAULT_PORT = 9090;

	/**
	 * Legacy-Property-Präfix der entfernten Einzel-Port-Konfiguration (nur für Cleanup beim Start).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann in einer späteren Version entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String LEGACY_WEBSERVER_PORTS_PROP = "webserver_ports";
	/**
	 * Legacy-Präfix für Einzel-Port-Einträge (nur für Cleanup beim Start).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann in einer späteren Version entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String LEGACY_WEBSERVER_PORT_PREFIX = "webserver_port_";
	/**
	 * Legacy-Property der entfernten globalen „Blattnamen anzeigen"-Option (nur für Cleanup beim Start).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann in einer späteren Version entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String LEGACY_WEBSERVER_SHEETNAMEN_ANZEIGEN_PROP = "webserver_sheetnamen_anzeigen";

	private static final String WEBSERVER_COMPOSITE_PORTS_PROP = "webserver_composite_ports";
	private static final String WEBSERVER_COMPOSITE_PREFIX = "webserver_composite_";
	private static final String WEBSERVER_COMPOSITE_AKTIV_SUFFIX = "_aktiv";
	private static final String WEBSERVER_COMPOSITE_NAME_SUFFIX = "_name";
	private static final String WEBSERVER_COMPOSITE_ZOOM_SUFFIX = "_zoom";
	private static final String WEBSERVER_COMPOSITE_MIT_HEADER_FOOTER_SUFFIX = "_mit_header_footer";
	private static final String WEBSERVER_COMPOSITE_LAYOUT_SUFFIX = "_layout";
	private static final String WEBSERVER_COMPOSITE_PANEL_COUNT_SUFFIX = "_panel_count";
	private static final String WEBSERVER_COMPOSITE_PANEL_INFIX = "_panel_";
	private static final String WEBSERVER_COMPOSITE_PANEL_SHEET_SUFFIX = "_sheet";
	private static final String WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX = "_zoom";
	private static final String WEBSERVER_COMPOSITE_PANEL_SICHTBARER_TABELLENANTEIL_SUFFIX = "_sichtbarer_tabellenanteil";
	/**
	 * Legacy-Suffix der entfernten boolean-Property „zentriert" (nur für Migration / Cleanup).
	 *
	 * @deprecated Migrations-Code für ein entferntes Datenformat; kann in einer späteren Version entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String LEGACY_WEBSERVER_COMPOSITE_PANEL_ZENTRIERT_SUFFIX = "_zentriert";
	private static final String WEBSERVER_COMPOSITE_PANEL_HALIGN_SUFFIX = "_halign";
	private static final String WEBSERVER_COMPOSITE_PANEL_VALIGN_SUFFIX = "_valign";
	private static final String WEBSERVER_COMPOSITE_PANEL_BLATTNAME_SUFFIX = "_blattname";
	private static final String WEBSERVER_COMPOSITE_PANEL_TYP_SUFFIX = "_typ";
	private static final String WEBSERVER_COMPOSITE_PANEL_URL_SUFFIX = "_url";
	private static final String WEBSERVER_COMPOSITE_RAND_DICKE_SUFFIX = "_rand_dicke";
	private static final String WEBSERVER_COMPOSITE_RAND_ART_SUFFIX = "_rand_art";
	private static final String WEBSERVER_COMPOSITE_RAND_FARBE_SUFFIX = "_rand_farbe";
	private static final String WEBSERVER_COMPOSITE_RAND_TRANSPARENZ_SUFFIX = "_rand_transparenz";
	private static final String WEBSERVER_COMPOSITE_RAND_ANIMATION_SUFFIX = "_rand_animation";

	private static final String STARTUP_TURNIER_MODUS_PROP = "startup.turnier.modus";

	// FTP/SFTP-Server (zentrale Liste, primär über LibreOffice-Konfiguration)
	private static final String FTP_SERVER_JSON_PROP = "ftp_server_liste";

	/**
	 * Legacy-Präfix des entfernten host-basierten Upload-Passwort-Caches (nur für Cleanup
	 * beim Start). Das Passwort ist jetzt Teil von {@link FtpServerEintrag}.
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann entfernt werden, sobald davon
	 *             auszugehen ist, dass keine Alt-Installation die Property-Datei mit diesem
	 *             Präfix mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static final String LEGACY_UPLOAD_PASSWORT_PRAEFIX = "upload.passwort.";

	// Turnier-Startseite (dedizierter Webserver, läuft parallel zu den Composite-Views)
	private static final String STARTSEITE_PORT_PROP  = "startseite_port";
	private static final String STARTSEITE_AKTIV_PROP = "startseite_aktiv";
	private static final String STARTSEITE_ZOOM_PROP  = "startseite_zoom";
	public static final int STARTSEITE_DEFAULT_PORT = 9200;

	// Timer (jetzt Document Properties, siehe TimerDialog) – Legacy nur für Cleanup beim Start
	/**
	 * Legacy-Property der entfernten globalen Timer-Dauer (nur für Cleanup beim Start).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann in einer späteren Version entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String LEGACY_TIMER_DAUER_PROP            = "timer_letzte_dauer";
	/**
	 * Legacy-Property des entfernten globalen Timer-Ports (nur für Cleanup beim Start).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann in einer späteren Version entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String LEGACY_TIMER_PORT_PROP             = "timer_letzter_port";
	/**
	 * Legacy-Property der entfernten globalen Timer-Bezeichnung (nur für Cleanup beim Start).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann in einer späteren Version entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String LEGACY_TIMER_BEZEICHNUNG_PROP      = "timer_letzte_bezeichnung";
	/**
	 * Legacy-Property der entfernten globalen Timer-Hintergrundfarbe (nur für Cleanup beim Start).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann in einer späteren Version entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String LEGACY_TIMER_HINTERGRUNDFARBE_PROP = "timer_hintergrundfarbe";

	public static final int DEFAULT_ZOOM = 100;
	public static final int DEFAULT_SICHTBARER_TABELLENANTEIL = 100;

	// zentrale Runtime-Map
	private static final ConcurrentHashMap<String, String> propMap = new ConcurrentHashMap<>();

	private static volatile GlobalProperties instance = null;

	private static final ReentrantLock fileLock = new ReentrantLock();
	private static volatile XComponentContext libreOfficeContext;
	private static volatile boolean webserverRegieInLibreOffice = false;
	private static volatile boolean pluginOptionenInLibreOffice = false;
	private static volatile boolean ftpServerInLibreOffice = false;
	private static volatile boolean startseiteInLibreOffice = false;
	private static volatile boolean startupModusInLibreOffice = false;
	private static volatile boolean compositeViewsInLibreOffice = false;
	private static volatile boolean tabFarbenInLibreOffice = false;
	private static volatile Map<String, Integer> tabFarbenCache = Map.of();

	/**
	 * Legacy-Präfix der Tab-Farben-Werte aus der Properties-Datei (nur für Cleanup beim Start).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann entfernt werden, sobald davon
	 *             auszugehen ist, dass keine Alt-Installation diese Schlüssel mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static final String TABFARBE_PRAEFIX = "tabfarbe.";

	/** Mapping vom Konfig-Property-Key (z.B. "Tab-Farbe Meldeliste") auf den XCU-Property-Namen. */
	private static final Map<String, String> TABFARBE_KEY_MAPPING = Map.ofEntries(
			Map.entry("Tab-Farbe Meldeliste", "Meldeliste"),
			Map.entry("Tab-Farbe Teilnehmer", "Teilnehmer"),
			Map.entry("Tab-Farbe Spielrunde", "Spielrunde"),
			Map.entry("Tab-Farbe Rangliste", "Rangliste"),
			Map.entry("Tab-Farbe Direktvergleich", "Direktvergleich"),
			Map.entry("Tab-Farbe KO-Turnierbaum", "KoTurnierbaum"),
			Map.entry("Tab-Farbe Cadrage", "Cadrage"),
			Map.entry("Tab-Farbe Poule-Vorrunde", "PouleVorrunde"),
			Map.entry("Tab-Farbe Poule-Vorrunden-Rangliste", "PouleVorrundenRangliste"),
			Map.entry("Tab-Farbe Kaskaden-KO", "KaskadenKo"),
			Map.entry("Tab-Farbe Team-Paarungen", "SupermeleeTeamPaarungen"));

	private static final Gson GSON = new GsonBuilder().create();

	/**
	 * Rohdaten eines einzelnen Panels in einem Composite View (vor Resolver-Erstellung).
	 *
	 * @param typ                   Anzeigemodus des Panels
	 * @param sheetConfig           Sheet-Konfigurations-String (nur relevant wenn typ == BLATT)
	 * @param zoom                  Zoom-Faktor in %
	 * @param sichtbarerTabellenAnteil sichtbarer Anteil der Gesamttabelle in % (10–100)
	 * @param horizontalAusrichtung horizontale Ausrichtung: {@code "kein"} / {@code "links"} / {@code "mitte"} / {@code "rechts"}
	 * @param vertikalAusrichtung   vertikale Ausrichtung:   {@code "kein"} / {@code "oben"} / {@code "mitte"} / {@code "unten"}
	 * @param blattnameAnzeigen     ob der Blattname als Kopfzeile angezeigt wird
	 * @param externeUrl            externe URL oder lokaler Dateipfad (nur relevant wenn typ == URL oder STATISCHE_DATEI)
	 */
	public record PanelEintragRoh(
			PanelTyp typ,
			String sheetConfig,
			int zoom,
			int sichtbarerTabellenAnteil,
			String horizontalAusrichtung,
			String vertikalAusrichtung,
			boolean blattnameAnzeigen,
			String externeUrl) {
		public PanelEintragRoh {
			horizontalAusrichtung = PanelAusrichtung.normiereHorizontal(horizontalAusrichtung);
			vertikalAusrichtung   = PanelAusrichtung.normiereVertikal(vertikalAusrichtung);
			sichtbarerTabellenAnteil = normiereSichtbarerTabellenAnteil(sichtbarerTabellenAnteil);
		}

		public PanelEintragRoh(
				PanelTyp typ,
				String sheetConfig,
				int zoom,
				String horizontalAusrichtung,
				String vertikalAusrichtung,
				boolean blattnameAnzeigen,
				String externeUrl) {
			this(typ, sheetConfig, zoom, DEFAULT_SICHTBARER_TABELLENANTEIL,
					horizontalAusrichtung, vertikalAusrichtung, blattnameAnzeigen, externeUrl);
		}
	}

	/**
	 * Rohdaten eines Composite View (vor Resolver-Erstellung).
	 *
	 * @param port            TCP-Port
	 * @param name            optionaler Anzeigename des Views (leer = ohne Name)
	 * @param aktiv           ob dieser View aktiv ist
	 * @param zoom            globaler Zoom-Faktor in % (10–500)
	 * @param mitHeaderFooter ob Header/Footer (aus Panel 0) global einmal gerendert werden sollen
	 * @param layoutJson      JSON-serialisierter {@link SplitKnoten}-Baum (kann leer sein → Default-Layout)
	 * @param panels          geordnete Liste der Panel-Konfigurationen (Reihenfolge = panelIndex)
	 * @param rand            Konfiguration des Gesamtrahmens (Dicke/Art/Farbe/Transparenz/Animation)
	 */
	public record CompositeViewEintragRoh(
			int port, String name, boolean aktiv, int zoom, boolean mitHeaderFooter,
			String layoutJson,
			List<PanelEintragRoh> panels,
			RandKonfiguration rand) {
		public CompositeViewEintragRoh {
			name = name == null ? "" : name;
			rand = rand == null ? RandKonfiguration.KEINER : rand;
		}

		@Override
		public String toString() {
			return port + " [composite, panels=" + panels.size() + "]";
		}
	}

	public record RegieZielRoh(
			String id,
			String name,
			String slug,
			boolean aktiv,
			String viewId) {
		public RegieZielRoh {
			id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
			name = name == null ? "" : name.trim();
			String berechneterSlug = (slug == null || slug.isBlank()) ? RegieSlug.ausName(name) : slug.trim();
			slug = berechneterSlug;
			viewId = viewId == null ? "" : viewId.trim();
		}
	}

	/**
	 * Rohdaten eines zentral konfigurierten FTP/SFTP-Servers (Name, Zugangsdaten inkl. Passwort,
	 * Ziel-Verzeichnis) – vollständige Upload-Konfiguration in einem Objekt, verwaltet auf der
	 * Optionsseite Extras &gt; Optionen &gt; PétTurnMngr &gt; FTP-Server.
	 */
	public record FtpServerEintrag(
			String id, String name, UploadProtokoll protokoll, String host, int port,
			String benutzer, String passwort, String remotePfad) {
		public FtpServerEintrag {
			id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id.trim();
			name = name == null ? "" : name.trim();
			protokoll = protokoll != null ? protokoll : UploadProtokoll.SFTP;
			host = host == null ? "" : host.trim();
			benutzer = benutzer == null ? "" : benutzer.trim();
			passwort = passwort == null ? "" : passwort;
			remotePfad = remotePfad == null ? "" : remotePfad.trim();
		}

		/** Anzeigename für Listen: konfigurierter Name, sonst Host:Port. */
		public String anzeigeName() {
			return name.isBlank() ? host + ":" + port : name;
		}
	}

	public static GlobalProperties get() {
		if (instance == null) {
			synchronized (GlobalProperties.class) {
				if (instance == null) {
					try {
						instance = new GlobalProperties();
					} catch (Exception e) {
						logger.error("Initialisierung fehlgeschlagen", e);
						instance = new GlobalProperties(true);
					}
				}
			}
		}
		return instance;
	}

	private GlobalProperties() {
		readProperties();
		ladePluginOptionenAusLibreOffice();
		ladeWebserverRegieAusLibreOffice();
		ladeFtpServerAusLibreOffice();
		ladeStartseiteAusLibreOffice();
		ladeStartupModusAusLibreOffice();
		ladeCompositeViewsAusLibreOffice();
		ladeTabFarbenAusLibreOffice();
		bereinigeLegacyEinzelPortProperties();
		bereinigeLegacyTimerProperties();
		bereinigeLegacyUploadPasswortProperties();
		bereinigeLegacyTabFarbenProperties();
		safeSetLogLevel();
	}

	public static void setLibreOfficeContext(XComponentContext context) {
		libreOfficeContext = context;
		GlobalProperties lokaleInstanz = instance;
		if (lokaleInstanz != null) {
			lokaleInstanz.ladePluginOptionenAusLibreOffice();
			lokaleInstanz.ladeWebserverRegieAusLibreOffice();
			lokaleInstanz.ladeFtpServerAusLibreOffice();
			lokaleInstanz.ladeStartseiteAusLibreOffice();
			lokaleInstanz.ladeStartupModusAusLibreOffice();
			lokaleInstanz.ladeCompositeViewsAusLibreOffice();
			lokaleInstanz.ladeTabFarbenAusLibreOffice();
			lokaleInstanz.safeSetLogLevel();
		}
	}

	/**
	 * Entfernt einmalig alle Legacy-Properties der entfernten Einzel-Port-Webserver-Konfiguration
	 * ({@code webserver_ports}, {@code webserver_port_*}) aus {@link #propMap} und persistiert,
	 * falls etwas zu löschen war. Keine Migration – Altdaten werden stillschweigend verworfen.
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann entfernt werden, sobald davon
	 *             auszugehen ist, dass keine Alt-Installation diese Schlüssel mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static void bereinigeLegacyEinzelPortProperties() {
		var zuLoeschen = new ArrayList<String>();
		for (var key : propMap.keySet()) {
			if (key.equals(LEGACY_WEBSERVER_PORTS_PROP)
					|| key.startsWith(LEGACY_WEBSERVER_PORT_PREFIX)
					|| key.equals(LEGACY_WEBSERVER_SHEETNAMEN_ANZEIGEN_PROP)) {
				zuLoeschen.add(key);
			}
		}
		if (zuLoeschen.isEmpty()) {
			return;
		}
		zuLoeschen.forEach(propMap::remove);
		logger.info("{} Legacy-Einzel-Port-Property/-ies entfernt", zuLoeschen.size());
		speichernDatei();
	}

	/**
	 * Entfernt einmalig die Legacy-Properties der entfernten globalen Timer-Einstellungen
	 * ({@code timer_letzte_dauer}, {@code timer_letzter_port}, {@code timer_letzte_bezeichnung},
	 * {@code timer_hintergrundfarbe}) aus {@link #propMap} und persistiert, falls etwas zu löschen
	 * war. Keine Migration – Timer-Einstellungen liegen jetzt pro Dokument (siehe TimerDialog).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann entfernt werden, sobald davon
	 *             auszugehen ist, dass keine Alt-Installation diese Schlüssel mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static void bereinigeLegacyTimerProperties() {
		boolean geaendert = false;
		geaendert |= propMap.remove(LEGACY_TIMER_DAUER_PROP) != null;
		geaendert |= propMap.remove(LEGACY_TIMER_PORT_PROP) != null;
		geaendert |= propMap.remove(LEGACY_TIMER_BEZEICHNUNG_PROP) != null;
		geaendert |= propMap.remove(LEGACY_TIMER_HINTERGRUNDFARBE_PROP) != null;
		if (geaendert) {
			logger.info("Legacy-Timer-Properties entfernt");
			speichernDatei();
		}
	}

	/**
	 * Entfernt einmalig alle Legacy-Tab-Farben-Properties ({@code tabfarbe.*}) aus {@link #propMap}
	 * und persistiert, falls etwas zu löschen war. Keine Migration – die globalen Tab-Farben-Defaults
	 * liegen jetzt in der LibreOffice-Konfiguration (siehe {@link LibreOfficeTabFarbenSpeicher}),
	 * Alt-Werte werden beim Umstieg bewusst verworfen.
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann entfernt werden, sobald davon
	 *             auszugehen ist, dass keine Alt-Installation diese Schlüssel mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static void bereinigeLegacyTabFarbenProperties() {
		var zuLoeschen = new ArrayList<String>();
		for (var key : propMap.keySet()) {
			if (key.startsWith(TABFARBE_PRAEFIX)) {
				zuLoeschen.add(key);
			}
		}
		if (zuLoeschen.isEmpty()) {
			return;
		}
		zuLoeschen.forEach(propMap::remove);
		logger.info("{} Legacy-Tab-Farben-Property/-ies entfernt", zuLoeschen.size());
		speichernDatei();
	}

	/**
	 * Entfernt einmalig alle Legacy-Upload-Passwörter ({@code upload.passwort.<host>}) aus
	 * {@link #propMap} und persistiert, falls etwas zu löschen war. Keine Migration – das
	 * Passwort ist jetzt Teil des zentralen {@link FtpServerEintrag} (FTP-Server-Optionsseite).
	 *
	 * @deprecated Cleanup-Code für eine entfernte Funktion; kann entfernt werden, sobald davon
	 *             auszugehen ist, dass keine Alt-Installation die Property-Datei mit diesen
	 *             Schlüsseln mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static void bereinigeLegacyUploadPasswortProperties() {
		var zuLoeschen = new ArrayList<String>();
		for (var key : propMap.keySet()) {
			if (key.startsWith(LEGACY_UPLOAD_PASSWORT_PRAEFIX)) {
				zuLoeschen.add(key);
			}
		}
		if (zuLoeschen.isEmpty()) {
			return;
		}
		zuLoeschen.forEach(propMap::remove);
		logger.info("{} Legacy-Upload-Passwort-Property/-ies entfernt", zuLoeschen.size());
		speichernDatei();
	}

	private GlobalProperties(boolean fallback) {
		if (fallback) {
			logger.warn("GlobalProperties im Fallback-Modus gestartet (leere Konfiguration)");
		}
	}

	// ----------------------------------------------------
	// Laden / Speichern
	// ----------------------------------------------------
	private void readProperties() {
		fileLock.lock();
		try {
			var path = Path.of(System.getProperty("user.home"), FILENAME);

			try {
				Files.createFile(path);
			} catch (java.nio.file.FileAlreadyExistsException ignored) {
			}

			Properties loaded = new Properties();

			try (var in = Files.newInputStream(path)) {
				loaded.load(in);
			}

			propMap.clear();
			for (String key : loaded.stringPropertyNames()) {
				propMap.put(key, loaded.getProperty(key));
			}

		} catch (Exception e) {
			logger.error("Fehler beim Laden", e);
		} finally {
			fileLock.unlock();
		}
	}

	private static void speichernDatei() {

		fileLock.lock();

		try {
			var path = Path.of(System.getProperty("user.home"), FILENAME);
			var tmp = path.resolveSibling(FILENAME + ".tmp");

			Properties props = new Properties();
			for (var e : propMap.entrySet()) {
				if (istBereitsInLibreOfficeGespeichert(e.getKey())) {
					continue;
				}
				props.setProperty(e.getKey(), e.getValue());
			}

			try (var out = Files.newOutputStream(tmp)) {
				props.store(out, "Petanque Turnier Manager Konfiguration");
			}

			Files.move(tmp, path,
					java.nio.file.StandardCopyOption.REPLACE_EXISTING,
					java.nio.file.StandardCopyOption.ATOMIC_MOVE);

		} catch (Exception e) {
			logger.error("Fehler beim Speichern", e);
		} finally {
			fileLock.unlock();
		}
	}

	/**
	 * @deprecated Nur solange relevant, wie {@link #speichernDatei()} bereits nach LibreOffice
	 *             migrierte Legacy-Schlüssel beim Schreiben der Properties-Datei überspringen muss;
	 *             kann mit dem restlichen Legacy-Import-Mechanismus entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	@SuppressWarnings({ "deprecation", "removal" })
	private static boolean istBereitsInLibreOfficeGespeichert(String key) {
		return (webserverRegieInLibreOffice && istWebserverRegieLegacyKey(key))
				|| (pluginOptionenInLibreOffice && istPluginOptionenLegacyKey(key))
				|| (ftpServerInLibreOffice && istFtpServerLegacyKey(key))
				|| (startseiteInLibreOffice && istStartseiteLegacyKey(key))
				|| (startupModusInLibreOffice && istStartupModusLegacyKey(key))
				|| (compositeViewsInLibreOffice && istCompositeViewsLegacyKey(key));
	}

	// Ruft bewusst noch @Deprecated-Legacy-Import-Methoden auf (Speicher + pluginOptionenAusMap()
	// + bereinigeLegacyPluginOptionenProperties()); kann entfernt werden, sobald der einmalige Import
	// als abgeschlossen gilt.
	@SuppressWarnings({ "deprecation", "removal" })
	private void ladePluginOptionenAusLibreOffice() {
		XComponentContext context = libreOfficeContext;
		if (context == null) {
			return;
		}
		try {
			var speicher = new LibreOfficePluginOptionenSpeicher(context);
			if (!speicher.istLegacyImportErledigt()) {
				speicher.importiereLegacy(pluginOptionenAusMap());
				setPluginOptionenInLibreOffice(true);
				bereinigeLegacyPluginOptionenProperties();
			}
			pluginOptionenInMap(speicher.laden());
			setPluginOptionenInLibreOffice(true);
		} catch (IllegalStateException e) {
			setPluginOptionenInLibreOffice(false);
			logger.warn("LibreOffice-Konfiguration nicht verfügbar, verwende Legacy-Properties", e);
		}
	}

	// Ruft bewusst noch @Deprecated-Legacy-Import-Methoden auf (Speicher + webserverRegieOptionenAusMap()
	// + bereinigeLegacyWebserverRegieProperties()); kann entfernt werden, sobald der einmalige Import
	// als abgeschlossen gilt.
	@SuppressWarnings({ "deprecation", "removal" })
	private void ladeWebserverRegieAusLibreOffice() {
		XComponentContext context = libreOfficeContext;
		if (context == null) {
			return;
		}
		try {
			var speicher = new LibreOfficeWebserverRegieSpeicher(context);
			if (!speicher.istLegacyImportErledigt()) {
				speicher.importiereLegacy(webserverRegieOptionenAusMap());
				setWebserverRegieInLibreOffice(true);
				bereinigeLegacyWebserverRegieProperties();
			}
			webserverRegieOptionenInMap(speicher.laden());
			setWebserverRegieInLibreOffice(true);
		} catch (IllegalStateException e) {
			setWebserverRegieInLibreOffice(false);
			logger.warn("LibreOffice-Webserver-Regie-Konfiguration nicht verfügbar, verwende Legacy-Properties", e);
		}
	}

	private void ladeFtpServerAusLibreOffice() {
		XComponentContext context = libreOfficeContext;
		if (context == null) {
			return;
		}
		try {
			String json = new LibreOfficeFtpServerSpeicher(context).laden();
			if (json.isBlank()) {
				propMap.remove(FTP_SERVER_JSON_PROP);
			} else {
				propMap.put(FTP_SERVER_JSON_PROP, json);
			}
			setFtpServerInLibreOffice(true);
		} catch (IllegalStateException e) {
			setFtpServerInLibreOffice(false);
			logger.warn("LibreOffice-FTP-Server-Konfiguration nicht verfügbar, verwende Legacy-Properties", e);
		}
	}

	// Ruft bewusst noch @Deprecated-Legacy-Import-Methoden auf (Speicher + startseiteOptionenAusMap()
	// + bereinigeLegacyStartseiteProperties()); kann entfernt werden, sobald der einmalige Import
	// als abgeschlossen gilt.
	@SuppressWarnings({ "deprecation", "removal" })
	private void ladeStartseiteAusLibreOffice() {
		XComponentContext context = libreOfficeContext;
		if (context == null) {
			return;
		}
		try {
			var speicher = new LibreOfficeStartseiteSpeicher(context);
			if (!speicher.istLegacyImportErledigt()) {
				speicher.importiereLegacy(startseiteOptionenAusMap());
				setStartseiteInLibreOffice(true);
				bereinigeLegacyStartseiteProperties();
			}
			startseiteOptionenInMap(speicher.laden());
			setStartseiteInLibreOffice(true);
		} catch (IllegalStateException e) {
			setStartseiteInLibreOffice(false);
			logger.warn("LibreOffice-Konfiguration für Turnier-Startseite nicht verfügbar, verwende Legacy-Properties", e);
		}
	}

	// Ruft bewusst noch @Deprecated-Legacy-Import-Methoden auf (Speicher + istStartupTurnierModus()
	// + bereinigeLegacyStartupModusProperties()); kann entfernt werden, sobald der einmalige Import
	// als abgeschlossen gilt.
	@SuppressWarnings({ "deprecation", "removal" })
	private void ladeStartupModusAusLibreOffice() {
		XComponentContext context = libreOfficeContext;
		if (context == null) {
			return;
		}
		try {
			var speicher = new LibreOfficeStartupModusSpeicher(context);
			if (!speicher.istLegacyImportErledigt()) {
				speicher.importiereLegacy(isStartupTurnierModus());
				setStartupModusInLibreOffice(true);
				bereinigeLegacyStartupModusProperties();
			}
			startupModusInMap(speicher.laden());
			setStartupModusInLibreOffice(true);
		} catch (IllegalStateException e) {
			setStartupModusInLibreOffice(false);
			logger.warn("LibreOffice-Konfiguration für Startup-Modus nicht verfügbar, verwende Legacy-Properties", e);
		}
	}

	// Ruft bewusst noch @Deprecated-Legacy-Import-Methoden auf (Speicher + getCompositeViewEintraege()
	// + bereinigeLegacyCompositeViewsProperties()); kann entfernt werden, sobald der einmalige Import
	// als abgeschlossen gilt.
	@SuppressWarnings({ "deprecation", "removal" })
	private void ladeCompositeViewsAusLibreOffice() {
		XComponentContext context = libreOfficeContext;
		if (context == null) {
			return;
		}
		try {
			var speicher = new LibreOfficeCompositeViewsSpeicher(context);
			if (!speicher.istLegacyImportErledigt()) {
				var legacyOptionen = compositeViewsOptionenAusEintraegen(isWebserverAktiv(), getCompositeViewEintraege());
				speicher.importiereLegacy(legacyOptionen);
				setCompositeViewsInLibreOffice(true);
				bereinigeLegacyCompositeViewsProperties();
			}
			var optionen = speicher.laden();
			compositeViewsFlatInMap(optionen.aktiv(), compositeViewsEintraegeAusJson(optionen.eintraegeJson()));
			setCompositeViewsInLibreOffice(true);
		} catch (IllegalStateException e) {
			setCompositeViewsInLibreOffice(false);
			logger.warn("LibreOffice-Konfiguration für Composite-Views nicht verfügbar, verwende Legacy-Properties", e);
		}
	}

	/**
	 * Lädt die globalen Tab-Farben-Defaults aus der LibreOffice-Konfiguration. Kein Legacy-Import –
	 * bestehende {@code tabfarbe.*}-Werte aus der Properties-Datei werden nicht übernommen (siehe
	 * {@link #bereinigeLegacyTabFarbenProperties()}). Ist kein LO-Kontext vorhanden oder schlägt der
	 * Zugriff fehl, liefert {@link #getTabFarbe(String, int)} für diese Sitzung den übergebenen
	 * Default-Wert.
	 */
	private void ladeTabFarbenAusLibreOffice() {
		XComponentContext context = libreOfficeContext;
		if (context == null) {
			return;
		}
		try {
			setTabFarbenInLibreOffice(new LibreOfficeTabFarbenSpeicher(context).laden());
		} catch (IllegalStateException e) {
			setTabFarbenNichtInLibreOffice();
			logger.warn("LibreOffice-Konfiguration für Tab-Farben nicht verfügbar, verwende Hardcoded-Defaults", e);
		}
	}

	/**
	 * @deprecated Nur für den einmaligen Legacy-Import in die LibreOffice-Konfiguration
	 *             ({@link #ladePluginOptionenAusLibreOffice()}); kann entfernt werden, sobald
	 *             davon auszugehen ist, dass keine Alt-Installation mehr importiert werden muss.
	 */
	@Deprecated(forRemoval = true)
	private PluginOptionen pluginOptionenAusMap() {
		return new PluginOptionen(
				getBoolean(AUTOSAVE_PROP),
				getBoolean(CREATE_BACKUP_PROP),
				getBoolean(NEW_VERSION_CHECK_PROP),
				getBooleanMitDefault(PROZESSBOX_AUTOMATISCH_ANZEIGEN_PROP, true),
				getBooleanMitDefault(PROZESSBOX_AUTOMATISCH_SCHLIESSEN_PROP, true),
				getBoolean(PERFORMANCE_LOGGING_PROP),
				getLogLevel(),
				getBooleanMitDefault(AUTO_UPDATE_DIALOG_PROP, true));
	}

	private static void pluginOptionenInMap(PluginOptionen optionen) {
		setBooleanProp(AUTOSAVE_PROP, optionen.autosave());
		setBooleanProp(CREATE_BACKUP_PROP, optionen.backup());
		setBooleanProp(NEW_VERSION_CHECK_PROP, optionen.newVersionCheck());
		propMap.put(PROZESSBOX_AUTOMATISCH_ANZEIGEN_PROP,
				Boolean.toString(optionen.prozessBoxAutomatischAnzeigen()));
		propMap.put(PROZESSBOX_AUTOMATISCH_SCHLIESSEN_PROP,
				Boolean.toString(optionen.prozessBoxAutomatischSchliessen()));
		setBooleanProp(PERFORMANCE_LOGGING_PROP, optionen.performanceLogging());
		if (optionen.logLevel().isBlank()) {
			propMap.remove(LOG_LEVEL_PROP);
		} else {
			propMap.put(LOG_LEVEL_PROP, optionen.logLevel());
		}
		setBooleanProp(AUTO_UPDATE_DIALOG_PROP, optionen.autoUpdateDialogBeimStart());
	}

	/**
	 * @deprecated Nur für den einmaligen Legacy-Import in die LibreOffice-Konfiguration
	 *             ({@link #ladeWebserverRegieAusLibreOffice()}); kann entfernt werden, sobald
	 *             davon auszugehen ist, dass keine Alt-Installation mehr importiert werden muss.
	 */
	@Deprecated(forRemoval = true)
	private WebserverRegieOptionen webserverRegieOptionenAusMap() {
		return new WebserverRegieOptionen(
				getBooleanMitDefault(WEBSERVER_REGIE_AKTIV_PROP, true),
				getWebserverRegiePort(),
				propMap.getOrDefault(WEBSERVER_REGIE_ZIELE_PROP, "").trim());
	}

	private static void webserverRegieOptionenInMap(WebserverRegieOptionen optionen) {
		propMap.put(WEBSERVER_REGIE_AKTIV_PROP, Boolean.toString(optionen.aktiv()));
		propMap.put(WEBSERVER_REGIE_PORT_PROP,
				String.valueOf(normierePort(optionen.port(), WEBSERVER_REGIE_DEFAULT_PORT)));
		if (optionen.zieleJson().isBlank()) {
			propMap.remove(WEBSERVER_REGIE_ZIELE_PROP);
		} else {
			propMap.put(WEBSERVER_REGIE_ZIELE_PROP, optionen.zieleJson());
		}
	}

	/**
	 * @deprecated Cleanup-Code für den einmaligen Import der Webserver-Regie-Einstellungen in die
	 *             LibreOffice-Konfiguration; kann entfernt werden, sobald davon auszugehen ist,
	 *             dass keine Alt-Installation diese Schlüssel mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static void bereinigeLegacyWebserverRegieProperties() {
		boolean geaendert = false;
		geaendert |= propMap.remove(WEBSERVER_REGIE_AKTIV_PROP) != null;
		geaendert |= propMap.remove(WEBSERVER_REGIE_PORT_PROP) != null;
		geaendert |= propMap.remove(WEBSERVER_REGIE_ZIELE_PROP) != null;
		if (geaendert) {
			logger.info("Legacy-Webserver-Regie-Properties entfernt");
			speichernDatei();
		}
	}

	/**
	 * @deprecated Nur solange relevant, wie {@link #speichernDatei()} Legacy-Webserver-Regie-Schlüssel
	 *             beim Schreiben der Properties-Datei überspringen muss; kann mit dem restlichen
	 *             Legacy-Import-Mechanismus entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static boolean istWebserverRegieLegacyKey(String key) {
		return WEBSERVER_REGIE_AKTIV_PROP.equals(key)
				|| WEBSERVER_REGIE_PORT_PROP.equals(key)
				|| WEBSERVER_REGIE_ZIELE_PROP.equals(key);
	}

	private static void setWebserverRegieInLibreOffice(boolean wert) {
		webserverRegieInLibreOffice = wert;
	}

	/**
	 * @deprecated Cleanup-Code für den einmaligen Import der Plugin-Optionen in die LibreOffice-Konfiguration;
	 *             kann entfernt werden, sobald davon auszugehen ist, dass keine Alt-Installation diese
	 *             Schlüssel mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static void bereinigeLegacyPluginOptionenProperties() {
		boolean geaendert = false;
		geaendert |= propMap.remove(AUTOSAVE_PROP) != null;
		geaendert |= propMap.remove(CREATE_BACKUP_PROP) != null;
		geaendert |= propMap.remove(NEW_VERSION_CHECK_PROP) != null;
		geaendert |= propMap.remove(PROZESSBOX_AUTOMATISCH_ANZEIGEN_PROP) != null;
		geaendert |= propMap.remove(PROZESSBOX_AUTOMATISCH_SCHLIESSEN_PROP) != null;
		geaendert |= propMap.remove(PERFORMANCE_LOGGING_PROP) != null;
		geaendert |= propMap.remove(LOG_LEVEL_PROP) != null;
		geaendert |= propMap.remove(AUTO_UPDATE_DIALOG_PROP) != null;
		if (geaendert) {
			logger.info("Legacy-Plugin-Optionen-Properties entfernt");
			speichernDatei();
		}
	}

	/**
	 * @deprecated Nur solange relevant, wie {@link #speichernDatei()} Legacy-Plugin-Optionen-Schlüssel
	 *             beim Schreiben der Properties-Datei überspringen muss; kann mit dem restlichen
	 *             Legacy-Import-Mechanismus entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static boolean istPluginOptionenLegacyKey(String key) {
		return AUTOSAVE_PROP.equals(key)
				|| CREATE_BACKUP_PROP.equals(key)
				|| NEW_VERSION_CHECK_PROP.equals(key)
				|| PROZESSBOX_AUTOMATISCH_ANZEIGEN_PROP.equals(key)
				|| PROZESSBOX_AUTOMATISCH_SCHLIESSEN_PROP.equals(key)
				|| PERFORMANCE_LOGGING_PROP.equals(key)
				|| LOG_LEVEL_PROP.equals(key)
				|| AUTO_UPDATE_DIALOG_PROP.equals(key);
	}

	private static void setPluginOptionenInLibreOffice(boolean wert) {
		pluginOptionenInLibreOffice = wert;
	}

	/**
	 * @deprecated Nur solange relevant, wie {@link #speichernDatei()} den bereits nach LibreOffice
	 *             migrierten FTP-Server-Legacy-Schlüssel beim Schreiben der Properties-Datei
	 *             überspringen muss; kann mit dem restlichen Legacy-Import-Mechanismus entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static boolean istFtpServerLegacyKey(String key) {
		return FTP_SERVER_JSON_PROP.equals(key);
	}

	private static void setFtpServerInLibreOffice(boolean wert) {
		ftpServerInLibreOffice = wert;
	}

	/**
	 * @deprecated Nur für den einmaligen Legacy-Import in die LibreOffice-Konfiguration
	 *             ({@link #ladeStartseiteAusLibreOffice()}); kann entfernt werden, sobald davon
	 *             auszugehen ist, dass keine Alt-Installation mehr importiert werden muss.
	 */
	@Deprecated(forRemoval = true)
	private StartseiteOptionen startseiteOptionenAusMap() {
		return new StartseiteOptionen(getStartseitePort(), isStartseiteAktiv(), getStartseiteZoom());
	}

	private static void startseiteOptionenInMap(StartseiteOptionen optionen) {
		propMap.put(STARTSEITE_PORT_PROP, String.valueOf(optionen.port()));
		setBooleanProp(STARTSEITE_AKTIV_PROP, optionen.aktiv());
		propMap.put(STARTSEITE_ZOOM_PROP, String.valueOf(optionen.zoom()));
	}

	/**
	 * @deprecated Cleanup-Code für den einmaligen Import der Turnier-Startseite in die
	 *             LibreOffice-Konfiguration; kann entfernt werden, sobald davon auszugehen ist,
	 *             dass keine Alt-Installation diese Schlüssel mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static void bereinigeLegacyStartseiteProperties() {
		boolean geaendert = false;
		geaendert |= propMap.remove(STARTSEITE_PORT_PROP) != null;
		geaendert |= propMap.remove(STARTSEITE_AKTIV_PROP) != null;
		geaendert |= propMap.remove(STARTSEITE_ZOOM_PROP) != null;
		if (geaendert) {
			logger.info("Legacy-Turnier-Startseite-Properties entfernt");
			speichernDatei();
		}
	}

	/**
	 * @deprecated Nur solange relevant, wie {@link #speichernDatei()} Legacy-Turnier-Startseite-Schlüssel
	 *             beim Schreiben der Properties-Datei überspringen muss; kann mit dem restlichen
	 *             Legacy-Import-Mechanismus entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static boolean istStartseiteLegacyKey(String key) {
		return STARTSEITE_PORT_PROP.equals(key)
				|| STARTSEITE_AKTIV_PROP.equals(key)
				|| STARTSEITE_ZOOM_PROP.equals(key);
	}

	private static void setStartseiteInLibreOffice(boolean wert) {
		startseiteInLibreOffice = wert;
	}

	private static void startupModusInMap(boolean aktiv) {
		setBooleanProp(STARTUP_TURNIER_MODUS_PROP, aktiv);
	}

	/**
	 * @deprecated Cleanup-Code für den einmaligen Import des Startup-Modus in die
	 *             LibreOffice-Konfiguration; kann entfernt werden, sobald davon auszugehen ist,
	 *             dass keine Alt-Installation diesen Schlüssel mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static void bereinigeLegacyStartupModusProperties() {
		if (propMap.remove(STARTUP_TURNIER_MODUS_PROP) != null) {
			logger.info("Legacy-Startup-Modus-Property entfernt");
			speichernDatei();
		}
	}

	/**
	 * @deprecated Nur solange relevant, wie {@link #speichernDatei()} den Legacy-Startup-Modus-Schlüssel
	 *             beim Schreiben der Properties-Datei überspringen muss; kann mit dem restlichen
	 *             Legacy-Import-Mechanismus entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static boolean istStartupModusLegacyKey(String key) {
		return STARTUP_TURNIER_MODUS_PROP.equals(key);
	}

	private static void setStartupModusInLibreOffice(boolean wert) {
		startupModusInLibreOffice = wert;
	}

	private static CompositeViewsOptionen compositeViewsOptionenAusEintraegen(boolean aktiv, List<CompositeViewEintragRoh> eintraege) {
		String json = eintraege == null || eintraege.isEmpty() ? "" : GSON.toJson(eintraege);
		return new CompositeViewsOptionen(aktiv, json);
	}

	private static List<CompositeViewEintragRoh> compositeViewsEintraegeAusJson(String json) {
		if (json == null || json.isBlank()) {
			return new ArrayList<>();
		}
		try {
			var typ = new TypeToken<List<CompositeViewEintragRoh>>() { }.getType();
			List<CompositeViewEintragRoh> gelesen = GSON.fromJson(json, typ);
			return gelesen == null ? new ArrayList<>() : gelesen;
		} catch (RuntimeException e) {
			logger.warn("Fehler beim Lesen der Composite-View-Einträge aus LibreOffice-Konfiguration", e);
			return new ArrayList<>();
		}
	}

	/**
	 * @deprecated Cleanup-Code für den einmaligen Import der Composite-Views in die
	 *             LibreOffice-Konfiguration; kann entfernt werden, sobald davon auszugehen ist,
	 *             dass keine Alt-Installation diese Schlüssel mehr besitzt.
	 */
	@Deprecated(forRemoval = true)
	private static void bereinigeLegacyCompositeViewsProperties() {
		var zuLoeschen = new ArrayList<String>();
		for (var key : propMap.keySet()) {
			if (istCompositeViewsLegacyKey(key)) {
				zuLoeschen.add(key);
			}
		}
		if (zuLoeschen.isEmpty()) {
			return;
		}
		zuLoeschen.forEach(propMap::remove);
		logger.info("{} Legacy-Composite-Views-Property/-ies entfernt", zuLoeschen.size());
		speichernDatei();
	}

	/**
	 * @deprecated Nur solange relevant, wie {@link #speichernDatei()} Legacy-Composite-Views-Schlüssel
	 *             beim Schreiben der Properties-Datei überspringen muss; kann mit dem restlichen
	 *             Legacy-Import-Mechanismus entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static boolean istCompositeViewsLegacyKey(String key) {
		return WEBSERVER_AKTIV_PROP.equals(key)
				|| WEBSERVER_COMPOSITE_PORTS_PROP.equals(key)
				|| key.startsWith(WEBSERVER_COMPOSITE_PREFIX);
	}

	private static void setCompositeViewsInLibreOffice(boolean wert) {
		compositeViewsInLibreOffice = wert;
	}
	// ----------------------------------------------------
	// Getter
	// ----------------------------------------------------

	private boolean getBoolean(String key) {
		return Boolean.parseBoolean(propMap.getOrDefault(key, "false"));
	}

	/** Liest einen boolean-Property; bei fehlendem Schlüssel wird {@code defaultWert} verwendet. */
	private boolean getBooleanMitDefault(String key, boolean defaultWert) {
		var val = propMap.get(key);
		if (val == null || val.isBlank()) return defaultWert;
		return Boolean.parseBoolean(val.trim());
	}

	public boolean isAutoSave() {
		return getBoolean(AUTOSAVE_PROP);
	}

	public boolean isCreateBackup() {
		return getBoolean(CREATE_BACKUP_PROP);
	}

	public boolean isNewVersionCheckImmerTrue() {
		return getBoolean(NEW_VERSION_CHECK_PROP);
	}

	public boolean isAutoUpdateDialogBeimStartAktiv() {
		return getBooleanMitDefault(AUTO_UPDATE_DIALOG_PROP, true);
	}

	/**
	 * Tag-Name der Version, für die der Benutzer "nicht mehr nachfragen" gewählt hat
	 * (z.B. {@code v1.2.3}), oder {@link Optional#empty()} falls nicht gesetzt.
	 */
	public Optional<String> getUpdateSkipVersion() {
		var wert = propMap.get(UPDATE_SKIP_VERSION_PROP);
		return (wert == null || wert.isBlank()) ? Optional.empty() : Optional.of(wert);
	}

	public void setUpdateSkipVersion(String versionTag) {
		propMap.put(UPDATE_SKIP_VERSION_PROP, versionTag);
		speichernDatei();
	}

	public boolean isProzessBoxAutomatischAnzeigen() {
		return getBooleanMitDefault(PROZESSBOX_AUTOMATISCH_ANZEIGEN_PROP, true);
	}

	public boolean isProzessBoxAutomatischSchliessen() {
		return getBooleanMitDefault(PROZESSBOX_AUTOMATISCH_SCHLIESSEN_PROP, true);
	}

	public boolean isPerformanceLogging() {
		return getBoolean(PERFORMANCE_LOGGING_PROP);
	}

	public boolean isWebserverAktiv() {
		return getBoolean(WEBSERVER_AKTIV_PROP);
	}

	/**
	 * Speichert ausschließlich das globale Webserver-Flag. Wird von der Optionsseite
	 * „Composite Views" genutzt, damit das Flag gesetzt werden kann, ohne die
	 * Composite-View-Properties neu zu schreiben.
	 */
	public void speichernWebserverAktiv(boolean aktiv) {
		setBooleanProp(WEBSERVER_AKTIV_PROP, aktiv);
		XComponentContext context = libreOfficeContext;
		if (context != null) {
			try {
				var optionen = compositeViewsOptionenAusEintraegen(aktiv, getCompositeViewEintraege());
				new LibreOfficeCompositeViewsSpeicher(context).speichern(optionen);
				setCompositeViewsInLibreOffice(true);
			} catch (IllegalStateException e) {
				logger.warn("Speichern des Composite-Views-Aktiv-Flags in LibreOffice-Konfiguration fehlgeschlagen, verwende Legacy-Datei", e);
				setCompositeViewsInLibreOffice(false);
				speichernDatei();
			}
		} else {
			speichernDatei();
		}
	}

	public boolean isStartseiteAktiv() {
		return getBoolean(STARTSEITE_AKTIV_PROP);
	}

	public boolean isWebserverRegieAktiv() {
		return getBooleanMitDefault(WEBSERVER_REGIE_AKTIV_PROP, true);
	}

	public int getWebserverRegiePort() {
		var val = propMap.get(WEBSERVER_REGIE_PORT_PROP);
		if (val == null || val.isBlank()) return WEBSERVER_REGIE_DEFAULT_PORT;
		try {
			int port = Integer.parseInt(val.trim());
			if (istGueltigerPort(port)) return port;
		} catch (NumberFormatException e) {
			logger.warn("Ungültiger Webserver-Regie-Port '{}', verwende Default {}", val, WEBSERVER_REGIE_DEFAULT_PORT);
		}
		return WEBSERVER_REGIE_DEFAULT_PORT;
	}

	public List<RegieZielRoh> getWebserverRegieZiele() {
		try {
			var json = propMap.getOrDefault(WEBSERVER_REGIE_ZIELE_PROP, "").trim();
			if (json.isEmpty()) {
				return new ArrayList<>();
			}
			var typ = new TypeToken<List<RegieZielRoh>>() { }.getType();
			List<RegieZielRoh> gelesen = GSON.fromJson(json, typ);
			if (gelesen == null) {
				return new ArrayList<>();
			}
			return validierteRegieZiele(gelesen);
		} catch (RuntimeException e) {
			logger.warn("Fehler beim Lesen der Webserver-Regie-Ziele", e);
			return new ArrayList<>();
		}
	}

	public void speichernWebserverRegie(boolean aktiv, int port, List<RegieZielRoh> ziele) {
		try {
			var validiert = validierteRegieZiele(ziele == null ? List.of() : ziele);
			String zieleJson = validiert.isEmpty() ? "" : GSON.toJson(validiert);
			var optionen = new WebserverRegieOptionen(aktiv, normierePort(port, WEBSERVER_REGIE_DEFAULT_PORT), zieleJson);
			webserverRegieOptionenInMap(optionen);
			XComponentContext context = libreOfficeContext;
			if (context != null) {
				try {
					new LibreOfficeWebserverRegieSpeicher(context).speichern(optionen);
					setWebserverRegieInLibreOffice(true);
				} catch (IllegalStateException e) {
					logger.warn("Speichern der Webserver-Regie in LibreOffice-Konfiguration fehlgeschlagen, verwende Legacy-Datei", e);
					setWebserverRegieInLibreOffice(false);
					speichernDatei();
				}
			} else {
				speichernDatei();
			}
		} catch (RuntimeException e) {
			logger.error("Fehler beim Speichern der Webserver-Regie", e);
		}
	}

	public void speichernWebserverRegieOptionen(boolean aktiv, int port) {
		speichernWebserverRegie(aktiv, port, getWebserverRegieZiele());
	}

	// ----------------------------------------------------
	// FTP/SFTP-Server-Liste
	// ----------------------------------------------------

	public List<FtpServerEintrag> getFtpServerEintraege() {
		try {
			var json = propMap.getOrDefault(FTP_SERVER_JSON_PROP, "").trim();
			if (json.isEmpty()) {
				return new ArrayList<>();
			}
			var typ = new TypeToken<List<FtpServerEintrag>>() { }.getType();
			List<FtpServerEintrag> gelesen = GSON.fromJson(json, typ);
			return gelesen == null ? new ArrayList<>() : gelesen;
		} catch (RuntimeException e) {
			logger.warn("Fehler beim Lesen der FTP-Server-Liste", e);
			return new ArrayList<>();
		}
	}

	public void speichernFtpServer(List<FtpServerEintrag> eintraege) {
		try {
			var liste = eintraege == null ? List.<FtpServerEintrag>of() : eintraege;
			String json = liste.isEmpty() ? "" : GSON.toJson(liste);
			if (json.isBlank()) {
				propMap.remove(FTP_SERVER_JSON_PROP);
			} else {
				propMap.put(FTP_SERVER_JSON_PROP, json);
			}
			XComponentContext context = libreOfficeContext;
			if (context != null) {
				try {
					new LibreOfficeFtpServerSpeicher(context).speichern(json);
					setFtpServerInLibreOffice(true);
				} catch (IllegalStateException e) {
					logger.warn("Speichern der FTP-Server-Liste in LibreOffice-Konfiguration fehlgeschlagen, verwende Legacy-Datei", e);
					setFtpServerInLibreOffice(false);
					speichernDatei();
				}
			} else {
				speichernDatei();
			}
		} catch (RuntimeException e) {
			logger.error("Fehler beim Speichern der FTP-Server-Liste", e);
		}
	}

	public int getStartseitePort() {
		var val = propMap.get(STARTSEITE_PORT_PROP);
		if (val == null || val.isBlank()) return STARTSEITE_DEFAULT_PORT;
		try {
			int port = Integer.parseInt(val.trim());
			if (port >= 1 && port <= 65535) return port;
		} catch (NumberFormatException e) {
			logger.warn("Ungültiger Startseite-Port '{}', verwende Default {}", val, STARTSEITE_DEFAULT_PORT);
		}
		return STARTSEITE_DEFAULT_PORT;
	}

	public int getStartseiteZoom() {
		return parseZoom(propMap.get(STARTSEITE_ZOOM_PROP));
	}

	public void speichernStartseite(int port, boolean aktiv, int zoom) {
		try {
			var optionen = new StartseiteOptionen(port, aktiv, zoom);
			startseiteOptionenInMap(optionen);
			XComponentContext context = libreOfficeContext;
			if (context != null) {
				try {
					new LibreOfficeStartseiteSpeicher(context).speichern(optionen);
					setStartseiteInLibreOffice(true);
				} catch (IllegalStateException e) {
					logger.warn("Speichern der Turnier-Startseite in LibreOffice-Konfiguration fehlgeschlagen, verwende Legacy-Datei", e);
					setStartseiteInLibreOffice(false);
					speichernDatei();
				}
			} else {
				speichernDatei();
			}
		} catch (RuntimeException e) {
			logger.error("Fehler beim Speichern der Turnier-Startseite", e);
		}
	}

	private static List<RegieZielRoh> validierteRegieZiele(List<RegieZielRoh> ziele) {
		var result = new ArrayList<RegieZielRoh>();
		Set<String> slugs = new HashSet<>();
		for (var ziel : ziele) {
			if (ziel == null || ziel.name().isBlank()) {
				continue;
			}
			var normalisiert = new RegieZielRoh(
					ziel.id(), ziel.name(), RegieSlug.ausName(ziel.name()),
					ziel.aktiv(), ziel.viewId());
			RegieSlug.validiere(normalisiert.slug());
			if (!slugs.add(normalisiert.slug())) {
				throw new IllegalArgumentException("Doppelter Webserver-Regie-Slug: " + normalisiert.slug());
			}
			result.add(normalisiert);
		}
		return result;
	}

	public boolean isStartupTurnierModus() {
		return getBoolean(STARTUP_TURNIER_MODUS_PROP);
	}

	public void setStartupTurnierModus(boolean aktiv) {
		startupModusInMap(aktiv);
		XComponentContext context = libreOfficeContext;
		if (context != null) {
			try {
				new LibreOfficeStartupModusSpeicher(context).speichern(aktiv);
				setStartupModusInLibreOffice(true);
			} catch (IllegalStateException e) {
				logger.warn("Speichern des Startup-Modus in LibreOffice-Konfiguration fehlgeschlagen, verwende Legacy-Datei", e);
				setStartupModusInLibreOffice(false);
				speichernDatei();
			}
		} else {
			speichernDatei();
		}
	}

	public String getLogLevel() {
		return propMap.getOrDefault(LOG_LEVEL_PROP, "").trim();
	}

	// ----------------------------------------------------
	// Composite View-Konfiguration
	// ----------------------------------------------------

	/**
	 * Gibt alle gespeicherten Composite View-Einträge zurück (aktiv und inaktiv).
	 */
	public List<CompositeViewEintragRoh> getCompositeViewEintraege() {
		List<CompositeViewEintragRoh> eintraege = new ArrayList<>();
		try {
			var portsStr = propMap.getOrDefault(WEBSERVER_COMPOSITE_PORTS_PROP, "").trim();
			if (portsStr.isEmpty()) return eintraege;

			for (var portStr : portsStr.split(",")) {
				portStr = portStr.trim();
				if (portStr.isEmpty()) continue;
				try {
					int port = Integer.parseInt(portStr);
					boolean aktiv = getBoolean(WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_AKTIV_SUFFIX);
					String name = propMap.getOrDefault(WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_NAME_SUFFIX, "").trim();
					int zoom = parseZoom(propMap.get(WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_ZOOM_SUFFIX));
					boolean mitHeaderFooter = getBooleanMitDefault(
							WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_MIT_HEADER_FOOTER_SUFFIX, true);
					// Layout (Split-Baum) ist optional: fehlt es (z.B. ältere Zwischenstände), wird
					// beim Konfig-Aufbau ein Default-Layout erzeugt – der Eintrag bleibt erhalten.
					String layoutJson = propMap.getOrDefault(
							WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_LAYOUT_SUFFIX, "").trim();
					RandKonfiguration rand = parseRand(WEBSERVER_COMPOSITE_PREFIX + port);

					int panelCount = 0;
					try {
						panelCount = Integer.parseInt(propMap.getOrDefault(
								WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_PANEL_COUNT_SUFFIX, "0").trim());
					} catch (NumberFormatException ignored) {
					}

					List<PanelEintragRoh> panels = new ArrayList<>();
					for (int i = 0; i < panelCount; i++) {
						String panelPrefix = WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_PANEL_INFIX + i;
						PanelTyp panelTyp = parsePanelTyp(propMap.get(panelPrefix + WEBSERVER_COMPOSITE_PANEL_TYP_SUFFIX));
						String sheetConfig = propMap.getOrDefault(panelPrefix + WEBSERVER_COMPOSITE_PANEL_SHEET_SUFFIX, "").trim();
						String externeUrl = propMap.getOrDefault(panelPrefix + WEBSERVER_COMPOSITE_PANEL_URL_SUFFIX, "").trim();
						if (panelTyp == PanelTyp.BLATT && sheetConfig.isEmpty()) continue;
						if ((panelTyp == PanelTyp.URL || panelTyp == PanelTyp.STATISCHE_DATEI)
								&& externeUrl.isEmpty()) continue;
						int panelZoom = parseZoom(propMap.get(panelPrefix + WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX));
						int sichtbarerTabellenAnteil = parseSichtbarerTabellenAnteil(
								propMap.get(panelPrefix + WEBSERVER_COMPOSITE_PANEL_SICHTBARER_TABELLENANTEIL_SUFFIX));
						String panelHAlign = propMap.get(panelPrefix + WEBSERVER_COMPOSITE_PANEL_HALIGN_SUFFIX);
						String panelVAlign = propMap.get(panelPrefix + WEBSERVER_COMPOSITE_PANEL_VALIGN_SUFFIX);
						if (panelHAlign == null && panelVAlign == null
								&& getBoolean(panelPrefix + LEGACY_WEBSERVER_COMPOSITE_PANEL_ZENTRIERT_SUFFIX)) {
							// Migration: alte boolean-Property "_zentriert" → Horizontal=Mitte
							panelHAlign = PanelAusrichtung.H_MITTE;
						}
						boolean panelBlattnameAnzeigen = getBoolean(panelPrefix + WEBSERVER_COMPOSITE_PANEL_BLATTNAME_SUFFIX);
						panels.add(new PanelEintragRoh(panelTyp, sheetConfig, panelZoom, sichtbarerTabellenAnteil,
								panelHAlign, panelVAlign, panelBlattnameAnzeigen, externeUrl));
					}
					if (!panels.isEmpty()) {
						eintraege.add(new CompositeViewEintragRoh(port, name, aktiv, zoom, mitHeaderFooter, layoutJson, panels, rand));
					}
				} catch (Exception e) {
					logger.warn("Ungültiger Composite-Port-Eintrag '{}'", portStr, e);
				}
			}
		} catch (Exception e) {
			logger.warn("Fehler beim Lesen der Composite-View-Einträge", e);
		}
		return eintraege;
	}

	/**
	 * Gibt alle aktiven Composite Views als fertige Konfigurationsobjekte zurück.
	 */
	public List<CompositeViewKonfiguration> getCompositeViewKonfigurationen() {
		List<CompositeViewKonfiguration> konfigs = new ArrayList<>();
		var gson = new GsonBuilder()
				.registerTypeAdapter(SplitKnoten.class, new SplitKnotenAdapter())
				.create();
		for (var eintrag : getCompositeViewEintraege()) {
			if (!eintrag.aktiv()) continue;
			try {
				SplitKnoten wurzel = null;
				if (!eintrag.layoutJson().isBlank()) {
					try {
						wurzel = gson.fromJson(eintrag.layoutJson(), SplitKnoten.class);
					} catch (RuntimeException e) {
						logger.warn("Ungültiger Layout-JSON für Port {}, verwende Default-Layout", eintrag.port());
					}
				}
				List<PanelKonfiguration> panels = new ArrayList<>();
				for (var p : eintrag.panels()) {
					if (p.typ() == PanelTyp.TIMER) {
						panels.add(new PanelKonfiguration(PanelTyp.TIMER, "", null, p.zoom(),
								p.sichtbarerTabellenAnteil(), p.horizontalAusrichtung(), p.vertikalAusrichtung(), false, ""));
						continue;
					}
					if (p.typ() == PanelTyp.URL) {
						panels.add(new PanelKonfiguration(PanelTyp.URL, "", null, p.zoom(),
								p.sichtbarerTabellenAnteil(), p.horizontalAusrichtung(), p.vertikalAusrichtung(), p.blattnameAnzeigen(), p.externeUrl()));
						continue;
					}
					if (p.typ() == PanelTyp.STATISCHE_DATEI) {
						panels.add(new PanelKonfiguration(PanelTyp.STATISCHE_DATEI, "", null, p.zoom(),
								p.sichtbarerTabellenAnteil(), p.horizontalAusrichtung(), p.vertikalAusrichtung(), false, p.externeUrl()));
						continue;
					}
					if (p.typ() == PanelTyp.TURNIERSTARTSEITE) {
						panels.add(new PanelKonfiguration(PanelTyp.TURNIERSTARTSEITE, "", null, p.zoom(),
								p.sichtbarerTabellenAnteil(), p.horizontalAusrichtung(), p.vertikalAusrichtung(), false, ""));
						continue;
					}
					var resolver = SheetResolverFactory.erstellen(p.sheetConfig());
					if (resolver == null) {
						logger.warn("Resolver null für Panel-Config '{}'", p.sheetConfig());
						continue;
					}
					panels.add(new PanelKonfiguration(PanelTyp.BLATT, p.sheetConfig(), resolver, p.zoom(),
							p.sichtbarerTabellenAnteil(), p.horizontalAusrichtung(), p.vertikalAusrichtung(), p.blattnameAnzeigen(), ""));
				}
				if (!panels.isEmpty()) {
					if (wurzel == null) {
						wurzel = defaultSplitLayout(panels.size());
					}
					konfigs.add(new CompositeViewKonfiguration(eintrag.port(), eintrag.name(), eintrag.zoom(), wurzel, panels,
							eintrag.mitHeaderFooter(), eintrag.rand()));
				}
			} catch (Exception e) {
				logger.error("Fehler bei Composite-View-Konfiguration {}", eintrag, e);
			}
		}
		return konfigs;
	}

	/**
	 * Erzeugt ein Default-Split-Layout für {@code panelCount} Panels (links-lehnende
	 * Horizontal-Teilung), falls kein gespeichertes Layout vorliegt.
	 */
	private static SplitKnoten defaultSplitLayout(int panelCount) {
		SplitKnoten wurzel = new SplitBlatt(0);
		for (int i = 1; i < panelCount; i++) {
			wurzel = SplitTeilung.horizontal(wurzel, new SplitBlatt(i));
		}
		return wurzel;
	}

	/**
	 * Schreibt das globale Webserver-Aktiv-Flag sowie alle Composite View-Einträge als Flat-Keys
	 * in die {@link #propMap} (Legacy-Struktur). Löscht zuvor alle alten Composite-Einträge.
	 * Persistiert nicht selbst – das übernimmt der Aufrufer ({@link #speichernDatei()} oder
	 * {@link LibreOfficeCompositeViewsSpeicher}).
	 */
	private static void compositeViewsFlatInMap(boolean aktiv, List<CompositeViewEintragRoh> eintraege) {
		setBooleanProp(WEBSERVER_AKTIV_PROP, aktiv);
		// Alte Einträge löschen
		for (var key : new ArrayList<>(propMap.keySet())) {
			if (key.startsWith(WEBSERVER_COMPOSITE_PREFIX)) {
				propMap.remove(key);
			}
		}
		propMap.remove(WEBSERVER_COMPOSITE_PORTS_PROP);

		if (eintraege.isEmpty()) {
			return;
		}
		var ports = new StringBuilder();
		for (var eintrag : eintraege) {
			if (!ports.isEmpty()) ports.append(",");
			ports.append(eintrag.port());
			String prefix = WEBSERVER_COMPOSITE_PREFIX + eintrag.port();
			if (eintrag.aktiv())
				propMap.put(prefix + WEBSERVER_COMPOSITE_AKTIV_SUFFIX, "true");
			if (!eintrag.name().isBlank())
				propMap.put(prefix + WEBSERVER_COMPOSITE_NAME_SUFFIX, eintrag.name().trim());
			if (eintrag.zoom() != DEFAULT_ZOOM)
				propMap.put(prefix + WEBSERVER_COMPOSITE_ZOOM_SUFFIX, String.valueOf(eintrag.zoom()));
			// Default = true → nur explizit "false" persistieren (migrationssicher).
			if (!eintrag.mitHeaderFooter())
				propMap.put(prefix + WEBSERVER_COMPOSITE_MIT_HEADER_FOOTER_SUFFIX, "false");
			if (eintrag.layoutJson() != null && !eintrag.layoutJson().isBlank())
				propMap.put(prefix + WEBSERVER_COMPOSITE_LAYOUT_SUFFIX, eintrag.layoutJson());
			// Default = RandKonfiguration.KEINER → nur bei Abweichung persistieren (migrationssicher).
			var rand = eintrag.rand();
			if (rand.dicke() != 0)
				propMap.put(prefix + WEBSERVER_COMPOSITE_RAND_DICKE_SUFFIX, String.valueOf(rand.dicke()));
			if (!RandKonfiguration.ART_KEIN.equals(rand.art()))
				propMap.put(prefix + WEBSERVER_COMPOSITE_RAND_ART_SUFFIX, rand.art());
			if (rand.farbe() != 0x000000)
				propMap.put(prefix + WEBSERVER_COMPOSITE_RAND_FARBE_SUFFIX, String.format("%06x", rand.farbe()));
			if (rand.transparenz() != 0)
				propMap.put(prefix + WEBSERVER_COMPOSITE_RAND_TRANSPARENZ_SUFFIX, String.valueOf(rand.transparenz()));
			if (!RandKonfiguration.ANIMATION_KEINE.equals(rand.animation()))
				propMap.put(prefix + WEBSERVER_COMPOSITE_RAND_ANIMATION_SUFFIX, rand.animation());
			propMap.put(prefix + WEBSERVER_COMPOSITE_PANEL_COUNT_SUFFIX, String.valueOf(eintrag.panels().size()));
			for (int i = 0; i < eintrag.panels().size(); i++) {
				var panel = eintrag.panels().get(i);
				String panelPrefix = prefix + WEBSERVER_COMPOSITE_PANEL_INFIX + i;
				propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_TYP_SUFFIX, panel.typ().name());
				if (panel.typ() == PanelTyp.URL || panel.typ() == PanelTyp.STATISCHE_DATEI) {
					propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_URL_SUFFIX, panel.externeUrl());
				} else if (panel.typ() == PanelTyp.TURNIERSTARTSEITE) {
					// Keine zusätzliche Quelle: das Panel nutzt die bestehende Startseiten-Konfiguration.
				} else {
					propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_SHEET_SUFFIX, panel.sheetConfig());
				}
				if (panel.zoom() != DEFAULT_ZOOM)
					propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX, String.valueOf(panel.zoom()));
				if (panel.sichtbarerTabellenAnteil() != DEFAULT_SICHTBARER_TABELLENANTEIL)
					propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_SICHTBARER_TABELLENANTEIL_SUFFIX,
							String.valueOf(panel.sichtbarerTabellenAnteil()));
				if (!PanelAusrichtung.KEIN.equals(panel.horizontalAusrichtung()))
					propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_HALIGN_SUFFIX, panel.horizontalAusrichtung());
				if (!PanelAusrichtung.KEIN.equals(panel.vertikalAusrichtung()))
					propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_VALIGN_SUFFIX, panel.vertikalAusrichtung());
				if (panel.blattnameAnzeigen())
					propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_BLATTNAME_SUFFIX, "true");
			}
		}
		propMap.put(WEBSERVER_COMPOSITE_PORTS_PROP, ports.toString());
	}

	/**
	 * Speichert alle Composite View-Einträge sowie das globale Webserver-Aktiv-Flag.
	 */
	public void speichernCompositeViews(boolean aktiv, List<CompositeViewEintragRoh> eintraege) {
		try {
			var normalisierteEintraege = eintraege == null ? List.<CompositeViewEintragRoh>of() : eintraege;
			compositeViewsFlatInMap(aktiv, normalisierteEintraege);
			XComponentContext context = libreOfficeContext;
			if (context != null) {
				try {
					var optionen = compositeViewsOptionenAusEintraegen(aktiv, normalisierteEintraege);
					new LibreOfficeCompositeViewsSpeicher(context).speichern(optionen);
					setCompositeViewsInLibreOffice(true);
				} catch (IllegalStateException e) {
					logger.warn("Speichern der Composite-Views in LibreOffice-Konfiguration fehlgeschlagen, verwende Legacy-Datei", e);
					setCompositeViewsInLibreOffice(false);
					speichernDatei();
				}
			} else {
				speichernDatei();
			}
		} catch (Exception e) {
			logger.error("Fehler beim Speichern der Composite Views", e);
		}
	}

	// ----------------------------------------------------
	// Speichern
	// ----------------------------------------------------

	public void speichern(boolean autosave, boolean backup, boolean newVersionCheck,
			boolean prozessBoxAutomatischAnzeigen, boolean prozessBoxAutomatischSchliessen,
			boolean performanceLogging, String logLevel, boolean autoUpdateDialogBeimStart) {
		try {
			PluginOptionen optionen = new PluginOptionen(autosave, backup, newVersionCheck,
					prozessBoxAutomatischAnzeigen, prozessBoxAutomatischSchliessen,
					performanceLogging, logLevel, autoUpdateDialogBeimStart);
			pluginOptionenInMap(optionen);
			XComponentContext context = libreOfficeContext;
			if (context != null) {
				try {
					new LibreOfficePluginOptionenSpeicher(context).speichern(optionen);
					setPluginOptionenInLibreOffice(true);
				} catch (IllegalStateException e) {
					logger.warn("Speichern in LibreOffice-Konfiguration fehlgeschlagen, verwende Legacy-Datei", e);
					setPluginOptionenInLibreOffice(false);
					speichernDatei();
				}
			} else {
				speichernDatei();
			}
			safeSetLogLevel();
			PerfLog.invalidateCache();

		} catch (Exception e) {
			logger.error("Fehler beim Speichern", e);
		}
	}

	// ----------------------------------------------------
	// Extras
	// ----------------------------------------------------

	/**
	 * Liest den globalen Tab-Farben-Default aus der LibreOffice-Konfiguration.
	 *
	 * @param konfigPropKey Config-Property-Key, z.B. "Tab-Farbe Meldeliste"
	 * @param defaultVal    Fallback, falls kein LO-Kontext verfügbar oder der Key unbekannt ist
	 * @return gespeicherter globaler Tab-Farben-Default oder {@code defaultVal}
	 */
	public int getTabFarbe(String konfigPropKey, int defaultVal) {
		if (!tabFarbenInLibreOffice) {
			return defaultVal;
		}
		String xcuPropName = TABFARBE_KEY_MAPPING.get(konfigPropKey);
		if (xcuPropName == null) {
			return defaultVal;
		}
		Integer wert = tabFarbenCache.get(xcuPropName);
		return wert == null ? defaultVal : wert;
	}

	/**
	 * Setzt mehrere globale Tab-Farben-Defaults in einem Rutsch und persistiert sie mit einem
	 * einzigen Commit in der LibreOffice-Konfiguration. Ohne LO-Kontext ist dies ein No-Op (die
	 * Options-Seite kann ohnehin nur innerhalb von LibreOffice geöffnet werden).
	 *
	 * @param konfigPropKeyZuFarbe Config-Property-Key (z.B. "Tab-Farbe Meldeliste") auf neuen Farbwert (0xRRGGBB)
	 */
	public void setzeTabFarben(Map<String, Integer> konfigPropKeyZuFarbe) {
		XComponentContext context = libreOfficeContext;
		if (context == null) {
			logger.warn("Kein LibreOffice-Kontext, Tab-Farben können nicht gespeichert werden");
			return;
		}
		try {
			var neueWerte = new HashMap<>(tabFarbenCache);
			for (var eintrag : konfigPropKeyZuFarbe.entrySet()) {
				String xcuPropName = TABFARBE_KEY_MAPPING.get(eintrag.getKey());
				if (xcuPropName != null) {
					neueWerte.put(xcuPropName, eintrag.getValue());
				}
			}
			new LibreOfficeTabFarbenSpeicher(context).speichern(neueWerte);
			setTabFarbenInLibreOffice(neueWerte);
		} catch (IllegalStateException e) {
			logger.warn("Tab-Farben konnten nicht in LibreOffice-Konfiguration gespeichert werden", e);
		}
	}

	private static void setTabFarbenInLibreOffice(Map<String, Integer> werte) {
		tabFarbenCache = Map.copyOf(werte);
		tabFarbenInLibreOffice = true;
	}

	private static void setTabFarbenNichtInLibreOffice() {
		tabFarbenInLibreOffice = false;
		tabFarbenCache = Map.of();
	}

	private static void setBooleanProp(String key, boolean value) {
		if (value) propMap.put(key, "true");
		else propMap.remove(key);
	}

	private static PanelTyp parsePanelTyp(String value) {
		if (value == null || value.isBlank()) return PanelTyp.BLATT;
		try {
			return PanelTyp.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			logger.warn("Unbekannter PanelTyp '{}', verwende BLATT", value.trim());
			return PanelTyp.BLATT;
		}
	}

	private static int parseZoom(String value) {
		if (value == null || value.isBlank()) return DEFAULT_ZOOM;
		try {
			int zoom = Integer.parseInt(value.trim());
			if (zoom >= 10 && zoom <= 500) return zoom;
			logger.warn("Zoom-Wert außerhalb des erlaubten Bereichs (10-500): {}, verwende Standard {}", zoom, DEFAULT_ZOOM);
			return DEFAULT_ZOOM;
		} catch (NumberFormatException e) {
			logger.warn("Ungültiger Zoom-Wert '{}', verwende Standard {}", value.trim(), DEFAULT_ZOOM);
			return DEFAULT_ZOOM;
		}
	}

	static int normierePort(int port, int defaultWert) {
		return istGueltigerPort(port) ? port : defaultWert;
	}

	private static boolean istGueltigerPort(int port) {
		return port >= 1 && port <= 65535;
	}

	private static int parseSichtbarerTabellenAnteil(String value) {
		if (value == null || value.isBlank()) return DEFAULT_SICHTBARER_TABELLENANTEIL;
		try {
			return normiereSichtbarerTabellenAnteil(Integer.parseInt(value.trim()));
		} catch (NumberFormatException e) {
			logger.warn("Ungültiger sichtbarer Tabellenanteil '{}', verwende Standard {}",
					value.trim(), DEFAULT_SICHTBARER_TABELLENANTEIL);
			return DEFAULT_SICHTBARER_TABELLENANTEIL;
		}
	}

	private static int normiereSichtbarerTabellenAnteil(int wert) {
		if (wert >= 10 && wert <= 100) return wert;
		logger.warn("Sichtbarer Tabellenanteil außerhalb des erlaubten Bereichs (10-100): {}, verwende Standard {}",
				wert, DEFAULT_SICHTBARER_TABELLENANTEIL);
		return DEFAULT_SICHTBARER_TABELLENANTEIL;
	}

	/**
	 * Liest die Rand-Konfiguration (Gesamtrahmen) eines Composite View unter dem gegebenen
	 * Property-Präfix. Fehlende Keys ergeben {@link RandKonfiguration#KEINER} (kein Rahmen) –
	 * migrationssicher für Alt-Configs ohne Rand-Properties.
	 */
	private static RandKonfiguration parseRand(String prefix) {
		String dickeStr = propMap.get(prefix + WEBSERVER_COMPOSITE_RAND_DICKE_SUFFIX);
		String art = propMap.get(prefix + WEBSERVER_COMPOSITE_RAND_ART_SUFFIX);
		String farbeStr = propMap.get(prefix + WEBSERVER_COMPOSITE_RAND_FARBE_SUFFIX);
		String transparenzStr = propMap.get(prefix + WEBSERVER_COMPOSITE_RAND_TRANSPARENZ_SUFFIX);
		String animation = propMap.get(prefix + WEBSERVER_COMPOSITE_RAND_ANIMATION_SUFFIX);
		if (dickeStr == null && art == null && farbeStr == null && transparenzStr == null && animation == null) {
			return RandKonfiguration.KEINER;
		}
		int dicke = parseIntOderDefault(dickeStr, 0);
		int farbe = parseHexFarbeOderDefault(farbeStr, 0x000000);
		int transparenz = parseIntOderDefault(transparenzStr, 0);
		return new RandKonfiguration(dicke, art, farbe, transparenz, animation);
	}

	private static int parseIntOderDefault(String value, int defaultWert) {
		if (value == null || value.isBlank()) return defaultWert;
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			logger.warn("Ungültiger Ganzzahl-Wert '{}', verwende Standard {}", value.trim(), defaultWert);
			return defaultWert;
		}
	}

	private static int parseHexFarbeOderDefault(String value, int defaultWert) {
		if (value == null || value.isBlank()) return defaultWert;
		try {
			return Integer.parseInt(value.trim(), 16);
		} catch (NumberFormatException e) {
			logger.warn("Ungültiger Rand-Farbwert '{}', verwende Standard {}", value.trim(), defaultWert);
			return defaultWert;
		}
	}

	private void safeSetLogLevel() {
		try {
			var logLevel = getLogLevel();
			Level level = switch (logLevel) {
				case "trace" -> Level.TRACE;
				case "debug" -> Level.DEBUG;
				case "info" -> Level.INFO;
				case "warn" -> Level.WARN;
				case "error" -> Level.ERROR;
				case "" -> Level.INFO;
				default -> {
					logger.warn("Unbekanntes LogLevel: {}", logLevel);
					yield null;
				}
			};
			if (level != null) {
				// setLevel(String, Level) ruft intern ctx.updateLoggers() auf und propagiert
				// die Änderung an alle bereits erzeugten Logger-Instanzen.
				Configurator.setLevel(Log4J.LOGGERNAME, level);
			}
		} catch (Exception e) {
			logger.error("Fehler beim Setzen des LogLevels", e);
		}
	}


	// nur für Tests
	static void resetForTest() {
		instance = null;
		propMap.clear();
		libreOfficeContext = null;
		setWebserverRegieInLibreOffice(false);
		setPluginOptionenInLibreOffice(false);
		setFtpServerInLibreOffice(false);
		setStartseiteInLibreOffice(false);
		setStartupModusInLibreOffice(false);
		setCompositeViewsInLibreOffice(false);
		setTabFarbenNichtInLibreOffice();
	}
}
