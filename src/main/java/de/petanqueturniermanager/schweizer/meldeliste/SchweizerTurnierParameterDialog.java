/*
 * Erstellung : 27.02.2026 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
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
import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteSpaltenDialogOption;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungDialogOption;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungModus;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeige;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;

/**
 * Modaler Dialog zur Abfrage der Turnier-Parameter vor dem Anlegen einer neuen
 * Schweizer Meldeliste.
 * <p>
 * Abgefragt werden:
 * <ul>
 * <li>Meldeliste: Formation (Tête / Doublette / Triplette / Nur Teamname), Zusatzspalten „Teamname“ und „Verein“, Mêlée-Anmeldung</li>
 * <li>Anzeige der Teams im Spielplan: Teamnummer / Spielernamen / Teamname</li>
 * <li>Ranglisten-Wertung: mit / ohne Buchholz</li>
 * </ul>
 */
public class SchweizerTurnierParameterDialog {

	/** Ergebnis des Dialogs. */
	public record TurnierParameter(Formation formation, boolean teamnameAnzeigen, boolean vereinsnameAnzeigen,
			Optional<MeleeAnmeldungModus> meleeAnmeldung, TeamAnzeige spielplanTeamAnzeige, SchweizerRankingModus rankingModus) {
	}

	private static final String LST_SPIELPLAN = "lstSpielplan";
	private static final String LST_RANKING = "lstRankingModus";

	// Layout in Dialog-Einheiten: Labels links, alle Listboxen in einer gemeinsamen Spalte
	private static final int DIALOG_BREITE = 210;
	private static final int RAND_X = 8;
	private static final int INHALT_BREITE = DIALOG_BREITE - 2 * RAND_X;
	private static final int LABEL_BREITE = 88;
	private static final int LISTBOX_X = RAND_X + LABEL_BREITE + 2;
	private static final int LISTBOX_BREITE = DIALOG_BREITE - RAND_X - LISTBOX_X;
	private static final int CONTROL_HOEHE = 12;
	private static final int LABEL_HOEHE = 10;
	private static final int GRUPPE_HOEHE = 8;
	private static final int BUTTON_BREITE = 60;
	private static final int BUTTON_HOEHE = 14;

	private static final int Y_GRUPPE_MELDELISTE = 4;
	private static final int Y_FORMATION = 16;
	private static final int Y_SPALTEN = 32;
	private static final int Y_MELEE = Y_SPALTEN + MeldelisteSpaltenDialogOption.HOEHE;
	private static final int Y_GRUPPE_SPIELPLAN = Y_MELEE + MeleeAnmeldungDialogOption.HOEHE + 6;
	private static final int Y_SPIELPLAN = Y_GRUPPE_SPIELPLAN + 12;
	private static final int Y_GRUPPE_RANGLISTE = Y_SPIELPLAN + 20;
	private static final int Y_RANKING = Y_GRUPPE_RANGLISTE + 12;
	private static final int Y_BUTTONS = Y_RANKING + 24;
	private static final int DIALOG_HOEHE = Y_BUTTONS + BUTTON_HOEHE + 8;

	private final WorkingSpreadsheet workingSpreadsheet;
	private volatile boolean okPressed = false;

