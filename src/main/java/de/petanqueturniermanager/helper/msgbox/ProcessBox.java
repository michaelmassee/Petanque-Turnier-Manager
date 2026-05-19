package de.petanqueturniermanager.helper.msgbox;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XAnimation;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XTopWindow;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindow2;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.Log4J;
import de.petanqueturniermanager.comp.newrelease.ReleaseUpdateService;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.timer.TimerListener;
import de.petanqueturniermanager.timer.TimerState;
import de.petanqueturniermanager.timer.TimerZustand;

/**
 * Process-Statusfenster als modeloser UNO-Dialog. Zeigt Log-Ausgaben, einen
 * Throbber (LO-eigene Spinner-Animation), Statusicons für Erfolg/Fehler, eine
 * Timer-Zeile und eine optionale Hinweiszeile auf eine neue Plugin-Version.
 *
 * <p>Threading: Aufrufer aus Worker-Threads (z.B. {@link SheetRunner}) setzen
 * Properties direkt — die UNO-Bridge serialisiert intern über den SolarMutex.
 * Lifecycle-Operationen (Sichtbarkeit, Auto-Close) laufen ebenfalls direkt.
 */
public class ProcessBox implements TimerListener {

    private static final Logger logger = LogManager.getLogger(ProcessBox.class);

    private static final int AUTO_CLOSE_DELAY_MS = 5000;
    private static final String TITLE = "Pétanque Turnier Manager";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Dialog-Geometrie in AppFont-Einheiten (ca. 1.5 px pro Einheit)
    private static final int DLG_WIDTH = 320;
    private static final int DLG_HEIGHT = 180;
    private static final int PAD = 4;
    private static final int LOG_HEIGHT = 110;
    private static final int VERSION_Y = PAD + LOG_HEIGHT + 2;
    private static final int VERSION_HEIGHT = 10;
    private static final int TIMER_Y = VERSION_Y + VERSION_HEIGHT + 2;
    private static final int TIMER_HEIGHT = 10;
    private static final int FOOTER_Y = TIMER_Y + TIMER_HEIGHT + 2;
    private static final int FOOTER_HEIGHT = 14;
    private static final int STATUS_WIDTH = 20;
    private static final int BUTTON_WIDTH = 30;

    private static final int COLOR_VERSION = 0xCC4400;
    private static final int COLOR_TIMER_TITEL = 0x666666;
    private static final int COLOR_TIMER_LAEUFT = 0x00AA33;
    private static final int COLOR_TIMER_PAUSE = 0xBB9900;
    private static final int COLOR_TIMER_BEENDET = 0xCC0000;
    private static final int COLOR_TIMER_INAKTIV = 0x555555;

    private static volatile ProcessBox processBox;
    private static volatile boolean headlessMode = false;

    public static void setHeadlessMode(boolean headless) {
        headlessMode = headless;
    }

    public static ProcessBox from() {
        var pb = processBox;
        if (pb == null) {
            logger.error("ProcessBox nicht initialisiert");
            throw new IllegalStateException("ProcessBox nicht initialisiert");
        }
        return pb;
    }

    public static ProcessBox init(XComponentContext xContext) {
        logger.debug("ProcessBox INIT");
        checkNotNull(xContext);
        if (processBox == null) {
            synchronized (ProcessBox.class) {
                if (processBox == null) {
                    processBox = new ProcessBox(xContext);
                }
            }
        }
        return processBox;
    }

    public static ProcessBox forceinit(XComponentContext xContext) {
        checkNotNull(xContext);
        checkState(!SheetRunner.isRunning(), "forceinit darf nicht aufgerufen werden während SheetRunner aktiv ist");
        synchronized (ProcessBox.class) {
            if (processBox != null) {
                processBox._dispose();
            }
            processBox = new ProcessBox(xContext);
            return processBox;
        }
    }

    public static void dispose() {
        synchronized (ProcessBox.class) {
            if (processBox != null) {
                processBox._dispose();
                processBox = null;
            }
        }
    }

