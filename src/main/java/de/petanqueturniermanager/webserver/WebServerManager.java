package de.petanqueturniermanager.webserver;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XModifyBroadcaster;

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.CompositeViewEintragRoh;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.timer.TimerListener;
import de.petanqueturniermanager.timer.TimerState;
import de.petanqueturniermanager.timer.TimerWebServerInstanz;

/**
 * Singleton-Verwaltung aller Webserver-Instanzen.
 * <p>
 * Startet und stoppt HTTP-Server-Instanzen gemäß den globalen Plugin-Einstellungen.
 * <p>
 * Das Mapping (UNO → Model) und der SSE-Push erfolgen <strong>immer</strong> im SheetRunner-Thread
 * (via {@link #sseRefreshSenden}), um UNO-Thread-Safety zu gewährleisten.
 * HTTP-Handler-Threads greifen nur auf gecachte JSON-Strings zu.
 * <p>
 * SSE-Protokoll:
 * <ul>
 *   <li>{@code typ="init"} – vollständiger Zustand, beim ersten Render oder für neue Verbindungen</li>
 *   <li>{@code typ="diff"} – nur geänderte Zellen, nach jedem weiteren Render</li>
 *   <li>{@code typ="hinweis"} – Hinweismeldung, wenn das Sheet noch nicht verfügbar ist</li>
 * </ul>
 */
public final class WebServerManager implements TimerListener {

