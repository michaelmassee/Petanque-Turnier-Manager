/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties.element;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XTextListener;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.gui.LabelPlusTextBox;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * Kompaktes einzeiliges Textfeld ohne Textarea-Edit-Button — für kurze Werte wie eine Uhrzeit
 * (HH:MM). Siehe {@link StringConfigElement} für die Variante mit Textarea-Editor.
 *
 * @author Michael Massee
 */
public class SimpleTextConfigElement implements ConfigElement, XTextListener {

	LabelPlusTextBox labelPlusTextBox;
	ConfigProperty<?> configProperty;
	private WorkingSpreadsheet workingSpreadsheet;

	public SimpleTextConfigElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<String> configProperty,
			WorkingSpreadsheet workingSpreadsheet) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		var labelText = configProperty.getDescription() != null ? configProperty.getDescription() : configProperty.getKey();
		labelPlusTextBox = LabelPlusTextBox.from(guiFactoryCreateParam).labelText(labelText).helpText(labelText)
				.addXTextListener(this).fieldText(getPropertyValue());
	}

	@Override
	public Layout getLayout() {
		if (labelPlusTextBox != null) {
			return labelPlusTextBox.getLayout();
		}
		return new HorizontalLayout();
	}

	private void setPropertyValue(String newVal) {
		if (Objects.equals(getPropertyValue(), newVal)) {
			return; // nichts zu tun
		}

		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		docPropHelper.setStringProperty(configProperty.getKey(), newVal);
		configProperty.invokeNachSpeichernAktion(workingSpreadsheet);
	}

	String getPropertyValue() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		return docPropHelper.getStringProperty(configProperty.getKey(), (String) configProperty.getDefaultVal());
	}

	@Override
	public void disposing(EventObject arg0) {
		workingSpreadsheet = null;
		configProperty = null;
		labelPlusTextBox = null;
	}

	@Override
	public void textChanged(TextEvent arg0) {
		if (labelPlusTextBox != null) {
			setPropertyValue(labelPlusTextBox.getFieldText());
		}
	}

}
