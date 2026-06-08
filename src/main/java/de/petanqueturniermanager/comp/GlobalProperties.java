package de.petanqueturniermanager.comp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.GsonBuilder;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import de.petanqueturniermanager.helper.perflog.PerfLog;
import de.petanqueturniermanager.webserver.CompositeViewKonfiguration;
import de.petanqueturniermanager.webserver.PanelAusrichtung;
import de.petanqueturniermanager.webserver.PanelKonfiguration;
import de.petanqueturniermanager.webserver.PanelTyp;
import de.petanqueturniermanager.webserver.SheetResolverFactory;
import de.petanqueturniermanager.webserver.SplitKnoten;
import de.petanqueturniermanager.webserver.SplitKnotenAdapter;

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

	// zentrale Runtime-Map
	private static final ConcurrentHashMap<String, String> propMap = new ConcurrentHashMap<>();

	private static volatile GlobalProperties instance = null;

	private static final ReentrantLock fileLock = new ReentrantLock();

	private static final String TABFARBE_PRAEFIX = "tabfarbe.";

	/**
	 * Rohdaten eines einzelnen Panels in einem Composite View (vor Resolver-Erstellung).
	 *
	 * @param typ                   Anzeigemodus: {@link PanelTyp#BLATT} oder {@link PanelTyp#URL}
	 * @param sheetConfig           Sheet-Konfigurations-String (nur relevant wenn typ == BLATT)
	 * @param zoom                  Zoom-Faktor in %
	 * @param horizontalAusrichtung horizontale Ausrichtung: {@code "kein"} / {@code "links"} / {@code "mitte"} / {@code "rechts"}
	 * @param vertikalAusrichtung   vertikale Ausrichtung:   {@code "kein"} / {@code "oben"} / {@code "mitte"} / {@code "unten"}
	 * @param blattnameAnzeigen     ob der Blattname als Kopfzeile angezeigt wird
	 * @param externeUrl            externe URL (nur relevant wenn typ == URL)
	 */
	public record PanelEintragRoh(
			PanelTyp typ,
			String sheetConfig,
			int zoom,
			String horizontalAusrichtung,
			String vertikalAusrichtung,
			boolean blattnameAnzeigen,
			String externeUrl) {
		public PanelEintragRoh {
			horizontalAusrichtung = PanelAusrichtung.normiereHorizontal(horizontalAusrichtung);
			vertikalAusrichtung   = PanelAusrichtung.normiereVertikal(vertikalAusrichtung);
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
	 * @param layoutJson      JSON-serialisierter {@link SplitKnoten}-Baum
	 * @param panels          Liste der Panel-Konfigurationen (Reihenfolge = panelIndex)
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
		bereinigeLegacyEinzelPortProperties();
		safeSetLogLevel();
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
					String layoutJson = propMap.getOrDefault(WEBSERVER_COMPOSITE_PREFIX + port + WEBSERVER_COMPOSITE_LAYOUT_SUFFIX, "").trim();
					if (layoutJson.isEmpty()) continue;

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
						if (panelTyp == PanelTyp.URL && externeUrl.isEmpty()) continue;
						int panelZoom = parseZoom(propMap.get(panelPrefix + WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX));
						String panelHAlign = propMap.get(panelPrefix + WEBSERVER_COMPOSITE_PANEL_HALIGN_SUFFIX);
						String panelVAlign = propMap.get(panelPrefix + WEBSERVER_COMPOSITE_PANEL_VALIGN_SUFFIX);
						if (panelHAlign == null && panelVAlign == null
								&& getBoolean(panelPrefix + LEGACY_WEBSERVER_COMPOSITE_PANEL_ZENTRIERT_SUFFIX)) {
							// Migration: alte boolean-Property "_zentriert" → Horizontal=Mitte
							panelHAlign = PanelAusrichtung.H_MITTE;
						}
						boolean panelBlattnameAnzeigen = getBoolean(panelPrefix + WEBSERVER_COMPOSITE_PANEL_BLATTNAME_SUFFIX);
						panels.add(new PanelEintragRoh(panelTyp, sheetConfig, panelZoom, panelHAlign, panelVAlign, panelBlattnameAnzeigen, externeUrl));
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
				SplitKnoten wurzel = gson.fromJson(eintrag.layoutJson(), SplitKnoten.class);
				if (wurzel == null) {
					logger.warn("Ungültiger Layout-JSON für Port {}", eintrag.port());
					continue;
				}
				List<PanelKonfiguration> panels = new ArrayList<>();
				for (var p : eintrag.panels()) {
					if (p.typ() == PanelTyp.TIMER) {
						panels.add(new PanelKonfiguration(PanelTyp.TIMER, "", null, p.zoom(),
								p.horizontalAusrichtung(), p.vertikalAusrichtung(), false, ""));
						continue;
					}
					if (p.typ() == PanelTyp.URL) {
						panels.add(new PanelKonfiguration(PanelTyp.URL, "", null, p.zoom(),
								p.horizontalAusrichtung(), p.vertikalAusrichtung(), p.blattnameAnzeigen(), p.externeUrl()));
						continue;
					}
					var resolver = SheetResolverFactory.erstellen(p.sheetConfig());
					if (resolver == null) {
						logger.warn("Resolver null für Panel-Config '{}'", p.sheetConfig());
						continue;
					}
					panels.add(new PanelKonfiguration(PanelTyp.BLATT, p.sheetConfig(), resolver, p.zoom(),
							p.horizontalAusrichtung(), p.vertikalAusrichtung(), p.blattnameAnzeigen(), ""));
				}
				if (!panels.isEmpty()) {
					konfigs.add(new CompositeViewKonfiguration(eintrag.port(), eintrag.name(), eintrag.zoom(), wurzel, panels, eintrag.mitHeaderFooter()));
				}
			} catch (Exception e) {
				logger.error("Fehler bei Composite-View-Konfiguration {}", eintrag, e);
			}
		}
		return konfigs;
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
					propMap.put(prefix + WEBSERVER_COMPOSITE_LAYOUT_SUFFIX, eintrag.layoutJson());
					propMap.put(prefix + WEBSERVER_COMPOSITE_PANEL_COUNT_SUFFIX, String.valueOf(eintrag.panels().size()));
					for (int i = 0; i < eintrag.panels().size(); i++) {
						var panel = eintrag.panels().get(i);
						String panelPrefix = prefix + WEBSERVER_COMPOSITE_PANEL_INFIX + i;
						propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_TYP_SUFFIX, panel.typ().name());
						if (panel.typ() == PanelTyp.URL) {
							propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_URL_SUFFIX, panel.externeUrl());
						} else {
							propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_SHEET_SUFFIX, panel.sheetConfig());
						}
						if (panel.zoom() != DEFAULT_ZOOM)
							propMap.put(panelPrefix + WEBSERVER_COMPOSITE_PANEL_ZOOM_SUFFIX, String.valueOf(panel.zoom()));
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
			setBooleanProp(AUTOSAVE_PROP, autosave);
			setBooleanProp(CREATE_BACKUP_PROP, backup);
			setBooleanProp(NEW_VERSION_CHECK_PROP, newVersionCheck);
			setBooleanProp(PERFORMANCE_LOGGING_PROP, performanceLogging);
			// Default-true-Properties: explizit "false" persistieren, sonst greift beim Lesen wieder der Default.
			propMap.put(PROZESSBOX_AUTOMATISCH_ANZEIGEN_PROP, Boolean.toString(prozessBoxAutomatischAnzeigen));
			propMap.put(PROZESSBOX_AUTOMATISCH_SCHLIESSEN_PROP, Boolean.toString(prozessBoxAutomatischSchliessen));

			if (logLevel != null && !logLevel.isBlank()) {
				propMap.put(LOG_LEVEL_PROP, logLevel.trim().toLowerCase());
			} else {
				propMap.remove(LOG_LEVEL_PROP);
			}

			speichernDatei();
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
	}
}