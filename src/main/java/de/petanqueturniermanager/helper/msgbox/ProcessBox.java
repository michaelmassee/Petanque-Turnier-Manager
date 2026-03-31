package de.petanqueturniermanager.helper.msgbox;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import de.petanqueturniermanager.comp.newrelease.NewReleaseChecker;
import de.petanqueturniermanager.helper.i18n.I18n;

public class ProcessBox {
	private static final Logger logger = LogManager.getLogger(ProcessBox.class);

	private static final int ANZAHLSPALTEN = 1;

	private static final int MIN_HEIGHT = 200;
	private static final int MIN_WIDTH = 600;
	private static final String TITLE = "Pétanque Turnier Manager";
	private static volatile ProcessBox processBox;
	private static boolean headlessMode = false;
	private static final SimpleDateFormat SIMPLEDATEFORMAT = new SimpleDateFormat("HH:mm:ss");

	public static void setHeadlessMode(boolean headless) {
		headlessMode = headless;
	}

	private final JFrame frame;
	private JTextArea logOut;
	private JButton logfileBtn;
	private JButton cancelBtn;
	private String prefix;

	private JLabel statusLabel;
	private JLabel infoLabel;
	private JLabel neueVersionLabel;

	private ImageIcon imageIconReady;
	private ImageIcon imageIconError;

	private ArrayList<ImageIcon> inworkIcons;

	private DialogTools dialogTools;
	private final ScheduledExecutorService drawInWorkIcon;
	private ScheduledFuture<?> drawInWorkIconScheduled;

	private boolean isFehler = false;
	private final XComponentContext xContext;

	private ProcessBox(XComponentContext xContext) {
		this.xContext = checkNotNull(xContext);
		if (headlessMode) {
			frame = null;
			drawInWorkIcon = null;
			return;
		}
		frame = new JFrame();
		dialogTools = DialogTools.from(xContext, frame);
		drawInWorkIcon = Executors.newScheduledThreadPool(1);
		inworkIcons = new ArrayList<>();
		logOut = null;
		logfileBtn = null;
		cancelBtn = null;
		prefix = null;
		statusLabel = null;
		neueVersionLabel = null;
		imageIconReady = null;
		imageIconError = null;
		initBox();
	}

	public static ProcessBox from() {
		if (ProcessBox.processBox == null) {
			logger.error("ProcessBox nicht initialisiert");
			throw new NullPointerException("ProcessBox nicht initialisiert");
		}
		return ProcessBox.processBox;
	}

	/**
	 * Box schließen, kann wieder geöfnet werden
	 */
	public static void dispose() {
		synchronized (ProcessBox.class) {
			if (ProcessBox.processBox != null) {
				ProcessBox.processBox._dispose();
			}
		}
	}

	/**
	 * nur intern verwenden
	 */
	private void _dispose() {
		if (drawInWorkIcon != null) {
			drawInWorkIcon.shutdownNow();
		}
		if (drawInWorkIconScheduled != null) {
			drawInWorkIconScheduled.cancel(true);
		}
		if (frame != null) {
			frame.dispose();
		}
		dialogTools = null;
		ProcessBox.processBox = null;
	}

