/**
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XTextListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.fields.LabelPlusTextPlusTextareaBox;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public class StringConfigSidebarElement implements ConfigSidebarElement, XTextListener {

	static final Logger logger = LogManager.getLogger(StringConfigSidebarElement.class);

	LabelPlusTextPlusTextareaBox labelPlusTextPlusTextareaBox;
	ConfigProperty<?> configProperty;
	private WorkingSpreadsheet workingSpreadsheet;
	TextAreaDialog textAreaDialog;

	public StringConfigSidebarElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<String> configProperty, WorkingSpreadsheet workingSpreadsheet) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		textAreaDialog = new TextAreaDialog(workingSpreadsheet);
		labelPlusTextPlusTextareaBox = LabelPlusTextPlusTextareaBox.from(guiFactoryCreateParam).labelText(configProperty.getKey()).helpText(configProperty.getDescription())
				.addXTextListener(this).addXActionListener(btnXActionListener).fieldText(getPropertyValue());
	}

	@Override
	public Layout getLayout() {
		return labelPlusTextPlusTextareaBox.getLayout();
	}

	private void setPropertyValue(String newVal) {
		if (StringUtils.equals(getPropertyValue(), newVal)) {
			return; // nichts zu tun
		}

		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		docPropHelper.setStringProperty(configProperty.getKey(), newVal);
	}

	String getPropertyValue() {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		return docPropHelper.getStringProperty(configProperty.getKey(), true, (String) configProperty.getDefaultVal());
	}

	@Override
	public void disposing(EventObject arg0) {
		textAreaDialog = null;
		workingSpreadsheet = null;
		configProperty = null;
		labelPlusTextPlusTextareaBox = null;
	}

	@Override
	public void textChanged(TextEvent arg0) {
		setPropertyValue(labelPlusTextPlusTextareaBox.getFieldText());
	}

	XActionListener btnXActionListener = new XActionListener() {

		@Override
		public void disposing(EventObject arg0) {
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// btn Klicked
			try {
				textAreaDialog.initTextArea(configProperty.getKey(), configProperty.getKey(), labelPlusTextPlusTextareaBox.getFieldText());
				textAreaDialog.createDialog();
				// update
				labelPlusTextPlusTextareaBox.fieldText(getPropertyValue());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	};
}