    private static final Logger logger = LogManager.getLogger(WebServerManager.class);
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SplitKnoten.class, new SplitKnotenAdapter())
            .create();
    private static final WebServerManager INSTANCE = new WebServerManager();

    /** Maximale Anzahl URL-Slots im Menü. */
    private static final int MAX_URL_SLOTS = 10;

    /** Alle laufenden Einzel-Instanzen (alle konfigurierten Ports). */
    private final List<WebServerInstanz> instanzen = new ArrayList<>();
    /** Alle laufenden Composite-View-Instanzen. */
    private final List<CompositeViewInstanz> compositeInstanzen = new ArrayList<>();
    /** Die ersten MAX_URL_SLOTS Instanzen (Einzel + Composite, nach Port sortiert) für Menü-URL-Anzeige. */
    private final List<WebServerSlot> slots = new ArrayList<>(MAX_URL_SLOTS);

    private final TabellenMapper mapper = new TabellenMapper();
    private final DiffEngine diffEngine = new DiffEngine();
    private final WebserverModifyListener modifyListener = new WebserverModifyListener();

    /** Letztes Modell pro Port (port → TabelleModel). */
    private final Map<Integer, TabelleModel> letzteModelle = new ConcurrentHashMap<>();
    /** Letzter Seitentitel pro Port (port → Titel). */
    private final Map<Integer, String> letzteTitel = new ConcurrentHashMap<>();
    /** Monoton steigende Version pro Port (port → AtomicInteger). */
    private final Map<Integer, AtomicInteger> versionen = new ConcurrentHashMap<>();

    /** Letzte Panel-Modelle pro Composite-Port und Panel-ID (port → panelId → TabelleModel). */
    private final Map<Integer, Map<Integer, TabelleModel>> letzteCompositeModelle = new ConcurrentHashMap<>();
    /** Letzte Panel-Titel pro Composite-Port und Panel-ID (port → panelId → Titel). */
    private final Map<Integer, Map<Integer, String>> letzteCompositeTitel = new ConcurrentHashMap<>();
    /** Monoton steigende Version pro Composite-Port (port → AtomicInteger). */
    private final Map<Integer, AtomicInteger> compositeVersionen = new ConcurrentHashMap<>();
    /** Letzte URL pro Composite-Port und Panel-ID (port → panelId → url). Diff-Cache für URL-Panels. */
    private final Map<Integer, Map<Integer, String>> letzteCompositeUrls = new ConcurrentHashMap<>();
    /** Initialisierte TIMER-Panels pro Composite-Port (port → Set<panelId>). ConcurrentHashMap erlaubt keine null-Werte,
     * daher können TIMER-Panels nicht in letzteCompositeModelle als null-Marker gespeichert werden. */
    private final Map<Integer, Set<Integer>> initialisiertePanels = new ConcurrentHashMap<>();

    /** Dokument, das den WebServer gestartet hat – nur dieses darf ihn wieder stoppen. */
    private XSpreadsheetDocument ownerDocument = null;
    private XSpreadsheetDocument registriertesDocument = null;
    private boolean laeuft = false;
    private boolean letzterSheetnamenAnzeigen = false;
    private TimerWebServerInstanz timerInstanz;
    private volatile TimerState letzterTimerZustand = TimerState.inaktiv();

    private final List<Runnable> statusListener = new CopyOnWriteArrayList<>();

    private WebServerManager() {
    }

    public void addStatusListener(Runnable listener) {
        statusListener.add(listener);
    }

    public void removeStatusListener(Runnable listener) {
        statusListener.remove(listener);
    }

    private void statusListenerBenachrichtigen() {
        statusListener.forEach(Runnable::run);
    }

    public static WebServerManager get() {
        return INSTANCE;
    }

    /**
     * Startet alle konfigurierten Webserver-Instanzen.
     * Führt nach dem Start direkt ein initiales Rendering durch.
     *
     * @param ctx LibreOffice-Kontext für das initiale Rendering
     */
    public synchronized void starten(XComponentContext ctx) {
        if (laeuft) {
            logger.debug("WebServerManager läuft bereits");
            return;
        }
        var portKonfigs = new ArrayList<>(GlobalProperties.get().getPortKonfigurationen());
        var compositeKonfigs = new ArrayList<>(GlobalProperties.get().getCompositeViewKonfigurationen());
        if (portKonfigs.isEmpty() && compositeKonfigs.isEmpty()) {
            logger.info("Keine Webserver-Ports konfiguriert, kein Start");
            return;
        }
        instanzen.clear();
        slots.clear();
        compositeInstanzen.clear();
        portKonfigs.sort(Comparator.comparingInt(PortKonfiguration::port));
        safeProcessBoxInfo(I18n.get("webserver.prozessbox.starten"));

        for (var konfig : portKonfigs) {
            try {
                var instanz = new WebServerInstanz(konfig);
                instanz.starten();
                // Sofort Initial-Hinweis setzen, damit Clients die sich vor dem ersten Rendering
                // verbinden i18n-Text erhalten statt einer leeren Antwort.
                instanz.setCachedInitJson(GSON.toJson(SseNachricht.hinweis(
                        I18n.get("webserver.hinweis.kein.dokument.titel"),
                        I18n.get("webserver.hinweis.kein.dokument.text"))));
                instanzen.add(instanz);
                versionen.put(konfig.port(), new AtomicInteger(0));
                logger.info("Webserver gestartet auf Port {}", konfig.port());
                safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestartet.url", buildUrl(konfig.port())));
            } catch (IOException e) {
                logger.error("Fehler beim Starten des WebServers auf Port {}: {}",
                        konfig.port(), e.getMessage(), e);
                safeProcessBoxFehler(I18n.get("webserver.prozessbox.fehler.port", konfig.port(), e.getMessage()));
            }
        }

        for (var konfig : compositeKonfigs) {
            try {
                var instanz = new CompositeViewInstanz(konfig);
                instanz.starten();
                instanz.setCachedInitJson(GSON.toJson(CompositeSseNachricht.hinweis(
                        I18n.get("webserver.hinweis.kein.dokument.titel"),
                        I18n.get("webserver.hinweis.kein.dokument.text"))));
                compositeInstanzen.add(instanz);
                compositeVersionen.put(konfig.port(), new AtomicInteger(0));
                letzteCompositeModelle.put(konfig.port(), new ConcurrentHashMap<>());
                letzteCompositeTitel.put(konfig.port(), new ConcurrentHashMap<>());
                letzteCompositeUrls.put(konfig.port(), new ConcurrentHashMap<>());
                initialisiertePanels.put(konfig.port(), ConcurrentHashMap.newKeySet());
                logger.info("Composite-View-Server gestartet auf Port {}", konfig.port());
                safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestartet.url", buildUrl(konfig.port())));
            } catch (IOException e) {
                logger.error("Fehler beim Starten des Composite-View-Servers auf Port {}: {}",
                        konfig.port(), e.getMessage(), e);
                safeProcessBoxFehler(I18n.get("webserver.prozessbox.fehler.port", konfig.port(), e.getMessage()));
            }
        }

        // Slots (Einzel + Composite) nach Port sortiert befüllen
        var alleInstanzen = new ArrayList<WebServerSlot>(instanzen.size() + compositeInstanzen.size());
        alleInstanzen.addAll(instanzen);
        alleInstanzen.addAll(compositeInstanzen);
        alleInstanzen.stream()
                .filter(WebServerSlot::laeuft)
                .sorted(Comparator.comparingInt(WebServerSlot::getPort))
                .limit(MAX_URL_SLOTS)
                .forEach(slots::add);

        if (!instanzen.isEmpty() || !compositeInstanzen.isEmpty()) {
            laeuft = true;
            ownerDocument = new WorkingSpreadsheet(ctx).getWorkingSpreadsheetDocument();
            logger.info("Webserver Owner-Dokument gesetzt: {}", ownerDocument != null ? "ja" : "null");
            statusListenerBenachrichtigen();
            try {
                sseRefreshSendenIntern(new WorkingSpreadsheet(ctx));
            } catch (Exception e) {
                logger.debug("Initiales Rendering fehlgeschlagen: {}", e.getMessage());
                sendeHinweisAnAlle(
                        I18n.get("webserver.hinweis.kein.dokument.titel"),
                        I18n.get("webserver.hinweis.kein.dokument.text"));
            }
        }
    }

    /**
     * Startet den Timer-Webserver auf dem angegebenen Port (oder gibt die laufende Instanz zurück).
     * Wird von {@link de.petanqueturniermanager.timer.TimerManager} beim Timer-Start aufgerufen.
     *
     * @param port TCP-Port für den Timer-Server
     * @throws IOException wenn der Port nicht gebunden werden kann
     */
    public synchronized void timerServerBesorgen(int port) throws IOException {
        if (timerInstanz != null && timerInstanz.getPort() == port && timerInstanz.laeuft()) {
            return;
        }
        if (timerInstanz != null) {
            timerInstanz.stoppen();
            timerInstanz = null;
        }
        timerInstanz = new TimerWebServerInstanz(port);
        timerInstanz.starten();
        logger.info("Timer-Webserver gestartet auf Port {}", port);
    }

    /**
     * Empfängt Timer-Zustandsänderungen und leitet sie an den laufenden Timer-Webserver
     * sowie an alle Composite-Instanzen mit TIMER-Panels weiter.
     * Registriert einmalig in {@code PetanqueTurnierMngrSingleton.init()}.
     */
    @Override
    public synchronized void onChange(TimerState state) {
        letzterTimerZustand = state;
        if (timerInstanz != null && timerInstanz.laeuft()) {
            timerInstanz.onChange(state);
        }
        for (var instanz : compositeInstanzen) {
            if (!instanz.hatTimerPanels()) {
                continue;
            }
            sendeTimerUpdateAnComposite(instanz, state);
        }
    }

    /**
     * Pusht den aktuellen Timer-Zustand als diff-Nachricht an alle TIMER-Panels
     * der gegebenen Composite-Instanz.
     */
    private void sendeTimerUpdateAnComposite(CompositeViewInstanz instanz, TimerState state) {
        var panelNachrichten = new ArrayList<CompositePanelNachricht>();
        var konfig = instanz.getKonfiguration();
        for (int i = 0; i < konfig.panels().size(); i++) {
            if (konfig.panels().get(i).typ() == PanelTyp.TIMER) {
                var panelKonfig = konfig.panels().get(i);
                panelNachrichten.add(CompositePanelNachricht.timer(i, panelKonfig.zoom(), panelKonfig.zentriert(), state));
            }
        }
        if (panelNachrichten.isEmpty()) {
            return;
        }
        int version = compositeVersionen.getOrDefault(konfig.port(), new AtomicInteger(0)).incrementAndGet();
        instanz.sseNachrichtPushen(GSON.toJson(CompositeSseNachricht.diff(version, panelNachrichten, konfig.wurzel(), konfig.zoom())));
    }

    /**
     * Stoppt alle laufenden Webserver-Instanzen und schließt alle SSE-Verbindungen.
     */
    public synchronized void stoppen() {
        if (timerInstanz != null) {
            timerInstanz.stoppen();
            timerInstanz = null;
        }
        if (!laeuft) {
            return;
        }
        deregistriereModifyListener();
        for (var instanz : instanzen) {
            instanz.stoppen();
        }
        for (var instanz : compositeInstanzen) {
            instanz.stoppen();
        }
        instanzen.clear();
        slots.clear();
        compositeInstanzen.clear();
        letzteModelle.clear();
        letzteTitel.clear();
        versionen.clear();
        letzteCompositeModelle.clear();
        letzteCompositeTitel.clear();
        compositeVersionen.clear();
        letzteCompositeUrls.clear();
        initialisiertePanels.clear();
        laeuft = false;
        letzterSheetnamenAnzeigen = false;
        ownerDocument = null;
        statusListenerBenachrichtigen();
        logger.info("Webserver gestoppt");
        safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestoppt"));
    }

    /**
     * Wird vom SheetRunner-Thread nach erfolgreichem {@code doRun()} aufgerufen.
     * Mappt das Sheet, berechnet Diffs und pusht SSE-Nachrichten.
     * <p>
     * Aufrufe für fremde Dokumente (nicht das Owner-Dokument) werden ignoriert –
     * der WebServer zeigt ausschließlich Sheets des Dokuments, das ihn gestartet hat.
     * <p>
     * <strong>Muss im SheetRunner-Thread aufgerufen werden</strong> – greift auf UNO zu.
     *
     * @param ws aktuelles Arbeits-Spreadsheet
     */
    public void sseRefreshSenden(WorkingSpreadsheet ws) {
        if (!laeuft) {
            return;
        }
        if (!istOwnerDocument(ws.getWorkingSpreadsheetDocument())) {
            logger.debug("sseRefreshSenden ignoriert – ws gehört nicht zum Owner-Dokument");
            return;
        }
        sseRefreshSendenIntern(ws);
    }

    /**
     * Aktualisiert Zoom und Zentrieren aller laufenden Instanzen aus den gespeicherten GlobalProperties
     * und pusht den neuen Zustand sofort an alle verbundenen Browser-Clients.
     * <p>
     * Muss nach {@link GlobalProperties#speichernWebserver} aufgerufen werden.
     * Kein UNO-Zugriff erforderlich – verwendet ausschließlich gecachte Modelle.
     */
    public synchronized void konfigurationGeaendert() {
        if (!laeuft) {
            return;
        }
        boolean neuerSheetnamenAnzeigen = GlobalProperties.get().isSheetnamenKopfzeileAnzeigen();
        boolean globalGeaendert = neuerSheetnamenAnzeigen != letzterSheetnamenAnzeigen;
        if (globalGeaendert) {
            letzterSheetnamenAnzeigen = neuerSheetnamenAnzeigen;
        }
        var eintraege = GlobalProperties.get().getPortEintraege();
        for (var instanz : instanzen) {
            int port = instanz.getKonfiguration().port();
            eintraege.stream()
                    .filter(e -> e.port() == port)
                    .findFirst()
                    .ifPresent(e -> {
                        if (!globalGeaendert
                                && e.zoom() == instanz.getKonfiguration().zoom()
                                && e.zentrieren() == instanz.getKonfiguration().zentrieren()) {
                            return; // keine Änderung, kein Push
                        }
                        var neueKonfig = new PortKonfiguration(
                                port, instanz.getKonfiguration().resolver(), e.zoom(), e.zentrieren());
                        TabelleModel modell = letzteModelle.get(port);
                        String neuesJson = null;
                        if (modell != null) {
                            String titel = letzteTitel.getOrDefault(port, "");
                            int version = versionen.get(port).incrementAndGet();
                            neuesJson = GSON.toJson(SseNachricht.init(version, modell, titel,
                                    e.zoom(), e.zentrieren(), neuerSheetnamenAnzeigen));
                        }
                        instanz.setKonfiguration(neueKonfig, neuesJson);
                    });
        }

        var compositeEintraege = GlobalProperties.get().getCompositeViewEintraege();
        for (var instanz : compositeInstanzen) {
            int port = instanz.getKonfiguration().port();
            compositeEintraege.stream()
                    .filter(e -> e.port() == port && e.aktiv())
                    .findFirst()
                    .ifPresent(e -> {
                        var panelModelle = letzteCompositeModelle.get(port);
                        var panelTitel = letzteCompositeTitel.get(port);
                        if (panelModelle == null) {
                            return;
                        }
                        var konfig = instanz.getKonfiguration();
                        int neueAnzahl = e.panels().size();
                        int alteAnzahl = konfig.panels().size();

                        // Neue Wurzel aus layoutJson parsen
                        SplitKnoten neueWurzel = konfig.wurzel();
                        try {
                            var parsedWurzel = GSON.fromJson(e.layoutJson(), SplitKnoten.class);
                            if (parsedWurzel != null) {
                                neueWurzel = parsedWurzel;
                            }
                        } catch (Exception ex) {
                            logger.warn("Ungültiger Layout-JSON bei konfigurationGeaendert für Port {}", port, ex);
                        }

                        // Alte sheetConfig → alten Index mappen, um Resolver und Modelle
                        // korrekt auf neue Positionen zu übertragen (auch nach Löschung/Umnummerierung).
                        var altSheetConfigZuIndex = new HashMap<String, Integer>();
                        for (int i = 0; i < alteAnzahl; i++) {
                            altSheetConfigZuIndex.put(konfig.panels().get(i).sheetConfig(), i);
                        }

                        // Neue Panel-Einstellungen mit sheetConfig-basiertem Matching aufbauen.
                        // Für bekannte Panels (gleiche sheetConfig): alten Resolver + Modell behalten.
                        // Für neue Panels: Resolver aus sheetConfig erstellen, kein Modell-Cache.
                        var alleInitPanels = new ArrayList<CompositePanelNachricht>();
                        var neuePanelKonfigs = new ArrayList<PanelKonfiguration>();
                        var neuePanelModelle = new ConcurrentHashMap<Integer, TabelleModel>();
                        var neuePanelTitel = new ConcurrentHashMap<Integer, String>();
                        var neueUrlCache = letzteCompositeUrls.computeIfAbsent(port, p -> new ConcurrentHashMap<>());
                        for (int i = 0; i < neueAnzahl; i++) {
                            var neuerPanelEintrag = e.panels().get(i);
                            if (neuerPanelEintrag.typ() == PanelTyp.TIMER) {
                                neuePanelKonfigs.add(new PanelKonfiguration(
                                        PanelTyp.TIMER, "", null,
                                        neuerPanelEintrag.zoom(), neuerPanelEintrag.zentriert(), false, ""));
                                alleInitPanels.add(CompositePanelNachricht.timer(i, neuerPanelEintrag.zoom(), neuerPanelEintrag.zentriert(), letzterTimerZustand));
                                continue;
                            }
                            if (neuerPanelEintrag.typ() == PanelTyp.URL) {
                                neuePanelKonfigs.add(new PanelKonfiguration(
                                        PanelTyp.URL, "", null,
                                        neuerPanelEintrag.zoom(), neuerPanelEintrag.zentriert(), neuerPanelEintrag.blattnameAnzeigen(),
                                        neuerPanelEintrag.externeUrl()));
                                neueUrlCache.put(i, neuerPanelEintrag.externeUrl());
                                alleInitPanels.add(CompositePanelNachricht.url(i, neuerPanelEintrag.externeUrl()));
                                continue;
                            }
                            var altIndex = altSheetConfigZuIndex.get(neuerPanelEintrag.sheetConfig());
                            SheetResolver resolver = altIndex != null
                                    ? konfig.panels().get(altIndex).resolver()
                                    : SheetResolverFactory.erstellen(neuerPanelEintrag.sheetConfig());
                            neuePanelKonfigs.add(new PanelKonfiguration(
                                    PanelTyp.BLATT, neuerPanelEintrag.sheetConfig(), resolver,
                                    neuerPanelEintrag.zoom(), neuerPanelEintrag.zentriert(), neuerPanelEintrag.blattnameAnzeigen(), ""));
                            var vollModell = altIndex != null ? panelModelle.get(altIndex) : null;
                            if (vollModell != null) {
                                neuePanelModelle.put(i, vollModell);
                                // altIndex != null ist hier garantiert (vollModell != null impliziert altIndex != null)
                                String titel = panelTitel != null ? panelTitel.getOrDefault(altIndex, "") : "";
                                neuePanelTitel.put(i, titel);
                                alleInitPanels.add(CompositePanelNachricht.init(
                                        i, vollModell, titel,
                                        neuerPanelEintrag.zoom(), neuerPanelEintrag.zentriert(), neuerPanelEintrag.blattnameAnzeigen()));
                            }
                        }

                        // Cache durch korrekt re-indizierte Maps ersetzen
                        panelModelle.clear();
                        panelModelle.putAll(neuePanelModelle);
                        initialisiertePanels.computeIfPresent(port, (p, s) -> { s.clear(); return s; });
                        if (panelTitel != null) {
                            panelTitel.clear();
                            panelTitel.putAll(neuePanelTitel);
                        }

                        String cachedJson = null;
                        String pushJson = null;
                        if (!alleInitPanels.isEmpty()) {
                            int version = compositeVersionen.get(port).incrementAndGet();
                            cachedJson = GSON.toJson(CompositeSseNachricht.init(
                                    version, alleInitPanels, neueWurzel, e.zoom()));
                            // Sind nicht alle Panels im Cache (geänderte sheetConfig), würde ein
                            // composite_init die fehlenden Panels im Browser löschen → diff pushen,
                            // damit bestehende Panel-Zustände erhalten bleiben.
                            pushJson = alleInitPanels.size() < neueAnzahl
                                    ? GSON.toJson(CompositeSseNachricht.diff(version, alleInitPanels, neueWurzel, e.zoom()))
                                    : cachedJson;
                        }
                        instanz.setKonfiguration(
                                new CompositeViewKonfiguration(port, e.zoom(), neueWurzel, neuePanelKonfigs),
                                cachedJson, pushJson);
                    });
        }

        reconciliereCompositeInstanzen(compositeEintraege);
    }

    /**
     * Gleicht die laufenden Composite-View-Instanzen mit den aktuellen GlobalProperties ab:
     * stoppt Instanzen für entfernte/deaktivierte Einträge und startet neue für hinzugekommene/
     * reaktivierte Einträge. Aktualisiert danach die Slots-Liste.
     */
    private void reconciliereCompositeInstanzen(List<CompositeViewEintragRoh> compositeEintraege) {
        Set<Integer> aktivePorte = new HashSet<>();
        for (var e : compositeEintraege) {
            if (e.aktiv()) {
                aktivePorte.add(e.port());
            }
        }
        Set<Integer> laufendePorte = new HashSet<>();
        for (var instanz : compositeInstanzen) {
            laufendePorte.add(instanz.getKonfiguration().port());
        }

        // Instanzen stoppen, deren Port nicht mehr aktiv konfiguriert ist
        var zuStoppen = new ArrayList<CompositeViewInstanz>();
        for (var instanz : compositeInstanzen) {
            if (!aktivePorte.contains(instanz.getKonfiguration().port())) {
                zuStoppen.add(instanz);
            }
        }
        for (var instanz : zuStoppen) {
            int port = instanz.getKonfiguration().port();
            instanz.stoppen();
            compositeInstanzen.remove(instanz);
            letzteCompositeModelle.remove(port);
            letzteCompositeTitel.remove(port);
            compositeVersionen.remove(port);
            letzteCompositeUrls.remove(port);
            initialisiertePanels.remove(port);
            logger.info("Composite-View-Server auf Port {} gestoppt (konfigurationGeaendert)", port);
            safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestoppt.port", port));
        }

        // Neue Instanzen starten für aktive Einträge ohne laufende Instanz
        for (var konfig : GlobalProperties.get().getCompositeViewKonfigurationen()) {
            if (!laufendePorte.contains(konfig.port())) {
                try {
                    var instanz = new CompositeViewInstanz(konfig);
                    instanz.starten();
                    var versionZaehler = new AtomicInteger(0);
                    compositeVersionen.put(konfig.port(), versionZaehler);
                    letzteCompositeModelle.put(konfig.port(), new ConcurrentHashMap<>());
                    letzteCompositeTitel.put(konfig.port(), new ConcurrentHashMap<>());
                    letzteCompositeUrls.put(konfig.port(), new ConcurrentHashMap<>());
                    initialisiertePanels.put(konfig.port(), ConcurrentHashMap.newKeySet());
                    initialisiereNeueCompositeInstanz(instanz, konfig, versionZaehler);
                    compositeInstanzen.add(instanz);
                    logger.info("Composite-View-Server auf Port {} gestartet (konfigurationGeaendert)", konfig.port());
                    safeProcessBoxInfo(I18n.get("webserver.prozessbox.gestartet.url", buildUrl(konfig.port())));
                } catch (IOException e) {
                    logger.error("Fehler beim Starten des Composite-View-Servers auf Port {}: {}", konfig.port(), e.getMessage(), e);
                    safeProcessBoxFehler(I18n.get("webserver.prozessbox.fehler.port", konfig.port(), e.getMessage()));
                }
            }
        }

        // Slots nach Änderungen neu aufbauen
        if (!zuStoppen.isEmpty() || aktivePorte.stream().anyMatch(p -> !laufendePorte.contains(p))) {
            slots.clear();
            var alleInstanzen = new ArrayList<WebServerSlot>(instanzen.size() + compositeInstanzen.size());
            alleInstanzen.addAll(instanzen);
            alleInstanzen.addAll(compositeInstanzen);
            alleInstanzen.stream()
                    .filter(WebServerSlot::laeuft)
                    .sorted(Comparator.comparingInt(WebServerSlot::getPort))
                    .limit(MAX_URL_SLOTS)
                    .forEach(slots::add);
        }
    }

    /**
     * Setzt den initialen SSE-Cache für eine neu gestartete Composite-Instanz.
     * <p>
     * Panels ohne Sheet-Zugriff (TIMER, URL) können sofort initialisiert werden.
     * BLATT-Panels ohne gecachtes Modell werden erst beim nächsten Rendering befüllt;
     * bis dahin bleibt der Hinweis-Cache für diese Instanz erhalten.
     */
    private void initialisiereNeueCompositeInstanz(
            CompositeViewInstanz instanz,
            CompositeViewKonfiguration konfig,
            AtomicInteger versionZaehler) {
        var alleInitPanels = new ArrayList<CompositePanelNachricht>();
        for (int i = 0; i < konfig.panels().size(); i++) {
            var panelKonfig = konfig.panels().get(i);
            if (panelKonfig.typ() == PanelTyp.TIMER) {
                alleInitPanels.add(CompositePanelNachricht.timer(
                        i, panelKonfig.zoom(), panelKonfig.zentriert(), letzterTimerZustand));
            } else if (panelKonfig.typ() == PanelTyp.URL) {
                alleInitPanels.add(CompositePanelNachricht.url(i, panelKonfig.externeUrl()));
                letzteCompositeUrls.get(konfig.port()).put(i, panelKonfig.externeUrl());
            }
        }
        if (alleInitPanels.isEmpty() || alleInitPanels.size() < konfig.panels().size()) {
            // Nicht alle Panels können ohne Sheet initialisiert werden → vorerst Hinweis
            instanz.setCachedInitJson(GSON.toJson(CompositeSseNachricht.hinweis(
                    I18n.get("webserver.hinweis.kein.dokument.titel"),
                    I18n.get("webserver.hinweis.kein.dokument.text"))));
            return;
        }
        int version = versionZaehler.incrementAndGet();
        instanz.setCachedInitJson(GSON.toJson(
                CompositeSseNachricht.init(version, alleInitPanels, konfig.wurzel(), konfig.zoom())));
    }

    private void sseRefreshSendenIntern(WorkingSpreadsheet ws) {
        registriereModifyListenerFallsNoetig(ws);
        for (var instanz : instanzen) {
            renderUndPushen(instanz, ws);
        }
        for (var instanz : compositeInstanzen) {
            renderUndPushenComposite(instanz, ws);
        }
    }

    private void registriereModifyListenerFallsNoetig(WorkingSpreadsheet ws) {
        var xDoc = ws.getWorkingSpreadsheetDocument();
        if (xDoc == null || xDoc.equals(registriertesDocument)) {
            return;
        }
        deregistriereModifyListener();
        try {
            var broadcaster = Lo.qi(XModifyBroadcaster.class, xDoc);
            modifyListener.setWs(ws);
            broadcaster.addModifyListener(modifyListener);
            registriertesDocument = xDoc;
            logger.debug("WebserverModifyListener am Dokument registriert");
        } catch (Exception e) {
            logger.debug("XModifyBroadcaster nicht verfügbar – Live-Updates deaktiviert: {}",
                    e.getMessage());
        }
    }

    private void deregistriereModifyListener() {
        if (registriertesDocument == null) {
            return;
        }
        try {
            var broadcaster = Lo.qi(XModifyBroadcaster.class, registriertesDocument);
            if (broadcaster != null) {
                broadcaster.removeModifyListener(modifyListener);
            }
        } catch (Exception e) {
            logger.debug("Fehler beim Entfernen des ModifyListeners: {}", e.getMessage());
        }
        modifyListener.setWs(null);
        registriertesDocument = null;
        logger.debug("WebserverModifyListener deregistriert");
    }

    private void renderUndPushen(WebServerInstanz instanz, WorkingSpreadsheet ws) {
        var resolver = instanz.getKonfiguration().resolver();
        int port = instanz.getKonfiguration().port();
        try {
            Optional<XSpreadsheet> sheetOpt = resolver.resolve(ws);
            if (sheetOpt.isEmpty()) {
                sendeHinweis(instanz,
                        I18n.get("webserver.hinweis.sheet.nicht.gefunden.titel"),
                        I18n.get("webserver.hinweis.sheet.nicht.gefunden.text",
                                resolver.getAnzeigeName()));
                return;
            }

            var sheet = sheetOpt.get();
            String neuerTitel = baueSeitenTitel(resolver, sheet);
            TabelleModel neuesModell = mapper.map(sheet, ws.getWorkingSpreadsheetDocument());
            TabelleModel altesModell = letzteModelle.get(port);
            String alterTitel = letzteTitel.get(port);
            int zoom = instanz.getKonfiguration().zoom();
            boolean zentrieren = instanz.getKonfiguration().zentrieren();
            boolean sheetnamenAnzeigen = GlobalProperties.get().isSheetnamenKopfzeileAnzeigen();

            // Layout-Änderung (Größe oder Startversatz) → vollständiges Init erzwingen
            if (altesModell != null && layoutGeaendert(altesModell, neuesModell)) {
                altesModell = null;
            }

            TabelleModel diffModell = diffEngine.diff(altesModell, neuesModell);

            boolean keineDiffNoetig = altesModell != null
                    && diffModell.getZellen().isEmpty()
                    && !kopfFusszeileGeaendert(altesModell, neuesModell)
                    && java.util.Objects.equals(alterTitel, neuerTitel);
            if (keineDiffNoetig) {
                return; // nichts geändert, kein Push
            }

            int version = versionen.get(port).incrementAndGet();
            letzteModelle.put(port, neuesModell);
            letzteTitel.put(port, neuerTitel);

            // Init-Cache immer mit vollem State aktualisieren (für neue/reconnectende Verbindungen)
            SseNachricht initNachricht = SseNachricht.init(version, neuesModell, neuerTitel,
                    zoom, zentrieren, sheetnamenAnzeigen);
            instanz.setCachedInitJson(GSON.toJson(initNachricht));

            // Push: "init" beim ersten Mal, sonst "diff"
            SseNachricht push = (altesModell == null)
                    ? initNachricht
                    : SseNachricht.diff(version, diffModell, neuerTitel, zoom, zentrieren, sheetnamenAnzeigen);
            instanz.sseNachrichtPushen(GSON.toJson(push));

        } catch (Exception e) {
            logger.error("Fehler beim Rendern für Port {}: {}", port, e.getMessage(), e);
            safeProcessBoxFehler(I18n.get("webserver.prozessbox.render.fehler", port, e.getMessage()));
        }
    }

    private void renderUndPushenComposite(CompositeViewInstanz instanz, WorkingSpreadsheet ws) {
        int port = instanz.getKonfiguration().port();
        var konfig = instanz.getKonfiguration();
        var panelModelle = letzteCompositeModelle.computeIfAbsent(port, p -> new ConcurrentHashMap<>());
        var panelTitel = letzteCompositeTitel.computeIfAbsent(port, p -> new ConcurrentHashMap<>());
        try {
            var panelNachrichten = new ArrayList<CompositePanelNachricht>();
            boolean irgendeineAenderung = false;
            boolean erstesRendering = false;

            var urlCache = letzteCompositeUrls.computeIfAbsent(port, p -> new ConcurrentHashMap<>());
            String erstesNichtGefundenesPanel = null;
            for (int i = 0; i < konfig.panels().size(); i++) {
                var panelKonfig = konfig.panels().get(i);

                // TIMER-Panel: kein Sheet-Lookup, Timer-Zustand aus letzterTimerZustand
                if (panelKonfig.typ() == PanelTyp.TIMER) {
                    panelNachrichten.add(CompositePanelNachricht.timer(i, panelKonfig.zoom(), panelKonfig.zentriert(), letzterTimerZustand));
                    // irgendeineAenderung nur beim ersten Rendering setzen – laufende Timer-Updates
                    // kommen über onChange() direkt als diff-Push ohne Sheet-Refresh
                    var timerPanels = initialisiertePanels.computeIfAbsent(port, p -> ConcurrentHashMap.newKeySet());
                    if (timerPanels.add(i)) {
                        irgendeineAenderung = true;
                        erstesRendering = true;
                    }
                    continue;
                }

                // URL-Panel: kein Sheet-Lookup, nur Diff auf URL-Änderung
                if (panelKonfig.typ() == PanelTyp.URL) {
                    String letzteUrl = urlCache.get(i);
                    if (!java.util.Objects.equals(letzteUrl, panelKonfig.externeUrl())) {
                        panelNachrichten.add(CompositePanelNachricht.url(i, panelKonfig.externeUrl()));
                        urlCache.put(i, panelKonfig.externeUrl());
                        irgendeineAenderung = true;
                        erstesRendering = erstesRendering || letzteUrl == null;
                    }
                    continue;
                }

                var sheetOpt = panelKonfig.resolver().resolve(ws);
                if (sheetOpt.isEmpty()) {
                    if (erstesNichtGefundenesPanel == null) {
                        erstesNichtGefundenesPanel = panelKonfig.resolver().getAnzeigeName();
                    }
                    continue;
                }
                var sheet = sheetOpt.get();
                String neuerTitel = baueSeitenTitel(panelKonfig.resolver(), sheet);
                TabelleModel neuesModell = mapper.map(sheet, ws.getWorkingSpreadsheetDocument());
                TabelleModel altesModell = panelModelle.get(i);
                String alterTitel = panelTitel.get(i);

                if (altesModell != null && layoutGeaendert(altesModell, neuesModell)) {
                    altesModell = null;
                }

                TabelleModel diffModell = diffEngine.diff(altesModell, neuesModell);
                boolean panelGeaendert = altesModell == null
                        || !diffModell.getZellen().isEmpty()
                        || kopfFusszeileGeaendert(altesModell, neuesModell)
                        || !java.util.Objects.equals(alterTitel, neuerTitel);

                if (altesModell == null) {
                    erstesRendering = true;
                }

                panelModelle.put(i, neuesModell);
                panelTitel.put(i, neuerTitel);

                if (panelGeaendert) {
                    irgendeineAenderung = true;
                }
                // Beim ersten Rendering (altesModell == null) vollständiges Modell senden,
                // sonst nur Diff wenn nötig
                TabelleModel zuSendendesModell = (altesModell == null) ? neuesModell : diffModell;
                panelNachrichten.add(CompositePanelNachricht.init(i, zuSendendesModell, neuerTitel, panelKonfig.zoom(), panelKonfig.zentriert(), panelKonfig.blattnameAnzeigen()));
            }

            if (!irgendeineAenderung) {
                if (erstesNichtGefundenesPanel != null) {
                    sendeCompositeHinweis(instanz,
                            I18n.get("webserver.hinweis.sheet.nicht.gefunden.titel"),
                            I18n.get("webserver.hinweis.sheet.nicht.gefunden.text", erstesNichtGefundenesPanel));
                }
                return;
            }

            int version = compositeVersionen.get(port).incrementAndGet();

            // Init-Cache mit vollem State aller Panels befüllen
            var alleInitPanels = new ArrayList<CompositePanelNachricht>();
            for (int i = 0; i < konfig.panels().size(); i++) {
                var panelKonfig = konfig.panels().get(i);
                if (panelKonfig.typ() == PanelTyp.TIMER) {
                    alleInitPanels.add(CompositePanelNachricht.timer(i, panelKonfig.zoom(), panelKonfig.zentriert(), letzterTimerZustand));
                    continue;
                }
                if (panelKonfig.typ() == PanelTyp.URL) {
                    String cachedUrl = urlCache.get(i);
                    if (cachedUrl != null) {
                        alleInitPanels.add(CompositePanelNachricht.url(i, cachedUrl));
                    }
                    continue;
                }
                var vollModell = panelModelle.get(i);
                if (vollModell != null) {
                    alleInitPanels.add(CompositePanelNachricht.init(i, vollModell, panelTitel.getOrDefault(i, ""), panelKonfig.zoom(), panelKonfig.zentriert(), panelKonfig.blattnameAnzeigen()));
                }
            }
            CompositeSseNachricht initNachricht = CompositeSseNachricht.init(version, alleInitPanels, konfig.wurzel(), konfig.zoom());
            instanz.setCachedInitJson(GSON.toJson(initNachricht));

            // Push: beim ersten Rendering init, sonst diff mit geänderten Panels
            CompositeSseNachricht push = erstesRendering
                    ? initNachricht
                    : CompositeSseNachricht.diff(version, panelNachrichten, konfig.wurzel(), konfig.zoom());
            instanz.sseNachrichtPushen(GSON.toJson(push));

        } catch (Exception e) {
            logger.error("Fehler beim Rendern des Composite Views für Port {}: {}", port, e.getMessage(), e);
            safeProcessBoxFehler(I18n.get("webserver.prozessbox.render.fehler", port, e.getMessage()));
        }
    }

    private void sendeCompositeHinweis(CompositeViewInstanz instanz, String titel, String text) {
        String json = GSON.toJson(CompositeSseNachricht.hinweis(titel, text));
        instanz.setCachedInitJson(json);
        instanz.sseNachrichtPushen(json);
    }

    /**
     * Baut den Seitentitel aus dem Anzeigenamen des Resolvers und – wenn vorhanden – der laufenden Nummer.
     * Beispiel: "Spielrunde" + Nummer 3 → "Spielrunde 3"
     */
    private static String baueSeitenTitel(SheetResolver resolver, XSpreadsheet sheet) {
        var name = resolver.getAnzeigeName();
        return resolver.getNummer(sheet)
                .map(nr -> name + " " + nr)
                .orElse(name);
    }

    private void sendeHinweis(WebServerInstanz instanz, String titel, String text) {
        String json = GSON.toJson(SseNachricht.hinweis(titel, text));
        instanz.setCachedInitJson(json);
        instanz.sseNachrichtPushen(json);
    }

    private void sendeHinweisAnAlle(String titel, String text) {
        for (var instanz : instanzen) {
            sendeHinweis(instanz, titel, text);
        }
        for (var instanz : compositeInstanzen) {
            sendeCompositeHinweis(instanz, titel, text);
        }
    }

    public boolean isLaeuft() {
        return laeuft;
    }

    /**
     * Gibt true zurück wenn Slot (0-basiert) eine aktive, laufende Instanz hat.
     * Prüft explizit {@code instanz.laeuft()} – verhindert optimistisch-falsches Menü.
     */
    public synchronized boolean hatInstanzFuerSlot(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            return false;
        }
        var instanz = slots.get(slot);
        return instanz != null && instanz.laeuft();
    }

    /** true wenn irgendein Slot eine aktive Instanz hat (für Separator-Sichtbarkeit). */
    public synchronized boolean hatIrgendeinenAktivenSlot() {
        return slots.stream().anyMatch(i -> i != null && i.laeuft());
    }

    /**
     * Gibt die URL (Hostname:PORT) für den angegebenen Slot zurück, oder {@code null} wenn kein aktiver Port.
     * Gibt {@code null} zurück wenn slot außerhalb des gültigen Bereichs – kein Crash.
     */
    public synchronized String getUrlFuerSlot(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            return null;
        }
        var instanz = slots.get(slot);
        return (instanz != null && instanz.laeuft())
                ? buildUrl(instanz.getPort())
                : null;
    }

    /**
     * Gibt den Menü-Label für einen Slot zurück: "[AnzeigeName] – [URL]".
     * <p>
     * Der {@code AnzeigeName} stammt aus dem {@link SheetResolver} – er ist bereits auf spätere
     * Erweiterungen (mehrere Sheets pro Port) vorbereitet: der Resolver kann dann einen
     * kombinierten Namen zurückgeben, ohne dass diese Methode angepasst werden muss.
     *
     * @param slot 0-basierter Slot-Index
     * @return Label, oder {@code null} wenn kein aktiver Port für den Slot
     */
    public synchronized String getMenuLabelFuerSlot(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            return null;
        }
        var instanz = slots.get(slot);
        if (instanz == null || !instanz.laeuft()) {
            return null;
        }
        var url = buildUrl(instanz.getPort());
        var name = instanz.getAnzeigeName();
        return name + " – " + url;
    }

    /**
     * Prüft ob das übergebene Dokument das Owner-Dokument des laufenden WebServers ist.
     * Nur das Owner-Dokument darf den WebServer stoppen und sieht die URL-Slots als aktiv.
     *
     * @param doc das zu prüfende Dokument (darf null sein)
     * @return true wenn WS läuft und {@code doc} das Owner-Dokument ist
     */
    public synchronized boolean istOwnerDocument(XSpreadsheetDocument doc) {
        return laeuft && ownerDocument != null && doc != null && ownerDocument.equals(doc);
    }

    /**
     * Prüft ob sich das Frontend-Layout geändert hat: Zeilen-/Spaltenanzahl
     * oder die absolute Startposition des gerenderten Bereichs.
     * <p>
     * Reine Inhaltsänderungen ohne Strukturänderung werden hier nicht erkannt –
     * die {@link DiffEngine} übernimmt diesen Teil.
     */
    private static boolean layoutGeaendert(TabelleModel altes, TabelleModel neues) {
        return altes.getZeilen()      != neues.getZeilen()
            || altes.getSpalten()     != neues.getSpalten()
            || altes.getStartZeile()  != neues.getStartZeile()
            || altes.getStartSpalte() != neues.getStartSpalte();
    }

    /**
     * Prüft ob sich Kopf- oder Fußzeile geändert hat.
     * Nötig, da die {@link DiffEngine} nur Zellinhalte vergleicht.
     */
    private static boolean kopfFusszeileGeaendert(TabelleModel altes, TabelleModel neues) {
        return !java.util.Objects.equals(altes.getKopfzeileLinks(),  neues.getKopfzeileLinks())
            || !java.util.Objects.equals(altes.getKopfzeileMitte(),  neues.getKopfzeileMitte())
            || !java.util.Objects.equals(altes.getKopfzeileRechts(), neues.getKopfzeileRechts())
            || !java.util.Objects.equals(altes.getFusszeileLinks(),  neues.getFusszeileLinks())
            || !java.util.Objects.equals(altes.getFusszeileMitte(),  neues.getFusszeileMitte())
            || !java.util.Objects.equals(altes.getFusszeileRechts(), neues.getFusszeileRechts());
    }

    /** Baut eine URL aus einem Port – zentrale Methode, kein String-Duplikat. */
    private static String buildUrl(int port) {
        return "http://" + ermittleHostname() + ":" + port + "/";
    }

    /**
     * Ermittelt den Hostnamen oder die IP-Adresse dieses Rechners für die URL-Anzeige.
     * <p>
     * Reihenfolge: Systemhostname → erste nicht-loopback IPv4-Adresse → "localhost".
     */
    private static String ermittleHostname() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            if (hostname != null && !hostname.equals("localhost") && !hostname.startsWith("127.")) {
                return hostname;
            }
        } catch (Exception e) {
            logger.debug("Hostname nicht ermittelbar: {}", e.getMessage());
        }
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                var iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                var adressen = iface.getInetAddresses();
                while (adressen.hasMoreElements()) {
                    var adresse = adressen.nextElement();
                    if (adresse instanceof Inet4Address && !adresse.isLoopbackAddress()) {
                        return adresse.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Netzwerk-Interfaces nicht ermittelbar: {}", e.getMessage());
        }
        return "localhost";
    }

    /** ProcessBox-Info-Meldung, sicher auch wenn ProcessBox nicht initialisiert ist. */
    private static void safeProcessBoxInfo(String msg) {
        try {
            ProcessBox.from().info(msg);
        } catch (Exception e) {
            logger.debug("ProcessBox nicht verfügbar für Meldung: {}", msg);
        }
    }

    /** ProcessBox-Fehler-Meldung, sicher auch wenn ProcessBox nicht initialisiert ist. */
    private static void safeProcessBoxFehler(String msg) {
        try {
            ProcessBox.from().fehler(msg);
        } catch (Exception e) {
            logger.debug("ProcessBox nicht verfügbar für Fehlermeldung: {}", msg);
        }
    }
}
