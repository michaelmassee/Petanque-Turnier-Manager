/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
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
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.helper.farbe.FarbwahlDialog;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.ko.konfiguration.KoPropertiesSpalte;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterPropertiesSpalte;

/**
 * Event-Handler fuer die Tab-Farben-Seite unter Extras -> Optionen.
 * <p>
 * Verwaltet die globalen Tab-Farben-Defaults (Sheet-Register-Farben) fuer alle Turniersysteme.
 * Layout identisch zum dokumentweiten Konfiguration-&gt;Farben-Dialog
 * ({@code LabelPlusBackgrColorAndColorChooser}): pro Eintrag ein Name-Label, eine Farbflaeche
 * und ein Button mit dem Farbwahl-Icon. Tab-Farben sind ausschliesslich hier, ueber
 * Extras -&gt; Optionen -&gt; PétTurnMngr, konfigurierbar; ein dokumentweiter Override existiert
 * nicht (der Farben-Dialog im Dokument blendet Tab-Farb-Properties bewusst aus).
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
	private static final String CTL_NAME_PRAEFIX = "TabFarbenName";
	private static final String CTL_SWATCH_PRAEFIX = "TabFarbenSwatch";
	private static final String CTL_BTN_PRAEFIX = "TabFarbenBtn";

	/** Selbes Icon wie im dokumentweiten Konfiguration-&gt;Farben-Dialog (LabelPlusBackgrColorAndColorChooser). */
	private static final String FARBWAHL_ICON = "konfig/colorpicker.png";

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

	/** Arbeitskopie der Tab-Farben (konfigPropKey -&gt; Farbe); {@code null} bis zum ersten Laden. */
	private Map<String, Integer> pufferFarben;

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
			} else if (EVENT_OK.equals(event)) {
				speichereAusOberflaeche();
			}
			return true;
		} catch (Exception e) {
			logger.error("TabFarbenOptionsEventHandler.callHandlerMethod: Fehler bei method={}", method, e);
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
		if (pufferFarben == null) {
			pufferFarben = new LinkedHashMap<>();
			for (var eintrag : EINTRAEGE) {
				pufferFarben.put(eintrag.konfigPropKey(),
						GlobalProperties.get().getTabFarbe(eintrag.konfigPropKey(), eintrag.hardcodedDefault()));
			}
		}
		setLabel(container, CTL_LABEL, I18n.get("tab.farben.konfig.bereich"));
		String iconUrl = ExtensionsHelper.from(context).getImageUrlDir() + FARBWAHL_ICON;
		for (int i = 0; i < EINTRAEGE.size(); i++) {
			int zeile = i + 1;
			setLabel(container, CTL_NAME_PRAEFIX + zeile, I18n.get(EINTRAEGE.get(i).nameI18nKey()) + ":");
			setImageUrl(container, CTL_BTN_PRAEFIX + zeile, iconUrl);
			aktualisiereSwatch(container, i);
		}
		registriereListener(container);
	}

	private void speichereAusOberflaeche() {
		if (pufferFarben != null) {
			GlobalProperties.get().setzeTabFarben(pufferFarben);
		}
	}

	private void registriereListener(XControlContainer container) {
		if (listenerContainer != null && UnoRuntime.areSame(listenerContainer, container)) {
			return;
		}
		for (int i = 0; i < EINTRAEGE.size(); i++) {
			int index = i;
			registriereActionListener(container, CTL_BTN_PRAEFIX + (i + 1), () -> farbeAendern(container, index));
		}
		listenerContainer = container;
	}

	// ---- Aktionen ----

	private void farbeAendern(XControlContainer container, int index) {
		var eintrag = EINTRAEGE.get(index);
		int aktuelleFarbe = pufferFarben.get(eintrag.konfigPropKey());
		OptionalInt neueFarbe = FarbwahlDialog.waehle(context, pagePeer, aktuelleFarbe);
		if (neueFarbe.isPresent()) {
			pufferFarben.put(eintrag.konfigPropKey(), neueFarbe.getAsInt());
			aktualisiereSwatch(container, index);
		}
	}

	// ---- Hilfsmethoden ----

	private void aktualisiereSwatch(XControlContainer container, int index) {
		var eintrag = EINTRAEGE.get(index);
		int farbe = pufferFarben.get(eintrag.konfigPropKey());
		setBackgroundColor(container, CTL_SWATCH_PRAEFIX + (index + 1), farbe);
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

	private static void setBackgroundColor(XControlContainer container, String name, int farbe) {
		XPropertySet props = modelProperties(container, name);
		if (props == null) {
			return;
		}
		try {
			props.setPropertyValue("BackgroundColor", farbe & 0xFFFFFF);
		} catch (Exception e) {
			logger.debug("BackgroundColor fuer Control {} konnte nicht gesetzt werden", name, e);
		}
	}

	private static void setImageUrl(XControlContainer container, String name, String imageUrl) {
		XPropertySet props = modelProperties(container, name);
		if (props == null) {
			return;
		}
		try {
			props.setPropertyValue("ImageURL", imageUrl);
		} catch (Exception e) {
			logger.debug("ImageURL fuer Control {} konnte nicht gesetzt werden", name, e);
		}
	}

	private static void setLabel(XControlContainer container, String name, String label) {
		XPropertySet props = modelProperties(container, name);
		if (props == null) {
			return;
		}
		try {
			props.setPropertyValue("Label", label);
		} catch (Exception e) {
			logger.debug("Label fuer Control {} konnte nicht gesetzt werden", name, e);
		}
	}

	private static XPropertySet modelProperties(XControlContainer container, String name) {
		XControl control = container.getControl(name);
		if (control == null) {
			return null;
		}
		return UnoRuntime.queryInterface(XPropertySet.class, control.getModel());
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
