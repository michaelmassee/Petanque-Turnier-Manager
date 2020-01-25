/**
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XItemListener;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.fields.LabelPlusCheckBox;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public class BooleanConfigSidebarElement implements ConfigSidebarElement, XItemListener {

	private LabelPlusCheckBox labelPlusCheckBox;
	private ConfigProperty<?> configProperty;
	private WorkingSpreadsheet workingSpreadsheet;

	BooleanConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<?> configProperty, WorkingSpreadsheet workingSpreadsheet) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		labelPlusCheckBox = LabelPlusCheckBox.from(guiFactoryCreateParam).labelText(configProperty.getKey()).addListener(this).setStat(getPropertyValue());
	}

	@Override
	public Layout getLayout() {
		return labelPlusCheckBox.getLayout();
	}

	private void setPropertyValue(boolean newVal) {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		docPropHelper.setBooleanProperty(configProperty.getKey(), newVal);
	}

	private boolean getPropertyValue() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		return docPropHelper.getBooleanProperty(configProperty.getKey(), (Boolean) configProperty.getDefaultVal());
	}

	@Override
	public void disposing(EventObject arg0) {
		workingSpreadsheet = null;
		configProperty = null;
		labelPlusCheckBox = null;
	}

	@Override
	public void itemStateChanged(ItemEvent itemEvent) {
		setPropertyValue((itemEvent.Selected > 0 ? true : false));
	}
}
