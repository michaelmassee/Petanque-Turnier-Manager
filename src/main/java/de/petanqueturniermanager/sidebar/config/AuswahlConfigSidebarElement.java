/**
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XItemListener;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ComboBoxItem;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.fields.LabelPlusCombobox;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public class AuswahlConfigSidebarElement implements ConfigSidebarElement, XItemListener {

	private LabelPlusCombobox labelPlusCombobox;
	private AuswahlConfigProperty configProperty;
	private WorkingSpreadsheet workingSpreadsheet;

	public AuswahlConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam, AuswahlConfigProperty configProperty, WorkingSpreadsheet workingSpreadsheet) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		labelPlusCombobox = LabelPlusCombobox.from(guiFactoryCreateParam).labelText(configProperty.getKey()).helpText(configProperty.getDescription())
				.addAuswahlItems(configProperty.getAuswahl()).addListener(this).select(getComboboxItemValue());
	}

	@Override
	public Layout getLayout() {
		return labelPlusCombobox.getLayout();
	}

	private void setPropertyValue(String keyVal) {
		if (StringUtils.equalsIgnoreCase(getPropertyValue(), keyVal)) {
			// nichts zu tun
			return;
		}
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		docPropHelper.setStringProperty(configProperty.getKey(), keyVal);
	}

	private String getComboboxItemValue() {
		// find text
		String key = getPropertyValue();
		ComboBoxItem itemFromVal = configProperty.getAuswahl().stream().filter(cmbItem -> key.equalsIgnoreCase(cmbItem.getKey())).findAny()
				.orElse(configProperty.getAuswahl().get(0));
		return itemFromVal.getText();
	}

	/**
	 * @return keyval, or default
	 */
	private String getPropertyValue() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		return docPropHelper.getStringProperty(configProperty.getKey(), configProperty.getDefaultVal());
	}

	@Override
	public void disposing(EventObject arg0) {
		workingSpreadsheet = null;
		configProperty = null;
		labelPlusCombobox = null;
	}

	@Override
	public void itemStateChanged(ItemEvent itemEvent) {
		if (itemEvent != null && itemEvent.Selected < configProperty.getAuswahl().size()) {
			ComboBoxItem comboBoxItem = configProperty.getAuswahl().get(itemEvent.Selected);
			setPropertyValue(comboBoxItem.getKey());
		}
	}
}
