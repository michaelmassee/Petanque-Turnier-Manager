/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

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
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.ko.konfiguration.KoSpielbaumTeamAnzeige;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterGruppenModus;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter für ein neues Maastrichter Turnier.
 * Erweitert den Schweizer-Dialog um Anzahl Vorrunden und Finalgruppen-Einteilungsmodus.
 */
public class MaastrichterTurnierParameterDialog {

	/** Ergebnis des Dialogs. */
	public static class TurnierParameter {
		public final Formation formation;
		public final boolean teamnameAnzeigen;
		public final boolean vereinsnameAnzeigen;
		public final SpielplanTeamAnzeige spielplanTeamAnzeige;
		public final SchweizerRankingModus rankingModus;
		public final int anzVorrunden;
		public final KoSpielbaumTeamAnzeige spielbaumTeamAnzeige;
		public final SpielrundeSpielbahn spielbaumSpielbahn;
		public final boolean spielUmPlatz3;
		public final int gruppenGroesse;
		public final int minRestGroesse;
		public final MaastrichterGruppenModus gruppenModus;

		public TurnierParameter(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
				SpielplanTeamAnzeige spielplanTeamAnzeige, SchweizerRankingModus rankingModus, int anzVorrunden,
				KoSpielbaumTeamAnzeige spielbaumTeamAnzeige, SpielrundeSpielbahn spielbaumSpielbahn,
				boolean spielUmPlatz3, int gruppenGroesse, int minRestGroesse,
				MaastrichterGruppenModus gruppenModus) {
			this.formation = formation;
			this.teamnameAnzeigen = teamnameAnzeigen;
			this.vereinsnameAnzeigen = vereinsnameAnzeigen;
			this.spielplanTeamAnzeige = spielplanTeamAnzeige;
			this.rankingModus = rankingModus;
			this.anzVorrunden = anzVorrunden;
			this.spielbaumTeamAnzeige = spielbaumTeamAnzeige;
			this.spielbaumSpielbahn = spielbaumSpielbahn;
			this.spielUmPlatz3 = spielUmPlatz3;
			this.gruppenGroesse = gruppenGroesse;
			this.minRestGroesse = minRestGroesse;
			this.gruppenModus = gruppenModus;
		}
	}

	private final WorkingSpreadsheet workingSpreadsheet;
	private volatile boolean okPressed = false;

