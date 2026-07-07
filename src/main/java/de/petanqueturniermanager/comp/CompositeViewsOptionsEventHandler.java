/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XWindow;
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

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.webserver.CompositeViewListeDialog;
import de.petanqueturniermanager.webserver.WebServerManager;

/**
 * Event-Handler fuer die Composite-Views-Seite unter Extras -&gt; Optionen.
 * <p>
 * Die Seite verwaltet nur das globale Webserver-Flag und den Einstieg in den
 * Composite-Views-Verwaltungs-Dialog; die eigentliche (dynamische) View-Konfiguration
 * bleibt im modalen {@link CompositeViewListeDialog}.
 */
public final class CompositeViewsOptionsEventHandler extends WeakBase
		implements XServiceInfo, XContainerWindowEventHandler {

	private static final Logger logger = LogManager.getLogger(CompositeViewsOptionsEventHandler.class);

	private static final String IMPLEMENTATION_NAME = CompositeViewsOptionsEventHandler.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.CompositeViewsOptionsEventHandler";
	private static final String[] SERVICE_NAMES = { SERVICE_NAME };

	private static final String METHOD_EXTERNAL_EVENT = "external_event";
	private static final String EVENT_INITIALIZE = "initialize";
	private static final String EVENT_BACK = "back";
	private static final String EVENT_OK = "ok";

	private static final String CTL_COMPOSITE_VIEWS_LABEL = "CompositeViewsLabel";
	private static final String CTL_WEBSERVER_AKTIV = "WebserverAktiv";
	private static final String CTL_COMPOSITE_VIEWS_STATUS = "CompositeViewsStatus";
	private static final String CTL_VERWALTEN = "CompositeViewsVerwalten";

	private final XComponentContext context;

	/** Container, an dessen „Verwalten"-Button bereits ein Listener haengt (verhindert Doppelregistrierung). */
	private XControlContainer verwaltenListenerContainer;

	public CompositeViewsOptionsEventHandler(XComponentContext context) {
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
				speichereAusOberflaeche(window);
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
		setzeLabels(container);
		setCheckbox(container, CTL_WEBSERVER_AKTIV, GlobalProperties.get().isWebserverAktiv());
		aktualisiereStatus(container);
		registriereVerwaltenListener(container);
	}

	private void speichereAusOberflaeche(XWindow window) {
		XControlContainer container = container(window);
		GlobalProperties properties = GlobalProperties.get();
		boolean alterWert = properties.isWebserverAktiv();
		boolean neuerWert = checkbox(container, CTL_WEBSERVER_AKTIV);
		properties.speichernWebserverAktiv(neuerWert);
		if (alterWert != neuerWert) {
			WebServerManager.get().konfigurationGeaendert();
		}
	}

	private void setzeLabels(XControlContainer container) {
		setLabel(container, CTL_COMPOSITE_VIEWS_LABEL, I18n.get("konfig.webserver.views.bereich"));
		setLabel(container, CTL_WEBSERVER_AKTIV, I18n.get("konfig.webserver.views.aktiv"));
		setLabel(container, CTL_VERWALTEN, I18n.get("konfig.webserver.views.verwalten"));
	}

	private void aktualisiereStatus(XControlContainer container) {
		int anzahl = GlobalProperties.get().getCompositeViewEintraege().size();
		setLabel(container, CTL_COMPOSITE_VIEWS_STATUS, I18n.get("konfig.webserver.views.status", anzahl));
	}

	private void registriereVerwaltenListener(XControlContainer container) {
		if (verwaltenListenerContainer != null && UnoRuntime.areSame(verwaltenListenerContainer, container)) {
			return;
		}
		XButton button = control(container, CTL_VERWALTEN, XButton.class);
		if (button == null) {
			return;
		}
		button.addActionListener(new XActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				oeffneVerwaltung(container);
			}

			@Override
			public void disposing(EventObject event) {
				// nichts zu tun
			}
		});
		verwaltenListenerContainer = container;
	}

	private void oeffneVerwaltung(XControlContainer container) {
		try {
			new CompositeViewListeDialog(context).zeigen(null);
		} catch (Exception e) {
			logger.error("Composite-Views-Verwaltungsdialog konnte nicht geoeffnet werden", e);
		}
		aktualisiereStatus(container);
	}

	private static XControlContainer container(XWindow window) {
		XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
		if (container == null) {
			throw new IllegalStateException("Optionsseite hat kein XControlContainer");
		}
		return container;
	}

	private static boolean checkbox(XControlContainer container, String name) {
		XCheckBox checkBox = control(container, name, XCheckBox.class);
		return checkBox != null && checkBox.getState() == 1;
	}

	private static void setCheckbox(XControlContainer container, String name, boolean wert) {
		XCheckBox checkBox = control(container, name, XCheckBox.class);
		if (checkBox != null) {
			checkBox.setState((short) (wert ? 1 : 0));
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
			return Factory.createComponentFactory(CompositeViewsOptionsEventHandler.class, SERVICE_NAMES);
		}
		return null;
	}
}
