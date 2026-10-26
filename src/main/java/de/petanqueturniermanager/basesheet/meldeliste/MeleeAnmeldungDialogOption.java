/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ItemEvent;
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
 * Auswahl „Mêlée-Anmeldung“ (Listbox) für die Start-Dialoge der Turniersysteme mit wählbarer
 * Meldeliste-Formation. Einträge: „Keine“ gefolgt von allen {@link MeleeAnmeldungModus}-Werten.
 * <p>
 * Kopplung an die Formation-Auswahl des Dialogs:
 * <ul>
 * <li>Formation ohne Mêlée-Anmeldung ({@link Formation#erlaubtMeleeAnmeldung()} falsch): Listbox
 * deaktiviert, Auswahl „Keine“.</li>
 * <li>Gemischter Modus („ohne Freilos“) gewählt: die Formation wird auf {@link Formation#TRIPLETTE}
 * umgestellt, da gemischte Teams drei Spielerspalten brauchen.</li>
 * <li>Formation weg von Triplette bei gemischtem Modus: Rückfall auf {@link MeleeAnmeldungModus#EINFACH}.</li>
 * </ul>
 */
public final class MeleeAnmeldungDialogOption {

	private static final Logger logger = LogManager.getLogger(MeleeAnmeldungDialogOption.class);

	/** Control-Name der Listbox im Dialog-Modell. */
	public static final String CONTROL_NAME = "lstMeleeAnmeldung";

	private static final String LABEL_NAME = "lblMeleeAnmeldung";

	/** Vertikaler Platz (Dialog-Einheiten), den die Auswahl inklusive Abstand belegt. */
	public static final int HOEHE = 14;

	private static final int LISTBOX_HOEHE = 12;
	private static final int LABEL_HOEHE = 10;
	private static final int ABSTAND_LABEL_LISTBOX = 2;

	/** Listbox-Position „Keine“; die Modi folgen in {@link MeleeAnmeldungModus#values()}-Reihenfolge. */
	private static final short POS_KEINE = 0;

	private MeleeAnmeldungDialogOption() {
	}

	/**
	 * Fügt Label und Listbox (Auswahl „Keine“) zum Dialog-Modell hinzu.
	 *
	 * @param labelBreite    Breite des Labels; die Listbox beginnt dahinter mit kleinem Abstand und ist
	 *                       so an den übrigen Listboxen des Dialogs ausgerichtet
	 * @param startFormation im Dialog vorausgewählte Formation, bestimmt den initialen Aktiv-Zustand
	 */
	public static void hinzufuegen(XMultiServiceFactory xMSF, XNameContainer cont, int x, int y, int breite,
			int labelBreite, Formation startFormation) throws com.sun.star.uno.Exception {
		Object labelModel = xMSF.createInstance("com.sun.star.awt.UnoControlFixedTextModel");
		XPropertySet labelProps = Lo.qi(XPropertySet.class, labelModel);
		labelProps.setPropertyValue("Label", I18n.get("dialog.melee.anmeldung.label"));
		labelProps.setPropertyValue("PositionX", Integer.valueOf(x));
		labelProps.setPropertyValue("PositionY", Integer.valueOf(y + 1));
		labelProps.setPropertyValue("Width", Integer.valueOf(labelBreite));
		labelProps.setPropertyValue("Height", Integer.valueOf(LABEL_HOEHE));
		cont.insertByName(LABEL_NAME, labelModel);

		int listBoxX = x + labelBreite + ABSTAND_LABEL_LISTBOX;
		Object model = xMSF.createInstance("com.sun.star.awt.UnoControlListBoxModel");
		XPropertySet props = Lo.qi(XPropertySet.class, model);
		props.setPropertyValue("HelpText", I18n.get("dialog.melee.anmeldung.hilfe"));
		props.setPropertyValue("PositionX", Integer.valueOf(listBoxX));
		props.setPropertyValue("PositionY", Integer.valueOf(y));
		props.setPropertyValue("Width", Integer.valueOf(breite - labelBreite - ABSTAND_LABEL_LISTBOX));
		props.setPropertyValue("Height", Integer.valueOf(LISTBOX_HOEHE));
		props.setPropertyValue("Dropdown", Boolean.TRUE);
		props.setPropertyValue("StringItemList", eintraege());
		props.setPropertyValue("SelectedItems", new short[] { POS_KEINE });
		props.setPropertyValue("Enabled", Boolean.valueOf(startFormation.erlaubtMeleeAnmeldung()));
		cont.insertByName(CONTROL_NAME, model);
	}

	private static String[] eintraege() {
		MeleeAnmeldungModus[] modi = MeleeAnmeldungModus.values();
		String[] eintraege = new String[modi.length + 1];
		eintraege[POS_KEINE] = I18n.get("dialog.melee.anmeldung.keine");
		for (MeleeAnmeldungModus modus : modi) {
			eintraege[position(modus)] = modus.getBezeichnung();
		}
		return eintraege;
	}

	/**
	 * Koppelt die Mêlée-Auswahl an die Formation-Auswahl des Dialogs (siehe Klassenbeschreibung).
	 * Unterstützt Listboxen und Radio-Buttons; nach jeder Änderung an einem der Controls wird die
	 * aktuelle Formation über {@code aktuelleFormation} neu gelesen.
	 *
	 * @param aktuelleFormation liest die im Dialog gerade gewählte Formation
	 * @param formationSetzen   stellt die Formation-Auswahl des Dialogs um
	 * @param formationControls Namen der Controls, über die die Formation gewählt wird
	 */
	public static void anFormationKoppeln(XControlContainer xcc, Supplier<Formation> aktuelleFormation,
			Consumer<Formation> formationSetzen, String... formationControls) {
		XItemListener formationListener = new ItemListenerAdapter(
				() -> formationGeaendert(xcc, aktuelleFormation.get()));
		for (String name : formationControls) {
			XControl ctrl = xcc.getControl(name);
			if (ctrl == null) {
				continue;
			}
			XListBox listBox = Lo.qi(XListBox.class, ctrl);
			if (listBox != null) {
				listBox.addItemListener(formationListener);
			}
			XRadioButton radio = Lo.qi(XRadioButton.class, ctrl);
			if (radio != null) {
				radio.addItemListener(formationListener);
			}
		}
		XListBox modusListBox = modusListBox(xcc);
		if (modusListBox != null) {
			modusListBox.addItemListener(new ItemListenerAdapter(
					() -> modusGeaendert(xcc, aktuelleFormation.get(), formationSetzen)));
		}
	}

	/**
	 * Wählt in einer Formation-Listbox die übergebene Position aus – Hilfe für
	 * {@code formationSetzen} in Dialogen mit Listbox-Formation.
	 */
	public static void listBoxPositionWaehlen(XControlContainer xcc, String controlName, short position) {
		XControl ctrl = xcc.getControl(controlName);
		XListBox listBox = ctrl == null ? null : Lo.qi(XListBox.class, ctrl);
		if (listBox != null) {
			listBox.selectItemPos(position, true);
		}
	}

	/**
	 * @param formation die beim Bestätigen gewählte Formation
	 * @return gewählter Modus; leer bei „Keine“ oder wenn der Modus nicht zur Formation passt
	 */
	public static Optional<MeleeAnmeldungModus> leseModus(XControlContainer xcc, Formation formation) {
		return gewaehlterModus(xcc).filter(modus -> modus.passtZu(formation));
	}

	private static void formationGeaendert(XControlContainer xcc, Formation formation) {
		boolean moeglich = formation.erlaubtMeleeAnmeldung();
		setzeEnabled(xcc, moeglich);
		if (!moeglich) {
			waehleModusPosition(xcc, POS_KEINE);
			return;
		}
		gewaehlterModus(xcc).filter(modus -> !modus.passtZu(formation))
				.ifPresent(modus -> waehleModusPosition(xcc, position(MeleeAnmeldungModus.EINFACH)));
	}

	private static void modusGeaendert(XControlContainer xcc, Formation formation,
			Consumer<Formation> formationSetzen) {
		gewaehlterModus(xcc).filter(modus -> !modus.passtZu(formation))
				.ifPresent(modus -> formationSetzen.accept(Formation.TRIPLETTE));
	}

	private static Optional<MeleeAnmeldungModus> gewaehlterModus(XControlContainer xcc) {
		XListBox listBox = modusListBox(xcc);
		if (listBox == null) {
			return Optional.empty();
		}
		int position = listBox.getSelectedItemPos();
		MeleeAnmeldungModus[] modi = MeleeAnmeldungModus.values();
		if (position <= POS_KEINE || position > modi.length) {
			return Optional.empty();
		}
		return Optional.of(modi[position - 1]);
	}

	private static short position(MeleeAnmeldungModus modus) {
		return (short) (modus.ordinal() + 1);
	}

	private static void waehleModusPosition(XControlContainer xcc, short position) {
		XListBox listBox = modusListBox(xcc);
		if (listBox != null) {
			listBox.selectItemPos(position, true);
		}
	}

	private static void setzeEnabled(XControlContainer xcc, boolean enabled) {
		XControl ctrl = xcc.getControl(CONTROL_NAME);
		if (ctrl == null) {
			return;
		}
		try {
			Lo.qi(XPropertySet.class, ctrl.getModel()).setPropertyValue("Enabled", Boolean.valueOf(enabled));
		} catch (com.sun.star.uno.Exception e) {
			logger.error("Mêlée-Anmeldung-Auswahl konnte nicht aktualisiert werden", e);
		}
	}

	private static XListBox modusListBox(XControlContainer xcc) {
		XControl ctrl = xcc.getControl(CONTROL_NAME);
		return ctrl == null ? null : Lo.qi(XListBox.class, ctrl);
	}

	/** Leitet jede Auswahländerung an eine Aktion weiter. */
	private static final class ItemListenerAdapter implements XItemListener {

		private final Runnable aktion;

		ItemListenerAdapter(Runnable aktion) {
			this.aktion = aktion;
		}

		@Override
		public void itemStateChanged(ItemEvent event) {
			aktion.run();
		}

		@Override
		public void disposing(EventObject event) {
			// keine Ressourcen zu lösen
		}
	}
}
