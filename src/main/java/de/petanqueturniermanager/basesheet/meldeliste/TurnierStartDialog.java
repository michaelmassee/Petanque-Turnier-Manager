/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.sun.star.awt.PushButtonType;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * Gemeinsamer Aufbau der modalen Start-Dialoge der Turniersysteme (Parameter vor dem Anlegen einer
 * neuen Meldeliste).
 * <p>
 * Einheitliches Layout: beschriftete Abschnitte ({@link #abschnitt}), darunter Zeilen mit Label links
 * und Eingabe-Control in einer gemeinsamen rechten Spalte. Die Controls werden von oben nach unten an
 * einem laufenden Zeilen-Cursor angeordnet, die Dialoghöhe ergibt sich daraus. Typischer Ablauf:
 *
 * <pre>
 * TurnierStartDialog dialog = TurnierStartDialog.erstellen(context, titel);
 * dialog.meldeliste(formation, teamname, vereinsname);
 * dialog.abschnitt(I18n.get("dialog.gruppe.spielplan"));
 * dialog.auswahl("lstX", label, eintraege, vorauswahl);
 * return dialog.anzeigen(d -&gt; new Parameter(d.formation(), d.gewaehlt("lstX")));
 * </pre>
 */
public final class TurnierStartDialog {

	private static final String LST_FORMATION = "lstFormation";
	private static final String BTN_OK = "btnOk";
	private static final String BTN_ABBRECHEN = "btnCancel";
	/** Rückgabe von {@link XDialog#execute()}, wenn der Dialog über den OK-Button beendet wurde. */
	private static final short ERGEBNIS_OK = 1;

	private static final SpielrundeSpielbahn[] SPIELBAHNEN = { SpielrundeSpielbahn.X, SpielrundeSpielbahn.L,
			SpielrundeSpielbahn.N, SpielrundeSpielbahn.R };

	private static final Formation[] FORMATIONEN = { Formation.TETE, Formation.DOUBLETTE, Formation.TRIPLETTE,
			Formation.NUR_TEAMNAME };

	// Layout in Dialog-Einheiten
	private static final int DIALOG_BREITE = 230;
	private static final int RAND_X = 8;
	private static final int INHALT_BREITE = DIALOG_BREITE - 2 * RAND_X;
	private static final int LABEL_BREITE = 118;
	private static final int CONTROL_X = RAND_X + LABEL_BREITE + 2;
	private static final int CONTROL_BREITE = DIALOG_BREITE - RAND_X - CONTROL_X;
	private static final int ZAHL_BREITE = 40;
	private static final int CONTROL_HOEHE = 12;
	private static final int LABEL_HOEHE = 10;
	private static final int ZEILEN_ABSTAND = 16;
	private static final int CHECKBOX_ABSTAND = 14;
	private static final int ABSCHNITT_HOEHE = 8;
	private static final int ABSCHNITT_ABSTAND_OBEN = 6;
	private static final int ABSCHNITT_ABSTAND_UNTEN = 4;
	private static final int BUTTON_BREITE = 60;
	private static final int BUTTON_HOEHE = 14;
	private static final int BUTTON_ABSTAND = 8;
	private static final int RAND_OBEN = 4;
	private static final int RAND_UNTEN = 8;

	private final XComponentContext context;
	private final Object dialog;
	private final XPropertySet dialogProps;
	private final XMultiServiceFactory xMSF;
	private final XNameContainer cont;
	private final XControlContainer xcc;
	private int y = RAND_OBEN;
	private int anzAbschnitte = 0;

	private TurnierStartDialog(XComponentContext context, String titel) throws com.sun.star.uno.Exception {
		this.context = context;
		XMultiComponentFactory xMCF = context.getServiceManager();
		Object dialogModel = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialogModel", context);
		dialogProps = Lo.qi(XPropertySet.class, dialogModel);
		dialogProps.setPropertyValue("PositionX", Integer.valueOf(50));
		dialogProps.setPropertyValue("PositionY", Integer.valueOf(50));
		dialogProps.setPropertyValue("Width", Integer.valueOf(DIALOG_BREITE));
		dialogProps.setPropertyValue("Title", titel);
		dialogProps.setPropertyValue("Moveable", Boolean.TRUE);

		dialog = xMCF.createInstanceWithContext("com.sun.star.awt.UnoControlDialog", context);
		Lo.qi(XControl.class, dialog).setModel(Lo.qi(XControlModel.class, dialogModel));

		xMSF = Lo.qi(XMultiServiceFactory.class, dialogModel);
		cont = Lo.qi(XNameContainer.class, dialogModel);
		xcc = Lo.qi(XControlContainer.class, dialog);
	}

	public static TurnierStartDialog erstellen(XComponentContext context, String titel)
			throws com.sun.star.uno.Exception {
		return new TurnierStartDialog(context, titel);
	}

	// ---------------------------------------------------------------
	// Aufbau
	// ---------------------------------------------------------------

	/** Abschnittsüberschrift: beschriftete Trennlinie über die volle Dialogbreite. */
	public void abschnitt(String titel) throws com.sun.star.uno.Exception {
		if (anzAbschnitte > 0) {
			y += ABSCHNITT_ABSTAND_OBEN;
		}
		anzAbschnitte++;
		XPropertySet props = neuesModel("com.sun.star.awt.UnoControlFixedLineModel", RAND_X / 2, y,
				DIALOG_BREITE - RAND_X, ABSCHNITT_HOEHE);
		props.setPropertyValue("Label", titel);
		cont.insertByName("abschnitt" + anzAbschnitte, props);
		y += ABSCHNITT_HOEHE + ABSCHNITT_ABSTAND_UNTEN;
	}

	/**
	 * Abschnitt „Meldeliste“: Formation, Zusatzspalten Teamname/Verein und Mêlée-Anmeldung, inklusive
	 * der Kopplungen an die Formation-Auswahl. Ausgelesen über {@link #formation()},
	 * {@link #teamname()}, {@link #vereinsname()} und {@link #meleeAnmeldung()}.
	 */
	public void meldeliste(Formation startFormation, boolean teamname, boolean vereinsname)
			throws com.sun.star.uno.Exception {
		abschnitt(I18n.get("dialog.gruppe.meldeliste"));
		String[] formationen = new String[FORMATIONEN.length];
		for (int i = 0; i < FORMATIONEN.length; i++) {
			formationen[i] = FORMATIONEN[i].getBezeichnung();
		}
		auswahl(LST_FORMATION, I18n.get("dialog.label.formation"), formationen, formationIndex(startFormation));
		MeldelisteSpaltenDialogOption.hinzufuegen(xMSF, cont, RAND_X, y, INHALT_BREITE, startFormation, teamname,
				vereinsname);
		y += MeldelisteSpaltenDialogOption.HOEHE;
		MeleeAnmeldungDialogOption.hinzufuegen(xMSF, cont, RAND_X, y, INHALT_BREITE, LABEL_BREITE, startFormation);
		y += MeleeAnmeldungDialogOption.HOEHE + 2;

		MeleeAnmeldungDialogOption.anFormationKoppeln(xcc, this::formation,
				formation -> MeleeAnmeldungDialogOption.listBoxPositionWaehlen(xcc, LST_FORMATION,
						formationIndex(formation)),
				LST_FORMATION);
		MeldelisteSpaltenDialogOption.anFormationKoppeln(xcc, this::formation, LST_FORMATION);
	}

	/** Zeile mit Label und Dropdown-Listbox. */
	public void auswahl(String name, String label, String[] eintraege, int vorauswahl)
			throws com.sun.star.uno.Exception {
		auswahl(name, label, eintraege, vorauswahl, null);
	}

	/** Zeile mit Label und Dropdown-Listbox, mit Tooltip. */
	public void auswahl(String name, String label, String[] eintraege, int vorauswahl, @Nullable String hilfeText)
			throws com.sun.star.uno.Exception {
		label(name, label);
		XPropertySet props = neuesModel("com.sun.star.awt.UnoControlListBoxModel", CONTROL_X, y, CONTROL_BREITE,
				CONTROL_HOEHE);
		props.setPropertyValue("Dropdown", Boolean.TRUE);
		props.setPropertyValue("StringItemList", eintraege);
		props.setPropertyValue("SelectedItems", new short[] { (short) vorauswahl });
		einfuegen(name, props, hilfeText);
		y += ZEILEN_ABSTAND;
	}

	/** Zeile „Spielbahn“: ob und wie im Spielbaum eine Bahnspalte erzeugt wird. */
	public void spielbahn(String name, SpielrundeSpielbahn vorauswahl) throws com.sun.star.uno.Exception {
		String[] eintraege = { I18n.get("dialog.spielbahn.keine"), I18n.get("dialog.spielbahn.leer"),
				I18n.get("dialog.spielbahn.nummeriert"), I18n.get("dialog.spielbahn.zufaellig") };
		int index = 0;
		for (int i = 0; i < SPIELBAHNEN.length; i++) {
			if (SPIELBAHNEN[i] == vorauswahl) {
				index = i;
			}
		}
		auswahl(name, I18n.get("dialog.label.spielbahn"), eintraege, index);
	}

	/** Zeile mit Label und ganzzahligem Eingabefeld (Wertebereich {@code min..max}). */
	public void zahl(String name, String label, int wert, int min, int max, @Nullable String hilfeText)
			throws com.sun.star.uno.Exception {
		label(name, label);
		XPropertySet props = neuesModel("com.sun.star.awt.UnoControlNumericFieldModel", CONTROL_X, y, ZAHL_BREITE,
				CONTROL_HOEHE);
		props.setPropertyValue("Value", (double) wert);
		props.setPropertyValue("ValueMin", (double) min);
		props.setPropertyValue("ValueMax", (double) max);
		props.setPropertyValue("DecimalAccuracy", (short) 0);
		props.setPropertyValue("Spin", Boolean.TRUE);
		einfuegen(name, props, hilfeText);
		y += ZEILEN_ABSTAND;
	}

	/** Zeile mit Label und freiem Textfeld (anfangs leer). */
	public void text(String name, String label, @Nullable String hilfeText) throws com.sun.star.uno.Exception {
		label(name, label);
		XPropertySet props = neuesModel("com.sun.star.awt.UnoControlEditModel", CONTROL_X, y, ZAHL_BREITE,
				CONTROL_HOEHE);
		props.setPropertyValue("Text", "");
		einfuegen(name, props, hilfeText);
		y += ZEILEN_ABSTAND;
	}

	/** Checkbox über die volle Inhaltsbreite. */
	public void checkBox(String name, String label, boolean angehakt, @Nullable String hilfeText)
			throws com.sun.star.uno.Exception {
		XPropertySet props = neuesModel("com.sun.star.awt.UnoControlCheckBoxModel", RAND_X, y, INHALT_BREITE,
				LABEL_HOEHE);
		props.setPropertyValue("Label", label);
		props.setPropertyValue("State", (short) (angehakt ? 1 : 0));
		einfuegen(name, props, hilfeText);
		y += CHECKBOX_ABSTAND;
	}

	// ---------------------------------------------------------------
	// Anzeigen
	// ---------------------------------------------------------------

	/**
	 * Ergänzt OK/Abbrechen, zeigt den Dialog modal an und wertet ihn bei OK aus. Der Dialog wird danach
	 * in jedem Fall freigegeben.
	 *
	 * @param auswerten liest die Eingaben über die Lese-Methoden dieses Dialogs aus
	 * @return Ergebnis von {@code auswerten} bei OK, leer bei Abbrechen
	 */
	public <T> Optional<T> anzeigen(Function<TurnierStartDialog, T> auswerten) throws com.sun.star.uno.Exception {
		ProcessBox.from().hide();
		try {
			buttonsHinzufuegen();
			XToolkit xToolkit = Lo.qi(XToolkit.class,
					context.getServiceManager().createInstanceWithContext("com.sun.star.awt.Toolkit", context));
			XControl xControl = Lo.qi(XControl.class, dialog);
			Lo.qi(XWindow.class, xControl).setVisible(false);
			xControl.createPeer(xToolkit, null);
			boolean ok = Lo.qi(XDialog.class, dialog).execute() == ERGEBNIS_OK;

			return ok ? Optional.of(auswerten.apply(this)) : Optional.empty();
		} finally {
			Lo.qi(XComponent.class, dialog).dispose();
			ProcessBox.from().visibleWennAutomatisch();
		}
	}

	private void buttonsHinzufuegen() throws com.sun.star.uno.Exception {
		y += BUTTON_ABSTAND;
		int mitte = DIALOG_BREITE / 2;
		button(BTN_OK, I18n.get("dialog.button.ok"), mitte - BUTTON_BREITE - 4, PushButtonType.OK);
		button(BTN_ABBRECHEN, I18n.get("dialog.button.abbrechen"), mitte + 4, PushButtonType.CANCEL);
		y += BUTTON_HOEHE + RAND_UNTEN;
		dialogProps.setPropertyValue("Height", Integer.valueOf(y));
	}

	// ---------------------------------------------------------------
	// Auslesen (nur innerhalb von anzeigen(...) gültig)
	// ---------------------------------------------------------------

	public Formation formation() {
		short position = gewaehlt(LST_FORMATION);
		return position >= 0 && position < FORMATIONEN.length ? FORMATIONEN[position] : Formation.TETE;
	}

	public boolean teamname() {
		return MeldelisteSpaltenDialogOption.leseTeamname(xcc, formation());
	}

	public boolean vereinsname() {
		return MeldelisteSpaltenDialogOption.leseVereinsname(xcc);
	}

	public Optional<MeleeAnmeldungModus> meleeAnmeldung() {
		return MeleeAnmeldungDialogOption.leseModus(xcc, formation());
	}

	/** Gegenstück zu {@link #spielbahn(String, SpielrundeSpielbahn)}. */
	public SpielrundeSpielbahn gewaehlteSpielbahn(String name) {
		short position = gewaehlt(name);
		return position >= 0 && position < SPIELBAHNEN.length ? SPIELBAHNEN[position] : SpielrundeSpielbahn.X;
	}

	/** @return gewählte Position der Listbox, 0 wenn nicht vorhanden */
	public short gewaehlt(String name) {
		XListBox listBox = control(XListBox.class, name);
		return listBox != null ? listBox.getSelectedItemPos() : 0;
	}

	public boolean istAngehakt(String name) {
		XCheckBox checkBox = control(XCheckBox.class, name);
		return checkBox != null && checkBox.getState() == 1;
	}

	public int zahlWert(String name, int standard) {
		XNumericField feld = control(XNumericField.class, name);
		return feld != null ? (int) feld.getValue() : standard;
	}

	public String textWert(String name) {
		XTextComponent feld = control(XTextComponent.class, name);
		return feld != null ? feld.getText() : "";
	}

	// ---------------------------------------------------------------
	// Hilfsmethoden
	// ---------------------------------------------------------------

	private static short formationIndex(Formation formation) {
		for (short i = 0; i < FORMATIONEN.length; i++) {
			if (FORMATIONEN[i] == formation) {
				return i;
			}
		}
		return 0;
	}

	private void label(String controlName, String text) throws com.sun.star.uno.Exception {
		XPropertySet props = neuesModel("com.sun.star.awt.UnoControlFixedTextModel", RAND_X, y + 2, LABEL_BREITE,
				LABEL_HOEHE);
		props.setPropertyValue("Label", text);
		cont.insertByName("lbl_" + controlName, props);
	}

	/** Button mit Standard-Verhalten: {@code OK}/{@code CANCEL} beenden den Dialog selbst. */
	private void button(String name, String label, int x, PushButtonType typ) throws com.sun.star.uno.Exception {
		XPropertySet props = neuesModel("com.sun.star.awt.UnoControlButtonModel", x, y, BUTTON_BREITE, BUTTON_HOEHE);
		props.setPropertyValue("Label", label);
		props.setPropertyValue("PushButtonType", (short) typ.getValue());
		cont.insertByName(name, props);
	}

	private void einfuegen(String name, XPropertySet props, @Nullable String hilfeText)
			throws com.sun.star.uno.Exception {
		if (hilfeText != null) {
			props.setPropertyValue("HelpText", hilfeText);
		}
		cont.insertByName(name, props);
	}

	private XPropertySet neuesModel(String service, int x, int posY, int breite, int hoehe)
			throws com.sun.star.uno.Exception {
		XPropertySet props = Lo.qi(XPropertySet.class, xMSF.createInstance(service));
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(posY));
		props.setPropertyValue("Width", Integer.valueOf(breite));
		props.setPropertyValue("Height", Integer.valueOf(hoehe));
		return props;
	}

	private <C> @Nullable C control(Class<C> typ, String name) {
		XControl ctrl = xcc.getControl(name);
		return ctrl == null ? null : Lo.qi(typ, ctrl);
	}
}
