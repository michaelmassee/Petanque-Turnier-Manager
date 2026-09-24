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
 * Checkbox „Mêlée-Anmeldung“ für die Start-Dialoge der Turniersysteme mit wählbarer
 * Meldeliste-Formation.
 * <p>
 * Die Checkbox ist nur bei Formationen bedienbar, die eine Mêlée-Anmeldung fachlich zulassen
 * ({@link Formation#erlaubtMeleeAnmeldung()}). Wird im Dialog die Formation umgestellt, folgt der
 * Aktiv-Zustand der Checkbox live; beim Wechsel auf eine unzulässige Formation wird ein gesetzter
 * Haken zurückgenommen.
 */
public final class MeleeAnmeldungDialogOption {

	private static final Logger logger = LogManager.getLogger(MeleeAnmeldungDialogOption.class);

	/** Control-Name der Checkbox im Dialog-Modell. */
	public static final String CONTROL_NAME = "cbMeleeAnmeldung";

	/** Vertikaler Platz (Dialog-Einheiten), den die Checkbox inklusive Abstand belegt. */
	public static final int HOEHE = 14;

	private static final int CHECKBOX_HOEHE = 10;

	private MeleeAnmeldungDialogOption() {
	}

	/**
	 * Fügt die Checkbox (nicht angehakt) zum Dialog-Modell hinzu.
	 *
	 * @param startFormation im Dialog vorausgewählte Formation, bestimmt den initialen Aktiv-Zustand
	 */
	public static void hinzufuegen(XMultiServiceFactory xMSF, XNameContainer cont, int x, int y, int breite,
			Formation startFormation) throws com.sun.star.uno.Exception {
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlCheckBoxModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("Label", I18n.get("dialog.melee.anmeldung.label"));
		props.setPropertyValue("HelpText", I18n.get("dialog.melee.anmeldung.hilfe"));
		props.setPropertyValue("PositionX", Integer.valueOf(x));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(breite));
		props.setPropertyValue("Height", Integer.valueOf(CHECKBOX_HOEHE));
		props.setPropertyValue("State", (short) 0);
		props.setPropertyValue("Enabled", Boolean.valueOf(startFormation.erlaubtMeleeAnmeldung()));
		cont.insertByName(CONTROL_NAME, model);
	}

	/**
	 * Koppelt den Aktiv-Zustand der Checkbox an die Formation-Auswahl des Dialogs. Unterstützt
	 * Listboxen und Radio-Buttons; nach jeder Änderung an einem der Controls wird die aktuelle
	 * Formation über {@code aktuelleFormation} neu gelesen.
	 *
	 * @param aktuelleFormation liest die im Dialog gerade gewählte Formation
	 * @param formationControls Namen der Controls, über die die Formation gewählt wird
	 */
	public static void anFormationKoppeln(XControlContainer xcc, Supplier<Formation> aktuelleFormation,
			String... formationControls) {
		XItemListener listener = new XItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				aktualisieren(xcc, aktuelleFormation.get());
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
	 * @return {@code true} wenn die Checkbox angehakt ist und die Formation eine Mêlée-Anmeldung zulässt
	 */
	public static boolean istGewaehlt(XControlContainer xcc, Formation formation) {
		if (!formation.erlaubtMeleeAnmeldung()) {
			return false;
		}
		XCheckBox checkBox = checkBox(xcc);
		return checkBox != null && checkBox.getState() == 1;
	}

	private static void aktualisieren(XControlContainer xcc, Formation formation) {
		XControl ctrl = xcc.getControl(CONTROL_NAME);
		if (ctrl == null) {
			return;
		}
		boolean moeglich = formation.erlaubtMeleeAnmeldung();
		try {
			XPropertySet props = Lo.qi(XPropertySet.class, ctrl.getModel());
			props.setPropertyValue("Enabled", Boolean.valueOf(moeglich));
			if (!moeglich) {
				props.setPropertyValue("State", (short) 0);
			}
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Mêlée-Anmeldung-Checkbox konnte nicht aktualisiert werden", e);
		}
	}

	private static XCheckBox checkBox(XControlContainer xcc) {
		XControl ctrl = xcc.getControl(CONTROL_NAME);
		return ctrl == null ? null : Lo.qi(XCheckBox.class, ctrl);
	}
}