	/**
	 * Neue Box anlegen; vorhandene Instanz wird vorher disposed.
	 */
	public static ProcessBox forceinit(XComponentContext xContext) {
		checkNotNull(xContext);
		synchronized (ProcessBox.class) {
			if (ProcessBox.processBox != null) {
				ProcessBox.processBox._dispose();
			}
			ProcessBox.processBox = new ProcessBox(xContext);
			return ProcessBox.processBox;
		}
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

	private void initBox() {
		logger.debug("ProcessBox.initBox");
		frame.setLayout(new GridBagLayout());
		setIcons();

		// log
		initLog(0);
		// Neue-Version-Hinweiszeile (nur sichtbar wenn neue Version verfügbar)
		initNeueVersionZeile(1);
		// Footer: Status, Info, Log, Stop
		initSpieltagUndSpielrundInfo(2);

		frame.setPreferredSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		frame.setSize(MIN_WIDTH, MIN_HEIGHT);
		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		title(TITLE);

		// Callback registrieren: Neue-Version-Label einblenden sobald Cache-Thread fertig
		NewReleaseChecker.addCacheUpdateCallback(this::aktualisiereNeueVersionLabel);
	}

	private void initNeueVersionZeile(int zeile) {
		neueVersionLabel = new JLabel();
		neueVersionLabel.setForeground(new Color(0xcc4400));
		neueVersionLabel.setFont(neueVersionLabel.getFont().deriveFont(Font.BOLD, 13f));
		neueVersionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		neueVersionLabel.setVisible(false);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = zeile;
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
		NewReleaseChecker checker = new NewReleaseChecker();
		boolean neueVersion = checker.checkForNewRelease(xContext);
		SwingUtilities.invokeLater(() -> {
			if (neueVersion) {
				neueVersionLabel.setText(I18n.get("processbox.neue.version.verfuegbar"));
			}
			neueVersionLabel.setVisible(neueVersion);
			frame.revalidate();
		});
	}

	private void setIcons() {
		// icons laden
		try {
			BufferedImage img256 = ImageIO
					.read(this.getClass().getResourceAsStream("petanqueturniermanager-logo-256px.png"));
			Image[] images = { img256 };
			frame.setIconImages(java.util.Arrays.asList(images));

			// https://loading.io/
			// https://ezgif.com/
			for (int i = 0; i < 31; i++) {
				inworkIcons.add(new ImageIcon(
						ImageIO.read(this.getClass().getResourceAsStream("spinner/frame-" + i + ".png"))));
			}
			imageIconReady = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("check25x32.png")));
			imageIconError = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("cross32x32.png")));
		} catch (IOException e) {
			logger.debug(e);
		}
	}

	private void initSpieltagUndSpielrundInfo(int startZeile) {

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
				Color color1 = new Color(Integer.valueOf("eaf4ff", 16).intValue());
				Color color2 = new Color(Integer.valueOf("d6e9ff", 16).intValue());
				GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
				g2d.setPaint(gp);
				g2d.fillRect(0, 0, w, h);
			}
		};

		Border raisedbevel = BorderFactory.createRaisedBevelBorder();
		panel.setBorder(raisedbevel);
		{
			GridBagConstraints gridBagConstraintsFrame = new GridBagConstraints();
			gridBagConstraintsFrame.gridy = startZeile; // zeile
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
			logfileBtn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Log4J.openLogFile();
				}
			});

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
			cancelBtn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					SheetRunner.cancelRunner();
				}
			});

			gridBagConstraintsPanel.insets = new Insets(0, 5, 0, 5);
			gridBagConstraintsPanel.anchor = GridBagConstraints.EAST;
			gridBagConstraintsPanel.weightx = 0.5;
			panel.add(cancelBtn, gridBagConstraintsPanel);
		}
		// ----------------------------------------------------------
	}

	private void initLog(int startZeile) {
		logOut = new JTextArea();
		logOut.setEditable(false);
		logOut.setLineWrap(false);
		// move to the last pos
		((DefaultCaret) logOut.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		JScrollPane scrollPane = new JScrollPane(logOut);

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0; // spalte
		gridBagConstraints.gridy = startZeile; // zeile

		gridBagConstraints.gridwidth = ANZAHLSPALTEN;

		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.weighty = 0.5;
		gridBagConstraints.insets = new Insets(5, 5, 5, 5);

		gridBagConstraints.fill = GridBagConstraints.BOTH;
		frame.add(scrollPane, gridBagConstraints);
	}

	public ProcessBox clearWennNotRunning() {
		if (!SheetRunner.isRunning()) {
			clear();
		}
		return this;
	}

	public ProcessBox infoText(String newInfoText) {
		if (infoLabel != null) {
			infoLabel.setText(newInfoText);
		}
		return this;
	}

	public ProcessBox prefix(String nextLogPrefix) {
		prefix = nextLogPrefix;
		return this;
	}

	public ProcessBox clear() {
		if (logOut == null) return this;
		isFehler = false;
		logOut.setText(null);
		return this;
	}

	public synchronized ProcessBox fehler(String logMsg) {
		info(I18n.get("processbox.fehler.prefix") + " " + logMsg);
		isFehler = true;
		return this;
	}

	public synchronized ProcessBox info(String logMsg) {
		logger.debug("ProcessBox info ->" + logMsg);
		if (headlessMode || logOut == null) {
			return this;
		}
		checkNotNull(logMsg);

		logOut.append(SIMPLEDATEFORMAT.format(new Date()));
		logOut.append(" | ");

		if (prefix != null) {
			logOut.append(prefix);
			logOut.append(": ");
			prefix = null;
		}
		logOut.append(logMsg + "\r\n");
		return this;
	}

	public ProcessBox title(String title) {
		if (frame == null) return this;
		frame.setTitle(title);
		return this;
	}

	public ProcessBox moveInsideTopWindow() {
		if (dialogTools == null) return this;
		dialogTools.moveInsideTopWindow();
		return this;
	}

	/** Zeigt die ProcessBox im Vordergrund – sicher aufrufbar auch wenn noch nicht initialisiert. */
	public static void zeigeImVordergrund() {
		if (processBox != null) {
			SwingUtilities.invokeLater(() -> processBox.visible().toFront());
		}
	}

	public ProcessBox hide() {
		if (frame == null) return this;
		frame.setVisible(false);
		return this;
	}

	public ProcessBox visible() {
		if (frame == null) return this;
		frame.setVisible(true);
		moveInsideTopWindow();
		return this;
	}

	public ProcessBox toFront() {
		if (frame == null) return this;
		frame.toFront();
		return this;
	}

	public ProcessBox run() {
		logger.debug("ProcessBox run");
		if (headlessMode) return this;
		if (drawInWorkIconScheduled != null && !drawInWorkIconScheduled.isDone()) {
			return this;
		}
		toFront();
		if (cancelBtn != null) {
			cancelBtn.setEnabled(true);
		}
		statusLabel.setToolTipText("In Arbeit");
		drawInWorkIconScheduled = drawInWorkIcon.scheduleAtFixedRate(new UpdateInWorkIcon(inworkIcons, statusLabel), 0,
				100, TimeUnit.MILLISECONDS);
		return this;
	}

	public ProcessBox ready() {
		if (headlessMode) return this;
		if (drawInWorkIconScheduled != null) {
			drawInWorkIconScheduled.cancel(true);
		}
		toFront();
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
		return this;
	}

	/**
	 * @return the frame
	 */
	public final JFrame getFrame() {
		return frame;
	}

}
// Animated gifs are not working !

class UpdateInWorkIcon implements Runnable {
	private int inWorkImgIdx = 0;
	private final ArrayList<ImageIcon> inworkIcons;
	private final JLabel statusLabel;

	public UpdateInWorkIcon(ArrayList<ImageIcon> inworkIcons, JLabel statusLabel) {
		this.inworkIcons = inworkIcons;
		this.statusLabel = statusLabel;
	}

	@Override
	public void run() {
		if (inWorkImgIdx >= inworkIcons.size()) {
			inWorkImgIdx = 0;
		}
		statusLabel.setIcon(inworkIcons.get(inWorkImgIdx));
		inWorkImgIdx++;
	}
}
