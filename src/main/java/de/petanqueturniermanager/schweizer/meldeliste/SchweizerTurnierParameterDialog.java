/**
 * Erstellung : 27.02.2026 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.Optional;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
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

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter vor dem Anlegen einer neuen
 * Schweizer Meldeliste.
 * <p>
 * Abgefragt werden:
 * <ul>
 * <li>Formation: Tête / Doublette / Triplette (Radio-Buttons)</li>
 * <li>Teamname anzeigen: Ja / Nein (Checkbox)</li>
 * </ul>
 */
public class SchweizerTurnierParameterDialog {

	/** Ergebnis des Dialogs. */
	public static class TurnierParameter {
		public final Formation formation;
		public final boolean teamnameAnzeigen;
		public final boolean vereinsnameAnzeigen;

		public TurnierParameter(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen) {
			this.formation = formation;
			this.teamnameAnzeigen = teamnameAnzeigen;
			this.vereinsnameAnzeigen = vereinsnameAnzeigen;
		}
	}

	private final WorkingSpreadsheet workingSpreadsheet;
	private volatile boolean okPressed = false;

	public SchweizerTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	/**
	 * Zeigt den Dialog an und gibt das Ergebnis zurück.
	 *
	 * @param defaultFormation          vorausgewählte Formation
	 * @param defaultTeamnameAnzeigen   vorausgewählter Teamname-Status
	 * @param defaultVereinsnameAnzeigen vorausgewählter Vereinsname-Status
	 * @return Optional mit TurnierParameter bei OK, leer bei Abbrechen
	 */
	public Optional<TurnierParameter> show(Formation defaultFormation, boolean defaultTeamnameAnzeigen,
			boolean defaultVereinsnameAnzeigen)
			throws com.sun.star.uno.Exception {

		ProcessBox.from().hide();

		XComponentContext context = workingSpreadsheet.getxContext();
		XMultiComponentFactory xMCF = context.getServiceManager();

		// 1. Dialog-Modell anlegen und Eigenschaften setzen (VOR setModel)
		Object dialogModel = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", context);
		XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
		dlgProps.setPropertyValue("PositionX", Integer.valueOf(50));
		dlgProps.setPropertyValue("PositionY", Integer.valueOf(50));
		dlgProps.setPropertyValue("Width", Integer.valueOf(160));
		dlgProps.setPropertyValue("Height", Integer.valueOf(135));
		dlgProps.setPropertyValue("Title", "Schweizer Turnier \u2013 Parameter");
		dlgProps.setPropertyValue("Moveable", Boolean.TRUE);

		// 2. Dialog-Control anlegen und Modell setzen
		Object dialog = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", context);
		XControl xControl = Lo.qi(XControl.class, dialog);
		xControl.setModel(Lo.qi(XControlModel.class, dialogModel));

		// 3. Child-Controls via XMultiServiceFactory des Dialog-Modells anlegen
		XMultiServiceFactory xMSF = Lo.qi(XMultiServiceFactory.class, dialogModel);
		XNameContainer cont = Lo.qi(XNameContainer.class, dialogModel);
		XControlContainer xcc = Lo.qi(XControlContainer.class, dialog);

		addLabel(xMSF, cont, "lblFormation", "Formation:", 8, 8, 80, 10);

		addRadioButton(xMSF, cont, "radioTete",
				Formation.TETE.getBezeichnung(), 8, 21, 140, 10,
				defaultFormation == Formation.TETE);
		addRadioButton(xMSF, cont, "radioDoublette",
				Formation.DOUBLETTE.getBezeichnung(), 8, 33, 140, 10,
				defaultFormation == Formation.DOUBLETTE);
		addRadioButton(xMSF, cont, "radioTriplette",
				Formation.TRIPLETTE.getBezeichnung(), 8, 45, 140, 10,
				defaultFormation == Formation.TRIPLETTE);

		addFixedLine(xMSF, cont, "sep1", 5, 59, 150, 2);

		addCheckBox(xMSF, cont, "cbTeamname", "Teamname anzeigen",
				8, 65, 140, 10, defaultTeamnameAnzeigen);

		addCheckBox(xMSF, cont, "cbVereinsname", "Vereinsname anzeigen",
				8, 79, 140, 10, defaultVereinsnameAnzeigen);

		addFixedLine(xMSF, cont, "sep2", 5, 95, 150, 2);

		addButton(xMSF, cont, "btnOk", "OK", 22, 105, 50, 14);
		addButton(xMSF, cont, "btnCancel", "Abbrechen", 88, 105, 60, 14);

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

		// 5. Peer erzeugen (instantiiert alle Child-Controls aus dem Modell)
		Object toolkit = xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", context);
		XToolkit xToolkit = Lo.qi(XToolkit.class, toolkit);
		XWindow xWindow = Lo.qi(XWindow.class, xControl);
		xWindow.setVisible(false);
		xControl.createPeer(xToolkit, null);

		// 6. Dialog anzeigen (blockiert bis endExecute oder Fenster-Schliessen)
		xDialog.execute();

		Optional<TurnierParameter> result = Optional.empty();
		if (okPressed) {
			Formation formation = readFormation(xcc);
			boolean teamnameAnzeigen = readCheckBoxState(xcc, "cbTeamname");
			boolean vereinsnameAnzeigen = readCheckBoxState(xcc, "cbVereinsname");
			result = Optional.of(new TurnierParameter(formation, teamnameAnzeigen, vereinsnameAnzeigen));
		}

		Lo.qi(XComponent.class, dialog).dispose();
		ProcessBox.from().visible();

		return result;
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden – Zustand auslesen
	// ---------------------------------------------------------------

	private Formation readFormation(XControlContainer xcc) {
		if (isRadioSelected(xcc, "radioTete")) {
			return Formation.TETE;
		}
		if (isRadioSelected(xcc, "radioDoublette")) {
			return Formation.DOUBLETTE;
		}
		return Formation.TRIPLETTE;
	}

	private boolean isRadioSelected(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return false;
		}
		XRadioButton radio = Lo.qi(XRadioButton.class, ctrl);
		return radio != null && radio.getState();
	}

	private boolean readCheckBoxState(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return false;
		}
		XCheckBox cb = Lo.qi(XCheckBox.class, ctrl);
		return cb != null && cb.getState() == 1;
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

	private void addCheckBox(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String label, int x, int y, int w, int h, boolean checked)
			throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlCheckBoxModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", label);
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
		props.setPropertyValue("State", (short) (checked ? 1 : 0));
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

	public static SchweizerTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerTurnierParameterDialog(workingSpreadsheet);
	}
}
