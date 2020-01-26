/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.star.awt.InvalidateStyle;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;

/**
 * @author Michael Massee
 *
 */
public class ConfigSidebarContent extends BaseSidebarContent {

	private boolean didAddFields = false;

	private List<ConfigSidebarElement> configElements = new ArrayList<>();

	/**
	 * @param workingSpreadsheet
	 * @param parentWindow
	 */
	public ConfigSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow) {
		super(workingSpreadsheet, parentWindow);
	}

	@Override
	protected void disposing(EventObject event) {
		configElements.clear();
	}

	@Override
	protected void updateFieldContens(ITurnierEvent eventObj) {
		// Turnier vorhanden ?
		addFields();
	}

	@Override
	protected void addFields() {
		if (didAddFields) {
			return;
		}

		List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(getCurrentSpreadsheet());
		if (konfigProperties == null) {
			// kein Turnier vorhanden
			return;
		}

		didAddFields = true;
		konfigProperties.stream().filter(konfigprop -> konfigprop.isInSideBar()).collect(Collectors.toList()).forEach(konfigprop -> addPropToPanel(konfigprop));

		// https://www.openoffice.org/api/docs/common/ref/com/sun/star/awt/InvalidateStyle.html
		// InvalidateStyle.UPDATE
		// force repaint
		// funktioniert manchmal .... !?!?
		getGuiFactoryCreateParam().getWindowPeer().invalidate((short) (InvalidateStyle.TRANSPARENT | InvalidateStyle.CHILDREN));
	}

	private void addPropToPanel(ConfigProperty<?> configProperty) {

		switch (configProperty.getType()) {
		case STRING:
			// create textfield mit btn
			StringConfigSidebarElement stringConfigSidebarElement = new StringConfigSidebarElement(getGuiFactoryCreateParam(), configProperty, getCurrentSpreadsheet());
			getLayout().addLayout(stringConfigSidebarElement.getLayout(), 1);
			configElements.add(stringConfigSidebarElement);
			break;
		case BOOLEAN:
			// create checkbox
			BooleanConfigSidebarElement booleanConfigSidebarElement = new BooleanConfigSidebarElement(getGuiFactoryCreateParam(), configProperty, getCurrentSpreadsheet());
			getLayout().addLayout(booleanConfigSidebarElement.getLayout(), 1);
			configElements.add(booleanConfigSidebarElement);
			break;
		case COLOR:
			break;
		case INTEGER:
			break;
		default:
			break;
		}

	}

}