	public MaastrichterTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	public Optional<TurnierParameter> show(Formation defaultFormation, boolean defaultTeamnameAnzeigen,
			boolean defaultVereinsnameAnzeigen, SpielplanTeamAnzeige defaultSpielplanTeamAnzeige,
			SchweizerRankingModus defaultRankingModus, int defaultAnzVorrunden,
			KoSpielbaumTeamAnzeige defaultSpielbaumTeamAnzeige, SpielrundeSpielbahn defaultSpielbaumSpielbahn,
			boolean defaultSpielUmPlatz3, int defaultGruppenGroesse, int defaultMinRestGroesse,
			MaastrichterGruppenModus defaultGruppenModus)
			throws com.sun.star.uno.Exception {

		ProcessBox.from().hide();

		XComponentContext context = workingSpreadsheet.getxContext();
		XMultiComponentFactory xMCF = context.getServiceManager();

		Object dialogModel = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", context);
		XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
		dlgProps.setPropertyValue("PositionX", Integer.valueOf(50));
		dlgProps.setPropertyValue("PositionY", Integer.valueOf(50));
		dlgProps.setPropertyValue("Width", Integer.valueOf(160));
		dlgProps.setPropertyValue("Height", Integer.valueOf(410));
		dlgProps.setPropertyValue("Title", I18n.get("dialog.maastrichter.titel"));
		dlgProps.setPropertyValue("Moveable", Boolean.TRUE);

		Object dialog = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", context);
		XControl xControl = Lo.qi(XControl.class, dialog);
		xControl.setModel(Lo.qi(XControlModel.class, dialogModel));

		XMultiServiceFactory xMSF = Lo.qi(XMultiServiceFactory.class, dialogModel);
		XNameContainer cont = Lo.qi(XNameContainer.class, dialogModel);
		XControlContainer xcc = Lo.qi(XControlContainer.class, dialog);

		addLabel(xMSF, cont, "lblFormation", I18n.get("dialog.maastrichter.formation.label"), 8, 8, 80, 10);
		addRadioButton(xMSF, cont, "radioTete", Formation.TETE.getBezeichnung(), 8, 21, 140, 10,
				defaultFormation == Formation.TETE);
		addRadioButton(xMSF, cont, "radioDoublette", Formation.DOUBLETTE.getBezeichnung(), 8, 33, 140, 10,
				defaultFormation == Formation.DOUBLETTE);
		addRadioButton(xMSF, cont, "radioTriplette", Formation.TRIPLETTE.getBezeichnung(), 8, 45, 140, 10,
				defaultFormation == Formation.TRIPLETTE);

		addFixedLine(xMSF, cont, "sep1", 5, 59, 150, 2);
		addCheckBox(xMSF, cont, "cbTeamname", I18n.get("dialog.maastrichter.teamname.anzeigen"), 8, 65, 140, 10,
				defaultTeamnameAnzeigen);
		addCheckBox(xMSF, cont, "cbVereinsname", I18n.get("dialog.maastrichter.vereinsname.anzeigen"), 8, 79, 140, 10,
				defaultVereinsnameAnzeigen);

		addFixedLine(xMSF, cont, "sep2", 5, 95, 150, 2);
		addLabel(xMSF, cont, "lblSpielplan", I18n.get("dialog.maastrichter.spielplan.anzeige.label"), 8, 99, 140, 10);
		addRadioButton(xMSF, cont, "radioSpielplanNr", I18n.get("dialog.maastrichter.auswahl.nr"), 8, 111, 140, 10,
				defaultSpielplanTeamAnzeige == SpielplanTeamAnzeige.NR);
		addRadioButton(xMSF, cont, "radioSpielplanName", I18n.get("dialog.maastrichter.auswahl.name"), 8, 123, 140, 10,
				defaultSpielplanTeamAnzeige == SpielplanTeamAnzeige.NAME);

		addFixedLine(xMSF, cont, "sep3", 5, 137, 150, 2);
		addLabel(xMSF, cont, "lblRankingModus", I18n.get("dialog.maastrichter.ranking.modus.label"), 8, 141, 140, 10);
		addRadioButton(xMSF, cont, "radioMitBuchholz", I18n.get("dialog.maastrichter.ranking.mit.buchholz"), 8, 153,
				140, 10, defaultRankingModus != SchweizerRankingModus.OHNE_BUCHHOLZ);
		addRadioButton(xMSF, cont, "radioOhneBuchholz", I18n.get("dialog.maastrichter.ranking.ohne.buchholz"), 8, 165,
				140, 10, defaultRankingModus == SchweizerRankingModus.OHNE_BUCHHOLZ);

		addFixedLine(xMSF, cont, "sep4", 5, 179, 150, 2);
		addLabel(xMSF, cont, "lblAnzVorrunden", I18n.get("dialog.maastrichter.anz.vorrunden.label"), 8, 183, 100, 10);
		addNumericField(xMSF, cont, "nfAnzVorrunden", defaultAnzVorrunden, 2, 5, 112, 181, 40, 12);

		addFixedLine(xMSF, cont, "sep5", 5, 200, 150, 2);
		addLabel(xMSF, cont, "lblSpielbaumTeamAnzeige", I18n.get("dialog.maastrichter.spielbaum.anzeige.label"), 8,
				204, 140, 10);
		addRadioButton(xMSF, cont, "radioSpielbaumNr", I18n.get("dialog.maastrichter.auswahl.nr"), 8, 216, 140, 10,
				defaultSpielbaumTeamAnzeige == KoSpielbaumTeamAnzeige.NR);
		addRadioButton(xMSF, cont, "radioSpielbaumName", I18n.get("dialog.maastrichter.auswahl.name"), 8, 228, 140, 10,
				defaultSpielbaumTeamAnzeige == KoSpielbaumTeamAnzeige.NAME);

		addCheckBox(xMSF, cont, "cbSpielUmPlatz3", I18n.get("dialog.maastrichter.spiel.um.platz3"), 8, 242, 140, 10,
				defaultSpielUmPlatz3);

		addFixedLine(xMSF, cont, "sep6", 5, 256, 150, 2);
		addLabel(xMSF, cont, "lblGruppenModus", I18n.get("dialog.maastrichter.gruppen.modus.label"), 8, 260, 140, 10);
		addRadioButton(xMSF, cont, "radioNachSiegen", I18n.get("dialog.maastrichter.gruppen.modus.nach.siegen"), 8,
				272, 140, 10, defaultGruppenModus == MaastrichterGruppenModus.NACH_SIEGEN);
		addRadioButton(xMSF, cont, "radioNachGroesse", I18n.get("dialog.maastrichter.gruppen.modus.nach.groesse"), 8,
				284, 140, 10, defaultGruppenModus == MaastrichterGruppenModus.NACH_GROESSE);
		addLabel(xMSF, cont, "lblGruppenModusHinweis", I18n.get("dialog.maastrichter.gruppen.modus.hinweis"), 8, 296,
				150, 10);

		addFixedLine(xMSF, cont, "sep7", 5, 310, 150, 2);
		addLabel(xMSF, cont, "lblGruppenGroesse", I18n.get("dialog.maastrichter.gruppen.groesse.label"), 8, 314, 100,
				10);
		addNumericField(xMSF, cont, "nfGruppenGroesse", defaultGruppenGroesse, 2, 256, 112, 312, 40, 12);
		addLabel(xMSF, cont, "lblMinRestGroesse", I18n.get("dialog.maastrichter.min.rest.groesse.label"), 8, 329, 100,
				10);
		addNumericField(xMSF, cont, "nfMinRestGroesse", defaultMinRestGroesse, 1, 256, 112, 327, 40, 12);

		addButton(xMSF, cont, "btnOk", I18n.get("dialog.ok"), 22, 390, 50, 14);
		addButton(xMSF, cont, "btnCancel", I18n.get("dialog.abbrechen"), 88, 390, 60, 14);

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

		Object toolkit = xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", context);
		XToolkit xToolkit = Lo.qi(XToolkit.class, toolkit);
		XWindow xWindow = Lo.qi(XWindow.class, xControl);
		xWindow.setVisible(false);
		xControl.createPeer(xToolkit, null);

		xDialog.execute();

		Optional<TurnierParameter> result = Optional.empty();
		if (okPressed) {
			Formation formation = readFormation(xcc);
			boolean teamnameAnzeigen = readCheckBoxState(xcc, "cbTeamname");
			boolean vereinsnameAnzeigen = readCheckBoxState(xcc, "cbVereinsname");
			SpielplanTeamAnzeige spielplanAnzeige = isRadioSelected(xcc, "radioSpielplanName")
					? SpielplanTeamAnzeige.NAME : SpielplanTeamAnzeige.NR;
			SchweizerRankingModus rankingModus = isRadioSelected(xcc, "radioOhneBuchholz")
					? SchweizerRankingModus.OHNE_BUCHHOLZ : SchweizerRankingModus.MIT_BUCHHOLZ;
			int anzVorrunden = readNumericField(xcc, "nfAnzVorrunden", defaultAnzVorrunden);
			KoSpielbaumTeamAnzeige spielbaumTeamAnzeige = isRadioSelected(xcc, "radioSpielbaumName")
					? KoSpielbaumTeamAnzeige.NAME : KoSpielbaumTeamAnzeige.NR;
			boolean spielUmPlatz3 = readCheckBoxState(xcc, "cbSpielUmPlatz3");
			MaastrichterGruppenModus gruppenModus = isRadioSelected(xcc, "radioNachGroesse")
					? MaastrichterGruppenModus.NACH_GROESSE : MaastrichterGruppenModus.NACH_SIEGEN;
			int gruppenGroesse = readNumericField(xcc, "nfGruppenGroesse", defaultGruppenGroesse);
			int minRestGroesse = readNumericField(xcc, "nfMinRestGroesse", defaultMinRestGroesse);
			result = Optional.of(new TurnierParameter(formation, teamnameAnzeigen, vereinsnameAnzeigen,
					spielplanAnzeige, rankingModus, anzVorrunden, spielbaumTeamAnzeige,
					SpielrundeSpielbahn.X, spielUmPlatz3, gruppenGroesse, minRestGroesse, gruppenModus));
		}

		Lo.qi(XComponent.class, dialog).dispose();
		ProcessBox.from().visible();

		return result;
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden – Zustand auslesen
	// ---------------------------------------------------------------

	private Formation readFormation(XControlContainer xcc) {
		if (isRadioSelected(xcc, "radioTete")) return Formation.TETE;
		if (isRadioSelected(xcc, "radioDoublette")) return Formation.DOUBLETTE;
		return Formation.TRIPLETTE;
	}

	private boolean isRadioSelected(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) return false;
		XRadioButton radio = Lo.qi(XRadioButton.class, ctrl);
		return radio != null && radio.getState();
	}

