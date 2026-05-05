package de.petanqueturniermanager.konfigdialog.dialog.mainkonfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.GraphicsEnvironment;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.petanqueturniermanager.helper.msgbox.DialogTools;

/**
 * Verifiziert, dass {@link MainKonfigDialog} korrekt aufgebaut wird:
 * Frame, Tree mit Konfigurations-Knoten, SplitPane, Content-Panel und
 * mindestens ein registriertes {@link ConfigPanel}.
 *
 * <p>Der Test wird übersprungen, wenn die JVM headless läuft (z.B. CI ohne
 * Display) – Swing-Komponenten benötigen einen GraphicsEnvironment.
 */
public class MainKonfigDialogTest {

	private DialogTools dialogToolsMock;

	@BeforeEach
	public void setup() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"Headless-JVM – MainKonfigDialog benötigt ein Display");
		dialogToolsMock = Mockito.mock(DialogTools.class);
	}

	@Test
	public void testDialogStrukturAufbau() {
		MainKonfigDialog dlg = new MainKonfigDialog(dialogToolsMock);
		dlg.initBox();

		assertThat(dlg.getFrame()).as("Frame muss erzeugt sein").isNotNull();
		assertThat(dlg.getFrame().getTitle()).as("Titel").isEqualTo("Konfiguration");
		assertThat(dlg.getTree()).as("Tree muss aufgebaut sein").isNotNull();
		assertThat(dlg.getSplitPane()).as("SplitPane muss aufgebaut sein").isNotNull();
		assertThat(dlg.getContent()).as("Content-Panel muss aufgebaut sein").isNotNull();

		assertThat(dlg.getConfigPanelList())
				.as("Mindestens das Spielrunden-Panel muss registriert sein")
				.isNotEmpty();
	}

	@Test
	public void testTreeWurzelKonotenKorrekt() {
		MainKonfigDialog dlg = new MainKonfigDialog(dialogToolsMock);
		dlg.initBox();

		JTree tree = dlg.getTree();
		assertThat(tree.getModel().getRoot()).isInstanceOf(DefaultMutableTreeNode.class);

		DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
		assertThat(root.getUserObject()).as("Root-Knoten").isEqualTo("Konfiguration");
		assertThat(tree.isRootVisible()).as("Root-Knoten muss sichtbar sein").isTrue();
	}

	@Test
	public void testKonfigPanelsWerdenRegistriert() {
		MainKonfigDialog dlg = new MainKonfigDialog(dialogToolsMock);
		dlg.initBox();

		assertThat(dlg.getConfigPanelList())
				.as("Spielrunden-Konfigurations-Panel muss registriert sein")
				.hasAtLeastOneElementOfType(SpielrundenKonfigPanel.class);
	}

	@Test
	public void testTitleAendernUebernimmtNeuenTitel() {
		MainKonfigDialog dlg = new MainKonfigDialog(dialogToolsMock);
		dlg.initBox();

		dlg.title("Anderer Titel");
		assertThat(dlg.getFrame().getTitle()).isEqualTo("Anderer Titel");
	}
}
