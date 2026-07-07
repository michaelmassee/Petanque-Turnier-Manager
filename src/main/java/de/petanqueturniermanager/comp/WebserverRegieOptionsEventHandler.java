/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.Arrays;
import java.util.OptionalInt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
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
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.webserver.WebServerManager;

/**
 * Event-Handler fuer die Webserver-Regie-Seite unter Extras -> Optionen.
 */
public final class WebserverRegieOptionsEventHandler extends WeakBase
		implements XServiceInfo, XContainerWindowEventHandler {

	private static final Logger logger = LogManager.getLogger(WebserverRegieOptionsEventHandler.class);

	private static final String IMPLEMENTATION_NAME = WebserverRegieOptionsEventHandler.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.WebserverRegieOptionsEventHandler";
	private static final String[] SERVICE_NAMES = { SERVICE_NAME };

	private static final String METHOD_EXTERNAL_EVENT = "external_event";
	private static final String EVENT_INITIALIZE = "initialize";
	private static final String EVENT_BACK = "back";
	private static final String EVENT_OK = "ok";

	private static final String CTL_WEBSERVER_REGIE_LABEL = "WebserverRegieLabel";
	private static final String CTL_WEBSERVER_REGIE_ACTIVE = "WebserverRegieActive";
	private static final String CTL_WEBSERVER_REGIE_PORT_LABEL = "WebserverRegiePortLabel";
	private static final String CTL_WEBSERVER_REGIE_PORT = "WebserverRegiePort";

	private final XComponentContext context;

	public WebserverRegieOptionsEventHandler(XComponentContext context) {
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
		GlobalProperties properties = GlobalProperties.get();
		setCheckbox(container, CTL_WEBSERVER_REGIE_ACTIVE, properties.isWebserverRegieAktiv());
		setText(container, CTL_WEBSERVER_REGIE_PORT, String.valueOf(properties.getWebserverRegiePort()));
	}

	private void speichereAusOberflaeche(XWindow window) {
		XControlContainer container = container(window);
		GlobalProperties properties = GlobalProperties.get();
		boolean alterRegieAktiv = properties.isWebserverRegieAktiv();
		int alterRegiePort = properties.getWebserverRegiePort();
		boolean regieAktiv = checkbox(container, CTL_WEBSERVER_REGIE_ACTIVE);

		String portText = text(container, CTL_WEBSERVER_REGIE_PORT).trim();
		OptionalInt regiePortOpt = parsePort(portText);
		if (regiePortOpt.isEmpty()) {
			zeigeFehler(I18n.get("konfig.webserver.regie.port.ungueltig", portText));
			return;
		}
		int regiePort = regiePortOpt.getAsInt();
		if (regieAktiv && istPortDurchCompositeViewBelegt(properties, regiePort)) {
			zeigeFehler(I18n.get("webserver.regie.konfig.fehler.port.duplikat", regiePort));
			return;
		}

		properties.speichernWebserverRegieOptionen(regieAktiv, regiePort);
		if (alterRegieAktiv != regieAktiv || alterRegiePort != regiePort) {
			WebServerManager.get().konfigurationGeaendert();
		}
	}

	private void zeigeFehler(String meldung) {
		MessageBox.from(context, MessageBoxTypeEnum.ERROR_OK)
				.caption(I18n.get("konfig.webserver.regie.bereich"))
				.message(meldung)
				.show();
	}

	private static boolean istPortDurchCompositeViewBelegt(GlobalProperties properties, int port) {
		return properties.getCompositeViewEintraege().stream()
				.anyMatch(eintrag -> eintrag.port() == port);
	}

	private void setzeLabels(XControlContainer container) {
		setLabel(container, CTL_WEBSERVER_REGIE_LABEL, I18n.get("konfig.webserver.regie.bereich"));
		setLabel(container, CTL_WEBSERVER_REGIE_ACTIVE, I18n.get("konfig.webserver.regie.aktiv"));
		setLabel(container, CTL_WEBSERVER_REGIE_PORT_LABEL, I18n.get("konfig.webserver.regie.port"));
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

	private static String text(XControlContainer container, String name) {
		XTextComponent text = control(container, name, XTextComponent.class);
		return text == null ? "" : text.getText();
	}

	private static OptionalInt parsePort(String portText) {
		try {
			int port = Integer.parseInt(portText);
			if (port >= 1 && port <= 65535) {
				return OptionalInt.of(port);
			}
		} catch (NumberFormatException ignored) {
			// ungueltige Eingabe -> leeres Optional, Meldung erfolgt beim Aufrufer
		}
		return OptionalInt.empty();
	}

	private static void setText(XControlContainer container, String name, String wert) {
		XTextComponent text = control(container, name, XTextComponent.class);
		if (text != null) {
			text.setText(wert == null ? "" : wert);
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
			return Factory.createComponentFactory(WebserverRegieOptionsEventHandler.class, SERVICE_NAMES);
		}
		return null;
	}
}
