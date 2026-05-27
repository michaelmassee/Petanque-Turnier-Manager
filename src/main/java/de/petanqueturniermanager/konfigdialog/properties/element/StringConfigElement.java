/*
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties.element;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
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
import de.petanqueturniermanager.konfigdialog.gui.LabelPlusTextPlusTextareaBox;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public class StringConfigElement implements ConfigElement, XTextListener {

	static final Logger logger = LogManager.getLogger(StringConfigElement.class);

	LabelPlusTextPlusTextareaBox labelPlusTextPlusTextareaBox;
	ConfigProperty<?> configProperty;
	private WorkingSpreadsheet workingSpreadsheet;
	TextAreaDialog textAreaDialog;

	public StringConfigElement(GuiFactoryCreateParam guiFactoryCreateParam, ConfigProperty<String> configProperty, WorkingSpreadsheet workingSpreadsheet) {
		this.configProperty = checkNotNull(configProperty);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
		textAreaDialog = new TextAreaDialog(workingSpreadsheet);
		var labelText = configProperty.getDescription() != null ? configProperty.getDescription() : configProperty.getKey();
		labelPlusTextPlusTextareaBox = LabelPlusTextPlusTextareaBox.from(guiFactoryCreateParam).labelText(labelText).helpText(labelText)
				.addXTextListener(this).addXActionListener(btnXActionListener).fieldText(getPropertyValue());
	}

	@Override
	public Layout getLayout() {
		if (labelPlusTextPlusTextareaBox != null) {
			return labelPlusTextPlusTextareaBox.getLayout();
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
		textAreaDialog = null;
		workingSpreadsheet = null;
		configProperty = null;
		labelPlusTextPlusTextareaBox = null;
	}

	@Override
	public void textChanged(TextEvent arg0) {
		if (labelPlusTextPlusTextareaBox != null) {
			setPropertyValue(labelPlusTextPlusTextareaBox.getFieldText());
		}
	}

	XActionListener btnXActionListener = new XActionListener() {

		@Override
		public void disposing(EventObject arg0) {
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {

			try {
				if (ObjectUtils.allNotNull(textAreaDialog, labelPlusTextPlusTextareaBox)) {
					String alterWert = getPropertyValue();
					textAreaDialog.initTextArea(configProperty.getKey(), configProperty.getKey(), labelPlusTextPlusTextareaBox.getFieldText());
					textAreaDialog.createDialog();
					String neuerWert = getPropertyValue();
					labelPlusTextPlusTextareaBox.fieldText(neuerWert);
					// TextAreaDialog.save() schreibt die Doc-Property direkt und umgeht
					// damit den textChanged-Pfad; nachSpeichernAktion (z.B. PageStyle-
					// Live-Update) muss hier explizit angestoßen werden.
					if (!Objects.equals(alterWert, neuerWert)) {
						configProperty.invokeNachSpeichernAktion(workingSpreadsheet);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	};
}
