package de.petanqueturniermanager.helper.msgbox;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
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

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.DocumentHelper;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class ProcessBox {
	private static final Logger logger = LogManager.getLogger(ProcessBox.class);

	private static final int ANZAHLSPALTEN = 1;

	private static final int X_OFFSET = 50;
	private static final int Y_OFFSET = 50;

	private static final int MIN_HEIGHT = 200;
	private static final int MIN_WIDTH = 500;
	private static final String TITLE = "Pétanque Turnier Manager";
	private static ProcessBox processBox = null;
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

	private final JFrame frame;
	private final XComponentContext xContext;
	private JTextArea logOut = null;
	private JButton cancelBtn = null;
	private String prefix = null;
	private JTextField spieltagText = null;
	private JTextField spielrundeText = null;

	private JLabel statusLabel = null;

	private ImageIcon imageIconReady = null;
	private ImageIcon imageIconError = null;

	private ArrayList<ImageIcon> inworkIcons = new ArrayList<ImageIcon>();

	private final ScheduledExecutorService drawInWorkIcon = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> drawInWorkIconScheduled;

	private boolean isFehler = false;

	private ProcessBox(XComponentContext xContext) {
		this.xContext = checkNotNull(xContext);
		frame = new JFrame();
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
			ProcessBox.processBox.title(TITLE);
		}
		ProcessBox.processBox.toFront();
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
		moveInsideTopWindow();
	}

	private void setIcons() {

		// icons laden
		// https://www.flaticon.com/free-icon/podium_889537#term=winner%20podium&page=1&position=11

		try {
			BufferedImage img16 = ImageIO.read(this.getClass().getResourceAsStream("podium16x16.png"));
			BufferedImage img24 = ImageIO.read(this.getClass().getResourceAsStream("podium24x24.png"));
			BufferedImage img32 = ImageIO.read(this.getClass().getResourceAsStream("podium32x32.png"));
			BufferedImage img64 = ImageIO.read(this.getClass().getResourceAsStream("podium64x64.png"));
			BufferedImage img128 = ImageIO.read(this.getClass().getResourceAsStream("podium128x128.png"));
			BufferedImage img256 = ImageIO.read(this.getClass().getResourceAsStream("podium256x256.png"));
			BufferedImage img512 = ImageIO.read(this.getClass().getResourceAsStream("podium512x512.png"));
			Image[] images = { img16, img24, img32, img64, img128, img256, img512 };
			frame.setIconImages(java.util.Arrays.asList(images));

			inworkIcons.add(new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("update25x25_1.png"))));
			inworkIcons.add(new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("update25x25_2.png"))));
			inworkIcons.add(new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("update25x25_3.png"))));
			inworkIcons.add(new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("update25x25_4.png"))));
			this.imageIconReady = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("check25x32.png")));
			this.imageIconError = new ImageIcon(ImageIO.read(this.getClass().getResourceAsStream("cross32x32.png")));

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
				Color color1 = new Color(Integer.valueOf("eaf4ff", 16));
				Color color2 = new Color(Integer.valueOf("d6e9ff", 16));
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

		JLabel spieltagLabel = new JLabel("Spieltag:");
		spieltagLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel spielrundeLabel = new JLabel("Spielrunde:");
		spielrundeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		panel.add(spieltagLabel, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte
		panel.add(spieltagText, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte
		panel.add(spielrundeLabel, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte
		panel.add(spielrundeText, gridBagConstraintsPanel);
		gridBagConstraintsPanel.gridx++; // spalte

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

	public ProcessBox spielTag(SpielTagNr spieltag) throws GenerateException {
		spieltagText.setText("" + spieltag.getNr());
		return this;
	}

	public ProcessBox spielRunde(SpielRundeNr spielrunde) throws GenerateException {
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
		visible();
		return this;
	}

	public ProcessBox moveInsideTopWindow() {
		XFrame currentFrame = DocumentHelper.getCurrentFrame(xContext);
		XWindow containerWindow = currentFrame.getContainerWindow();
		Rectangle posSize = containerWindow.getPosSize();

		int state = frame.getExtendedState();
		if (Frame.NORMAL != state) {
			frame.setExtendedState(Frame.NORMAL);
		}

		int newXPos = frame.getX();
		int newYPos = frame.getY();
		if (newXPos < posSize.X || newXPos > (posSize.X + posSize.Width)) {
			newXPos = posSize.X + X_OFFSET;
		}

		if (newYPos < posSize.Y || newYPos > (posSize.Y + posSize.Height)) {
			newYPos = posSize.Y + Y_OFFSET;
		}

		if (frame.getX() != newXPos || frame.getY() != newYPos) {
			frame.setLocation(newXPos, newYPos);
		}
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
		this.drawInWorkIconScheduled = drawInWorkIcon.scheduleAtFixedRate(new UpdateInWorkIcon(inworkIcons, statusLabel), 0, 600, TimeUnit.MILLISECONDS);
		return this;
	}

	public ProcessBox ready() {
		this.drawInWorkIconScheduled.cancel(true);
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
