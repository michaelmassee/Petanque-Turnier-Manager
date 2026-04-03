package de.petanqueturniermanager.helper.msgbox;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;

import java.awt.Font;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.Log4J;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;
import de.petanqueturniermanager.helper.i18n.I18n;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ProcessBox {
    private static final Logger logger = LogManager.getLogger(ProcessBox.class);

    private static final int ANZAHLSPALTEN = 1;

    private static final int MIN_HEIGHT = 200;
    private static final int MIN_WIDTH = 600;
    private static final String TITLE = "Pétanque Turnier Manager";
    private static volatile ProcessBox processBox;
    private static boolean headlessMode = false;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");


    public static void setHeadlessMode(boolean headless) {
        headlessMode = headless;
    }

    private final JFrame frame;
    private JTextArea logOut;
    private JButton logfileBtn;
    private JButton cancelBtn;
    private final ThreadLocal<String> threadLocalPrefix = new ThreadLocal<>();

    private JLabel statusLabel;
    private JLabel infoLabel;
    private JLabel neueVersionLabel;

    private ImageIcon imageIconReady;
    private ImageIcon imageIconError;

    private ArrayList<ImageIcon> inworkIcons;

    private DialogTools dialogTools;
    private Timer spinnerTimer;
    private int inWorkImgIdx = 0;

    private volatile boolean disposed = false;
    private final AtomicBoolean laeuft = new AtomicBoolean(false);
    private boolean isFehler = false;
    private final XComponentContext xContext;

    private ProcessBox(XComponentContext xContext) {
        this.xContext = checkNotNull(xContext);
        if (headlessMode) {
            frame = null;
            return;
        }
        frame = new JFrame();
        dialogTools = DialogTools.from(xContext, frame);
        inworkIcons = new ArrayList<>();
        spinnerTimer = new Timer(100, _ -> {
            statusLabel.setIcon(inworkIcons.get(inWorkImgIdx));
            inWorkImgIdx = (inWorkImgIdx + 1) % inworkIcons.size();
        });
        logOut = null;
        logfileBtn = null;
        cancelBtn = null;
        statusLabel = null;
        neueVersionLabel = null;
        imageIconReady = null;
        imageIconError = null;
        initBox();
    }

    public static ProcessBox from() {
        var pb = ProcessBox.processBox; // ein einzelner volatile-Lesezugriff
        if (pb == null) {
            logger.error("ProcessBox nicht initialisiert");
            throw new IllegalStateException("ProcessBox nicht initialisiert");
        }
        return pb;
    }

    /**
     * Box schließen, kann wieder geöfnet werden
     */
    public static void dispose() {
        synchronized (ProcessBox.class) {
            if (ProcessBox.processBox != null) {
                ProcessBox.processBox._dispose();
                ProcessBox.processBox = null;
            }
        }
    }

    /**
     * nur intern verwenden
     */
    private void _dispose() {
        disposed = true; // sofort: alle anderen Threads schlagen ab
        laeuft.set(false);
        SwingUtilities.invokeLater(() -> {
            if (spinnerTimer != null) {
                spinnerTimer.stop();
            }
            if (frame != null) {
                frame.dispose();
            }
        });
        dialogTools = null;
    }


    /**
     * Einmalige Initialisierung; bei bereits vorhandener Instanz wird diese zurückgegeben.
     */
    public static ProcessBox init(XComponentContext xContext) {
        logger.debug("ProcessBox INIT");
        checkNotNull(xContext);
        if (ProcessBox.processBox == null) {
            synchronized (ProcessBox.class) {
                if (ProcessBox.processBox == null) {
                    ProcessBox.processBox = new ProcessBox(xContext);
                }
            }
        }
        return ProcessBox.processBox;
    }

    public static ProcessBox forceinit(XComponentContext xContext) {
        checkNotNull(xContext);
        checkState(!SheetRunner.isRunning(), "forceinit darf nicht aufgerufen werden während SheetRunner aktiv ist");
        synchronized (ProcessBox.class) {
            if (ProcessBox.processBox != null) {
                ProcessBox.processBox._dispose();
            }
            ProcessBox.processBox = new ProcessBox(xContext);
            return ProcessBox.processBox;
        }
    }

    private void initBox() {
        logger.debug("ProcessBox.initBox");
        frame.setLayout(new GridBagLayout());
        setIcons();

        // log
        initLog();
        // Neue-Version-Hinweiszeile (nur sichtbar wenn neue Version verfügbar)
        initNeueVersionZeile();
        // Footer: Status, Info, Log, Stop
        initSpieltagUndSpielrundInfo();

        frame.setPreferredSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        frame.setSize(MIN_WIDTH, MIN_HEIGHT);
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        title(TITLE);

        // Callback registrieren: Neue-Version-Label einblenden sobald Cache-Thread fertig
        NewReleaseChecker.addCacheUpdateCallback(this::aktualisiereNeueVersionLabel);
    }

    private void initNeueVersionZeile() {
        neueVersionLabel = new JLabel();
        neueVersionLabel.setForeground(new Color(0xcc4400));
        neueVersionLabel.setFont(neueVersionLabel.getFont().deriveFont(Font.BOLD, 13f));
        neueVersionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        neueVersionLabel.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 5, 2, 5);
        frame.add(neueVersionLabel, gbc);
    }

    private void aktualisiereNeueVersionLabel() {
        if (neueVersionLabel == null) {
            return;
        }
        var checker = new NewReleaseChecker();
        boolean neueVersion = checker.checkForNewRelease(xContext);
        String installierteVersion = ExtensionsHelper.from(xContext).getVersionNummer();
        String neueVersionNummer = checker.latestVersionFromCacheFile();
        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            if (neueVersion) {
                neueVersionLabel.setText(I18n.get("processbox.neue.version.verfuegbar",
                        installierteVersion != null ? installierteVersion : "?",
                        neueVersionNummer != null ? neueVersionNummer : "?"));
            }
            neueVersionLabel.setVisible(neueVersion);
            frame.revalidate();
        });
    }

    private void setIcons() {
        // icons laden
        try {
            var logoStream = this.getClass().getResourceAsStream("petanqueturniermanager-logo-256px.png");
            if (logoStream != null) {
                BufferedImage img256 = ImageIO.read(logoStream);
                frame.setIconImages(java.util.List.of(img256));
            }

            // https://loading.io/
            // https://ezgif.com/
            for (int i = 0; i < 31; i++) {
                var stream = this.getClass().getResourceAsStream("spinner/frame-" + i + ".png");
                if (stream != null) {
                    inworkIcons.add(new ImageIcon(ImageIO.read(stream)));
                }
            }
            var readyStream = this.getClass().getResourceAsStream("check25x32.png");
            if (readyStream != null) {
                imageIconReady = new ImageIcon(ImageIO.read(readyStream));
            }
            var errorStream = this.getClass().getResourceAsStream("cross32x32.png");
            if (errorStream != null) {
                imageIconError = new ImageIcon(ImageIO.read(errorStream));
            }
        } catch (IOException e) {
            logger.debug(e);
        }
    }

    private void initSpieltagUndSpielrundInfo() {

        // -----------------------------
        // Footer Panel
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(Integer.parseInt("eaf4ff", 16));
                Color color2 = new Color(Integer.parseInt("d6e9ff", 16));
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };

        Border raisedbevel = BorderFactory.createRaisedBevelBorder();
        panel.setBorder(raisedbevel);
        {
            GridBagConstraints gridBagConstraintsFrame = new GridBagConstraints();
            gridBagConstraintsFrame.gridy = 2; // zeile
            gridBagConstraintsFrame.gridx = 0; // spalte
            gridBagConstraintsFrame.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraintsFrame.weightx = 0.5;
            gridBagConstraintsFrame.insets = new Insets(0, 5, 5, 5);
            frame.add(panel, gridBagConstraintsFrame);
        }
        // -----------------------------

        GridBagConstraints gridBagConstraintsPanel = new GridBagConstraints();
        gridBagConstraintsPanel.gridy = 0; // zeile
        gridBagConstraintsPanel.gridx = 0;

        // Working/ERROR/OKAY Image
        // https://www.flaticon.com/packs/electronic-and-web-element-collection-2
        statusLabel = new JLabel(imageIconReady);
        statusLabel.setToolTipText("status");
        statusLabel.setVisible(true);
        gridBagConstraintsPanel.insets = new Insets(0, 5, 0, 30);
        gridBagConstraintsPanel.anchor = GridBagConstraints.WEST;
        panel.add(statusLabel, gridBagConstraintsPanel);
        gridBagConstraintsPanel.gridx++;

        gridBagConstraintsPanel.insets = new Insets(0, 5, 0, 0);

        infoLabel = new JLabel("..");
        infoLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(infoLabel, gridBagConstraintsPanel);
        gridBagConstraintsPanel.gridx++;

        // ----------------------------------------------------------
        {
            logfileBtn = new JButton("Log");
            logfileBtn.setEnabled(true);
            logfileBtn.setToolTipText("Logdatei");
            logfileBtn.addActionListener(_ -> Log4J.openLogFile());

            gridBagConstraintsPanel.insets = new Insets(0, 5, 0, 5);
            gridBagConstraintsPanel.anchor = GridBagConstraints.EAST;
            gridBagConstraintsPanel.weightx = 0.5;
            panel.add(logfileBtn, gridBagConstraintsPanel);
        }
        gridBagConstraintsPanel.gridx++; // spalte
        // ----------------------------------------------------------
        {
            cancelBtn = new JButton("Stop");
            cancelBtn.setEnabled(false);
            cancelBtn.setToolTipText("Stop verarbeitung");
            cancelBtn.addActionListener(_ -> SheetRunner.cancelRunner());

            gridBagConstraintsPanel.insets = new Insets(0, 5, 0, 5);
            gridBagConstraintsPanel.anchor = GridBagConstraints.EAST;
            gridBagConstraintsPanel.weightx = 0.5;
            panel.add(cancelBtn, gridBagConstraintsPanel);
        }
        // ----------------------------------------------------------
    }

    private void initLog() {
        logOut = new JTextArea();
        logOut.setEditable(false);
        logOut.setLineWrap(false);
        // move to the last pos
        ((DefaultCaret) logOut.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(logOut);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0; // spalte
        gridBagConstraints.gridy = 0; // zeile

        gridBagConstraints.gridwidth = ANZAHLSPALTEN;

        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);

        gridBagConstraints.fill = GridBagConstraints.BOTH;
        frame.add(scrollPane, gridBagConstraints);
    }

    public ProcessBox clearWennNotRunning() {
        if (!disposed && !SheetRunner.isRunning()) {
            clear();
        }
        return this;
    }

    public ProcessBox infoText(String newInfoText) {
        if (disposed || infoLabel == null) return this;
        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            infoLabel.setText(newInfoText);
        });
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
        if (disposed || logOut == null) return this;
        isFehler = false;
        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            logOut.setText(null);
        });
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
            if (disposed || headlessMode || logOut == null) return this;

            checkNotNull(logMsg);

            var currentPrefix = threadLocalPrefix.get();
            final String msgToAppend = LocalTime.now().format(TIME_FORMATTER) + " | " +
                    (currentPrefix != null ? currentPrefix + ": " : "") +
                    logMsg + "\r\n";

            SwingUtilities.invokeLater(() -> {
                if (disposed) return;
                logOut.append(msgToAppend);
            });
            return this;
        } finally {
            threadLocalPrefix.remove();
        }
    }


    public ProcessBox title(String title) {
        if (disposed || frame == null) return this;
        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            frame.setTitle(title);
        });
        return this;
    }

    public ProcessBox moveInsideTopWindow() {
        if (disposed || dialogTools == null) return this;
        dialogTools.moveInsideTopWindow();
        return this;
    }

    /**
     * Zeigt die ProcessBox im Vordergrund – sicher aufrufbar auch wenn noch nicht initialisiert.
     */
    public static void zeigeImVordergrund() {
        var pb = processBox;
        if (pb != null) {
            pb.visible();
            pb.toFront();
        }
    }

    public ProcessBox hide() {
        if (disposed || frame == null) return this;
        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            frame.setVisible(false);
        });
        return this;
    }

    public ProcessBox visible() {
        if (disposed || frame == null) return this;
        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            frame.setVisible(true);
            moveInsideTopWindow();
        });
        return this;
    }

    public ProcessBox toFront() {
        if (disposed || frame == null) return this;
        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            frame.toFront();
        });
        return this;
    }

    public ProcessBox run() {
        logger.debug("ProcessBox run");
        if (headlessMode || disposed || frame == null) return this;
        if (!laeuft.compareAndSet(false, true)) {
            return this;
        }
        if (disposed) {
            laeuft.set(false);
            return this;
        }

        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            frame.toFront();
            if (cancelBtn != null) {
                cancelBtn.setEnabled(true);
            }
            statusLabel.setToolTipText("In Arbeit");
            spinnerTimer.restart();
        });
        return this;
    }

    public ProcessBox ready() {
        if (headlessMode || disposed || frame == null) return this;
        if (!laeuft.getAndSet(false)) {
            return this;
        }

        SwingUtilities.invokeLater(() -> {
            if (disposed) return;
            if (spinnerTimer != null) {
                spinnerTimer.stop();
            }
            frame.toFront();
            if (cancelBtn != null) {
                cancelBtn.setEnabled(false);
            }
            if (isFehler) {
                if (statusLabel != null && imageIconError != null) {
                    statusLabel.setIcon(imageIconError);
                    statusLabel.setToolTipText("Fehler");
                }
            } else {
                if (statusLabel != null && imageIconReady != null) {
                    statusLabel.setIcon(imageIconReady);
                    statusLabel.setToolTipText("Fertig");
                }
            }
        });
        return this;
    }

    /**
     * @return the frame
     */
    public final JFrame getFrame() {
        return frame;
    }

}
