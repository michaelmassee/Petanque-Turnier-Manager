/*
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
import com.sun.star.awt.XListBox;
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
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.ko.konfiguration.KoPropertiesSpalte;
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
		public final int minLetzteGruppeGroesse;

		public TurnierParameter(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
				KoSpielbaumTeamAnzeige spielbaumTeamAnzeige, SpielrundeSpielbahn spielbaumSpielbahn,
				boolean spielUmPlatz3, int gruppenGroesse, int minLetzteGruppeGroesse) {
			this.formation = formation;
			this.teamnameAnzeigen = teamnameAnzeigen;
			this.vereinsnameAnzeigen = vereinsnameAnzeigen;
			this.spielbaumTeamAnzeige = spielbaumTeamAnzeige;
			this.spielbaumSpielbahn = spielbaumSpielbahn;
			this.spielUmPlatz3 = spielUmPlatz3;
			this.gruppenGroesse = gruppenGroesse;
			this.minLetzteGruppeGroesse = minLetzteGruppeGroesse;
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
			int defaultGruppenGroesse, int defaultMinLetzteGruppeGroesse) throws com.sun.star.uno.Exception {

		ProcessBox.from().hide();

		XComponentContext context = workingSpreadsheet.getxContext();
		XMultiComponentFactory xMCF = context.getServiceManager();

		// 1. Dialog-Modell anlegen
		Object dialogModel = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", context);
		XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
		dlgProps.setPropertyValue("PositionX", Integer.valueOf(50));
		dlgProps.setPropertyValue("PositionY", Integer.valueOf(50));
		dlgProps.setPropertyValue("Width", Integer.valueOf(160));
		dlgProps.setPropertyValue("Height", Integer.valueOf(215));
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
		addListBox(xMSF, cont, "lstFormation",
				new String[] { Formation.TETE.getBezeichnung(),
						Formation.DOUBLETTE.getBezeichnung(),
						Formation.TRIPLETTE.getBezeichnung() },
				formationIndex(defaultFormation), 92, 6, 60, 12);

		addFixedLine(xMSF, cont, "sep1", 5, 24, 150, 2);

		addCheckBox(xMSF, cont, "cbTeamname", "Teamname anzeigen", 8, 30, 140, 10, defaultTeamnameAnzeigen);
		addCheckBox(xMSF, cont, "cbVereinsname", "Vereinsname anzeigen", 8, 44, 140, 10, defaultVereinsnameAnzeigen);

		addFixedLine(xMSF, cont, "sep2", 5, 58, 150, 2);

		addLabel(xMSF, cont, "lblSpielbaum", "Anzeige im Spielbaum:", 8, 64, 80, 10);
		addListBox(xMSF, cont, "lstSpielbaum",
				new String[] { "Teamnummer", "Teamname" },
				(short) (defaultSpielbaumTeamAnzeige == KoSpielbaumTeamAnzeige.NAME ? 1 : 0),
				92, 62, 60, 12);

		addFixedLine(xMSF, cont, "sep3", 5, 80, 150, 2);

		addLabel(xMSF, cont, "lblSpielbahn", "Spielbahn im Spielbaum:", 8, 86, 70, 10);
		addListBox(xMSF, cont, "lstSpielbahn",
				new String[] { "Keine Spalte", "Leere Spalte (händisch)",
						"Durchnummerieren (1-n)", "Zufällig vergeben" },
				spielbahnIndex(defaultSpielbahn), 82, 84, 70, 12);

		addFixedLine(xMSF, cont, "sep4", 5, 104, 150, 2);

		addCheckBox(xMSF, cont, "cbPlatz3", "Spiel um Platz 3/4", 8, 110, 140, 10, defaultSpielUmPlatz3);

		addFixedLine(xMSF, cont, "sep5", 5, 124, 150, 2);

		addLabel(xMSF, cont, "lblGruppenGroesse", "Gruppen Größe:", 8, 130, 80, 10);
		addListBox(xMSF, cont, "lstGruppenGroesse", erlaubteGruppenGroessenAlsStrings(),
				(short) KoPropertiesSpalte.indexAusGruppenGroesse(defaultGruppenGroesse), 92, 142, 60, 12);

		addFixedLine(xMSF, cont, "sep6", 5, 158, 150, 2);

		addLabel(xMSF, cont, "lblMinLetzteGruppe", I18n.get("dialog.ko.min.letzte.gruppe.label"), 8, 164, 80, 10);
		addListBox(xMSF, cont, "lstMinLetzteGruppe", erlaubteMinLetzteGruppenGroessenAlsStrings(),
				(short) KoPropertiesSpalte.indexAusMinLetzteGruppenGroesse(defaultMinLetzteGruppeGroesse), 92, 176, 60, 12);

		addFixedLine(xMSF, cont, "sep7", 5, 192, 150, 2);

		addButton(xMSF, cont, "btnOk", "OK", 22, 198, 50, 14);
		addButton(xMSF, cont, "btnCancel", "Abbrechen", 88, 198, 60, 14);

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
			KoSpielbaumTeamAnzeige spielbaumAnzeige = readListBoxSelected(xcc, "lstSpielbaum") == 1
					? KoSpielbaumTeamAnzeige.NAME
					: KoSpielbaumTeamAnzeige.NR;
			SpielrundeSpielbahn spielbahn = switch (readListBoxSelected(xcc, "lstSpielbahn")) {
				case 1 -> SpielrundeSpielbahn.L;
				case 2 -> SpielrundeSpielbahn.N;
				case 3 -> SpielrundeSpielbahn.R;
				default -> SpielrundeSpielbahn.X;
			};
			boolean spielUmPlatz3 = readCheckBoxState(xcc, "cbPlatz3");
			int gruppenGroesse = KoPropertiesSpalte.getErlaubteGruppenGroessen()
					.get(readListBoxSelected(xcc, "lstGruppenGroesse"));
			int minLetzteGruppeGroesse = KoPropertiesSpalte.getErlaubteMinLetzteGruppenGroessen()
					.get(readListBoxSelected(xcc, "lstMinLetzteGruppe"));
			result = Optional.of(new TurnierParameter(formation, teamnameAnzeigen, vereinsnameAnzeigen,
					spielbaumAnzeige, spielbahn, spielUmPlatz3, gruppenGroesse, minLetzteGruppeGroesse));
		}

		Lo.qi(XComponent.class, dialog).dispose();
		ProcessBox.from().visibleWennAutomatisch();

		return result;
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden – Zustand auslesen
	// ---------------------------------------------------------------

	private Formation readFormation(XControlContainer xcc) {
		return switch (readListBoxSelected(xcc, "lstFormation")) {
			case 1 -> Formation.DOUBLETTE;
			case 2 -> Formation.TRIPLETTE;
			default -> Formation.TETE;
		};
	}

	private static short formationIndex(Formation formation) {
		return switch (formation) {
			case DOUBLETTE -> 1;
			case TRIPLETTE -> 2;
			default -> 0;
		};
	}

	private static short spielbahnIndex(SpielrundeSpielbahn spielbahn) {
		return switch (spielbahn) {
			case L -> 1;
			case N -> 2;
			case R -> 3;
			default -> 0;
		};
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

	private static String[] erlaubteGruppenGroessenAlsStrings() {
		return KoPropertiesSpalte.getErlaubteGruppenGroessen().stream()
				.map(String::valueOf).toArray(String[]::new);
	}

	private static String[] erlaubteMinLetzteGruppenGroessenAlsStrings() {
		return KoPropertiesSpalte.getErlaubteMinLetzteGruppenGroessen().stream()
				.map(String::valueOf).toArray(String[]::new);
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
}
