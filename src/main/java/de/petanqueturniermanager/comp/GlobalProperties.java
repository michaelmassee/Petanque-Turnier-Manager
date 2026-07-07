package de.petanqueturniermanager.comp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import de.petanqueturniermanager.webserver.CompositeViewKonfiguration;
import de.petanqueturniermanager.webserver.PanelAusrichtung;
import de.petanqueturniermanager.webserver.PanelKonfiguration;
import de.petanqueturniermanager.webserver.PanelTyp;
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
	private static final String PROZESSBOX_AUTOMATISCH_ANZEIGEN_PROP = "prozessbox.automatisch.anzeigen";
	private static final String PROZESSBOX_AUTOMATISCH_SCHLIESSEN_PROP = "prozessbox.automatisch.schliessen";
	private static final String PERFORMANCE_LOGGING_PROP = "performance.logging";

	private static final String WEBSERVER_AKTIV_PROP = "webserver_aktiv";

	private static final String WEBSERVER_REGIE_AKTIV_PROP = "webserver_regie_aktiv";
	private static final String WEBSERVER_REGIE_PORT_PROP = "webserver_regie_port";
	private static final String WEBSERVER_REGIE_ZIELE_PROP = "webserver_regie_ziele";
	public static final int WEBSERVER_REGIE_DEFAULT_PORT = 9090;

	/** Legacy-Property-Präfix der entfernten Einzel-Port-Konfiguration (nur für Cleanup beim Start). */
	private static final String LEGACY_WEBSERVER_PORTS_PROP = "webserver_ports";
	/** Legacy-Präfix für Einzel-Port-Einträge (nur für Cleanup beim Start). */
	private static final String LEGACY_WEBSERVER_PORT_PREFIX = "webserver_port_";
	/** Legacy-Property der entfernten globalen „Blattnamen anzeigen"-Option (nur für Cleanup beim Start). */
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
	/** Legacy-Suffix der entfernten boolean-Property „zentriert" (nur für Migration / Cleanup). */
	private static final String LEGACY_WEBSERVER_COMPOSITE_PANEL_ZENTRIERT_SUFFIX = "_zentriert";
	private static final String WEBSERVER_COMPOSITE_PANEL_HALIGN_SUFFIX = "_halign";
	private static final String WEBSERVER_COMPOSITE_PANEL_VALIGN_SUFFIX = "_valign";
	private static final String WEBSERVER_COMPOSITE_PANEL_BLATTNAME_SUFFIX = "_blattname";
	private static final String WEBSERVER_COMPOSITE_PANEL_TYP_SUFFIX = "_typ";
	private static final String WEBSERVER_COMPOSITE_PANEL_URL_SUFFIX = "_url";

	private static final String STARTUP_TURNIER_MODUS_PROP = "startup.turnier.modus";

	// Turnier-Startseite (dedizierter Webserver, läuft parallel zu den Composite-Views)
	private static final String STARTSEITE_PORT_PROP  = "startseite_port";
	private static final String STARTSEITE_AKTIV_PROP = "startseite_aktiv";
	private static final String STARTSEITE_ZOOM_PROP  = "startseite_zoom";
	public static final int STARTSEITE_DEFAULT_PORT = 9200;

	// Timer
	private static final String TIMER_DAUER_PROP            = "timer_letzte_dauer";
	private static final String TIMER_PORT_PROP             = "timer_letzter_port";
	private static final String TIMER_BEZEICHNUNG_PROP      = "timer_letzte_bezeichnung";
	private static final String TIMER_HINTERGRUNDFARBE_PROP = "timer_hintergrundfarbe";

	public static final int DEFAULT_ZOOM = 100;
	public static final int DEFAULT_SICHTBARER_TABELLENANTEIL = 100;

	// zentrale Runtime-Map
	private static final ConcurrentHashMap<String, String> propMap = new ConcurrentHashMap<>();

	private static volatile GlobalProperties instance = null;

	private static final ReentrantLock fileLock = new ReentrantLock();
	private static volatile XComponentContext libreOfficeContext;

	private static final String TABFARBE_PRAEFIX = "tabfarbe.";
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
	 */
	public record CompositeViewEintragRoh(
			int port, String name, boolean aktiv, int zoom, boolean mitHeaderFooter,
			String layoutJson,
			List<PanelEintragRoh> panels) {
		public CompositeViewEintragRoh {
			name = name == null ? "" : name;
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
		bereinigeLegacyEinzelPortProperties();
		safeSetLogLevel();
	}

	public static void setLibreOfficeContext(XComponentContext context) {
		libreOfficeContext = context;
		GlobalProperties lokaleInstanz = instance;
		if (lokaleInstanz != null) {
			lokaleInstanz.ladePluginOptionenAusLibreOffice();
			lokaleInstanz.safeSetLogLevel();
		}
	}

	/**
	 * Entfernt einmalig alle Legacy-Properties der entfernten Einzel-Port-Webserver-Konfiguration
	 * ({@code webserver_ports}, {@code webserver_port_*}) aus {@link #propMap} und persistiert,
	 * falls etwas zu löschen war. Keine Migration – Altdaten werden stillschweigend verworfen.
	 */
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

	private void ladePluginOptionenAusLibreOffice() {
		XComponentContext context = libreOfficeContext;
		if (context == null) {
			return;
		}
		try {
			var speicher = new LibreOfficePluginOptionenSpeicher(context);
			if (!speicher.istLegacyImportErledigt()) {
				speicher.importiereLegacy(Map.copyOf(propMap), pluginOptionenAusMap());
			}
			pluginOptionenInMap(speicher.laden());
		} catch (IllegalStateException e) {
			logger.warn("LibreOffice-Konfiguration nicht verfügbar, verwende Legacy-Properties", e);
		}
	}

	private PluginOptionen pluginOptionenAusMap() {
		return new PluginOptionen(
				getBoolean(AUTOSAVE_PROP),
				getBoolean(CREATE_BACKUP_PROP),
				getBoolean(NEW_VERSION_CHECK_PROP),
				getBooleanMitDefault(PROZESSBOX_AUTOMATISCH_ANZEIGEN_PROP, true),
				getBooleanMitDefault(PROZESSBOX_AUTOMATISCH_SCHLIESSEN_PROP, true),
				getBoolean(PERFORMANCE_LOGGING_PROP),
				getLogLevel());
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
			if (port >= 1 && port <= 65535) return port;
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
			setBooleanProp(WEBSERVER_REGIE_AKTIV_PROP, aktiv);
			propMap.put(WEBSERVER_REGIE_PORT_PROP, String.valueOf(port));
			var validiert = validierteRegieZiele(ziele == null ? List.of() : ziele);
			if (validiert.isEmpty()) {
				propMap.remove(WEBSERVER_REGIE_ZIELE_PROP);
			} else {
				propMap.put(WEBSERVER_REGIE_ZIELE_PROP, GSON.toJson(validiert));
			}
			speichernDatei();
		} catch (RuntimeException e) {
			logger.error("Fehler beim Speichern der Webserver-Regie", e);
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
		propMap.put(STARTSEITE_PORT_PROP, String.valueOf(port));
		setBooleanProp(STARTSEITE_AKTIV_PROP, aktiv);
		propMap.put(STARTSEITE_ZOOM_PROP, String.valueOf(zoom));
		speichernDatei();
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
		setBooleanProp(STARTUP_TURNIER_MODUS_PROP, aktiv);
		speichernDatei();
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
						eintraege.add(new CompositeViewEintragRoh(port, name, aktiv, zoom, mitHeaderFooter, layoutJson, panels));
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
					konfigs.add(new CompositeViewKonfiguration(eintrag.port(), eintrag.name(), eintrag.zoom(), wurzel, panels, eintrag.mitHeaderFooter()));
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
	 * Speichert alle Composite View-Einträge sowie das globale Webserver-Aktiv-Flag
	 * in der Properties-Datei. Löscht zuvor alle alten Composite-Einträge.
	 */
	public void speichernCompositeViews(boolean aktiv, List<CompositeViewEintragRoh> eintraege) {
		try {
			setBooleanProp(WEBSERVER_AKTIV_PROP, aktiv);
			// Alte Einträge löschen
			for (var alt : getCompositeViewEintraege()) {
				String prefix = WEBSERVER_COMPOSITE_PREFIX + alt.port();
				propMap.remove(prefix + WEBSERVER_COMPOSITE_AKTIV_SUFFIX);
				propMap.remove(prefix + WEBSERVER_COMPOSITE_NAME_SUFFIX);
				propMap.remove(prefix + WEBSERVER_COMPOSITE_ZOOM_SUFFIX);
				propMap.remove(prefix + WEBSERVER_COMPOSITE_MIT_HEADER_FOOTER_SUFFIX);
				propMap.remove(prefix + WEBSERVER_COMPOSITE_LAYOUT_SUFFIX);
				propMap.remove(prefix + WEBSERVER_COMPOSITE_PANEL_COUNT_SUFFIX);
				for (int i = 0; i < alt.panels().size(); i++) {
					String panelPrefix = prefix + WEBSERVER_COMPOSITE_PANEL_INFIX + i;
					propMap.remove(panelPrefix + WEBSERVER_COMPOSITE_PANEL_SHEET_SUFFIX);
					propMap.remove(panelPrefix + WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX);
					propMap.remove(panelPrefix + WEBSERVER_COMPOSITE_PANEL_SICHTBARER_TABELLENANTEIL_SUFFIX);
					propMap.remove(panelPrefix + LEGACY_WEBSERVER_COMPOSITE_PANEL_ZENTRIERT_SUFFIX);
					propMap.remove(panelPrefix + WEBSERVER_COMPOSITE_PANEL_HALIGN_SUFFIX);
					propMap.remove(panelPrefix + WEBSERVER_COMPOSITE_PANEL_VALIGN_SUFFIX);
					propMap.remove(panelPrefix + WEBSERVER_COMPOSITE_PANEL_BLATTNAME_SUFFIX);
					propMap.remove(panelPrefix + WEBSERVER_COMPOSITE_PANEL_TYP_SUFFIX);
					propMap.remove(panelPrefix + WEBSERVER_COMPOSITE_PANEL_URL_SUFFIX);
				}
			}
			propMap.remove(WEBSERVER_COMPOSITE_PORTS_PROP);

			if (!eintraege.isEmpty()) {
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
			speichernDatei();
		} catch (Exception e) {
			logger.error("Fehler beim Speichern der Composite Views", e);
		}
	}

	// ----------------------------------------------------
	// Speichern
	// ----------------------------------------------------

	public void speichern(boolean autosave, boolean backup, boolean newVersionCheck,
			boolean prozessBoxAutomatischAnzeigen, boolean prozessBoxAutomatischSchliessen,
			boolean performanceLogging, String logLevel) {
		try {
			PluginOptionen optionen = new PluginOptionen(autosave, backup, newVersionCheck,
					prozessBoxAutomatischAnzeigen, prozessBoxAutomatischSchliessen,
					performanceLogging, logLevel);
			pluginOptionenInMap(optionen);
			XComponentContext context = libreOfficeContext;
			if (context != null) {
				try {
					new LibreOfficePluginOptionenSpeicher(context).speichern(optionen);
				} catch (IllegalStateException e) {
					logger.warn("Speichern in LibreOffice-Konfiguration fehlgeschlagen, verwende Legacy-Datei", e);
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

	public int getTabFarbe(String konfigPropKey, int defaultVal) {
		try {
			var key = toTabFarbGlobalKey(konfigPropKey);
			var val = propMap.get(key);

			if (val == null || val.isBlank()) return defaultVal;

			return Integer.parseInt(val.trim(), 16);

		} catch (Exception e) {
			logger.warn("Fehler bei TabFarbe {}", konfigPropKey, e);
			return defaultVal;
		}
	}

	private static String toTabFarbGlobalKey(String konfigPropKey) {
		return TABFARBE_PRAEFIX + konfigPropKey.toLowerCase()
				.replace("tab-farbe ", "")
				.replace(" ", "_");
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


	// ----------------------------------------------------
	// Timer-Einstellungen
	// ----------------------------------------------------

	/** Liefert die zuletzt gespeicherte Timer-Dauer als "MM:SS"-String oder {@code defaultWert}. */
	public String getTimerLetzteDauer(String defaultWert) {
		var val = propMap.get(TIMER_DAUER_PROP);
		return (val != null && !val.isBlank()) ? val.trim() : defaultWert;
	}

	/** Liefert den zuletzt gespeicherten Timer-Webserver-Port oder {@code defaultWert}. */
	public int getTimerLetzterPort(int defaultWert) {
		try {
			var val = propMap.get(TIMER_PORT_PROP);
			if (val == null || val.isBlank()) return defaultWert;
			return Integer.parseInt(val.trim());
		} catch (NumberFormatException e) {
			return defaultWert;
		}
	}

	/** Liefert die zuletzt gespeicherte Timer-Bezeichnung (kann leer sein). */
	public String getTimerLetzteBezeichnung() {
		var val = propMap.get(TIMER_BEZEICHNUNG_PROP);
		return val != null ? val : "";
	}

	/**
	 * Liefert die zuletzt gespeicherte Timer-Hintergrundfarbe als RGB-Integer.
	 *
	 * @param defaultWert Rückgabewert wenn kein Wert gespeichert ist
	 * @return Farbe als RGB-Integer (z.B. {@code 0x000000} für Schwarz)
	 */
	public int getTimerHintergrundFarbe(int defaultWert) {
		try {
			var val = propMap.get(TIMER_HINTERGRUNDFARBE_PROP);
			if (val == null || val.isBlank()) return defaultWert;
			return Integer.parseInt(val.trim(), 16);
		} catch (NumberFormatException e) {
			return defaultWert;
		}
	}

	/**
	 * Speichert die letzten Timer-Einstellungen dauerhaft.
	 *
	 * @param dauer            Dauer als "MM:SS"
	 * @param port             Webserver-Port
	 * @param bezeichnung      optionaler Rundenname (darf null sein)
	 * @param hintergrundFarbe Hintergrundfarbe als RGB-Integer
	 */
	public void speichernTimerEinstellungen(String dauer, int port, String bezeichnung, int hintergrundFarbe) {
		if (dauer != null && !dauer.isBlank()) {
			propMap.put(TIMER_DAUER_PROP, dauer.trim());
		}
		propMap.put(TIMER_PORT_PROP, String.valueOf(port));
		if (bezeichnung != null && !bezeichnung.isBlank()) {
			propMap.put(TIMER_BEZEICHNUNG_PROP, bezeichnung.trim());
		} else {
			propMap.remove(TIMER_BEZEICHNUNG_PROP);
		}
		propMap.put(TIMER_HINTERGRUNDFARBE_PROP, String.format("%06x", hintergrundFarbe & 0xFFFFFF));
		speichernDatei();
	}

	// ---------------------------------------------------------------
	// Upload-Passwort (pro Host, nicht im ODS-Dokument gespeichert)
	// ---------------------------------------------------------------

	private static final String UPLOAD_PASSWORT_PRAEFIX = "upload.passwort.";

	public String getUploadPasswort(String host) {
		if (host == null || host.isBlank()) {
			return "";
		}
		return propMap.getOrDefault(UPLOAD_PASSWORT_PRAEFIX + host.trim().toLowerCase(), "");
	}

	public void setUploadPasswort(String host, String passwort) {
		if (host == null || host.isBlank()) {
			return;
		}
		String schluessel = UPLOAD_PASSWORT_PRAEFIX + host.trim().toLowerCase();
		if (passwort == null || passwort.isEmpty()) {
			propMap.remove(schluessel);
		} else {
			propMap.put(schluessel, passwort);
		}
		speichernDatei();
	}

	// nur für Tests
	static void resetForTest() {
		instance = null;
		propMap.clear();
		libreOfficeContext = null;
	}
}
