/*
 * Erstellung 07.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties.element;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.konfigdialog.properties.element.color.BackgrnColorConfigElement;
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
				AuswahlConfigElement auswahlConfigElement = new AuswahlConfigElement(
						guiFactoryCreateParam, (AuswahlConfigProperty) configProperty, currentSpreadsheet);
				layout.addLayout(auswahlConfigElement.getLayout(), 1);
			} else {
				// create textfield mit btn
				@SuppressWarnings("unchecked")
				StringConfigElement stringConfigElement = new StringConfigElement(
						guiFactoryCreateParam, (ConfigProperty<String>) configProperty, currentSpreadsheet);
				layout.addLayout(stringConfigElement.getLayout(), 1);
			}
			break;
		case BOOLEAN:
			// create checkbox
			@SuppressWarnings("unchecked")
			BooleanConfigElement booleanConfigElement = new BooleanConfigElement(
					guiFactoryCreateParam, (ConfigProperty<Boolean>) configProperty, currentSpreadsheet);
			layout.addLayout(booleanConfigElement.getLayout(), 1);
			break;
		case COLOR:
			// create colorpicker
			@SuppressWarnings("unchecked")
			BackgrnColorConfigElement backgrnColorConfigElement = new BackgrnColorConfigElement(
					guiFactoryCreateParam, (ConfigProperty<Integer>) configProperty, currentSpreadsheet);
			layout.addLayout(backgrnColorConfigElement.getLayout(), 1);
			break;
		case INTEGER:
			@SuppressWarnings("unchecked")
			IntegerConfigElement integerConfigElement = new IntegerConfigElement(
					guiFactoryCreateParam, (ConfigProperty<Integer>) configProperty, currentSpreadsheet);
			layout.addLayout(integerConfigElement.getLayout(), 1);
			break;
		default:
			break;
		}

	}

}
