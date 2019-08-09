/**
 * Erstellung 30.07.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfiguration.dialog;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.msgbox.DialogTools;

/**
 * @author Michael Massee<br>
 * Singleton<br>
 * jframe mit alle konfiguration
 *
 */
public class MainKonfigDialog {

	private static final Logger logger = LogManager.getLogger(MainKonfigDialog.class);

	private static MainKonfigDialog konfigDialog;
	private static final int MIN_HEIGHT = 200;
	private static final int MIN_WIDTH = 500;
	private static final String TITLE = "Konfiguration";

	// weil problemen mit uno classloader + reflection, wird hier eine static liste verwendet
	private static List<ConfigPanel> configPanelList = new ArrayList<>();
	static {
		configPanelList.add(new SpielrundenKonfigPanel());
	}

	private JFrame frame;
	private JSplitPane splitPane;
	private JTree tree;
	private JPanel content;
	private final DialogTools dialogTools;

	MainKonfigDialog(DialogTools dialogTools) {
		this.dialogTools = dialogTools;
	}

	private MainKonfigDialog(XComponentContext xContext) {
		dialogTools = DialogTools.from(checkNotNull(xContext), frame);
		initBox();
	}

	public static MainKonfigDialog from() {
		if (MainKonfigDialog.konfigDialog == null) {
			throw new NullPointerException("MainKonfigDialog nicht initialisiert");
		}
		return MainKonfigDialog.konfigDialog;
	}

	/**
	 * @param context
	 */
	public static MainKonfigDialog init(XComponentContext xContext) {
		checkNotNull(xContext);
		if (MainKonfigDialog.konfigDialog == null) {
			MainKonfigDialog.konfigDialog = new MainKonfigDialog(xContext);
		}
		return MainKonfigDialog.konfigDialog;
	}

	@VisibleForTesting
	void initBox() {
		frame = new JFrame();
		frame.setPreferredSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
		frame.setSize(MIN_WIDTH, MIN_HEIGHT);
		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.setAlwaysOnTop(true);
		title(TITLE);
		initTree();
		initContent();
		initSplitPane();
	}

	private void initSplitPane() {
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setLeftComponent(tree);
		splitPane.setRightComponent(content);
		splitPane.setDividerLocation(100);
		splitPane.setPreferredSize(new Dimension(500, 300));

		frame.add(splitPane, BorderLayout.CENTER);
	}

	private void initContent() {
		content = new JPanel();
		Dimension minimumSize = new Dimension(100, 50);
		content.setMinimumSize(minimumSize);
	}

	private void initTree() {
		logger.info("Init Tree " + configPanelList.size() + " panels");
		Border raisedbevel = BorderFactory.createRaisedBevelBorder();
		Dimension minimumSize = new Dimension(100, 50);

		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Konfiguration");
		tree = new JTree(top);
		tree.setRootVisible(false);
		tree.setBorder(raisedbevel);
		tree.setMinimumSize(minimumSize);

		for (ConfigPanel pnl : configPanelList) {
			top.add(new DefaultMutableTreeNode(pnl.getLabel()));
		}
	}

	public MainKonfigDialog open() {
		visible();
		toFront();
		return this;
	}

	public MainKonfigDialog title(String title) {
		frame.setTitle(title);
		return this;
	}

	public MainKonfigDialog moveInsideTopWindow() {
		dialogTools.moveInsideTopWindow();
		return this;
	}

	public MainKonfigDialog hide() {
		frame.setVisible(false);
		return this;
	}

	public MainKonfigDialog visible() {
		frame.setVisible(true);
		moveInsideTopWindow();
		return this;
	}

	public MainKonfigDialog toFront() {
		frame.toFront();
		return this;
	}

	// TODO
	// classLoader.loadClass(info.getName()); // not working ???
	// private List<ConfigPanel> listConfigPanels() {
	//
	// List<ConfigPanel> ret = new ArrayList<>();
	// ClassPath classPath;
	// try {
	// ClassLoader classLoader = getClass().getClassLoader();
	// ClassLoader parent = classLoader.getParent();
	// ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
	// classPath = ClassPath.from(classLoader);
	// ImmutableSet<ClassPath.ClassInfo> classes = classPath.getTopLevelClassesRecursive("de.petanqueturniermanager");
	// for (ClassPath.ClassInfo info : classes) {
	// Class<?> loadClass2 = parent.loadClass(info.getName());
	// Class<?> loadClass = classLoader.loadClass(info.getName());
	// Class<?> clazz = Class.forName(info.getName());
	// if (clazz.isAssignableFrom(ConfigPanel.class)) {
	// ret.add((ConfigPanel) clazz.newInstance());
	// }
	// }
	// } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
	// e.printStackTrace();
	// }
	// return ret;
	// }

}
