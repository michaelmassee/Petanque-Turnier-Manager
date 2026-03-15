/**
 * Erstellung : 15.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.meldeliste;

import java.util.Optional;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XRadioButton;
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
 * Modaler Start-Dialog für neue SuperMêlée-Turnier-Meldelisten.
 * <p>
 * Abgefragt wird:
 * <ul>
 * <li>Supermêlée Modus: Triplette / Doublette (Radio-Buttons)</li>
 * </ul>
 */
public class SupermeleeStartDialog {

	/** Ergebnis des Dialogs. */
	public record StartParameter(boolean triplette) {
	}

	private final WorkingSpreadsheet workingSpreadsheet;
	private volatile boolean okPressed = false;

	public SupermeleeStartDialog(WorkingSpreadsheet workingSpreadsheet) {
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
		dlgProps.setPropertyValue("Width", Integer.valueOf(160));
		dlgProps.setPropertyValue("Height", Integer.valueOf(80));
		dlgProps.setPropertyValue("Title", "Neues Superm\u00eal\u00e9e-Turnier");
		dlgProps.setPropertyValue("Moveable", Boolean.TRUE);

		// 2. Dialog-Control anlegen
		Object dialog = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", context);
		XControl xControl = Lo.qi(XControl.class, dialog);
		xControl.setModel(Lo.qi(XControlModel.class, dialogModel));

		// 3. Child-Controls anlegen
		XMultiServiceFactory xMSF = Lo.qi(XMultiServiceFactory.class, dialogModel);
		XNameContainer cont = Lo.qi(XNameContainer.class, dialogModel);
		XControlContainer xcc = Lo.qi(XControlContainer.class, dialog);

		addLabel(xMSF, cont, "lblModus", "Superm\u00eal\u00e9e Modus:", 8, 8, 140, 10);
		addRadioButton(xMSF, cont, "radioTriplette", "Triplette", 8, 21, 140, 10, true);
		addRadioButton(xMSF, cont, "radioDoublette", "Doublette", 8, 33, 140, 10, false);

		addFixedLine(xMSF, cont, "sep1", 5, 49, 150, 2);

		addButton(xMSF, cont, "btnOk", "OK", 22, 57, 50, 14);
		addButton(xMSF, cont, "btnCancel", "Abbrechen", 88, 57, 60, 14);

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
			boolean triplette = isRadioSelected(xcc, "radioTriplette");
			result = Optional.of(new StartParameter(triplette));
		}

		Lo.qi(XComponent.class, dialog).dispose();
		ProcessBox.from().visible();

		return result;
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden – Zustand auslesen
	// ---------------------------------------------------------------

	private boolean isRadioSelected(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return false;
		}
		XRadioButton radio = Lo.qi(XRadioButton.class, ctrl);
		return radio != null && radio.getState();
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

	private void addRadioButton(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String label, int x, int y, int w, int h, boolean selected)
			throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlRadioButtonModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", label);
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
		props.setPropertyValue("State", (short) (selected ? 1 : 0));
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

	public static SupermeleeStartDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new SupermeleeStartDialog(workingSpreadsheet);
	}
}