    public static void zeigeImVordergrund() {
        var pb = processBox;
        if (pb != null) {
            pb.visible();
            pb.toFront();
        }
    }

    public static void applyVordergrundEinstellung() {
        var pb = processBox;
        if (pb == null || pb.disposed) return;
        boolean automatisch = GlobalProperties.get().isProzessBoxAutomatischAnzeigen();
        pb.setVisibleInternal(automatisch);
        if (automatisch) {
            pb.toFrontInternal();
        }
    }

    // ── Instanz-State ──────────────────────────────────────────────────────────

    private final XComponentContext xContext;
    private final ThreadLocal<String> threadLocalPrefix = new ThreadLocal<>();
    private final AtomicBoolean laeuft = new AtomicBoolean(false);
    private final Runnable versionsStatusListener = this::aktualisiereNeueVersionLabel;

    private volatile boolean disposed = false;
    private volatile boolean isFehler = false;

    // UNO-Controls (null im Headless-Modus oder wenn Init fehlschlug)
    private XControl dialogControl;
    private XDialog xDialog;
    private XWindow xWindow;
    private XControlContainer controls;

    private XPropertySet logEditProps;
    private XPropertySet infoLabelProps;
    private XPropertySet neueVersionLabelProps;
    private XPropertySet timerUhrProps;
    private XPropertySet timerBezeichnungProps;
    private XPropertySet throbberProps;
    private XAnimation throbber;
    private XPropertySet readyImageProps;
    private XPropertySet errorImageProps;
    private XPropertySet stopBtnProps;

    private ScheduledExecutorService autoCloseExec;
    private ScheduledFuture<?> autoCloseTask;

