/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import java.util.Optional;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XNumericField;
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
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.ko.konfiguration.KoSpielbaumTeamAnzeige;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter vor dem Anlegen einer neuen
 * K.-O.-Meldeliste.
 * <p>
 * Abgefragt werden:
 * <ul>
 * <li>Formation: Tête / Doublette / Triplette</li>
 * <li>Teamname anzeigen</li>
 * <li>Vereinsname anzeigen</li>
 * <li>Anzeige im Spielbaum: Teamnummer / Teamname</li>
 * <li>Bahnnummer im Spielbaum</li>
 * <li>Spiel um Platz 3/4</li>
 * <li>Turnierbaum Gruppen Größe</li>
 * <li>Turnierbaum Min. Rest-Größe</li>
 * </ul>
 */
public class KoTurnierParameterDialog {

	/** Ergebnis des Dialogs. */
	public static class TurnierParameter {
		public final Formation formation;
		public final boolean teamnameAnzeigen;
		public final boolean vereinsnameAnzeigen;
		public final KoSpielbaumTeamAnzeige spielbaumTeamAnzeige;
		public final SpielrundeSpielbahn spielbaumSpielbahn;
		public final boolean spielUmPlatz3;
		public final int gruppenGroesse;
		public final int minRestGroesse;

		public TurnierParameter(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
				KoSpielbaumTeamAnzeige spielbaumTeamAnzeige, SpielrundeSpielbahn spielbaumSpielbahn,
				boolean spielUmPlatz3, int gruppenGroesse, int minRestGroesse) {
			this.formation = formation;
			this.teamnameAnzeigen = teamnameAnzeigen;
			this.vereinsnameAnzeigen = vereinsnameAnzeigen;
			this.spielbaumTeamAnzeige = spielbaumTeamAnzeige;
			this.spielbaumSpielbahn = spielbaumSpielbahn;
			this.spielUmPlatz3 = spielUmPlatz3;
			this.gruppenGroesse = gruppenGroesse;
			this.minRestGroesse = minRestGroesse;
		}
	}

	private final WorkingSpreadsheet workingSpreadsheet;
	private volatile boolean okPressed = false;

