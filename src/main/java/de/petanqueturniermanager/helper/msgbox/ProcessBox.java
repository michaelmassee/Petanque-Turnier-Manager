package de.petanqueturniermanager.helper.msgbox;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
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
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.Log4J;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public class ProcessBox {
	private static final Logger logger = LogManager.getLogger(ProcessBox.class);

	private static final int ANZAHLSPALTEN = 1;

	private static final int MIN_HEIGHT = 200;
	private static final int MIN_WIDTH = 550;
	private static final String TITLE = "Pétanque Turnier Manager";
	private static ProcessBox processBox = null;
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

	private final JFrame frame;
	private JTextArea logOut = null;
	private JButton logfileBtn = null;
	private JButton cancelBtn = null;
	private String prefix = null;
	private JTextField spieltagText = null;
	private JTextField spielrundeText = null;
	private JTextField spielSystemText = null;

	private JLabel statusLabel = null;

	private ImageIcon imageIconReady = null;
	private ImageIcon imageIconError = null;

	private ArrayList<ImageIcon> inworkIcons = new ArrayList<>();

	private final DialogTools dialogTools;
	private final ScheduledExecutorService drawInWorkIcon = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> drawInWorkIconScheduled;

	private boolean isFehler = false;

	private ProcessBox(XComponentContext xContext) {
		frame = new JFrame();
		dialogTools = DialogTools.from(checkNotNull(xContext), frame);
		initBox();
	}

	public static ProcessBox from() {
		if (ProcessBox.processBox == null) {
			throw new NullPointerException("ProcessBox nicht initialisiert");
		}
		return ProcessBox.processBox;
	}

	public static ProcessBox init(XComponentContext xContext) {
		checkNotNull(xContext);
		if (ProcessBox.processBox == null) {
			ProcessBox.processBox = new ProcessBox(xContext);
		}
		return ProcessBox.processBox;
	}

	private void initBox() {
		frame.setLayout(new GridBagLayout());
		setIcons();

		// log
		initLog(0);
		// 2 Info felder Spieltag, Spielrunde
		initSpieltagUndSpielrundInfo(1);

		frame.setPreferredSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		frame.setSize(MIN_WIDTH, MIN_HEIGHT);
		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		title(TITLE);
	}

	private void setIcons() {
		// icons laden
		try {
			// BufferedImage img16 = ImageIO.read(this.getClass().getResourceAsStream("podium16x16.png"));
			// BufferedImage img24 = ImageIO.read(this.getClass().getResourceAsStream("podium24x24.png"));
			// BufferedImage img32 = ImageIO.read(this.getClass().getResourceAsStream("podium32x32.png"));
			// BufferedImage img64 = ImageIO.read(this.getClass().getResourceAsStream("podium64x64.png"));
			// BufferedImage img128 = ImageIO.read(this.getClass().getResourceAsStream("podium128x128.png"));
			BufferedImage img256 = ImageIO.read(this.getClass().getResourceAsStream("petanqueturniermanager-logo-256px.png"));
			// BufferedImage img512 = ImageIO.read(this.getClass().getResourceAsStream("petanqueturniermanager-logo-256px.png"));
			Image[] images = { img256 };
			frame.setIconImages(java.util.Arrays.asList(images));

			// https://loading.io/
			// https://ezgif.com/
			for (int i = 0; i < 31; i++) {
				inworkIcons.add(new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("spinner/frame-" + i + ".png"))));
			}
			imageIconReady = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("check25x32.png")));
			imageIconError = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("cross32x32.png")));
		} catch (IOException e) {
			logger.debug(e);
		}
	}

	private JTextField newNumberJTextField() {
		JTextField jTextField = new JTextField();
		jTextField.setMinimumSize(new Dimension(40, 10));
		jTextField.setEditable(false);
		jTextField.setText("0");
		return jTextField;
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

		spieltagText = newNumberJTextField();
		spielrundeText = newNumberJTextField();
		spielSystemText = newNumberJTextField();

		JLabel spieltagLabel = new JLabel("Tag:");
		spieltagLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel spielrundeLabel = new JLabel("Runde:");
		spielrundeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel spielSystemLabel = new JLabel("System:");
		spielrundeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		panel.add(spieltagLabel, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte
		panel.add(spieltagText, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte

		panel.add(spielrundeLabel, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte
		panel.add(spielrundeText, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte

		panel.add(spielSystemLabel, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte
		panel.add(spielSystemText, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte

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

	public ProcessBox prefix(String nextLogPrefix) {
		prefix = nextLogPrefix;
		return this;
	}

	public ProcessBox clear() {
		isFehler = false;
		logOut.setText(null);
		return this;
	}

	public ProcessBox turnierSystem(TurnierSystem spielSystem) {
		spielSystemText.setText("" + spielSystem);
		return this;
	}

	public ProcessBox spielTag(SpielTagNr spieltag) {
		spieltagText.setText("" + spieltag.getNr());
		return this;
	}

	public ProcessBox spielRunde(SpielRundeNr spielrunde) {
		spielrundeText.setText("" + spielrunde.getNr());
		return this;
	}

	public synchronized ProcessBox fehler(String logMsg) {
		info("Fehler: " + logMsg);
		isFehler = true;
		return this;
	}

	public synchronized ProcessBox info(String logMsg) {
		checkNotNull(logOut);
		checkNotNull(logMsg);

		logOut.append(simpleDateFormat.format(new Date()));
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
		frame.setTitle(title);
		return this;
	}

	public ProcessBox moveInsideTopWindow() {
		dialogTools.moveInsideTopWindow();
		return this;
	}

	public ProcessBox hide() {
		frame.setVisible(false);
		return this;
	}

	public ProcessBox visible() {
		frame.setVisible(true);
		moveInsideTopWindow();
		return this;
	}

	public ProcessBox toFront() {
		frame.toFront();
		return this;
	}

	public ProcessBox run() {
		toFront();
		if (cancelBtn != null) {
			cancelBtn.setEnabled(true);
		}
		statusLabel.setToolTipText("In Arbeit");
		drawInWorkIconScheduled = drawInWorkIcon.scheduleAtFixedRate(new UpdateInWorkIcon(inworkIcons, statusLabel), 0, 100, TimeUnit.MILLISECONDS);
		return this;
	}

	public ProcessBox ready() {
		drawInWorkIconScheduled.cancel(true);
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

}
// Animatide gifs are not working !

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