    private ProcessBox(XComponentContext xContext) {
        this.xContext = checkNotNull(xContext);
        if (headlessMode) {
            return;
        }
        try {
            initDialog();
        } catch (RuntimeException | com.sun.star.uno.Exception e) {
            logger.error("ProcessBox-Dialog konnte nicht initialisiert werden", e);
        }
        try {
            ReleaseUpdateService.get().addStatusListener(versionsStatusListener);
            aktualisiereNeueVersionLabel();
        } catch (IllegalStateException e) {
            logger.debug("ReleaseUpdateService noch nicht initialisiert – Versions-Label inaktiv");
        }
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private void initDialog() throws com.sun.star.uno.Exception {
        XMultiComponentFactory mcf = xContext.getServiceManager();

        // Dialog-Modell
        Object dialogModel = mcf.createInstanceWithContext(
                "com.sun.star.awt.UnoControlDialogModel", xContext);
        XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
        dlgProps.setPropertyValue("PositionX", 60);
        dlgProps.setPropertyValue("PositionY", 60);
        dlgProps.setPropertyValue("Width", DLG_WIDTH);
        dlgProps.setPropertyValue("Height", DLG_HEIGHT);
        dlgProps.setPropertyValue("Moveable", Boolean.TRUE);
        dlgProps.setPropertyValue("Sizeable", Boolean.TRUE);
        dlgProps.setPropertyValue("Closeable", Boolean.TRUE);
        dlgProps.setPropertyValue("Title", TITLE);

        XMultiServiceFactory msf = Lo.qi(XMultiServiceFactory.class, dialogModel);
        XNameContainer modelContainer = Lo.qi(XNameContainer.class, dialogModel);

        // Log-Edit
        logEditProps = addControl(msf, modelContainer, "logEdit",
                "com.sun.star.awt.UnoControlEditModel",
                PAD, PAD, DLG_WIDTH - 2 * PAD, LOG_HEIGHT, m -> {
                    m.setPropertyValue("MultiLine", Boolean.TRUE);
                    m.setPropertyValue("ReadOnly", Boolean.TRUE);
                    m.setPropertyValue("VScroll", Boolean.TRUE);
                });

        // Neue-Version-Hinweis
        neueVersionLabelProps = addControl(msf, modelContainer, "nvLabel",
                "com.sun.star.awt.UnoControlFixedTextModel",
                PAD, VERSION_Y, DLG_WIDTH - 2 * PAD, VERSION_HEIGHT, m -> {
                    m.setPropertyValue("Align", (short) 1);
                    m.setPropertyValue("TextColor", COLOR_VERSION);
                    m.setPropertyValue("EnableVisible", Boolean.FALSE);
                });

        // Timer-Zeile
        addControl(msf, modelContainer, "timerTitel",
                "com.sun.star.awt.UnoControlFixedTextModel",
                PAD, TIMER_Y, 80, TIMER_HEIGHT, m -> {
                    m.setPropertyValue("Label", I18n.get("timer.processbox.zeile.label") + ":");
                    m.setPropertyValue("TextColor", COLOR_TIMER_TITEL);
                });
        timerUhrProps = addControl(msf, modelContainer, "timerUhr",
                "com.sun.star.awt.UnoControlFixedTextModel",
                PAD + 82, TIMER_Y, 50, TIMER_HEIGHT, m -> {
                    m.setPropertyValue("Label", "--:--");
                    m.setPropertyValue("TextColor", COLOR_TIMER_INAKTIV);
                });
        timerBezeichnungProps = addControl(msf, modelContainer, "timerBez",
                "com.sun.star.awt.UnoControlFixedTextModel",
                PAD + 134, TIMER_Y, DLG_WIDTH - PAD - 138, TIMER_HEIGHT, m -> {
                    m.setPropertyValue("Label", "");
                    m.setPropertyValue("TextColor", COLOR_TIMER_TITEL);
                });

        // Status-Bereich (überlappend an Position PAD/FOOTER_Y):
        // Throbber (sichtbar während run), Ready/Error-Bild (sichtbar nach ready)
        throbberProps = addControl(msf, modelContainer, "throbber",
                "com.sun.star.awt.SpinningProgressControlModel",
                PAD, FOOTER_Y, STATUS_WIDTH, FOOTER_HEIGHT, m -> {
                    m.setPropertyValue("EnableVisible", Boolean.FALSE);
                });

        String readyUrl = extractImageToTemp("check25x32.png");
        readyImageProps = addControl(msf, modelContainer, "readyImg",
                "com.sun.star.awt.UnoControlImageControlModel",
                PAD, FOOTER_Y, STATUS_WIDTH, FOOTER_HEIGHT, m -> {
                    if (readyUrl != null) {
                        m.setPropertyValue("ImageURL", readyUrl);
                    }
                    m.setPropertyValue("ScaleImage", Boolean.TRUE);
                    m.setPropertyValue("Border", (short) 0);
                });

        String errorUrl = extractImageToTemp("cross32x32.png");
        errorImageProps = addControl(msf, modelContainer, "errorImg",
                "com.sun.star.awt.UnoControlImageControlModel",
                PAD, FOOTER_Y, STATUS_WIDTH, FOOTER_HEIGHT, m -> {
                    if (errorUrl != null) {
                        m.setPropertyValue("ImageURL", errorUrl);
                    }
                    m.setPropertyValue("ScaleImage", Boolean.TRUE);
                    m.setPropertyValue("Border", (short) 0);
                    m.setPropertyValue("EnableVisible", Boolean.FALSE);
                });

        // Info-Label rechts neben Status
        int infoX = PAD + STATUS_WIDTH + 4;
        int buttonsBlock = 2 * BUTTON_WIDTH + 8;
        infoLabelProps = addControl(msf, modelContainer, "infoLbl",
                "com.sun.star.awt.UnoControlFixedTextModel",
                infoX, FOOTER_Y + 2, DLG_WIDTH - infoX - buttonsBlock - PAD, FOOTER_HEIGHT - 4, m -> {
                    m.setPropertyValue("Label", "..");
                    m.setPropertyValue("Align", (short) 0);
                });

        // Buttons
        addControl(msf, modelContainer, "logBtn",
                "com.sun.star.awt.UnoControlButtonModel",
                DLG_WIDTH - 2 * BUTTON_WIDTH - PAD - 4, FOOTER_Y, BUTTON_WIDTH, FOOTER_HEIGHT, m -> {
                    m.setPropertyValue("Label", "Log");
                    m.setPropertyValue("HelpText", "Logdatei");
                });
        stopBtnProps = addControl(msf, modelContainer, "stopBtn",
                "com.sun.star.awt.UnoControlButtonModel",
                DLG_WIDTH - BUTTON_WIDTH - PAD, FOOTER_Y, BUTTON_WIDTH, FOOTER_HEIGHT, m -> {
                    m.setPropertyValue("Label", "Stop");
                    m.setPropertyValue("HelpText", "Stop Verarbeitung");
                    m.setPropertyValue("Enabled", Boolean.FALSE);
                });

        // Dialog-Control + Peer
        Object dialog = mcf.createInstanceWithContext(
                "com.sun.star.awt.UnoControlDialog", xContext);
        dialogControl = Lo.qi(XControl.class, dialog);
        dialogControl.setModel(Lo.qi(XControlModel.class, dialogModel));
        xDialog = Lo.qi(XDialog.class, dialog);
        xWindow = Lo.qi(XWindow.class, dialogControl);

        Object toolkit = mcf.createInstanceWithContext("com.sun.star.awt.Toolkit", xContext);
        XToolkit xToolkit = Lo.qi(XToolkit.class, toolkit);
        xWindow.setVisible(false);
        dialogControl.createPeer(xToolkit, null);

        controls = Lo.qi(XControlContainer.class, dialog);

        // Throbber-Animation auflösen
        XControl throbberControl = controls.getControl("throbber");
        throbber = Lo.qi(XAnimation.class, throbberControl);

        // Button-Listener
        XButton logBtn = Lo.qi(XButton.class, controls.getControl("logBtn"));
        if (logBtn != null) {
            logBtn.addActionListener(new ButtonListener(_ -> Log4J.openLogFile()));
        }
        XButton stopBtn = Lo.qi(XButton.class, controls.getControl("stopBtn"));
        if (stopBtn != null) {
            stopBtn.addActionListener(new ButtonListener(_ -> SheetRunner.cancelRunner()));
        }
    }

    @FunctionalInterface
    private interface ModelInitializer {
        void apply(XPropertySet model) throws com.sun.star.uno.Exception;
    }

    private static XPropertySet addControl(XMultiServiceFactory msf, XNameContainer container,
            String name, String service, int x, int y, int w, int h,
            ModelInitializer initializer) throws com.sun.star.uno.Exception {
        Object model = msf.createInstance(service);
        XPropertySet props = Lo.qi(XPropertySet.class, model);
        props.setPropertyValue("PositionX", x);
        props.setPropertyValue("PositionY", y);
        props.setPropertyValue("Width", w);
        props.setPropertyValue("Height", h);
        props.setPropertyValue("Name", name);
        initializer.apply(props);
        container.insertByName(name, model);
        return props;
    }

    /** Extrahiert eine PNG-Resource in eine temporäre Datei und liefert die {@code file://}-URL. */
    private String extractImageToTemp(String resourceName) {
        try (InputStream in = getClass().getResourceAsStream(resourceName)) {
            if (in == null) {
                logger.warn("Resource nicht gefunden: {}", resourceName);
                return null;
            }
            Path tmp = Files.createTempFile("ptm-" + resourceName.replace('.', '-') + "-", ".png");
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            return tmp.toUri().toString();
        } catch (IOException e) {
            logger.warn("Konnte Resource {} nicht extrahieren", resourceName, e);
            return null;
        }
    }

    // ── Dispose ────────────────────────────────────────────────────────────────

    private void _dispose() {
        disposed = true;
        laeuft.set(false);
        try {
            ReleaseUpdateService.get().removeStatusListener(versionsStatusListener);
        } catch (IllegalStateException e) {
            // Service wurde nie initialisiert – ok.
        }
        stopAutoCloseTask();
        if (autoCloseExec != null) {
            autoCloseExec.shutdownNow();
            autoCloseExec = null;
        }
        try {
            if (throbber != null && throbber.isAnimationRunning()) {
                throbber.stopAnimation();
            }
        } catch (RuntimeException e) {
            logger.debug("Throbber-Stop beim Dispose fehlgeschlagen", e);
        }
        if (xWindow != null) {
            try {
                xWindow.setVisible(false);
            } catch (RuntimeException e) {
                logger.debug("setVisible(false) beim Dispose fehlgeschlagen", e);
            }
        }
        if (dialogControl != null) {
            try {
                XComponent xc = Lo.qi(XComponent.class, dialogControl);
                if (xc != null) {
                    xc.dispose();
                }
            } catch (RuntimeException e) {
                logger.debug("Dialog-Dispose fehlgeschlagen", e);
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public ProcessBox clearWennNotRunning() {
        if (!disposed && !SheetRunner.isRunning()) {
            clear();
        }
        return this;
    }

    public ProcessBox infoText(String newInfoText) {
        if (disposed || infoLabelProps == null) return this;
        setPropertySafe(infoLabelProps, "Label", newInfoText != null ? newInfoText : "");
        return this;
    }

    public ProcessBox prefix(String nextLogPrefix) {
        if (disposed) return this;
        if (nextLogPrefix != null) {
            threadLocalPrefix.set(nextLogPrefix);
        } else {
            threadLocalPrefix.remove();
        }
        return this;
    }

    public ProcessBox clear() {
        if (disposed || logEditProps == null) return this;
        isFehler = false;
        setPropertySafe(logEditProps, "Text", "");
        return this;
    }

    public synchronized ProcessBox fehler(String logMsg) {
        info(I18n.get("processbox.fehler.prefix") + " " + logMsg);
        isFehler = true;
        return this;
    }

    public synchronized ProcessBox info(String logMsg) {
        logger.debug("ProcessBox info -> {}", logMsg);
        try {
            if (disposed || headlessMode || logEditProps == null) return this;
            checkNotNull(logMsg);

            var currentPrefix = threadLocalPrefix.get();
            String msgToAppend = LocalTime.now().format(TIME_FORMATTER) + " | "
                    + (currentPrefix != null ? currentPrefix + ": " : "")
                    + logMsg + "\r\n";
            appendLog(msgToAppend);
            planeAutoCloseFallsMoeglich();
            return this;
        } finally {
            threadLocalPrefix.remove();
        }
    }

    private void appendLog(String zeile) {
        try {
            Object current = logEditProps.getPropertyValue("Text");
            String currentStr = current instanceof String s ? s : "";
            logEditProps.setPropertyValue("Text", currentStr + zeile);
        } catch (com.sun.star.uno.Exception e) {
            logger.debug("appendLog fehlgeschlagen", e);
        }
    }

    public ProcessBox title(String title) {
        if (disposed || xDialog == null) return this;
        try {
            xDialog.setTitle(title);
        } catch (RuntimeException e) {
            logger.debug("setTitle fehlgeschlagen", e);
        }
        return this;
    }

    public ProcessBox moveInsideTopWindow() {
        // UNO-Dialog wird vom Toolkit ohnehin innerhalb des LO-Fensters platziert.
        return this;
    }

    public boolean istSichtbar() {
        if (disposed || xWindow == null) return false;
        try {
            XWindow2 xw2 = Lo.qi(XWindow2.class, xWindow);
            return xw2 != null && xw2.isVisible();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public ProcessBox hide() {
        if (disposed || xWindow == null) return this;
        setVisibleInternal(false);
        return this;
    }

    public ProcessBox visible() {
        if (disposed || xWindow == null) return this;
        setVisibleInternal(true);
        return this;
    }

    public ProcessBox toFront() {
        if (disposed || xWindow == null) return this;
        if (!GlobalProperties.get().isProzessBoxAutomatischAnzeigen()) return this;
        toFrontInternal();
        return this;
    }

    public ProcessBox visibleWennAutomatisch() {
        if (disposed || xWindow == null) return this;
        if (!GlobalProperties.get().isProzessBoxAutomatischAnzeigen()) return this;
        return visible();
    }

    public ProcessBox run() {
        logger.debug("ProcessBox run");
        if (headlessMode || disposed || xWindow == null) return this;
        if (!laeuft.compareAndSet(false, true)) {
            return this;
        }
        if (disposed) {
            laeuft.set(false);
            return this;
        }
        stopAutoCloseTask();

        boolean automatisch = GlobalProperties.get().isProzessBoxAutomatischAnzeigen();
        if (automatisch) {
            setVisibleInternal(true);
            toFrontInternal();
        }
        if (stopBtnProps != null) {
            setPropertySafe(stopBtnProps, "Enabled", Boolean.TRUE);
        }

        // Throbber sichtbar machen + starten, Ready/Error verstecken
        setPropertySafe(readyImageProps, "EnableVisible", Boolean.FALSE);
        setPropertySafe(errorImageProps, "EnableVisible", Boolean.FALSE);
        setPropertySafe(throbberProps, "EnableVisible", Boolean.TRUE);
        if (throbber != null) {
            try {
                throbber.startAnimation();
            } catch (RuntimeException e) {
                logger.debug("Throbber-Start fehlgeschlagen", e);
            }
        }
        return this;
    }

    public ProcessBox ready() {
        if (headlessMode || disposed || xWindow == null) return this;
        if (!laeuft.getAndSet(false)) {
            return this;
        }

        boolean automatisch = GlobalProperties.get().isProzessBoxAutomatischAnzeigen();
        if (automatisch) {
            setVisibleInternal(true);
            toFrontInternal();
        }
        if (stopBtnProps != null) {
            setPropertySafe(stopBtnProps, "Enabled", Boolean.FALSE);
        }

        if (throbber != null) {
            try {
                throbber.stopAnimation();
            } catch (RuntimeException e) {
                logger.debug("Throbber-Stop fehlgeschlagen", e);
            }
        }
        setPropertySafe(throbberProps, "EnableVisible", Boolean.FALSE);
        if (isFehler) {
            setPropertySafe(readyImageProps, "EnableVisible", Boolean.FALSE);
            setPropertySafe(errorImageProps, "EnableVisible", Boolean.TRUE);
        } else {
            setPropertySafe(errorImageProps, "EnableVisible", Boolean.FALSE);
            setPropertySafe(readyImageProps, "EnableVisible", Boolean.TRUE);
        }

        planeAutoCloseFallsMoeglich();
        return this;
    }

    /** Liefert das XWindow des Dialogs (oder null im Headless-Modus / vor Init). */
    public final XWindow getXWindow() {
        return xWindow;
    }

    /** Liefert das XAnimation des Throbber-Controls (für Tests). */
    public final XAnimation getThrobberAnimation() {
        return throbber;
    }

    /** Liefert den aktuellen Log-Text (für Tests). */
    public final String getLogText() {
        if (logEditProps == null) return "";
        try {
            Object o = logEditProps.getPropertyValue("Text");
            return o instanceof String s ? s : "";
        } catch (com.sun.star.uno.Exception e) {
            return "";
        }
    }

    // ── Auto-Close ─────────────────────────────────────────────────────────────

    private void planeAutoCloseFallsMoeglich() {
        if (disposed || xWindow == null || headlessMode) return;
        if (laeuft.get()) return;
        if (isFehler) return;
        if (!GlobalProperties.get().isProzessBoxAutomatischAnzeigen()) return;
        if (!GlobalProperties.get().isProzessBoxAutomatischSchliessen()) return;

        stopAutoCloseTask();
        if (autoCloseExec == null) {
            autoCloseExec = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ProcessBox-AutoClose");
                t.setDaemon(true);
                return t;
            });
        }
        autoCloseTask = autoCloseExec.schedule(() -> {
            if (disposed || xWindow == null) return;
            if (laeuft.get() || isFehler) return;
            setVisibleInternal(false);
        }, AUTO_CLOSE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void stopAutoCloseTask() {
        ScheduledFuture<?> task = autoCloseTask;
        if (task != null) {
            task.cancel(false);
            autoCloseTask = null;
        }
    }

    // ── Versions-Hinweis ───────────────────────────────────────────────────────

    private void aktualisiereNeueVersionLabel() {
        if (neueVersionLabelProps == null) return;
        boolean neueVersion;
        String installierteVersion;
        String neueVersionNummer;
        try {
            var service = ReleaseUpdateService.get();
            neueVersion = service.isUpdateVerfuegbar()
                    || GlobalProperties.get().isNewVersionCheckImmerTrue();
            installierteVersion = service.getInstallierteVersion().orElse(null);
            neueVersionNummer = service.getNeuesteVersionTag().orElse(null);
        } catch (IllegalStateException e) {
            return;
        }
        if (disposed) return;
        if (neueVersion) {
            setPropertySafe(neueVersionLabelProps, "Label",
                    I18n.get("processbox.neue.version.verfuegbar",
                            installierteVersion != null ? installierteVersion : "?",
                            neueVersionNummer != null ? neueVersionNummer : "?"));
        }
        setPropertySafe(neueVersionLabelProps, "EnableVisible", neueVersion);
    }

    // ── TimerListener ──────────────────────────────────────────────────────────

    @Override
    public void onChange(TimerState state) {
        if (disposed || headlessMode || timerUhrProps == null) return;
        if (state.zustand() == TimerZustand.BEENDET) {
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
        setPropertySafe(timerUhrProps, "Label", state.anzeige());
        setPropertySafe(timerBezeichnungProps, "Label",
                state.bezeichnung() != null ? state.bezeichnung() : "");
        Integer farbe = switch (state.zustand()) {
            case LAEUFT   -> COLOR_TIMER_LAEUFT;
            case PAUSIERT -> COLOR_TIMER_PAUSE;
            case BEENDET  -> COLOR_TIMER_BEENDET;
            case INAKTIV  -> COLOR_TIMER_INAKTIV;
        };
        setPropertySafe(timerUhrProps, "TextColor", farbe);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void setVisibleInternal(boolean sichtbar) {
        if (xWindow == null) return;
        try {
            xWindow.setVisible(sichtbar);
        } catch (RuntimeException e) {
            logger.debug("setVisible({}) fehlgeschlagen", sichtbar, e);
        }
    }

    private void toFrontInternal() {
        if (dialogControl == null) return;
        try {
            XTopWindow top = Lo.qi(XTopWindow.class, dialogControl.getPeer());
            if (top != null) {
                top.toFront();
            }
        } catch (RuntimeException e) {
            logger.debug("toFront fehlgeschlagen", e);
        }
    }

    private static void setPropertySafe(XPropertySet props, String name, Object value) {
        if (props == null) return;
        try {
            props.setPropertyValue(name, value);
        } catch (com.sun.star.uno.Exception e) {
            logger.debug("setPropertyValue({}, ...) fehlgeschlagen", name, e);
        }
    }

    // ── Listener-Helfer ────────────────────────────────────────────────────────

    /** Adapter, der eine Consumer-Callback-Funktion an XActionListener bindet. */
    private record ButtonListener(java.util.function.Consumer<ActionEvent> handler)
            implements XActionListener {

        private ButtonListener {
            Objects.requireNonNull(handler);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                handler.accept(e);
            } catch (RuntimeException ex) {
                logger.error("Button-Handler-Fehler", ex);
            }
        }

        @Override
        public void disposing(EventObject e) {
            // nichts
        }
    }

}