	public SchweizerTurnierParameterDialog(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	/**
	 * Zeigt den Dialog an und gibt das Ergebnis zurück.
	 *
	 * @param defaultFormation            vorausgewählte Formation
	 * @param defaultTeamnameAnzeigen     vorausgewählter Teamname-Status
	 * @param defaultVereinsnameAnzeigen  vorausgewählter Vereinsname-Status
	 * @param defaultSpielplanTeamAnzeige vorausgewählte Spielplan-Anzeige
	 * @param defaultRankingModus         vorausgewählte Ranglisten-Wertung
	 * @return Optional mit TurnierParameter bei OK, leer bei Abbrechen
	 */
	public Optional<TurnierParameter> show(Formation defaultFormation, boolean defaultTeamnameAnzeigen,
			boolean defaultVereinsnameAnzeigen, TeamAnzeige defaultSpielplanTeamAnzeige,
			SchweizerRankingModus defaultRankingModus) throws com.sun.star.uno.Exception {

		ProcessBox.from().hide();

		XComponentContext context = workingSpreadsheet.getxContext();
		XMultiComponentFactory xMCF = context.getServiceManager();

		// 1. Dialog-Modell anlegen und Eigenschaften setzen (VOR setModel)
		Object dialogModel = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", context);
		XPropertySet dlgProps = Lo.qi(XPropertySet.class, dialogModel);
		dlgProps.setPropertyValue("PositionX", Integer.valueOf(50));
		dlgProps.setPropertyValue("PositionY", Integer.valueOf(50));
		dlgProps.setPropertyValue("Width", Integer.valueOf(DIALOG_BREITE));
		dlgProps.setPropertyValue("Height", Integer.valueOf(DIALOG_HOEHE));
		dlgProps.setPropertyValue("Title", I18n.get("dialog.schweizer.titel"));
		dlgProps.setPropertyValue("Moveable", Boolean.TRUE);

		// 2. Dialog-Control anlegen und Modell setzen
		Object dialog = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", context);
		XControl xControl = Lo.qi(XControl.class, dialog);
		xControl.setModel(Lo.qi(XControlModel.class, dialogModel));

		// 3. Child-Controls via XMultiServiceFactory des Dialog-Modells anlegen
		XMultiServiceFactory xMSF = Lo.qi(XMultiServiceFactory.class, dialogModel);
		XNameContainer cont = Lo.qi(XNameContainer.class, dialogModel);
		XControlContainer xcc = Lo.qi(XControlContainer.class, dialog);

		// Abschnitt Meldeliste: welche Spalten die Meldeliste bekommt
		addGruppe(xMSF, cont, "grpMeldeliste", I18n.get("dialog.schweizer.gruppe.meldeliste"), Y_GRUPPE_MELDELISTE);
		addLabel(xMSF, cont, "lblFormation", I18n.get("dialog.poule.label.formation"), Y_FORMATION);
		addListBox(xMSF, cont, "lstFormation",
				new String[] { Formation.TETE.getBezeichnung(),
						Formation.DOUBLETTE.getBezeichnung(),
						Formation.TRIPLETTE.getBezeichnung(),
						Formation.NUR_TEAMNAME.getBezeichnung() },
				formationIndex(defaultFormation), Y_FORMATION, null);
		MeldelisteSpaltenDialogOption.hinzufuegen(xMSF, cont, RAND_X, Y_SPALTEN, INHALT_BREITE, defaultFormation,
				defaultTeamnameAnzeigen, defaultVereinsnameAnzeigen);
		MeleeAnmeldungDialogOption.hinzufuegen(xMSF, cont, RAND_X, Y_MELEE, INHALT_BREITE, LABEL_BREITE,
				defaultFormation);

		// Abschnitt Spielplan
		addGruppe(xMSF, cont, "grpSpielplan", I18n.get("dialog.schweizer.gruppe.spielplan"), Y_GRUPPE_SPIELPLAN);
		addLabel(xMSF, cont, "lblSpielplan", I18n.get("dialog.schweizer.spielplan.anzeige.label"), Y_SPIELPLAN);
		addListBox(xMSF, cont, LST_SPIELPLAN,
				new String[] { I18n.get("dialog.schweizer.auswahl.nr"), I18n.get("dialog.schweizer.auswahl.spieler"),
						I18n.get("dialog.schweizer.auswahl.name") },
				defaultSpielplanTeamAnzeige.dialogIndex(), Y_SPIELPLAN, I18n.get("dialog.schweizer.spielplan.hilfe"));

		// Abschnitt Rangliste
		addGruppe(xMSF, cont, "grpRangliste", I18n.get("dialog.schweizer.gruppe.rangliste"), Y_GRUPPE_RANGLISTE);
		addLabel(xMSF, cont, "lblRankingModus", I18n.get("dialog.schweizer.ranking.modus.label"), Y_RANKING);
		addListBox(xMSF, cont, LST_RANKING,
				new String[] { I18n.get("dialog.schweizer.ranking.mit.buchholz"),
						I18n.get("dialog.schweizer.ranking.ohne.buchholz") },
				(short) (defaultRankingModus == SchweizerRankingModus.OHNE_BUCHHOLZ ? 1 : 0), Y_RANKING, null);

		addButton(xMSF, cont, "btnOk", I18n.get("dialog.button.ok"), DIALOG_BREITE / 2 - BUTTON_BREITE - 4);
		addButton(xMSF, cont, "btnCancel", I18n.get("dialog.button.abbrechen"), DIALOG_BREITE / 2 + 4);

		MeleeAnmeldungDialogOption.anFormationKoppeln(xcc, () -> readFormation(xcc),
				formation -> MeleeAnmeldungDialogOption.listBoxPositionWaehlen(xcc, "lstFormation",
						formationIndex(formation)),
				"lstFormation");
		MeldelisteSpaltenDialogOption.anFormationKoppeln(xcc, () -> readFormation(xcc), "lstFormation");

		// 4. Button-Listener VOR createPeer() anhängen
		XDialog xDialog = Lo.qi(XDialog.class, dialog);
		okPressed = false;
		attachButtonListener(xcc, "btnOk", new XActionListener() {
			@Override
			public void disposing(EventObject e) {
				// keine Ressourcen zu lösen
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
				// keine Ressourcen zu lösen
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
			boolean teamnameAnzeigen = MeldelisteSpaltenDialogOption.leseTeamname(xcc, formation);
			boolean vereinsnameAnzeigen = MeldelisteSpaltenDialogOption.leseVereinsname(xcc);
			Optional<MeleeAnmeldungModus> meleeAnmeldung = MeleeAnmeldungDialogOption.leseModus(xcc, formation);
			TeamAnzeige spielplanAnzeige = TeamAnzeige.ausDialogIndex(readListBoxSelected(xcc, LST_SPIELPLAN));
			SchweizerRankingModus rankingModus = readListBoxSelected(xcc, LST_RANKING) == 1
					? SchweizerRankingModus.OHNE_BUCHHOLZ : SchweizerRankingModus.MIT_BUCHHOLZ;
			result = Optional.of(new TurnierParameter(formation, teamnameAnzeigen, vereinsnameAnzeigen, meleeAnmeldung,
					spielplanAnzeige, rankingModus));
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
			case 3 -> Formation.NUR_TEAMNAME;
			default -> Formation.TETE;
		};
	}

	private static short formationIndex(Formation formation) {
		return switch (formation) {
			case DOUBLETTE -> 1;
			case TRIPLETTE -> 2;
			case NUR_TEAMNAME -> 3;
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

	/** Abschnittsüberschrift: beschriftete Trennlinie über die volle Dialogbreite. */
	private void addGruppe(XMultiServiceFactory xMSF, XNameContainer cont, String name, String titel, int y)
			throws com.sun.star.uno.Exception {
		XPropertySet props = neuesModel(xMSF, "com.sun.star.awt.UnoControlFixedLineModel", RAND_X / 2, y,
				DIALOG_BREITE - RAND_X, GRUPPE_HOEHE);
		props.setPropertyValue("Label", titel);
		cont.insertByName(name, props);
	}

	private void addLabel(XMultiServiceFactory xMSF, XNameContainer cont, String name, String text, int y)
			throws com.sun.star.uno.Exception {
		XPropertySet props = neuesModel(xMSF, "com.sun.star.awt.UnoControlFixedTextModel", RAND_X, y + 2,
				LABEL_BREITE, LABEL_HOEHE);
		props.setPropertyValue("Label", text);
		cont.insertByName(name, props);
	}

	private void addListBox(XMultiServiceFactory xMSF, XNameContainer cont, String name, String[] items,
			short selectedIndex, int y, @Nullable String hilfeText) throws com.sun.star.uno.Exception {
		XPropertySet props = neuesModel(xMSF, "com.sun.star.awt.UnoControlListBoxModel", LISTBOX_X, y,
				LISTBOX_BREITE, CONTROL_HOEHE);
		props.setPropertyValue("Dropdown", Boolean.TRUE);
		props.setPropertyValue("StringItemList", items);
		props.setPropertyValue("SelectedItems", new short[] { selectedIndex });
		if (hilfeText != null) {
			props.setPropertyValue("HelpText", hilfeText);
		}
		cont.insertByName(name, props);
	}

	private void addButton(XMultiServiceFactory xMSF, XNameContainer cont, String name, String label, int x)
			throws com.sun.star.uno.Exception {
		XPropertySet props = neuesModel(xMSF, "com.sun.star.awt.UnoControlButtonModel", x, Y_BUTTONS,
				BUTTON_BREITE, BUTTON_HOEHE);
		props.setPropertyValue("Label", label);
		cont.insertByName(name, props);
	}

	private static XPropertySet neuesModel(XMultiServiceFactory xMSF, String service, int x, int y, int w, int h)
			throws com.sun.star.uno.Exception {
		XPropertySet props = Lo.qi(XPropertySet.class, xMSF.createInstance(service));
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(w));
		props.setPropertyValue("Height", Integer.valueOf(h));
		return props;
	}

	public static SchweizerTurnierParameterDialog from(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerTurnierParameterDialog(workingSpreadsheet);
	}
}
