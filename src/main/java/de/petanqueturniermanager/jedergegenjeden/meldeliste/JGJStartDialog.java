/*
 * Erstellung : 15.03.2026 / Michael Massee
 **/

package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import java.util.Optional;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XRadioButton;
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

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * Modaler Start-Dialog für neue JGJ-Meldelisten.
 * <p>
 * Abgefragt wird:
 * <ul>
 * <li>Formation: Tête / Doublette / Triplette (Radio-Buttons)</li>
 * <li>Teamname anzeigen (Checkbox)</li>
 * <li>Vereinsname anzeigen (Checkbox)</li>
 * <li>Anzeige im Spielplan: Teamnummer / Teamname (Radio-Buttons)</li>
 * </ul>
 */
public class JGJStartDialog {

	/** Ergebnis des Dialogs. */
	public record StartParameter(Formation formation,
		boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
		SpielplanTeamAnzeige spielplanTeamAnzeige,
		int gruppengroesse,
		boolean mitRueckrunde) {
	}

	private final WorkingSpreadsheet workingSpreadsheet;
	private volatile boolean okPressed = false;

	public JGJStartDialog(WorkingSpreadsheet workingSpreadsheet) {
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
		dlgProps.setPropertyValue("Width", Integer.valueOf(170));
		dlgProps.setPropertyValue("Height", Integer.valueOf(196));
		dlgProps.setPropertyValue("Title", I18n.get("dialog.jgj.title.neue.meldeliste"));
		dlgProps.setPropertyValue("Moveable", Boolean.TRUE);

		// 2. Dialog-Control anlegen
		Object dialog = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", context);
		XControl xControl = Lo.qi(XControl.class, dialog);
		xControl.setModel(Lo.qi(XControlModel.class, dialogModel));

		// 3. Child-Controls anlegen
		XMultiServiceFactory xMSF = Lo.qi(XMultiServiceFactory.class, dialogModel);
		XNameContainer cont = Lo.qi(XNameContainer.class, dialogModel);
		XControlContainer xcc = Lo.qi(XControlContainer.class, dialog);

		addLabel(xMSF, cont, "lblFormation", I18n.get("dialog.jgj.label.formation"), 8, 8, 100, 10);
		addRadioButton(xMSF, cont, "radioTete",
				Formation.TETE.getBezeichnung(), 8, 20, 150, 10, true);
		addRadioButton(xMSF, cont, "radioDoublette",
				Formation.DOUBLETTE.getBezeichnung(), 8, 32, 150, 10, false);
		addRadioButton(xMSF, cont, "radioTriplette",
				Formation.TRIPLETTE.getBezeichnung(), 8, 44, 150, 10, false);

		addFixedLine(xMSF, cont, "sep1", 5, 58, 160, 2);

		addCheckBox(xMSF, cont, "cbTeamname",
				I18n.get("dialog.jgj.label.teamname"), 8, 64, 150, 10, false);
		addCheckBox(xMSF, cont, "cbVereinsname",
				I18n.get("dialog.jgj.label.vereinsname"), 8, 78, 150, 10, false);

		addFixedLine(xMSF, cont, "sep2", 5, 92, 160, 2);

		addLabel(xMSF, cont, "lblSpielplanAnzeige", I18n.get("dialog.jgj.label.spielplan.anzeige"), 8, 96, 80, 10);
		addListBox(xMSF, cont, "lstSpielplanAnzeige",
				new String[] { I18n.get("dialog.jgj.spielplan.teamnummer"),
						I18n.get("dialog.jgj.spielplan.teamname") },
				(short) 0, 92, 94, 70, 12);

		addFixedLine(xMSF, cont, "sep3", 5, 112, 160, 2);

		addLabel(xMSF, cont, "lblGruppengroesse", I18n.get("dialog.jgj.label.gruppengroesse"), 8, 116, 150, 10);
		addEditField(xMSF, cont, "editGruppengroesse", 8, 128, 60, 14);

		addCheckBox(xMSF, cont, "cbRueckrunde",
				I18n.get("dialog.jgj.label.rueckrunde"), 8, 146, 150, 12, false);

		addFixedLine(xMSF, cont, "sep4", 5, 162, 160, 2);

		addButton(xMSF, cont, "btnOk", "OK", 30, 176, 50, 14);
		addButton(xMSF, cont, "btnCancel", I18n.get("button.abbrechen"), 100, 176, 60, 14);

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
			Formation formation = readFormation(xcc);
			boolean teamnameAnzeigen = readCheckBoxState(xcc, "cbTeamname");
			boolean vereinsnameAnzeigen = readCheckBoxState(xcc, "cbVereinsname");
			SpielplanTeamAnzeige spielplanAnzeige = readListBoxSelected(xcc, "lstSpielplanAnzeige") == 1
					? SpielplanTeamAnzeige.NAME : SpielplanTeamAnzeige.NR;
			int gruppengroesse = parseGruppengroesse(readEditText(xcc, "editGruppengroesse"));
			boolean mitRueckrunde = readCheckBoxState(xcc, "cbRueckrunde");
			result = Optional.of(new StartParameter(formation, teamnameAnzeigen, vereinsnameAnzeigen, spielplanAnzeige, gruppengroesse, mitRueckrunde));
		}

		Lo.qi(XComponent.class, dialog).dispose();
		ProcessBox.from().visible();

		return result;
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden – Zustand auslesen
	// ---------------------------------------------------------------

	private int parseGruppengroesse(String text) {
		try {
			return Math.max(0, Integer.parseInt(text.trim()));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String readEditText(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return "";
		}
		XTextComponent text = Lo.qi(XTextComponent.class, ctrl);
		return text != null ? text.getText() : "";
	}

	private Formation readFormation(XControlContainer xcc) {
		if (isRadioSelected(xcc, "radioDoublette")) {
			return Formation.DOUBLETTE;
		}
		if (isRadioSelected(xcc, "radioTriplette")) {
			return Formation.TRIPLETTE;
		}
		return Formation.TETE;
	}

	private boolean isRadioSelected(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return false;
		}
		XRadioButton radio = Lo.qi(XRadioButton.class, ctrl);
		return radio != null && radio.getState();
	}

	private short readListBoxSelected(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return 0;
		}
		XListBox lb = Lo.qi(XListBox.class, ctrl);
		return lb != null ? lb.getSelectedItemPos() : 0;
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

	private void addListBox(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, String[] items, short selectedIndex, int x, int y, int w, int h)
			throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Dropdown", Boolean.TRUE);
		props.setPropertyValue("StringItemList", items);
		props.setPropertyValue("SelectedItems", new short[] { selectedIndex });
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
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

	public static JGJStartDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new JGJStartDialog(workingSpreadsheet);
	}
}
