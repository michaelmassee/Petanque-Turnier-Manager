/**
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XItemListener;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.fields.LabelPlusCheckBox;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public class BooleanConfigSidebarElement implements ConfigSidebarElement, XItemListener {

	private LabelPlusCheckBox labelPlusCheckBox;
	private ConfigProperty<Boolean> configProperty;
	private WorkingSpreadsheet workingSpreadsheet;

	public BooleanConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<Boolean> configProperty, WorkingSpreadsheet workingSpreadsheet) {
		this(guiFactoryCreateParam, configProperty, workingSpreadsheet, false);
	}

	public BooleanConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<Boolean> configProperty, WorkingSpreadsheet workingSpreadsheet,
			boolean readOnly) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		labelPlusCheckBox = LabelPlusCheckBox.from(guiFactoryCreateParam).labelText(configProperty.getKey()).helpText(configProperty.getDescription()).addListener(this)
				.setStat(getPropertyValue()).readOnly(readOnly);
	}

	@Override
	public Layout getLayout() {
		if (labelPlusCheckBox != null) {
			return labelPlusCheckBox.getLayout();
		}
		return new HorizontalLayout();
	}

	private void setPropertyValue(boolean newVal) {
		if (getPropertyValue() == newVal) {
			return; // nichts zu tun
		}

		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		docPropHelper.setBooleanProperty(configProperty.getKey(), newVal);
	}

	private boolean getPropertyValue() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		return docPropHelper.getBooleanProperty(configProperty.getKey(), configProperty.getDefaultVal());
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

	@Override
	public void onPropertiesChanged(ITurnierEvent eventObj) {
		if (labelPlusCheckBox != null) {
			labelPlusCheckBox.setStat(getPropertyValue());
		}
	}
}
