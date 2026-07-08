/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.helper.farbe.FarbwahlDialog;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ko.konfiguration.KoPropertiesSpalte;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterPropertiesSpalte;

/**
 * Event-Handler fuer die Tab-Farben-Seite unter Extras -> Optionen.
 * <p>
 * Verwaltet die globalen Tab-Farben-Defaults (Sheet-Register-Farben) fuer alle Turniersysteme.
 * Diese Defaults koennen pro Turnier-Dokument weiterhin ueber den Konfigurations-Dialog
 * (FarbenDialog) im jeweiligen Dokument ueberschrieben werden.
 */
public final class TabFarbenOptionsEventHandler extends WeakBase
		implements XServiceInfo, XContainerWindowEventHandler {

	private static final Logger logger = LogManager.getLogger(TabFarbenOptionsEventHandler.class);

	private static final String IMPLEMENTATION_NAME = TabFarbenOptionsEventHandler.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.TabFarbenOptionsEventHandler";
	private static final String[] SERVICE_NAMES = { SERVICE_NAME };

	private static final String METHOD_EXTERNAL_EVENT = "external_event";
	private static final String EVENT_INITIALIZE = "initialize";
	private static final String EVENT_BACK = "back";
	private static final String EVENT_OK = "ok";

	private static final String CTL_LABEL = "TabFarbenLabel";
	private static final String CTL_LISTE = "TabFarbenListe";
	private static final String CTL_AENDERN = "TabFarbenAendern";
	private static final String CTL_STANDARDISIEREN = "TabFarbenStandardisieren";

	/** Konfig-Property-Key, Anzeigename-Key, hardcodierter Default. Reihenfolge = Anzeigereihenfolge. */
	private record TabFarbenEintrag(String konfigPropKey, String nameI18nKey, int hardcodedDefault) {
	}

	private static final List<TabFarbenEintrag> EINTRAEGE = List.of(
			new TabFarbenEintrag(BasePropertiesSpalte.KONFIG_PROP_TAB_COLOR_MELDELISTE,
					"tab.farben.name.meldeliste", SheetTabFarben.MELDELISTE),
			new TabFarbenEintrag(BasePropertiesSpalte.KONFIG_PROP_TAB_COLOR_TEILNEHMER,
					"tab.farben.name.teilnehmer", SheetTabFarben.TEILNEHMER),
			new TabFarbenEintrag(BasePropertiesSpalte.KONFIG_PROP_TAB_COLOR_SPIELRUNDE,
					"tab.farben.name.spielrunde", SheetTabFarben.SPIELRUNDE),
			new TabFarbenEintrag(BasePropertiesSpalte.KONFIG_PROP_TAB_COLOR_RANGLISTE,
					"tab.farben.name.rangliste", SheetTabFarben.RANGLISTE),
			new TabFarbenEintrag(BasePropertiesSpalte.KONFIG_PROP_TAB_COLOR_DIREKTVERGLEICH,
					"tab.farben.name.direktvergleich", SheetTabFarben.DIREKTVERGLEICH),
			new TabFarbenEintrag(KoPropertiesSpalte.KONFIG_PROP_TAB_COLOR_KO_TURNIERBAUM,
					"tab.farben.name.ko.turnierbaum", SheetTabFarben.KO_TURNIERBAUM),
			new TabFarbenEintrag(MaastrichterPropertiesSpalte.KONFIG_PROP_TAB_COLOR_CADRAGE,
					"tab.farben.name.cadrage", SheetTabFarben.FORME_CADRAGE),
			new TabFarbenEintrag("Tab-Farbe Poule-Vorrunde",
					"tab.farben.name.poule.vorrunde", SheetTabFarben.POULE_VORRUNDE),
			new TabFarbenEintrag("Tab-Farbe Poule-Vorrunden-Rangliste",
					"tab.farben.name.poule.vorrunden.rangliste", SheetTabFarben.POULE_VORRUNDEN_RANGLISTE),
			new TabFarbenEintrag("Tab-Farbe Kaskaden-KO",
					"tab.farben.name.kaskaden.ko", SheetTabFarben.KO_TURNIERBAUM),
			new TabFarbenEintrag("Tab-Farbe Team-Paarungen",
					"tab.farben.name.supermelee.team.paarungen", SheetTabFarben.SUPERMELEE_TEAM_PAARUNGEN));

	private final XComponentContext context;

	/** Container, an dessen Buttons bereits Listener haengen (verhindert Doppelregistrierung). */
	private XControlContainer listenerContainer;

	/** Peer der Optionsseite, als Parent fuer den modalen Farbwahl-Dialog. */
	private XWindowPeer pagePeer;

	public TabFarbenOptionsEventHandler(XComponentContext context) {
		this.context = context;
		GlobalProperties.setLibreOfficeContext(context);
	}

	@Override
	public boolean callHandlerMethod(XWindow window, Object eventObject, String method)
			throws WrappedTargetException {
		if (!METHOD_EXTERNAL_EVENT.equals(method)) {
			return true;
		}
		try {
			String event = AnyConverter.toString(eventObject);
			if (EVENT_INITIALIZE.equals(event) || EVENT_BACK.equals(event)) {
				ladeInOberflaeche(window);
			}
			return true;
		} catch (Exception e) {
			throw new WrappedTargetException(e, method, this, e);
		}
	}

	@Override
	public String[] getSupportedMethodNames() {
		return new String[] { METHOD_EXTERNAL_EVENT };
	}

	private void ladeInOberflaeche(XWindow window) {
		XControlContainer container = container(window);
		pagePeer = windowPeer(window);
		setLabel(container, CTL_LABEL, I18n.get("tab.farben.konfig.bereich"));
		setLabel(container, CTL_AENDERN, I18n.get("tab.farben.konfig.btn.aendern"));
		setLabel(container, CTL_STANDARDISIEREN, I18n.get("tab.farben.konfig.btn.standardisieren"));
		aktualisiereListe(container);
		registriereListener(container);
	}

	private void registriereListener(XControlContainer container) {
		if (listenerContainer != null && UnoRuntime.areSame(listenerContainer, container)) {
			return;
		}
		registriereActionListener(container, CTL_AENDERN, () -> farbeAendern(container));
		registriereActionListener(container, CTL_STANDARDISIEREN, () -> aufStandardZuruecksetzen(container));
		listenerContainer = container;
	}

	// ---- Aktionen ----

	private void farbeAendern(XControlContainer container) {
		var eintrag = ausgewaehlterEintrag(container);
		if (eintrag == null) {
			return;
		}
		int aktuelleFarbe = GlobalProperties.get().getTabFarbe(eintrag.konfigPropKey(), eintrag.hardcodedDefault());
		OptionalInt neueFarbe = FarbwahlDialog.waehle(context, pagePeer, aktuelleFarbe);
		if (neueFarbe.isPresent()) {
			GlobalProperties.get().setzeTabFarbe(eintrag.konfigPropKey(), neueFarbe.getAsInt());
			aktualisiereListe(container);
		}
	}

	private void aufStandardZuruecksetzen(XControlContainer container) {
		var eintrag = ausgewaehlterEintrag(container);
		if (eintrag == null) {
			return;
		}
		GlobalProperties.get().setzeTabFarbe(eintrag.konfigPropKey(), eintrag.hardcodedDefault());
		aktualisiereListe(container);
	}

	private TabFarbenEintrag ausgewaehlterEintrag(XControlContainer container) {
		int idx = selectedPos(container, CTL_LISTE);
		if (idx < 0 || idx >= EINTRAEGE.size()) {
			zeigeFehler(I18n.get("tab.farben.konfig.fehler.keine.auswahl"));
			return null;
		}
		return EINTRAEGE.get(idx);
	}

	private void zeigeFehler(String meldung) {
		MessageBox.from(context, MessageBoxTypeEnum.ERROR_OK)
				.caption(I18n.get("tab.farben.konfig.fehler.titel"))
				.message(meldung)
				.show();
	}

	// ---- Hilfsmethoden ----

	private void aktualisiereListe(XControlContainer container) {
		String[] items = EINTRAEGE.stream()
				.map(e -> formatiereZeile(e, GlobalProperties.get().getTabFarbe(e.konfigPropKey(), e.hardcodedDefault())))
				.toArray(String[]::new);
		setListItems(container, CTL_LISTE, items);
	}

	private static String formatiereZeile(TabFarbenEintrag eintrag, int farbe) {
		return I18n.get("tab.farben.konfig.liste.zeile", I18n.get(eintrag.nameI18nKey()),
				String.format("%06X", farbe & 0xFFFFFF));
	}

	// ---- UNO-Control-Hilfsmethoden ----

	private static XControlContainer container(XWindow window) {
		XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
		if (container == null) {
			throw new IllegalStateException("Optionsseite hat kein XControlContainer");
		}
		return container;
	}

	/**
	 * Peer der Optionsseite, als Parent fuer den modalen Farbwahl-Dialog. Das an
	 * {@code callHandlerMethod} uebergebene {@code window} ist selbst kein {@link XWindowPeer},
	 * sondern ein {@link XControl}, dessen {@code getPeer()} den echten Fenster-Peer liefert
	 * (siehe FtpServerOptionsEventHandler.windowPeer()).
	 */
	private static XWindowPeer windowPeer(XWindow window) {
		XControl control = UnoRuntime.queryInterface(XControl.class, window);
		return control == null ? null : control.getPeer();
	}

	private static short selectedPos(XControlContainer container, String name) {
		XListBox listBox = control(container, name, XListBox.class);
		return listBox == null ? -1 : listBox.getSelectedItemPos();
	}

	private static void setListItems(XControlContainer container, String name, String[] items) {
		XControl control = container.getControl(name);
		if (control == null) {
			return;
		}
		XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, control.getModel());
		if (props == null) {
			return;
		}
		try {
			props.setPropertyValue("StringItemList", items);
		} catch (Exception e) {
			logger.debug("StringItemList fuer Control {} konnte nicht gesetzt werden", name, e);
		}
	}

	private static void setLabel(XControlContainer container, String name, String label) {
		XControl control = container.getControl(name);
		if (control == null) {
			return;
		}
		XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, control.getModel());
		if (props == null) {
			return;
		}
		try {
			props.setPropertyValue("Label", label);
		} catch (Exception e) {
			logger.debug("Label fuer Control {} konnte nicht gesetzt werden", name, e);
		}
	}

	private static void registriereActionListener(XControlContainer container, String name, Runnable aktion) {
		XButton button = control(container, name, XButton.class);
		if (button == null) {
			return;
		}
		button.addActionListener(new XActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				aktion.run();
			}

			@Override
			public void disposing(EventObject event) {
				// nichts zu tun
			}
		});
	}

	private static <T> T control(XControlContainer container, String name, Class<T> type) {
		XControl control = container.getControl(name);
		return control == null ? null : UnoRuntime.queryInterface(type, control);
	}

	@Override
	public String getImplementationName() {
		return IMPLEMENTATION_NAME;
	}

	@Override
	public boolean supportsService(String name) {
		return Arrays.asList(SERVICE_NAMES).contains(name);
	}

	@Override
	public String[] getSupportedServiceNames() {
		return SERVICE_NAMES;
	}

	public static boolean __writeRegistryServiceInfo(XRegistryKey registryKey) {
		return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME, SERVICE_NAMES, registryKey);
	}

	public static XSingleComponentFactory __getComponentFactory(String implementationName) {
		if (IMPLEMENTATION_NAME.equals(implementationName)) {
			return Factory.createComponentFactory(TabFarbenOptionsEventHandler.class, SERVICE_NAMES);
		}
		return null;
	}
}
