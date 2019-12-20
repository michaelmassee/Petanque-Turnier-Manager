/**
 * Erstellung 01.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.dialog.mainkonfig;

import java.lang.ref.WeakReference;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Michael Massee
 *
 */
public class MainKonfigTreeSelectionListner implements TreeSelectionListener {

	private static final Logger logger = LogManager.getLogger(MainKonfigTreeSelectionListner.class);

	private final WeakReference<JTree> tree;

	public MainKonfigTreeSelectionListner(JTree tree) {
		this.tree = new WeakReference<>(tree);
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {

		JTree jtree = tree.get();

		if (jtree != null) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) jtree.getLastSelectedPathComponent();
			/// * if nothing is selected */
			if (node == null) {
				return;
			}
			Object nodeInfo = node.getUserObject();

			if (nodeInfo instanceof ConfigPanel) {
				logger.info("selection xx");
			}

		}
	}

}
