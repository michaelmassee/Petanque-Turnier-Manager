/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.io.IOException;
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

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.TournamentSyncClient;

/**
 * Event-Handler fuer die "PTM Online"-Seite unter Extras -&gt; Optionen.
 * <p>
 * Verwaltet API-Key und Basis-URL der PTM-Online-Anbindung (siehe {@link LibreOfficePtmOnlineSpeicher}).
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
	private static final String CTL_API_KEY_ANZEIGEN = "PtmOnlineApiKeyAnzeigen";
	private static final String CTL_BASE_URL_LABEL = "PtmOnlineBaseUrlLabel";
	private static final String CTL_BASE_URL_FELD = "PtmOnlineBaseUrlFeld";
	private static final String CTL_VERBINDUNG_TESTEN = "PtmOnlineVerbindungTesten";

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
		setLabel(container, CTL_LABEL, I18n.get("ptmonline.konfig.bereich"));
		setLabel(container, CTL_API_KEY_LABEL, I18n.get("ptmonline.konfig.label.apikey"));
		setLabel(container, CTL_API_KEY_ANZEIGEN, I18n.get("ptmonline.config.label.apikey.anzeigen"));
		setLabel(container, CTL_BASE_URL_LABEL, I18n.get("ptmonline.konfig.label.baseurl"));
		setLabel(container, CTL_VERBINDUNG_TESTEN, I18n.get("ptmonline.konfig.btn.testen"));

		var zugangsdaten = new LibreOfficePtmOnlineSpeicher(context).laden();
		setText(container, CTL_API_KEY_FELD, zugangsdaten.apiKey());
		setText(container, CTL_BASE_URL_FELD, zugangsdaten.baseUrl());

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
		XButton button = control(container, CTL_VERBINDUNG_TESTEN, XButton.class);
		if (button != null) {
			button.addActionListener(new XActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					verbindungTesten(container);
				}

				@Override
				public void disposing(EventObject event) {
					// nichts zu tun
				}
			});
		}
		XButton anzeigenButton = control(container, CTL_API_KEY_ANZEIGEN, XButton.class);
		if (anzeigenButton != null) {
			anzeigenButton.addActionListener(new XActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					beimApiKeyAnzeigenGeklickt(container);
				}

				@Override
				public void disposing(EventObject event) {
					// nichts zu tun
				}
			});
		}
		listenerContainer = container;
	}

	/**
	 * Schaltet bei jedem Klick die Maskierung (EchoChar) des API-Key-Felds um. Vorbild:
	 * {@code PtmOnlineConfigDialog.beimApiKeyAnzeigenGeklickt} — gleicher vcl-Repaint-Workaround.
	 */
	private static void beimApiKeyAnzeigenGeklickt(XControlContainer container) {
		XControl feldCtrl = container.getControl(CTL_API_KEY_FELD);
		XControl buttonCtrl = container.getControl(CTL_API_KEY_ANZEIGEN);
		if (feldCtrl == null || buttonCtrl == null) {
			return;
		}
		XPropertySet feldProps = UnoRuntime.queryInterface(XPropertySet.class, feldCtrl.getModel());
		XPropertySet buttonProps = UnoRuntime.queryInterface(XPropertySet.class, buttonCtrl.getModel());
		if (feldProps == null || buttonProps == null) {
			return;
		}
		try {
			short aktuellerEchoChar = (short) feldProps.getPropertyValue("EchoChar");
			boolean sichtbar = aktuellerEchoChar == 0;
			feldProps.setPropertyValue("EchoChar", sichtbar ? (short) '*' : (short) 0);
			erzwingeRepaint(feldCtrl);
			buttonProps.setPropertyValue("Label", I18n.get(
					sichtbar ? "ptmonline.config.label.apikey.anzeigen" : "ptmonline.config.label.apikey.verbergen"));
		} catch (Exception e) {
			logger.debug("API-Key Anzeige-Umschaltung fehlgeschlagen", e);
		}
	}

	private static void erzwingeRepaint(XControl ctrl) {
		XWindow fenster = UnoRuntime.queryInterface(XWindow.class, ctrl.getPeer());
		if (fenster == null) {
			return;
		}
		fenster.setVisible(false);
		fenster.setVisible(true);
	}

	private void verbindungTesten(XControlContainer container) {
		String apiKey = getText(container, CTL_API_KEY_FELD);
		String baseUrl = getText(container, CTL_BASE_URL_FELD);
		try {
			new TournamentSyncClient(baseUrl, apiKey).pruefeVerbindung();
			MessageBox.from(context, MessageBoxTypeEnum.INFO_OK)
					.caption(I18n.get("ptmonline.konfig.test.titel"))
					.message(I18n.get("ptmonline.konfig.test.erfolg")).show();
		} catch (IOException e) {
			logger.warn("PTM-Online-Verbindungstest fehlgeschlagen: {}", e.getMessage(), e);
			MessageBox.from(context, MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("ptmonline.konfig.test.titel"))
					.message(I18n.get("ptmonline.konfig.test.fehler", e.getMessage())).show();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
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
