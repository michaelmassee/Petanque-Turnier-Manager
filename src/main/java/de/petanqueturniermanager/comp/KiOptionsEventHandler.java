/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.helper.BrowserOeffner;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ki.KiOptionen;

public final class KiOptionsEventHandler extends com.sun.star.lib.uno.helper.WeakBase
        implements XServiceInfo, XContainerWindowEventHandler {

    private static final String IMPLEMENTATION_NAME = KiOptionsEventHandler.class.getName();
    private static final String SERVICE_NAME = "de.petanqueturniermanager.KiOptionsEventHandler";
    private static final String[] SERVICE_NAMES = { SERVICE_NAME };
    private static final Logger logger = LogManager.getLogger(KiOptionsEventHandler.class);
    private static final String OPENAI_API_KEYS_URL = "https://platform.openai.com/api-keys";

    private static final String METHOD_EXTERNAL_EVENT = "external_event";
    private static final String EVENT_INITIALIZE = "initialize";
    private static final String EVENT_BACK = "back";
    private static final String EVENT_OK = "ok";

    private static final String CTL_API_KEY = "KiApiKey";
    private static final String CTL_MODEL = "KiModel";
    private static final String CTL_BASE_URL = "KiBaseUrl";
    private static final String CTL_TIMEOUT = "KiTimeout";
    private static final String CTL_FULL_CONTEXT = "KiFullContext";
    private static final String CTL_API_KEY_OEFFNEN = "KiApiKeyOeffnen";
    private static final String CTL_STANDARDWERTE = "KiStandardwerte";
    private static final String CTL_PRUEFEN = "KiPruefen";
    private static final String CTL_STATUS = "KiStatus";

    private final XComponentContext context;
    private XControlContainer listenerContainer;

    public KiOptionsEventHandler(XComponentContext context) {
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
        setLabel(container, "KiLabel", I18n.get("ki.options.label"));
        setLabel(container, "KiSetupHint", I18n.get("ki.options.setup.hint"));
        setLabel(container, "KiApiKeyLabel", I18n.get("ki.options.api.key"));
        setLabel(container, "KiApiKeyHelp", I18n.get("ki.options.api.key.help"));
        setLabel(container, "KiModelLabel", I18n.get("ki.options.model"));
        setLabel(container, "KiBaseUrlLabel", I18n.get("ki.options.base.url"));
        setLabel(container, "KiTimeoutLabel", I18n.get("ki.options.timeout"));
        setLabel(container, CTL_FULL_CONTEXT, I18n.get("ki.options.full.context"));
        setLabel(container, CTL_API_KEY_OEFFNEN, I18n.get("ki.options.api.key.oeffnen"));
        setLabel(container, CTL_STANDARDWERTE, I18n.get("ki.options.standardwerte"));
        setLabel(container, CTL_PRUEFEN, I18n.get("ki.options.pruefen"));
        setLabel(container, "KiPrivacyHint", I18n.get("ki.options.privacy.hint"));

        KiOptionen optionen = GlobalProperties.get().getKiOptionen();
        setText(container, CTL_API_KEY, optionen.apiKey());
        setText(container, CTL_MODEL, optionen.model());
        setText(container, CTL_BASE_URL, optionen.baseUrl());
        setText(container, CTL_TIMEOUT, Integer.toString(optionen.timeoutSekunden()));
        setCheckbox(container, CTL_FULL_CONTEXT, optionen.vollstaendigenKontextSenden());
        aktualisiereStatus(container);
        registriereListener(container);
    }

    private void speichereAusOberflaeche(XWindow window) {
        XControlContainer container = container(window);
        KiOptionen optionen = optionenAusOberflaeche(container);
        List<KiOptionen.KonfigurationsFehler> fehler = optionen.apiKonfigurationsFehler();
        if (!fehler.isEmpty()) {
            zeigeWarnung(I18n.get("ki.options.pruefung.fehler", fehlerText(fehler)));
            aktualisiereStatus(container);
            return;
        }
        GlobalProperties.get().speichernKiOptionen(optionen);
    }

    private void registriereListener(XControlContainer container) {
        if (listenerContainer != null && UnoRuntime.areSame(listenerContainer, container)) {
            return;
        }
        registriereActionListener(container, CTL_API_KEY_OEFFNEN, this::oeffneApiKeySeite);
        registriereActionListener(container, CTL_STANDARDWERTE, () -> setzeStandardwerte(container));
        registriereActionListener(container, CTL_PRUEFEN, () -> pruefeKonfiguration(container));
        listenerContainer = container;
    }

    private void oeffneApiKeySeite() {
        try {
            BrowserOeffner.oeffne(OPENAI_API_KEYS_URL);
        } catch (IOException e) {
            logger.warn("OpenAI API-Key-Seite konnte nicht geoeffnet werden: {}", e.getMessage(), e);
            zeigeWarnung(I18n.get("ki.options.api.key.oeffnen.fehler", OPENAI_API_KEYS_URL));
        }
    }

    private static void setzeStandardwerte(XControlContainer container) {
        setText(container, CTL_MODEL, KiOptionen.DEFAULT_MODEL);
        setText(container, CTL_BASE_URL, KiOptionen.DEFAULT_BASE_URL);
        setText(container, CTL_TIMEOUT, Integer.toString(KiOptionen.DEFAULT_TIMEOUT_SEKUNDEN));
        aktualisiereStatus(container);
    }

    private void pruefeKonfiguration(XControlContainer container) {
        KiOptionen optionen = optionenAusOberflaeche(container);
        List<KiOptionen.KonfigurationsFehler> fehler = optionen.apiKonfigurationsFehler();
        if (fehler.isEmpty()) {
            MessageBox.from(context, MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("ki.options.pruefung.titel"))
                    .message(I18n.get("ki.options.pruefung.ok"))
                    .show();
        } else {
            zeigeWarnung(I18n.get("ki.options.pruefung.fehler", fehlerText(fehler)));
        }
        aktualisiereStatus(container);
    }

    private static void aktualisiereStatus(XControlContainer container) {
        KiOptionen optionen = optionenAusOberflaeche(container);
        List<KiOptionen.KonfigurationsFehler> fehler = optionen.apiKonfigurationsFehler();
        setLabel(container, CTL_STATUS, fehler.isEmpty()
                ? I18n.get("ki.options.status.ok")
                : I18n.get("ki.options.status.fehlt", fehlerText(fehler)));
    }

    private static KiOptionen optionenAusOberflaeche(XControlContainer container) {
        return new KiOptionen(
                text(container, CTL_API_KEY),
                text(container, CTL_MODEL),
                text(container, CTL_BASE_URL),
                parseInt(text(container, CTL_TIMEOUT), KiOptionen.DEFAULT_TIMEOUT_SEKUNDEN),
                checkbox(container, CTL_FULL_CONTEXT));
    }

    private static String fehlerText(List<KiOptionen.KonfigurationsFehler> fehler) {
        return fehler.stream()
                .map(KiOptionsEventHandler::fehlerLabel)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private static String fehlerLabel(KiOptionen.KonfigurationsFehler fehler) {
        return switch (fehler) {
            case API_KEY -> I18n.get("ki.api.key");
            case MODELL -> I18n.get("ki.model");
            case BASE_URL -> I18n.get("ki.base.url");
            case TIMEOUT -> I18n.get("ki.timeout.seconds");
        };
    }

    private void zeigeWarnung(String meldung) {
        MessageBox.from(context, MessageBoxTypeEnum.WARN_OK)
                .caption(I18n.get("ki.options.pruefung.titel"))
                .message(meldung)
                .show();
    }

    private static int parseInt(String wert, int fallback) {
        try {
            return Integer.parseInt(wert.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
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
        try {
            if (props != null) {
                props.setPropertyValue("Label", label);
            }
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
            return Factory.createComponentFactory(KiOptionsEventHandler.class, SERVICE_NAMES);
        }
        return null;
    }
}