	public KoTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	public static KoTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new KoTurnierParameterDialog(workingSpreadsheet);
	}

	/**
	 * Zeigt den Dialog an und gibt das Ergebnis zurück.
	 *
	 * @return Optional mit TurnierParameter bei OK, leer bei Abbrechen
	 */
	public Optional<TurnierParameter> show(Formation defaultFormation, boolean defaultTeamnameAnzeigen,
			boolean defaultVereinsnameAnzeigen, KoSpielbaumTeamAnzeige defaultSpielbaumTeamAnzeige,
			SpielrundeSpielbahn defaultSpielbahn, boolean defaultSpielUmPlatz3,
			int defaultGruppenGroesse, int defaultMinRestGroesse) throws com.sun.star.uno.Exception {

		ProcessBox.from().hide();

		XComponentContext context = workingSpreadsheet.getxContext();
		XMultiComponentFactory xMCF = context.getServiceManager();

		// 1. Dialog-Modell anlegen
		Object dialogModel = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", context);
		XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
		dlgProps.setPropertyValue("PositionX", Integer.valueOf(50));
		dlgProps.setPropertyValue("PositionY", Integer.valueOf(50));
		dlgProps.setPropertyValue("Width", Integer.valueOf(160));
		dlgProps.setPropertyValue("Height", Integer.valueOf(320));
		dlgProps.setPropertyValue("Title", "K.-O. Turnier \u2013 Parameter");
		dlgProps.setPropertyValue("Moveable", Boolean.TRUE);

		// 2. Dialog-Control anlegen
		Object dialog = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", context);
		XControl xControl = Lo.qi(XControl.class, dialog);
		xControl.setModel(Lo.qi(XControlModel.class, dialogModel));

		// 3. Child-Controls anlegen
		XMultiServiceFactory xMSF = Lo.qi(XMultiServiceFactory.class, dialogModel);
		XNameContainer cont = Lo.qi(XNameContainer.class, dialogModel);
		XControlContainer xcc = Lo.qi(XControlContainer.class, dialog);

		addLabel(xMSF, cont, "lblFormation", "Formation:", 8, 8, 80, 10);
		addRadioButton(xMSF, cont, "radioTete", Formation.TETE.getBezeichnung(), 8, 21, 140, 10,
				defaultFormation == Formation.TETE);
		addRadioButton(xMSF, cont, "radioDoublette", Formation.DOUBLETTE.getBezeichnung(), 8, 33, 140, 10,
				defaultFormation == Formation.DOUBLETTE);
		addRadioButton(xMSF, cont, "radioTriplette", Formation.TRIPLETTE.getBezeichnung(), 8, 45, 140, 10,
				defaultFormation == Formation.TRIPLETTE);

		addFixedLine(xMSF, cont, "sep1", 5, 59, 150, 2);

		addCheckBox(xMSF, cont, "cbTeamname", "Teamname anzeigen", 8, 65, 140, 10, defaultTeamnameAnzeigen);
		addCheckBox(xMSF, cont, "cbVereinsname", "Vereinsname anzeigen", 8, 79, 140, 10, defaultVereinsnameAnzeigen);

		addFixedLine(xMSF, cont, "sep2", 5, 93, 150, 2);

		addLabel(xMSF, cont, "lblSpielbaum", "Anzeige im Spielbaum:", 8, 99, 140, 10);
		addRadioButton(xMSF, cont, "radioSpielbaumNr", "Teamnummer", 8, 111, 140, 10,
				defaultSpielbaumTeamAnzeige == KoSpielbaumTeamAnzeige.NR);
		addRadioButton(xMSF, cont, "radioSpielbaumName", "Teamname", 8, 123, 140, 10,
				defaultSpielbaumTeamAnzeige == KoSpielbaumTeamAnzeige.NAME);

		addFixedLine(xMSF, cont, "sep3", 5, 137, 150, 2);

		addLabel(xMSF, cont, "lblSpielbahn", "Spielbahn im Spielbaum:", 8, 143, 140, 10);
		addRadioButton(xMSF, cont, "radioSpielbahnX", "Keine Spalte", 8, 155, 140, 10,
				defaultSpielbahn == SpielrundeSpielbahn.X);
		addRadioButton(xMSF, cont, "radioSpielbahnL", "Leere Spalte (händisch)", 8, 167, 140, 10,
				defaultSpielbahn == SpielrundeSpielbahn.L);
		addRadioButton(xMSF, cont, "radioSpielbahnN", "Durchnummerieren (1-n)", 8, 179, 140, 10,
				defaultSpielbahn == SpielrundeSpielbahn.N);
		addRadioButton(xMSF, cont, "radioSpielbahnR", "Zufällig vergeben", 8, 191, 140, 10,
				defaultSpielbahn == SpielrundeSpielbahn.R);

		addFixedLine(xMSF, cont, "sep4", 5, 207, 150, 2);

		addCheckBox(xMSF, cont, "cbPlatz3", "Spiel um Platz 3/4", 8, 213, 140, 10, defaultSpielUmPlatz3);

		addFixedLine(xMSF, cont, "sep5", 5, 227, 150, 2);

		addLabel(xMSF, cont, "lblGruppenGroesse", "Gruppen Größe:", 8, 233, 100, 10);
		addNumericField(xMSF, cont, "tfGruppenGroesse", 8, 245, 60, 12, defaultGruppenGroesse, 2, 512);

		addFixedLine(xMSF, cont, "sep6", 5, 261, 150, 2);

		addLabel(xMSF, cont, "lblMinRestGroesse", "Min. Rest-Größe:", 8, 267, 100, 10);
		addNumericField(xMSF, cont, "tfMinRestGroesse", 8, 279, 60, 12, defaultMinRestGroesse, 1, 512);

		addFixedLine(xMSF, cont, "sep7", 5, 295, 150, 2);

		addButton(xMSF, cont, "btnOk", "OK", 22, 303, 50, 14);
		addButton(xMSF, cont, "btnCancel", "Abbrechen", 88, 303, 60, 14);

		// 4. Button-Listener anhängen
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

		Optional<TurnierParameter> result = Optional.empty();
		if (okPressed) {
			Formation formation = readFormation(xcc);
			boolean teamnameAnzeigen = readCheckBoxState(xcc, "cbTeamname");
			boolean vereinsnameAnzeigen = readCheckBoxState(xcc, "cbVereinsname");
			KoSpielbaumTeamAnzeige spielbaumAnzeige = isRadioSelected(xcc, "radioSpielbaumName")
					? KoSpielbaumTeamAnzeige.NAME
					: KoSpielbaumTeamAnzeige.NR;
			SpielrundeSpielbahn spielbahn = SpielrundeSpielbahn.X;
			if (isRadioSelected(xcc, "radioSpielbahnL")) spielbahn = SpielrundeSpielbahn.L;
			else if (isRadioSelected(xcc, "radioSpielbahnN")) spielbahn = SpielrundeSpielbahn.N;
			else if (isRadioSelected(xcc, "radioSpielbahnR")) spielbahn = SpielrundeSpielbahn.R;
			boolean spielUmPlatz3 = readCheckBoxState(xcc, "cbPlatz3");
			int gruppenGroesse = readNumericFieldValue(xcc, "tfGruppenGroesse", defaultGruppenGroesse);
			int minRestGroesse = readNumericFieldValue(xcc, "tfMinRestGroesse", defaultMinRestGroesse);
			result = Optional.of(new TurnierParameter(formation, teamnameAnzeigen, vereinsnameAnzeigen,
					spielbaumAnzeige, spielbahn, spielUmPlatz3, gruppenGroesse, minRestGroesse));
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

	private int readNumericFieldValue(XControlContainer xcc, String name, int fallback) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) {
			return fallback;
		}
		XNumericField nf = Lo.qi(XNumericField.class, ctrl);
		return (nf != null) ? (int) nf.getValue() : fallback;
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
	// Hilfsmethoden – Controls hinzufügen
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

	private void addNumericField(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, int x, int y, int w, int h, int value, int min, int max)
			throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlNumericFieldModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
		props.setPropertyValue("Value", (double) value);
		props.setPropertyValue("ValueMin", (double) min);
		props.setPropertyValue("ValueMax", (double) max);
		props.setPropertyValue("DecimalAccuracy", (short) 0);
		props.setPropertyValue("Spin", Boolean.TRUE);
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
}
