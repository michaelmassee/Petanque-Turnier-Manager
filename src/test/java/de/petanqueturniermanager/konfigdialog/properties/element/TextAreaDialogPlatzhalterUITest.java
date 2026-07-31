package de.petanqueturniermanager.konfigdialog.properties.element;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.awt.Selection;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XMultiComponentFactory;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.konfigdialog.dialog.element.UITextArea;

/**
 * Isolierter Nachbau des Platzhalter-Button-Mechanismus aus {@link TextAreaDialog}
 * (ohne den blockierenden {@code xDialog.execute()}-Aufruf), um zu verifizieren, dass
 * {@code XTextComponent.insertText(Selection, String)} auf dem per {@link UITextArea}
 * erzeugten Edit-Control tatsächlich Text einfügt.
 */
public class TextAreaDialogPlatzhalterUITest extends BaseCalcUITest {

	@Test
	void insertTextFuegtTokenAnCursorpositionEin() throws Exception {
		XMultiComponentFactory xMultiComponentFactory = wkingSpreadsheet.getxContext().getServiceManager();

		Object dialogModel = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel",
				wkingSpreadsheet.getxContext());
		XPropertySet xPSetDialog = Lo.qi(XPropertySet.class, dialogModel);
		xPSetDialog.setPropertyValue("PositionX", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("PositionY", Integer.valueOf(50));
		xPSetDialog.setPropertyValue("Width", Integer.valueOf(300));
		xPSetDialog.setPropertyValue("Height", Integer.valueOf(150));

		Object dialog = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.UnoControlDialog",
				wkingSpreadsheet.getxContext());
		XControl xControl = Lo.qi(XControl.class, dialog);
		XControlModel xControlModel = Lo.qi(XControlModel.class, dialogModel);
		xControl.setModel(xControlModel);
		XControlContainer xControlCont = Lo.qi(XControlContainer.class, dialog);

		XTextComponent textComponent = UITextArea.from(dialogModel)
				.name("testTextArea")
				.posX(10).posY(10).width(200).height(60)
				.multiLine(true)
				.text("Vorher: ")
				.doInsert(xControlCont);

		assertThat(textComponent).as("XTextComponent muss erzeugt worden sein").isNotNull();

		// Peer erzeugen (wie in TextAreaDialog.createDialog()) - insertText soll auch ohne Peer
		// funktionieren, aber wir bilden den realen Ablauf möglichst genau nach.
		Object toolkit = xMultiComponentFactory.createInstanceWithContext("com.sun.star.awt.Toolkit",
				wkingSpreadsheet.getxContext());
		XToolkit xToolkit = Lo.qi(XToolkit.class, toolkit);
		XWindow xWindow = Lo.qi(XWindow.class, xControl);
		xWindow.setVisible(false);
		xControl.createPeer(xToolkit, null);

		Selection selection = textComponent.getSelection();
		textComponent.insertText(selection, "{DATUM}");

		assertThat(textComponent.getText())
				.as("insertText muss das Token an der Cursorposition einfügen")
				.contains("{DATUM}");

		Lo.qi(com.sun.star.lang.XComponent.class, dialog).dispose();
	}
}
