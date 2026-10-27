/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XRadioButton;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XMultiServiceFactory;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Checkboxen „Zusätzliche Spalte Teamname“ und „Zusätzliche Spalte Verein je Spieler“ für die
 * Start-Dialoge der Turniersysteme mit wählbarer Meldeliste-Formation.
 * <p>
 * Beide Optionen legen fest, welche Eingabespalten die Meldeliste bekommt; die Tooltips erklären,
 * wo die Werte später erscheinen. Bei {@link Formation#NUR_TEAMNAME} ist der Teamname die einzige
 * Team-Identität: die Teamname-Checkbox wird dann angehakt und gesperrt.
 */
public final class MeldelisteSpaltenDialogOption {

	private static final Logger logger = LogManager.getLogger(MeldelisteSpaltenDialogOption.class);

	private static final String CB_TEAMNAME = "cbTeamname";
	private static final String CB_VEREINSNAME = "cbVereinsname";

	private static final int ZEILEN_ABSTAND = 14;
	private static final int CHECKBOX_HOEHE = 10;

	/** Vertikaler Platz (Dialog-Einheiten), den beide Checkboxen inklusive Abstand belegen. */
	public static final int HOEHE = 2 * ZEILEN_ABSTAND;

	private MeldelisteSpaltenDialogOption() {
	}

	/**
	 * Fügt beide Checkboxen untereinander zum Dialog-Modell hinzu.
	 *
	 * @param startFormation im Dialog vorausgewählte Formation, bestimmt den initialen Zustand der
	 *                       Teamname-Checkbox
	 */
	public static void hinzufuegen(XMultiServiceFactory xMSF, XNameContainer cont, int x, int y, int breite,
			Formation startFormation, boolean teamname, boolean vereinsname) throws com.sun.star.uno.Exception {
		boolean erzwungen = teamnameErzwungen(startFormation);
		checkBoxHinzufuegen(xMSF, cont, CB_TEAMNAME, I18n.get("dialog.meldeliste.spalte.teamname"),
				I18n.get("dialog.meldeliste.spalte.teamname.hilfe"), x, y, breite, erzwungen || teamname, !erzwungen);
		checkBoxHinzufuegen(xMSF, cont, CB_VEREINSNAME, I18n.get("dialog.meldeliste.spalte.verein"),
				I18n.get("dialog.meldeliste.spalte.verein.hilfe"), x, y + ZEILEN_ABSTAND, breite, vereinsname, true);
	}

	/**
	 * Koppelt die Teamname-Checkbox an die Formation-Auswahl (siehe Klassenbeschreibung). Unterstützt
	 * Listboxen und Radio-Buttons.
	 *
	 * @param aktuelleFormation liest die im Dialog gerade gewählte Formation
	 * @param formationControls Namen der Controls, über die die Formation gewählt wird
	 */
	public static void anFormationKoppeln(XControlContainer xcc, Supplier<Formation> aktuelleFormation,
			String... formationControls) {
		XItemListener listener = new XItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				teamnameAktualisieren(xcc, teamnameErzwungen(aktuelleFormation.get()));
			}

			@Override
			public void disposing(EventObject event) {
				// keine Ressourcen zu lösen
			}
		};
		for (String name : formationControls) {
			XControl ctrl = xcc.getControl(name);
			if (ctrl == null) {
				continue;
			}
			XListBox listBox = Lo.qi(XListBox.class, ctrl);
			if (listBox != null) {
				listBox.addItemListener(listener);
			}
			XRadioButton radio = Lo.qi(XRadioButton.class, ctrl);
			if (radio != null) {
				radio.addItemListener(listener);
			}
		}
	}

	/**
	 * @param formation die beim Bestätigen gewählte Formation
	 * @return ob die Meldeliste eine Teamname-Spalte bekommt
	 */
	public static boolean leseTeamname(XControlContainer xcc, Formation formation) {
		return teamnameErzwungen(formation) || istAngehakt(xcc, CB_TEAMNAME);
	}

	/** @return ob die Meldeliste je Spieler eine Vereinsspalte bekommt */
	public static boolean leseVereinsname(XControlContainer xcc) {
		return istAngehakt(xcc, CB_VEREINSNAME);
	}

	private static boolean teamnameErzwungen(Formation formation) {
		return formation == Formation.NUR_TEAMNAME;
	}

	private static void checkBoxHinzufuegen(XMultiServiceFactory xMSF, XNameContainer cont, String name,
			String label, String hilfeText, int x, int y, int breite, boolean angehakt, boolean enabled)
			throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlCheckBoxModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", label);
		props.setPropertyValue("HelpText", hilfeText);
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(breite));
		props.setPropertyValue("Height", Integer.valueOf(CHECKBOX_HOEHE));
		props.setPropertyValue("State", (short) (angehakt ? 1 : 0));
		props.setPropertyValue("Enabled", Boolean.valueOf(enabled));
		cont.insertByName(name, model);
	}

	private static void teamnameAktualisieren(XControlContainer xcc, boolean erzwungen) {
		XControl ctrl = xcc.getControl(CB_TEAMNAME);
		if (ctrl == null) {
			return;
		}
		if (erzwungen) {
			Lo.qi(XCheckBox.class, ctrl).setState((short) 1);
		}
		try {
			Lo.qi(XPropertySet.class, ctrl.getModel()).setPropertyValue("Enabled", Boolean.valueOf(!erzwungen));
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Teamname-Checkbox konnte nicht aktualisiert werden", e);
		}
	}

	private static boolean istAngehakt(XControlContainer xcc, String name) {
		XControl ctrl = xcc.getControl(name);
		XCheckBox checkBox = ctrl == null ? null : Lo.qi(XCheckBox.class, ctrl);
		return checkBox != null && checkBox.getState() == 1;
	}
}
