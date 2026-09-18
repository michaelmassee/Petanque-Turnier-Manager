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
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
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

import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.PtmOnlineApiClient;
import de.petanqueturniermanager.onlinesync.PtmOnlineException;

/**
 * Event-Handler fuer die "PTM Online"-Seite unter Extras -&gt; Optionen.
 * <p>
 * Verwaltet API-Key und Basis-URL der PTM-Online-Anbindung (siehe {@link LibreOfficePtmOnlineSpeicher}) sowie
 * "Verbindung testen" und "Verbindung trennen".
 */
public final class PtmOnlineOptionsEventHandler extends WeakBase implements XServiceInfo, XContainerWindowEventHandler {

	private static final Logger logger = LogManager.getLogger(PtmOnlineOptionsEventHandler.class);

	private static final String IMPLEMENTATION_NAME = PtmOnlineOptionsEventHandler.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.PtmOnlineOptionsEventHandler";
	private static final String[] SERVICE_NAMES = { SERVICE_NAME };

	private static final String METHOD_EXTERNAL_EVENT = "external_event";
	private static final String EVENT_INITIALIZE = "initialize";
	private static final String EVENT_BACK = "back";
	private static final String EVENT_OK = "ok";

	private static final String CTL_LABEL = "PtmOnlineLabel";
	private static final String CTL_API_KEY_LABEL = "PtmOnlineApiKeyLabel";
	private static final String CTL_API_KEY_FELD = "PtmOnlineApiKeyFeld";
	private static final String CTL_BASE_URL_LABEL = "PtmOnlineBaseUrlLabel";
	private static final String CTL_BASE_URL_FELD = "PtmOnlineBaseUrlFeld";
	private static final String CTL_VERBINDUNG_TESTEN = "PtmOnlineVerbindungTesten";
	private static final String CTL_TRENNEN = "PtmOnlineTrennen";
	private static final String CTL_STATUS = "PtmOnlineStatus";

	private final XComponentContext context;

	/** Container, an dessen Buttons bereits Listener haengen (verhindert Doppelregistrierung). */
	private XControlContainer listenerContainer;

	public PtmOnlineOptionsEventHandler(XComponentContext context) {
		this.context = context;
	}

	@Override
	public boolean callHandlerMethod(XWindow window, Object eventObject, String method) throws WrappedTargetException {
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
		setLabel(container, CTL_LABEL, I18n.get("ptmonline.optionen.bereich"));
		setLabel(container, CTL_API_KEY_LABEL, I18n.get("ptmonline.optionen.label.apikey"));
		setLabel(container, CTL_BASE_URL_LABEL, I18n.get("ptmonline.optionen.label.baseurl"));
		setLabel(container, CTL_VERBINDUNG_TESTEN, I18n.get("ptmonline.optionen.btn.testen"));
		setLabel(container, CTL_TRENNEN, I18n.get("ptmonline.optionen.btn.trennen"));

		var zugangsdaten = new LibreOfficePtmOnlineSpeicher(context).laden();
		setText(container, CTL_API_KEY_FELD, zugangsdaten.apiKey());
		setText(container, CTL_BASE_URL_FELD, zugangsdaten.baseUrl());
		aktualisiereStatus(container, zugangsdaten.isConfigured()
				? I18n.get("ptmonline.optionen.status.verbunden")
				: I18n.get("ptmonline.optionen.status.getrennt"));

		registriereListener(container);
	}

	private void speichereAusOberflaeche(XWindow window) {
		XControlContainer container = container(window);
		String apiKey = getText(container, CTL_API_KEY_FELD);
		String baseUrl = getText(container, CTL_BASE_URL_FELD);
		new LibreOfficePtmOnlineSpeicher(context).speichern(apiKey, baseUrl);
	}

	private void registriereListener(XControlContainer container) {
		if (listenerContainer != null && UnoRuntime.areSame(listenerContainer, container)) {
			return;
		}
		registriereActionListener(container, CTL_VERBINDUNG_TESTEN, () -> verbindungTesten(container));
		registriereActionListener(container, CTL_TRENNEN, () -> verbindungTrennen(container));
		listenerContainer = container;
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

	/**
	 * Netzwerk-I/O darf den LO-Main-Thread nicht blockieren, daher off-thread; das Ergebnis wird per
	 * {@link LoMainThread#post} zurueck auf den Main-Thread marshalliert, bevor UI-Controls angefasst werden.
	 */
	private void verbindungTesten(XControlContainer container) {
		String apiKey = getText(container, CTL_API_KEY_FELD);
		String baseUrl = getText(container, CTL_BASE_URL_FELD);
		aktualisiereStatus(container, I18n.get("ptmonline.optionen.test.laeuft"));
		Thread worker = new Thread(() -> {
			try {
				new PtmOnlineApiClient(apiKey, baseUrl).pruefeVerbindung();
				LoMainThread.post(context, () -> aktualisiereStatus(container, I18n.get("ptmonline.optionen.test.erfolg")));
			} catch (PtmOnlineException e) {
				logger.warn("PTM-Online-Verbindungstest fehlgeschlagen: {}", e.getMessage(), e);
				LoMainThread.post(context,
						() -> aktualisiereStatus(container, I18n.get("ptmonline.optionen.test.fehler", e.getMessage())));
			}
		}, "PTM-Online-Verbindungstest");
		worker.start();
	}

	private void verbindungTrennen(XControlContainer container) {
		setText(container, CTL_API_KEY_FELD, "");
		setText(container, CTL_BASE_URL_FELD, "");
		new LibreOfficePtmOnlineSpeicher(context).speichern("", "");
		aktualisiereStatus(container, I18n.get("ptmonline.optionen.status.getrennt"));
	}

	private void aktualisiereStatus(XControlContainer container, String text) {
		setLabel(container, CTL_STATUS, text);
	}

	// ---- UNO-Control-Hilfsmethoden ----

	private static XControlContainer container(XWindow window) {
		XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
		if (container == null) {
			throw new IllegalStateException("Optionsseite hat kein XControlContainer");
		}
		return container;
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

	private static void setText(XControlContainer container, String name, String text) {
		XTextComponent textComponent = control(container, name, XTextComponent.class);
		if (textComponent != null) {
			textComponent.setText(text == null ? "" : text);
		}
	}

	private static String getText(XControlContainer container, String name) {
		XTextComponent textComponent = control(container, name, XTextComponent.class);
		return textComponent == null ? "" : textComponent.getText().trim();
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
			return Factory.createComponentFactory(PtmOnlineOptionsEventHandler.class, SERVICE_NAMES);
		}
		return null;
	}
}
