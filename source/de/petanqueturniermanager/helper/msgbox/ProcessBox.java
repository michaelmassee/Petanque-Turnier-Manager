package de.petanqueturniermanager.helper.msgbox;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;

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

	private static final int ANZAHLSPALTEN = 1;

	private static final int X_OFFSET = 50;
	private static final int Y_OFFSET = 50;

	private static final int MIN_HEIGHT = 150;
	private static final int MIN_WIDTH = 300;
	private static final String TITLE = "Pétanque Turnier Manager";
	private static ProcessBox processBox = null;
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

	private final JFrame frame;
	private final XComponentContext xContext;
	private JTextArea logOut = null;
	private String prefix = null;
	private JTextField spieltagText = null;
	private JTextField spielrundeText = null;

	private ProcessBox(XComponentContext xContext) {
		checkNotNull(xContext);
		this.xContext = xContext;
		this.frame = new JFrame();
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

		// 2 Info felder Spieltag, Spielrunde
		initSpieltagUndSpielrundInfo(0);

		// log
		initLog(1);

		frame.setPreferredSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		frame.setSize(MIN_WIDTH, MIN_HEIGHT);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		moveInsideTopWindow();
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
		// Header Panel
		JPanel panel = new JPanel(new FlowLayout());
		Border raisedbevel = BorderFactory.createRaisedBevelBorder();
		panel.setBorder(raisedbevel);
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridy = startZeile; // zeile
		gridBagConstraints.gridx = 0; // spalte
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new Insets(5, 5, 0, 5);
		frame.add(panel, gridBagConstraints);
		// -----------------------------

		spieltagText = newNumberJTextField();
		spielrundeText = newNumberJTextField();

		JLabel spieltagLabel = new JLabel("Spieltag:");
		spieltagLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel spielrundeLabel = new JLabel("Spielrunde:");
		spielrundeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		panel.add(spieltagLabel);
		panel.add(spieltagText);
		panel.add(spielrundeLabel);
		panel.add(spielrundeText);
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

	public ProcessBox fehler(String logMsg) {
		info("Fehler: " + logMsg);
		return this;
	}

	public ProcessBox info(String logMsg) {
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
}