	private boolean readCheckBoxState(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) return false;
		XCheckBox cb = Lo.qi(XCheckBox.class, ctrl);
		return cb != null && cb.getState() == 1;
	}

	private int readNumericField(XControlContainer xcc, String name, int defaultVal) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl == null) return defaultVal;
		XNumericField nf = Lo.qi(XNumericField.class, ctrl);
		if (nf == null) return defaultVal;
		return (int) nf.getValue();
	}

	private void attachButtonListener(XControlContainer xcc, String name, XActionListener listener) {
		XControl ctrl = xcc.getControl(name);
		if (ctrl != null) {
			XButton btn = Lo.qi(XButton.class, ctrl);
			if (btn != null) btn.addActionListener(listener);
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

	private void addNumericField(XMultiServiceFactory xMSF, XNameContainer cont,
			String name, int defaultVal, int minVal, int maxVal, int x, int y, int w, int h)
			throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlNumericFieldModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Value", (double) defaultVal);
		props.setPropertyValue("ValueMin", (double) minVal);
		props.setPropertyValue("ValueMax", (double) maxVal);
		props.setPropertyValue("DecimalAccuracy", (short) 0);
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
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

	public static MaastrichterTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new MaastrichterTurnierParameterDialog(workingSpreadsheet);
	}

}
