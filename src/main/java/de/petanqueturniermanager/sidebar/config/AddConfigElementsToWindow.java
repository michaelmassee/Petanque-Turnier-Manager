/**
 * Erstellung 07.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.config.color.BackgrnColorConfigSidebarElement;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public class AddConfigElementsToWindow {

	private final GuiFactoryCreateParam guiFactoryCreateParam;
	private final WorkingSpreadsheet currentSpreadsheet;
	private final Layout layout;

	public AddConfigElementsToWindow(GuiFactoryCreateParam guiFactoryCreateParam, WorkingSpreadsheet currentSpreadsheet,
			Layout layout) {
		this.guiFactoryCreateParam = checkNotNull(guiFactoryCreateParam);
		this.currentSpreadsheet = checkNotNull(currentSpreadsheet);
		this.layout = checkNotNull(layout);
	}

	public void addPropToPanel(ConfigProperty<?> configProperty) {

		switch (configProperty.getType()) {
		case STRING:

			if (configProperty instanceof AuswahlConfigProperty) {
				// ComboBox
				AuswahlConfigSidebarElement auswahlConfigSidebarElement = new AuswahlConfigSidebarElement(
						guiFactoryCreateParam, (AuswahlConfigProperty) configProperty, currentSpreadsheet);
				layout.addLayout(auswahlConfigSidebarElement.getLayout(), 1);
			} else {
				// create textfield mit btn
				@SuppressWarnings("unchecked")
				StringConfigSidebarElement stringConfigSidebarElement = new StringConfigSidebarElement(
						guiFactoryCreateParam, (ConfigProperty<String>) configProperty, currentSpreadsheet);
				layout.addLayout(stringConfigSidebarElement.getLayout(), 1);
			}
			break;
		case BOOLEAN:
			// create checkbox
			@SuppressWarnings("unchecked")
			BooleanConfigSidebarElement booleanConfigSidebarElement = new BooleanConfigSidebarElement(
					guiFactoryCreateParam, (ConfigProperty<Boolean>) configProperty, currentSpreadsheet);
			layout.addLayout(booleanConfigSidebarElement.getLayout(), 1);
			break;
		case COLOR:
			// create colorpicker
			@SuppressWarnings("unchecked")
			BackgrnColorConfigSidebarElement backgrnColorConfigSidebarElement = new BackgrnColorConfigSidebarElement(
					guiFactoryCreateParam, (ConfigProperty<Integer>) configProperty, currentSpreadsheet);
			layout.addLayout(backgrnColorConfigSidebarElement.getLayout(), 1);
			break;
		case INTEGER:
			@SuppressWarnings("unchecked")
			IntegerConfigSidebarElement integerConfigSidebarElement = new IntegerConfigSidebarElement(
					guiFactoryCreateParam, (ConfigProperty<Integer>) configProperty, currentSpreadsheet);
			layout.addLayout(integerConfigSidebarElement.getLayout(), 1);
			break;
		default:
			break;
		}

	}

}
