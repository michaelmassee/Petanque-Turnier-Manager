/**
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XTextListener;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.fields.LabelPlusNumericField;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public class IntegerConfigSidebarElement implements ConfigSidebarElement, XTextListener {

	static final Logger logger = LogManager.getLogger(IntegerConfigSidebarElement.class);

	LabelPlusNumericField labelPlusNumericField;
	ConfigProperty<?> configProperty;
	private WorkingSpreadsheet workingSpreadsheet;
	TextAreaDialog textAreaDialog;

	public IntegerConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<Integer> configProperty, WorkingSpreadsheet workingSpreadsheet) {
		this(guiFactoryCreateParam, configProperty, workingSpreadsheet, false);
	}

	public IntegerConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<Integer> configProperty, WorkingSpreadsheet workingSpreadsheet,
			boolean readOnly) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		textAreaDialog = new TextAreaDialog(workingSpreadsheet);
		labelPlusNumericField = LabelPlusNumericField.from(guiFactoryCreateParam).labelText(configProperty.getKey()).helpText(configProperty.getDescription())
				.addXTextListener(this).fieldVal(getPropertyValue()).readOnly(readOnly);
	}

	@Override
	public Layout getLayout() {
		if (labelPlusNumericField != null) {
			return labelPlusNumericField.getLayout();
		}
		return new HorizontalLayout();
	}

	private void setPropertyValue(Integer newVal) {
		if (getPropertyValue() == newVal) {
			return; // nichts zu tun
		}

		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		docPropHelper.setIntProperty(configProperty.getKey(), newVal);
	}

	Integer getPropertyValue() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		return docPropHelper.getIntProperty(configProperty.getKey(), (Integer) configProperty.getDefaultVal());
	}

	@Override
	public void disposing(EventObject arg0) {
		textAreaDialog = null;
		workingSpreadsheet = null;
		configProperty = null;
		labelPlusNumericField = null;
	}

	@Override
	public void textChanged(TextEvent arg0) {
		if (labelPlusNumericField != null) {
			setPropertyValue((int) Math.round(labelPlusNumericField.getFieldVal()));
		}
	}

	@Override
	public void onPropertiesChanged(ITurnierEvent eventObj) {
		if (labelPlusNumericField != null) {
			labelPlusNumericField.fieldVal(getPropertyValue());
		}
	}
}
