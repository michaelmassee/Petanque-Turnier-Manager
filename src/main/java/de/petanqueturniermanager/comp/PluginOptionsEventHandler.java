/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.Arrays;

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

import de.petanqueturniermanager.comp.newrelease.ReleaseUpdateService;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Event-Handler fuer die PTM-Seite unter Extras -> Optionen.
 */
public final class PluginOptionsEventHandler extends WeakBase
		implements XServiceInfo, XContainerWindowEventHandler {

	private static final Logger logger = LogManager.getLogger(PluginOptionsEventHandler.class);

	private static final String IMPLEMENTATION_NAME = PluginOptionsEventHandler.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.PluginOptionsEventHandler";
	private static final String[] SERVICE_NAMES = { SERVICE_NAME };

	private static final String METHOD_EXTERNAL_EVENT = "external_event";
	private static final String EVENT_INITIALIZE = "initialize";
	private static final String EVENT_BACK = "back";
	private static final String EVENT_OK = "ok";

	private static final String CTL_AUTOSAVE = "Autosave";
	private static final String CTL_BACKUP = "Backup";
	private static final String CTL_NEW_VERSION_CHECK = "NewVersionCheck";
	private static final String CTL_AUTO_UPDATE_DIALOG_STARTUP = "AutoUpdateDialogStartup";
	private static final String CTL_PROCESSBOX_SHOW = "ProcessBoxAutomaticallyShow";
	private static final String CTL_PROCESSBOX_CLOSE = "ProcessBoxAutomaticallyClose";
	private static final String CTL_PERFORMANCE_LOGGING = "PerformanceLogging";
	private static final String CTL_LOG_LEVEL = "LogLevel";
	private static final String CTL_LOG_LEVEL_LABEL = "LogLevelLabel";

	public PluginOptionsEventHandler(XComponentContext context) {
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
		setCheckbox(container, CTL_AUTOSAVE, properties.isAutoSave());
		setCheckbox(container, CTL_BACKUP, properties.isCreateBackup());
		setCheckbox(container, CTL_NEW_VERSION_CHECK, properties.isNewVersionCheckImmerTrue());
		setCheckbox(container, CTL_AUTO_UPDATE_DIALOG_STARTUP, properties.isAutoUpdateDialogBeimStartAktiv());
		setCheckbox(container, CTL_PROCESSBOX_SHOW, properties.isProzessBoxAutomatischAnzeigen());
		setCheckbox(container, CTL_PROCESSBOX_CLOSE, properties.isProzessBoxAutomatischSchliessen());
		setCheckbox(container, CTL_PERFORMANCE_LOGGING, properties.isPerformanceLogging());
		setText(container, CTL_LOG_LEVEL, properties.getLogLevel());
	}

	private void speichereAusOberflaeche(XWindow window) {
		XControlContainer container = container(window);
		GlobalProperties properties = GlobalProperties.get();
		properties.speichern(
				checkbox(container, CTL_AUTOSAVE),
				checkbox(container, CTL_BACKUP),
				checkbox(container, CTL_NEW_VERSION_CHECK),
				checkbox(container, CTL_PROCESSBOX_SHOW),
				checkbox(container, CTL_PROCESSBOX_CLOSE),
				checkbox(container, CTL_PERFORMANCE_LOGGING),
				text(container, CTL_LOG_LEVEL),
				checkbox(container, CTL_AUTO_UPDATE_DIALOG_STARTUP));
		try {
			ReleaseUpdateService.get().loeseListenerAus();
		} catch (IllegalStateException e) {
			logger.debug("ReleaseUpdateService nicht initialisiert");
		}
	}

	private void setzeLabels(XControlContainer container) {
		setLabel(container, CTL_AUTOSAVE, I18n.get("konfig.plugin.autosave"));
		setLabel(container, CTL_BACKUP, I18n.get("konfig.plugin.backup"));
		setLabel(container, CTL_NEW_VERSION_CHECK, I18n.get("konfig.plugin.new.version.check"));
		setLabel(container, CTL_AUTO_UPDATE_DIALOG_STARTUP, I18n.get("konfig.plugin.auto.update.dialog.startup"));
		setLabel(container, CTL_PROCESSBOX_SHOW, I18n.get("konfig.prozessbox.automatisch.anzeigen"));
		setLabel(container, CTL_PROCESSBOX_CLOSE, I18n.get("konfig.prozessbox.automatisch.schliessen"));
		setLabel(container, CTL_PERFORMANCE_LOGGING, I18n.get("konfig.performance.logging"));
		setLabel(container, CTL_LOG_LEVEL_LABEL, I18n.get("konfig.plugin.log.level"));
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
			return Factory.createComponentFactory(PluginOptionsEventHandler.class, SERVICE_NAMES);
		}
		return null;
	}
}
