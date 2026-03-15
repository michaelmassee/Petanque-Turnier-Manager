/**
 * Erstellung : 15.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.liga.meldeliste;

import java.util.Optional;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * Modaler Start-Dialog für neue Liga-Meldelisten.
 * <p>
 * Abgefragt wird:
 * <ul>
 * <li>Gruppenname (Textfeld)</li>
 * </ul>
 */
public class LigaStartDialog {

	/** Ergebnis des Dialogs. */
	public record StartParameter(String gruppenname) {
	}

	private final WorkingSpreadsheet workingSpreadsheet;
	private volatile boolean okPressed = false;

	public LigaStartDialog(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	/**
	 * Zeigt den Dialog an und gibt das Ergebnis zurück.
	 *
	 * @return Optional mit StartParameter bei OK, leer bei Abbrechen
	 */
	public Optional<StartParameter> show() throws com.sun.star.uno.Exception {

		ProcessBox.from().hide();

		XComponentContext context = workingSpreadsheet.getxContext();
		XMultiComponentFactory xMCF = context.getServiceManager();

		// 1. Dialog-Modell anlegen
		Object dialogModel = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", context);
		XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
		dlgProps.setPropertyValue("PositionX", Integer.valueOf(50));
		dlgProps.setPropertyValue("PositionY", Integer.valueOf(50));
		dlgProps.setPropertyValue("Width", Integer.valueOf(200));
		dlgProps.setPropertyValue("Height", Integer.valueOf(75));
		dlgProps.setPropertyValue("Title", "Neue Liga-Meldeliste");
		dlgProps.setPropertyValue("Moveable", Boolean.TRUE);

		// 2. Dialog-Control anlegen
		Object dialog = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", context);
		XControl xControl = Lo.qi(XControl.class, dialog);
		xControl.setModel(Lo.qi(XControlModel.class, dialogModel));

		// 3. Child-Controls anlegen
		XMultiServiceFactory xMSF = Lo.qi(XMultiServiceFactory.class, dialogModel);
		XNameContainer cont = Lo.qi(XNameContainer.class, dialogModel);
		XControlContainer xcc = Lo.qi(XControlContainer.class, dialog);

		addLabel(xMSF, cont, "lblGruppenname", "Gruppenname:", 8, 8, 184, 10);
		addEditField(xMSF, cont, "editGruppenname", 8, 22, 184, 14);

		addFixedLine(xMSF, cont, "sep1", 5, 42, 190, 2);

		addButton(xMSF, cont, "btnOk", "OK", 42, 52, 50, 14);
		addButton(xMSF, cont, "btnCancel", "Abbrechen", 108, 52, 60, 14);

		// 4. Button-Listener VOR createPeer() anhängen
		XDialog xDialog = Lo.qi(XDialog.class, dialog);
		okPressed = false;
		attachButtonListener(xcc, "btnOk", new XActionListener() {
			@Override
			public void disposing(EventObject e) {
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				okPressed = true;
				xDialog.endExecute();
			}
		});
		attachButtonListener(xcc, "btnCancel", new XActionListener() {
			@Override
			public void disposing(EventObject e) {
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				xDialog.endExecute();
			}
		});

		// 5. Peer erzeugen
		Object toolkit = xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", context);
		XToolkit xToolkit = Lo.qi(XToolkit.class, toolkit);
		XWindow xWindow = Lo.qi(XWindow.class, xControl);
		xWindow.setVisible(false);
		xControl.createPeer(xToolkit, null);

		// 6. Dialog anzeigen (blockiert)
		xDialog.execute();

		Optional<StartParameter> result = Optional.empty();
		if (okPressed) {
			String gruppenname = readEditText(xcc, "editGruppenname");
			result = Optional.of(new StartParameter(gruppenname));
		}

		Lo.qi(XComponent.class, dialog).dispose();
		ProcessBox.from().visible();

		return result;
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden – Zustand auslesen
	// ---------------------------------------------------------------

	private String readEditText(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return "";
		}
		XTextComponent text = Lo.qi(XTextComponent.class, ctrl);
		return text != null ? text.getText() : "";
	}

	private void attachButtonListener(XControlContainer xcc, String name, XActionListener listener) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl != null) {
			XButton btn = Lo.qi(XButton.class, ctrl);
			if (btn != null) {
				btn.addActionListener(listener);
			}
		}
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden – Controls zum Dialog-Modell hinzufügen
	// ---------------------------------------------------------------

	private void addLabel(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String text, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", text);
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
		cont.insertByName(name, model);
	}

	private void addEditField(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlEditModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
		props.setPropertyValue("Text", "");
		cont.insertByName(name, model);
	}

	private void addButton(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String label, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlButtonModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", label);
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
		cont.insertByName(name, model);
	}

	private void addFixedLine(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, int x, int y, int w, int h) throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlFixedLineModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
		cont.insertByName(name, model);
	}

	public static LigaStartDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new LigaStartDialog(workingSpreadsheet);
	}
}
